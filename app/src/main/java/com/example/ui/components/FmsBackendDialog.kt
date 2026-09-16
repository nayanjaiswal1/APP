package com.example.ui.components

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Login
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.AppDatabase
import com.example.data.remote.FmsClientManager
import com.example.data.remote.FmsConnectionState
import com.example.data.remote.FmsSyncManager
import com.example.data.remote.dto.NaturalLanguageQueryDto
import com.example.ui.theme.NegativeRed
import com.example.ui.theme.NegativeRedBg
import com.example.ui.theme.PositiveGreen
import com.example.ui.theme.PositiveGreenBg
import com.example.ui.theme.PurpleContainer
import com.example.ui.theme.PurplePrimary
import com.example.ui.theme.PurplePrimaryDark
import com.example.ui.theme.TextMuted
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FmsBackendDialog(
    clientManager: FmsClientManager,
    syncManager: FmsSyncManager,
    database: AppDatabase,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val status by clientManager.status.collectAsState()

    var inputUrl by remember(status.baseUrl) { mutableStateOf(status.baseUrl) }
    var loginEmail by remember { mutableStateOf("") }
    var loginPassword by remember { mutableStateOf("") }
    var registerName by remember { mutableStateOf("") }
    var isRegisterMode by remember { mutableStateOf(false) }
    var showLoginForm by remember { mutableStateOf(false) }

    var isTestingConnection by remember { mutableStateOf(false) }
    var isLoggingIn by remember { mutableStateOf(false) }
    var isSyncing by remember { mutableStateOf(false) }
    var syncFeedbackMessage by remember { mutableStateOf<String?>(null) }

    // AI Query in-dialog test
    var aiQueryText by remember { mutableStateOf("") }
    var aiQueryResponse by remember { mutableStateOf<String?>(null) }
    var isAiQuerying by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .verticalScroll(rememberScrollState())
                .testTag("fms_backend_bottom_sheet")
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(
                                when (status.connectionState) {
                                    FmsConnectionState.CONNECTED -> PositiveGreenBg
                                    FmsConnectionState.ERROR -> NegativeRedBg
                                    else -> PurpleContainer
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = when (status.connectionState) {
                                FmsConnectionState.CONNECTED -> Icons.Default.CloudDone
                                FmsConnectionState.ERROR -> Icons.Default.CloudOff
                                else -> Icons.Default.Cloud
                            },
                            contentDescription = null,
                            tint = when (status.connectionState) {
                                FmsConnectionState.CONNECTED -> PositiveGreen
                                FmsConnectionState.ERROR -> NegativeRed
                                else -> PurplePrimary
                            },
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = "FMS Backend Integration",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Connect to AI-based Finance Management System",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMuted
                        )
                    }
                }

                IconButton(onClick = onDismiss) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 1. Connection Status Banner
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(
                    containerColor = when (status.connectionState) {
                        FmsConnectionState.CONNECTED -> PositiveGreenBg
                        FmsConnectionState.WAKING_UP -> com.example.ui.theme.WarningAmberBg
                        FmsConnectionState.CONFLICT -> com.example.ui.theme.WarningAmberBg
                        FmsConnectionState.ERROR, FmsConnectionState.UNREACHABLE, FmsConnectionState.SESSION_EXPIRED -> NegativeRedBg
                        else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    }
                ),
                border = BorderStroke(
                    1.dp,
                    when (status.connectionState) {
                        FmsConnectionState.CONNECTED -> PositiveGreen.copy(alpha = 0.4f)
                        FmsConnectionState.WAKING_UP, FmsConnectionState.CONFLICT -> com.example.ui.theme.WarningAmber.copy(alpha = 0.4f)
                        FmsConnectionState.ERROR, FmsConnectionState.UNREACHABLE, FmsConnectionState.SESSION_EXPIRED -> NegativeRed.copy(alpha = 0.4f)
                        else -> MaterialTheme.colorScheme.surfaceVariant
                    }
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = when (status.connectionState) {
                                FmsConnectionState.CONNECTED -> "Backend Connected"
                                FmsConnectionState.CHECKING -> "Checking Status..."
                                FmsConnectionState.WAKING_UP -> "⚡ Cloud Server Waking Up (~30s)"
                                FmsConnectionState.CONFLICT -> "⚠️ Data Conflict Detected (409)"
                                FmsConnectionState.UNREACHABLE -> "Cloud Server Unreachable (Offline)"
                                FmsConnectionState.SESSION_EXPIRED -> "🔒 Session Expired (401)"
                                FmsConnectionState.ERROR -> "Connection Failed"
                                FmsConnectionState.DISCONNECTED -> "Offline / Not Connected"
                            },
                            fontWeight = FontWeight.Bold,
                            color = when (status.connectionState) {
                                FmsConnectionState.CONNECTED -> PositiveGreen
                                FmsConnectionState.WAKING_UP, FmsConnectionState.CONFLICT -> com.example.ui.theme.WarningAmber
                                FmsConnectionState.ERROR, FmsConnectionState.UNREACHABLE, FmsConnectionState.SESSION_EXPIRED -> NegativeRed
                                else -> MaterialTheme.colorScheme.onSurface
                            },
                            fontSize = 14.sp
                        )

                        Text(
                            text = status.lastPingMessage ?: status.errorMessage ?: "Configure host URL below and test connection",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMuted,
                            maxLines = 2
                        )
                    }

                    Button(
                        onClick = {
                            isTestingConnection = true
                            scope.launch {
                                val res = clientManager.testConnection()
                                isTestingConnection = false
                                if (res.isSuccess) {
                                    Toast.makeText(context, "Connected: ${res.getOrNull()}", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "Error: ${res.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
                                }
                            }
                        },
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PurplePrimary),
                        enabled = !isTestingConnection,
                        modifier = Modifier.testTag("test_fms_connection_button")
                    ) {
                        if (isTestingConnection) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(imageVector = Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Ping / Health", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 2. Base URL Configuration
            Text(
                text = "Backend Server URL",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(6.dp))

            OutlinedTextField(
                value = inputUrl,
                onValueChange = { inputUrl = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("fms_base_url_input"),
                placeholder = { Text("http://10.0.2.2:3000/") },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                trailingIcon = {
                    if (inputUrl != status.baseUrl) {
                        Button(
                            onClick = {
                                val success = clientManager.updateBaseUrl(inputUrl)
                                if (success) {
                                    Toast.makeText(context, "Base URL updated securely", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "Invalid or insecure URL. Remote hosts must use HTTPS.", Toast.LENGTH_LONG).show()
                                }
                            },
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = PurplePrimary),
                            modifier = Modifier.padding(end = 4.dp)
                        ) {
                            Text("Save", fontSize = 11.sp)
                        }
                    }
                }
            )

            Spacer(modifier = Modifier.height(6.dp))

            // URL Presets Chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = inputUrl == "https://mindforge-backend-m7uz.onrender.com/",
                    onClick = {
                        inputUrl = "https://mindforge-backend-m7uz.onrender.com/"
                        clientManager.updateBaseUrl(inputUrl)
                    },
                    label = { Text("Render Cloud (Live)", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = PurpleContainer)
                )

                FilterChip(
                    selected = inputUrl == "http://10.0.2.2:3000/",
                    onClick = {
                        inputUrl = "http://10.0.2.2:3000/"
                        clientManager.updateBaseUrl(inputUrl)
                    },
                    label = { Text("Emulator", fontSize = 11.sp) },
                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = PurpleContainer)
                )

                FilterChip(
                    selected = inputUrl == "http://localhost:3000/",
                    onClick = {
                        inputUrl = "http://localhost:3000/"
                        clientManager.updateBaseUrl(inputUrl)
                    },
                    label = { Text("Localhost", fontSize = 11.sp) },
                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = PurpleContainer)
                )
            }

            Spacer(modifier = Modifier.height(18.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(14.dp))

            // 3. Authentication & User Session
            Text(
                text = "Authentication (JWT)",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(6.dp))

            if (status.isAuthenticated) {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = PurpleContainer.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.Person, contentDescription = null, tint = PurplePrimary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = status.userName ?: "Authenticated User",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                                Text(
                                    text = status.userEmail ?: "Session Active",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextMuted
                                )
                            }
                        }

                        OutlinedButton(
                            onClick = {
                                clientManager.logout()
                                Toast.makeText(context, "Logged out", Toast.LENGTH_SHORT).show()
                            },
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, NegativeRed)
                        ) {
                            Text("Logout", color = NegativeRed, fontSize = 12.sp)
                        }
                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Demo Login Button (Calls /api/auth/demo-login directly from OpenAPI schema)
                    Button(
                        onClick = {
                            isLoggingIn = true
                            scope.launch {
                                val res = clientManager.executeDemoLogin()
                                isLoggingIn = false
                                if (res.isSuccess) {
                                    Toast.makeText(context, "Demo Login successful!", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "Demo login failed: ${res.exceptionOrNull()?.message}", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PurplePrimary),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("fms_demo_login_button"),
                        enabled = !isLoggingIn
                    ) {
                        if (isLoggingIn) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(imageVector = Icons.Default.Key, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Demo Login", fontWeight = FontWeight.Bold)
                        }
                    }

                    // Toggle custom login form
                    OutlinedButton(
                        onClick = { showLoginForm = !showLoginForm },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(imageVector = Icons.Default.Login, contentDescription = null, tint = PurplePrimary, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (showLoginForm) "Hide Form" else "User Login", color = PurplePrimaryDark)
                    }
                }

                if (showLoginForm) {
                    Spacer(modifier = Modifier.height(10.dp))
                    
                    // Tab toggle between Sign In and Register
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = !isRegisterMode,
                            onClick = { isRegisterMode = false },
                            label = { Text("Sign In", fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = PurpleContainer)
                        )
                        FilterChip(
                            selected = isRegisterMode,
                            onClick = { isRegisterMode = true },
                            label = { Text("Register New Account", fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = PurpleContainer)
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    if (isRegisterMode) {
                        OutlinedTextField(
                            value = registerName,
                            onValueChange = { registerName = it },
                            label = { Text("Your Full Name") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                    }

                    OutlinedTextField(
                        value = loginEmail,
                        onValueChange = { loginEmail = it },
                        label = { Text("Email") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = loginPassword,
                        onValueChange = { loginPassword = it },
                        label = { Text("Password (min 8 chars)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = {
                            isLoggingIn = true
                            scope.launch {
                                if (isRegisterMode) {
                                    val res = clientManager.executeRegister(registerName, loginEmail, loginPassword)
                                    isLoggingIn = false
                                    if (res.isSuccess) {
                                        Toast.makeText(context, res.getOrNull() ?: "Registered successfully", Toast.LENGTH_LONG).show()
                                        isRegisterMode = false
                                    } else {
                                        Toast.makeText(context, "Registration failed: ${res.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
                                    }
                                } else {
                                    val res = clientManager.executeLogin(loginEmail, loginPassword)
                                    isLoggingIn = false
                                    if (res.isSuccess) {
                                        Toast.makeText(context, "Logged in as ${res.getOrNull()?.email}", Toast.LENGTH_SHORT).show()
                                    } else {
                                        Toast.makeText(context, "Login failed: ${res.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
                                    }
                                }
                            }
                        },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = PurplePrimary),
                        enabled = !isLoggingIn
                    ) {
                        if (isLoggingIn) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        } else {
                            Text(if (isRegisterMode) "Create Account" else "Sign In with Email")
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(14.dp))

            // 4. Two-Way Cloud Synchronization
            Text(
                text = "Database Synchronization",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(6.dp))

            Button(
                onClick = {
                    isSyncing = true
                    syncFeedbackMessage = null
                    scope.launch {
                        val result = syncManager.syncAll(database)
                        isSyncing = false
                        syncFeedbackMessage = result.message
                        Toast.makeText(context, if (result.isSuccess) "Sync finished!" else "Sync error", Toast.LENGTH_SHORT).show()
                    }
                },
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PurplePrimary),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("fms_sync_all_button"),
                enabled = !isSyncing
            ) {
                if (isSyncing) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Syncing Accounts, Groups & Transactions...", fontWeight = FontWeight.Bold)
                } else {
                    Icon(imageVector = Icons.Default.CloudSync, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Sync Data with FMS Server", fontWeight = FontWeight.Bold)
                }
            }

            syncFeedbackMessage?.let { msg ->
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = msg,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (msg.contains("failed", ignoreCase = true)) NegativeRed else PositiveGreen
                )
            }

            Spacer(modifier = Modifier.height(18.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(14.dp))

            // 5. AI Query Test (/api/ai/query)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null, tint = PurplePrimary, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "FMS AI Assistant (/api/ai/query)",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = aiQueryText,
                    onValueChange = { aiQueryText = it },
                    placeholder = { Text("e.g. How much did I spend this month?") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp)
                )

                Button(
                    onClick = {
                        if (aiQueryText.isNotBlank()) {
                            isAiQuerying = true
                            aiQueryResponse = null
                            scope.launch {
                                try {
                                    val res = withContext(Dispatchers.IO) {
                                        clientManager.getService().queryFinancialAi(NaturalLanguageQueryDto(aiQueryText))
                                    }
                                    isAiQuerying = false
                                    if (res.isSuccessful && res.body() != null) {
                                        aiQueryResponse = res.body()?.answer ?: res.body()?.response ?: "Received response from AI"
                                    } else {
                                        aiQueryResponse = "Request returned code: ${res.code()}"
                                    }
                                } catch (e: Exception) {
                                    isAiQuerying = false
                                    aiQueryResponse = "Error: ${e.localizedMessage ?: "Failed"}"
                                }
                            }
                        }
                    },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PurplePrimary),
                    enabled = !isAiQuerying && aiQueryText.isNotBlank()
                ) {
                    if (isAiQuerying) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    } else {
                        Icon(imageVector = Icons.Default.Send, contentDescription = "Send", modifier = Modifier.size(16.dp))
                    }
                }
            }

            aiQueryResponse?.let { resp ->
                Spacer(modifier = Modifier.height(8.dp))
                Card(
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = PurpleContainer.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = resp,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(10.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
