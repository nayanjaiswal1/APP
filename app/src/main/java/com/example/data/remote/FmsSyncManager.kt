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
    val isOffline: Boolean = false,
    val isUnauthorized: Boolean = false,
    val conflictsResolved: Int = 0,
    val accountsSynced: Int = 0,
    val groupsSynced: Int = 0,
    val transactionsSynced: Int = 0,
    val message: String
)

class FmsSyncManager(private val clientManager: FmsClientManager) {

    private val isoDateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)

    suspend fun syncAll(db: AppDatabase): FmsSyncResult = withContext(Dispatchers.IO) {
        try {
            val service = try {
                clientManager.getService()
            } catch (e: Exception) {
                val msg = clientManager.handleNetworkException("Sync All", e)
                return@withContext FmsSyncResult(
                    isSuccess = false,
                    isOffline = true,
                    message = msg
                )
            }

            var accountsCount = 0
            var groupsCount = 0
            var transactionsCount = 0
            var conflictsCount = 0

            // 1. Sync Accounts
            try {
                val accountsResponse = service.getAccounts()
                if (accountsResponse.isSuccessful) {
                    val remoteAccounts = accountsResponse.body() ?: emptyList()
                    for (remote in remoteAccounts) {
                        val localId = Math.abs(remote.id.hashCode().toLong())
                        val existing = db.accountDao().getAccountById(localId)
                        if (existing == null) {
                            db.accountDao().insertAccount(
                                AccountEntity(
                                    id = localId,
                                    name = remote.name,
                                    type = remote.type.uppercase(Locale.US),
                                    balance = remote.balance,
                                    currency = remote.currency ?: "$",
                                    institution = remote.institution ?: "Bank",
                                    accountNumberLast4 = remote.accountNumber ?: "0000"
                                )
                            )
                        } else {
                            // Merge conflict resolution: keep local if newer or update balance
                            db.accountDao().updateAccount(
                                existing.copy(
                                    name = remote.name,
                                    balance = remote.balance,
                                    institution = remote.institution ?: existing.institution
                                )
                            )
                            conflictsCount++
                        }
                        accountsCount++
                    }
                } else if (accountsResponse.code() == 401) {
                    return@withContext FmsSyncResult(
                        isSuccess = false,
                        isUnauthorized = true,
                        message = "Session expired. Please sign in to sync with cloud."
                    )
                }
            } catch (e: Exception) {
                clientManager.handleNetworkException("Sync Accounts", e)
            }

            // 2. Sync Groups
            try {
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
            } catch (e: Exception) {
                clientManager.handleNetworkException("Sync Groups", e)
            }

            // 3. Sync Transactions
            try {
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
                        } else {
                            conflictsCount++
                        }
                    }
                }
            } catch (e: Exception) {
                clientManager.handleNetworkException("Sync Transactions", e)
            }

            val summaryMsg = if (conflictsCount > 0) {
                "Synced $transactionsCount transactions, $accountsCount accounts ($conflictsCount merged with server)."
            } else {
                "Synced $transactionsCount transactions, $accountsCount accounts, and $groupsCount groups."
            }

            FmsSyncResult(
                isSuccess = true,
                accountsSynced = accountsCount,
                groupsSynced = groupsCount,
                transactionsSynced = transactionsCount,
                conflictsResolved = conflictsCount,
                message = summaryMsg
            )
        } catch (e: Exception) {
            val msg = clientManager.handleNetworkException("Sync All", e)
            FmsSyncResult(
                isSuccess = false,
                isOffline = true,
                message = msg
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
            if (res.isSuccessful) {
                true
            } else if (res.code() == 409) {
                // Conflict: Transaction already exists on server, treat as resolved
                true
            } else {
                false
            }
        } catch (e: Exception) {
            clientManager.handleNetworkException("Push Expense (${expense.title})", e)
            false
        }
    }
}
