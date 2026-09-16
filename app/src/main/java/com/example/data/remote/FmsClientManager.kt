package com.example.data.remote

import android.content.Context
import android.content.SharedPreferences
import com.example.BuildConfig
import com.example.data.remote.dto.HealthCheckDto
import com.example.data.remote.dto.LoginDto
import com.example.data.remote.dto.LoginResponse
import com.example.data.remote.dto.UserResponseDto
import com.example.data.security.SecureStorage
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor
import org.json.JSONObject
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLException

enum class FmsConnectionState {
    DISCONNECTED,
    CHECKING,
    CONNECTED,
    WAKING_UP,       // Render free tier cold-start (container booting up, ~30-50s)
    UNREACHABLE,     // Network offline, DNS lookup failure, connection refused (offline mode)
    CONFLICT,        // HTTP 409 Conflict on resource / synchronization
    SESSION_EXPIRED, // HTTP 401 Unauthorized / Token expired
    ERROR            // Server 5xx or unclassified failure
}

data class ConflictInfo(
    val entityType: String,
    val title: String,
    val message: String,
    val entityId: String? = null,
    val suggestedAction: String = "Merge or overwrite server resource",
    val timestamp: Long = System.currentTimeMillis()
)

sealed class FmsApiResult<out T> {
    data class Success<out T>(val data: T) : FmsApiResult<T>()
    data class Conflict(val message: String, val entityType: String = "Resource", val entityId: String? = null, val rawError: String? = null) : FmsApiResult<Nothing>()
    data class Unreachable(val message: String, val isRenderColdStart: Boolean = false) : FmsApiResult<Nothing>()
    data class Unauthorized(val message: String) : FmsApiResult<Nothing>()
    data class ValidationError(val message: String, val fields: Map<String, String> = emptyMap()) : FmsApiResult<Nothing>()
    data class RateLimited(val message: String) : FmsApiResult<Nothing>()
    data class ServerError(val code: Int, val message: String) : FmsApiResult<Nothing>()
    data class GeneralError(val message: String, val throwable: Throwable? = null) : FmsApiResult<Nothing>()

    fun getOrNull(): T? = (this as? Success)?.data
    val isSuccess: Boolean get() = this is Success
}

data class FmsBackendStatus(
    val connectionState: FmsConnectionState = FmsConnectionState.DISCONNECTED,
    val baseUrl: String = "https://mindforge-backend-m7uz.onrender.com/",
    val isAuthenticated: Boolean = false,
    val userEmail: String? = null,
    val userName: String? = null,
    val lastPingMessage: String? = null,
    val errorMessage: String? = null,
    val conflictInfo: ConflictInfo? = null,
    val isWakingUp: Boolean = false,
    val pendingOfflineCount: Int = 0
) {
    val hasConflict: Boolean get() = conflictInfo != null || connectionState == FmsConnectionState.CONFLICT
}

class FmsClientManager(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("fms_backend_prefs", Context.MODE_PRIVATE)

    private fun getDecryptedToken(): String? {
        val stored = prefs.getString(KEY_AUTH_TOKEN, null) ?: return null
        return SecureStorage.decrypt(stored)
    }

    private fun setEncryptedToken(token: String?) {
        if (token.isNullOrBlank()) {
            prefs.edit().remove(KEY_AUTH_TOKEN).apply()
        } else {
            val encrypted = SecureStorage.encrypt(token) ?: token
            prefs.edit().putString(KEY_AUTH_TOKEN, encrypted).apply()
        }
    }

    private fun getEffectiveBaseUrl(): String {
        val saved = prefs.getString(KEY_BASE_URL, null)
        return if (saved.isNullOrBlank() || saved.contains("10.0.2.2") || saved.contains("localhost")) {
            prefs.edit().putString(KEY_BASE_URL, DEFAULT_BASE_URL).apply()
            DEFAULT_BASE_URL
        } else {
            saved
        }
    }

    private val _status = MutableStateFlow(
        FmsBackendStatus(
            baseUrl = getEffectiveBaseUrl(),
            isAuthenticated = !getDecryptedToken().isNullOrBlank(),
            userEmail = prefs.getString(KEY_USER_EMAIL, null),
            userName = prefs.getString(KEY_USER_NAME, null)
        )
    )
    val status: StateFlow<FmsBackendStatus> = _status.asStateFlow()

    private var moshi: Moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private var apiService: FmsApiService? = null

    init {
        rebuildApiService()
        CoroutineScope(Dispatchers.IO).launch {
            testConnection()
        }
    }

    private fun rebuildApiService() {
        val currentBaseUrl = ensureTrailingSlash(prefs.getString(KEY_BASE_URL, DEFAULT_BASE_URL) ?: DEFAULT_BASE_URL)
        val isRender = currentBaseUrl.contains("onrender.com")
        // Render free-tier cold starts can take 30-50s to spin up from sleep
        val timeoutSeconds = if (isRender) 45L else 20L

        val authInterceptor = Interceptor { chain ->
            val token = getDecryptedToken()
            val requestBuilder = chain.request().newBuilder()
            if (!token.isNullOrBlank()) {
                requestBuilder.addHeader("Authorization", "Bearer $token")
            }
            requestBuilder.addHeader("Accept", "application/json")
            val response = chain.proceed(requestBuilder.build())

            // Auto-detect expired auth token from backend
            if (response.code == 401 && !token.isNullOrBlank()) {
                setEncryptedToken(null)
                _status.value = _status.value.copy(
                    isAuthenticated = false,
                    connectionState = FmsConnectionState.SESSION_EXPIRED,
                    errorMessage = "Your session expired. Please sign in again or continue in offline mode."
                )
            }
            response
        }

        val logging = HttpLoggingInterceptor().apply {
            redactHeader("Authorization")
            redactHeader("Cookie")
            redactHeader("Set-Cookie")
            // Prevent credential/token leakage in Logcat logs
            level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BASIC else HttpLoggingInterceptor.Level.NONE
        }

        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(logging)
            .connectTimeout(timeoutSeconds, TimeUnit.SECONDS)
            .readTimeout(timeoutSeconds, TimeUnit.SECONDS)
            .writeTimeout(timeoutSeconds, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()

        try {
            val retrofit = Retrofit.Builder()
                .baseUrl(currentBaseUrl)
                .client(okHttpClient)
                .addConverterFactory(MoshiConverterFactory.create(moshi))
                .build()

            apiService = retrofit.create(FmsApiService::class.java)
        } catch (e: Exception) {
            e.printStackTrace()
            apiService = null
        }
    }

    fun getService(): FmsApiService {
        if (apiService == null) {
            rebuildApiService()
        }
        return apiService ?: throw IllegalStateException("FmsApiService could not be initialized")
    }

    fun validateAndSanitizeUrl(url: String): String? {
        val trimmed = url.trim()
        val parsed = trimmed.toHttpUrlOrNull() ?: return null
        // Loopback / emulator IPs can use HTTP; public remote hosts MUST use HTTPS to prevent cleartext exposure
        val isLoopback = parsed.host in listOf("10.0.2.2", "localhost", "127.0.0.1")
        if (parsed.scheme != "https" && !isLoopback) {
            return null
        }
        return ensureTrailingSlash(trimmed)
    }

    fun updateBaseUrl(newUrl: String): Boolean {
        val sanitized = validateAndSanitizeUrl(newUrl) ?: return false
        prefs.edit().putString(KEY_BASE_URL, sanitized).apply()
        _status.value = _status.value.copy(
            baseUrl = sanitized,
            connectionState = FmsConnectionState.DISCONNECTED,
            lastPingMessage = null,
            errorMessage = null,
            conflictInfo = null
        )
        rebuildApiService()
        return true
    }

    fun reportConflict(conflictInfo: ConflictInfo) {
        _status.value = _status.value.copy(
            connectionState = FmsConnectionState.CONFLICT,
            conflictInfo = conflictInfo,
            errorMessage = conflictInfo.message
        )
    }

    fun clearConflict() {
        _status.value = _status.value.copy(
            conflictInfo = null,
            connectionState = if (_status.value.connectionState == FmsConnectionState.CONFLICT) {
                FmsConnectionState.CONNECTED
            } else {
                _status.value.connectionState
            },
            errorMessage = null
        )
    }

    fun dismissError() {
        _status.value = _status.value.copy(errorMessage = null)
    }

    fun handleNetworkException(operation: String, e: Throwable): String {
        val isRender = _status.value.baseUrl.contains("onrender.com")
        val errorMsg: String
        val newState: FmsConnectionState

        when (e) {
            is UnknownHostException -> {
                errorMsg = "Backend unreachable (DNS / Offline). Changes are saved locally on device."
                newState = FmsConnectionState.UNREACHABLE
            }
            is SocketTimeoutException -> {
                if (isRender) {
                    errorMsg = "Render backend is waking up (free tier cold-start). Changes are safely saved locally."
                    newState = FmsConnectionState.WAKING_UP
                } else {
                    errorMsg = "Connection timed out for $operation. Operating in offline mode."
                    newState = FmsConnectionState.UNREACHABLE
                }
            }
            is ConnectException -> {
                errorMsg = "Could not connect to backend server. Operating in offline mode."
                newState = FmsConnectionState.UNREACHABLE
            }
            is SSLException -> {
                errorMsg = "Secure connection to backend failed. Operating in offline mode."
                newState = FmsConnectionState.UNREACHABLE
            }
            else -> {
                errorMsg = e.localizedMessage ?: "Operation failed: $operation. Running locally."
                newState = FmsConnectionState.ERROR
            }
        }

        _status.value = _status.value.copy(
            connectionState = newState,
            errorMessage = errorMsg,
            isWakingUp = (newState == FmsConnectionState.WAKING_UP)
        )
        return errorMsg
    }

    suspend fun <T> executeSafely(
        operationName: String,
        apiCall: suspend (FmsApiService) -> retrofit2.Response<T>
    ): FmsApiResult<T> = withContext(Dispatchers.IO) {
        val service = try {
            getService()
        } catch (e: Exception) {
            val msg = handleNetworkException(operationName, e)
            return@withContext FmsApiResult.Unreachable(msg)
        }

        try {
            val response = apiCall(service)
            if (response.isSuccessful && response.body() != null) {
                _status.value = _status.value.copy(
                    connectionState = FmsConnectionState.CONNECTED,
                    errorMessage = null,
                    isWakingUp = false
                )
                FmsApiResult.Success(response.body()!!)
            } else if (response.isSuccessful && response.body() == null) {
                // For Response<Unit> or empty bodies
                @Suppress("UNCHECKED_CAST")
                FmsApiResult.Success(Unit as T)
            } else {
                val code = response.code()
                val rawError = response.errorBody()?.string()
                val parsedObj = try { JSONObject(rawError ?: "") } catch (_: Exception) { null }
                val parsedMsg = parsedObj?.optString("message")?.ifBlank { null }
                    ?: parsedObj?.optString("error")?.ifBlank { null }

                when (code) {
                    401 -> {
                        setEncryptedToken(null)
                        _status.value = _status.value.copy(
                            isAuthenticated = false,
                            connectionState = FmsConnectionState.SESSION_EXPIRED,
                            errorMessage = "Session expired. Please sign in again."
                        )
                        FmsApiResult.Unauthorized(parsedMsg ?: "Unauthorized. Please sign in.")
                    }
                    403 -> {
                        FmsApiResult.GeneralError(parsedMsg ?: "Access denied (Forbidden).", null)
                    }
                    409 -> {
                        val conflictMsg = parsedMsg ?: "Conflict: Item already exists on the server or version mismatch."
                        val info = ConflictInfo(
                            entityType = operationName,
                            title = "Sync Conflict",
                            message = conflictMsg
                        )
                        reportConflict(info)
                        FmsApiResult.Conflict(conflictMsg, entityType = operationName, rawError = rawError)
                    }
                    422 -> {
                        val fields = mutableMapOf<String, String>()
                        parsedObj?.optJSONObject("fields")?.let { f ->
                            val keys = f.keys()
                            while (keys.hasNext()) {
                                val k = keys.next()
                                fields[k] = f.optString(k)
                            }
                        }
                        val validationMsg = parsedMsg ?: "Validation error: Check your input fields."
                        FmsApiResult.ValidationError(validationMsg, fields)
                    }
                    429 -> {
                        val rateMsg = "Rate limit reached. Please wait a moment."
                        _status.value = _status.value.copy(errorMessage = rateMsg)
                        FmsApiResult.RateLimited(rateMsg)
                    }
                    502, 503, 504 -> {
                        val isRender = _status.value.baseUrl.contains("onrender.com")
                        if (isRender) {
                            _status.value = _status.value.copy(
                                connectionState = FmsConnectionState.WAKING_UP,
                                errorMessage = "Render server waking up from sleep. Changes are saved locally.",
                                isWakingUp = true
                            )
                            FmsApiResult.Unreachable("Render instance is starting up...", isRenderColdStart = true)
                        } else {
                            val msg = "Server temporarily unavailable (HTTP $code)"
                            _status.value = _status.value.copy(
                                connectionState = FmsConnectionState.ERROR,
                                errorMessage = msg
                            )
                            FmsApiResult.ServerError(code, msg)
                        }
                    }
                    else -> {
                        val msg = parsedMsg ?: "HTTP $code: ${response.message()}"
                        _status.value = _status.value.copy(
                            connectionState = FmsConnectionState.ERROR,
                            errorMessage = msg
                        )
                        FmsApiResult.ServerError(code, msg)
                    }
                }
            }
        } catch (e: Exception) {
            val msg = handleNetworkException(operationName, e)
            val isRender = _status.value.baseUrl.contains("onrender.com")
            val isCold = isRender && (e is SocketTimeoutException || _status.value.connectionState == FmsConnectionState.WAKING_UP)
            FmsApiResult.Unreachable(msg, isRenderColdStart = isCold)
        }
    }

    suspend fun testConnection(): Result<String> = withContext(Dispatchers.IO) {
        _status.value = _status.value.copy(connectionState = FmsConnectionState.CHECKING, errorMessage = null)
        try {
            val service = getService()
            var response = try {
                service.checkHealth()
            } catch (e: Exception) {
                null
            }
            if (response == null || !response.isSuccessful) {
                try {
                    val fallback = service.checkApiHealth()
                    if (fallback.isSuccessful) {
                        response = fallback
                    }
                } catch (_: Exception) {}
            }

            if (response != null && response.isSuccessful) {
                val body: HealthCheckDto? = response.body()
                val statusText = body?.displayStatus ?: "OK"
                val msg = "Online ($statusText)${body?.version?.let { " - v$it" } ?: ""}"
                _status.value = _status.value.copy(
                    connectionState = FmsConnectionState.CONNECTED,
                    lastPingMessage = msg,
                    errorMessage = null,
                    isWakingUp = false
                )
                Result.success(msg)
            } else {
                val code = response?.code() ?: 0
                val isRender = _status.value.baseUrl.contains("onrender.com")
                if (isRender && (code in 502..504 || code == 0)) {
                    val msg = "Render instance waking up (cold-start)..."
                    _status.value = _status.value.copy(
                        connectionState = FmsConnectionState.WAKING_UP,
                        errorMessage = msg,
                        isWakingUp = true
                    )
                    Result.failure(Exception(msg))
                } else {
                    val err = if (response != null) "HTTP ${response.code()}: ${response.message()}" else "Service unavailable"
                    _status.value = _status.value.copy(
                        connectionState = FmsConnectionState.ERROR,
                        errorMessage = err
                    )
                    Result.failure(Exception(err))
                }
            }
        } catch (e: Exception) {
            val msg = handleNetworkException("Ping Health Check", e)
            Result.failure(Exception(msg))
        }
    }

    suspend fun executeLogin(email: String, pass: String): Result<UserResponseDto> = withContext(Dispatchers.IO) {
        try {
            val service = getService()
            val response = service.login(LoginDto(email = email.trim(), password = pass))
            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!
                saveAuthSession(body)
                Result.success(body.user ?: UserResponseDto(email = email))
            } else {
                val rawError = response.errorBody()?.string()
                val parsedError = try {
                    JSONObject(rawError ?: "").optString("error", null)
                } catch (_: Exception) {
                    null
                }
                val msg = parsedError ?: "Login failed (HTTP ${response.code()})"
                Result.failure(Exception(msg))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun executeRegister(name: String, email: String, pass: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val service = getService()
            val response = service.register(
                com.example.data.remote.dto.RegisterDto(
                    email = email.trim(),
                    password = pass,
                    name = name.trim().ifBlank { "User" },
                    acceptTerms = true
                )
            )
            if (response.isSuccessful) {
                Result.success("Registration successful! Check your email to verify your account.")
            } else {
                val rawError = response.errorBody()?.string()
                val parsedMsg = try {
                    val obj = JSONObject(rawError ?: "")
                    val err = obj.optString("error")
                    val fields = obj.optJSONObject("fields")
                    val details = mutableListOf<String>()
                    if (fields != null) {
                        val it = fields.keys()
                        while (it.hasNext()) {
                            details.add(fields.optString(it.next()))
                        }
                    }
                    if (details.isNotEmpty()) {
                        details.joinToString("\n")
                    } else err.ifBlank { null }
                } catch (_: Exception) {
                    null
                }
                val msg = parsedMsg ?: "Registration failed (HTTP ${response.code()})"
                Result.failure(Exception(msg))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun executeDemoLogin(): Result<UserResponseDto> = withContext(Dispatchers.IO) {
        try {
            val service = getService()
            val response = service.demoLogin()
            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!
                saveAuthSession(body)
                Result.success(body.user ?: UserResponseDto(email = "demo@fms.internal", firstName = "Demo", lastName = "User"))
            } else {
                Result.failure(Exception("Demo login failed (HTTP ${response.code()})"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun saveAuthSession(loginResponse: LoginResponse) {
        val token = loginResponse.effectiveToken ?: ""
        val user = loginResponse.user
        setEncryptedToken(token)
        prefs.edit()
            .putString(KEY_USER_EMAIL, user?.email)
            .putString(KEY_USER_NAME, user?.displayName)
            .apply()

        _status.value = _status.value.copy(
            isAuthenticated = token.isNotBlank(),
            userEmail = user?.email,
            userName = user?.displayName
        )
        rebuildApiService()
    }

    fun logout() {
        setEncryptedToken(null)
        prefs.edit()
            .remove(KEY_USER_EMAIL)
            .remove(KEY_USER_NAME)
            .apply()

        _status.value = _status.value.copy(
            isAuthenticated = false,
            userEmail = null,
            userName = null
        )
        rebuildApiService()
    }

    private fun ensureTrailingSlash(url: String): String {
        return if (!url.endsWith("/")) "$url/" else url
    }

    companion object {
        const val DEFAULT_BASE_URL = "https://mindforge-backend-m7uz.onrender.com/"
        private const val KEY_BASE_URL = "fms_base_url"
        private const val KEY_AUTH_TOKEN = "fms_auth_token"
        private const val KEY_USER_EMAIL = "fms_user_email"
        private const val KEY_USER_NAME = "fms_user_name"

        @Volatile
        private var INSTANCE: FmsClientManager? = null

        fun getInstance(context: Context): FmsClientManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: FmsClientManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}
