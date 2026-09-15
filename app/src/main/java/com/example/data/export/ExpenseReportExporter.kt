package com.example.data.export

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import com.example.data.local.entity.ExpenseEntity
import com.example.data.local.entity.GroupEntity
import com.example.data.local.entity.UserEntity
import java.io.File
import java.io.FileOutputStream
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

sealed class FileSaveResult {
    data class Success(val uri: Uri, val fileName: String, val displayPath: String) : FileSaveResult()
    data class Error(val message: String, val throwable: Throwable? = null) : FileSaveResult()
}

data class CategorySummaryItem(
    val category: String,
    val totalAmount: Double,
    val count: Int,
    val percentage: Float,
    val maxExpense: Double,
    val avgExpense: Double
)

data class ExpenseReportData(
    val title: String,
    val periodLabel: String,
    val generatedDate: String,
    val currency: String,
    val totalSpend: Double,
    val totalCount: Int,
    val avgSpend: Double,
    val topCategory: String,
    val categories: List<CategorySummaryItem>,
    val transactions: List<ExpenseEntity>,
    val userMap: Map<Long, String>,
    val groupMap: Map<Long, String>
)

object ExpenseReportExporter {

    fun prepareReportData(
        expenses: List<ExpenseEntity>,
        users: List<UserEntity>,
        groups: List<GroupEntity>,
        monthCalendar: Calendar?, // null for current filtered/custom period
        periodTitleOverride: String? = null
    ): ExpenseReportData {
        val userMap = users.associate { it.id to it.name }
        val groupMap = groups.associate { it.id to it.name }

        // Filter for specific month if specified
        val targetExpenses = if (monthCalendar != null) {
            val startCal = monthCalendar.clone() as Calendar
            startCal.set(Calendar.DAY_OF_MONTH, 1)
            startCal.set(Calendar.HOUR_OF_DAY, 0)
            startCal.set(Calendar.MINUTE, 0)
            startCal.set(Calendar.SECOND, 0)
            startCal.set(Calendar.MILLISECOND, 0)

            val endCal = monthCalendar.clone() as Calendar
            endCal.set(Calendar.DAY_OF_MONTH, endCal.getActualMaximum(Calendar.DAY_OF_MONTH))
            endCal.set(Calendar.HOUR_OF_DAY, 23)
            endCal.set(Calendar.MINUTE, 59)
            endCal.set(Calendar.SECOND, 59)
            endCal.set(Calendar.MILLISECOND, 999)

            expenses.filter { it.dateMillis in startCal.timeInMillis..endCal.timeInMillis && !it.isSettlement }
        } else {
            expenses.filter { !it.isSettlement }
        }

        val totalSpend = targetExpenses.sumOf { it.amount }
        val count = targetExpenses.size
        val avgSpend = if (count > 0) totalSpend / count else 0.0

        val categoryGroups = targetExpenses.groupBy { it.category.uppercase() }
        val categorySummaries = categoryGroups.map { (cat, list) ->
            val sum = list.sumOf { it.amount }
            val c = list.size
            CategorySummaryItem(
                category = cat,
                totalAmount = sum,
                count = c,
                percentage = if (totalSpend > 0) (sum / totalSpend).toFloat() else 0f,
                maxExpense = list.maxOfOrNull { it.amount } ?: 0.0,
                avgExpense = if (c > 0) sum / c else 0.0
            )
        }.sortedByDescending { it.totalAmount }

        val topCategory = categorySummaries.firstOrNull()?.category ?: "None"
        val currency = targetExpenses.firstOrNull()?.currency ?: "$"

        val periodLabel = if (monthCalendar != null) {
            val sdf = SimpleDateFormat("MMMM yyyy", Locale.US)
            sdf.format(monthCalendar.time)
        } else {
            periodTitleOverride ?: "All Filtered Records"
        }

        val nowStr = SimpleDateFormat("MMM d, yyyy 'at' h:mm a", Locale.US).format(Date())

        return ExpenseReportData(
            title = "Monthly Categorized Expense Report",
            periodLabel = periodLabel,
            generatedDate = nowStr,
            currency = currency,
            totalSpend = totalSpend,
            totalCount = count,
            avgSpend = avgSpend,
            topCategory = topCategory,
            categories = categorySummaries,
            transactions = targetExpenses.sortedByDescending { it.dateMillis },
            userMap = userMap,
            groupMap = groupMap
        )
    }

    // ==========================================
    // 1. CSV EXPORT & STORAGE PERSISTENCE
    // ==========================================

    /**
     * Converts a collection of stored [ExpenseEntity] instances into standard RFC 4180 CSV format.
     *
     * @param expenses The stored expense entities to convert.
     * @param userMap Optional mapping of user ID to display name for resolving payers and participants.
     * @param groupMap Optional mapping of group ID to group name.
     * @param includeHeader Whether to output the CSV column header line.
     * @return Fully formatted CSV text string ready for export or saving.
     */
    fun convertExpensesToCsv(
        expenses: List<ExpenseEntity>,
        userMap: Map<Long, String> = emptyMap(),
        groupMap: Map<Long, String> = emptyMap(),
        includeHeader: Boolean = true
    ): String {
        val sb = StringBuilder()
        val sdfDate = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val sdfTime = SimpleDateFormat("HH:mm:ss", Locale.US)

        if (includeHeader) {
            sb.append("ID,Date,Time,Title,Category,Amount,Currency,Payer,Group,Split Type,Split Details,Notes,Is Settlement,Source Message\n")
        }

        for (exp in expenses) {
            val dateStr = sdfDate.format(Date(exp.dateMillis))
            val timeStr = sdfTime.format(Date(exp.dateMillis))
            val payerName = userMap[exp.payerId] ?: "User ${exp.payerId}"
            val groupName = exp.groupId?.let { groupMap[it] } ?: "Personal"
            val formattedAmount = String.format(Locale.US, "%.2f", exp.amount)
            val isSettlementStr = if (exp.isSettlement) "Yes" else "No"
            val notesStr = exp.notes ?: ""
            val sourceStr = exp.sourceMessage ?: ""

            sb.append(escapeCsv(exp.id.toString())).append(",")
            sb.append(escapeCsv(dateStr)).append(",")
            sb.append(escapeCsv(timeStr)).append(",")
            sb.append(escapeCsv(exp.title)).append(",")
            sb.append(escapeCsv(exp.category)).append(",")
            sb.append(escapeCsv(formattedAmount)).append(",")
            sb.append(escapeCsv(exp.currency)).append(",")
            sb.append(escapeCsv(payerName)).append(",")
            sb.append(escapeCsv(groupName)).append(",")
            sb.append(escapeCsv(exp.splitType)).append(",")
            sb.append(escapeCsv(exp.splitDetailsJson)).append(",")
            sb.append(escapeCsv(notesStr)).append(",")
            sb.append(escapeCsv(isSettlementStr)).append(",")
            sb.append(escapeCsv(sourceStr)).append("\n")
        }

        return sb.toString()
    }

    /**
     * Overload to convert stored [ExpenseEntity] instances to CSV format with default headers.
     */
    fun convertExpensesToCsv(expenses: List<ExpenseEntity>): String {
        return convertExpensesToCsv(expenses, emptyMap(), emptyMap(), true)
    }

    /**
     * Converts full [ExpenseReportData] including executive summary metadata,
     * category distribution breakdown, and itemized transactions into a multi-section CSV string.
     */
    fun convertReportDataToCsv(data: ExpenseReportData): String {
        val sb = StringBuilder()
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US)

        // Meta header
        sb.append("=== SPLITEXPENSE FINANCIAL REPORT ===\n")
        sb.append("Report Period,${escapeCsv(data.periodLabel)}\n")
        sb.append("Generated On,${escapeCsv(data.generatedDate)}\n")
        sb.append("Total Spending,${String.format(Locale.US, "%.2f", data.totalSpend)}\n")
        sb.append("Total Transactions,${data.totalCount}\n")
        sb.append("Average Transaction,${String.format(Locale.US, "%.2f", data.avgSpend)}\n")
        sb.append("Top Category,${escapeCsv(data.topCategory)}\n\n")

        // Category Breakdown Table
        sb.append("=== CATEGORY BREAKDOWN ===\n")
        sb.append("Category,Total Amount,Percentage (%),Count,Average Amount,Highest Expense\n")
        for (cat in data.categories) {
            sb.append("${escapeCsv(cat.category)},")
            sb.append("${String.format(Locale.US, "%.2f", cat.totalAmount)},")
            sb.append("${String.format(Locale.US, "%.1f", cat.percentage * 100)}%,")
            sb.append("${cat.count},")
            sb.append("${String.format(Locale.US, "%.2f", cat.avgExpense)},")
            sb.append("${String.format(Locale.US, "%.2f", cat.maxExpense)}\n")
        }
        sb.append("\n")

        // Detailed Transactions Table
        sb.append("=== ITEMIZED TRANSACTIONS ===\n")
        sb.append("ID,Date,Title,Category,Amount,Currency,Group,Paid By,Split Type,Notes\n")
        for (exp in data.transactions) {
            val dateStr = sdf.format(Date(exp.dateMillis))
            val groupName = exp.groupId?.let { data.groupMap[it] } ?: "Personal"
            val payerName = data.userMap[exp.payerId] ?: "User ${exp.payerId}"
            val notes = exp.notes ?: ""

            sb.append("${escapeCsv(exp.id.toString())},")
            sb.append("${escapeCsv(dateStr)},")
            sb.append("${escapeCsv(exp.title)},")
            sb.append("${escapeCsv(exp.category)},")
            sb.append("${String.format(Locale.US, "%.2f", exp.amount)},")
            sb.append("${escapeCsv(exp.currency)},")
            sb.append("${escapeCsv(groupName)},")
            sb.append("${escapeCsv(payerName)},")
            sb.append("${escapeCsv(exp.splitType)},")
            sb.append("${escapeCsv(notes)}\n")
        }

        return sb.toString()
    }

    /**
     * Generates a temporary CSV report file in the app's cache directory.
     */
    fun generateCsvReport(context: Context, data: ExpenseReportData): File {
        val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
        val sanitizedPeriod = data.periodLabel.replace(" ", "_").replace(",", "")
        val file = File(exportDir, "Expense_Report_${sanitizedPeriod}_${System.currentTimeMillis()}.csv")

        val csvString = convertReportDataToCsv(data)
        file.writeText(csvString, Charsets.UTF_8)
        return file
    }

    /**
     * Writes CSV text content directly to a destination URI selected by the user
     * via Android's Storage Access Framework (SAF) ActivityResultContracts.CreateDocument.
     *
     * @param context Android context.
     * @param uri The destination content URI.
     * @param csvContent The CSV string to write.
     * @return True if write operation succeeded, false otherwise.
     */
    fun writeCsvToUri(context: Context, uri: Uri, csvContent: String): Boolean {
        return try {
            context.contentResolver.openOutputStream(uri, "wt")?.use { outputStream ->
                outputStream.write(csvContent.toByteArray(Charsets.UTF_8))
                outputStream.flush()
            } ?: return false
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Saves CSV formatted data directly to the device's public storage (Downloads directory).
     * On Android 10+ (API 29+), uses MediaStore.Downloads.
     * On older versions, writes to Environment.DIRECTORY_DOWNLOADS.
     *
     * @param context Android context.
     * @param csvContent The CSV formatted string to persist.
     * @param fileName The desired filename (e.g. "SplitExpense_2026-09-15.csv").
     * @return [FileSaveResult] representing either Success with the URI/path or Error.
     */
    fun saveCsvToDeviceStorage(
        context: Context,
        csvContent: String,
        fileName: String = "SplitExpense_Report_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())}.csv"
    ): FileSaveResult {
        return try {
            val sanitizedName = if (fileName.endsWith(".csv", ignoreCase = true)) fileName else "$fileName.csv"

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, sanitizedName)
                    put(MediaStore.MediaColumns.MIME_TYPE, "text/csv")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/SplitExpense")
                }
                val resolver = context.contentResolver
                val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                    ?: throw IllegalStateException("Could not create MediaStore entry in Downloads directory")

                resolver.openOutputStream(uri, "wt")?.use { out ->
                    out.write(csvContent.toByteArray(Charsets.UTF_8))
                    out.flush()
                } ?: throw IllegalStateException("Could not open output stream for URI: $uri")

                FileSaveResult.Success(uri, sanitizedName, "Downloads/SplitExpense/$sanitizedName")
            } else {
                val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                val targetDir = File(downloadsDir, "SplitExpense").apply { mkdirs() }
                val targetFile = File(targetDir, sanitizedName)
                targetFile.writeText(csvContent, Charsets.UTF_8)
                val uri = Uri.fromFile(targetFile)
                FileSaveResult.Success(uri, sanitizedName, targetFile.absolutePath)
            }
        } catch (e: Exception) {
            FileSaveResult.Error(e.message ?: "Failed to save CSV to device storage", e)
        }
    }

    /**
     * Helper to escape CSV cell values compliant with RFC 4180.
     */
    fun escapeCsv(value: String): String {
        return if (value.contains(",") || value.contains("\"") || value.contains("\n") || value.contains("\r")) {
            "\"${value.replace("\"", "\"\"")}\""
        } else {
            value
        }
    }

    // ==========================================
    // 2. PDF EXPORT
    // ==========================================

    fun generatePdfReport(context: Context, data: ExpenseReportData): File {
        val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
        val sanitizedPeriod = data.periodLabel.replace(" ", "_").replace(",", "")
        val file = File(exportDir, "Expense_Report_${sanitizedPeriod}_${System.currentTimeMillis()}.pdf")

        val pdfDoc = PdfDocument()
        val pageWidth = 595 // A4 standard width in points
        val pageHeight = 842 // A4 standard height in points
        var pageNumber = 1

        val sdf = SimpleDateFormat("MMM d, yyyy", Locale.US)

        // Setup Paints
        val primaryPaint = Paint().apply {
            color = Color.parseColor("#6750A4")
            isAntiAlias = true
        }
        val secondaryPaint = Paint().apply {
            color = Color.parseColor("#7D5260")
            isAntiAlias = true
        }
        val textDarkPaint = Paint().apply {
            color = Color.parseColor("#1C1B1F")
            textSize = 10f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            isAntiAlias = true
        }
        val textMutedPaint = Paint().apply {
            color = Color.parseColor("#79747E")
            textSize = 9f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            isAntiAlias = true
        }
        val headerTitlePaint = Paint().apply {
            color = Color.WHITE
            textSize = 18f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        val sectionTitlePaint = Paint().apply {
            color = Color.parseColor("#6750A4")
            textSize = 12f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        val tableHeaderPaint = Paint().apply {
            color = Color.parseColor("#21005D")
            textSize = 9.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        val cardBgPaint = Paint().apply {
            color = Color.parseColor("#F7F2FA")
            isAntiAlias = true
        }
        val tableBgAltPaint = Paint().apply {
            color = Color.parseColor("#F9F9FB")
            isAntiAlias = true
        }
        val borderPaint = Paint().apply {
            color = Color.parseColor("#E6E0E9")
            style = Paint.Style.STROKE
            strokeWidth = 0.8f
            isAntiAlias = true
        }
        val barColorPaint = Paint().apply {
            color = Color.parseColor("#7C4DFF")
            isAntiAlias = true
        }
        val barBgPaint = Paint().apply {
            color = Color.parseColor("#EADDFF")
            isAntiAlias = true
        }

        // --- PAGE 1: Header, Executive KPI, and Category Breakdown ---
        var pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
        var page = pdfDoc.startPage(pageInfo)
        var canvas = page.canvas

        // Header Banner
        val headerRect = RectF(0f, 0f, pageWidth.toFloat(), 85f)
        canvas.drawRect(headerRect, primaryPaint)

        canvas.drawText("EXPENSE & FINANCIAL REPORT", 30f, 38f, headerTitlePaint)
        headerTitlePaint.textSize = 11f
        headerTitlePaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas.drawText("Period: ${data.periodLabel.uppercase()} • Generated: ${data.generatedDate}", 30f, 58f, headerTitlePaint)
        headerTitlePaint.textSize = 18f
        headerTitlePaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)

        // KPI Summary Cards (4 Columns)
        var yPos = 105f
        val kpiCardWidth = (pageWidth - 60f - 30f) / 4f
        val kpiHeight = 55f

        val kpis = listOf(
            Pair("TOTAL SPEND", "${data.currency}${String.format(Locale.US, "%,.2f", data.totalSpend)}"),
            Pair("TRANSACTIONS", "${data.totalCount} items"),
            Pair("AVG / EXPENSE", "${data.currency}${String.format(Locale.US, "%,.2f", data.avgSpend)}"),
            Pair("TOP CATEGORY", data.topCategory)
        )

        for (i in kpis.indices) {
            val left = 30f + i * (kpiCardWidth + 10f)
            val rect = RectF(left, yPos, left + kpiCardWidth, yPos + kpiHeight)
            canvas.drawRoundRect(rect, 8f, 8f, cardBgPaint)
            canvas.drawRoundRect(rect, 8f, 8f, borderPaint)

            canvas.drawText(kpis[i].first, left + 10f, yPos + 18f, textMutedPaint)
            textDarkPaint.textSize = 11f
            textDarkPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText(kpis[i].second, left + 10f, yPos + 38f, textDarkPaint)
            textDarkPaint.textSize = 10f
            textDarkPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        }

        yPos += kpiHeight + 25f

        // Category Breakdown Section
        canvas.drawText("CATEGORY BREAKDOWN & DISTRIBUTION", 30f, yPos, sectionTitlePaint)
        yPos += 14f

        // Table Header for Categories
        val catHeaderRect = RectF(30f, yPos, pageWidth - 30f, yPos + 22f)
        canvas.drawRoundRect(catHeaderRect, 4f, 4f, cardBgPaint)
        canvas.drawRoundRect(catHeaderRect, 4f, 4f, borderPaint)

        canvas.drawText("Category", 40f, yPos + 15f, tableHeaderPaint)
        canvas.drawText("Share", 160f, yPos + 15f, tableHeaderPaint)
        canvas.drawText("Count", 270f, yPos + 15f, tableHeaderPaint)
        canvas.drawText("Avg", 340f, yPos + 15f, tableHeaderPaint)
        canvas.drawText("Max", 420f, yPos + 15f, tableHeaderPaint)
        canvas.drawText("Total Spend", 485f, yPos + 15f, tableHeaderPaint)

        yPos += 24f

        val maxCatsOnPage1 = 9
        val catsToDraw = data.categories.take(maxCatsOnPage1)

        for ((idx, cat) in catsToDraw.withIndex()) {
            val rowY = yPos + (idx * 26f)
            if (idx % 2 == 1) {
                canvas.drawRect(30f, rowY - 4f, pageWidth - 30f, rowY + 22f, tableBgAltPaint)
            }

            // Category Name
            textDarkPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText(cat.category, 40f, rowY + 12f, textDarkPaint)
            textDarkPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)

            // Progress bar for Share
            val barLeft = 160f
            val barWidth = 65f
            val barHeight = 6f
            val progressWidth = barWidth * cat.percentage.coerceIn(0f, 1f)
            canvas.drawRoundRect(RectF(barLeft, rowY + 6f, barLeft + barWidth, rowY + 6f + barHeight), 3f, 3f, barBgPaint)
            canvas.drawRoundRect(RectF(barLeft, rowY + 6f, barLeft + progressWidth, rowY + 6f + barHeight), 3f, 3f, barColorPaint)
            canvas.drawText("${String.format(Locale.US, "%.1f", cat.percentage * 100)}%", barLeft + barWidth + 6f, rowY + 12f, textMutedPaint)

            canvas.drawText("${cat.count}", 275f, rowY + 12f, textDarkPaint)
            canvas.drawText("${data.currency}${String.format(Locale.US, "%.2f", cat.avgExpense)}", 340f, rowY + 12f, textDarkPaint)
            canvas.drawText("${data.currency}${String.format(Locale.US, "%.2f", cat.maxExpense)}", 420f, rowY + 12f, textDarkPaint)

            textDarkPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText("${data.currency}${String.format(Locale.US, "%.2f", cat.totalAmount)}", 485f, rowY + 12f, textDarkPaint)
            textDarkPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        }

        yPos += (catsToDraw.size * 26f) + 20f

        // Recent Transactions Header preview on Page 1
        canvas.drawText("ITEMIZED TRANSACTIONS LEDGER", 30f, yPos, sectionTitlePaint)
        yPos += 14f

        val txHeaderRect = RectF(30f, yPos, pageWidth - 30f, yPos + 22f)
        canvas.drawRoundRect(txHeaderRect, 4f, 4f, cardBgPaint)
        canvas.drawRoundRect(txHeaderRect, 4f, 4f, borderPaint)

        canvas.drawText("Date", 40f, yPos + 15f, tableHeaderPaint)
        canvas.drawText("Title / Merchant", 115f, yPos + 15f, tableHeaderPaint)
        canvas.drawText("Category", 260f, yPos + 15f, tableHeaderPaint)
        canvas.drawText("Group / Entity", 355f, yPos + 15f, tableHeaderPaint)
        canvas.drawText("Payer", 440f, yPos + 15f, tableHeaderPaint)
        canvas.drawText("Amount", 505f, yPos + 15f, tableHeaderPaint)

        yPos += 24f

        // Draw transactions on Page 1 until bottom
        var txIndex = 0
        while (txIndex < data.transactions.size && yPos < pageHeight - 50f) {
            val tx = data.transactions[txIndex]
            if (txIndex % 2 == 1) {
                canvas.drawRect(30f, yPos - 4f, pageWidth - 30f, yPos + 18f, tableBgAltPaint)
            }

            val dateStr = sdf.format(Date(tx.dateMillis))
            val titleStr = if (tx.title.length > 22) tx.title.take(20) + "..." else tx.title
            val groupStr = tx.groupId?.let { data.groupMap[it] ?: "Group" } ?: "Personal"
            val payerStr = data.userMap[tx.payerId]?.take(10) ?: "You"

            canvas.drawText(dateStr, 40f, yPos + 11f, textMutedPaint)
            canvas.drawText(titleStr, 115f, yPos + 11f, textDarkPaint)
            canvas.drawText(tx.category, 260f, yPos + 11f, textMutedPaint)
            canvas.drawText(groupStr, 355f, yPos + 11f, textMutedPaint)
            canvas.drawText(payerStr, 440f, yPos + 11f, textMutedPaint)

            textDarkPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText("${tx.currency}${String.format(Locale.US, "%.2f", tx.amount)}", 505f, yPos + 11f, textDarkPaint)
            textDarkPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)

            yPos += 22f
            txIndex++
        }

        // Draw Page 1 Footer
        canvas.drawText("SplitExpense Financial Report • Page $pageNumber", 30f, pageHeight - 20f, textMutedPaint)
        canvas.drawText("Confidential", pageWidth - 80f, pageHeight - 20f, textMutedPaint)
        pdfDoc.finishPage(page)

        // --- SUBSEQUENT PAGES: Additional Transactions if needed ---
        while (txIndex < data.transactions.size) {
            pageNumber++
            pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
            page = pdfDoc.startPage(pageInfo)
            canvas = page.canvas

            // Page Header
            yPos = 40f
            canvas.drawText("ITEMIZED TRANSACTIONS LEDGER (CONTINUED)", 30f, yPos, sectionTitlePaint)
            yPos += 16f

            val contHeaderRect = RectF(30f, yPos, pageWidth - 30f, yPos + 22f)
            canvas.drawRoundRect(contHeaderRect, 4f, 4f, cardBgPaint)
            canvas.drawRoundRect(contHeaderRect, 4f, 4f, borderPaint)

            canvas.drawText("Date", 40f, yPos + 15f, tableHeaderPaint)
            canvas.drawText("Title / Merchant", 115f, yPos + 15f, tableHeaderPaint)
            canvas.drawText("Category", 260f, yPos + 15f, tableHeaderPaint)
            canvas.drawText("Group / Entity", 355f, yPos + 15f, tableHeaderPaint)
            canvas.drawText("Payer", 440f, yPos + 15f, tableHeaderPaint)
            canvas.drawText("Amount", 505f, yPos + 15f, tableHeaderPaint)

            yPos += 24f

            var pageTxCount = 0
            while (txIndex < data.transactions.size && yPos < pageHeight - 50f) {
                val tx = data.transactions[txIndex]
                if (pageTxCount % 2 == 1) {
                    canvas.drawRect(30f, yPos - 4f, pageWidth - 30f, yPos + 18f, tableBgAltPaint)
                }

                val dateStr = sdf.format(Date(tx.dateMillis))
                val titleStr = if (tx.title.length > 22) tx.title.take(20) + "..." else tx.title
                val groupStr = tx.groupId?.let { data.groupMap[it] ?: "Group" } ?: "Personal"
                val payerStr = data.userMap[tx.payerId]?.take(10) ?: "You"

                canvas.drawText(dateStr, 40f, yPos + 11f, textMutedPaint)
                canvas.drawText(titleStr, 115f, yPos + 11f, textDarkPaint)
                canvas.drawText(tx.category, 260f, yPos + 11f, textMutedPaint)
                canvas.drawText(groupStr, 355f, yPos + 11f, textMutedPaint)
                canvas.drawText(payerStr, 440f, yPos + 11f, textMutedPaint)

                textDarkPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                canvas.drawText("${tx.currency}${String.format(Locale.US, "%.2f", tx.amount)}", 505f, yPos + 11f, textDarkPaint)
                textDarkPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)

                yPos += 22f
                txIndex++
                pageTxCount++
            }

            canvas.drawText("SplitExpense Financial Report • Page $pageNumber", 30f, pageHeight - 20f, textMutedPaint)
            canvas.drawText("Confidential", pageWidth - 80f, pageHeight - 20f, textMutedPaint)
            pdfDoc.finishPage(page)
        }

        FileOutputStream(file).use { out ->
            pdfDoc.writeTo(out)
        }
        pdfDoc.close()

        return file
    }

    // ==========================================
    // 3. SHARING & INTENTS
    // ==========================================

    fun shareExportFile(context: Context, file: File, mimeType: String, title: String) {
        val uri: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, title)
            putExtra(Intent.EXTRA_TEXT, "Here is the categorized monthly financial expense report exported from SplitExpense.")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        context.startActivity(Intent.createChooser(intent, "Share Expense Report via"))
    }

    fun openExportFile(context: Context, file: File, mimeType: String) {
        val uri: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )

        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, mimeType)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            // Fallback to sharing if no default viewer
            shareExportFile(context, file, mimeType, "Expense Report")
        }
    }
}
