package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.ExpenseEntity
import com.example.data.local.entity.GroupEntity
import com.example.data.local.entity.UserEntity
import com.example.domain.model.UserBalanceSummary
import com.example.ui.components.AvatarCircle
import com.example.ui.components.BalanceSummaryCard
import com.example.ui.components.ExpenseItemCard
import com.example.ui.components.ExportReportBottomSheet
import com.example.ui.theme.NegativeRed
import com.example.ui.theme.NegativeRedBg
import com.example.ui.theme.PositiveGreen
import com.example.ui.theme.PositiveGreenBg
import com.example.ui.theme.PurpleContainer
import com.example.ui.theme.PurplePrimary
import com.example.ui.theme.PurplePrimaryDark
import com.example.ui.theme.SettledGray
import com.example.ui.theme.TextDark
import com.example.ui.theme.TextMuted
import com.example.ui.viewmodel.SettlePrefill
import java.util.Locale
import kotlin.math.abs

@Composable
fun HomeScreen(
    users: List<UserEntity>,
    groups: List<GroupEntity>,
    expenses: List<ExpenseEntity>,
    currentUserId: Long,
    netBalance: Double,
    totalYouAreOwed: Double,
    totalYouOwe: Double,
    friendSummaries: List<UserBalanceSummary>,
    onAddExpenseClick: () -> Unit,
    onVoiceExpenseClick: () -> Unit = {},
    onParseMessageClick: () -> Unit,
    onSettleUpClick: (SettlePrefill?) -> Unit,
    onSimplifyDebtsClick: () -> Unit,
    onAddFriendClick: () -> Unit,
    onDeleteExpense: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val groupMap = groups.associateBy { it.id }
    var showExportDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .testTag("home_screen_content"),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Balance Summary Hero Card
        item {
            Spacer(modifier = Modifier.height(4.dp))
            BalanceSummaryCard(
                netBalance = netBalance,
                totalYouAreOwed = totalYouAreOwed,
                totalYouOwe = totalYouOwe,
                onAddExpenseClick = onAddExpenseClick,
                onVoiceExpenseClick = onVoiceExpenseClick,
                onParseMessageClick = onParseMessageClick,
                onSettleUpClick = { onSettleUpClick(null) },
                onSimplifyDebtsClick = onSimplifyDebtsClick
            )
        }

        // Friends & Balances Section Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Friends & Balances",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )

                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = PurpleContainer,
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .clickable { onAddFriendClick() }
                        .testTag("add_friend_chip")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.PersonAdd,
                            contentDescription = null,
                            tint = PurplePrimaryDark,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Add Friend",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = PurplePrimaryDark
                        )
                    }
                }
            }
        }

        // Friends Balances Cards
        if (friendSummaries.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No friends added yet. Tap 'Add Friend' to get started!",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextMuted
                        )
                    }
                }
            }
        } else {
            items(friendSummaries) { friend ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("friend_card_${friend.userId}"),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Avatar + Name
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            AvatarCircle(
                                name = friend.userName,
                                colorHex = friend.userAvatarColor,
                                size = 44.dp
                            )
                            Spacer(modifier = Modifier.width(14.dp))
                            Column {
                                Text(
                                    text = friend.userName,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )

                                Spacer(modifier = Modifier.height(2.dp))

                                val balance = friend.netBalance
                                if (abs(balance) < 0.01) {
                                    Text(
                                        text = "settled up",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = SettledGray
                                    )
                                } else if (balance > 0.01) {
                                    Text(
                                        text = "owes you $${String.format(Locale.US, "%.2f", balance)}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = PositiveGreen,
                                        fontWeight = FontWeight.Bold
                                    )
                                } else {
                                    Text(
                                        text = "you owe $${String.format(Locale.US, "%.2f", abs(balance))}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = NegativeRed,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        // Settle Button if non-zero
                        if (abs(friend.netBalance) > 0.01) {
                            val owesYou = friend.netBalance > 0.01
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (owesYou) PositiveGreenBg else NegativeRedBg,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable {
                                        if (owesYou) {
                                            // They pay you
                                            onSettleUpClick(
                                                SettlePrefill(
                                                    fromUserId = friend.userId,
                                                    toUserId = currentUserId,
                                                    suggestedAmount = friend.netBalance
                                                )
                                            )
                                        } else {
                                            // You pay them
                                            onSettleUpClick(
                                                SettlePrefill(
                                                    fromUserId = currentUserId,
                                                    toUserId = friend.userId,
                                                    suggestedAmount = abs(friend.netBalance)
                                                )
                                            )
                                        }
                                    }
                            ) {
                                Text(
                                    text = "Settle",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (owesYou) PositiveGreen else NegativeRed,
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Recent Expenses Header
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Recent Transactions",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (expenses.isNotEmpty()) {
                        OutlinedButton(
                            onClick = { showExportDialog = true },
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier
                                .height(32.dp)
                                .testTag("home_export_csv_button"),
                            border = BorderStroke(1.dp, PurplePrimary)
                        ) {
                            Icon(
                                imageVector = Icons.Default.FileDownload,
                                contentDescription = "Export CSV",
                                tint = PurplePrimary,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Export", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = PurplePrimaryDark)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text(
                        text = "${expenses.size} total",
                        style = MaterialTheme.typography.labelMedium,
                        color = TextMuted
                    )
                }
            }
        }

        // Expenses List
        if (expenses.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.ReceiptLong,
                            contentDescription = null,
                            tint = TextMuted,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "No expenses recorded yet",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Tap 'Add Expense' or 'Smart Parse' to split a bill!",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMuted
                        )
                    }
                }
            }
        } else {
            items(expenses.take(15)) { expense ->
                val groupName = expense.groupId?.let { groupMap[it]?.name }
                ExpenseItemCard(
                    expense = expense,
                    users = users,
                    currentUserId = currentUserId,
                    onDeleteClick = onDeleteExpense,
                    groupName = groupName
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(40.dp))
        }
    }

    if (showExportDialog) {
        ExportReportBottomSheet(
            allExpenses = expenses,
            filteredExpenses = expenses,
            users = users,
            groups = groups,
            onDismiss = { showExportDialog = false }
        )
    }
}

