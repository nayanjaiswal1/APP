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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CallSplit
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.ExpenseEntity
import com.example.data.local.entity.GroupEntity
import com.example.data.local.entity.UserEntity
import com.example.domain.model.GroupSummary
import com.example.ui.components.AvatarCircle
import com.example.ui.components.CategoryIcon
import com.example.ui.components.ExpenseItemCard
import com.example.ui.theme.NegativeRed
import com.example.ui.theme.PositiveGreen
import com.example.ui.theme.PurpleContainer
import com.example.ui.theme.PurplePrimary
import com.example.ui.theme.PurplePrimaryDark
import com.example.ui.theme.PurpleSecondaryContainer
import com.example.ui.theme.PurpleSurfaceVariant
import com.example.ui.theme.SettledGray
import com.example.ui.theme.TextMuted
import java.util.Locale
import kotlin.math.abs

@Composable
fun GroupsScreen(
    groups: List<GroupEntity>,
    users: List<UserEntity>,
    expenses: List<ExpenseEntity>,
    currentUserId: Long,
    groupSummaries: Map<Long, GroupSummary>,
    selectedGroupId: Long?,
    onSelectGroup: (Long?) -> Unit,
    onCreateGroupClick: () -> Unit,
    onAddExpenseInGroup: () -> Unit,
    onSimplifyGroupDebts: () -> Unit,
    onDeleteExpense: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val userMap = users.associateBy { it.id }

    if (selectedGroupId != null) {
        val selectedGroup = groups.find { it.id == selectedGroupId }
        if (selectedGroup != null) {
            val groupExpenses = expenses.filter { it.groupId == selectedGroupId }
            val summary = groupSummaries[selectedGroupId]
            val memberIds = selectedGroup.memberIds.split(",").mapNotNull { it.trim().toLongOrNull() }
            val groupMembers = memberIds.mapNotNull { userMap[it] }

            // Group Detail View
            LazyColumn(
                modifier = modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
                    .testTag("group_detail_screen"),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Top Navigation Bar
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { onSelectGroup(null) }) {
                            Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = selectedGroup.name,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // Group Hero Banner Card
                item {
                    val coverColor = try {
                        Color(android.graphics.Color.parseColor(selectedGroup.coverColorHex))
                    } catch (e: Exception) {
                        PurplePrimary
                    }

                    Card(
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier.fillMaxWidth()
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
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = coverColor.copy(alpha = 0.15f)
                                ) {
                                    Text(
                                        text = selectedGroup.category.uppercase(),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = coverColor,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    )
                                }

                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = PurpleContainer,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable { onSimplifyGroupDebts() }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.CallSplit,
                                            contentDescription = null,
                                            tint = PurplePrimaryDark,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "Simplify Debts",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = PurplePrimaryDark,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Bottom
                            ) {
                                Column {
                                    Text(
                                        text = "Total Group Spending",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TextMuted
                                    )
                                    Text(
                                        text = "$${String.format(Locale.US, "%.2f", summary?.totalSpend ?: 0.0)}",
                                        style = MaterialTheme.typography.displaySmall,
                                        fontWeight = FontWeight.Bold,
                                        color = PurplePrimaryDark
                                    )
                                }

                                Column(horizontalAlignment = Alignment.End) {
                                    val userNet = summary?.userNetBalance ?: 0.0
                                    Text(
                                        text = "Your Balance",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TextMuted
                                    )
                                    Text(
                                        text = if (userNet > 0.01) "+$${String.format(Locale.US, "%.2f", userNet)}" else if (userNet < -0.01) "-$${String.format(Locale.US, "%.2f", abs(userNet))}" else "$0.00",
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = if (userNet > 0.01) PositiveGreen else if (userNet < -0.01) NegativeRed else PurplePrimaryDark
                                    )
                                }
                            }
                        }
                    }
                }

                // Member Balances Horizontal Row
                item {
                    Text(
                        text = "Group Members (${groupMembers.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(groupMembers) { member ->
                            val balance = summary?.memberBalances?.get(member.id) ?: 0.0
                            val isYou = member.id == currentUserId
                            val name = if (isYou) "You" else member.name

                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = MaterialTheme.colorScheme.surface,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant),
                                shadowElevation = 0.5.dp
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    AvatarCircle(name = name, colorHex = member.avatarColorHex, size = 32.dp)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(text = name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                        Text(
                                            text = if (abs(balance) < 0.01) "settled" else if (balance > 0) "+$${String.format(Locale.US, "%.2f", balance)}" else "-$${String.format(Locale.US, "%.2f", abs(balance))}",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = if (balance > 0.01) PositiveGreen else if (balance < -0.01) NegativeRed else SettledGray
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Group Expenses Feed Header
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Group Expenses",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = PurplePrimary,
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { onAddExpenseInGroup() }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Add in Group",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }

                if (groupExpenses.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(28.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ReceiptLong,
                                    contentDescription = null,
                                    tint = TextMuted,
                                    modifier = Modifier.size(40.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "No expenses in this group yet",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium,
                                    color = TextMuted
                                )
                            }
                        }
                    }
                } else {
                    items(groupExpenses) { exp ->
                        ExpenseItemCard(
                            expense = exp,
                            users = users,
                            currentUserId = currentUserId,
                            onDeleteClick = onDeleteExpense
                        )
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(40.dp))
                }
            }
            return
        }
    }

    // All Groups List View
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .testTag("groups_screen_content"),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        item {
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Groups",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Manage shared group budgets & trips",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted
                    )
                }

                Button(
                    onClick = onCreateGroupClick,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PurplePrimary),
                    modifier = Modifier.testTag("create_group_button")
                ) {
                    Icon(imageVector = Icons.Default.GroupAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("New Group", fontWeight = FontWeight.Bold)
                }
            }
        }

        // Groups Cards
        if (groups.isEmpty()) {
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
                        Icon(imageVector = Icons.Default.Group, contentDescription = null, tint = TextMuted, modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(text = "No groups yet", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Text(text = "Create a group for your trip, housemates, or dinner club!", style = MaterialTheme.typography.bodySmall, color = TextMuted)
                    }
                }
            }
        } else {
            items(groups) { group ->
                val summary = groupSummaries[group.id]
                val memberIds = group.memberIds.split(",").mapNotNull { it.trim().toLongOrNull() }
                val memberUsers = memberIds.mapNotNull { userMap[it] }
                val coverColor = try {
                    Color(android.graphics.Color.parseColor(group.coverColorHex))
                } catch (e: Exception) {
                    PurplePrimary
                }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .clickable { onSelectGroup(group.id) }
                        .testTag("group_item_${group.id}"),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp)
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        // Colored accent strip
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(5.dp)
                                .background(coverColor)
                        )

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(42.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(coverColor.copy(alpha = 0.12f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Group,
                                            contentDescription = null,
                                            tint = coverColor,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = group.name,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "${group.category} • ${memberIds.size} members",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = TextMuted
                                        )
                                    }
                                }

                                // Personal Net in Group
                                val userNet = summary?.userNetBalance ?: 0.0
                                Column(horizontalAlignment = Alignment.End) {
                                    if (abs(userNet) < 0.01) {
                                        Text(text = "settled up", style = MaterialTheme.typography.labelSmall, color = SettledGray)
                                    } else if (userNet > 0.01) {
                                        Text(text = "you are owed", style = MaterialTheme.typography.labelSmall, color = PositiveGreen)
                                        Text(
                                            text = "+$${String.format(Locale.US, "%.2f", userNet)}",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = PositiveGreen
                                        )
                                    } else {
                                        Text(text = "you owe", style = MaterialTheme.typography.labelSmall, color = NegativeRed)
                                        Text(
                                            text = "-$${String.format(Locale.US, "%.2f", abs(userNet))}",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = NegativeRed
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // Member Avatar Overlaps & Total spend
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy((-6).dp)
                                ) {
                                    memberUsers.take(4).forEach { member ->
                                        AvatarCircle(
                                            name = if (member.id == currentUserId) "You" else member.name,
                                            colorHex = member.avatarColorHex,
                                            size = 28.dp
                                        )
                                    }
                                }

                                Text(
                                    text = "Total Spend: $${String.format(Locale.US, "%.2f", summary?.totalSpend ?: 0.0)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Medium,
                                    color = TextMuted
                                )
                            }
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}
