package com.example.ui.screens

import android.widget.Toast
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Repeat
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
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.remote.FmsApiResult
import com.example.data.remote.FmsClientManager
import com.example.data.remote.dto.CreateReminderDto
import com.example.data.remote.dto.ReminderResponseDto
import com.example.ui.components.BackendStatusBanner
import com.example.ui.components.CategoryIcon
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
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RemindersScreen(
    clientManager: FmsClientManager,
    onBack: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    var remindersList by remember {
        mutableStateOf<List<ReminderResponseDto>>(
            listOf(
                ReminderResponseDto(
                    id = "rem-1",
                    title = "Wifi & Fiber Internet",
                    amount = 65.0,
                    dueDate = "2026-09-18",
                    frequency = "monthly",
                    category = "Utilities",
                    notes = "Auto-debit from checking account",
                    isCompleted = false
                ),
                ReminderResponseDto(
                    id = "rem-2",
                    title = "Electricity & Power Bill",
                    amount = 112.50,
                    dueDate = "2026-09-22",
                    frequency = "monthly",
                    category = "Utilities",
                    notes = "City Power provider",
                    isCompleted = false
                ),
                ReminderResponseDto(
                    id = "rem-3",
                    title = "HDFC Credit Card Statement",
                    amount = 450.0,
                    dueDate = "2026-09-28",
                    frequency = "monthly",
                    category = "Financial",
                    notes = "Pay statement balance in full",
                    isCompleted = false
                ),
                ReminderResponseDto(
                    id = "rem-4",
                    title = "Netflix 4K Subscription",
                    amount = 19.99,
                    dueDate = "2026-09-14",
                    frequency = "monthly",
                    category = "Entertainment",
                    notes = "Family plan",
                    isCompleted = true
                )
            )
        )
    }

    var selectedFilter by remember { mutableStateOf("Upcoming") }
    var isAddDialogOpen by remember { mutableStateOf(false) }

    // Fetch from backend
    LaunchedEffect(Unit) {
        val res = clientManager.executeSafely("Fetch Reminders") { service ->
            service.getReminders()
        }
        if (res is FmsApiResult.Success && res.data.isNotEmpty()) {
            remindersList = res.data
        }
    }

    val pendingReminders = remindersList.filter { it.isCompleted != true }
    val totalPendingAmount = pendingReminders.sumOf { it.amount }

    val filteredList = remindersList.filter { item ->
        when (selectedFilter) {
            "Upcoming" -> item.isCompleted != true
            "Completed" -> item.isCompleted == true
            else -> true
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
                .testTag("reminders_screen"),
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
                            modifier = Modifier.testTag("reminders_back_button")
                        ) {
                            Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                    Column {
                        Text(
                            text = "Bills & Reminders",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = "Never miss a credit card bill or subscription payment",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMuted
                        )
                    }
                }
            }

            // Backend Status Banner
            item {
                BackendStatusBanner(
                    clientManager = clientManager,
                    onResolveConflict = { conflict, action ->
                        if (action == com.example.ui.components.ConflictResolutionAction.USE_SERVER_VERSION) {
                            coroutineScope.launch {
                                val res = clientManager.executeSafely("Refresh Reminders") { it.getReminders() }
                                if (res is FmsApiResult.Success) {
                                    remindersList = res.data
                                }
                            }
                        }
                    }
                )
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
                        .testTag("reminders_summary_card")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(text = "Upcoming Bills Due", style = MaterialTheme.typography.labelMedium, color = TextMuted)
                            Text(
                                text = String.format(Locale.US, "$%.2f", totalPendingAmount),
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Text(
                                text = "${pendingReminders.size} payments pending this cycle",
                                fontSize = 12.sp,
                                color = TextMuted
                            )
                        }

                        Surface(
                            shape = CircleShape,
                            color = WarningAmberBg,
                            modifier = Modifier.size(52.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.NotificationsActive,
                                    contentDescription = null,
                                    tint = WarningAmber,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Filter Chips
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("Upcoming", "Completed", "All").forEach { filter ->
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

            // Reminder Items
            items(filteredList, key = { it.id }) { item ->
                val isDone = item.isCompleted == true

                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("reminder_item_${item.id}")
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                CategoryIcon(category = item.category ?: "Utilities", size = 42.dp)
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = item.title,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        textDecoration = if (isDone) TextDecoration.LineThrough else TextDecoration.None,
                                        color = if (isDone) TextMuted else MaterialTheme.colorScheme.onBackground
                                    )
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.Repeat,
                                            contentDescription = null,
                                            tint = TextMuted,
                                            modifier = Modifier.size(13.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = item.frequency?.replaceFirstChar { it.uppercase() } ?: "Monthly",
                                            fontSize = 12.sp,
                                            color = TextMuted
                                        )
                                    }
                                }
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = String.format(Locale.US, "$%.2f", item.amount),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isDone) TextMuted else MaterialTheme.colorScheme.onBackground
                                )

                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isDone) PositiveGreenBg else WarningAmberBg
                                ) {
                                    Text(
                                        text = if (isDone) "Paid" else "Due: ${item.dueDate}",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isDone) PositiveGreen else WarningAmber,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }

                        if (!item.notes.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(text = item.notes, fontSize = 12.sp, color = TextMuted)
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = {
                                    remindersList = remindersList.filterNot { it.id == item.id }
                                    coroutineScope.launch {
                                        clientManager.executeSafely("Delete Reminder (${item.title})") { service ->
                                            service.deleteReminder(item.id)
                                        }
                                    }
                                },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete", tint = TextMuted, modifier = Modifier.size(18.dp))
                            }

                            if (!isDone) {
                                Button(
                                    onClick = {
                                        remindersList = remindersList.map {
                                            if (it.id == item.id) it.copy(isCompleted = true) else it
                                        }
                                        coroutineScope.launch {
                                            val completeRes = clientManager.executeSafely("Complete Reminder (${item.title})") { service ->
                                                service.completeReminder(item.id)
                                            }
                                            if (completeRes is FmsApiResult.Success) {
                                                Toast.makeText(context, "${item.title} marked paid on cloud", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = PositiveGreen),
                                    modifier = Modifier.testTag("btn_complete_reminder_${item.id}")
                                ) {
                                    Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Mark as Paid", fontSize = 12.sp)
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
                .testTag("fab_add_reminder")
        ) {
            Icon(imageVector = Icons.Default.Add, contentDescription = "Add Reminder")
        }
    }

    if (isAddDialogOpen) {
        AddReminderDialog(
            onDismiss = { isAddDialogOpen = false },
            onAdd = { title, amount, dueDate, frequency, category, notes ->
                val newItem = ReminderResponseDto(
                    id = "rem-${System.currentTimeMillis()}",
                    title = title,
                    amount = amount,
                    dueDate = dueDate,
                    frequency = frequency,
                    category = category,
                    notes = notes,
                    isCompleted = false
                )
                remindersList = listOf(newItem) + remindersList
                isAddDialogOpen = false
                coroutineScope.launch {
                    val addRes = clientManager.executeSafely("Create Reminder ($title)") { service ->
                        service.createReminder(
                            CreateReminderDto(
                                title = title,
                                amount = amount,
                                dueDate = dueDate,
                                frequency = frequency,
                                category = category,
                                notes = notes
                            )
                        )
                    }
                    when (addRes) {
                        is FmsApiResult.Success -> {
                            Toast.makeText(context, "Reminder '$title' synced to cloud.", Toast.LENGTH_SHORT).show()
                        }
                        is FmsApiResult.Conflict -> {
                            Toast.makeText(context, "Conflict: Bill reminder already exists for '$title'.", Toast.LENGTH_LONG).show()
                        }
                        is FmsApiResult.Unreachable -> {
                            val msg = if (addRes.isRenderColdStart) "Saved locally! (Render server waking up...)" else "Saved locally! (Backend unreachable)"
                            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                        }
                        is FmsApiResult.Unauthorized -> {
                            Toast.makeText(context, "Saved locally. Sign in to sync with cloud backend.", Toast.LENGTH_LONG).show()
                        }
                        else -> {
                            Toast.makeText(context, "Reminder saved to device.", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        )
    }
}

@Composable
fun AddReminderDialog(
    onDismiss: () -> Unit,
    onAdd: (title: String, amount: Double, dueDate: String, frequency: String, category: String, notes: String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var amountText by remember { mutableStateOf("") }
    var dueDateText by remember { mutableStateOf("2026-09-30") }
    var frequency by remember { mutableStateOf("monthly") }
    var category by remember { mutableStateOf("Utilities") }
    var notesText by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .testTag("add_reminder_dialog")
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(
                    text = "New Bill Reminder",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Bill Name (e.g. Electric Bill)") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_reminder_title"),
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
                        .testTag("input_reminder_amount"),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = dueDateText,
                    onValueChange = { dueDateText = it },
                    label = { Text("Due Date (YYYY-MM-DD)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = category,
                    onValueChange = { category = it },
                    label = { Text("Category (Utilities, Rent, Subscription)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = notesText,
                    onValueChange = { notesText = it },
                    label = { Text("Notes (Optional)") },
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
                            if (amt > 0 && title.isNotBlank()) {
                                onAdd(title, amt, dueDateText, frequency, category, notesText)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PurplePrimary),
                        modifier = Modifier.testTag("btn_save_reminder")
                    ) {
                        Text("Save Reminder")
                    }
                }
            }
        }
    }
}
