package com.example.data.parser

import java.text.SimpleDateFormat
import java.util.Locale
import java.util.regex.Pattern

data class StatementTransaction(
    val id: String = java.util.UUID.randomUUID().toString(),
    val dateStr: String,
    val dateMillis: Long,
    val description: String,
    val cleanMerchant: String,
    val amount: Double,
    val isDebit: Boolean,
    val category: String,
    val currency: String = "$",
    val rawLine: String = "",
    var isSelected: Boolean = true
)

object StatementParserEngine {

    /**
     * Parses a bank or credit card statement from raw CSV, TSV, or plain text.
     */
    fun parseStatement(rawText: String, defaultCurrency: String = "$"): List<StatementTransaction> {
        if (rawText.isBlank()) return emptyList()

        val lines = rawText.lines().map { it.trim() }.filter { it.isNotBlank() }
        val transactions = mutableListOf<StatementTransaction>()

        // Check if CSV with header
        val isCsv = lines.any { it.contains(",") }

        for (line in lines) {
            // Skip typical header rows
            val lower = line.lowercase()
            if (lower.startsWith("date") || lower.startsWith("transaction date") || lower.startsWith("posting date") ||
                lower.contains("account summary") || lower.contains("opening balance") || lower.contains("closing balance") ||
                lower.startsWith("page ") || lower.contains("statement period")) {
                continue
            }

            val tx = parseStatementLine(line, defaultCurrency)
            if (tx != null && tx.amount > 0.0) {
                transactions.add(tx)
            }
        }

        return transactions
    }

    private fun parseStatementLine(line: String, defaultCurrency: String): StatementTransaction? {
        val now = System.currentTimeMillis()

        // 1. If comma or tab delimited
        if (line.contains(",") || line.contains("\t") || line.contains(";")) {
            val delimiter = if (line.contains(",")) "," else if (line.contains("\t")) "\t" else ";"
            val parts = splitCsvLine(line, delimiter)
            if (parts.size >= 3) {
                var dateStr = ""
                var desc = ""
                var amount = 0.0
                var isDebit = true

                // Detect date column (usually index 0 or 1)
                for (p in parts) {
                    if (dateStr.isEmpty() && isLikelyDate(p)) {
                        dateStr = p.trim('\"', ' ')
                    } else if (desc.isEmpty() && p.length > 2 && !isLikelyAmount(p) && !isLikelyDate(p)) {
                        desc = p.trim('\"', ' ')
                    } else if (amount == 0.0 && isLikelyAmount(p)) {
                        val cleanAmt = p.replace("$", "").replace("₹", "").replace("€", "").replace("£", "").replace(",", "").trim('\"', ' ')
                        val amtVal = cleanAmt.toDoubleOrNull()
                        if (amtVal != null) {
                            amount = kotlin.math.abs(amtVal)
                            isDebit = !p.contains("+") && (cleanAmt.startsWith("-") || !lowerContainsCredit(line))
                        }
                    }
                }

                if (desc.isNotBlank() && amount > 0.0) {
                    val category = inferCategory(desc)
                    val merchant = cleanMerchantName(desc)
                    return StatementTransaction(
                        dateStr = dateStr.ifBlank { "Recent" },
                        dateMillis = parseDateToMillis(dateStr) ?: now,
                        description = desc,
                        cleanMerchant = merchant,
                        amount = amount,
                        isDebit = isDebit,
                        category = category,
                        currency = defaultCurrency,
                        rawLine = line,
                        isSelected = isDebit // Default select expenses, skip salary/credits
                    )
                }
            }
        }

        // 2. Free-text statement line parsing
        val datePattern = Pattern.compile("""(\d{1,2}[/-]\d{1,2}[/-]\d{2,4}|\d{4}-\d{2}-\d{2}|\b(?:Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec)[a-z]*\s+\d{1,2}(?:,?\s+\d{4})?)""", Pattern.CASE_INSENSITIVE)
        val amountPattern = Pattern.compile("""(?:[\$₹€£¥]\s*)?([0-9]+(?:,[0-9]{3})*\.[0-9]{2})""")

        val dateMatcher = datePattern.matcher(line)
        val amountMatcher = amountPattern.matcher(line)

        var foundDate = ""
        if (dateMatcher.find()) {
            foundDate = dateMatcher.group(1) ?: ""
        }

        var foundAmount = 0.0
        var isDebit = true
        if (amountMatcher.find()) {
            val rawAmt = amountMatcher.group(1)?.replace(",", "")
            foundAmount = rawAmt?.toDoubleOrNull() ?: 0.0
            if (line.contains("CR", ignoreCase = true) || line.contains("CREDIT", ignoreCase = true) || line.contains("+")) {
                isDebit = false
            }
        }

        if (foundAmount > 0.0) {
            var cleanDesc = line
            if (foundDate.isNotEmpty()) cleanDesc = cleanDesc.replace(foundDate, "")
            cleanDesc = cleanDesc.replace(Regex("""[\$₹€£¥]?\s*[0-9]+(?:,[0-9]{3})*\.[0-9]{2}"""), "")
            cleanDesc = cleanDesc.replace(Regex("""\b(DEBIT|CREDIT|DR|CR|PURCHASE|POS|CARD\s+\d+)\b""", RegexOption.IGNORE_CASE), "")
            cleanDesc = cleanDesc.trim().trim('-', ':', ',', '|', ' ')

            if (cleanDesc.isBlank()) cleanDesc = "Card Transaction"
            val category = inferCategory(cleanDesc)
            val merchant = cleanMerchantName(cleanDesc)

            return StatementTransaction(
                dateStr = foundDate.ifBlank { "Recent" },
                dateMillis = parseDateToMillis(foundDate) ?: now,
                description = cleanDesc,
                cleanMerchant = merchant,
                amount = foundAmount,
                isDebit = isDebit,
                category = category,
                currency = defaultCurrency,
                rawLine = line,
                isSelected = isDebit
            )
        }

        return null
    }

    private fun isLikelyDate(text: String): Boolean {
        val t = text.trim('\"', ' ')
        return t.matches(Regex("""^\d{1,2}[/-]\d{1,2}[/-]\d{2,4}$""")) ||
               t.matches(Regex("""^\d{4}-\d{2}-\d{2}$""")) ||
               t.matches(Regex("""^(Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec)[a-z]*\s+\d{1,2}(,?\s+\d{4})?$""", RegexOption.IGNORE_CASE))
    }

    private fun isLikelyAmount(text: String): Boolean {
        val clean = text.replace("$", "").replace("₹", "").replace("€", "").replace("£", "").replace(",", "").trim('\"', ' ')
        return clean.matches(Regex("""^-?[0-9]+(\.[0-9]{1,2})?$"""))
    }

    private fun lowerContainsCredit(line: String): Boolean {
        val l = line.lowercase()
        return l.contains("credit") || l.contains(" cr ") || l.contains("payroll") || l.contains("deposit") || l.contains("refund")
    }

    private fun splitCsvLine(line: String, delimiter: String): List<String> {
        val result = mutableListOf<String>()
        var cur = StringBuilder()
        var inQuotes = false
        for (ch in line) {
            if (ch == '\"') {
                inQuotes = !inQuotes
            } else if (ch.toString() == delimiter && !inQuotes) {
                result.add(cur.toString().trim())
                cur = StringBuilder()
            } else {
                cur.append(ch)
            }
        }
        result.add(cur.toString().trim())
        return result
    }

    private fun parseDateToMillis(dateStr: String): Long? {
        val formats = listOf(
            "MM/dd/yyyy", "MM/dd/yy", "yyyy-MM-dd", "dd-MMM-yyyy", "dd/MM/yyyy", "MMM d, yyyy", "MMM d"
        )
        for (fmt in formats) {
            try {
                val sdf = SimpleDateFormat(fmt, Locale.US)
                val d = sdf.parse(dateStr)
                if (d != null) return d.time
            } catch (_: Exception) {}
        }
        return null
    }

    private fun inferCategory(desc: String): String {
        val l = desc.lowercase()
        return when {
            l.contains("starbucks") || l.contains("coffee") || l.contains("cafe") ||
            l.contains("restaurant") || l.contains("uber eats") || l.contains("doordash") ||
            l.contains("mcdonald") || l.contains("chipotle") || l.contains("pizza") ||
            l.contains("bakery") || l.contains("diner") || l.contains("bar") || l.contains("swiggy") || l.contains("zomato") -> "FOOD"

            l.contains("uber") || l.contains("lyft") || l.contains("chevron") ||
            l.contains("shell") || l.contains("delta air") || l.contains("united air") ||
            l.contains("parking") || l.contains("transit") || l.contains("gas") || l.contains("flight") -> "TRAVEL"

            l.contains("walmart") || l.contains("target") || l.contains("amazon") ||
            l.contains("costco") || l.contains("trader joe") || l.contains("whole foods") ||
            l.contains("safeway") || l.contains("grocery") || l.contains("supermarket") ||
            l.contains("apple store") || l.contains("best buy") || l.contains("zara") -> "SHOPPING"

            l.contains("netflix") || l.contains("spotify") || l.contains("hulu") ||
            l.contains("cinema") || l.contains("amc") || l.contains("disney") ||
            l.contains("hbo") || l.contains("concert") || l.contains("ticket") -> "ENTERTAINMENT"

            l.contains("electric") || l.contains("water") || l.contains("internet") ||
            l.contains("comcast") || l.contains("at&t") || l.contains("verizon") ||
            l.contains("pge") || l.contains("utility") || l.contains("wifi") -> "UTILITIES"

            l.contains("airbnb") || l.contains("hotel") || l.contains("marriott") ||
            l.contains("hilton") || l.contains("rent") || l.contains("leasing") -> "RENT"

            else -> "GENERAL"
        }
    }

    private fun cleanMerchantName(desc: String): String {
        var clean = desc.replace(Regex("""\b(POS|PURCHASE|CARD|DEBIT|CREDIT|RECURRING|TXN|AUTH|ONLINE|PAYMENT)\b""", RegexOption.IGNORE_CASE), "")
        clean = clean.replace(Regex("""#\d+"""), "")
        clean = clean.replace(Regex("""\d{4,}\b"""), "")
        clean = clean.replace(Regex("""\s+"""), " ").trim()
        if (clean.length > 25) {
            clean = clean.substring(0, 25).trim()
        }
        return if (clean.isBlank()) desc.take(20) else clean
    }

    val SAMPLE_STATEMENTS = listOf(
        SampleStatement(
            title = "Chase Sapphire Card Statement (CSV)",
            accountName = "Chase Sapphire Preferred",
            rawContent = """
                Transaction Date,Post Date,Description,Category,Type,Amount,Memo
                08/28/2026,08/29/2026,TRADER JOE'S #142 SAN FRANCISCO,Groceries,Sale,-78.40,Weekly Groceries
                08/29/2026,08/30/2026,UBER TRIP SFO AIRPORT,Travel,Sale,-46.20,Ride to Airport
                08/30/2026,08/31/2026,STARBUCKS COFFEE DOWNTOWN,Dining,Sale,-14.75,Morning Coffee
                08/31/2026,09/01/2026,NETFLIX.COM MONTHLY,Entertainment,Sale,-19.99,Streaming
                09/01/2026,09/02/2026,CHIPOTLE MEXICAN GRILL,Dining,Sale,-32.50,Dinner with Alex
                09/02/2026,09/02/2026,CHEVRON GAS STATION 0841,Gas,Sale,-55.00,Fuel Refill
                09/02/2026,09/03/2026,AIRBNB LAKE TAHOE CABIN,Travel,Sale,-240.00,Cabin Weekend
            """.trimIndent()
        ),
        SampleStatement(
            title = "Bank of America Checking Statement",
            accountName = "Bank of America Checking",
            rawContent = """
                08/27/2026  COMCAST FIBER INTERNET UTILITY BILL  $75.00
                08/29/2026  WHOLE FOODS MARKET GROCERY           $112.30
                08/30/2026  PG&E ELECTRIC & GAS UTILITY          $89.45
                09/01/2026  TARGET DOWNTOWN SUPPLIES             $64.20
                09/01/2026  BLUE BOTTLE COFFEE CAFE              $16.50
                09/02/2026  SAFEWAY SUPERMARKET DINNER           $52.10
            """.trimIndent()
        ),
        SampleStatement(
            title = "Apple Card Monthly Export",
            accountName = "Apple Card (Mastercard)",
            rawContent = """
                Transaction Date,Clearing Date,Description,Merchant,Category,Amount (USD)
                08/25/2026,08/26/2026,Apple Services iCloud+ & Music,Apple Inc,Services,12.99
                08/28/2026,08/29/2026,Doordash Italian Pizza Kitchen,DoorDash,Dining,48.50
                08/31/2026,09/01/2026,AMC Theatres Cinema Tickets,AMC,Entertainment,36.00
                09/01/2026,09/01/2026,Uber Eats Sushi Delivery,UberEats,Dining,54.20
                09/02/2026,09/02/2026,Trader Joe's Snack Supplies,Trader Joe's,Groceries,28.75
            """.trimIndent()
        )
    )
}

data class SampleStatement(
    val title: String,
    val accountName: String,
    val rawContent: String
)
