package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.local.entity.ExpenseEntity
import com.example.data.remote.ConflictInfo
import com.example.data.remote.FmsApiResult
import com.example.data.remote.FmsClientManager
import com.example.data.remote.dto.BudgetResponseDto
import com.example.data.remote.dto.CreateBudgetDto
import com.example.ui.components.BackendStatusBanner
import com.example.ui.components.CategoryIcon
import com.example.ui.components.getCategoryVisual
import com.example.ui.theme.NegativeRed
import com.example.ui.theme.NegativeRedBg
import com.example.ui.theme.PositiveGreen
import com.example.ui.theme.PositiveGreenBg
import com.example.ui.theme.PurpleContainer
import com.example.ui.theme.PurplePrimary
import com.example.ui.theme.PurplePrimaryDark
import com.example.ui.theme.TextMuted
import com.example.ui.theme.WarningAmber
import com.example.ui.theme.WarningAmberBg
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetsScreen(
    expenses: List<ExpenseEntity>,
    clientManager: FmsClientManager,
    onBack: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    var budgets by remember {
        mutableStateOf<List<BudgetResponseDto>>(
            listOf(
                BudgetResponseDto(
                    id = "b-1",
                    category = "Food & Dining",
                    amount = 500.0,
                    spent = 345.0,
                    remaining = 155.0,
                    alertThresholdPercentage = 80.0
                ),
                BudgetResponseDto(
                    id = "b-2",
                    category = "Groceries",
                    amount = 350.0,
                    spent = 210.0,
                    remaining = 140.0,
                    alertThresholdPercentage = 80.0
                ),
                BudgetResponseDto(
                    id = "b-3",
                    category = "Shopping",
                    amount = 200.0,
                    spent = 220.0,
                    remaining = 0.0,
                    isOverBudget = true,
                    alertThresholdPercentage = 80.0
                ),
                BudgetResponseDto(
                    id = "b-4",
                    category = "Entertainment",
                    amount = 150.0,
                    spent = 65.0,
                    remaining = 85.0,
                    alertThresholdPercentage = 80.0
                ),
                BudgetResponseDto(
                    id = "b-5",
                    category = "Transportation",
                    amount = 120.0,
                    spent = 95.0,
                    remaining = 25.0,
                    alertThresholdPercentage = 80.0
                )
            )
        )
    }

    var isAddDialogOpen by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }

    // Fetch from remote backend if connected
    LaunchedEffect(Unit) {
        val result = clientManager.executeSafely("Fetch Budgets") { service ->
            service.getBudgets()
        }
        if (result is FmsApiResult.Success && result.data.isNotEmpty()) {
            budgets = result.data
        }
    }

    val totalBudget = budgets.sumOf { it.amount }
    val totalSpent = budgets.sumOf { it.spent }
    val totalRemaining = (totalBudget - totalSpent).coerceAtLeast(0.0)
    val overallProgress = if (totalBudget > 0) (totalSpent / totalBudget).toFloat().coerceIn(0f, 1f) else 0f
    val overBudgetCount = budgets.count { it.spent > it.amount }

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
                .testTag("budgets_screen"),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (onBack != null) {
                        IconButton(
                            onClick = onBack,
                            modifier = Modifier.testTag("budgets_back_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back"
                            )
                        }
                    }
                    Column {
                        Text(
                            text = "Budgets & Limits",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = "Track monthly limits and overspending alerts",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMuted
                        )
                    }
                }
            }

            // Universal Backend Status Banner (Handles Offline, Render spin-up, Conflicts, and 401)
            item {
                BackendStatusBanner(
                    clientManager = clientManager,
                    onResolveConflict = { conflict, action ->
                        // Re-fetch or keep local based on user choice
                        if (action == com.example.ui.components.ConflictResolutionAction.USE_SERVER_VERSION) {
                            coroutineScope.launch {
                                val res = clientManager.executeSafely("Refresh Budgets") { it.getBudgets() }
                                if (res is FmsApiResult.Success) {
                                    budgets = res.data
                                }
                            }
                        }
                    }
                )
            }

            // Overview Card
            item {
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("budget_overview_card")
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Monthly Budget Summary",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (overBudgetCount > 0) NegativeRedBg else PositiveGreenBg
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = if (overBudgetCount > 0) Icons.Default.Warning else Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = if (overBudgetCount > 0) NegativeRed else PositiveGreen,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = if (overBudgetCount > 0) "$overBudgetCount Exceeded" else "On Track",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (overBudgetCount > 0) NegativeRed else PositiveGreen
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(text = "Total Spent", style = MaterialTheme.typography.labelMedium, color = TextMuted)
                                Text(
                                    text = String.format(Locale.US, "$%.2f", totalSpent),
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(text = "Total Budget", style = MaterialTheme.typography.labelMedium, color = TextMuted)
                                Text(
                                    text = String.format(Locale.US, "$%.2f", totalBudget),
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = PurplePrimaryDark
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        LinearProgressIndicator(
                            progress = { overallProgress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = if (overallProgress >= 1f) NegativeRed else if (overallProgress >= 0.8f) WarningAmber else PurplePrimary,
                            trackColor = PurpleContainer
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = String.format(Locale.US, "%.0f%% of total used", overallProgress * 100),
                                style = MaterialTheme.typography.bodySmall,
                                color = TextMuted
                            )
                            Text(
                                text = String.format(Locale.US, "$%.2f remaining", totalRemaining),
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = if (totalRemaining > 0) PositiveGreen else NegativeRed
                            )
                        }
                    }
                }
            }

            // Alerts header
            if (overBudgetCount > 0) {
                item {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = NegativeRedBg),
                        border = BorderStroke(1.dp, NegativeRed.copy(alpha = 0.2f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.NotificationsActive,
                                contentDescription = null,
                                tint = NegativeRed,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Over-budget Notice",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = NegativeRed
                                )
                                Text(
                                    text = "One or more category budgets have exceeded their planned threshold.",
                                    fontSize = 12.sp,
                                    color = NegativeRed.copy(alpha = 0.85f)
                                )
                            }
                        }
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Category Budgets (${budgets.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    TextButton(
                        onClick = { isAddDialogOpen = true },
                        modifier = Modifier.testTag("btn_add_budget_header")
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add Budget")
                    }
                }
            }

            // Category items
            items(budgets, key = { it.id }) { item ->
                val ratio = if (item.amount > 0) (item.spent / item.amount).toFloat() else 0f
                val isExceeded = item.spent > item.amount
                val isNearLimit = !isExceeded && ratio >= 0.8f
                val visual = getCategoryVisual(item.category)

                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(
                        1.dp,
                        if (isExceeded) NegativeRed.copy(alpha = 0.3f) else MaterialTheme.colorScheme.surfaceVariant
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("budget_item_${item.id}")
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                CategoryIcon(category = item.category, size = 40.dp)
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = item.category,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = String.format(Locale.US, "$%.2f of $%.2f", item.spent, item.amount),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextMuted
                                    )
                                }
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (isExceeded) NegativeRedBg else if (isNearLimit) WarningAmberBg else PositiveGreenBg
                                ) {
                                    Text(
                                        text = if (isExceeded) "Exceeded" else if (isNearLimit) "80%+ Used" else "Normal",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isExceeded) NegativeRed else if (isNearLimit) WarningAmber else PositiveGreen,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }

                                IconButton(
                                    onClick = {
                                        budgets = budgets.filterNot { it.id == item.id }
                                        coroutineScope.launch {
                                            try {
                                                clientManager.getService()?.deleteBudget(item.id)
                                            } catch (_: Exception) { }
                                        }
                                    },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Delete",
                                        tint = TextMuted,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        LinearProgressIndicator(
                            progress = { ratio.coerceIn(0f, 1f) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = if (isExceeded) NegativeRed else if (isNearLimit) WarningAmber else PurplePrimary,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = String.format(Locale.US, "%.0f%% used", ratio * 100),
                                fontSize = 11.sp,
                                color = TextMuted
                            )
                            val remaining = (item.amount - item.spent).coerceAtLeast(0.0)
                            Text(
                                text = if (isExceeded) {
                                    String.format(Locale.US, "Over by $%.2f", item.spent - item.amount)
                                } else {
                                    String.format(Locale.US, "$%.2f left", remaining)
                                },
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isExceeded) NegativeRed else PositiveGreen
                            )
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(60.dp))
            }
        }

        FloatingActionButton(
            onClick = { isAddDialogOpen = true },
            containerColor = PurplePrimary,
            contentColor = Color.White,
            shape = CircleShape,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp)
                .testTag("fab_add_budget")
        ) {
            Icon(imageVector = Icons.Default.Add, contentDescription = "Add Budget")
        }
    }

    if (isAddDialogOpen) {
        AddBudgetDialog(
            onDismiss = { isAddDialogOpen = false },
            onAdd = { category, amount, threshold ->
                val newBudget = BudgetResponseDto(
                    id = "b-${System.currentTimeMillis()}",
                    category = category,
                    amount = amount,
                    spent = 0.0,
                    remaining = amount,
                    alertThresholdPercentage = threshold
                )
                budgets = listOf(newBudget) + budgets
                isAddDialogOpen = false
                coroutineScope.launch {
                    val result = clientManager.executeSafely("Create Budget ($category)") { service ->
                        service.createBudget(
                            CreateBudgetDto(
                                category = category,
                                amount = amount,
                                month = Calendar.getInstance().get(Calendar.MONTH) + 1,
                                year = Calendar.getInstance().get(Calendar.YEAR),
                                alertThresholdPercentage = threshold
                            )
                        )
                    }
                    when (result) {
                        is FmsApiResult.Success -> {
                            Toast.makeText(context, "Budget '$category' synced to cloud.", Toast.LENGTH_SHORT).show()
                        }
                        is FmsApiResult.Conflict -> {
                            Toast.makeText(context, "Conflict: Budget already exists on cloud for '$category'. Showing resolution options.", Toast.LENGTH_LONG).show()
                        }
                        is FmsApiResult.Unreachable -> {
                            val notice = if (result.isRenderColdStart) {
                                "Budget '$category' saved locally! (Render server is starting up...)"
                            } else {
                                "Budget '$category' saved locally! (Backend unreachable)"
                            }
                            Toast.makeText(context, notice, Toast.LENGTH_LONG).show()
                        }
                        is FmsApiResult.Unauthorized -> {
                            Toast.makeText(context, "Saved locally. Sign in to sync with cloud backend.", Toast.LENGTH_LONG).show()
                        }
                        else -> {
                            Toast.makeText(context, "Budget '$category' saved.", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        )
    }
}

@Composable
fun AddBudgetDialog(
    onDismiss: () -> Unit,
    onAdd: (category: String, amount: Double, threshold: Double) -> Unit
) {
    var category by remember { mutableStateOf("Food & Dining") }
    var amountText by remember { mutableStateOf("") }
    var thresholdText by remember { mutableStateOf("80") }

    val categories = listOf(
        "Food & Dining", "Groceries", "Shopping", "Entertainment",
        "Transportation", "Utilities", "Healthcare", "Travel", "Education", "Other"
    )

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .testTag("add_budget_dialog")
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(
                    text = "Create Budget",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Set a monthly spending limit for a category",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(text = "Category", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(6.dp))
                Row(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = category,
                        onValueChange = { category = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_budget_category"),
                        singleLine = true
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("Monthly Limit ($)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_budget_amount"),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = thresholdText,
                    onValueChange = { thresholdText = it },
                    label = { Text("Alert Threshold (%)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_budget_threshold"),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    OutlinedButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Button(
                        onClick = {
                            val amount = amountText.toDoubleOrNull() ?: 0.0
                            val threshold = thresholdText.toDoubleOrNull() ?: 80.0
                            if (amount > 0 && category.isNotBlank()) {
                                onAdd(category, amount, threshold)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PurplePrimary),
                        modifier = Modifier.testTag("btn_save_budget")
                    ) {
                        Text("Save Budget")
                    }
                }
            }
        }
    }
}
