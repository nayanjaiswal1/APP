package com.example.ui.components

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.export.ExpenseReportData
import com.example.data.export.ExpenseReportExporter
import com.example.data.export.FileSaveResult
import com.example.data.local.entity.ExpenseEntity
import com.example.data.local.entity.GroupEntity
import com.example.data.local.entity.UserEntity
import com.example.ui.theme.PositiveGreen
import com.example.ui.theme.PositiveGreenBg
import com.example.ui.theme.PurpleContainer
import com.example.ui.theme.PurplePrimary
import com.example.ui.theme.PurplePrimaryDark
import com.example.ui.theme.PurpleSurfaceVariant
import com.example.ui.theme.TextMuted
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

enum class ExportFormat(val title: String, val extension: String, val mimeType: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    PDF("PDF Document", ".pdf", "application/pdf", Icons.Default.PictureAsPdf),
    CSV("CSV Spreadsheet", ".csv", "text/csv", Icons.Default.TableChart)
}

enum class CsvExportStyle(val title: String, val description: String) {
    ITEMIZED_EXPENSES("Itemized Records", "Standard Expense entities with dates, amounts, categories & splits"),
    FULL_SUMMARY_REPORT("Executive Report", "Includes executive KPIs, category distribution & itemized logs")
}

enum class ExportPeriodType(val label: String) {
    THIS_MONTH("This Month"),
    LAST_MONTH("Last Month"),
    CURRENT_FILTER("Active Filtered"),
    ALL_TIME("All Time")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExportReportBottomSheet(
    allExpenses: List<ExpenseEntity>,
    filteredExpenses: List<ExpenseEntity>,
    users: List<UserEntity>,
    groups: List<GroupEntity>,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    var selectedFormat by remember { mutableStateOf(ExportFormat.CSV) }
    var selectedPeriodType by remember { mutableStateOf(ExportPeriodType.THIS_MONTH) }
    var csvExportStyle by remember { mutableStateOf(CsvExportStyle.ITEMIZED_EXPENSES) }

    var isGenerating by remember { mutableStateOf(false) }
    var generatedFile by remember { mutableStateOf<File?>(null) }
    var savedStorageLocation by remember { mutableStateOf<String?>(null) }
    var showSuccessBanner by remember { mutableStateOf(false) }
    var isPreviewOpen by remember { mutableStateOf(false) }

    // Calculate Report Data dynamically based on selected period
    val reportData: ExpenseReportData = remember(selectedPeriodType, allExpenses, filteredExpenses) {
        when (selectedPeriodType) {
            ExportPeriodType.THIS_MONTH -> {
                val cal = Calendar.getInstance()
                ExpenseReportExporter.prepareReportData(allExpenses, users, groups, cal)
            }
            ExportPeriodType.LAST_MONTH -> {
                val cal = Calendar.getInstance()
                cal.add(Calendar.MONTH, -1)
                ExpenseReportExporter.prepareReportData(allExpenses, users, groups, cal)
            }
            ExportPeriodType.CURRENT_FILTER -> {
                ExpenseReportExporter.prepareReportData(
                    filteredExpenses,
                    users,
                    groups,
                    null,
                    "Current Active Filtered View"
                )
            }
            ExportPeriodType.ALL_TIME -> {
                ExpenseReportExporter.prepareReportData(
                    allExpenses,
                    users,
                    groups,
                    null,
                    "All Recorded History"
                )
            }
        }
    }

    // Helper to generate the current CSV string based on selected style
    val currentCsvString = remember(reportData, csvExportStyle) {
        if (csvExportStyle == CsvExportStyle.FULL_SUMMARY_REPORT) {
            ExpenseReportExporter.convertReportDataToCsv(reportData)
        } else {
            ExpenseReportExporter.convertExpensesToCsv(
                expenses = reportData.transactions,
                userMap = reportData.userMap,
                groupMap = reportData.groupMap
            )
        }
    }

    // Storage Access Framework (SAF) Document Creators for saving to device storage
    val saveCsvLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri: Uri? ->
        if (uri != null) {
            isGenerating = true
            showSuccessBanner = false
            scope.launch {
                val success = withContext(Dispatchers.IO) {
                    ExpenseReportExporter.writeCsvToUri(context, uri, currentCsvString)
                }
                if (success) {
                    val cachedFile = withContext(Dispatchers.IO) {
                        ExpenseReportExporter.generateCsvReport(context, reportData)
                    }
                    generatedFile = cachedFile
                    savedStorageLocation = uri.lastPathSegment ?: "Chosen Device Storage"
                    showSuccessBanner = true
                    Toast.makeText(context, "CSV successfully saved to device storage!", Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(context, "Failed to write CSV to storage", Toast.LENGTH_SHORT).show()
                }
                isGenerating = false
            }
        }
    }

    val savePdfLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri: Uri? ->
        if (uri != null) {
            isGenerating = true
            showSuccessBanner = false
            scope.launch {
                val file = withContext(Dispatchers.IO) {
                    ExpenseReportExporter.generatePdfReport(context, reportData)
                }
                val success = withContext(Dispatchers.IO) {
                    try {
                        context.contentResolver.openOutputStream(uri, "wt")?.use { out ->
                            file.inputStream().use { input -> input.copyTo(out) }
                            out.flush()
                        }
                        true
                    } catch (e: Exception) {
                        e.printStackTrace()
                        false
                    }
                }
                if (success) {
                    generatedFile = file
                    savedStorageLocation = uri.lastPathSegment ?: "Chosen Device Storage"
                    showSuccessBanner = true
                    Toast.makeText(context, "PDF saved to device storage!", Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(context, "Failed to save PDF", Toast.LENGTH_SHORT).show()
                }
                isGenerating = false
            }
        }
    }

    // Quick download action to device's public Downloads directory
    fun quickSaveToDownloads() {
        isGenerating = true
        showSuccessBanner = false
        scope.launch {
            if (selectedFormat == ExportFormat.CSV) {
                val sanitizedPeriod = reportData.periodLabel.replace(" ", "_").replace(",", "")
                val fileName = "Expenses_${sanitizedPeriod}_${System.currentTimeMillis()}.csv"
                val result = withContext(Dispatchers.IO) {
                    ExpenseReportExporter.saveCsvToDeviceStorage(context, currentCsvString, fileName)
                }
                val cachedFile = withContext(Dispatchers.IO) {
                    ExpenseReportExporter.generateCsvReport(context, reportData)
                }
                generatedFile = cachedFile
                isGenerating = false

                when (result) {
                    is FileSaveResult.Success -> {
                        savedStorageLocation = result.displayPath
                        showSuccessBanner = true
                        Toast.makeText(context, "Saved to ${result.displayPath}", Toast.LENGTH_LONG).show()
                    }
                    is FileSaveResult.Error -> {
                        Toast.makeText(context, "Failed to save: ${result.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            } else {
                val file = withContext(Dispatchers.IO) {
                    ExpenseReportExporter.generatePdfReport(context, reportData)
                }
                generatedFile = file
                savedStorageLocation = "Exports/${file.name}"
                isGenerating = false
                showSuccessBanner = true
                Toast.makeText(context, "PDF generated successfully!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        modifier = modifier.testTag("export_report_bottom_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(PurpleContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.FileDownload,
                            contentDescription = null,
                            tint = PurplePrimaryDark,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Export Expense Report",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Save CSV or PDF directly to device storage",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMuted
                        )
                    }
                }

                IconButton(onClick = onDismiss) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Success Banner after export
            AnimatedVisibility(visible = showSuccessBanner) {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = PositiveGreenBg),
                    border = BorderStroke(1.dp, PositiveGreen.copy(alpha = 0.5f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 14.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, tint = PositiveGreen, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "File Saved to Device Storage!",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = PositiveGreen
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Location: ${savedStorageLocation ?: "Device Storage"}",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (generatedFile != null) {
                            Text(
                                text = "Size: ${String.format(Locale.US, "%.1f KB", (generatedFile?.length() ?: 0L) / 1024.0)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextMuted
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            generatedFile?.let { file ->
                                Button(
                                    onClick = {
                                        ExpenseReportExporter.openExportFile(context, file, selectedFormat.mimeType)
                                    },
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = PositiveGreen),
                                    modifier = Modifier.height(36.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.FolderOpen, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Open File", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }

                                OutlinedButton(
                                    onClick = {
                                        ExpenseReportExporter.shareExportFile(
                                            context,
                                            file,
                                            selectedFormat.mimeType,
                                            "Expense Report - ${reportData.periodLabel}"
                                        )
                                    },
                                    shape = RoundedCornerShape(8.dp),
                                    border = BorderStroke(1.dp, PositiveGreen),
                                    modifier = Modifier.height(36.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.Share, contentDescription = null, tint = PositiveGreen, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Share", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = PositiveGreen)
                                }
                            }
                        }
                    }
                }
            }

            // 1. Period Selection Chips
            Text(
                text = "1. Select Report Period",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(ExportPeriodType.values()) { period ->
                    val isSelected = selectedPeriodType == period
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) PurplePrimary else MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                selectedPeriodType = period
                                showSuccessBanner = false
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (isSelected) {
                                Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                            }
                            Text(
                                text = period.label,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 2. Export Format Selection (CSV vs PDF)
            Text(
                text = "2. Select File Format",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                ExportFormat.values().forEach { format ->
                    val isSelected = selectedFormat == format
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) PurpleSurfaceVariant else MaterialTheme.colorScheme.surface
                        ),
                        border = BorderStroke(
                            if (isSelected) 2.dp else 1.dp,
                            if (isSelected) PurplePrimary else MaterialTheme.colorScheme.surfaceVariant
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(14.dp))
                            .clickable {
                                selectedFormat = format
                                showSuccessBanner = false
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) PurplePrimary else PurpleContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = format.icon,
                                    contentDescription = null,
                                    tint = if (isSelected) Color.White else PurplePrimaryDark,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = format.title,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) PurplePrimaryDark else MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = format.extension,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextMuted
                                )
                            }
                        }
                    }
                }
            }

            // If CSV is selected, provide options for style & live preview
            if (selectedFormat == ExportFormat.CSV) {
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = "CSV Content Layout",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CsvExportStyle.values().forEach { style ->
                        val isSelected = csvExportStyle == style
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                csvExportStyle = style
                                showSuccessBanner = false
                            },
                            label = {
                                Text(style.title, fontSize = 12.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = PurpleContainer,
                                selectedLabelColor = PurplePrimaryDark
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Live CSV String Preview Toggle
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(imageVector = Icons.Default.Description, contentDescription = null, tint = PurplePrimary, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "CSV Preview (${reportData.transactions.size} records)",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = {
                                        clipboardManager.setText(AnnotatedString(currentCsvString))
                                        Toast.makeText(context, "CSV copied to clipboard!", Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.ContentCopy, contentDescription = "Copy CSV", tint = PurplePrimary, modifier = Modifier.size(18.dp))
                                }

                                IconButton(
                                    onClick = { isPreviewOpen = !isPreviewOpen },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = if (isPreviewOpen) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                        contentDescription = "Toggle Preview",
                                        tint = TextMuted,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }

                        if (isPreviewOpen) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 160.dp)
                                    .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(8.dp))
                                    .padding(8.dp)
                                    .verticalScroll(rememberScrollState())
                                    .horizontalScroll(rememberScrollState())
                            ) {
                                Text(
                                    text = currentCsvString,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp,
                                    lineHeight = 15.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 3. Report Live Summary Card
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
                            text = "Report Summary (${reportData.periodLabel})",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = PurplePrimaryDark
                        )
                        Text(
                            text = "${reportData.totalCount} transactions",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextMuted
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(text = "Total Spending", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                            Text(
                                text = "${reportData.currency}${String.format(Locale.US, "%,.2f", reportData.totalSpend)}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(text = "Top Spending Category", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                            Text(
                                text = reportData.topCategory,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = PurplePrimary
                            )
                        }
                    }

                    if (reportData.categories.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "Top Categories: " + reportData.categories.take(4).joinToString(", ") { "${it.category} (${String.format(Locale.US, "%.0f%%", it.percentage * 100)})" } + if (reportData.categories.size > 4) "..." else "",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextMuted
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Action Buttons: Save to Device Storage / Downloads / Share
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Primary Action: Save to Device Storage (SAF File Picker)
                Button(
                    onClick = {
                        val sanitizedPeriod = reportData.periodLabel.replace(" ", "_").replace(",", "")
                        if (selectedFormat == ExportFormat.CSV) {
                            val defaultName = "Expenses_${sanitizedPeriod}_${System.currentTimeMillis()}.csv"
                            saveCsvLauncher.launch(defaultName)
                        } else {
                            val defaultName = "Expense_Report_${sanitizedPeriod}_${System.currentTimeMillis()}.pdf"
                            savePdfLauncher.launch(defaultName)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("save_to_device_storage_button"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PurplePrimary),
                    enabled = !isGenerating && reportData.totalCount > 0
                ) {
                    if (isGenerating) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Processing...", fontWeight = FontWeight.Bold)
                    } else {
                        Icon(imageVector = Icons.Default.Save, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Save ${selectedFormat.name} to Device Storage", fontWeight = FontWeight.Bold)
                    }
                }

                // Secondary Row: Quick Save to Downloads & Share
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = { quickSaveToDownloads() },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("quick_download_button"),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.5.dp, PurplePrimary),
                        enabled = !isGenerating && reportData.totalCount > 0
                    ) {
                        Icon(imageVector = Icons.Default.Download, contentDescription = null, tint = PurplePrimary)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Downloads", fontWeight = FontWeight.Bold, color = PurplePrimaryDark)
                    }

                    OutlinedButton(
                        onClick = {
                            isGenerating = true
                            showSuccessBanner = false
                            scope.launch {
                                val file = withContext(Dispatchers.IO) {
                                    if (selectedFormat == ExportFormat.PDF) {
                                        ExpenseReportExporter.generatePdfReport(context, reportData)
                                    } else {
                                        ExpenseReportExporter.generateCsvReport(context, reportData)
                                    }
                                }
                                isGenerating = false
                                generatedFile = file
                                showSuccessBanner = true
                                ExpenseReportExporter.shareExportFile(
                                    context,
                                    file,
                                    selectedFormat.mimeType,
                                    "Expense Report - ${reportData.periodLabel}"
                                )
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("share_report_button"),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.5.dp, PurplePrimary),
                        enabled = !isGenerating && reportData.totalCount > 0
                    ) {
                        Icon(imageVector = Icons.Default.Share, contentDescription = null, tint = PurplePrimary)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Share", fontWeight = FontWeight.Bold, color = PurplePrimaryDark)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
