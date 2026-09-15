package com.example.data.parser

import java.util.regex.Pattern

object MessageParserEngine {

    /**
     * Parse message using dual engine:
     * 1. Tries Gemini AI parser first if API key configured and reachable.
     * 2. Uses high-accuracy Local Regex & NLP rule engine as primary offline parser.
     */
    suspend fun parse(rawText: String): ParsedExpenseResult {
        val trimmed = rawText.trim()
        if (trimmed.isBlank()) {
            return ParsedExpenseResult(
                title = "Expense",
                amount = 0.0,
                currency = "$",
                category = "GENERAL",
                merchant = "",
                dateMillis = System.currentTimeMillis(),
                rawMessage = rawText,
                confidence = 0.0f
            )
        }

        // Try AI first
        val aiResult = GeminiParserClient.parseMessageWithGemini(trimmed)
        if (aiResult != null && aiResult.amount > 0) {
            return aiResult
        }

        // Fallback to local intelligent Regex & NLP Engine
        return parseLocalNLP(trimmed)
    }

    /**
     * Parse voice-to-text spoken expense:
     * 1. Uses Gemini AI with voice-tuned prompts to extract amount, title, category, merchant, split.
     * 2. Fallback to spoken-aware local NLP engine if offline or API key missing.
     */
    suspend fun parseVoiceTranscript(spokenText: String): ParsedExpenseResult {
        val trimmed = spokenText.trim()
        if (trimmed.isBlank()) {
            return ParsedExpenseResult(
                title = "Expense",
                amount = 0.0,
                currency = "$",
                category = "GENERAL",
                merchant = "",
                dateMillis = System.currentTimeMillis(),
                rawMessage = spokenText,
                confidence = 0.0f
            )
        }

        // 1. Try Gemini AI voice parsing
        val aiResult = GeminiParserClient.parseVoiceExpenseWithGemini(trimmed)
        if (aiResult != null && aiResult.amount > 0) {
            return aiResult
        }

        // 2. Fallback to Spoken-Aware Local NLP
        val normalizedSpoken = preprocessSpokenText(trimmed)
        val localResult = parseLocalNLP(normalizedSpoken)
        return localResult.copy(
            rawMessage = spokenText,
            notes = localResult.notes ?: "Spoken: \"$spokenText\""
        )
    }

    /**
     * Normalizes spoken number words (e.g., "twenty dollars" -> "20 dollars", "fifty bucks" -> "50 bucks")
     */
    private fun preprocessSpokenText(text: String): String {
        var result = text

        // Replace common spoken word numbers
        val wordNumberMap = listOf(
            Regex("""\bone hundred fifty\b""", RegexOption.IGNORE_CASE) to "150",
            Regex("""\bone hundred\b""", RegexOption.IGNORE_CASE) to "100",
            Regex("""\bninety\b""", RegexOption.IGNORE_CASE) to "90",
            Regex("""\beighty\b""", RegexOption.IGNORE_CASE) to "80",
            Regex("""\bseventy\b""", RegexOption.IGNORE_CASE) to "70",
            Regex("""\bsixty\b""", RegexOption.IGNORE_CASE) to "60",
            Regex("""\bfifty\b""", RegexOption.IGNORE_CASE) to "50",
            Regex("""\bforty\b""", RegexOption.IGNORE_CASE) to "40",
            Regex("""\bthirty\b""", RegexOption.IGNORE_CASE) to "30",
            Regex("""\btwenty five\b""", RegexOption.IGNORE_CASE) to "25",
            Regex("""\btwenty\b""", RegexOption.IGNORE_CASE) to "20",
            Regex("""\bfifteen\b""", RegexOption.IGNORE_CASE) to "15",
            Regex("""\btwelve\b""", RegexOption.IGNORE_CASE) to "12",
            Regex("""\bten\b""", RegexOption.IGNORE_CASE) to "10",
            Regex("""\bnine\b""", RegexOption.IGNORE_CASE) to "9",
            Regex("""\beight\b""", RegexOption.IGNORE_CASE) to "8",
            Regex("""\bseven\b""", RegexOption.IGNORE_CASE) to "7",
            Regex("""\bsix\b""", RegexOption.IGNORE_CASE) to "6",
            Regex("""\bfive\b""", RegexOption.IGNORE_CASE) to "5",
            Regex("""\bfour\b""", RegexOption.IGNORE_CASE) to "4",
            Regex("""\bthree\b""", RegexOption.IGNORE_CASE) to "3",
            Regex("""\btwo\b""", RegexOption.IGNORE_CASE) to "2",
            Regex("""\bone\b""", RegexOption.IGNORE_CASE) to "1"
        )

        for ((regex, replacement) in wordNumberMap) {
            result = result.replace(regex, replacement)
        }

        return result
    }

    /**
     * Parse multiple notifications / SMS alerts / transaction streams in batch.
     */
    suspend fun parseMultipleMessages(rawBatchText: String): List<ParsedExpenseResult> {
        val trimmed = rawBatchText.trim()
        if (trimmed.isBlank()) return emptyList()

        // Split by clear message boundaries: double newlines, dashes, or numbered lines
        val rawBlocks = splitIntoMessageBlocks(trimmed)
        val results = mutableListOf<ParsedExpenseResult>()

        for (block in rawBlocks) {
            val res = parse(block)
            if (res.amount > 0.0) {
                results.add(res)
            }
        }

        return results
    }

    private fun splitIntoMessageBlocks(text: String): List<String> {
        // Check for double newline separation
        if (text.contains("\n\n")) {
            return text.split(Regex("""\n\s*\n""")).map { it.trim() }.filter { it.isNotBlank() }
        }

        // Check for numbered bullets (e.g., "1. Bank alert... 2. Uber ride...")
        if (text.contains(Regex("""\n\d+[\.\)]\s+"""))) {
            return text.split(Regex("""(?<=\n|^)\d+[\.\)]\s+""")).map { it.trim() }.filter { it.isNotBlank() }
        }

        // Check for delimiter lines like --- or ===
        if (text.contains(Regex("""\n[-=_]{3,}\n"""))) {
            return text.split(Regex("""\n[-=_]{3,}\n""")).map { it.trim() }.filter { it.isNotBlank() }
        }

        // Check if each line is an SMS/alert
        val lines = text.lines().map { it.trim() }.filter { it.isNotBlank() }
        if (lines.size > 1 && lines.all { it.contains("$") || it.contains("₹") || it.contains("€") || it.contains("paid") || it.contains("debited") }) {
            return lines
        }

        // Default single block
        return listOf(text)
    }

    /**
     * Local Smart NLP & Regex parsing rules for SMS, Bank notifications, UPI, and Chat texts.
     */
    fun parseLocalNLP(text: String): ParsedExpenseResult {
        var detectedAmount = 0.0
        var detectedCurrency = "$"
        var detectedMerchant = ""
        var detectedTitle = ""
        var detectedCategory = "GENERAL"
        var splitCount = 2
        val participantNames = mutableListOf<String>()

        // 1. Currency detection
        when {
            text.contains("₹") || text.contains("INR", ignoreCase = true) || text.contains("Rs.", ignoreCase = true) || text.contains("Rs ", ignoreCase = true) -> detectedCurrency = "₹"
            text.contains("€") || text.contains("EUR", ignoreCase = true) -> detectedCurrency = "€"
            text.contains("£") || text.contains("GBP", ignoreCase = true) -> detectedCurrency = "£"
            text.contains("¥") || text.contains("JPY", ignoreCase = true) -> detectedCurrency = "¥"
            text.contains("CAD", ignoreCase = true) || text.contains("C$") -> detectedCurrency = "C$"
            else -> detectedCurrency = "$"
        }

        // 2. Amount extraction regex
        // Matches: $123.45, 123.45 USD, INR 1,450.00, Rs 500, 45.00 total, paid 850
        val amountRegexes = listOf(
            Pattern.compile("""(?:[\$₹€£¥]|INR|USD|EUR|GBP|Rs\.?)\s*([0-9]+(?:,[0-9]{3})*(?:\.[0-9]{1,2})?)""", Pattern.CASE_INSENSITIVE),
            Pattern.compile("""(?:debited|paid|charged|sent|spent|total|bill|amount|cost|for)\s*(?:of|by|is|was)?\s*(?:[\$₹€£¥]|INR|USD|EUR|GBP|Rs\.?)?\s*([0-9]+(?:,[0-9]{3})*(?:\.[0-9]{1,2})?)""", Pattern.CASE_INSENSITIVE),
            Pattern.compile("""([0-9]+(?:,[0-9]{3})*(?:\.[0-9]{1,2})?)\s*(?:[\$₹€£¥]|INR|USD|EUR|GBP|bucks|dollars|rupees)""", Pattern.CASE_INSENSITIVE),
            Pattern.compile("""\b([0-9]+\.[0-9]{2})\b""")
        )

        for (regex in amountRegexes) {
            val matcher = regex.matcher(text)
            if (matcher.find()) {
                val rawAmtStr = matcher.group(1)?.replace(",", "")
                val parsed = rawAmtStr?.toDoubleOrNull()
                if (parsed != null && parsed > 0.0) {
                    detectedAmount = parsed
                    break
                }
            }
        }

        // 3. Merchant / Store / Service extraction
        val merchantPatterns = listOf(
            Pattern.compile("""(?:at|to|on|for|vpa|merchant)\s+([A-Z0-9\s&'.-]{3,25}?)(?:\s+(?:on|ref|dated|bal|via|using|\.|\,))""", Pattern.CASE_INSENSITIVE),
            Pattern.compile("""(?:at|to|from)\s+([A-Za-z0-9\s&'.-]{3,20})""", Pattern.CASE_INSENSITIVE)
        )

        for (pattern in merchantPatterns) {
            val m = pattern.matcher(text)
            if (m.find()) {
                val candidate = m.group(1)?.trim()?.replace(Regex("""\s+"""), " ") ?: ""
                val lower = candidate.lowercase()
                if (candidate.isNotEmpty() && !lower.contains("account") && !lower.contains("card") && !lower.contains("split") && !lower.contains("people")) {
                    detectedMerchant = candidate
                    break
                }
            }
        }

        // 4. Split and Person extraction from Chat Messages
        // Matches: "split 3 ways", "between 4 of us", "3 people", "with Alex and Bob"
        val splitRegex = Pattern.compile("""(?:split|between|among)\s+([0-9]+)\s*(?:ways|people|of us)?""", Pattern.CASE_INSENSITIVE)
        val splitMatcher = splitRegex.matcher(text)
        if (splitMatcher.find()) {
            splitCount = splitMatcher.group(1)?.toIntOrNull()?.coerceIn(2, 20) ?: 2
        }

        // Check for names after "with", "and", "between"
        val withNamesRegex = Pattern.compile("""(?:with|and|including)\s+([A-Z][a-z]+(?:\s*(?:and|,)\s*[A-Z][a-z]+)*)""")
        val nameMatcher = withNamesRegex.matcher(text)
        if (nameMatcher.find()) {
            val namesStr = nameMatcher.group(1) ?: ""
            val foundNames = namesStr.split(Regex(""",|\band\b"""))
                .map { it.trim() }
                .filter { it.length in 2..20 && it[0].isUpperCase() }
            participantNames.addAll(foundNames)
            if (participantNames.isNotEmpty()) {
                splitCount = (participantNames.size + 1).coerceIn(2, 20) // +1 for "You"
            }
        }

        // 5. Category and Title Classification
        val lowerText = text.lowercase()
        when {
            lowerText.contains("dinner") || lowerText.contains("lunch") || lowerText.contains("pizza") ||
            lowerText.contains("burger") || lowerText.contains("sushi") || lowerText.contains("swiggy") ||
            lowerText.contains("zomato") || lowerText.contains("doordash") || lowerText.contains("ubereats") ||
            lowerText.contains("restaurant") || lowerText.contains("cafe") || lowerText.contains("coffee") ||
            lowerText.contains("starbucks") || lowerText.contains("bar") || lowerText.contains("drinks") ||
            lowerText.contains("food") -> {
                detectedCategory = "FOOD"
                detectedTitle = if (detectedMerchant.isNotBlank()) "Dining at $detectedMerchant" else "Group Meal / Food"
            }

            lowerText.contains("uber") || lowerText.contains("lyft") || lowerText.contains("ola") ||
            lowerText.contains("taxi") || lowerText.contains("flight") || lowerText.contains("gas") ||
            lowerText.contains("fuel") || lowerText.contains("parking") || lowerText.contains("toll") ||
            lowerText.contains("metro") || lowerText.contains("train") || lowerText.contains("ride") -> {
                detectedCategory = "TRAVEL"
                detectedTitle = if (detectedMerchant.isNotBlank()) "$detectedMerchant Ride" else "Travel & Ride"
            }

            lowerText.contains("grocery") || lowerText.contains("groceries") || lowerText.contains("trader joe") ||
            lowerText.contains("walmart") || lowerText.contains("target") || lowerText.contains("costco") ||
            lowerText.contains("safeway") || lowerText.contains("whole foods") || lowerText.contains("supermarket") ||
            lowerText.contains("shopping") || lowerText.contains("amazon") -> {
                detectedCategory = "SHOPPING"
                detectedTitle = if (detectedMerchant.isNotBlank()) "$detectedMerchant Shopping" else "Groceries & Supplies"
            }

            lowerText.contains("movie") || lowerText.contains("cinema") || lowerText.contains("concert") ||
            lowerText.contains("netflix") || lowerText.contains("spotify") || lowerText.contains("tickets") ||
            lowerText.contains("bowling") || lowerText.contains("game") -> {
                detectedCategory = "ENTERTAINMENT"
                detectedTitle = if (detectedMerchant.isNotBlank()) "$detectedMerchant Entertainment" else "Entertainment & Outing"
            }

            lowerText.contains("wifi") || lowerText.contains("internet") || lowerText.contains("electric") ||
            lowerText.contains("water") || lowerText.contains("power") || lowerText.contains("utility") ||
            lowerText.contains("bill") -> {
                detectedCategory = "UTILITIES"
                detectedTitle = if (detectedMerchant.isNotBlank()) "$detectedMerchant Bill" else "Utilities & Bills"
            }

            lowerText.contains("rent") || lowerText.contains("airbnb") || lowerText.contains("hotel") ||
            lowerText.contains("stay") || lowerText.contains("apartment") || lowerText.contains("cabin") -> {
                detectedCategory = "RENT"
                detectedTitle = if (detectedMerchant.isNotBlank()) "$detectedMerchant Stay" else "Rent & Accommodation"
            }

            else -> {
                detectedCategory = "GENERAL"
                detectedTitle = if (detectedMerchant.isNotBlank()) "Payment at $detectedMerchant" else "General Expense"
            }
        }

        // Clean up title
        if (detectedTitle.isBlank()) {
            detectedTitle = "Shared Expense"
        }

        return ParsedExpenseResult(
            title = detectedTitle,
            amount = detectedAmount,
            currency = detectedCurrency,
            category = detectedCategory,
            merchant = detectedMerchant,
            dateMillis = System.currentTimeMillis(),
            payerNameHint = if (lowerText.contains("you paid") || lowerText.contains("debited from your")) "You" else null,
            participantNames = participantNames,
            suggestedSplitCount = splitCount,
            rawMessage = text,
            confidence = if (detectedAmount > 0) 0.92f else 0.4f,
            parsedByAi = false
        )
    }

    /**
     * Useful pre-configured templates for quick demo and testing in UI
     */
    val SAMPLE_TEMPLATES = listOf(
        SampleMessageTemplate(
            title = "Bank Debit SMS",
            category = "SHOPPING",
            rawText = "Acct XX9812 Debited with USD 84.50 on 02-Sep-26 at WHOLE FOODS MKT. Avail Bal: USD 2,840.10. Ref: 98127391."
        ),
        SampleMessageTemplate(
            title = "WhatsApp Dinner Split",
            category = "FOOD",
            rawText = "Hey everyone! The Italian pizza and pasta dinner was $135 total with tip. Split 3 ways between Alex, Emma and me!"
        ),
        SampleMessageTemplate(
            title = "Uber Airport Ride",
            category = "TRAVEL",
            rawText = "Your Uber receipt: USD 46.20 charged to card ending 4120 for trip to SFO Airport with Liam."
        ),
        SampleMessageTemplate(
            title = "Roommates Utilities",
            category = "UTILITIES",
            rawText = "Hey roommates, Fiber Internet $75.00 + Electricity $105.00 = Total $180.00. 3 of us split equally ($60 each)."
        ),
        SampleMessageTemplate(
            title = "UPI Payment Alert",
            category = "FOOD",
            rawText = "Paid ₹1,650.00 to BBQ Nation via Google Pay for team lunch on 02 Sep. UPI Ref: 38291049281."
        )
    )

    val SAMPLE_BATCH_TEMPLATES = listOf(
        SampleBatchTemplate(
            title = "Weekend Trip 4-Alert Stream",
            description = "Gas, Dinner, Grocery & Cabin",
            rawBatch = """
                1. Bank Alert: Debited $72.40 at CHEVRON GAS 0812 on 01-Sep for fuel.
                
                2. Venmo Alert: You paid Emma $85.00 for Friday Night Tacos & Drinks.
                
                3. Card Alert: $142.80 charged at TRADER JOE'S #142 groceries for cabin.
                
                4. Airbnb Reservation: $320.00 charged for Tahoe Cabin Weekend split with 4 people.
            """.trimIndent()
        ),
        SampleBatchTemplate(
            title = "Daily Push Notifications (3 SMS)",
            description = "Coffee, Uber & Lunch",
            rawBatch = """
                Chase Alert: $12.75 at Blue Bottle Coffee Downtown.
                
                Uber Alert: Trip with Alex $34.50 charged to Card ...4120.
                
                Doordash: $48.20 for Sushi Bento Box lunch delivery split 2 ways.
            """.trimIndent()
        ),
        SampleBatchTemplate(
            title = "Monthly Utility & Bills Batch",
            description = "Internet, Power & Streaming",
            rawBatch = """
                1. Auto-pay: $80.00 charged for Comcast Fiber Internet Bill.
                
                2. PG&E Power: $115.50 debited for Home Electricity & Heating split 3 ways.
                
                3. Netflix.com: $22.99 monthly family subscription charged to Apple Card.
            """.trimIndent()
        )
    )
}

data class SampleBatchTemplate(
    val title: String,
    val description: String,
    val rawBatch: String
)

data class SampleMessageTemplate(
    val title: String,
    val category: String,
    val rawText: String
)
