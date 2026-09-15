package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "accounts")
data class AccountEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val type: String = "CREDIT_CARD", // CREDIT_CARD, CHECKING, SAVINGS, WALLET, CASH
    val institution: String = "Bank",
    val accountNumberLast4: String = "1234",
    val balance: Double = 0.0,
    val currency: String = "$",
    val colorHex: String = "#6750A4",
    val iconName: String = "credit_card",
    val createdAt: Long = System.currentTimeMillis()
)
