package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.example.ui.components.CategoryIcon
import com.example.ui.components.ExpenseItemCard
import com.example.ui.components.getCategoryVisual
import com.example.ui.theme.PurpleContainer
import com.example.ui.theme.PurplePrimary
import com.example.ui.theme.PurplePrimaryDark
import com.example.ui.theme.TextMuted
import java.util.Locale

@Composable
fun ActivityScreen(
    expenses: List<ExpenseEntity>,
    users: List<UserEntity>,
    groups: List<GroupEntity>,
    currentUserId: Long,
    onDeleteExpense: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val groupMap = groups.associateBy { it.id }

    val nonSettlements = expenses.filter { !it.isSettlement }
    val totalSpend = nonSettlements.sumOf { it.amount }

    // Category distribution
    val categoryTotals = nonSettlements.groupBy { it.category }
        .mapValues { entry -> entry.value.sumOf { it.amount } }
        .toList()
        .sortedByDescending { it.second }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .testTag("activity_screen"),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Top Header
        item {
            Spacer(modifier = Modifier.height(4.dp))
            Column {
                Text(
                    text = "Activity & Analytics",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Spending breakdown & transaction logs",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted
                )
            }
        }

        // Spending Analytics Hero Card
        item {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(PurpleContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PieChart,
                                    contentDescription = null,
                                    tint = PurplePrimaryDark,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Category Breakdown",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Text(
                            text = "Total: $${String.format(Locale.US, "%.2f", totalSpend)}",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = PurplePrimary
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    if (categoryTotals.isEmpty()) {
                        Text(
                            text = "No expenses recorded yet to analyze.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMuted
                        )
                    } else {
                        categoryTotals.forEach { (cat, amount) ->
                            val progress = if (totalSpend > 0) (amount / totalSpend).toFloat() else 0f
                            val pct = (progress * 100).toInt()
                            val (_, color) = getCategoryVisual(cat)

                            Column(modifier = Modifier.padding(vertical = 5.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        CategoryIcon(category = cat, size = 26.dp)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = cat.lowercase().replaceFirstChar { it.uppercase() },
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }

                                    Text(
                                        text = "$${String.format(Locale.US, "%.2f", amount)} ($pct%)",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = TextMuted
                                    )
                                }

                                Spacer(modifier = Modifier.height(5.dp))

                                LinearProgressIndicator(
                                    progress = { progress },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp)
                                        .clip(RoundedCornerShape(3.dp)),
                                    color = color,
                                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }

        // Full Chronological Log
        item {
            Text(
                text = "All Activity Feed (${expenses.size})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        if (expenses.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Box(modifier = Modifier.padding(24.dp), contentAlignment = Alignment.Center) {
                        Text(
                            text = "No activities recorded.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextMuted
                        )
                    }
                }
            }
        } else {
            items(expenses) { exp ->
                val groupName = exp.groupId?.let { groupMap[it]?.name }
                ExpenseItemCard(
                    expense = exp,
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
}

