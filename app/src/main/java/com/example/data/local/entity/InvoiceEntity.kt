package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "invoices")
data class InvoiceEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
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
    val rawOcrText: String = "",
    val translatedSummary: String = "",
    val lineItemsJson: String = "[]", // JSON array of line items
    val imageUri: String? = null,
    val linkedExpenseId: Long? = null,
    val category: String = "FOOD",
    val createdAt: Long = System.currentTimeMillis()
)
