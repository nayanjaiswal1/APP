package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "parsed_messages")
data class ParsedMessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val rawMessage: String,
    val parsedTitle: String,
    val parsedAmount: Double,
    val parsedCurrency: String = "$",
    val parsedCategory: String = "FOOD",
    val parsedMerchant: String = "",
    val parsedDateMillis: Long = System.currentTimeMillis(),
    val suggestedSplitCount: Int = 2,
    val status: String = "NEW", // NEW, CONVERTED, DISMISSED
    val createdAt: Long = System.currentTimeMillis()
)
