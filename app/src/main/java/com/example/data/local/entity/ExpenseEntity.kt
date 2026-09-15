package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "expenses")
data class ExpenseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val groupId: Long? = null, // null for direct 1-on-1 expense
    val title: String,
    val amount: Double,
    val currency: String = "$",
    val category: String = "FOOD", // FOOD, TRAVEL, SHOPPING, ENTERTAINMENT, UTILITIES, RENT, GENERAL
    val payerId: Long,
    val splitType: String = "EQUAL", // EQUAL, EXACT, PERCENTAGE, SHARES
    val splitDetailsJson: String = "", // JSON string of Map<Long, Double> or List<SplitItem>
    val dateMillis: Long = System.currentTimeMillis(),
    val notes: String? = null,
    val isSettlement: Boolean = false,
    val settlementFromId: Long? = null,
    val settlementToId: Long? = null,
    val sourceMessage: String? = null,
    val accountId: Long? = null,
    val invoiceId: Long? = null
)
