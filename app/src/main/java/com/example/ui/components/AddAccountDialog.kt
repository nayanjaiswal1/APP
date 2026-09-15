package com.example.ui.components

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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.window.Dialog
import com.example.ui.theme.PurplePrimary
import com.example.ui.theme.PurplePrimaryDark
import com.example.ui.theme.TextMuted

data class AccountTypeOption(
    val type: String,
    val label: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
)

val ACCOUNT_TYPES = listOf(
    AccountTypeOption("CREDIT_CARD", "Credit Card", Icons.Default.CreditCard),
    AccountTypeOption("CHECKING", "Checking", Icons.Default.AccountBalance),
    AccountTypeOption("SAVINGS", "Savings", Icons.Default.AccountBalance),
    AccountTypeOption("WALLET", "Digital Wallet", Icons.Default.AccountBalanceWallet),
    AccountTypeOption("CASH", "Cash", Icons.Default.AttachMoney)
)

val ACCOUNT_COLORS = listOf(
    "#21005D", "#6750A4", "#7D5260", "#00B77D", "#42A5F5", "#FF7043", "#3F51B5"
)

@Composable
fun AddAccountDialog(
    onDismiss: () -> Unit,
    onAddAccount: (name: String, type: String, institution: String, last4: String, balance: Double, currency: String, colorHex: String, iconName: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf("CREDIT_CARD") }
    var institution by remember { mutableStateOf("") }
    var last4 by remember { mutableStateOf("") }
    var balanceText by remember { mutableStateOf("") }
    var selectedColor by remember { mutableStateOf(ACCOUNT_COLORS[0]) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("add_account_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Add Financial Account",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Track cards, banks & import statements",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMuted
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = TextMuted)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Account Type Selector
                Text(
                    text = "ACCOUNT TYPE",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = TextMuted
                )
                Spacer(modifier = Modifier.height(8.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(ACCOUNT_TYPES) { opt ->
                        val isSelected = selectedType == opt.type
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) PurplePrimary else MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { selectedType = opt.type }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = opt.icon,
                                    contentDescription = null,
                                    tint = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = opt.label,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Account Name
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Account Nickname") },
                    placeholder = { Text("e.g. Chase Sapphire Preferred") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("account_name_input"),
                    shape = RoundedCornerShape(14.dp),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PurplePrimary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Institution & Last 4
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = institution,
                        onValueChange = { institution = it },
                        label = { Text("Bank / Institution") },
                        placeholder = { Text("e.g. Chase") },
                        modifier = Modifier
                            .weight(1.5f)
                            .testTag("account_institution_input"),
                        shape = RoundedCornerShape(14.dp),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PurplePrimary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    )

                    OutlinedTextField(
                        value = last4,
                        onValueChange = { if (it.length <= 4) last4 = it },
                        label = { Text("Last 4") },
                        placeholder = { Text("4120") },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("account_last4_input"),
                        shape = RoundedCornerShape(14.dp),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PurplePrimary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Starting Balance
                OutlinedTextField(
                    value = balanceText,
                    onValueChange = { balanceText = it },
                    label = { Text("Current Balance ($)") },
                    placeholder = { Text("0.00") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("account_balance_input"),
                    shape = RoundedCornerShape(14.dp),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PurplePrimary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Color Theme
                Text(
                    text = "CARD COLOR",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = TextMuted
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ACCOUNT_COLORS.forEach { colorHex ->
                        val color = try {
                            Color(android.graphics.Color.parseColor(colorHex))
                        } catch (_: Exception) {
                            PurplePrimary
                        }
                        val isSelected = selectedColor == colorHex

                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(color)
                                .clickable { selectedColor = colorHex },
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSelected) {
                                Box(
                                    modifier = Modifier
                                        .size(12.dp)
                                        .clip(CircleShape)
                                        .background(Color.White)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text("Cancel", color = TextMuted)
                    }

                    Button(
                        onClick = {
                            val bal = balanceText.toDoubleOrNull() ?: 0.0
                            val finalName = name.ifBlank { "${institution.ifBlank { "Account" }} ($selectedType)" }
                            onAddAccount(
                                finalName,
                                selectedType,
                                institution.ifBlank { "Bank" },
                                last4.ifBlank { "0000" },
                                bal,
                                "$",
                                selectedColor,
                                "credit_card"
                            )
                        },
                        modifier = Modifier
                            .weight(1.5f)
                            .testTag("save_account_button"),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PurplePrimary),
                        enabled = name.isNotBlank() || institution.isNotBlank()
                    ) {
                        Text("Save Account", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }
    }
}
