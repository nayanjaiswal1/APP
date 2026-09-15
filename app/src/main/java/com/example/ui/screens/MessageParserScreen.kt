package com.example.ui.screens

import android.Manifest
import android.content.ClipboardManager
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.MarkChatRead
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.data.local.entity.GroupEntity
import com.example.data.local.entity.InvoiceEntity
import com.example.data.local.entity.ParsedMessageEntity
import com.example.data.parser.InvoiceParserAndTranslator
import com.example.data.parser.MessageParserEngine
import com.example.data.parser.ParsedExpenseResult
import com.example.data.parser.ParsedInvoiceResult
import com.example.data.parser.SampleBatchTemplate
import com.example.data.parser.SampleInvoice
import com.example.data.parser.SampleMessageTemplate
import com.example.data.sms.DeviceSmsItem
import com.example.data.sms.DeviceSmsReader
import com.example.ui.components.CategoryIcon
import com.example.ui.theme.PositiveGreen
import com.example.ui.theme.PositiveGreenBg
import com.example.ui.theme.PurpleContainer
import com.example.ui.theme.PurplePrimary
import com.example.ui.theme.PurplePrimaryDark
import com.example.ui.theme.PurpleSecondaryContainer
import com.example.ui.theme.PurpleSurfaceVariant
import com.example.ui.theme.TextMuted
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class ParserMode(val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    AUTO_SMS("Auto SMS Sync", Icons.Default.MarkChatRead),
    NOTIFICATIONS("Single SMS", Icons.Default.Sms),
    BATCH_NOTIFICATIONS("Batch Alerts", Icons.Default.AutoAwesome),
    INVOICE_TRANSLATOR("Invoice & OCR", Icons.Default.ReceiptLong),
    STORED_INVOICES("Stored Invoices", Icons.Default.Description)
}

@Composable
fun MessageParserScreen(
    inputText: String,
    isParsing: Boolean,
    activeResult: ParsedExpenseResult?,
    parsedHistory: List<ParsedMessageEntity>,
    groups: List<GroupEntity>,
    // Batch Notification state
    batchInputText: String,
    isParsingBatch: Boolean,
    batchParsedResults: List<ParsedExpenseResult>,
    selectedBatchIndexes: Set<Int>,
    // Invoice Translation state
    invoiceInputText: String,
    isParsingInvoice: Boolean,
    activeParsedInvoice: ParsedInvoiceResult?,
    invoices: List<InvoiceEntity>,
    // Device SMS Auto-Sync state
    deviceSmsList: List<DeviceSmsItem> = emptyList(),
    isScanningDeviceSms: Boolean = false,
    hasSmsPermission: Boolean = false,
    selectedDeviceSmsIds: Set<Long> = emptySet(),
    smsSyncSuccessMessage: String? = null,
    // Callbacks
    onInputChange: (String) -> Unit,
    onParseClick: () -> Unit,
    onSelectTemplate: (SampleMessageTemplate) -> Unit,
    onClearResult: () -> Unit,
    onConvertToExpense: (ParsedExpenseResult) -> Unit,
    // Batch Callbacks
    onBatchInputChange: (String) -> Unit,
    onParseBatchClick: () -> Unit,
    onSelectBatchTemplate: (SampleBatchTemplate) -> Unit,
    onToggleBatchIndex: (Int) -> Unit,
    onSelectAllBatch: (Boolean) -> Unit,
    onAddSelectedBatchToExpenses: (targetGroupId: Long?) -> Unit,
    // Invoice Callbacks
    onInvoiceInputChange: (String) -> Unit,
    onSetInvoiceBitmap: (android.graphics.Bitmap?) -> Unit,
    onParseInvoiceClick: () -> Unit,
    onSelectSampleInvoice: (SampleInvoice) -> Unit,
    onSaveInvoiceAndAddToExpense: (ParsedInvoiceResult, targetGroupId: Long?) -> Unit,
    onOpenInvoiceDetail: (InvoiceEntity) -> Unit,
    // Device SMS Callbacks
    onScanDeviceSms: () -> Unit = {},
    onSetSmsPermissionGranted: (Boolean) -> Unit = {},
    onToggleDeviceSmsSelection: (Long) -> Unit = {},
    onSelectAllDeviceSms: (Boolean) -> Unit = {},
    onImportSelectedDeviceSms: (targetGroupId: Long?) -> Unit = {},
    onLoadSimulatedDeviceSms: () -> Unit = {},
    onClearSmsSyncMessage: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
    var currentMode by remember { mutableStateOf(ParserMode.AUTO_SMS) }
    var selectedAutoSmsGroupId by remember { mutableStateOf<Long?>(null) }
    var selectedBatchGroupId by remember { mutableStateOf<Long?>(null) }
    var selectedInvoiceGroupId by remember { mutableStateOf<Long?>(null) }

    // Direct SMS Permission Launcher
    val smsPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        onSetSmsPermissionGranted(isGranted)
        if (isGranted) {
            onScanDeviceSms()
        }
    }

    // Auto check permission on entry
    LaunchedEffect(Unit) {
        val granted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_SMS
        ) == PackageManager.PERMISSION_GRANTED
        if (granted) {
            onSetSmsPermissionGranted(true)
            if (deviceSmsList.isEmpty()) {
                onScanDeviceSms()
            }
        }
    }

    // Android Zero-Permission Photo Picker
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                val bitmap = BitmapFactory.decodeStream(inputStream)
                onSetInvoiceBitmap(bitmap)
                onParseInvoiceClick()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .testTag("smart_hub_screen"),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Mode Selector (Segmented Bar)
        item {
            Spacer(modifier = Modifier.height(4.dp))
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.fillMaxWidth()
            ) {
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(ParserMode.values()) { mode ->
                        val isSelected = currentMode == mode
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) PurplePrimary else Color.Transparent,
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { currentMode = mode }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = mode.icon,
                                    contentDescription = null,
                                    tint = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = mode.label,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }

        // =================================================================
        // MODE 0: AUTO DIRECT DEVICE SMS SYNC & PARSER
        // =================================================================
        if (currentMode == ParserMode.AUTO_SMS) {
            // Success Message Banner
            if (smsSyncSuccessMessage != null) {
                item {
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = PositiveGreenBg),
                        border = BorderStroke(1.dp, PositiveGreen.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, tint = PositiveGreen, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = smsSyncSuccessMessage,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = PositiveGreen
                                )
                            }
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Dismiss",
                                tint = PositiveGreen,
                                modifier = Modifier
                                    .size(18.dp)
                                    .clickable { onClearSmsSyncMessage() }
                            )
                        }
                    }
                }
            }

            // Permission Request or Active Status Header Card
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, if (hasSmsPermission) PurplePrimary.copy(alpha = 0.3f) else MaterialTheme.colorScheme.surfaceVariant),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(CircleShape)
                                        .background(if (hasSmsPermission) PositiveGreenBg else PurpleContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (hasSmsPermission) Icons.Default.CheckCircle else Icons.Default.MarkChatRead,
                                        contentDescription = null,
                                        tint = if (hasSmsPermission) PositiveGreen else PurplePrimaryDark,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "Direct SMS Auto-Sync",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = if (hasSmsPermission) "Direct inbox access granted" else "No copy-pasting required",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (hasSmsPermission) PositiveGreen else TextMuted
                                    )
                                }
                            }

                            if (hasSmsPermission) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = PositiveGreenBg
                                ) {
                                    Text(
                                        text = "LIVE SYNC",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = PositiveGreen,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = if (hasSmsPermission) {
                                "SplitExpense automatically scans your incoming bank debit alerts, card swipes, and UPI messages, extracting merchant, amount, category, and date instantly."
                            } else {
                                "Grant SMS permission to read transactional bank and card alerts directly from your device inbox. Your financial SMS messages are parsed securely on-device."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 18.sp
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (!hasSmsPermission) {
                                Button(
                                    onClick = { smsPermissionLauncher.launch(Manifest.permission.READ_SMS) },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("request_sms_permission_button"),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = PurplePrimary)
                                ) {
                                    Icon(imageVector = Icons.Default.Security, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Grant SMS Access", fontWeight = FontWeight.Bold)
                                }
                            } else {
                                Button(
                                    onClick = { onScanDeviceSms() },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("rescan_sms_button"),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = PurplePrimary),
                                    enabled = !isScanningDeviceSms
                                ) {
                                    if (isScanningDeviceSms) {
                                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                    } else {
                                        Icon(imageVector = Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                    }
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(if (isScanningDeviceSms) "Scanning..." else "Scan Inbox", fontWeight = FontWeight.Bold)
                                }
                            }

                            OutlinedButton(
                                onClick = { onLoadSimulatedDeviceSms() },
                                modifier = Modifier.testTag("load_demo_sms_button"),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, PurplePrimary.copy(alpha = 0.5f))
                            ) {
                                Text("Demo Alerts", fontWeight = FontWeight.SemiBold, color = PurplePrimaryDark)
                            }
                        }
                    }
                }
            }

            // Group Split Target Selector
            if (deviceSmsList.isNotEmpty()) {
                item {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Assign Imported Expenses To",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = if (selectedAutoSmsGroupId == null) "Personal Expense" else groups.find { it.id == selectedAutoSmsGroupId }?.name ?: "Group",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = PurplePrimaryDark
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                item {
                                    val isSelected = selectedAutoSmsGroupId == null
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = if (isSelected) PurplePrimary else MaterialTheme.colorScheme.surfaceVariant,
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(12.dp))
                                            .clickable { selectedAutoSmsGroupId = null }
                                    ) {
                                        Text(
                                            text = "Personal (No Split)",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                                        )
                                    }
                                }

                                items(groups) { grp ->
                                    val isSelected = selectedAutoSmsGroupId == grp.id
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = if (isSelected) PurplePrimary else MaterialTheme.colorScheme.surfaceVariant,
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(12.dp))
                                            .clickable { selectedAutoSmsGroupId = grp.id }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Groups,
                                                contentDescription = null,
                                                tint = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "${grp.name} (Split Equal)",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Batch Selection Bar & Actions
                item {
                    val allSelected = selectedDeviceSmsIds.size == deviceSmsList.size
                    val selectedCount = selectedDeviceSmsIds.size
                    val totalSelectedAmount = deviceSmsList
                        .filter { selectedDeviceSmsIds.contains(it.id) }
                        .sumOf { it.parsedResult.amount }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Detected Bank & Card SMS",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "$selectedCount of ${deviceSmsList.size} selected (${String.format(Locale.US, "$%.2f", totalSelectedAmount)})",
                                style = MaterialTheme.typography.labelSmall,
                                color = PurplePrimaryDark,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { onSelectAllDeviceSms(!allSelected) }
                            ) {
                                Text(
                                    text = if (allSelected) "Deselect All" else "Select All",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = PurplePrimaryDark,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }

                // List of Detected Device SMS Items
                items(deviceSmsList) { sms ->
                    val isChecked = selectedDeviceSmsIds.contains(sms.id)
                    var isExpanded by remember { mutableStateOf(false) }
                    val parsed = sms.parsedResult
                    val dateFormat = SimpleDateFormat("MMM d, h:mm a", Locale.getDefault())
                    val timeStr = dateFormat.format(Date(sms.dateMillis))

                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isChecked) PurpleSurfaceVariant else MaterialTheme.colorScheme.surface
                        ),
                        border = BorderStroke(
                            1.dp,
                            if (isChecked) PurplePrimary.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surfaceVariant
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = if (isChecked) 1.dp else 0.5.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onToggleDeviceSmsSelection(sms.id) }
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                    Checkbox(
                                        checked = isChecked,
                                        onCheckedChange = { onToggleDeviceSmsSelection(sms.id) },
                                        colors = CheckboxDefaults.colors(checkedColor = PurplePrimary)
                                    )

                                    Spacer(modifier = Modifier.width(4.dp))

                                    CategoryIcon(category = parsed.category, size = 36.dp)

                                    Spacer(modifier = Modifier.width(10.dp))

                                    Column {
                                        Text(
                                            text = parsed.title.ifBlank { "Expense" },
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold
                                        )

                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Surface(
                                                shape = RoundedCornerShape(4.dp),
                                                color = PurpleContainer
                                            ) {
                                                Text(
                                                    text = sms.sender,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = PurplePrimaryDark,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }

                                            Text(
                                                text = "• $timeStr",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = TextMuted
                                            )
                                        }
                                    }
                                }

                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = "${parsed.currency}${String.format(Locale.US, "%.2f", parsed.amount)}",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = PurplePrimaryDark
                                    )

                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant
                                    ) {
                                        Text(
                                            text = parsed.category,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }

                            // Expandable SMS Body Snippet
                            Spacer(modifier = Modifier.height(8.dp))
                            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { isExpanded = !isExpanded },
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = if (isExpanded) sms.body else sms.body.take(70) + if (sms.body.length > 70) "..." else "",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextMuted,
                                    modifier = Modifier.weight(1f),
                                    lineHeight = 15.sp
                                )

                                Text(
                                    text = if (isExpanded) "Less ▲" else "Raw SMS ▼",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = PurplePrimary,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(start = 8.dp)
                                )
                            }
                        }
                    }
                }

                // Import Button
                item {
                    val count = selectedDeviceSmsIds.size
                    val total = deviceSmsList
                        .filter { selectedDeviceSmsIds.contains(it.id) }
                        .sumOf { it.parsedResult.amount }

                    Button(
                        onClick = { onImportSelectedDeviceSms(selectedAutoSmsGroupId) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("import_selected_sms_button"),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PurplePrimary),
                        enabled = count > 0
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Import $count SMS Expense${if (count != 1) "s" else ""} (${String.format(Locale.US, "$%.2f", total)})",
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 15.sp
                        )
                    }
                }
            }
        }

        // =================================================================
        // MODE 1: SINGLE NOTIFICATION / SMS PARSER
        // =================================================================
        if (currentMode == ParserMode.NOTIFICATIONS) {
            // Quick Sample Templates
            item {
                Column {
                    Text(
                        text = "Try Sample SMS / Chat",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(MessageParserEngine.SAMPLE_TEMPLATES) { template ->
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = MaterialTheme.colorScheme.surface,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(14.dp))
                                    .clickable { onSelectTemplate(template) }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    CategoryIcon(category = template.category, size = 24.dp)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = template.title,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Input Text Area
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Paste Single SMS or Chat",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            Row {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = PurpleContainer,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable {
                                            val clipData = clipboardManager?.primaryClip
                                            if (clipData != null && clipData.itemCount > 0) {
                                                val text = clipData.getItemAt(0).text?.toString() ?: ""
                                                if (text.isNotBlank()) onInputChange(text)
                                            }
                                        }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(imageVector = Icons.Default.ContentPaste, contentDescription = "Paste", tint = PurplePrimaryDark, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Paste", style = MaterialTheme.typography.labelSmall, color = PurplePrimaryDark, fontWeight = FontWeight.Bold)
                                    }
                                }

                                if (inputText.isNotBlank()) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant,
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .clickable {
                                                onInputChange("")
                                                onClearResult()
                                            }
                                    ) {
                                        Icon(imageVector = Icons.Default.Clear, contentDescription = "Clear", modifier = Modifier.padding(4.dp).size(14.dp))
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedTextField(
                            value = inputText,
                            onValueChange = onInputChange,
                            placeholder = {
                                Text(
                                    "e.g. 'Bank alert: Debited $64.80 at Trader Joes' or 'Pizza was $45 split 3 ways with Alex and Emma'",
                                    fontSize = 13.sp,
                                    color = TextMuted.copy(alpha = 0.7f)
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(100.dp)
                                .testTag("message_input_field"),
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = PurplePrimary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Button(
                            onClick = onParseClick,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("parse_message_button"),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = PurplePrimary),
                            enabled = inputText.isNotBlank() && !isParsing
                        ) {
                            if (isParsing) {
                                CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Parsing...", fontWeight = FontWeight.Bold)
                            } else {
                                Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Parse Single Bill", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // Live Parsed Result Card
            if (activeResult != null && activeResult.amount > 0) {
                item {
                    AnimatedVisibility(visible = true, enter = fadeIn() + slideInVertically()) {
                        Card(
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.5.dp, PurpleSecondaryContainer),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                            modifier = Modifier.fillMaxWidth().testTag("parsed_result_card")
                        ) {
                            Column(modifier = Modifier.fillMaxWidth().padding(18.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        CategoryIcon(category = activeResult.category, size = 40.dp)
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Text(text = activeResult.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                            Text(text = activeResult.category, style = MaterialTheme.typography.labelSmall, color = TextMuted)
                                        }
                                    }

                                    Surface(shape = RoundedCornerShape(10.dp), color = PositiveGreenBg) {
                                        Text(
                                            text = "${(activeResult.confidence * 100).toInt()}% Match",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = PositiveGreen,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(PurpleSurfaceVariant)
                                        .padding(14.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(text = "DETECTED AMOUNT", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                                        Text(
                                            text = "${activeResult.currency}${String.format(Locale.US, "%.2f", activeResult.amount)}",
                                            style = MaterialTheme.typography.displaySmall,
                                            fontWeight = FontWeight.Bold,
                                            color = PurplePrimaryDark
                                        )
                                    }

                                    if (activeResult.suggestedSplitCount > 1) {
                                        Column(horizontalAlignment = Alignment.End) {
                                            Text(text = "SPLIT ${activeResult.suggestedSplitCount} WAYS", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                                            val perPerson = activeResult.amount / activeResult.suggestedSplitCount
                                            Text(
                                                text = "${activeResult.currency}${String.format(Locale.US, "%.2f", perPerson)}/ea",
                                                style = MaterialTheme.typography.titleLarge,
                                                fontWeight = FontWeight.Bold,
                                                color = PurplePrimary
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                Button(
                                    onClick = { onConvertToExpense(activeResult) },
                                    modifier = Modifier.fillMaxWidth().testTag("convert_to_expense_button"),
                                    shape = RoundedCornerShape(14.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = PurplePrimary)
                                ) {
                                    Icon(imageVector = Icons.Default.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(text = "Add to Splitwise Expense", color = Color.White, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }

            // History Section
            item {
                Text(text = "Parsed History (${parsedHistory.size})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }

            items(parsedHistory) { item ->
                val dateFormat = SimpleDateFormat("MMM d, h:mm a", Locale.getDefault())
                val timeStr = dateFormat.format(Date(item.createdAt))

                Card(
                    modifier = Modifier.fillMaxWidth().clickable {
                        onInputChange(item.rawMessage)
                        onParseClick()
                    },
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            CategoryIcon(category = item.parsedCategory, size = 36.dp)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(text = item.parsedTitle, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                Text(text = timeStr, style = MaterialTheme.typography.labelSmall, color = TextMuted)
                            }
                        }

                        Text(
                            text = "${item.parsedCurrency}${String.format(Locale.US, "%.2f", item.parsedAmount)}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = PurplePrimaryDark
                        )
                    }
                }
            }
        }

        // =================================================================
        // MODE 2: BATCH / MULTIPLE NOTIFICATION PARSER
        // =================================================================
        else if (currentMode == ParserMode.BATCH_NOTIFICATIONS) {
            item {
                Column {
                    Text(
                        text = "Try Batch Notification Streams",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(MessageParserEngine.SAMPLE_BATCH_TEMPLATES) { batchTemplate ->
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = MaterialTheme.colorScheme.surface,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(14.dp))
                                    .clickable { onSelectBatchTemplate(batchTemplate) }
                            ) {
                                Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                                    Text(text = batchTemplate.title, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                                    Text(text = batchTemplate.description, style = MaterialTheme.typography.labelSmall, color = TextMuted)
                                }
                            }
                        }
                    }
                }
            }

            // Batch Input Area
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Paste Multiple Push Notifications / SMS",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold
                            )

                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = PurpleContainer,
                                modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable {
                                    val clipData = clipboardManager?.primaryClip
                                    if (clipData != null && clipData.itemCount > 0) {
                                        val text = clipData.getItemAt(0).text?.toString() ?: ""
                                        if (text.isNotBlank()) onBatchInputChange(text)
                                    }
                                }
                            ) {
                                Row(modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(imageVector = Icons.Default.ContentPaste, contentDescription = null, tint = PurplePrimaryDark, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Paste Stream", style = MaterialTheme.typography.labelSmall, color = PurplePrimaryDark, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedTextField(
                            value = batchInputText,
                            onValueChange = onBatchInputChange,
                            placeholder = {
                                Text(
                                    "Paste multiple SMS/bank notifications separated by lines or numbered lists...",
                                    fontSize = 12.sp,
                                    color = TextMuted.copy(alpha = 0.7f)
                                )
                            },
                            modifier = Modifier.fillMaxWidth().height(120.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = PurplePrimary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Button(
                            onClick = onParseBatchClick,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = PurplePrimary),
                            enabled = batchInputText.isNotBlank() && !isParsingBatch
                        ) {
                            if (isParsingBatch) {
                                CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Extracting Multiple Notifications...", fontWeight = FontWeight.Bold)
                            } else {
                                Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Parse Multiple Notifications", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // Batch Extracted Results List
            if (batchParsedResults.isNotEmpty()) {
                val totalSelected = batchParsedResults.filterIndexed { idx, _ -> selectedBatchIndexes.contains(idx) }.sumOf { it.amount }

                item {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "PARSED TRANSACTIONS (${batchParsedResults.size})",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = PurplePrimaryDark
                            )

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    text = "Select All",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = PurplePrimary,
                                    modifier = Modifier.clickable { onSelectAllBatch(true) }
                                )
                                Text(text = "•", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                                Text(
                                    text = "Clear",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = TextMuted,
                                    modifier = Modifier.clickable { onSelectAllBatch(false) }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Target Splitwise Group Picker
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = PurpleContainer.copy(alpha = 0.5f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(imageVector = Icons.Default.Groups, contentDescription = null, tint = PurplePrimaryDark, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Add to:", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                }

                                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    item {
                                        val isSel = selectedBatchGroupId == null
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = if (isSel) PurplePrimary else MaterialTheme.colorScheme.surface,
                                            modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable { selectedBatchGroupId = null }
                                        ) {
                                            Text(
                                                text = "Personal Split",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = if (isSel) Color.White else MaterialTheme.colorScheme.onSurface,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                            )
                                        }
                                    }

                                    items(groups) { g ->
                                        val isSel = selectedBatchGroupId == g.id
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = if (isSel) PurplePrimary else MaterialTheme.colorScheme.surface,
                                            modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable { selectedBatchGroupId = g.id }
                                        ) {
                                            Text(
                                                text = g.name,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = if (isSel) Color.White else MaterialTheme.colorScheme.onSurface,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                items(batchParsedResults.size) { idx ->
                    val item = batchParsedResults[idx]
                    val isChecked = selectedBatchIndexes.contains(idx)

                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isChecked) PurpleContainer.copy(alpha = 0.35f) else MaterialTheme.colorScheme.surface
                        ),
                        border = BorderStroke(1.dp, if (isChecked) PurplePrimary.copy(alpha = 0.6f) else MaterialTheme.colorScheme.surfaceVariant),
                        modifier = Modifier.fillMaxWidth().clickable { onToggleBatchIndex(idx) }
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Checkbox(
                                    checked = isChecked,
                                    onCheckedChange = { onToggleBatchIndex(idx) },
                                    colors = CheckboxDefaults.colors(checkedColor = PurplePrimary)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                CategoryIcon(category = item.category, size = 32.dp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(text = item.title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                                    Text(
                                        text = "${item.category} • Split ${item.suggestedSplitCount} ways",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TextMuted
                                    )
                                }
                            }

                            Text(
                                text = "${item.currency}${String.format(Locale.US, "%.2f", item.amount)}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = PurplePrimaryDark
                            )
                        }
                    }
                }

                item {
                    Button(
                        onClick = { onAddSelectedBatchToExpenses(selectedBatchGroupId) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PurplePrimary),
                        enabled = selectedBatchIndexes.isNotEmpty()
                    ) {
                        Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Add ${selectedBatchIndexes.size} Expenses ($${String.format(Locale.US, "%.2f", totalSelected)})",
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }

        // =================================================================
        // MODE 3: INVOICE & RECEIPT TRANSLATOR
        // =================================================================
        else if (currentMode == ParserMode.INVOICE_TRANSLATOR) {
            // Foreign Invoice Sample Presets
            item {
                Column {
                    Text(
                        text = "Try Multilingual Sample Receipts",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(InvoiceParserAndTranslator.SAMPLE_INVOICES) { inv ->
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = MaterialTheme.colorScheme.surface,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(14.dp))
                                    .clickable { onSelectSampleInvoice(inv) }
                            ) {
                                Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                                    Text(text = inv.title, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                                    Text(text = "${inv.language} • ${inv.vendor}", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                                }
                            }
                        }
                    }
                }
            }

            // Invoice Upload & Text Box
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Upload or Paste Foreign Invoice",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold
                            )

                            // Photo Picker Button
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = PurpleContainer,
                                modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable {
                                    photoPickerLauncher.launch(
                                        androidx.activity.result.PickVisualMediaRequest(
                                            ActivityResultContracts.PickVisualMedia.ImageOnly
                                        )
                                    )
                                }
                            ) {
                                Row(modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(imageVector = Icons.Default.Image, contentDescription = null, tint = PurplePrimaryDark, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Pick Image", style = MaterialTheme.typography.labelSmall, color = PurplePrimaryDark, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedTextField(
                            value = invoiceInputText,
                            onValueChange = onInvoiceInputChange,
                            placeholder = {
                                Text(
                                    "Paste foreign receipt OCR, Japanese Izakaya bill, German Rechnung, French Facture, or Spanish invoice...",
                                    fontSize = 12.sp,
                                    color = TextMuted.copy(alpha = 0.7f)
                                )
                            },
                            modifier = Modifier.fillMaxWidth().height(120.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = PurplePrimary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Button(
                            onClick = onParseInvoiceClick,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = PurplePrimary),
                            enabled = invoiceInputText.isNotBlank() && !isParsingInvoice
                        ) {
                            if (isParsingInvoice) {
                                CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Translating & Parsing Line Items...", fontWeight = FontWeight.Bold)
                            } else {
                                Icon(imageVector = Icons.Default.Translate, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Translate & Extract Line Items", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // Live Translated Invoice Card
            if (activeParsedInvoice != null && activeParsedInvoice.totalAmount > 0) {
                item {
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.5.dp, PurpleSecondaryContainer),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.fillMaxWidth().padding(18.dp)) {
                            // Header
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = activeParsedInvoice.vendorName,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = PurplePrimaryDark
                                    )
                                    Text(
                                        text = "Language: ${activeParsedInvoice.originalLanguage} ➔ ${activeParsedInvoice.translatedLanguage}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TextMuted
                                    )
                                }

                                Surface(shape = RoundedCornerShape(10.dp), color = PositiveGreenBg) {
                                    Text(
                                        text = "Translated",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = PositiveGreen,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Total Amount Banner
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(PurpleSurfaceVariant)
                                    .padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(text = "TOTAL EXTRACTED", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                                    Text(
                                        text = "${activeParsedInvoice.currency}${String.format(Locale.US, "%.2f", activeParsedInvoice.totalAmount)}",
                                        style = MaterialTheme.typography.headlineMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = PurplePrimaryDark
                                    )
                                }

                                if (activeParsedInvoice.taxAmount > 0) {
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(text = "TAX / VAT", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                                        Text(
                                            text = "${activeParsedInvoice.currency}${String.format(Locale.US, "%.2f", activeParsedInvoice.taxAmount)}",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // Itemized Breakdown Table
                            Text(
                                text = "TRANSLATED LINE ITEMS (${activeParsedInvoice.lineItems.size})",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = TextMuted
                            )
                            Spacer(modifier = Modifier.height(6.dp))

                            activeParsedInvoice.lineItems.forEach { item ->
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(text = item.translatedName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                            if (item.originalName != item.translatedName && item.originalName.isNotBlank()) {
                                                Text(text = item.originalName, style = MaterialTheme.typography.labelSmall, color = TextMuted)
                                            }
                                        }

                                        Text(
                                            text = "${activeParsedInvoice.currency}${String.format(Locale.US, "%.2f", item.totalPrice)}",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = PurplePrimaryDark
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // Group split target
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = PurpleContainer.copy(alpha = 0.5f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Split with:", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        item {
                                            val isSel = selectedInvoiceGroupId == null
                                            Surface(
                                                shape = RoundedCornerShape(8.dp),
                                                color = if (isSel) PurplePrimary else MaterialTheme.colorScheme.surface,
                                                modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable { selectedInvoiceGroupId = null }
                                            ) {
                                                Text("Personal", style = MaterialTheme.typography.labelSmall, color = if (isSel) Color.White else MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                                            }
                                        }
                                        items(groups) { g ->
                                            val isSel = selectedInvoiceGroupId == g.id
                                            Surface(
                                                shape = RoundedCornerShape(8.dp),
                                                color = if (isSel) PurplePrimary else MaterialTheme.colorScheme.surface,
                                                modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable { selectedInvoiceGroupId = g.id }
                                            ) {
                                                Text(g.name, style = MaterialTheme.typography.labelSmall, color = if (isSel) Color.White else MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                                            }
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            Button(
                                onClick = { onSaveInvoiceAndAddToExpense(activeParsedInvoice, selectedInvoiceGroupId) },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = PurplePrimary)
                            ) {
                                Icon(imageVector = Icons.Default.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Store Invoice & Add Expense", fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }
                    }
                }
            }
        }

        // =================================================================
        // MODE 4: STORED INVOICES LIST
        // =================================================================
        else if (currentMode == ParserMode.STORED_INVOICES) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "Stored Invoices & Receipts", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(text = "${invoices.size} Saved", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                }
            }

            if (invoices.isEmpty()) {
                item {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                            Text("No stored invoices yet. Parse and translate an invoice above!", style = MaterialTheme.typography.bodySmall, color = TextMuted)
                        }
                    }
                }
            } else {
                items(invoices) { inv ->
                    val dateFormat = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
                    val dateStr = dateFormat.format(Date(inv.dateMillis))

                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant),
                        modifier = Modifier.fillMaxWidth().clickable { onOpenInvoiceDetail(inv) }
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(PurpleContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(imageVector = Icons.Default.ReceiptLong, contentDescription = null, tint = PurplePrimaryDark, modifier = Modifier.size(20.dp))
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(text = inv.vendorName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                                    Text(
                                        text = "${inv.originalLanguage} ➔ ${inv.translatedLanguage} • $dateStr",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TextMuted
                                    )
                                }
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "${inv.currency}${String.format(Locale.US, "%.2f", inv.totalAmount)}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = PurplePrimaryDark
                                )
                                Text(
                                    text = "View Breakdown ↗",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = PurplePrimary,
                                    fontWeight = FontWeight.SemiBold
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
