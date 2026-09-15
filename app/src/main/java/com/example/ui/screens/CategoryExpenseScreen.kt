package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.FilterAlt
import androidx.compose.material.icons.outlined.PieChart
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.ExpenseEntity
import com.example.data.local.entity.GroupEntity
import com.example.data.local.entity.UserEntity
import com.example.domain.model.DebtEngine
import com.example.ui.components.CategoryIcon
import com.example.ui.components.ExpenseItemCard
import com.example.ui.components.ExportReportBottomSheet
import com.example.ui.components.getCategoryVisual
import com.example.ui.theme.PositiveGreen
import com.example.ui.theme.PurpleContainer
import com.example.ui.theme.PurplePrimary
import com.example.ui.theme.PurplePrimaryDark
import com.example.ui.theme.PurpleSecondaryContainer
import com.example.ui.theme.PurpleSurfaceVariant
import com.example.ui.theme.TextDark
import com.example.ui.theme.TextMuted
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Locale

enum class ExpenseScopeFilter(val label: String) {
    ALL("All Expenses"),
    PERSONAL_ONLY("Personal Only"),
    SHARED_ONLY("Shared / Group"),
    SETTLEMENTS_ONLY("Settlements")
}

enum class PayerFilter(val label: String) {
    ALL("All Payers"),
    PAID_BY_ME("Paid by You"),
    PAID_BY_OTHERS("Paid by Friends")
}

enum class TimeframeFilter(val label: String) {
    ALL_TIME("All Time"),
    THIS_MONTH("This Month"),
    LAST_30_DAYS("Last 30 Days"),
    THIS_WEEK("This Week"),
    TODAY("Today"),
    THIS_YEAR("This Year")
}

enum class AmountRangeFilter(val label: String, val min: Double?, val max: Double?) {
    ALL("Any Amount", null, null),
    UNDER_25("Under $25", 0.0, 25.0),
    RANGE_25_100("$25 – $100", 25.0, 100.0),
    RANGE_100_300("$100 – $300", 100.0, 300.0),
    OVER_300("$300+", 300.0, null)
}

enum class ExpenseSortOption(val label: String) {
    DATE_DESC("Newest First"),
    DATE_ASC("Oldest First"),
    AMOUNT_DESC("Highest Amount"),
    AMOUNT_ASC("Lowest Amount"),
    CATEGORY_ASC("Category (A-Z)")
}

val ALL_KNOWN_CATEGORIES = listOf(
    "FOOD", "GROCERIES", "TRAVEL", "SHOPPING", "ENTERTAINMENT",
    "UTILITIES", "RENT", "HEALTHCARE", "PERSONAL", "EDUCATION", "GENERAL"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryExpenseScreen(
    expenses: List<ExpenseEntity>,
    users: List<UserEntity>,
    groups: List<GroupEntity>,
    currentUserId: Long,
    onDeleteExpense: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val groupMap = groups.associateBy { it.id }
    val focusManager = LocalFocusManager.current

    // Filter States
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf<String?>(null) }
    var selectedScope by remember { mutableStateOf(ExpenseScopeFilter.ALL) }
    var selectedGroupId by remember { mutableStateOf<Long?>(null) }
    var selectedPayer by remember { mutableStateOf(PayerFilter.ALL) }
    var selectedTimeframe by remember { mutableStateOf(TimeframeFilter.ALL_TIME) }
    var selectedAmountRange by remember { mutableStateOf(AmountRangeFilter.ALL) }
    var sortBy by remember { mutableStateOf(ExpenseSortOption.DATE_DESC) }

    var isFilterSheetOpen by remember { mutableStateOf(false) }
    var isExportSheetOpen by remember { mutableStateOf(false) }
    var selectedViewMode by remember { mutableStateOf(0) } // 0 = Category Analytics, 1 = Filtered Transactions

    // Date calculations for filters
    val now = System.currentTimeMillis()
    val calendar = Calendar.getInstance()

    // Filter logic
    val filteredExpenses = expenses.filter { exp ->
        // Search query
        val matchesSearch = if (searchQuery.isBlank()) true else {
            exp.title.contains(searchQuery, ignoreCase = true) ||
                    exp.category.contains(searchQuery, ignoreCase = true) ||
                    (exp.notes?.contains(searchQuery, ignoreCase = true) == true)
        }

        // Category filter
        val matchesCategory = if (selectedCategory == null) true else {
            exp.category.equals(selectedCategory, ignoreCase = true)
        }

        // Scope filter
        val isPersonal = (exp.groupId == null && exp.payerId == currentUserId && DebtEngine.parseSplitDetails(exp.splitDetailsJson).keys.let { it.isEmpty() || (it.size == 1 && it.contains(currentUserId)) })
        val matchesScope = when (selectedScope) {
            ExpenseScopeFilter.ALL -> true
            ExpenseScopeFilter.PERSONAL_ONLY -> isPersonal && !exp.isSettlement
            ExpenseScopeFilter.SHARED_ONLY -> !isPersonal && !exp.isSettlement
            ExpenseScopeFilter.SETTLEMENTS_ONLY -> exp.isSettlement
        }

        // Group filter
        val matchesGroup = if (selectedGroupId == null) true else {
            exp.groupId == selectedGroupId
        }

        // Payer filter
        val matchesPayer = when (selectedPayer) {
            PayerFilter.ALL -> true
            PayerFilter.PAID_BY_ME -> exp.payerId == currentUserId
            PayerFilter.PAID_BY_OTHERS -> exp.payerId != currentUserId
        }

        // Timeframe filter
        val matchesTimeframe = when (selectedTimeframe) {
            TimeframeFilter.ALL_TIME -> true
            TimeframeFilter.TODAY -> {
                calendar.timeInMillis = now
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                exp.dateMillis >= calendar.timeInMillis
            }
            TimeframeFilter.THIS_WEEK -> {
                calendar.timeInMillis = now
                calendar.set(Calendar.DAY_OF_WEEK, calendar.firstDayOfWeek)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                exp.dateMillis >= calendar.timeInMillis
            }
            TimeframeFilter.THIS_MONTH -> {
                calendar.timeInMillis = now
                calendar.set(Calendar.DAY_OF_MONTH, 1)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                exp.dateMillis >= calendar.timeInMillis
            }
            TimeframeFilter.LAST_30_DAYS -> {
                exp.dateMillis >= (now - 30L * 24 * 60 * 60 * 1000)
            }
            TimeframeFilter.THIS_YEAR -> {
                calendar.timeInMillis = now
                calendar.set(Calendar.DAY_OF_YEAR, 1)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                exp.dateMillis >= calendar.timeInMillis
            }
        }

        // Amount Range filter
        val matchesAmount = (selectedAmountRange.min == null || exp.amount >= selectedAmountRange.min!!) &&
                (selectedAmountRange.max == null || exp.amount <= selectedAmountRange.max!!)

        matchesSearch && matchesCategory && matchesScope && matchesGroup && matchesPayer && matchesTimeframe && matchesAmount
    }.sortedWith { a, b ->
        when (sortBy) {
            ExpenseSortOption.DATE_DESC -> b.dateMillis.compareTo(a.dateMillis)
            ExpenseSortOption.DATE_ASC -> a.dateMillis.compareTo(b.dateMillis)
            ExpenseSortOption.AMOUNT_DESC -> b.amount.compareTo(a.amount)
            ExpenseSortOption.AMOUNT_ASC -> a.amount.compareTo(b.amount)
            ExpenseSortOption.CATEGORY_ASC -> a.category.compareTo(b.category, ignoreCase = true)
        }
    }

    // Calculations on filtered results
    val nonSettlements = filteredExpenses.filter { !it.isSettlement }
    val totalFilteredSpend = nonSettlements.sumOf { it.amount }
    val averageExpense = if (nonSettlements.isNotEmpty()) totalFilteredSpend / nonSettlements.size else 0.0

    // Grouping by category
    val categoryBreakdown = nonSettlements.groupBy { it.category.uppercase() }
        .map { (cat, list) ->
            val sum = list.sumOf { it.amount }
            val count = list.size
            val maxAmount = list.maxOfOrNull { it.amount } ?: 0.0
            CategoryStat(
                category = cat,
                totalAmount = sum,
                count = count,
                maxAmount = maxAmount,
                avgAmount = if (count > 0) sum / count else 0.0,
                percentage = if (totalFilteredSpend > 0) (sum / totalFilteredSpend).toFloat() else 0f
            )
        }.sortedByDescending { it.totalAmount }

    // Count active filters
    var activeFilterCount = 0
    if (selectedCategory != null) activeFilterCount++
    if (selectedScope != ExpenseScopeFilter.ALL) activeFilterCount++
    if (selectedGroupId != null) activeFilterCount++
    if (selectedPayer != PayerFilter.ALL) activeFilterCount++
    if (selectedTimeframe != TimeframeFilter.ALL_TIME) activeFilterCount++
    if (selectedAmountRange != AmountRangeFilter.ALL) activeFilterCount++
    if (searchQuery.isNotBlank()) activeFilterCount++

    fun resetAllFilters() {
        searchQuery = ""
        selectedCategory = null
        selectedScope = ExpenseScopeFilter.ALL
        selectedGroupId = null
        selectedPayer = PayerFilter.ALL
        selectedTimeframe = TimeframeFilter.ALL_TIME
        selectedAmountRange = AmountRangeFilter.ALL
        sortBy = ExpenseSortOption.DATE_DESC
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .testTag("category_expense_screen"),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Screen Header
        item {
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Category Expenses",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Visual spending breakdown & multi-filter",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Export Report Button
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = PurpleContainer,
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { isExportSheetOpen = true }
                            .testTag("btn_export_report")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.FileDownload,
                                contentDescription = "Export Report",
                                tint = PurplePrimaryDark,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Export",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = PurplePrimaryDark
                            )
                        }
                    }

                    // Filter Sheet Button with Active Count Badge
                    BadgedBox(
                        badge = {
                            if (activeFilterCount > 0) {
                                Badge(
                                    containerColor = PurplePrimary,
                                    contentColor = Color.White
                                ) {
                                    Text(activeFilterCount.toString())
                                }
                            }
                        }
                    ) {
                        FilledIconButtonCustom(
                            onClick = { isFilterSheetOpen = true },
                            icon = Icons.Default.Tune,
                            contentDescription = "Open Filter Panel",
                            isActive = activeFilterCount > 0
                        )
                    }
                }
            }
        }

        // Live Search Input Box
        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("expense_search_input"),
                placeholder = { Text("Search by merchant, note, keyword...") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = TextMuted
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Clear",
                                tint = TextMuted
                            )
                        }
                    }
                },
                shape = RoundedCornerShape(16.dp),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedBorderColor = MaterialTheme.colorScheme.surfaceVariant,
                    focusedBorderColor = PurplePrimary
                )
            )
        }

        // Quick Category Filter Chips (Horizontal Scrolling)
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // "All Categories" chip
                FilterChip(
                    selected = selectedCategory == null,
                    onClick = { selectedCategory = null },
                    label = { Text("All Categories") },
                    leadingIcon = if (selectedCategory == null) {
                        { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                    } else null,
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = PurpleContainer,
                        selectedLabelColor = PurplePrimaryDark
                    )
                )

                ALL_KNOWN_CATEGORIES.forEach { cat ->
                    val isSelected = selectedCategory.equals(cat, ignoreCase = true)
                    val visual = getCategoryVisual(cat)
                    FilterChip(
                        selected = isSelected,
                        onClick = {
                            selectedCategory = if (isSelected) null else cat
                        },
                        label = {
                            Text(cat.lowercase().replaceFirstChar { it.uppercase() })
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = visual.icon,
                                contentDescription = null,
                                tint = if (isSelected) PurplePrimaryDark else visual.iconColor,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = PurpleContainer,
                            selectedLabelColor = PurplePrimaryDark
                        )
                    )
                }
            }
        }

        // Active Filter Badges Bar (with Clear All)
        if (activeFilterCount > 0) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Active ($activeFilterCount):",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = PurplePrimary
                    )

                    if (selectedCategory != null) {
                        ActiveFilterPill(
                            label = "Category: ${selectedCategory?.lowercase()?.replaceFirstChar { it.uppercase() }}",
                            onRemove = { selectedCategory = null }
                        )
                    }
                    if (selectedScope != ExpenseScopeFilter.ALL) {
                        ActiveFilterPill(
                            label = selectedScope.label,
                            onRemove = { selectedScope = ExpenseScopeFilter.ALL }
                        )
                    }
                    if (selectedGroupId != null) {
                        val gName = groupMap[selectedGroupId]?.name ?: "Group"
                        ActiveFilterPill(
                            label = "Group: $gName",
                            onRemove = { selectedGroupId = null }
                        )
                    }
                    if (selectedPayer != PayerFilter.ALL) {
                        ActiveFilterPill(
                            label = selectedPayer.label,
                            onRemove = { selectedPayer = PayerFilter.ALL }
                        )
                    }
                    if (selectedTimeframe != TimeframeFilter.ALL_TIME) {
                        ActiveFilterPill(
                            label = selectedTimeframe.label,
                            onRemove = { selectedTimeframe = TimeframeFilter.ALL_TIME }
                        )
                    }
                    if (selectedAmountRange != AmountRangeFilter.ALL) {
                        ActiveFilterPill(
                            label = selectedAmountRange.label,
                            onRemove = { selectedAmountRange = AmountRangeFilter.ALL }
                        )
                    }
                    if (searchQuery.isNotBlank()) {
                        ActiveFilterPill(
                            label = "\"$searchQuery\"",
                            onRemove = { searchQuery = "" }
                        )
                    }

                    TextButton(
                        onClick = { resetAllFilters() },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text("Reset All", color = PurplePrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // KPI Summary Cards Row
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Total Spend Card
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = PurpleContainer),
                    border = BorderStroke(1.dp, PurpleSecondaryContainer)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "TOTAL SPENT",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = PurplePrimaryDark,
                            letterSpacing = 0.5.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "$${String.format(Locale.US, "%.2f", totalFilteredSpend)}",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = PurplePrimaryDark
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "${filteredExpenses.size} items matched",
                            style = MaterialTheme.typography.bodySmall,
                            color = PurplePrimaryDark.copy(alpha = 0.7f)
                        )
                    }
                }

                // Average & Top Category Card
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "TOP CATEGORY",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = TextMuted,
                            letterSpacing = 0.5.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        val topCat = categoryBreakdown.firstOrNull()
                        Text(
                            text = topCat?.category?.lowercase()?.replaceFirstChar { it.uppercase() } ?: "None",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextDark,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (topCat != null) "${(topCat.percentage * 100).toInt()}% of total" else "Avg $0.00",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMuted
                        )
                    }
                }
            }
        }

        // View Mode Switch Tabs (Category Breakdown vs Itemized Expenses)
        item {
            PrimaryTabRow(
                selectedTabIndex = selectedViewMode,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = PurplePrimary
            ) {
                Tab(
                    selected = selectedViewMode == 0,
                    onClick = { selectedViewMode = 0 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.PieChart, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Category Chart", fontWeight = FontWeight.Bold)
                        }
                    }
                )
                Tab(
                    selected = selectedViewMode == 1,
                    onClick = { selectedViewMode = 1 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.ReceiptLong, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Expenses (${filteredExpenses.size})", fontWeight = FontWeight.Bold)
                        }
                    }
                )
            }
        }

        // VIEW MODE 0: CATEGORY BREAKDOWN & DONUT CHART
        if (selectedViewMode == 0) {
            // Category Donut Visualization Card
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
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Spending Distribution",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        if (categoryBreakdown.isEmpty()) {
                            Text(
                                text = "No expenses match the current filters.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextMuted,
                                modifier = Modifier.padding(vertical = 24.dp)
                            )
                        } else {
                            // Donut Chart
                            Box(
                                modifier = Modifier.size(180.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CategoryDonutChart(
                                    categories = categoryBreakdown,
                                    modifier = Modifier.fillMaxSize()
                                )

                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = "$${String.format(Locale.US, "%.0f", totalFilteredSpend)}",
                                        style = MaterialTheme.typography.headlineSmall,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = PurplePrimaryDark
                                    )
                                    Text(
                                        text = "${categoryBreakdown.size} categories",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TextMuted
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Category Details List
            if (categoryBreakdown.isNotEmpty()) {
                item {
                    Text(
                        text = "Category Drilldown",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                items(categoryBreakdown) { stat ->
                    CategoryStatCard(
                        stat = stat,
                        isSelected = selectedCategory.equals(stat.category, ignoreCase = true),
                        onClick = {
                            selectedCategory = if (selectedCategory.equals(stat.category, ignoreCase = true)) null else stat.category
                        }
                    )
                }
            }
        }

        // VIEW MODE 1: ITEMIZED FILTERED EXPENSES
        if (selectedViewMode == 1) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Matching Expenses (${filteredExpenses.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Sort,
                            contentDescription = "Sort",
                            tint = TextMuted,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = sortBy.label,
                            style = MaterialTheme.typography.bodySmall,
                            color = PurplePrimary,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.clickable {
                                sortBy = when (sortBy) {
                                    ExpenseSortOption.DATE_DESC -> ExpenseSortOption.AMOUNT_DESC
                                    ExpenseSortOption.AMOUNT_DESC -> ExpenseSortOption.AMOUNT_ASC
                                    ExpenseSortOption.AMOUNT_ASC -> ExpenseSortOption.CATEGORY_ASC
                                    ExpenseSortOption.CATEGORY_ASC -> ExpenseSortOption.DATE_ASC
                                    ExpenseSortOption.DATE_ASC -> ExpenseSortOption.DATE_DESC
                                }
                            }
                        )
                    }
                }
            }

            if (filteredExpenses.isEmpty()) {
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
                                imageVector = Icons.Outlined.FilterAlt,
                                contentDescription = null,
                                tint = TextMuted,
                                modifier = Modifier.size(44.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "No expenses match your filters",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Try adjusting your category, timeframe, or amount parameters.",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextMuted,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = { resetAllFilters() },
                                colors = ButtonDefaults.buttonColors(containerColor = PurplePrimary)
                            ) {
                                Text("Reset All Filters")
                            }
                        }
                    }
                }
            } else {
                items(filteredExpenses, key = { it.id }) { exp ->
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
        }

        item {
            Spacer(modifier = Modifier.height(40.dp))
        }
    }

    // MULTI-FILTER MODAL BOTTOM SHEET
    if (isFilterSheetOpen) {
        ModalBottomSheet(
            onDismissRequest = { isFilterSheetOpen = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            FilterPanelSheetContent(
                categories = ALL_KNOWN_CATEGORIES,
                groups = groups,
                selectedCategory = selectedCategory,
                selectedScope = selectedScope,
                selectedGroupId = selectedGroupId,
                selectedPayer = selectedPayer,
                selectedTimeframe = selectedTimeframe,
                selectedAmountRange = selectedAmountRange,
                sortBy = sortBy,
                matchingCount = filteredExpenses.size,
                onCategoryChange = { selectedCategory = it },
                onScopeChange = { selectedScope = it },
                onGroupChange = { selectedGroupId = it },
                onPayerChange = { selectedPayer = it },
                onTimeframeChange = { selectedTimeframe = it },
                onAmountRangeChange = { selectedAmountRange = it },
                onSortChange = { sortBy = it },
                onReset = { resetAllFilters() },
                onApply = { isFilterSheetOpen = false }
            )
        }
    }

    // EXPORT REPORT BOTTOM SHEET
    if (isExportSheetOpen) {
        ExportReportBottomSheet(
            allExpenses = expenses,
            filteredExpenses = filteredExpenses,
            users = users,
            groups = groups,
            onDismiss = { isExportSheetOpen = false }
        )
    }
}

data class CategoryStat(
    val category: String,
    val totalAmount: Double,
    val count: Int,
    val maxAmount: Double,
    val avgAmount: Double,
    val percentage: Float
)

@Composable
fun CategoryDonutChart(
    categories: List<CategoryStat>,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val strokeWidth = 36.dp.toPx()
        val diameter = size.minDimension - strokeWidth
        val topLeft = Offset(
            (size.width - diameter) / 2,
            (size.height - diameter) / 2
        )
        val arcSize = Size(diameter, diameter)

        var startAngle = -90f
        categories.forEach { stat ->
            val sweepAngle = (stat.percentage * 360f).coerceAtLeast(2f)
            val visual = getCategoryVisual(stat.category)

            drawArc(
                color = visual.iconColor,
                startAngle = startAngle,
                sweepAngle = sweepAngle,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Butt)
            )
            startAngle += sweepAngle
        }
    }
}

@Composable
fun CategoryStatCard(
    stat: CategoryStat,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val visual = getCategoryVisual(stat.category)
    val pctInt = (stat.percentage * 100).toInt()

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) PurpleContainer else MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(
            1.dp,
            if (isSelected) PurplePrimary else MaterialTheme.colorScheme.surfaceVariant
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CategoryIcon(category = stat.category, size = 38.dp)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = stat.category.lowercase().replaceFirstChar { it.uppercase() },
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = TextDark
                        )
                        Text(
                            text = "${stat.count} transaction${if (stat.count > 1) "s" else ""} • Avg $${String.format(Locale.US, "%.2f", stat.avgAmount)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMuted
                        )
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "$${String.format(Locale.US, "%.2f", stat.totalAmount)}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = PurplePrimaryDark
                    )
                    Text(
                        text = "$pctInt% of filtered",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = TextMuted
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            LinearProgressIndicator(
                progress = { stat.percentage },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = visual.iconColor,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        }
    }
}

@Composable
fun ActiveFilterPill(
    label: String,
    onRemove: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = PurpleContainer,
        border = BorderStroke(0.5.dp, PurpleSecondaryContainer)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = PurplePrimaryDark,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.width(4.dp))
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Remove filter",
                tint = PurplePrimaryDark,
                modifier = Modifier
                    .size(14.dp)
                    .clickable { onRemove() }
            )
        }
    }
}

@Composable
fun FilledIconButtonCustom(
    onClick: () -> Unit,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    isActive: Boolean
) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = if (isActive) PurplePrimary else MaterialTheme.colorScheme.surfaceVariant,
        contentColor = if (isActive) Color.White else TextDark,
        modifier = Modifier.size(42.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FilterPanelSheetContent(
    categories: List<String>,
    groups: List<GroupEntity>,
    selectedCategory: String?,
    selectedScope: ExpenseScopeFilter,
    selectedGroupId: Long?,
    selectedPayer: PayerFilter,
    selectedTimeframe: TimeframeFilter,
    selectedAmountRange: AmountRangeFilter,
    sortBy: ExpenseSortOption,
    matchingCount: Int,
    onCategoryChange: (String?) -> Unit,
    onScopeChange: (ExpenseScopeFilter) -> Unit,
    onGroupChange: (Long?) -> Unit,
    onPayerChange: (PayerFilter) -> Unit,
    onTimeframeChange: (TimeframeFilter) -> Unit,
    onAmountRangeChange: (AmountRangeFilter) -> Unit,
    onSortChange: (ExpenseSortOption) -> Unit,
    onReset: () -> Unit,
    onApply: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .padding(bottom = 28.dp)
    ) {
        // Sheet Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Filter Expenses",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            TextButton(onClick = onReset) {
                Text("Reset All", color = PurplePrimary, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f, fill = false),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Expense Scope / Type
            item {
                Text(
                    text = "Expense Type",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    ExpenseScopeFilter.values().forEach { scope ->
                        FilterChip(
                            selected = selectedScope == scope,
                            onClick = { onScopeChange(scope) },
                            label = { Text(scope.label) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = PurpleContainer,
                                selectedLabelColor = PurplePrimaryDark
                            )
                        )
                    }
                }
            }

            // 2. Categories
            item {
                Text(
                    text = "Category",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FilterChip(
                        selected = selectedCategory == null,
                        onClick = { onCategoryChange(null) },
                        label = { Text("All Categories") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = PurpleContainer,
                            selectedLabelColor = PurplePrimaryDark
                        )
                    )

                    categories.forEach { cat ->
                        val isSelected = selectedCategory.equals(cat, ignoreCase = true)
                        FilterChip(
                            selected = isSelected,
                            onClick = { onCategoryChange(if (isSelected) null else cat) },
                            label = { Text(cat.lowercase().replaceFirstChar { it.uppercase() }) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = PurpleContainer,
                                selectedLabelColor = PurplePrimaryDark
                            )
                        )
                    }
                }
            }

            // 3. Timeframe
            item {
                Text(
                    text = "Time Period",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    TimeframeFilter.values().forEach { tf ->
                        FilterChip(
                            selected = selectedTimeframe == tf,
                            onClick = { onTimeframeChange(tf) },
                            label = { Text(tf.label) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = PurpleContainer,
                                selectedLabelColor = PurplePrimaryDark
                            )
                        )
                    }
                }
            }

            // 4. Amount Range
            item {
                Text(
                    text = "Amount Range",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    AmountRangeFilter.values().forEach { range ->
                        FilterChip(
                            selected = selectedAmountRange == range,
                            onClick = { onAmountRangeChange(range) },
                            label = { Text(range.label) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = PurpleContainer,
                                selectedLabelColor = PurplePrimaryDark
                            )
                        )
                    }
                }
            }

            // 5. Payer
            item {
                Text(
                    text = "Paid By",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    PayerFilter.values().forEach { payer ->
                        FilterChip(
                            selected = selectedPayer == payer,
                            onClick = { onPayerChange(payer) },
                            label = { Text(payer.label) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = PurpleContainer,
                                selectedLabelColor = PurplePrimaryDark
                            )
                        )
                    }
                }
            }

            // 6. Group Filter (if groups exist)
            if (groups.isNotEmpty()) {
                item {
                    Text(
                        text = "Group",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        FilterChip(
                            selected = selectedGroupId == null,
                            onClick = { onGroupChange(null) },
                            label = { Text("All Groups") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = PurpleContainer,
                                selectedLabelColor = PurplePrimaryDark
                            )
                        )

                        groups.forEach { grp ->
                            val isSelected = selectedGroupId == grp.id
                            FilterChip(
                                selected = isSelected,
                                onClick = { onGroupChange(if (isSelected) null else grp.id) },
                                label = { Text(grp.name) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = PurpleContainer,
                                    selectedLabelColor = PurplePrimaryDark
                                )
                            )
                        }
                    }
                }
            }

            // 7. Sort By
            item {
                Text(
                    text = "Sort Order",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    ExpenseSortOption.values().forEach { sort ->
                        FilterChip(
                            selected = sortBy == sort,
                            onClick = { onSortChange(sort) },
                            label = { Text(sort.label) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = PurpleContainer,
                                selectedLabelColor = PurplePrimaryDark
                            )
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Apply Button
        Button(
            onClick = onApply,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .testTag("btn_apply_filters"),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = PurplePrimary)
        ) {
            Text(
                text = "Apply Filters ($matchingCount matching)",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
