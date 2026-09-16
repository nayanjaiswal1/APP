package com.example.data.parser

import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class ParsedItem(
    val name: String,
    val amount: Double
)

data class ParsedExpenseResult(
    val title: String,
    val amount: Double,
    val currency: String,
    val category: String,
    val merchant: String,
    val dateMillis: Long,
    val payerNameHint: String? = null,
    val participantNames: List<String> = emptyList(),
    val suggestedSplitCount: Int = 2,
    val rawMessage: String = "",
    val confidence: Float = 0.9f,
    val notes: String? = null,
    val itemizedBreakdown: List<ParsedItem> = emptyList(),
    val parsedByAi: Boolean = false
)

object GeminiParserClient {
    private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent"

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    suspend fun parseMessageWithGemini(rawMessage: String): ParsedExpenseResult? = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext null
        }

        try {
            val systemPrompt = """
                You are an expert expense and bill parsing AI. Extract financial transaction or bill splitting details from the given text message, SMS, receipt, or chat.
                Return ONLY valid JSON matching this exact structure:
                {
                   "title": "Short descriptive title (e.g. Dinner with Friends, Uber Ride, Trader Joe's)",
                   "amount": 0.00,
                   "currency": "$ or ₹ or € or £",
                   "category": "FOOD, TRAVEL, SHOPPING, ENTERTAINMENT, UTILITIES, RENT, or GENERAL",
                   "merchant": "Merchant or Store name if any",
                   "payerNameHint": "Name of person who paid if mentioned (or You)",
                   "participantNames": ["name1", "name2"],
                   "suggestedSplitCount": 2,
                   "notes": "Any extra notes or itemized detail",
                   "confidence": 0.95
                }
            """.trimIndent()

            val requestJson = JSONObject().apply {
                put("contents", JSONArray().apply {
                    put(JSONObject().apply {
                        put("parts", JSONArray().apply {
                            put(JSONObject().apply {
                                put("text", "$systemPrompt\n\nInput Message to parse:\n\"$rawMessage\"")
                            })
                        })
                    })
                })
                put("generationConfig", JSONObject().apply {
                    put("temperature", 0.1)
                    put("responseMimeType", "application/json")
                })
            }

            val requestBody = requestJson.toString().toRequestBody("application/json".toMediaType())
            val request = Request.Builder()
                .url(BASE_URL)
                .addHeader("x-goog-api-key", apiKey)
                .post(requestBody)
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                return@withContext null
            }

            val responseBody = response.body?.string() ?: return@withContext null
            parseJsonResponse(responseBody, rawMessage)
        } catch (e: Exception) {
            null
        }
    }

    suspend fun parseVoiceExpenseWithGemini(spokenTranscript: String): ParsedExpenseResult? = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext null
        }

        try {
            val systemPrompt = """
                You are an expert voice-to-expense financial assistant. The user spoke an expense into the microphone (e.g., 'Spent 20 dollars on lunch', 'Paid 45 dollars for dinner with Alex and Bob', 'Bought 85 dollars groceries at Trader Joe's', 'Uber ride 25 dollars', 'Paid electricity bill 120 bucks', 'Coffee at Starbucks 5 dollars').
                Extract and parse the spoken expense into clean structured JSON.
                Convert spoken word numbers (e.g., 'twenty dollars' -> 20.0, 'forty five' -> 45.0, 'one hundred' -> 100.0) into exact numeric amounts.
                Return ONLY valid JSON with this exact schema:
                {
                   "title": "Short clean title (e.g. Lunch, Dinner with Friends, Trader Joe's Groceries, Uber Ride, Electricity Bill)",
                   "amount": 0.00,
                   "currency": "$ or ₹ or € or £",
                   "category": "FOOD, TRAVEL, SHOPPING, ENTERTAINMENT, UTILITIES, RENT, or GENERAL",
                   "merchant": "Merchant / store / app name if mentioned (e.g. Starbucks, Uber, Trader Joe's, Target)",
                   "payerNameHint": "Name of payer if stated (or You)",
                   "participantNames": ["name1", "name2"],
                   "suggestedSplitCount": 1,
                   "notes": "Spoken description or context",
                   "confidence": 0.98
                }
            """.trimIndent()

            val requestJson = JSONObject().apply {
                put("contents", JSONArray().apply {
                    put(JSONObject().apply {
                        put("parts", JSONArray().apply {
                            put(JSONObject().apply {
                                put("text", "$systemPrompt\n\nSpoken Expense to parse:\n\"$spokenTranscript\"")
                            })
                        })
                    })
                })
                put("generationConfig", JSONObject().apply {
                    put("temperature", 0.1)
                    put("responseMimeType", "application/json")
                })
            }

            val requestBody = requestJson.toString().toRequestBody("application/json".toMediaType())
            val request = Request.Builder()
                .url(BASE_URL)
                .addHeader("x-goog-api-key", apiKey)
                .post(requestBody)
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                return@withContext null
            }

            val responseBody = response.body?.string() ?: return@withContext null
            parseJsonResponse(responseBody, spokenTranscript)
        } catch (e: Exception) {
            null
        }
    }

    private fun parseJsonResponse(responseBody: String, sourceText: String): ParsedExpenseResult? {
        val responseJson = JSONObject(responseBody)
        val candidates = responseJson.optJSONArray("candidates") ?: return null
        if (candidates.length() == 0) return null

        val candidate = candidates.getJSONObject(0)
        val content = candidate.getJSONObject("content")
        val parts = content.getJSONArray("parts")
        if (parts.length() == 0) return null

        val rawText = parts.getJSONObject(0).getString("text")
        val parsedObj = JSONObject(rawText)

        val title = parsedObj.optString("title", "Expense")
        val amount = parsedObj.optDouble("amount", 0.0)
        val currency = parsedObj.optString("currency", "$")
        val category = parsedObj.optString("category", "GENERAL").uppercase()
        val merchant = parsedObj.optString("merchant", "")
        val payerNameHint = parsedObj.optString("payerNameHint", null)
        val participantsJson = parsedObj.optJSONArray("participantNames")
        val participants = mutableListOf<String>()
        if (participantsJson != null) {
            for (i in 0 until participantsJson.length()) {
                participants.add(participantsJson.getString(i))
            }
        }
        val suggestedSplit = parsedObj.optInt("suggestedSplitCount", if (participants.isNotEmpty()) participants.size + 1 else 1)
        val notes = parsedObj.optString("notes", "")
        val confidence = parsedObj.optDouble("confidence", 0.95).toFloat()

        return ParsedExpenseResult(
            title = title,
            amount = amount,
            currency = if (currency.length > 3) "$" else currency,
            category = normalizeCategory(category),
            merchant = merchant,
            dateMillis = System.currentTimeMillis(),
            payerNameHint = payerNameHint,
            participantNames = participants,
            suggestedSplitCount = suggestedSplit,
            rawMessage = sourceText,
            confidence = confidence,
            notes = if (notes.isBlank()) null else notes,
            parsedByAi = true
        )
    }

    private fun normalizeCategory(cat: String): String {
        return when {
            cat.contains("FOOD") || cat.contains("DINING") || cat.contains("RESTAURANT") || cat.contains("CAFE") || cat.contains("BAR") -> "FOOD"
            cat.contains("TRAVEL") || cat.contains("RIDE") || cat.contains("UBER") || cat.contains("TAXI") || cat.contains("FLIGHT") -> "TRAVEL"
            cat.contains("SHOPPING") || cat.contains("GROCERY") || cat.contains("MARKET") || cat.contains("STORE") -> "SHOPPING"
            cat.contains("ENTERTAINMENT") || cat.contains("MOVIE") || cat.contains("GAME") || cat.contains("CONCERT") -> "ENTERTAINMENT"
            cat.contains("UTILITIES") || cat.contains("BILL") || cat.contains("WIFI") || cat.contains("ELECTRICITY") || cat.contains("WATER") -> "UTILITIES"
            cat.contains("RENT") || cat.contains("HOTEL") || cat.contains("AIRBNB") || cat.contains("CABIN") || cat.contains("STAY") -> "RENT"
            else -> "GENERAL"
        }
    }
}
