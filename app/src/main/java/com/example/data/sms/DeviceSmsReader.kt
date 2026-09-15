package com.example.data.sms

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Telephony
import androidx.core.content.ContextCompat
import com.example.data.parser.MessageParserEngine
import com.example.data.parser.ParsedExpenseResult

data class DeviceSmsItem(
    val id: Long,
    val sender: String,
    val body: String,
    val dateMillis: Long,
    val parsedResult: ParsedExpenseResult,
    val isFinancial: Boolean = true
)

object DeviceSmsReader {

    fun hasSmsPermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_SMS
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun readDeviceSms(context: Context, limit: Int = 50): List<DeviceSmsItem> {
        if (!hasSmsPermission(context)) {
            return emptyList()
        }

        val smsList = mutableListOf<DeviceSmsItem>()
        val uri = Uri.parse("content://sms/inbox")
        val projection = arrayOf(
            Telephony.Sms._ID,
            Telephony.Sms.ADDRESS,
            Telephony.Sms.BODY,
            Telephony.Sms.DATE
        )

        try {
            val cursor = context.contentResolver.query(
                uri,
                projection,
                null,
                null,
                "${Telephony.Sms.DATE} DESC"
            )

            cursor?.use {
                val idCol = it.getColumnIndexOrThrow(Telephony.Sms._ID)
                val addressCol = it.getColumnIndexOrThrow(Telephony.Sms.ADDRESS)
                val bodyCol = it.getColumnIndexOrThrow(Telephony.Sms.BODY)
                val dateCol = it.getColumnIndexOrThrow(Telephony.Sms.DATE)

                var count = 0
                while (it.moveToNext() && count < limit) {
                    val id = it.getLong(idCol)
                    val address = it.getString(addressCol) ?: "Unknown Sender"
                    val body = it.getString(bodyCol) ?: ""
                    val date = it.getLong(dateCol)

                    // Parse the body with the MessageParserEngine
                    val parsed = MessageParserEngine.parseLocalNLP(body)
                    val isFinancial = isLikelyFinancialSms(address, body, parsed)

                    if (isFinancial) {
                        smsList.add(
                            DeviceSmsItem(
                                id = id,
                                sender = formatSender(address),
                                body = body,
                                dateMillis = date,
                                parsedResult = parsed,
                                isFinancial = true
                            )
                        )
                        count++
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return smsList
    }

    private fun isLikelyFinancialSms(sender: String, body: String, parsed: ParsedExpenseResult): Boolean {
        if (parsed.amount > 0.0 && parsed.title.isNotBlank()) return true
        val lower = body.lowercase()
        val financialKeywords = listOf(
            "debited", "spent", "paid", "withdrawn", "purchase", "txn", "charged",
            "inr", "usd", "$", "rs.", "eur", "gbp", "credited", "ac no", "card",
            "bank", "alert", "otp", "upi", "vpa", "pos"
        )
        return financialKeywords.any { lower.contains(it) }
    }

    private fun formatSender(sender: String): String {
        return sender.trim().removePrefix("+").replace("-", "").uppercase()
    }

    fun getSimulatedDeviceInbox(): List<DeviceSmsItem> {
        val now = System.currentTimeMillis()
        val samples = listOf(
            SampleSms(
                id = 1001L,
                sender = "CHASE-ALERT",
                body = "Chase Alert: Your card ending in 4102 was charged $48.50 at Trader Joe's on 09/02/2026. Available credit: $4,210.00.",
                offsetMillis = 15 * 60 * 1000L
            ),
            SampleSms(
                id = 1002L,
                sender = "BOFA-TXN",
                body = "Bank of America: Debit card purchase of $84.20 at Whole Foods Market approved. Ref #BOFA-891024.",
                offsetMillis = 90 * 60 * 1000L
            ),
            SampleSms(
                id = 1003L,
                sender = "APPLE-PAY",
                body = "Apple Pay: You paid $32.40 to Blue Bottle Coffee using Apple Card (ending in 8912).",
                offsetMillis = 3 * 3600 * 1000L
            ),
            SampleSms(
                id = 1004L,
                sender = "UBER-RIDE",
                body = "Uber: Receipt for your ride with driver Alex: $27.90 charged to Visa ...5541. Thanks for riding!",
                offsetMillis = 6 * 3600 * 1000L
            ),
            SampleSms(
                id = 1005L,
                sender = "HDFC-BANK",
                body = "Alert: Rs 1,450.00 debited from HDFC Bank A/c xx7821 on 02-SEP-26 to ZOMATO MEDIA PVT LTD. UPI Ref: 681920412.",
                offsetMillis = 12 * 3600 * 1000L
            ),
            SampleSms(
                id = 1006L,
                sender = "TARGET-STORE",
                body = "Target: Purchase of $65.80 at Target Store #1042 approved on your Mastercard.",
                offsetMillis = 24 * 3600 * 1000L
            ),
            SampleSms(
                id = 1007L,
                sender = "AMEX-ALERT",
                body = "American Express: Large charge of $142.00 at Shell Gas & Oil on Card ending in 1004.",
                offsetMillis = 36 * 3600 * 1000L
            )
        )

        return samples.map { s ->
            val parsed = MessageParserEngine.parseLocalNLP(s.body)
            DeviceSmsItem(
                id = s.id,
                sender = s.sender,
                body = s.body,
                dateMillis = now - s.offsetMillis,
                parsedResult = parsed,
                isFinancial = true
            )
        }
    }

    private data class SampleSms(
        val id: Long,
        val sender: String,
        val body: String,
        val offsetMillis: Long
    )
}
