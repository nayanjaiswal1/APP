package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "statements")
data class StatementEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val accountId: Long,
    val fileName: String,
    val rawContent: String,
    val parsedTransactionsCount: Int = 0,
    val totalExpensesAmount: Double = 0.0,
    val statementPeriod: String? = null,
    val uploadedAt: Long = System.currentTimeMillis()
)
