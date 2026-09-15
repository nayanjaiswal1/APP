package com.example.data.repository

import com.example.data.local.AppDatabase
import com.example.data.local.entity.AccountEntity
import com.example.data.local.entity.ExpenseEntity
import com.example.data.local.entity.GroupEntity
import com.example.data.local.entity.InvoiceEntity
import com.example.data.local.entity.ParsedMessageEntity
import com.example.data.local.entity.StatementEntity
import com.example.data.local.entity.UserEntity
import kotlinx.coroutines.flow.Flow

class ExpenseRepository(private val db: AppDatabase) {

    val allUsers: Flow<List<UserEntity>> = db.userDao().getAllUsers()
    val allGroups: Flow<List<GroupEntity>> = db.groupDao().getAllGroups()
    val allExpenses: Flow<List<ExpenseEntity>> = db.expenseDao().getAllExpenses()
    val allParsedMessages: Flow<List<ParsedMessageEntity>> = db.parsedMessageDao().getAllParsedMessages()
    val allAccounts: Flow<List<AccountEntity>> = db.accountDao().getAllAccounts()
    val allStatements: Flow<List<StatementEntity>> = db.statementDao().getAllStatements()
    val allInvoices: Flow<List<InvoiceEntity>> = db.invoiceDao().getAllInvoices()

    fun getExpensesByGroup(groupId: Long): Flow<List<ExpenseEntity>> {
        return db.expenseDao().getExpensesByGroup(groupId)
    }

    fun getStatementsByAccount(accountId: Long): Flow<List<StatementEntity>> {
        return db.statementDao().getStatementsByAccount(accountId)
    }

    suspend fun getUserById(id: Long): UserEntity? = db.userDao().getUserById(id)
    suspend fun getGroupById(id: Long): GroupEntity? = db.groupDao().getGroupById(id)
    suspend fun getAccountById(id: Long): AccountEntity? = db.accountDao().getAccountById(id)
    suspend fun getInvoiceById(id: Long): InvoiceEntity? = db.invoiceDao().getInvoiceById(id)

    suspend fun insertUser(user: UserEntity): Long = db.userDao().insertUser(user)
    suspend fun updateUser(user: UserEntity) = db.userDao().updateUser(user)
    suspend fun deleteUser(user: UserEntity) = db.userDao().deleteUser(user)

    suspend fun insertGroup(group: GroupEntity): Long = db.groupDao().insertGroup(group)
    suspend fun updateGroup(group: GroupEntity) = db.groupDao().updateGroup(group)
    suspend fun deleteGroup(group: GroupEntity) = db.groupDao().deleteGroup(group)

    suspend fun insertExpense(expense: ExpenseEntity): Long = db.expenseDao().insertExpense(expense)
    suspend fun insertExpenses(expenses: List<ExpenseEntity>) {
        expenses.forEach { db.expenseDao().insertExpense(it) }
    }
    suspend fun updateExpense(expense: ExpenseEntity) = db.expenseDao().updateExpense(expense)
    suspend fun deleteExpenseById(id: Long) = db.expenseDao().deleteExpenseById(id)

    suspend fun insertAccount(account: AccountEntity): Long = db.accountDao().insertAccount(account)
    suspend fun updateAccount(account: AccountEntity) = db.accountDao().updateAccount(account)
    suspend fun deleteAccountById(id: Long) = db.accountDao().deleteAccountById(id)

    suspend fun insertStatement(statement: StatementEntity): Long = db.statementDao().insertStatement(statement)
    suspend fun deleteStatementById(id: Long) = db.statementDao().deleteStatementById(id)

    suspend fun insertInvoice(invoice: InvoiceEntity): Long = db.invoiceDao().insertInvoice(invoice)
    suspend fun updateInvoice(invoice: InvoiceEntity) = db.invoiceDao().updateInvoice(invoice)
    suspend fun deleteInvoiceById(id: Long) = db.invoiceDao().deleteInvoiceById(id)

    suspend fun insertParsedMessage(message: ParsedMessageEntity): Long = db.parsedMessageDao().insertParsedMessage(message)
    suspend fun updateParsedMessageStatus(id: Long, status: String) = db.parsedMessageDao().updateStatus(id, status)
    suspend fun deleteParsedMessage(message: ParsedMessageEntity) = db.parsedMessageDao().deleteParsedMessage(message)
}
