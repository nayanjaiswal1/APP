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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.ExpenseEntity
import com.example.data.local.entity.UserEntity
import com.example.domain.model.DebtEngine
import com.example.ui.theme.NegativeRed
import com.example.ui.theme.PositiveGreen
import com.example.ui.theme.PurpleContainer
import com.example.ui.theme.PurplePrimary
import com.example.ui.theme.PurplePrimaryDark
import com.example.ui.theme.SettledGray
import com.example.ui.theme.TextDark
import com.example.ui.theme.TextMuted
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

@Composable
fun ExpenseItemCard(
    expense: ExpenseEntity,
    users: List<UserEntity>,
    currentUserId: Long,
    onDeleteClick: (Long) -> Unit,
    modifier: Modifier = Modifier,
    groupName: String? = null
) {
    val payer = users.find { it.id == expense.payerId }
    val payerName = if (payer?.id == currentUserId) "You" else (payer?.name ?: "Someone")
    val splits = DebtEngine.parseSplitDetails(expense.splitDetailsJson)

    val dateFormat = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
    val dateStr = dateFormat.format(Date(expense.dateMillis))

    val yourShare = when (expense.splitType) {
        "PERCENTAGE" -> {
            val pct = splits[currentUserId] ?: 0.0
            expense.amount * (pct / 100.0)
        }
        "SHARES" -> {
            val totalShares = splits.values.sum().coerceAtLeast(1.0)
            val share = splits[currentUserId] ?: 0.0
            expense.amount * (share / totalShares)
        }
        else -> splits[currentUserId] ?: if (splits.isEmpty()) (expense.amount / users.size.coerceAtLeast(1)) else 0.0
    }

    val isSettlement = expense.isSettlement
    val isPayer = expense.payerId == currentUserId

    val yourNet = if (isSettlement) {
        if (expense.settlementFromId == currentUserId) -expense.amount else expense.amount
    } else {
        if (isPayer) (expense.amount - yourShare) else -yourShare
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("expense_card_${expense.id}"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Category or Settlement Icon
            if (isSettlement) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = PurpleContainer,
                    modifier = Modifier.size(44.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Settlement",
                            tint = PurplePrimaryDark,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            } else {
                CategoryIcon(category = expense.category, size = 44.dp)
            }

            Spacer(modifier = Modifier.width(14.dp))

            // Main Details
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = expense.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (expense.sourceMessage != null) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = "Parsed from message",
                            tint = PurplePrimary,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = if (isSettlement) {
                        expense.notes ?: "Payment completed"
                    } else {
                        "Shared with $payerName • $dateStr"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted
                )

                if (groupName != null) {
                    Text(
                        text = "in $groupName",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMuted.copy(alpha = 0.8f)
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Balance impact on "You"
            Column(horizontalAlignment = Alignment.End) {
                if (isSettlement) {
                    Text(
                        text = "${expense.currency}${String.format(Locale.US, "%.2f", expense.amount)}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = PurplePrimaryDark
                    )
                    Text(
                        text = "settled up",
                        style = MaterialTheme.typography.labelSmall,
                        color = SettledGray,
                        fontWeight = FontWeight.Medium
                    )
                } else if (abs(yourNet) < 0.01) {
                    Text(
                        text = "$0.00",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = SettledGray
                    )
                    Text(
                        text = "not involved",
                        style = MaterialTheme.typography.labelSmall,
                        color = SettledGray
                    )
                } else if (yourNet > 0.01) {
                    Text(
                        text = "$${String.format(Locale.US, "%.2f", yourNet)}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = PurplePrimaryDark
                    )
                    Text(
                        text = "You get back",
                        style = MaterialTheme.typography.labelSmall,
                        color = PositiveGreen,
                        fontWeight = FontWeight.Bold
                    )
                } else {
                    Text(
                        text = "$${String.format(Locale.US, "%.2f", abs(yourNet))}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = PurplePrimaryDark
                    )
                    Text(
                        text = "You owe $payerName",
                        style = MaterialTheme.typography.labelSmall,
                        color = NegativeRed,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Delete button
                IconButton(
                    onClick = { onDeleteClick(expense.id) },
                    modifier = Modifier.size(22.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = "Delete expense",
                        tint = TextMuted.copy(alpha = 0.4f),
                        modifier = Modifier.size(15.dp)
                    )
                }
            }
        }
    }
}

