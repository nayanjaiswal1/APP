package com.example.data.parser

import android.graphics.Bitmap
import android.util.Base64
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

data class InvoiceLineItem(
    val originalName: String,
    val translatedName: String,
    val quantity: Int = 1,
    val unitPrice: Double = 0.0,
    val totalPrice: Double = 0.0
)

data class ParsedInvoiceResult(
    val invoiceNumber: String? = null,
    val vendorName: String,
    val totalAmount: Double,
    val subtotalAmount: Double = 0.0,
    val taxAmount: Double = 0.0,
    val tipAmount: Double = 0.0,
    val currency: String = "$",
    val dateMillis: Long = System.currentTimeMillis(),
    val originalLanguage: String = "English",
    val translatedLanguage: String = "English",
    val category: String = "FOOD",
    val translatedSummary: String = "",
    val lineItems: List<InvoiceLineItem> = emptyList(),
    val rawOcrText: String = "",
    val confidence: Float = 0.95f,
    val parsedByAi: Boolean = false
)

object InvoiceParserAndTranslator {

    private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent"

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    suspend fun parseAndTranslate(
        rawText: String,
        bitmap: Bitmap? = null,
        targetLanguage: String = "English"
    ): ParsedInvoiceResult = withContext(Dispatchers.IO) {
        // 1. Attempt Gemini AI parsing (Text or Multimodal)
        val aiResult = parseWithGemini(rawText, bitmap, targetLanguage)
        if (aiResult != null && aiResult.totalAmount > 0.0) {
            return@withContext aiResult
        }

        // 2. Fallback to Local Multilingual NLP & Translation Engine
        return@withContext parseLocalInvoice(rawText, targetLanguage)
    }

    private suspend fun parseWithGemini(
        rawText: String,
        bitmap: Bitmap?,
        targetLanguage: String
    ): ParsedInvoiceResult? = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") return@withContext null

        try {
            val systemPrompt = """
                You are a world-class financial OCR invoice and receipt parser and multi-language translator.
                Analyze the invoice text or image. Detect the original language (e.g., Japanese, German, French, Spanish, Italian, Chinese, Korean, English, etc.).
                Translate vendor name, line items, and summary into $targetLanguage.
                Extract exact amounts (Subtotal, Tax/VAT, Tip, Total), Currency symbol, and itemized line items.
                
                Return ONLY valid JSON matching this exact structure:
                {
                   "invoiceNumber": "INV-12345",
                   "vendorName": "Vendor or Restaurant Name in English",
                   "originalLanguage": "Detected Language Name",
                   "translatedLanguage": "$targetLanguage",
                   "currency": "$ or € or ¥ or £",
                   "subtotalAmount": 0.00,
                   "taxAmount": 0.00,
                   "tipAmount": 0.00,
                   "totalAmount": 0.00,
                   "category": "FOOD, TRAVEL, SHOPPING, ENTERTAINMENT, UTILITIES, RENT, or GENERAL",
                   "translatedSummary": "One sentence summary of the invoice/receipt in $targetLanguage",
                   "lineItems": [
                      {
                         "originalName": "Item name in original language",
                         "translatedName": "Item name translated to $targetLanguage",
                         "quantity": 1,
                         "unitPrice": 0.00,
                         "totalPrice": 0.00
                      }
                   ],
                   "confidence": 0.98
                }
            """.trimIndent()

            val partsArray = JSONArray()

            if (bitmap != null) {
                val stream = ByteArrayOutputStream()
                bitmap.compress(Bitmap.CompressFormat.JPEG, 85, stream)
                val base64Data = Base64.encodeToString(stream.toByteArray(), Base64.NO_WRAP)

                partsArray.put(JSONObject().apply {
                    put("inlineData", JSONObject().apply {
                        put("mimeType", "image/jpeg")
                        put("data", base64Data)
                    })
                })
            }

            partsArray.put(JSONObject().apply {
                put("text", "$systemPrompt\n\nInput Invoice Text:\n\"$rawText\"")
            })

            val requestJson = JSONObject().apply {
                put("contents", JSONArray().apply {
                    put(JSONObject().apply {
                        put("parts", partsArray)
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
            if (!response.isSuccessful) return@withContext null

            val responseBody = response.body?.string() ?: return@withContext null
            val responseJson = JSONObject(responseBody)
            val candidates = responseJson.optJSONArray("candidates") ?: return@withContext null
            if (candidates.length() == 0) return@withContext null

            val candidate = candidates.getJSONObject(0)
            val content = candidate.getJSONObject("content")
            val parts = content.getJSONArray("parts")
            if (parts.length() == 0) return@withContext null

            val jsonText = parts.getJSONObject(0).getString("text")
            val parsedObj = JSONObject(jsonText)

            val invoiceNo = parsedObj.optString("invoiceNumber", null)
            val vendor = parsedObj.optString("vendorName", "Invoice Vendor")
            val origLang = parsedObj.optString("originalLanguage", "English")
            val transLang = parsedObj.optString("translatedLanguage", targetLanguage)
            val currency = parsedObj.optString("currency", "$")
            val subtotal = parsedObj.optDouble("subtotalAmount", 0.0)
            val tax = parsedObj.optDouble("taxAmount", 0.0)
            val tip = parsedObj.optDouble("tipAmount", 0.0)
            val total = parsedObj.optDouble("totalAmount", subtotal + tax + tip)
            val category = parsedObj.optString("category", "FOOD").uppercase()
            val summary = parsedObj.optString("translatedSummary", "")
            val confidence = parsedObj.optDouble("confidence", 0.95).toFloat()

            val lineItemsJson = parsedObj.optJSONArray("lineItems")
            val items = mutableListOf<InvoiceLineItem>()
            if (lineItemsJson != null) {
                for (i in 0 until lineItemsJson.length()) {
                    val itemObj = lineItemsJson.getJSONObject(i)
                    items.add(
                        InvoiceLineItem(
                            originalName = itemObj.optString("originalName", "Item ${i+1}"),
                            translatedName = itemObj.optString("translatedName", "Item ${i+1}"),
                            quantity = itemObj.optInt("quantity", 1),
                            unitPrice = itemObj.optDouble("unitPrice", 0.0),
                            totalPrice = itemObj.optDouble("totalPrice", 0.0)
                        )
                    )
                }
            }

            ParsedInvoiceResult(
                invoiceNumber = invoiceNo,
                vendorName = vendor,
                totalAmount = total,
                subtotalAmount = subtotal,
                taxAmount = tax,
                tipAmount = tip,
                currency = if (currency.length > 3) "$" else currency,
                dateMillis = System.currentTimeMillis(),
                originalLanguage = origLang,
                translatedLanguage = transLang,
                category = category,
                translatedSummary = summary,
                lineItems = items,
                rawOcrText = rawText,
                confidence = confidence,
                parsedByAi = true
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Local Offline Multilingual Invoice & Receipt Parsing Engine
     */
    fun parseLocalInvoice(rawText: String, targetLanguage: String = "English"): ParsedInvoiceResult {
        val lines = rawText.lines().map { it.trim() }.filter { it.isNotBlank() }
        val now = System.currentTimeMillis()

        // 1. Language Detection & Dictionary
        val detectedLanguage = detectLanguage(rawText)

        // 2. Vendor Name Detection
        var vendorName = "Merchant Invoice"
        for (line in lines.take(3)) {
            if (!line.contains("receipt", ignoreCase = true) && !line.contains("invoice", ignoreCase = true) &&
                !line.contains("tax", ignoreCase = true) && !line.contains("bill", ignoreCase = true) && line.length > 2) {
                vendorName = line.take(35)
                break
            }
        }

        // 3. Extract Totals & Amounts
        var total = 0.0
        var subtotal = 0.0
        var tax = 0.0
        var tip = 0.0
        var currency = "$"

        if (rawText.contains("€") || rawText.contains("EUR", ignoreCase = true)) currency = "€"
        else if (rawText.contains("¥") || rawText.contains("JPY", ignoreCase = true) || rawText.contains("円")) currency = "¥"
        else if (rawText.contains("£") || rawText.contains("GBP", ignoreCase = true)) currency = "£"
        else if (rawText.contains("₹") || rawText.contains("INR", ignoreCase = true)) currency = "₹"

        val amountRegex = Pattern.compile("""[0-9]+(?:,[0-9]{3})*(?:\.[0-9]{2})?""")

        val lineItems = mutableListOf<InvoiceLineItem>()

        for (line in lines) {
            val lower = line.lowercase()

            // Check for total line
            if (lower.contains("total") || lower.contains("gesamt") || lower.contains("合計") ||
                lower.contains("ttc") || lower.contains("grand total") || lower.contains("totale")) {
                val m = amountRegex.matcher(line)
                var lastVal = 0.0
                while (m.find()) {
                    val v = m.group().replace(",", "").toDoubleOrNull() ?: 0.0
                    if (v > 0) lastVal = v
                }
                if (lastVal > 0) total = lastVal
            } else if (lower.contains("subtotal") || lower.contains("zwischensumme") || lower.contains("小計") || lower.contains("sous-total") || lower.contains("sub total")) {
                val m = amountRegex.matcher(line)
                var lastVal = 0.0
                while (m.find()) {
                    val v = m.group().replace(",", "").toDoubleOrNull() ?: 0.0
                    if (v > 0) lastVal = v
                }
                if (lastVal > 0) subtotal = lastVal
            } else if (lower.contains("tax") || lower.contains("mwst") || lower.contains("tva") || lower.contains("iva") || lower.contains("消費税")) {
                val m = amountRegex.matcher(line)
                var lastVal = 0.0
                while (m.find()) {
                    val v = m.group().replace(",", "").toDoubleOrNull() ?: 0.0
                    if (v > 0) lastVal = v
                }
                if (lastVal > 0) tax = lastVal
            } else if (lower.contains("tip") || lower.contains("gratuity") || lower.contains("trinkgeld") || lower.contains("pourboire") || lower.contains("propina")) {
                val m = amountRegex.matcher(line)
                var lastVal = 0.0
                while (m.find()) {
                    val v = m.group().replace(",", "").toDoubleOrNull() ?: 0.0
                    if (v > 0) lastVal = v
                }
                if (lastVal > 0) tip = lastVal
            } else {
                // Potential line item
                val m = amountRegex.matcher(line)
                var foundPrice = 0.0
                while (m.find()) {
                    val v = m.group().replace(",", "").toDoubleOrNull() ?: 0.0
                    if (v > 0) foundPrice = v
                }

                if (foundPrice > 0.0 && line.length > 3) {
                    val rawItemName = line.replace(Regex("""[0-9]+(?:,[0-9]{3})*(?:\.[0-9]{2})?"""), "")
                        .replace(Regex("""[\$€¥£₹:\-x]"""), "")
                        .trim()

                    if (rawItemName.length >= 2 && !rawItemName.all { it.isDigit() || it.isWhitespace() }) {
                        val translated = translateTerm(rawItemName, detectedLanguage)
                        lineItems.add(
                            InvoiceLineItem(
                                originalName = rawItemName,
                                translatedName = translated,
                                quantity = 1,
                                unitPrice = foundPrice,
                                totalPrice = foundPrice
                            )
                        )
                    }
                }
            }
        }

        if (total == 0.0) {
            total = if (subtotal > 0.0) subtotal + tax + tip else lineItems.sumOf { it.totalPrice }
        }

        if (subtotal == 0.0 && total > 0.0) {
            subtotal = total - tax - tip
        }

        val translatedVendor = translateTerm(vendorName, detectedLanguage)
        val summary = "Invoice from $translatedVendor ($detectedLanguage) with ${lineItems.size} items."

        return ParsedInvoiceResult(
            invoiceNumber = "INV-" + (System.currentTimeMillis() % 100000),
            vendorName = translatedVendor,
            totalAmount = total,
            subtotalAmount = subtotal,
            taxAmount = tax,
            tipAmount = tip,
            currency = currency,
            dateMillis = now,
            originalLanguage = detectedLanguage,
            translatedLanguage = targetLanguage,
            category = inferInvoiceCategory(vendorName + " " + rawText),
            translatedSummary = summary,
            lineItems = lineItems,
            rawOcrText = rawText,
            confidence = 0.90f,
            parsedByAi = false
        )
    }

    private fun detectLanguage(text: String): String {
        val l = text.lowercase()
        return when {
            text.any { it in '\u3040'..'\u30ff' || it in '\u4e00'..'\u9faf' } && (text.contains("領収書") || text.contains("円") || text.contains("小計") || text.contains("合計")) -> "Japanese"
            text.any { it in '\uac00'..'\ud7af' } -> "Korean"
            text.any { it in '\u4e00'..'\u9fff' } -> "Chinese"
            l.contains("rechnung") || l.contains("mwst") || l.contains("gesamtbetrag") || l.contains("zwischensumme") -> "German"
            l.contains("facture") || l.contains("tva") || l.contains("sous-total") || l.contains("total ttc") -> "French"
            l.contains("factura") || l.contains("recibo") || l.contains("iva") || l.contains("propina") -> "Spanish"
            l.contains("fattura") || l.contains("scontrino") || l.contains("imponibile") -> "Italian"
            else -> "English"
        }
    }

    private fun translateTerm(text: String, language: String): String {
        var clean = text.trim()
        val dict = when (language) {
            "Japanese" -> JAPANESE_DICT
            "German" -> GERMAN_DICT
            "French" -> FRENCH_DICT
            "Spanish" -> SPANISH_DICT
            "Italian" -> ITALIAN_DICT
            else -> emptyMap()
        }

        for ((key, value) in dict) {
            if (clean.contains(key, ignoreCase = true)) {
                clean = clean.replace(key, value, ignoreCase = true)
            }
        }
        return clean.ifBlank { text }
    }

    private fun inferInvoiceCategory(text: String): String {
        val l = text.lowercase()
        return when {
            l.contains("restaurant") || l.contains("cafe") || l.contains("diner") || l.contains("bar") ||
            l.contains("izakaya") || l.contains("food") || l.contains("pizza") || l.contains("sushi") ||
            l.contains("bier") || l.contains("wine") || l.contains("ramen") || l.contains("bistro") -> "FOOD"

            l.contains("hotel") || l.contains("flight") || l.contains("airline") || l.contains("taxi") ||
            l.contains("uber") || l.contains("train") || l.contains("bahn") || l.contains("sncf") -> "TRAVEL"

            l.contains("supermarket") || l.contains("market") || l.contains("store") || l.contains("shop") ||
            l.contains("zara") || l.contains("ikea") || l.contains("apple") || l.contains("amazon") -> "SHOPPING"

            l.contains("electric") || l.contains("telecom") || l.contains("internet") || l.contains("strom") -> "UTILITIES"

            else -> "GENERAL"
        }
    }

    private val JAPANESE_DICT = mapOf(
        "鳥貴族" to "Torikizoku Izakaya",
        "領収書" to "Receipt",
        "焼き鳥盛り合わせ" to "Yakitori Platter",
        "焼き鳥" to "Grilled Chicken Skewers",
        "生ビール" to "Draft Beer",
        "特製ラーメン" to "Special Ramen Bowl",
        "ラーメン" to "Ramen",
        "枝豆" to "Edamame Beans",
        "餃子" to "Pan-fried Gyoza",
        "刺身盛り合わせ" to "Chef's Sashimi Assortment",
        "刺身" to "Fresh Sashimi",
        "小計" to "Subtotal",
        "消費税" to "Consumption Tax (10%)",
        "合計" to "Grand Total"
    )

    private val GERMAN_DICT = mapOf(
        "Brauhaus" to "Brewery Restaurant",
        "Rechnung" to "Invoice",
        "Schnitzel mit Pommes" to "Pork Schnitzel with Fries",
        "Currywurst" to "Currywurst with Special Sauce",
        "Dunkles Bier" to "Dark Draft Beer",
        "Weißbier" to "Wheat Beer",
        "Apfelstrudel" to "Warm Apple Strudel Dessert",
        "Zwischensumme" to "Subtotal",
        "MwSt" to "VAT Tax (19%)",
        "Gesamtbetrag" to "Total Amount",
        "Trinkgeld" to "Tip / Gratuity"
    )

    private val FRENCH_DICT = mapOf(
        "Bistro Parisien" to "Parisian Bistro Cafe",
        "Facture" to "Invoice",
        "Croissant au Beurre" to "Butter Croissant",
        "Café au Lait" to "Coffee with Milk",
        "Steak Frites" to "Grilled Steak with Fries",
        "Salade Niçoise" to "Nicoise Fresh Salad",
        "Bouteille de Vin" to "Bottle of Red Wine",
        "Crème Brûlée" to "Vanilla Creme Brulee",
        "Sous-total" to "Subtotal",
        "TVA" to "VAT (20%)",
        "Total TTC" to "Total (All Taxes Included)"
    )

    private val SPANISH_DICT = mapOf(
        "Taberna Los Amigos" to "Tapas Bar Los Amigos",
        "Factura" to "Invoice",
        "Recibo" to "Receipt",
        "Jamon Iberico" to "Iberian Cured Ham",
        "Patatas Bravas" to "Spicy Bravas Potatoes",
        "Paella de Mariscos" to "Seafood Paella",
        "Cerveza Estrella" to "Estrella Cold Beer",
        "Sangria Jarra" to "Pitcher of Sangria",
        "Churros con Chocolate" to "Crispy Churros with Chocolate",
        "Subtotal" to "Subtotal",
        "IVA" to "VAT (21%)",
        "Total EUR" to "Total Amount"
    )

    private val ITALIAN_DICT = mapOf(
        "Trattoria Da Mario" to "Trattoria Da Mario",
        "Fattura" to "Invoice",
        "Pizza Margherita" to "Classic Margherita Pizza",
        "Pasta Carbonara" to "Authentic Roman Carbonara",
        "Tiramisu Artigianale" to "Handmade Tiramisu",
        "Espresso Doppio" to "Double Shot Espresso",
        "Vino Chianti" to "Chianti Red Wine",
        "Imponibile" to "Subtotal",
        "IVA" to "VAT (22%)",
        "Totale" to "Grand Total"
    )

    val SAMPLE_INVOICES = listOf(
        SampleInvoice(
            title = "Tokyo Izakaya Torikizoku (Japanese)",
            language = "Japanese",
            vendor = "鳥貴族 渋谷店 (Torikizoku)",
            rawText = """
                鳥貴族 渋谷神南店 (Torikizoku Shibuya)
                領収書 No. JP-99214
                日時: 2026-09-02 20:30
                -----------------------------------
                焼き鳥盛り合わせ (Yakitori Set x2)   ¥1,800
                生ビール (Draft Beer x4)              ¥2,400
                特製ラーメン (Special Ramen x2)      ¥2,200
                枝豆 & 餃子 (Edamame & Gyoza)        ¥1,500
                刺身盛り合わせ (Sashimi Platter)      ¥2,400
                -----------------------------------
                小計 (Subtotal)                       ¥10,300
                消費税 10% (Tax)                       ¥1,030
                合計 (Total JPY)                      ¥11,330 ($68.50)
            """.trimIndent()
        ),
        SampleInvoice(
            title = "Munich Hofbräuhaus (German)",
            language = "German",
            vendor = "Münchner Brauhaus am Markt",
            rawText = """
                Münchner Brauhaus am Markt GmbH
                Rechnung Nr. DE-44021
                Datum: 02.09.2026 19:15
                -----------------------------------
                2x Schnitzel mit Pommes              €38.00
                4x Weißbier 0.5L                     €22.00
                1x Currywurst Spezial                €14.50
                2x Apfelstrudel                      €13.50
                -----------------------------------
                Zwischensumme                        €88.00
                MwSt 19% (Tax)                       €16.72
                Trinkgeld (Tip)                      €10.00
                Gesamtbetrag (Total)                €114.72
            """.trimIndent()
        ),
        SampleInvoice(
            title = "Parisian Bistro Montmartre (French)",
            language = "French",
            vendor = "Bistro Parisien Montmartre",
            rawText = """
                Bistro Parisien Montmartre
                Facture N° FR-77812
                Date: 01/09/2026
                -----------------------------------
                2x Steak Frites                      €48.00
                1x Salade Niçoise                    €16.50
                1x Bouteille de Vin Rouge            €28.00
                3x Café au Lait                      €12.00
                2x Crème Brûlée                      €15.50
                -----------------------------------
                Sous-total                          €120.00
                TVA 20%                              €24.00
                Total TTC (Total EUR)               €144.00
            """.trimIndent()
        ),
        SampleInvoice(
            title = "Barcelona Tapas & Market (Spanish)",
            language = "Spanish",
            vendor = "Taberna Los Amigos Barcelona",
            rawText = """
                Taberna Los Amigos Barcelona
                Factura Simplificada ES-3391
                Fecha: 02-09-2026
                -----------------------------------
                1x Jamon Iberico Plato               €24.00
                2x Patatas Bravas                    €14.00
                1x Paella de Mariscos                €36.00
                1x Sangria Jarra                     €18.00
                2x Churros con Chocolate             €10.00
                -----------------------------------
                Subtotal                            €102.00
                IVA 21%                              €21.42
                Total EUR                           €123.42
            """.trimIndent()
        ),
        SampleInvoice(
            title = "Cloud Infrastructure & Software (English)",
            language = "English",
            vendor = "Apex Cloud Services LLC",
            rawText = """
                Apex Cloud Services LLC
                INVOICE #APX-90412
                Date: September 2, 2026
                -----------------------------------
                Compute Instance Dedicated Tier      $120.00
                Postgres Managed High Availability    $65.00
                Global CDN Bandwidth 5TB              $45.00
                SSL Enterprise Domain Pack            $20.00
                -----------------------------------
                Subtotal                             $250.00
                Sales Tax 8.5%                        $21.25
                Total USD                            $271.25
            """.trimIndent()
        )
    )
}

data class SampleInvoice(
    val title: String,
    val language: String,
    val vendor: String,
    val rawText: String
)
