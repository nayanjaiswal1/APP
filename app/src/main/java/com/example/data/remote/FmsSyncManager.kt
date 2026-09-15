package com.example.data.remote

import com.example.data.local.AppDatabase
import com.example.data.local.entity.AccountEntity
import com.example.data.local.entity.ExpenseEntity
import com.example.data.local.entity.GroupEntity
import com.example.data.remote.dto.CreateAccountDto
import com.example.data.remote.dto.CreateTransactionDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class FmsSyncResult(
    val isSuccess: Boolean,
    val accountsSynced: Int = 0,
    val groupsSynced: Int = 0,
    val transactionsSynced: Int = 0,
    val message: String
)

class FmsSyncManager(private val clientManager: FmsClientManager) {

    private val isoDateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)

    suspend fun syncAll(db: AppDatabase): FmsSyncResult = withContext(Dispatchers.IO) {
        try {
            val service = clientManager.getService()
            var accountsCount = 0
            var groupsCount = 0
            var transactionsCount = 0

            // 1. Sync Accounts
            val accountsResponse = service.getAccounts()
            if (accountsResponse.isSuccessful) {
                val remoteAccounts = accountsResponse.body() ?: emptyList()
                for (remote in remoteAccounts) {
                    val existing = db.accountDao().getAccountById(remote.id.hashCode().toLong())
                    if (existing == null) {
                        db.accountDao().insertAccount(
                            AccountEntity(
                                id = Math.abs(remote.id.hashCode().toLong()),
                                name = remote.name,
                                type = remote.type.uppercase(Locale.US),
                                balance = remote.balance,
                                currency = remote.currency ?: "$",
                                institution = remote.institution ?: "Bank",
                                accountNumberLast4 = remote.accountNumber ?: "0000"
                            )
                        )
                    } else {
                        db.accountDao().updateAccount(
                            existing.copy(
                                name = remote.name,
                                balance = remote.balance,
                                institution = remote.institution ?: existing.institution
                            )
                        )
                    }
                    accountsCount++
                }
            }

            // 2. Sync Groups
            val groupsResponse = service.getGroups()
            if (groupsResponse.isSuccessful) {
                val remoteGroups = groupsResponse.body() ?: emptyList()
                for (remote in remoteGroups) {
                    val localId = Math.abs(remote.id.hashCode().toLong())
                    val existing = db.groupDao().getGroupById(localId)
                    if (existing == null) {
                        db.groupDao().insertGroup(
                            GroupEntity(
                                id = localId,
                                name = remote.name,
                                category = "General",
                                iconName = remote.icon ?: "group",
                                coverColorHex = remote.color ?: "#00B77D",
                                memberIds = "1"
                            )
                        )
                    }
                    groupsCount++
                }
            }

            // 3. Sync Transactions
            val transactionsResponse = service.getTransactions()
            if (transactionsResponse.isSuccessful) {
                val remoteTransactions = transactionsResponse.body() ?: emptyList()
                for (remote in remoteTransactions) {
                    val localId = Math.abs(remote.id.hashCode().toLong())
                    val existing = db.expenseDao().getExpenseById(localId)
                    if (existing == null) {
                        val parsedDate = try {
                            isoDateFormat.parse(remote.date)?.time ?: System.currentTimeMillis()
                        } catch (e: Exception) {
                            System.currentTimeMillis()
                        }

                        db.expenseDao().insertExpense(
                            ExpenseEntity(
                                id = localId,
                                title = remote.description,
                                amount = remote.amount,
                                currency = "$",
                                dateMillis = parsedDate,
                                payerId = 1L,
                                groupId = null,
                                category = remote.categoryId ?: "General",
                                splitType = "EQUAL",
                                splitDetailsJson = "{}",
                                notes = remote.notes ?: "Synced from FMS Backend",
                                isSettlement = remote.status == "settled"
                            )
                        )
                        transactionsCount++
                    }
                }
            }

            FmsSyncResult(
                isSuccess = true,
                accountsSynced = accountsCount,
                groupsSynced = groupsCount,
                transactionsSynced = transactionsCount,
                message = "Successfully synchronized $transactionsCount transactions, $accountsCount accounts, and $groupsCount groups with FMS backend."
            )
        } catch (e: Exception) {
            e.printStackTrace()
            FmsSyncResult(
                isSuccess = false,
                message = "Sync failed: ${e.localizedMessage ?: "Network error"}"
            )
        }
    }

    suspend fun pushLocalExpense(db: AppDatabase, expense: ExpenseEntity): Boolean = withContext(Dispatchers.IO) {
        try {
            val service = clientManager.getService()
            val accountId = "default-acc"
            val dateStr = isoDateFormat.format(Date(expense.dateMillis))

            val dto = CreateTransactionDto(
                type = if (expense.amount >= 0) "expense" else "income",
                amount = Math.abs(expense.amount),
                description = expense.title,
                date = dateStr,
                accountId = accountId,
                categoryId = expense.category,
                notes = expense.notes,
                status = if (expense.isSettlement) "settled" else "confirmed",
                scope = if (expense.groupId != null) "group" else "personal"
            )

            val res = service.createTransaction(dto)
            res.isSuccessful
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
