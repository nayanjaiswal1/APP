package com.example.ui.components

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.remote.ConflictInfo
import com.example.data.remote.FmsClientManager
import com.example.data.remote.FmsConnectionState
import com.example.ui.theme.NegativeRed
import com.example.ui.theme.NegativeRedBg
import com.example.ui.theme.PositiveGreen
import com.example.ui.theme.PositiveGreenBg
import com.example.ui.theme.PurpleContainer
import com.example.ui.theme.PurplePrimary
import com.example.ui.theme.PurplePrimaryDark
import com.example.ui.theme.TextMuted
import kotlinx.coroutines.launch

enum class ConflictResolutionAction {
    KEEP_LOCAL,
    OVERWRITE_SERVER,
    USE_SERVER_VERSION
}

/**
 * Universal backend connectivity & conflict status banner.
 * Gracefully informs user of offline mode, Render container waking up,
 * 409 conflicts, 401 expired sessions, and provides 1-tap recovery.
 */
@Composable
fun BackendStatusBanner(
    clientManager: FmsClientManager,
    onOpenSettings: (() -> Unit)? = null,
    onResolveConflict: ((ConflictInfo, ConflictResolutionAction) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val status by clientManager.status.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isChecking by remember { mutableStateOf(false) }
    var showConflictDialog by remember { mutableStateOf(false) }

    // Do not show anything if fully connected and no conflict
    val shouldShow = status.connectionState != FmsConnectionState.CONNECTED &&
            status.connectionState != FmsConnectionState.DISCONNECTED ||
            status.connectionState == FmsConnectionState.CONFLICT

    AnimatedVisibility(
        visible = shouldShow,
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically(),
        modifier = modifier
    ) {
        when (status.connectionState) {
            FmsConnectionState.WAKING_UP -> {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF8E1)),
                    border = BorderStroke(1.dp, Color(0xFFFFB300)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                        .testTag("banner_server_waking_up")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            color = Color(0xFFE65100),
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.5.dp
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Render Server Waking Up",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = Color(0xFFE65100)
                            )
                            Text(
                                text = "Free tier spins up from sleep (~30-50s). Local mode is active — all additions are safe.",
                                fontSize = 11.sp,
                                color = Color(0xFF8D6E63),
                                lineHeight = 15.sp
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        OutlinedButton(
                            onClick = {
                                isChecking = true
                                scope.launch {
                                    val res = clientManager.testConnection()
                                    isChecking = false
                                    if (res.isSuccess) {
                                        Toast.makeText(context, "Server is now online!", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFE65100)),
                            modifier = Modifier.testTag("btn_waking_up_check")
                        ) {
                            Text(if (isChecking) "Checking..." else "Check", fontSize = 11.sp)
                        }
                    }
                }
            }

            FmsConnectionState.UNREACHABLE -> {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5)),
                    border = BorderStroke(1.dp, Color(0xFFBDBDBD)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                        .testTag("banner_server_unreachable")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CloudOff,
                            contentDescription = "Offline Mode",
                            tint = Color(0xFF757575),
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Offline Mode Active",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = Color(0xFF424242)
                            )
                            Text(
                                text = "Backend unreachable. All changes are saved safely to your device's local database.",
                                fontSize = 11.sp,
                                color = Color(0xFF757575),
                                lineHeight = 15.sp
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        OutlinedButton(
                            onClick = {
                                isChecking = true
                                scope.launch {
                                    val res = clientManager.testConnection()
                                    isChecking = false
                                    if (res.isSuccess) {
                                        Toast.makeText(context, "Backend reconnected!", Toast.LENGTH_SHORT).show()
                                    } else {
                                        Toast.makeText(context, "Still offline: ${res.exceptionOrNull()?.message}", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = PurplePrimary),
                            modifier = Modifier.testTag("btn_retry_connection")
                        ) {
                            Text(if (isChecking) "Checking..." else "Retry", fontSize = 11.sp)
                        }
                    }
                }
            }

            FmsConnectionState.CONFLICT -> {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0)),
                    border = BorderStroke(1.dp, Color(0xFFFF9800)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                        .testTag("banner_sync_conflict")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.WarningAmber,
                            contentDescription = "Conflict",
                            tint = Color(0xFFE65100),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Server Conflict Detected",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = Color(0xFFE65100)
                            )
                            Text(
                                text = status.conflictInfo?.message ?: "Resource already exists on server or version mismatch.",
                                fontSize = 11.sp,
                                color = Color(0xFF8D6E63),
                                maxLines = 2
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = { showConflictDialog = true },
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE65100)),
                            modifier = Modifier.testTag("btn_resolve_conflict")
                        ) {
                            Text("Resolve", fontSize = 11.sp, color = Color.White)
                        }
                    }
                }
            }

            FmsConnectionState.SESSION_EXPIRED -> {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = NegativeRedBg),
                    border = BorderStroke(1.dp, NegativeRed.copy(alpha = 0.4f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                        .testTag("banner_session_expired")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Session Expired",
                            tint = NegativeRed,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Cloud Session Expired",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = NegativeRed
                            )
                            Text(
                                text = "Sign in again to continue syncing with the cloud backend.",
                                fontSize = 11.sp,
                                color = TextMuted
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = { onOpenSettings?.invoke() },
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = PurplePrimary),
                            modifier = Modifier.testTag("btn_session_sign_in")
                        ) {
                            Text("Sign In", fontSize = 11.sp, color = Color.White)
                        }
                    }
                }
            }

            FmsConnectionState.ERROR -> {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = NegativeRedBg),
                    border = BorderStroke(1.dp, NegativeRed.copy(alpha = 0.3f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                        .testTag("banner_backend_error")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Error",
                            tint = NegativeRed,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Connection Issue",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = NegativeRed
                            )
                            Text(
                                text = status.errorMessage ?: "Failed to reach server. Using offline data.",
                                fontSize = 11.sp,
                                color = TextMuted,
                                maxLines = 2
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        IconButton(
                            onClick = { clientManager.dismissError() },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Text("✕", fontSize = 14.sp, color = TextMuted)
                        }
                    }
                }
            }

            else -> {}
        }
    }

    // Conflict Resolution Dialog
    if (showConflictDialog && status.conflictInfo != null) {
        val conflict = status.conflictInfo!!
        Dialog(onDismissRequest = { showConflictDialog = false }) {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .testTag("dialog_conflict_resolution")
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFFFFF3E0),
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.WarningAmber,
                                    contentDescription = null,
                                    tint = Color(0xFFE65100),
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Resolve Sync Conflict",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = conflict.entityType,
                                style = MaterialTheme.typography.bodySmall,
                                color = TextMuted
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "The server rejected this update because a matching or conflicting record already exists:",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = conflict.message,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFFD84315),
                            modifier = Modifier.padding(12.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    Text(
                        text = "Choose how to resolve:",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Option 1: Keep Local
                    OutlinedButton(
                        onClick = {
                            showConflictDialog = false
                            clientManager.clearConflict()
                            onResolveConflict?.invoke(conflict, ConflictResolutionAction.KEEP_LOCAL)
                            Toast.makeText(context, "Retained local version.", Toast.LENGTH_SHORT).show()
                        },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.Start
                        ) {
                            Text("Keep Local Version", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text("Continue with device changes without modifying the server", fontSize = 11.sp, color = TextMuted)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Option 2: Overwrite Server
                    Button(
                        onClick = {
                            showConflictDialog = false
                            clientManager.clearConflict()
                            onResolveConflict?.invoke(conflict, ConflictResolutionAction.OVERWRITE_SERVER)
                            Toast.makeText(context, "Overwriting server with local data...", Toast.LENGTH_SHORT).show()
                        },
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PurplePrimary),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.Start
                        ) {
                            Text("Force Overwrite Server", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color.White)
                            Text("Replace the conflicting cloud copy with your local version", fontSize = 11.sp, color = Color.White.copy(alpha = 0.8f))
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Option 3: Use Server Version
                    OutlinedButton(
                        onClick = {
                            showConflictDialog = false
                            clientManager.clearConflict()
                            onResolveConflict?.invoke(conflict, ConflictResolutionAction.USE_SERVER_VERSION)
                            Toast.makeText(context, "Syncing with cloud version...", Toast.LENGTH_SHORT).show()
                        },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.Start
                        ) {
                            Text("Use Server Version", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text("Discard local edit and reload latest version from cloud", fontSize = 11.sp, color = TextMuted)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        OutlinedButton(
                            onClick = {
                                showConflictDialog = false
                                clientManager.clearConflict()
                            },
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Dismiss")
                        }
                    }
                }
            }
        }
    }
}
