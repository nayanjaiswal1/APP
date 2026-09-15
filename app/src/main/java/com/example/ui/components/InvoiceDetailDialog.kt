package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.local.entity.InvoiceEntity
import com.example.data.parser.InvoiceLineItem
import com.example.ui.theme.PositiveGreen
import com.example.ui.theme.PositiveGreenBg
import com.example.ui.theme.PurpleContainer
import com.example.ui.theme.PurplePrimary
import com.example.ui.theme.PurplePrimaryDark
import com.example.ui.theme.PurpleSurfaceVariant
import com.example.ui.theme.TextMuted
import org.json.JSONArray
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun InvoiceDetailDialog(
    invoice: InvoiceEntity,
    onDismiss: () -> Unit,
    onDelete: (Long) -> Unit
) {
    val dateFormat = SimpleDateFormat("MMM d, yyyy h:mm a", Locale.getDefault())
    val dateStr = dateFormat.format(Date(invoice.dateMillis))

    // Parse line items JSON
    val lineItems = mutableListOf<InvoiceLineItem>()
    try {
        val arr = JSONArray(invoice.lineItemsJson)
        for (i in 0 until arr.length()) {
            val obj = arr.getJSONObject(i)
            lineItems.add(
                InvoiceLineItem(
                    originalName = obj.optString("originalName", "Item ${i+1}"),
                    translatedName = obj.optString("translatedName", "Item ${i+1}"),
                    quantity = obj.optInt("quantity", 1),
                    unitPrice = obj.optDouble("unitPrice", 0.0),
                    totalPrice = obj.optDouble("totalPrice", 0.0)
                )
            )
        }
    } catch (_: Exception) {}

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.9f)
                .testTag("invoice_detail_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
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
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(PurpleContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.ReceiptLong,
                                contentDescription = null,
                                tint = PurplePrimaryDark,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = invoice.vendorName,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = PurplePrimaryDark
                            )
                            Text(
                                text = "Invoice #${invoice.invoiceNumber ?: "N/A"} • $dateStr",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextMuted
                            )
                        }
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = TextMuted)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Translation Badge & Category Banner
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(PurpleSurfaceVariant)
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Translate, contentDescription = null, tint = PurplePrimary, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "${invoice.originalLanguage} ➔ ${invoice.translatedLanguage}",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = PurplePrimaryDark
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = PositiveGreenBg
                    ) {
                        Text(
                            text = "Translated & Saved",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = PositiveGreen,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Summary Note
                    if (invoice.translatedSummary.isNotBlank()) {
                        item {
                            Text(
                                text = "AI SUMMARY",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = TextMuted
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = invoice.translatedSummary,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                        }
                    }

                    // Itemized Table
                    item {
                        Text(
                            text = "TRANSLATED LINE ITEMS (${lineItems.size})",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = TextMuted
                        )
                    }

                    items(lineItems) { item ->
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = item.translatedName,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    if (item.originalName != item.translatedName && item.originalName.isNotBlank()) {
                                        Text(
                                            text = item.originalName,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = TextMuted
                                        )
                                    }
                                    if (item.quantity > 1) {
                                        Text(
                                            text = "Qty: ${item.quantity} × ${invoice.currency}${String.format(Locale.US, "%.2f", item.unitPrice)}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = PurplePrimary
                                        )
                                    }
                                }

                                Text(
                                    text = "${invoice.currency}${String.format(Locale.US, "%.2f", item.totalPrice)}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = PurplePrimaryDark
                                )
                            }
                        }
                    }

                    // Totals Breakdown
                    item {
                        Spacer(modifier = Modifier.height(4.dp))
                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = PurpleContainer.copy(alpha = 0.4f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                if (invoice.subtotalAmount > 0) {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("Subtotal", style = MaterialTheme.typography.bodySmall, color = TextMuted)
                                        Text("${invoice.currency}${String.format(Locale.US, "%.2f", invoice.subtotalAmount)}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                                    }
                                }
                                if (invoice.taxAmount > 0) {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("Tax / VAT", style = MaterialTheme.typography.bodySmall, color = TextMuted)
                                        Text("${invoice.currency}${String.format(Locale.US, "%.2f", invoice.taxAmount)}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                                    }
                                }
                                if (invoice.tipAmount > 0) {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("Tip / Gratuity", style = MaterialTheme.typography.bodySmall, color = TextMuted)
                                        Text("${invoice.currency}${String.format(Locale.US, "%.2f", invoice.tipAmount)}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                                    }
                                }
                                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                    Text("Grand Total", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = PurplePrimaryDark)
                                    Text(
                                        "${invoice.currency}${String.format(Locale.US, "%.2f", invoice.totalAmount)}",
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = PurplePrimaryDark
                                    )
                                }
                            }
                        }
                    }

                    // Original OCR Text
                    if (invoice.rawOcrText.isNotBlank()) {
                        item {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "ORIGINAL RECEIPT / INVOICE OCR TEXT",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = TextMuted
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = invoice.rawOcrText,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontSize = 11.sp,
                                    color = TextMuted,
                                    modifier = Modifier.padding(10.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Bottom Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = { onDelete(invoice.id) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFB3261E))
                    ) {
                        Icon(imageVector = Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Delete")
                    }

                    Button(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1.5f),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PurplePrimary)
                    ) {
                        Text("Done", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
