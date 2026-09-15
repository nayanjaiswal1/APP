package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.GroupEntity
import com.example.data.local.entity.UserEntity
import com.example.ui.theme.PurpleContainer
import com.example.ui.theme.PurplePrimary
import com.example.ui.theme.PurplePrimaryDark
import com.example.ui.theme.TextMuted
import com.example.ui.viewmodel.SettlePrefill
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettleUpBottomSheet(
    users: List<UserEntity>,
    groups: List<GroupEntity>,
    currentUserId: Long,
    prefill: SettlePrefill? = null,
    onDismiss: () -> Unit,
    onConfirmSettle: (fromId: Long, toId: Long, amount: Double, groupId: Long?, note: String) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var fromUserId by remember { mutableStateOf(prefill?.fromUserId ?: currentUserId) }
    var toUserId by remember {
        mutableStateOf(
            prefill?.toUserId ?: (users.find { it.id != currentUserId }?.id ?: 2L)
        )
    }
    var amountStr by remember {
        mutableStateOf(
            if (prefill != null && prefill.suggestedAmount > 0) String.format(Locale.US, "%.2f", prefill.suggestedAmount) else ""
        )
    }
    var selectedGroupId by remember { mutableStateOf(prefill?.groupId) }
    var paymentMethod by remember { mutableStateOf("Cash") }
    var note by remember { mutableStateOf("Settlement payment") }

    val amount = amountStr.toDoubleOrNull() ?: 0.0

    val fromUser = users.find { it.id == fromUserId }
    val toUser = users.find { it.id == toUserId }
    val fromName = if (fromUser?.id == currentUserId) "You" else (fromUser?.name ?: "User")
    val toName = if (toUser?.id == currentUserId) "You" else (toUser?.name ?: "User")

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        modifier = Modifier.testTag("settle_up_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = PurplePrimary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Record a Settlement",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }

                IconButton(onClick = onDismiss) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // From -> To Transfer Pill
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Payer (From)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        AvatarCircle(name = fromName, colorHex = fromUser?.avatarColorHex ?: "#6750A4", size = 38.dp)
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(text = "Paid by", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                            Text(text = fromName, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        }
                    }

                    // Swap Button
                    IconButton(
                        onClick = {
                            val temp = fromUserId
                            fromUserId = toUserId
                            toUserId = temp
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.SwapHoriz,
                            contentDescription = "Swap payer and payee",
                            tint = PurplePrimary
                        )
                    }

                    // Recipient (To)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(horizontalAlignment = Alignment.End) {
                            Text(text = "Paid to", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                            Text(text = toName, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        AvatarCircle(name = toName, colorHex = toUser?.avatarColorHex ?: "#7D5260", size = 38.dp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Amount Input
            OutlinedTextField(
                value = amountStr,
                onValueChange = { amountStr = it },
                label = { Text("Settlement Amount ($)") },
                placeholder = { Text("0.00") },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("settle_amount_input"),
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PurplePrimary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Payment Methods
            Text(
                text = "Payment Method",
                style = MaterialTheme.typography.labelMedium,
                color = TextMuted
            )
            Spacer(modifier = Modifier.height(6.dp))

            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val methods = listOf("Cash", "UPI / Bank", "PayPal", "Venmo", "Apple Pay", "Other")
                items(methods) { m ->
                    val isSelected = paymentMethod == m
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) PurplePrimary else MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                paymentMethod = m
                                note = "Settlement via $m"
                            }
                    ) {
                        Text(
                            text = m,
                            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Note
            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text("Note") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PurplePrimary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Settle Button
            Button(
                onClick = {
                    if (amount > 0 && fromUserId != toUserId) {
                        onConfirmSettle(fromUserId, toUserId, amount, selectedGroupId, note)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("confirm_settle_button"),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PurplePrimary),
                enabled = amount > 0 && fromUserId != toUserId
            ) {
                Text(
                    text = "Record $${String.format(Locale.US, "%.2f", amount)} Payment",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }
        }
    }
}

