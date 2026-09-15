package com.example.data.remote.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

// ==========================================
// Authentication DTOs
// ==========================================

@JsonClass(generateAdapter = true)
data class RegisterDto(
    @Json(name = "email") val email: String,
    @Json(name = "password") val password: String,
    @Json(name = "name") val name: String? = null,
    @Json(name = "accept_terms") val acceptTerms: Boolean? = true,
    @Json(name = "firstName") val firstName: String? = null,
    @Json(name = "lastName") val lastName: String? = null
)

@JsonClass(generateAdapter = true)
data class LoginDto(
    @Json(name = "email") val email: String,
    @Json(name = "password") val password: String
)

@JsonClass(generateAdapter = true)
data class LoginResponse(
    @Json(name = "accessToken") val accessToken: String? = null,
    @Json(name = "token") val token: String? = null,
    @Json(name = "user") val user: UserResponseDto? = null,
    @Json(name = "message") val message: String? = null
) {
    val effectiveToken: String?
        get() = accessToken ?: token
}

@JsonClass(generateAdapter = true)
data class UserResponseDto(
    @Json(name = "id") val id: String? = null,
    @Json(name = "email") val email: String? = null,
    @Json(name = "firstName") val firstName: String? = null,
    @Json(name = "lastName") val lastName: String? = null,
    @Json(name = "role") val role: String? = null
) {
    val displayName: String
        get() {
            val full = listOfNotNull(firstName, lastName).joinToString(" ").trim()
            return if (full.isNotEmpty()) full else email ?: "User"
        }
}

// ==========================================
// Accounts DTOs
// ==========================================

@JsonClass(generateAdapter = true)
data class CreateAccountDto(
    @Json(name = "name") val name: String,
    @Json(name = "type") val type: String, // bank, wallet, cash, card, investment, loan, other
    @Json(name = "balance") val balance: Double,
    @Json(name = "currency") val currency: String? = "USD",
    @Json(name = "institution") val institution: String? = null,
    @Json(name = "accountNumber") val accountNumber: String? = null,
    @Json(name = "description") val description: String? = null,
    @Json(name = "statementPassword") val statementPassword: String? = null
)

@JsonClass(generateAdapter = true)
data class AccountResponseDto(
    @Json(name = "id") val id: String,
    @Json(name = "name") val name: String,
    @Json(name = "type") val type: String,
    @Json(name = "balance") val balance: Double = 0.0,
    @Json(name = "currency") val currency: String? = "USD",
    @Json(name = "institution") val institution: String? = null,
    @Json(name = "accountNumber") val accountNumber: String? = null,
    @Json(name = "description") val description: String? = null
)

// ==========================================
// Transactions DTOs
// ==========================================

@JsonClass(generateAdapter = true)
data class CreateTransactionDto(
    @Json(name = "type") val type: String, // income, expense, transfer
    @Json(name = "amount") val amount: Double,
    @Json(name = "description") val description: String,
    @Json(name = "date") val date: String,
    @Json(name = "accountId") val accountId: String,
    @Json(name = "categoryId") val categoryId: String? = null,
    @Json(name = "notes") val notes: String? = null,
    @Json(name = "location") val location: String? = null,
    @Json(name = "status") val status: String? = "confirmed",
    @Json(name = "scope") val scope: String? = "personal",
    @Json(name = "paymentMode") val paymentMode: String? = "cash",
    @Json(name = "sourceType") val sourceType: String? = "manual"
)

@JsonClass(generateAdapter = true)
data class TransactionResponseDto(
    @Json(name = "id") val id: String,
    @Json(name = "type") val type: String,
    @Json(name = "amount") val amount: Double,
    @Json(name = "description") val description: String,
    @Json(name = "date") val date: String,
    @Json(name = "accountId") val accountId: String? = null,
    @Json(name = "categoryId") val categoryId: String? = null,
    @Json(name = "notes") val notes: String? = null,
    @Json(name = "status") val status: String? = null,
    @Json(name = "scope") val scope: String? = null,
    @Json(name = "paymentMode") val paymentMode: String? = null
)

// ==========================================
// Groups DTOs
// ==========================================

@JsonClass(generateAdapter = true)
data class CreateGroupDto(
    @Json(name = "name") val name: String,
    @Json(name = "description") val description: String? = null,
    @Json(name = "icon") val icon: String? = null,
    @Json(name = "color") val color: String? = null,
    @Json(name = "currency") val currency: String? = "USD"
)

@JsonClass(generateAdapter = true)
data class GroupResponseDto(
    @Json(name = "id") val id: String,
    @Json(name = "name") val name: String,
    @Json(name = "description") val description: String? = null,
    @Json(name = "icon") val icon: String? = null,
    @Json(name = "color") val color: String? = null,
    @Json(name = "currency") val currency: String? = "USD"
)

@JsonClass(generateAdapter = true)
data class CreateGroupTransactionDto(
    @Json(name = "description") val description: String,
    @Json(name = "amount") val amount: Double,
    @Json(name = "date") val date: String,
    @Json(name = "paidBy") val paidBy: String,
    @Json(name = "splitType") val splitType: String = "equal",
    @Json(name = "splits") val splits: Map<String, Double> = emptyMap(),
    @Json(name = "notes") val notes: String? = null
)

@JsonClass(generateAdapter = true)
data class GroupBalanceDto(
    @Json(name = "userId") val userId: String? = null,
    @Json(name = "userName") val userName: String? = null,
    @Json(name = "balance") val balance: Double = 0.0
)

@JsonClass(generateAdapter = true)
data class SettleUpDto(
    @Json(name = "fromUserId") val fromUserId: String,
    @Json(name = "toUserId") val toUserId: String,
    @Json(name = "amount") val amount: Double,
    @Json(name = "date") val date: String,
    @Json(name = "notes") val notes: String? = null
)

// ==========================================
// Lend & Borrow DTOs
// ==========================================

@JsonClass(generateAdapter = true)
data class CreateLendBorrowDto(
    @Json(name = "type") val type: String, // lend, borrow
    @Json(name = "personName") val personName: String,
    @Json(name = "amount") val amount: Double,
    @Json(name = "date") val date: String,
    @Json(name = "dueDate") val dueDate: String? = null,
    @Json(name = "description") val description: String? = null,
    @Json(name = "notes") val notes: String? = null,
    @Json(name = "currency") val currency: String? = "USD"
)

@JsonClass(generateAdapter = true)
data class LendBorrowResponseDto(
    @Json(name = "id") val id: String,
    @Json(name = "type") val type: String,
    @Json(name = "personName") val personName: String,
    @Json(name = "amount") val amount: Double,
    @Json(name = "date") val date: String,
    @Json(name = "dueDate") val dueDate: String? = null,
    @Json(name = "description") val description: String? = null,
    @Json(name = "notes") val notes: String? = null,
    @Json(name = "currency") val currency: String? = "USD",
    @Json(name = "isSettled") val isSettled: Boolean? = false,
    @Json(name = "paidAmount") val paidAmount: Double? = 0.0
)

@JsonClass(generateAdapter = true)
data class LendBorrowSummaryDto(
    @Json(name = "totalLent") val totalLent: Double = 0.0,
    @Json(name = "totalBorrowed") val totalBorrowed: Double = 0.0,
    @Json(name = "netBalance") val netBalance: Double = 0.0,
    @Json(name = "activeCount") val activeCount: Int = 0
)

@JsonClass(generateAdapter = true)
data class RecordPaymentDto(
    @Json(name = "amount") val amount: Double,
    @Json(name = "date") val date: String? = null,
    @Json(name = "note") val note: String? = null
)

// ==========================================
// Budgets DTOs
// ==========================================

@JsonClass(generateAdapter = true)
data class CreateBudgetDto(
    @Json(name = "category") val category: String,
    @Json(name = "amount") val amount: Double,
    @Json(name = "month") val month: Int? = null,
    @Json(name = "year") val year: Int? = null,
    @Json(name = "alertThresholdPercentage") val alertThresholdPercentage: Double? = 80.0
)

@JsonClass(generateAdapter = true)
data class BudgetResponseDto(
    @Json(name = "id") val id: String,
    @Json(name = "category") val category: String,
    @Json(name = "amount") val amount: Double,
    @Json(name = "spent") val spent: Double = 0.0,
    @Json(name = "remaining") val remaining: Double? = null,
    @Json(name = "month") val month: Int? = null,
    @Json(name = "year") val year: Int? = null,
    @Json(name = "alertThresholdPercentage") val alertThresholdPercentage: Double? = 80.0,
    @Json(name = "isOverBudget") val isOverBudget: Boolean? = false
) {
    val effectiveRemaining: Double
        get() = remaining ?: (amount - spent).coerceAtLeast(0.0)

    val spentPercentage: Float
        get() = if (amount > 0) ((spent / amount) * 100.0).coerceAtLeast(0.0).toFloat() else 0f
}

@JsonClass(generateAdapter = true)
data class BudgetSummaryDto(
    @Json(name = "totalBudget") val totalBudget: Double = 0.0,
    @Json(name = "totalSpent") val totalSpent: Double = 0.0,
    @Json(name = "totalRemaining") val totalRemaining: Double = 0.0,
    @Json(name = "spentPercentage") val spentPercentage: Double = 0.0
)

@JsonClass(generateAdapter = true)
data class BudgetAlertDto(
    @Json(name = "id") val id: String? = null,
    @Json(name = "category") val category: String,
    @Json(name = "message") val message: String,
    @Json(name = "thresholdPercentage") val thresholdPercentage: Double = 80.0,
    @Json(name = "currentPercentage") val currentPercentage: Double = 0.0
)

// ==========================================
// Reminders & Bills DTOs
// ==========================================

@JsonClass(generateAdapter = true)
data class CreateReminderDto(
    @Json(name = "title") val title: String,
    @Json(name = "amount") val amount: Double,
    @Json(name = "dueDate") val dueDate: String,
    @Json(name = "frequency") val frequency: String? = "once", // once, weekly, monthly, yearly
    @Json(name = "category") val category: String? = "Bills & Utilities",
    @Json(name = "notes") val notes: String? = null
)

@JsonClass(generateAdapter = true)
data class ReminderResponseDto(
    @Json(name = "id") val id: String,
    @Json(name = "title") val title: String,
    @Json(name = "amount") val amount: Double,
    @Json(name = "dueDate") val dueDate: String,
    @Json(name = "frequency") val frequency: String? = "monthly",
    @Json(name = "category") val category: String? = "Bills",
    @Json(name = "notes") val notes: String? = null,
    @Json(name = "isCompleted") val isCompleted: Boolean? = false
)

// ==========================================
// Investments & Portfolio DTOs
// ==========================================

@JsonClass(generateAdapter = true)
data class CreateInvestmentDto(
    @Json(name = "name") val name: String,
    @Json(name = "type") val type: String, // stocks, mutual_fund, crypto, gold, fixed_deposit, real_estate
    @Json(name = "amount") val amount: Double,
    @Json(name = "units") val units: Double? = 1.0,
    @Json(name = "buyPrice") val buyPrice: Double? = null,
    @Json(name = "currentPrice") val currentPrice: Double? = null,
    @Json(name = "institution") val institution: String? = null,
    @Json(name = "date") val date: String? = null,
    @Json(name = "notes") val notes: String? = null
)

@JsonClass(generateAdapter = true)
data class InvestmentResponseDto(
    @Json(name = "id") val id: String,
    @Json(name = "name") val name: String,
    @Json(name = "type") val type: String,
    @Json(name = "amount") val amount: Double,
    @Json(name = "units") val units: Double? = 1.0,
    @Json(name = "buyPrice") val buyPrice: Double? = null,
    @Json(name = "currentPrice") val currentPrice: Double? = null,
    @Json(name = "institution") val institution: String? = null,
    @Json(name = "date") val date: String? = null,
    @Json(name = "notes") val notes: String? = null,
    @Json(name = "profitLoss") val profitLoss: Double? = 0.0,
    @Json(name = "profitLossPercentage") val profitLossPercentage: Double? = 0.0
) {
    val effectiveCurrentValue: Double
        get() = if (currentPrice != null && units != null && units > 0) {
            currentPrice * units
        } else {
            amount + (profitLoss ?: 0.0)
        }
}

@JsonClass(generateAdapter = true)
data class PortfolioSummaryDto(
    @Json(name = "totalInvested") val totalInvested: Double = 0.0,
    @Json(name = "currentValue") val currentValue: Double = 0.0,
    @Json(name = "totalProfitLoss") val totalProfitLoss: Double = 0.0,
    @Json(name = "profitLossPercentage") val profitLossPercentage: Double = 0.0
)

// ==========================================
// AI & Chat DTOs
// ==========================================

@JsonClass(generateAdapter = true)
data class AutoCategorizeDto(
    @Json(name = "description") val description: String,
    @Json(name = "amount") val amount: Double,
    @Json(name = "merchantName") val merchantName: String? = null
)

@JsonClass(generateAdapter = true)
data class CategorySuggestionDto(
    @Json(name = "category") val category: String? = null,
    @Json(name = "confidence") val confidence: Double? = null,
    @Json(name = "suggestedCategory") val suggestedCategory: String? = null
)

@JsonClass(generateAdapter = true)
data class NaturalLanguageQueryDto(
    @Json(name = "query") val query: String
)

@JsonClass(generateAdapter = true)
data class AiQueryResponseDto(
    @Json(name = "answer") val answer: String? = null,
    @Json(name = "response") val response: String? = null,
    @Json(name = "suggestions") val suggestions: List<String>? = null
)

@JsonClass(generateAdapter = true)
data class SendMessageDto(
    @Json(name = "message") val message: String,
    @Json(name = "conversationId") val conversationId: String? = null
)

@JsonClass(generateAdapter = true)
data class ChatMessageResponseDto(
    @Json(name = "response") val response: String? = null,
    @Json(name = "conversationId") val conversationId: String? = null
)

// ==========================================
// System & Health DTOs
// ==========================================

@JsonClass(generateAdapter = true)
data class HealthCheckDto(
    @Json(name = "status") val status: String? = null,
    @Json(name = "data") val data: String? = null,
    @Json(name = "message") val message: String? = null,
    @Json(name = "timestamp") val timestamp: String? = null,
    @Json(name = "version") val version: String? = null
) {
    val displayStatus: String
        get() = status ?: data ?: message ?: "OK"
}
