package com.example.data.remote

import android.content.Context
import android.content.SharedPreferences
import com.example.data.remote.dto.HealthCheckDto
import com.example.data.remote.dto.LoginDto
import com.example.data.remote.dto.LoginResponse
import com.example.data.remote.dto.UserResponseDto
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor
import org.json.JSONObject
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

enum class FmsConnectionState {
    DISCONNECTED,
    CHECKING,
    CONNECTED,
    ERROR
}

data class FmsBackendStatus(
    val connectionState: FmsConnectionState = FmsConnectionState.DISCONNECTED,
    val baseUrl: String = "https://mindforge-backend-m7uz.onrender.com/",
    val isAuthenticated: Boolean = false,
    val userEmail: String? = null,
    val userName: String? = null,
    val lastPingMessage: String? = null,
    val errorMessage: String? = null
)

class FmsClientManager(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("fms_backend_prefs", Context.MODE_PRIVATE)

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
            isAuthenticated = !prefs.getString(KEY_AUTH_TOKEN, "").isNullOrBlank(),
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

        val authInterceptor = Interceptor { chain ->
            val token = prefs.getString(KEY_AUTH_TOKEN, null)
            val requestBuilder = chain.request().newBuilder()
            if (!token.isNullOrBlank()) {
                requestBuilder.addHeader("Authorization", "Bearer $token")
            }
            requestBuilder.addHeader("Accept", "application/json")
            chain.proceed(requestBuilder.build())
        }

        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(logging)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .writeTimeout(20, TimeUnit.SECONDS)
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

    fun updateBaseUrl(newUrl: String) {
        val normalized = ensureTrailingSlash(newUrl.trim())
        prefs.edit().putString(KEY_BASE_URL, normalized).apply()
        _status.value = _status.value.copy(baseUrl = normalized, connectionState = FmsConnectionState.DISCONNECTED, lastPingMessage = null)
        rebuildApiService()
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
                    errorMessage = null
                )
                Result.success(msg)
            } else {
                val err = if (response != null) "HTTP ${response.code()}: ${response.message()}" else "Service unavailable"
                _status.value = _status.value.copy(
                    connectionState = FmsConnectionState.ERROR,
                    errorMessage = err
                )
                Result.failure(Exception(err))
            }
        } catch (e: Exception) {
            val err = e.localizedMessage ?: "Connection failed"
            _status.value = _status.value.copy(
                connectionState = FmsConnectionState.ERROR,
                errorMessage = err
            )
            Result.failure(e)
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
        prefs.edit()
            .putString(KEY_AUTH_TOKEN, token)
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
        prefs.edit()
            .remove(KEY_AUTH_TOKEN)
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
