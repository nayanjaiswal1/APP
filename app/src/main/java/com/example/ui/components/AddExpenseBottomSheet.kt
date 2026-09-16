package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
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
import com.example.data.parser.ParsedExpenseResult
import com.example.domain.model.SplitMode
import com.example.ui.theme.NegativeRed
import com.example.ui.theme.PositiveGreen
import com.example.ui.theme.PurpleContainer
import com.example.ui.theme.PurplePrimary
import com.example.ui.theme.PurplePrimaryDark
import com.example.ui.theme.PurpleSecondaryContainer
import com.example.ui.theme.PurpleSurfaceVariant
import com.example.ui.theme.TextMuted
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddExpenseBottomSheet(
    users: List<UserEntity>,
    groups: List<GroupEntity>,
    currentUserId: Long,
    prefill: ParsedExpenseResult? = null,
    onDismiss: () -> Unit,
    onSaveExpense: (
        title: String,
        amount: Double,
        currency: String,
        category: String,
        groupId: Long?,
        payerId: Long,
        splitType: String,
        splitDetails: Map<Long, Double>,
        notes: String?,
        sourceMessage: String?
    ) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var title by remember { mutableStateOf(prefill?.title ?: "") }
    var amountStr by remember { mutableStateOf(if (prefill != null && prefill.amount > 0) String.format(Locale.US, "%.2f", prefill.amount) else "") }
    var currency by remember { mutableStateOf(prefill?.currency ?: "$") }
    var category by remember { mutableStateOf(prefill?.category ?: "FOOD") }
    var selectedGroupId by remember { mutableStateOf<Long?>(null) }
    var payerId by remember { mutableStateOf(currentUserId) }
    var splitMode by remember { mutableStateOf(SplitMode.EQUAL) }
    var notes by remember { mutableStateOf(prefill?.notes ?: "") }

    // Participant selection & values
    val selectedMembers = remember { mutableStateMapOf<Long, Boolean>() }
    val customValues = remember { mutableStateMapOf<Long, String>() }

    // Determine candidate members based on group
    val candidateMembers = remember(selectedGroupId, groups, users) {
        if (selectedGroupId != null) {
            val group = groups.find { it.id == selectedGroupId }
            val memberIds = group?.memberIds?.split(",")?.mapNotNull { it.trim().toLongOrNull() } ?: emptyList()
            users.filter { it.id in memberIds }
        } else {
            users
        }
    }

    // Initialize member selections
    LaunchedEffect(candidateMembers) {
        candidateMembers.forEach { user ->
            if (!selectedMembers.containsKey(user.id)) {
                selectedMembers[user.id] = true
            }
            if (!customValues.containsKey(user.id)) {
                customValues[user.id] = "1"
            }
        }
    }

    val totalAmount = amountStr.toDoubleOrNull() ?: 0.0
    val activeMemberIds = candidateMembers.filter { selectedMembers[it.id] == true }.map { it.id }

    // Compute live splits
    val computedSplits: Map<Long, Double> = remember(splitMode, totalAmount, activeMemberIds, customValues) {
        val count = activeMemberIds.size.coerceAtLeast(1)
        when (splitMode) {
            SplitMode.EQUAL -> {
                val perPerson = ((totalAmount / count) * 100.0).roundToInt() / 100.0
                activeMemberIds.associateWith { perPerson }
            }
            SplitMode.EXACT -> {
                activeMemberIds.associateWith { uid ->
                    customValues[uid]?.toDoubleOrNull() ?: 0.0
                }
            }
            SplitMode.PERCENTAGE -> {
                activeMemberIds.associateWith { uid ->
                    val pct = customValues[uid]?.toDoubleOrNull() ?: (100.0 / count)
                    ((totalAmount * (pct / 100.0)) * 100.0).roundToInt() / 100.0
                }
            }
            SplitMode.SHARES -> {
                val shares = activeMemberIds.associateWith { uid ->
                    customValues[uid]?.toDoubleOrNull()?.coerceAtLeast(1.0) ?: 1.0
                }
                val totalShares = shares.values.sum().coerceAtLeast(1.0)
                shares.mapValues { ((totalAmount * (it.value / totalShares)) * 100.0).roundToInt() / 100.0 }
            }
        }
    }

    // Validation
    val splitSum = computedSplits.values.sum()
    val isBalanced = if (splitMode == SplitMode.EXACT) {
        abs(splitSum - totalAmount) < 0.02
    } else {
        true
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        modifier = Modifier.testTag("add_expense_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
        ) {
            // Sheet Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (prefill != null) "Review Parsed Expense" else "Add Expense",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    if (prefill?.parsedByAi == true) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = PurpleContainer
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = PurplePrimaryDark,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "AI Parsed",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = PurplePrimaryDark,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                IconButton(onClick = onDismiss) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Title and Amount Inputs
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = title,
                            onValueChange = { title = it },
                            label = { Text("Expense Title") },
                            placeholder = { Text("e.g. Dinner, Uber, Groceries") },
                            modifier = Modifier
                                .weight(1.8f)
                                .testTag("expense_title_input"),
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = PurplePrimary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.surfaceVariant
                            ),
                            singleLine = true
                        )

                        // Currency + Amount
                        OutlinedTextField(
                            value = amountStr,
                            onValueChange = { amountStr = it },
                            label = { Text("Amount ($currency)") },
                            placeholder = { Text("0.00") },
                            modifier = Modifier
                                .weight(1.2f)
                                .testTag("expense_amount_input"),
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = PurplePrimary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.surfaceVariant
                            ),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                        )
                    }
                }

                // Currency selector chips
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Currency:",
                            style = MaterialTheme.typography.labelMedium,
                            color = TextMuted
                        )
                        listOf("$", "₹", "€", "£", "¥", "C$").forEach { curr ->
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (currency == curr) PurplePrimary else MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable { currency = curr }
                            ) {
                                Text(
                                    text = curr,
                                    color = if (currency == curr) Color.White else MaterialTheme.colorScheme.onSurface,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }

                // Category Chips
                item {
                    Column {
                        Text(
                            text = "Category",
                            style = MaterialTheme.typography.labelMedium,
                            color = TextMuted
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            val categories = listOf("FOOD", "TRAVEL", "SHOPPING", "ENTERTAINMENT", "UTILITIES", "RENT", "GENERAL")
                            items(categories) { cat ->
                                FilterChip(
                                    selected = category == cat,
                                    onClick = { category = cat },
                                    label = { Text(cat.lowercase().replaceFirstChar { it.uppercase() }) },
                                    leadingIcon = {
                                        CategoryIcon(category = cat, size = 20.dp)
                                    },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = PurpleContainer,
                                        selectedLabelColor = PurplePrimaryDark
                                    )
                                )
                            }
                        }
                    }
                }

                // Group Selector
                item {
                    Column {
                        Text(
                            text = "Group",
                            style = MaterialTheme.typography.labelMedium,
                            color = TextMuted
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            item {
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (selectedGroupId == null) PurplePrimary else MaterialTheme.colorScheme.surfaceVariant,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable { selectedGroupId = null }
                                ) {
                                    Text(
                                        text = "No Group (Direct)",
                                        color = if (selectedGroupId == null) Color.White else MaterialTheme.colorScheme.onSurface,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                                    )
                                }
                            }

                            items(groups) { grp ->
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (selectedGroupId == grp.id) PurplePrimary else MaterialTheme.colorScheme.surfaceVariant,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable { selectedGroupId = grp.id }
                                ) {
                                    Text(
                                        text = grp.name,
                                        color = if (selectedGroupId == grp.id) Color.White else MaterialTheme.colorScheme.onSurface,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // Paid By Selector
                item {
                    Column {
                        Text(
                            text = "Paid by",
                            style = MaterialTheme.typography.labelMedium,
                            color = TextMuted
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(candidateMembers) { member ->
                                val isSelected = payerId == member.id
                                val name = if (member.id == currentUserId) "You" else member.name
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = if (isSelected) PurpleContainer else MaterialTheme.colorScheme.surfaceVariant,
                                    border = if (isSelected) BorderStroke(1.5.dp, PurplePrimary) else null,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(14.dp))
                                        .clickable { payerId = member.id }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        AvatarCircle(name = name, colorHex = member.avatarColorHex, size = 24.dp)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = name,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isSelected) PurplePrimaryDark else MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Split Mode Tabs
                item {
                    Column {
                        Text(
                            text = "Split method",
                            style = MaterialTheme.typography.labelMedium,
                            color = TextMuted
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        TabRow(
                            selectedTabIndex = splitMode.ordinal,
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.clip(RoundedCornerShape(14.dp))
                        ) {
                            SplitMode.values().forEach { mode ->
                                Tab(
                                    selected = splitMode == mode,
                                    onClick = { splitMode = mode },
                                    text = {
                                        Text(
                                            text = "${mode.displayName} ${mode.name.lowercase().replaceFirstChar { it.uppercase() }}",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = if (splitMode == mode) FontWeight.Bold else FontWeight.Normal
                                        )
                                    }
                                )
                            }
                        }
                    }
                }

                // Members list with shares / allocations
                item {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Split with (${activeMemberIds.size} people)",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold
                                )

                                if (splitMode == SplitMode.EXACT && totalAmount > 0) {
                                    val diff = totalAmount - splitSum
                                    Text(
                                        text = if (abs(diff) < 0.02) "Balanced ✓" else if (diff > 0) "$currency${String.format(Locale.US, "%.2f", diff)} left" else "Over by $currency${String.format(Locale.US, "%.2f", abs(diff))}",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (abs(diff) < 0.02) PositiveGreen else NegativeRed
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            candidateMembers.forEach { member ->
                                val isChecked = selectedMembers[member.id] == true
                                val memberName = if (member.id == currentUserId) "You" else member.name
                                val shareAmount = computedSplits[member.id] ?: 0.0

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Checkbox(
                                            checked = isChecked,
                                            onCheckedChange = { checked ->
                                                selectedMembers[member.id] = checked
                                            },
                                            colors = CheckboxDefaults.colors(checkedColor = PurplePrimary)
                                        )
                                        AvatarCircle(name = memberName, colorHex = member.avatarColorHex, size = 28.dp)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = memberName,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = if (isChecked) FontWeight.SemiBold else FontWeight.Normal
                                        )
                                    }

                                    if (isChecked) {
                                        if (splitMode == SplitMode.EQUAL) {
                                            Text(
                                                text = "$currency${String.format(Locale.US, "%.2f", shareAmount)}",
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.SemiBold,
                                                color = PurplePrimaryDark
                                            )
                                        } else {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                OutlinedTextField(
                                                    value = customValues[member.id] ?: "",
                                                    onValueChange = { customValues[member.id] = it },
                                                    modifier = Modifier.width(80.dp),
                                                    shape = RoundedCornerShape(10.dp),
                                                    singleLine = true,
                                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                                    colors = OutlinedTextFieldDefaults.colors(
                                                        focusedBorderColor = PurplePrimary,
                                                        unfocusedBorderColor = MaterialTheme.colorScheme.surfaceVariant
                                                    ),
                                                    suffix = {
                                                        Text(
                                                            text = when (splitMode) {
                                                                SplitMode.PERCENTAGE -> "%"
                                                                SplitMode.SHARES -> "x"
                                                                else -> ""
                                                            },
                                                            style = MaterialTheme.typography.labelSmall
                                                        )
                                                    }
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = "($currency${String.format(Locale.US, "%.2f", shareAmount)})",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = TextMuted
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Notes input
                item {
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("Notes / Description (Optional)") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PurplePrimary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.surfaceVariant
                        ),
                        singleLine = false,
                        maxLines = 2
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Save Button
            val isAmountValid = totalAmount.isFinite() && totalAmount > 0.0 && totalAmount <= 1_000_000_000.0
            val isTitleValid = title.isNotBlank() && title.trim().length <= 120

            Button(
                onClick = {
                    if (isTitleValid && isAmountValid) {
                        onSaveExpense(
                            title.trim().take(120),
                            totalAmount,
                            currency,
                            category,
                            selectedGroupId,
                            payerId,
                            splitMode.name,
                            computedSplits,
                            notes.trim().take(1000).ifBlank { null },
                            prefill?.rawMessage
                        )
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("save_expense_button"),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PurplePrimary),
                enabled = isTitleValid && isAmountValid && isBalanced && activeMemberIds.isNotEmpty()
            ) {
                Text(
                    text = "Save Expense ($currency${String.format(Locale.US, "%.2f", totalAmount)})",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }
        }
    }
}

