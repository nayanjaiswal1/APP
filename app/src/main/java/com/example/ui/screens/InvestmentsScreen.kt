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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CurrencyBitcoin
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.remote.FmsClientManager
import com.example.data.remote.dto.CreateInvestmentDto
import com.example.data.remote.dto.InvestmentResponseDto
import com.example.ui.theme.NegativeRed
import com.example.ui.theme.NegativeRedBg
import com.example.ui.theme.PositiveGreen
import com.example.ui.theme.PositiveGreenBg
import com.example.ui.theme.PurpleContainer
import com.example.ui.theme.PurplePrimary
import com.example.ui.theme.PurplePrimaryDark
import com.example.ui.theme.TextMuted
import kotlinx.coroutines.launch
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InvestmentsScreen(
    clientManager: FmsClientManager,
    onBack: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    var investmentsList by remember {
        mutableStateOf<List<InvestmentResponseDto>>(
            listOf(
                InvestmentResponseDto(
                    id = "inv-1",
                    name = "Vanguard S&P 500 ETF (VOO)",
                    type = "Stocks",
                    amount = 3500.0,
                    units = 7.5,
                    buyPrice = 466.66,
                    currentPrice = 512.20,
                    institution = "Vanguard",
                    profitLoss = 341.50,
                    profitLossPercentage = 9.75
                ),
                InvestmentResponseDto(
                    id = "inv-2",
                    name = "Apple Inc. (AAPL)",
                    type = "Stocks",
                    amount = 2200.0,
                    units = 10.0,
                    buyPrice = 220.0,
                    currentPrice = 238.40,
                    institution = "Charles Schwab",
                    profitLoss = 184.00,
                    profitLossPercentage = 8.36
                ),
                InvestmentResponseDto(
                    id = "inv-3",
                    name = "Bitcoin (BTC)",
                    type = "Crypto",
                    amount = 1500.0,
                    units = 0.024,
                    buyPrice = 62500.0,
                    currentPrice = 68400.0,
                    institution = "Coinbase",
                    profitLoss = 141.60,
                    profitLossPercentage = 9.44
                ),
                InvestmentResponseDto(
                    id = "inv-4",
                    name = "High Yield Certificate of Deposit",
                    type = "Fixed Deposit",
                    amount = 5000.0,
                    units = 1.0,
                    buyPrice = 5000.0,
                    currentPrice = 5240.0,
                    institution = "Marcus by Goldman Sachs",
                    profitLoss = 240.00,
                    profitLossPercentage = 4.80
                )
            )
        )
    }

    var selectedTypeFilter by remember { mutableStateOf("All") }
    var isAddDialogOpen by remember { mutableStateOf(false) }

    // Fetch from backend
    LaunchedEffect(Unit) {
        val service = clientManager.getService()
        if (service != null) {
            try {
                val res = service.getInvestments()
                if (res.isSuccessful && res.body() != null && res.body()!!.isNotEmpty()) {
                    investmentsList = res.body()!!
                }
            } catch (_: Exception) { }
        }
    }

    val totalInvested = investmentsList.sumOf { it.amount }
    val totalCurrentValue = investmentsList.sumOf { it.effectiveCurrentValue }
    val totalProfitLoss = totalCurrentValue - totalInvested
    val totalReturnPercentage = if (totalInvested > 0) (totalProfitLoss / totalInvested) * 100 else 0.0

    val filteredList = investmentsList.filter { item ->
        if (selectedTypeFilter == "All") true else item.type.equals(selectedTypeFilter, ignoreCase = true)
    }

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
                .testTag("investments_screen"),
            verticalArrangement = Arrangement.spacedBy(14.dp)
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
                            modifier = Modifier.testTag("investments_back_button")
                        ) {
                            Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                    Column {
                        Text(
                            text = "Investment Portfolio",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = "Asset allocation, returns, and performance tracking",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMuted
                        )
                    }
                }
            }

            // Portfolio Net Worth Card
            item {
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("portfolio_summary_card")
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(text = "Total Portfolio Value", style = MaterialTheme.typography.labelMedium, color = TextMuted)
                                Text(
                                    text = String.format(Locale.US, "$%.2f", totalCurrentValue),
                                    style = MaterialTheme.typography.headlineLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                            }

                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (totalProfitLoss >= 0) PositiveGreenBg else NegativeRedBg
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = if (totalProfitLoss >= 0) Icons.AutoMirrored.Filled.TrendingUp else Icons.AutoMirrored.Filled.TrendingDown,
                                        contentDescription = null,
                                        tint = if (totalProfitLoss >= 0) PositiveGreen else NegativeRed,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = String.format(Locale.US, "%s%.2f%%", if (totalReturnPercentage >= 0) "+" else "", totalReturnPercentage),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = if (totalProfitLoss >= 0) PositiveGreen else NegativeRed
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
                                Text(text = "Invested Capital", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                                Text(
                                    text = String.format(Locale.US, "$%.2f", totalInvested),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(text = "Total Gain / Loss", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                                Text(
                                    text = String.format(Locale.US, "%s$%.2f", if (totalProfitLoss >= 0) "+" else "-", kotlin.math.abs(totalProfitLoss)),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (totalProfitLoss >= 0) PositiveGreen else NegativeRed
                                )
                            }
                        }
                    }
                }
            }

            // Filters
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("All", "Stocks", "Crypto", "Fixed Deposit").forEach { filter ->
                        FilterChip(
                            selected = selectedTypeFilter == filter,
                            onClick = { selectedTypeFilter = filter },
                            label = { Text(filter, fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = PurpleContainer,
                                selectedLabelColor = PurplePrimaryDark
                            )
                        )
                    }
                }
            }

            // Holdings List
            items(filteredList, key = { it.id }) { item ->
                val pl = (item.effectiveCurrentValue - item.amount)
                val isProfit = pl >= 0

                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("investment_item_${item.id}")
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    shape = CircleShape,
                                    color = PurpleContainer,
                                    modifier = Modifier.size(42.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = if (item.type.equals("Crypto", ignoreCase = true)) Icons.Default.CurrencyBitcoin else Icons.Default.ShowChart,
                                            contentDescription = null,
                                            tint = PurplePrimaryDark,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column {
                                    Text(
                                        text = item.name,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "${item.type} • ${item.institution ?: "Portfolio"}",
                                        fontSize = 12.sp,
                                        color = TextMuted
                                    )
                                }
                            }

                            IconButton(
                                onClick = {
                                    investmentsList = investmentsList.filterNot { it.id == item.id }
                                    coroutineScope.launch {
                                        try {
                                            clientManager.getService()?.deleteInvestment(item.id)
                                        } catch (_: Exception) { }
                                    }
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete", tint = TextMuted, modifier = Modifier.size(16.dp))
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = String.format(Locale.US, "%.3f units @ $%.2f", item.units ?: 1.0, item.buyPrice ?: item.amount),
                                    fontSize = 11.sp,
                                    color = TextMuted
                                )
                                Text(
                                    text = String.format(Locale.US, "Current: $%.2f", item.effectiveCurrentValue),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text(text = "Gain / Loss", fontSize = 11.sp, color = TextMuted)
                                Text(
                                    text = String.format(Locale.US, "%s$%.2f", if (isProfit) "+" else "-", kotlin.math.abs(pl)),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = if (isProfit) PositiveGreen else NegativeRed
                                )
                            }
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
                .testTag("fab_add_investment")
        ) {
            Icon(imageVector = Icons.Default.Add, contentDescription = "Add Investment")
        }
    }

    if (isAddDialogOpen) {
        AddInvestmentDialog(
            onDismiss = { isAddDialogOpen = false },
            onAdd = { name, type, amount, units, currentPrice, institution ->
                val newItem = InvestmentResponseDto(
                    id = "inv-${System.currentTimeMillis()}",
                    name = name,
                    type = type,
                    amount = amount,
                    units = units,
                    buyPrice = if (units > 0) amount / units else amount,
                    currentPrice = currentPrice,
                    institution = institution,
                    profitLoss = (currentPrice * units) - amount
                )
                investmentsList = listOf(newItem) + investmentsList
                isAddDialogOpen = false
                coroutineScope.launch {
                    try {
                        clientManager.getService()?.createInvestment(
                            CreateInvestmentDto(
                                name = name,
                                type = type,
                                amount = amount,
                                units = units,
                                currentPrice = currentPrice,
                                institution = institution
                            )
                        )
                    } catch (_: Exception) { }
                }
            }
        )
    }
}

@Composable
fun AddInvestmentDialog(
    onDismiss: () -> Unit,
    onAdd: (name: String, type: String, amount: Double, units: Double, currentPrice: Double, institution: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var type by remember { mutableStateOf("Stocks") }
    var amountText by remember { mutableStateOf("") }
    var unitsText by remember { mutableStateOf("1.0") }
    var currentPriceText by remember { mutableStateOf("") }
    var institutionText by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .testTag("add_investment_dialog")
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(
                    text = "Add Holding / Investment",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Asset Name (e.g. S&P 500, Apple)") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_inv_name"),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = type,
                    onValueChange = { type = it },
                    label = { Text("Type (Stocks, Crypto, Mutual Fund, Gold)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = amountText,
                        onValueChange = { amountText = it },
                        label = { Text("Total Invested ($)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("input_inv_amount"),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    OutlinedTextField(
                        value = unitsText,
                        onValueChange = { unitsText = it },
                        label = { Text("Units / Qty") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = currentPriceText,
                        onValueChange = { currentPriceText = it },
                        label = { Text("Current Unit Price ($)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    OutlinedTextField(
                        value = institutionText,
                        onValueChange = { institutionText = it },
                        label = { Text("Broker / Platform") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    OutlinedButton(onClick = onDismiss) { Text("Cancel") }
                    Spacer(modifier = Modifier.width(10.dp))
                    Button(
                        onClick = {
                            val amt = amountText.toDoubleOrNull() ?: 0.0
                            val u = unitsText.toDoubleOrNull() ?: 1.0
                            val cp = currentPriceText.toDoubleOrNull() ?: (if (u > 0) amt / u else amt)
                            if (amt > 0 && name.isNotBlank()) {
                                onAdd(name, type, amt, u, cp, institutionText)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PurplePrimary),
                        modifier = Modifier.testTag("btn_save_investment")
                    ) {
                        Text("Save Asset")
                    }
                }
            }
        }
    }
}
