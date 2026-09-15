package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Wallet
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.AccountEntity
import com.example.data.local.entity.ExpenseEntity
import com.example.data.local.entity.StatementEntity
import com.example.ui.components.CategoryIcon
import com.example.ui.theme.PositiveGreen
import com.example.ui.theme.PositiveGreenBg
import com.example.ui.theme.PurpleContainer
import com.example.ui.theme.PurplePrimary
import com.example.ui.theme.PurplePrimaryDark
import com.example.ui.theme.PurpleSurfaceVariant
import com.example.ui.theme.TextMuted
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun AccountsScreen(
    accounts: List<AccountEntity>,
    statements: List<StatementEntity>,
    expenses: List<ExpenseEntity>,
    selectedAccountId: Long?,
    onSelectAccount: (Long?) -> Unit,
    onAddAccountClick: () -> Unit,
    onUploadStatementClick: (AccountEntity?) -> Unit,
    onDeleteAccount: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val totalBalance = accounts.sumOf { it.balance }
    val totalStatementExpenses = statements.sumOf { it.totalExpensesAmount }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .testTag("accounts_screen"),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Top Overview Hero Card
        item {
            Spacer(modifier = Modifier.height(4.dp))
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("accounts_overview_card"),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "TOTAL COMBINED BALANCE",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = TextMuted
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "$${String.format(Locale.US, "%,.2f", totalBalance)}",
                                style = MaterialTheme.typography.headlineLarge,
                                fontWeight = FontWeight.Bold,
                                color = PurplePrimaryDark
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = PurpleContainer
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AccountBalance,
                                    contentDescription = null,
                                    tint = PurplePrimaryDark,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "${accounts.size} Accounts",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = PurplePrimaryDark
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Quick Actions
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = { onUploadStatementClick(null) },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("upload_statement_banner_button"),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = PurplePrimary)
                        ) {
                            Icon(imageVector = Icons.Default.FileUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Parse Statement", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }

                        OutlinedButton(
                            onClick = onAddAccountClick,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("add_account_banner_button"),
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, PurplePrimary)
                        ) {
                            Icon(imageVector = Icons.Default.Add, contentDescription = null, tint = PurplePrimary, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Add Account", fontWeight = FontWeight.Bold, color = PurplePrimary, fontSize = 13.sp)
                        }
                    }
                }
            }
        }

        // Section: Linked Financial Accounts
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Financial Accounts",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${accounts.size} Active",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted
                )
            }
        }

        // Horizontal Card Deck
        item {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(accounts) { acc ->
                    val isSelected = selectedAccountId == acc.id
                    val cardColor = try {
                        Color(android.graphics.Color.parseColor(acc.colorHex))
                    } catch (_: Exception) {
                        PurplePrimaryDark
                    }

                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = cardColor),
                        border = if (isSelected) BorderStroke(2.dp, PurplePrimary) else null,
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        modifier = Modifier
                            .width(220.dp)
                            .height(130.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .clickable {
                                onSelectAccount(if (isSelected) null else acc.id)
                            }
                            .testTag("account_card_${acc.id}")
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(14.dp),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = when (acc.type) {
                                            "CREDIT_CARD" -> Icons.Default.CreditCard
                                            "WALLET" -> Icons.Default.Wallet
                                            else -> Icons.Default.AccountBalance
                                        },
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = acc.institution,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color.White.copy(alpha = 0.85f)
                                    )
                                }

                                Text(
                                    text = "•••• ${acc.accountNumberLast4}",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White.copy(alpha = 0.7f)
                                )
                            }

                            Column {
                                Text(
                                    text = acc.name,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    maxLines = 1
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "${acc.currency}${String.format(Locale.US, "%,.2f", acc.balance)}",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            }
        }

        // Section: Ingested Statements
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Parsed Statements",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${statements.size} Uploaded",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted
                )
            }
        }

        if (statements.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(imageVector = Icons.Default.Description, contentDescription = null, tint = TextMuted, modifier = Modifier.size(32.dp))
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "No statements uploaded yet",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Upload bank CSV or card statements to auto-parse expenses",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMuted
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Button(
                            onClick = { onUploadStatementClick(null) },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = PurplePrimary)
                        ) {
                            Text("Parse First Statement", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        } else {
            items(statements) { st ->
                val dateFormat = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
                val dateStr = dateFormat.format(Date(st.uploadedAt))
                val matchedAccount = accounts.find { it.id == st.accountId }

                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
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
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(PurpleContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(imageVector = Icons.Default.Description, contentDescription = null, tint = PurplePrimaryDark, modifier = Modifier.size(20.dp))
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = st.fileName,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "${matchedAccount?.name ?: "Account"} • $dateStr",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextMuted
                                )
                            }
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "$${String.format(Locale.US, "%.2f", st.totalExpensesAmount)}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = PurplePrimaryDark
                            )
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = PositiveGreenBg
                            ) {
                                Text(
                                    text = "${st.parsedTransactionsCount} Txns Ingested",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = PositiveGreen,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Section: Expenses Associated with Account
        val accountExpenses = if (selectedAccountId != null) {
            expenses.filter { it.accountId == selectedAccountId }
        } else {
            expenses.filter { it.accountId != null }
        }

        if (accountExpenses.isNotEmpty()) {
            item {
                Text(
                    text = if (selectedAccountId != null) "Account Transactions (${accountExpenses.size})" else "All Statement Transactions (${accountExpenses.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            items(accountExpenses) { exp ->
                val dateFormat = SimpleDateFormat("MMM d, h:mm a", Locale.getDefault())
                val timeStr = dateFormat.format(Date(exp.dateMillis))

                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            CategoryIcon(category = exp.category, size = 36.dp)
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(text = exp.title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                Text(text = timeStr, style = MaterialTheme.typography.labelSmall, color = TextMuted)
                            }
                        }

                        Text(
                            text = "${exp.currency}${String.format(Locale.US, "%.2f", exp.amount)}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = PurplePrimaryDark
                        )
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}
