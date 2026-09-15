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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.CallSplit
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.Schedule
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.remote.FmsClientManager
import com.example.data.remote.dto.CreateLendBorrowDto
import com.example.data.remote.dto.LendBorrowResponseDto
import com.example.data.remote.dto.RecordPaymentDto
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LendBorrowScreen(
    clientManager: FmsClientManager,
    onBack: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    var itemsList by remember {
        mutableStateOf<List<LendBorrowResponseDto>>(
            listOf(
                LendBorrowResponseDto(
                    id = "lb-1",
                    type = "lend",
                    personName = "Alex Chen",
                    amount = 120.0,
                    date = "2026-09-10",
                    dueDate = "2026-09-25",
                    description = "Concert tickets advance",
                    isSettled = false,
                    paidAmount = 0.0
                ),
                LendBorrowResponseDto(
                    id = "lb-2",
                    type = "borrow",
                    personName = "Sarah Connor",
                    amount = 60.0,
                    date = "2026-09-12",
                    dueDate = "2026-09-20",
                    description = "Lunch bill payback",
                    isSettled = false,
                    paidAmount = 20.0
                ),
                LendBorrowResponseDto(
                    id = "lb-3",
                    type = "lend",
                    personName = "David Kim",
                    amount = 45.0,
                    date = "2026-09-01",
                    dueDate = "2026-09-15",
                    description = "Uber ride split share",
                    isSettled = true,
                    paidAmount = 45.0
                )
            )
        )
    }

    var selectedFilter by remember { mutableStateOf("All") }
    var isAddDialogOpen by remember { mutableStateOf(false) }
    var paymentDialogItem by remember { mutableStateOf<LendBorrowResponseDto?>(null) }

    // Load from remote backend
    LaunchedEffect(Unit) {
        val service = clientManager.getService()
        if (service != null) {
            try {
                val res = service.getLendBorrow()
                if (res.isSuccessful && res.body() != null && res.body()!!.isNotEmpty()) {
                    itemsList = res.body()!!
                }
            } catch (_: Exception) { }
        }
    }

    val activeItems = itemsList.filter { it.isSettled != true }
    val totalLent = activeItems.filter { it.type.lowercase() == "lend" }
        .sumOf { it.amount - (it.paidAmount ?: 0.0) }
    val totalBorrowed = activeItems.filter { it.type.lowercase() == "borrow" }
        .sumOf { it.amount - (it.paidAmount ?: 0.0) }
    val netBalance = totalLent - totalBorrowed

    val filteredList = itemsList.filter { item ->
        when (selectedFilter) {
            "Lent" -> item.type.lowercase() == "lend"
            "Borrowed" -> item.type.lowercase() == "borrow"
            "Pending" -> item.isSettled != true
            "Settled" -> item.isSettled == true
            else -> true
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
                .testTag("lend_borrow_screen"),
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
                            modifier = Modifier.testTag("lend_borrow_back_button")
                        ) {
                            Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                    Column {
                        Text(
                            text = "Lend & Borrow",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = "Track personal loans, due dates and repayments",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMuted
                        )
                    }
                }
            }

            // Summary Card
            item {
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("lend_borrow_summary_card")
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Text(
                            text = "Net Outstanding Balance",
                            style = MaterialTheme.typography.labelMedium,
                            color = TextMuted
                        )
                        Text(
                            text = String.format(Locale.US, "%s$%.2f", if (netBalance >= 0) "+" else "-", kotlin.math.abs(netBalance)),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (netBalance >= 0) PositiveGreen else NegativeRed
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = PositiveGreenBg,
                                modifier = Modifier.weight(1f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ArrowUpward,
                                        contentDescription = null,
                                        tint = PositiveGreen,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(text = "You are Owed", fontSize = 11.sp, color = PositiveGreen)
                                        Text(
                                            text = String.format(Locale.US, "$%.2f", totalLent),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp,
                                            color = PositiveGreen
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = NegativeRedBg,
                                modifier = Modifier.weight(1f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ArrowDownward,
                                        contentDescription = null,
                                        tint = NegativeRed,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(text = "You Owe", fontSize = 11.sp, color = NegativeRed)
                                        Text(
                                            text = String.format(Locale.US, "$%.2f", totalBorrowed),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp,
                                            color = NegativeRed
                                        )
                                    }
                                }
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
                    listOf("All", "Pending", "Lent", "Borrowed", "Settled").forEach { filter ->
                        FilterChip(
                            selected = selectedFilter == filter,
                            onClick = { selectedFilter = filter },
                            label = { Text(filter, fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = PurpleContainer,
                                selectedLabelColor = PurplePrimaryDark
                            )
                        )
                    }
                }
            }

            // List of items
            items(filteredList, key = { it.id }) { item ->
                val isLend = item.type.lowercase() == "lend"
                val isSettled = item.isSettled == true
                val remainingAmount = item.amount - (item.paidAmount ?: 0.0)

                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("lend_borrow_item_${item.id}")
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
                                    color = if (isSettled) MaterialTheme.colorScheme.surfaceVariant else if (isLend) PositiveGreenBg else NegativeRedBg,
                                    modifier = Modifier.size(42.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = if (isSettled) Icons.Default.CheckCircle else if (isLend) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                                            contentDescription = null,
                                            tint = if (isSettled) TextMuted else if (isLend) PositiveGreen else NegativeRed,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column {
                                    Text(
                                        text = item.personName,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = if (isLend) "You lent" else "You borrowed",
                                        fontSize = 12.sp,
                                        color = TextMuted
                                    )
                                }
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = String.format(Locale.US, "$%.2f", item.amount),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSettled) TextMuted else if (isLend) PositiveGreen else NegativeRed
                                )
                                if (isSettled) {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = PositiveGreenBg
                                    ) {
                                        Text(
                                            text = "Settled",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = PositiveGreen,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                } else if ((item.paidAmount ?: 0.0) > 0.0) {
                                    Text(
                                        text = String.format(Locale.US, "Paid $%.2f", item.paidAmount),
                                        fontSize = 11.sp,
                                        color = TextMuted
                                    )
                                }
                            }
                        }

                        if (!item.description.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = item.description,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        if (!item.dueDate.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Schedule,
                                    contentDescription = null,
                                    tint = WarningAmber,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Due: ${item.dueDate}",
                                    fontSize = 11.sp,
                                    color = WarningAmber
                                )
                            }
                        }

                        if (!isSettled) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                OutlinedButton(
                                    onClick = { paymentDialogItem = item },
                                    modifier = Modifier.testTag("btn_record_payment_${item.id}")
                                ) {
                                    Icon(imageVector = Icons.Default.Payment, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Partial Pay", fontSize = 12.sp)
                                }

                                Spacer(modifier = Modifier.width(8.dp))

                                Button(
                                    onClick = {
                                        itemsList = itemsList.map {
                                            if (it.id == item.id) it.copy(isSettled = true, paidAmount = it.amount) else it
                                        }
                                        coroutineScope.launch {
                                            try {
                                                clientManager.getService()?.settleLendBorrow(item.id)
                                            } catch (_: Exception) { }
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = PositiveGreen),
                                    modifier = Modifier.testTag("btn_settle_${item.id}")
                                ) {
                                    Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Settle Full", fontSize = 12.sp)
                                }
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
                .testTag("fab_add_lend_borrow")
        ) {
            Icon(imageVector = Icons.Default.Add, contentDescription = "Add Record")
        }
    }

    if (isAddDialogOpen) {
        AddLendBorrowDialog(
            onDismiss = { isAddDialogOpen = false },
            onAdd = { type, person, amount, dueDate, desc ->
                val newItem = LendBorrowResponseDto(
                    id = "lb-${System.currentTimeMillis()}",
                    type = type,
                    personName = person,
                    amount = amount,
                    date = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date()),
                    dueDate = dueDate,
                    description = desc,
                    isSettled = false,
                    paidAmount = 0.0
                )
                itemsList = listOf(newItem) + itemsList
                isAddDialogOpen = false
                coroutineScope.launch {
                    try {
                        clientManager.getService()?.createLendBorrow(
                            CreateLendBorrowDto(
                                type = type,
                                personName = person,
                                amount = amount,
                                date = newItem.date,
                                dueDate = dueDate,
                                description = desc
                            )
                        )
                    } catch (_: Exception) { }
                }
            }
        )
    }

    paymentDialogItem?.let { targetItem ->
        RecordPaymentDialog(
            item = targetItem,
            onDismiss = { paymentDialogItem = null },
            onConfirmPayment = { paid ->
                val newPaid = ((targetItem.paidAmount ?: 0.0) + paid).coerceAtMost(targetItem.amount)
                val settled = newPaid >= targetItem.amount
                itemsList = itemsList.map {
                    if (it.id == targetItem.id) it.copy(paidAmount = newPaid, isSettled = settled) else it
                }
                paymentDialogItem = null
                coroutineScope.launch {
                    try {
                        clientManager.getService()?.recordLendBorrowPayment(
                            targetItem.id,
                            RecordPaymentDto(amount = paid)
                        )
                    } catch (_: Exception) { }
                }
            }
        )
    }
}

@Composable
fun AddLendBorrowDialog(
    onDismiss: () -> Unit,
    onAdd: (type: String, person: String, amount: Double, dueDate: String, desc: String) -> Unit
) {
    var type by remember { mutableStateOf("lend") } // lend, borrow
    var person by remember { mutableStateOf("") }
    var amountText by remember { mutableStateOf("") }
    var dueDateText by remember { mutableStateOf("") }
    var descText by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .testTag("add_lend_borrow_dialog")
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(
                    text = "New Lend / Borrow Record",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(modifier = Modifier.fillMaxWidth()) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (type == "lend") PositiveGreenBg else MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { type = "lend" }
                    ) {
                        Text(
                            text = "I Lent (+)",
                            fontWeight = FontWeight.Bold,
                            color = if (type == "lend") PositiveGreen else TextMuted,
                            modifier = Modifier.padding(vertical = 10.dp),
                            fontSize = 14.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (type == "borrow") NegativeRedBg else MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { type = "borrow" }
                    ) {
                        Text(
                            text = "I Borrowed (-)",
                            fontWeight = FontWeight.Bold,
                            color = if (type == "borrow") NegativeRed else TextMuted,
                            modifier = Modifier.padding(vertical = 10.dp),
                            fontSize = 14.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = person,
                    onValueChange = { person = it },
                    label = { Text("Person Name") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_lb_person"),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("Amount ($)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_lb_amount"),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = dueDateText,
                    onValueChange = { dueDateText = it },
                    label = { Text("Due Date (e.g. 2026-10-01)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = descText,
                    onValueChange = { descText = it },
                    label = { Text("Note / Description") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

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
                            if (amt > 0 && person.isNotBlank()) {
                                onAdd(type, person, amt, dueDateText, descText)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PurplePrimary),
                        modifier = Modifier.testTag("btn_save_lb")
                    ) {
                        Text("Save Record")
                    }
                }
            }
        }
    }
}

@Composable
fun RecordPaymentDialog(
    item: LendBorrowResponseDto,
    onDismiss: () -> Unit,
    onConfirmPayment: (Double) -> Unit
) {
    val remaining = item.amount - (item.paidAmount ?: 0.0)
    var amountText by remember { mutableStateOf(String.format(Locale.US, "%.2f", remaining)) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(
                    text = "Record Payment",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Payee: ${item.personName} • Outstanding: $${String.format(Locale.US, "%.2f", remaining)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("Payment Amount ($)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    OutlinedButton(onClick = onDismiss) { Text("Cancel") }
                    Spacer(modifier = Modifier.width(10.dp))
                    Button(
                        onClick = {
                            val p = amountText.toDoubleOrNull() ?: 0.0
                            if (p > 0) onConfirmPayment(p)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PositiveGreen)
                    ) {
                        Text("Confirm Payment")
                    }
                }
            }
        }
    }
}
