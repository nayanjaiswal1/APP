package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Group
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.local.entity.AccountEntity
import com.example.data.local.entity.GroupEntity
import com.example.data.parser.SampleStatement
import com.example.data.parser.StatementParserEngine
import com.example.data.parser.StatementTransaction
import com.example.ui.theme.PositiveGreen
import com.example.ui.theme.PositiveGreenBg
import com.example.ui.theme.PurpleContainer
import com.example.ui.theme.PurplePrimary
import com.example.ui.theme.PurplePrimaryDark
import com.example.ui.theme.TextMuted
import java.util.Locale

@Composable
fun UploadStatementDialog(
    accounts: List<AccountEntity>,
    groups: List<GroupEntity>,
    selectedAccount: AccountEntity?,
    statementInputText: String,
    isParsing: Boolean,
    parsedTransactions: List<StatementTransaction>,
    onDismiss: () -> Unit,
    onSelectAccount: (AccountEntity) -> Unit,
    onInputChange: (String) -> Unit,
    onParseClick: () -> Unit,
    onSelectSample: (SampleStatement) -> Unit,
    onToggleTransaction: (String) -> Unit,
    onSelectAll: (Boolean) -> Unit,
    onImportExpenses: (targetGroupId: Long?) -> Unit
) {
    var selectedTargetGroupId by remember { mutableStateOf<Long?>(null) }
    val selectedCount = parsedTransactions.count { it.isSelected }
    val totalSelectedAmount = parsedTransactions.filter { it.isSelected }.sumOf { it.amount }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.92f)
                .testTag("upload_statement_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                // Dialog Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(PurpleContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.FileUpload,
                                contentDescription = null,
                                tint = PurplePrimaryDark,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Bank Statement Parser",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = PurplePrimaryDark
                            )
                            Text(
                                text = "Extract card & bank transactions",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextMuted
                            )
                        }
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = TextMuted)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // 1. Account Selector
                    item {
                        Column {
                            Text(
                                text = "1. SELECT ACCOUNT",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = TextMuted
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(accounts) { acc ->
                                    val isSelected = selectedAccount?.id == acc.id
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = if (isSelected) PurplePrimary else MaterialTheme.colorScheme.surfaceVariant,
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(12.dp))
                                            .clickable { onSelectAccount(acc) }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = if (acc.type == "CREDIT_CARD") Icons.Default.CreditCard else Icons.Default.AccountBalance,
                                                contentDescription = null,
                                                tint = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "${acc.name} (..${acc.accountNumberLast4})",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // 2. Sample Statements Presets
                    item {
                        Column {
                            Text(
                                text = "2. LOAD SAMPLE STATEMENT OR PASTE BELOW",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = TextMuted
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(StatementParserEngine.SAMPLE_STATEMENTS) { sample ->
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = MaterialTheme.colorScheme.surface,
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(12.dp))
                                            .clickable { onSelectSample(sample) }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.AutoAwesome,
                                                contentDescription = null,
                                                tint = PurplePrimary,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = sample.title,
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // 3. Raw CSV / Text Input
                    item {
                        OutlinedTextField(
                            value = statementInputText,
                            onValueChange = onInputChange,
                            placeholder = {
                                Text(
                                    "Paste bank CSV, credit card export, or transaction list...",
                                    fontSize = 12.sp,
                                    color = TextMuted.copy(alpha = 0.7f)
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(110.dp)
                                .testTag("statement_input_field"),
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = PurplePrimary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Button(
                            onClick = onParseClick,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("parse_statement_button"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = PurplePrimary),
                            enabled = statementInputText.isNotBlank() && !isParsing
                        ) {
                            if (isParsing) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Parsing Statement...", fontSize = 13.sp)
                            } else {
                                Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Parse & Extract Transactions", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }
                    }

                    // 4. Extracted Transactions Review Table
                    if (parsedTransactions.isNotEmpty()) {
                        item {
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "EXTRACTED EXPENSES (${parsedTransactions.size})",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = PurplePrimaryDark
                                    )

                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Text(
                                            text = "Select All",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = PurplePrimary,
                                            modifier = Modifier.clickable { onSelectAll(true) }
                                        )
                                        Text(
                                            text = "•",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = TextMuted
                                        )
                                        Text(
                                            text = "Clear",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = TextMuted,
                                            modifier = Modifier.clickable { onSelectAll(false) }
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                // Split Target: Personal or Group
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = PurpleContainer.copy(alpha = 0.5f),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(imageVector = Icons.Default.Group, contentDescription = null, tint = PurplePrimaryDark, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Assign to:", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                        }

                                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            item {
                                                val isSelected = selectedTargetGroupId == null
                                                Surface(
                                                    shape = RoundedCornerShape(8.dp),
                                                    color = if (isSelected) PurplePrimary else MaterialTheme.colorScheme.surface,
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(8.dp))
                                                        .clickable { selectedTargetGroupId = null }
                                                ) {
                                                    Text(
                                                        text = "Personal",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                                                        fontWeight = FontWeight.Bold,
                                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                                    )
                                                }
                                            }
                                            items(groups) { g ->
                                                val isSelected = selectedTargetGroupId == g.id
                                                Surface(
                                                    shape = RoundedCornerShape(8.dp),
                                                    color = if (isSelected) PurplePrimary else MaterialTheme.colorScheme.surface,
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(8.dp))
                                                        .clickable { selectedTargetGroupId = g.id }
                                                ) {
                                                    Text(
                                                        text = g.name,
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                                                        fontWeight = FontWeight.Bold,
                                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        items(parsedTransactions) { tx ->
                            Card(
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (tx.isSelected) PurpleContainer.copy(alpha = 0.3f) else MaterialTheme.colorScheme.surface
                                ),
                                border = BorderStroke(1.dp, if (tx.isSelected) PurplePrimary.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surfaceVariant),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onToggleTransaction(tx.id) }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 10.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                        Checkbox(
                                            checked = tx.isSelected,
                                            onCheckedChange = { onToggleTransaction(tx.id) },
                                            colors = CheckboxDefaults.colors(checkedColor = PurplePrimary)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Column {
                                            Text(
                                                text = tx.cleanMerchant,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = tx.dateStr,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = TextMuted
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Surface(
                                                    shape = RoundedCornerShape(6.dp),
                                                    color = MaterialTheme.colorScheme.surfaceVariant
                                                ) {
                                                    Text(
                                                        text = tx.category,
                                                        style = MaterialTheme.typography.labelSmall,
                                                        fontSize = 9.sp,
                                                        color = TextMuted,
                                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = "${tx.currency}${String.format(Locale.US, "%.2f", tx.amount)}",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = if (tx.isDebit) PurplePrimaryDark else PositiveGreen
                                        )
                                        Text(
                                            text = if (tx.isDebit) "Debit" else "Credit",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontSize = 9.sp,
                                            color = TextMuted
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Bottom Import Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text("Cancel", color = TextMuted)
                    }

                    Button(
                        onClick = { onImportExpenses(selectedTargetGroupId) },
                        modifier = Modifier
                            .weight(2f)
                            .testTag("import_statement_expenses_button"),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PurplePrimary),
                        enabled = selectedCount > 0
                    ) {
                        Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Import $selectedCount (${selectedAccount?.currency ?: "$"}${String.format(Locale.US, "%.2f", totalSelectedAmount)})",
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}
