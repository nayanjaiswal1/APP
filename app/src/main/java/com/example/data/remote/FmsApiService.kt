package com.example.data.remote

import com.example.data.remote.dto.*
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Retrofit client for the Finance Management System (FMS) REST API
 * specified by OpenAPI 3.0.0
 */
interface FmsApiService {

    // ==========================================
    // Authentication
    // ==========================================

    @POST("api/auth/register")
    suspend fun register(@Body dto: RegisterDto): Response<Unit>

    @POST("api/auth/login")
    suspend fun login(@Body dto: LoginDto): Response<LoginResponse>

    @POST("api/auth/demo-login")
    suspend fun demoLogin(): Response<LoginResponse>

    @GET("api/auth/me")
    suspend fun getCurrentUser(): Response<UserResponseDto>

    @POST("api/auth/logout")
    suspend fun logout(): Response<Unit>

    // ==========================================
    // Accounts
    // ==========================================

    @GET("api/accounts")
    suspend fun getAccounts(
        @Query("searchTerm") searchTerm: String? = null,
        @Query("accountType") accountType: String? = null
    ): Response<List<AccountResponseDto>>

    @POST("api/accounts")
    suspend fun createAccount(@Body dto: CreateAccountDto): Response<AccountResponseDto>

    @GET("api/accounts/{id}")
    suspend fun getAccountById(@Path("id") id: String): Response<AccountResponseDto>

    @DELETE("api/accounts/{id}")
    suspend fun deleteAccount(@Path("id") id: String): Response<Unit>

    // ==========================================
    // Transactions
    // ==========================================

    @GET("api/transactions")
    suspend fun getTransactions(): Response<List<TransactionResponseDto>>

    @POST("api/transactions")
    suspend fun createTransaction(@Body dto: CreateTransactionDto): Response<TransactionResponseDto>

    @GET("api/transactions/{id}")
    suspend fun getTransactionById(@Path("id") id: String): Response<TransactionResponseDto>

    @DELETE("api/transactions/{id}")
    suspend fun deleteTransaction(@Path("id") id: String): Response<Unit>

    // ==========================================
    // Groups & Collaborative Ledgers
    // ==========================================

    @GET("api/groups")
    suspend fun getGroups(
        @Query("search") search: String = "",
        @Query("limit") limit: Int = 50,
        @Query("offset") offset: Int = 0
    ): Response<List<GroupResponseDto>>

    @POST("api/groups")
    suspend fun createGroup(@Body dto: CreateGroupDto): Response<GroupResponseDto>

    @GET("api/groups/{id}")
    suspend fun getGroupById(@Path("id") id: String): Response<GroupResponseDto>

    @GET("api/groups/{id}/balances")
    suspend fun getGroupBalances(@Path("id") id: String): Response<List<GroupBalanceDto>>

    @POST("api/groups/{id}/transactions")
    suspend fun createGroupTransaction(
        @Path("id") groupId: String,
        @Body dto: CreateGroupTransactionDto
    ): Response<Unit>

    @POST("api/groups/{id}/settle")
    suspend fun settleGroupDebt(
        @Path("id") groupId: String,
        @Body dto: SettleUpDto
    ): Response<Unit>

    // ==========================================
    // Budgets
    // ==========================================

    @GET("api/budgets")
    suspend fun getBudgets(
        @Query("month") month: Int? = null,
        @Query("year") year: Int? = null
    ): Response<List<BudgetResponseDto>>

    @POST("api/budgets")
    suspend fun createBudget(@Body dto: CreateBudgetDto): Response<BudgetResponseDto>

    @GET("api/budgets/summary")
    suspend fun getBudgetSummary(): Response<BudgetSummaryDto>

    @GET("api/budgets/alerts")
    suspend fun getBudgetAlerts(): Response<List<BudgetAlertDto>>

    @DELETE("api/budgets/{id}")
    suspend fun deleteBudget(@Path("id") id: String): Response<Unit>

    // ==========================================
    // Lend & Borrow
    // ==========================================

    @GET("api/lend-borrow")
    suspend fun getLendBorrow(
        @Query("status") status: String = "active"
    ): Response<List<LendBorrowResponseDto>>

    @POST("api/lend-borrow")
    suspend fun createLendBorrow(@Body dto: CreateLendBorrowDto): Response<LendBorrowResponseDto>

    @GET("api/lend-borrow/summary")
    suspend fun getLendBorrowSummary(): Response<LendBorrowSummaryDto>

    @POST("api/lend-borrow/{id}/payment")
    suspend fun recordLendBorrowPayment(
        @Path("id") id: String,
        @Body dto: RecordPaymentDto
    ): Response<Unit>

    @POST("api/lend-borrow/{id}/settle")
    suspend fun settleLendBorrow(@Path("id") id: String): Response<Unit>

    @DELETE("api/lend-borrow/{id}")
    suspend fun deleteLendBorrow(@Path("id") id: String): Response<Unit>

    // ==========================================
    // Reminders & Recurring Bills
    // ==========================================

    @GET("api/reminders")
    suspend fun getReminders(): Response<List<ReminderResponseDto>>

    @POST("api/reminders")
    suspend fun createReminder(@Body dto: CreateReminderDto): Response<ReminderResponseDto>

    @POST("api/reminders/{id}/complete")
    suspend fun completeReminder(@Path("id") id: String): Response<Unit>

    @DELETE("api/reminders/{id}")
    suspend fun deleteReminder(@Path("id") id: String): Response<Unit>

    // ==========================================
    // Investments & Portfolio
    // ==========================================

    @GET("api/investments")
    suspend fun getInvestments(): Response<List<InvestmentResponseDto>>

    @POST("api/investments")
    suspend fun createInvestment(@Body dto: CreateInvestmentDto): Response<InvestmentResponseDto>

    @GET("api/investments/portfolio")
    suspend fun getPortfolioSummary(): Response<PortfolioSummaryDto>

    @DELETE("api/investments/{id}")
    suspend fun deleteInvestment(@Path("id") id: String): Response<Unit>

    // ==========================================
    // AI Services & Chat
    // ==========================================

    @POST("api/ai/categorize")
    suspend fun autoCategorize(@Body dto: AutoCategorizeDto): Response<CategorySuggestionDto>

    @POST("api/ai/query")
    suspend fun queryFinancialAi(@Body dto: NaturalLanguageQueryDto): Response<AiQueryResponseDto>

    @POST("api/chat/message")
    suspend fun sendChatMessage(@Body dto: SendMessageDto): Response<ChatMessageResponseDto>

    // ==========================================
    // System Health
    // ==========================================

    @GET("health")
    suspend fun checkHealth(): Response<HealthCheckDto>

    @GET("api/health")
    suspend fun checkApiHealth(): Response<HealthCheckDto>
}
