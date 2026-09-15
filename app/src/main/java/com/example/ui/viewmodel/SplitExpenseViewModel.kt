package com.example.ui.viewmodel

import android.app.Application
import android.graphics.Bitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.local.entity.AccountEntity
import com.example.data.local.entity.ExpenseEntity
import com.example.data.local.entity.GroupEntity
import com.example.data.local.entity.InvoiceEntity
import com.example.data.local.entity.ParsedMessageEntity
import com.example.data.local.entity.StatementEntity
import com.example.data.local.entity.UserEntity
import com.example.data.parser.InvoiceLineItem
import com.example.data.parser.InvoiceParserAndTranslator
import com.example.data.parser.MessageParserEngine
import com.example.data.parser.ParsedExpenseResult
import com.example.data.parser.ParsedInvoiceResult
import com.example.data.parser.SampleBatchTemplate
import com.example.data.parser.SampleInvoice
import com.example.data.parser.SampleMessageTemplate
import com.example.data.parser.SampleStatement
import com.example.data.parser.StatementParserEngine
import com.example.data.parser.StatementTransaction
import com.example.data.repository.ExpenseRepository
import com.example.data.sms.DeviceSmsItem
import com.example.data.sms.DeviceSmsReader
import com.example.domain.model.DebtEngine
import com.example.domain.model.DebtTransfer
import com.example.domain.model.GroupSummary
import com.example.domain.model.UserBalanceSummary
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import kotlin.math.abs

data class BaseData(
    val users: List<UserEntity>,
    val groups: List<GroupEntity>,
    val expenses: List<ExpenseEntity>,
    val msgs: List<ParsedMessageEntity>
)

data class SettlePrefill(
    val fromUserId: Long,
    val toUserId: Long,
    val suggestedAmount: Double,
    val groupId: Long? = null
)

data class SplitExpenseUiState(
    val users: List<UserEntity> = emptyList(),
    val groups: List<GroupEntity> = emptyList(),
    val expenses: List<ExpenseEntity> = emptyList(),
    val parsedMessages: List<ParsedMessageEntity> = emptyList(),
    val accounts: List<AccountEntity> = emptyList(),
    val statements: List<StatementEntity> = emptyList(),
    val invoices: List<InvoiceEntity> = emptyList(),
    val currentUserId: Long = 1,
    val overallNetBalance: Double = 0.0,
    val totalYouAreOwed: Double = 0.0,
    val totalYouOwe: Double = 0.0,
    val friendSummaries: List<UserBalanceSummary> = emptyList(),
    val groupSummaries: Map<Long, GroupSummary> = emptyMap(),
    val globalSimplifiedTransfers: List<DebtTransfer> = emptyList(),
    val selectedGroupId: Long? = null,
    val selectedFriendId: Long? = null,
    val selectedAccountId: Long? = null,

    // Single message parsing
    val messageInputText: String = "",
    val isParsingMessage: Boolean = false,
    val activeParsedResult: ParsedExpenseResult? = null,
    val parseErrorMessage: String? = null,

    // Multi-notification batch parsing
    val batchInputText: String = "",
    val isParsingBatch: Boolean = false,
    val batchParsedResults: List<ParsedExpenseResult> = emptyList(),
    val selectedBatchIndexes: Set<Int> = emptySet(),

    // Statement parsing
    val statementInputText: String = "",
    val isParsingStatement: Boolean = false,
    val parsedStatementTransactions: List<StatementTransaction> = emptyList(),
    val selectedStatementAccount: AccountEntity? = null,

    // Invoice parsing & translation
    val invoiceInputText: String = "",
    val invoiceBitmap: Bitmap? = null,
    val invoiceTargetLanguage: String = "English",
    val isParsingInvoice: Boolean = false,
    val activeParsedInvoice: ParsedInvoiceResult? = null,
    val selectedInvoiceDetail: InvoiceEntity? = null,

    // Device SMS Auto-Sync
    val deviceSmsList: List<DeviceSmsItem> = emptyList(),
    val isScanningDeviceSms: Boolean = false,
    val hasSmsPermission: Boolean = false,
    val selectedDeviceSmsIds: Set<Long> = emptySet(),
    val smsSyncSuccessMessage: String? = null,

    // Dialogs & Modals
    val isAddExpenseOpen: Boolean = false,
    val isSettleUpOpen: Boolean = false,
    val isAddGroupOpen: Boolean = false,
    val isAddFriendOpen: Boolean = false,
    val isAddAccountOpen: Boolean = false,
    val isUploadStatementOpen: Boolean = false,
    val isInvoiceDetailOpen: Boolean = false,
    val isSimplifyDebtsOpen: Boolean = false,
    val settlePrefill: SettlePrefill? = null,
    val expensePrefill: ParsedExpenseResult? = null,

    // Voice-to-Text Gemini Expense Parsing
    val isVoiceExpenseOpen: Boolean = false,
    val voiceTranscript: String = "",
    val isListeningVoice: Boolean = false,
    val isParsingVoice: Boolean = false,
    val activeParsedVoiceResult: ParsedExpenseResult? = null,
    val voiceErrorMessage: String? = null,
    val voiceSuccessToast: String? = null
)

class SplitExpenseViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: ExpenseRepository
    init {
        val db = AppDatabase.getDatabase(application, viewModelScope)
        repository = ExpenseRepository(db)
    }

    private val _uiState = MutableStateFlow(SplitExpenseUiState())
    val uiState: StateFlow<SplitExpenseUiState> = _uiState.asStateFlow()

    init {
        observeData()
    }

    private fun observeData() {
        viewModelScope.launch {
            val baseFlow = combine(
                repository.allUsers,
                repository.allGroups,
                repository.allExpenses,
                repository.allParsedMessages
            ) { users, groups, expenses, msgs ->
                BaseData(users, groups, expenses, msgs)
            }

            val financialFlow = combine(
                repository.allAccounts,
                repository.allStatements,
                repository.allInvoices
            ) { accounts, statements, invoices ->
                Triple(accounts, statements, invoices)
            }

            combine(baseFlow, financialFlow) { base, financial ->
                computeState(
                    users = base.users,
                    groups = base.groups,
                    expenses = base.expenses,
                    parsedMsgs = base.msgs,
                    accounts = financial.first,
                    statements = financial.second,
                    invoices = financial.third
                )
            }.collect { newState ->
                _uiState.value = newState
            }
        }
    }

    private fun computeState(
        users: List<UserEntity>,
        groups: List<GroupEntity>,
        expenses: List<ExpenseEntity>,
        parsedMsgs: List<ParsedMessageEntity>,
        accounts: List<AccountEntity>,
        statements: List<StatementEntity>,
        invoices: List<InvoiceEntity>
    ): SplitExpenseUiState {
        val currentUserId = users.find { it.isCurrentUser }?.id ?: 1L
        val allUserIds = users.map { it.id }

        // 1. Calculate overall global net balances
        val netBalances = DebtEngine.calculateNetBalances(allUserIds, expenses)
        val globalTransfers = DebtEngine.simplifyDebts(netBalances)
        val directTransfers = DebtEngine.calculateDirectTransfers(allUserIds, expenses)

        val userNet = netBalances[currentUserId] ?: 0.0

        // Calculate positive and negative sums for "You"
        var youAreOwedSum = 0.0
        var youOweSum = 0.0

        val friendSummaries = mutableListOf<UserBalanceSummary>()

        for (user in users) {
            if (user.id == currentUserId) continue

            val transfersToPay = directTransfers.filter { it.fromUserId == currentUserId && it.toUserId == user.id }
            val transfersToReceive = directTransfers.filter { it.fromUserId == user.id && it.toUserId == currentUserId }

            val amountYouOweThem = transfersToPay.sumOf { it.amount }
            val amountTheyOweYou = transfersToReceive.sumOf { it.amount }
            val pairwiseNet = amountTheyOweYou - amountYouOweThem

            if (pairwiseNet > 0.01) {
                youAreOwedSum += pairwiseNet
            } else if (pairwiseNet < -0.01) {
                youOweSum += abs(pairwiseNet)
            }

            friendSummaries.add(
                UserBalanceSummary(
                    userId = user.id,
                    userName = user.name,
                    userAvatarColor = user.avatarColorHex,
                    netBalance = pairwiseNet,
                    transfersToPay = transfersToPay,
                    transfersToReceive = transfersToReceive
                )
            )
        }

        // 2. Compute per-group summaries
        val groupSummaries = mutableMapOf<Long, GroupSummary>()
        for (group in groups) {
            val groupMemberIds = group.memberIds.split(",")
                .mapNotNull { it.trim().toLongOrNull() }
            val groupExpenses = expenses.filter { it.groupId == group.id }
            val groupSpend = groupExpenses.filter { !it.isSettlement }.sumOf { it.amount }

            val groupNetBalances = DebtEngine.calculateNetBalances(groupMemberIds, groupExpenses)
            val groupSimplified = DebtEngine.simplifyDebts(groupNetBalances)
            val groupDirect = DebtEngine.calculateDirectTransfers(groupMemberIds, groupExpenses)
            val groupUserNet = groupNetBalances[currentUserId] ?: 0.0

            groupSummaries[group.id] = GroupSummary(
                totalSpend = groupSpend,
                userNetBalance = groupUserNet,
                simplifiedTransfers = groupSimplified,
                directTransfers = groupDirect,
                memberBalances = groupNetBalances
            )
        }

        return _uiState.value.copy(
            users = users,
            groups = groups,
            expenses = expenses,
            parsedMessages = parsedMsgs,
            accounts = accounts,
            statements = statements,
            invoices = invoices,
            currentUserId = currentUserId,
            overallNetBalance = userNet,
            totalYouAreOwed = youAreOwedSum,
            totalYouOwe = youOweSum,
            friendSummaries = friendSummaries,
            groupSummaries = groupSummaries,
            globalSimplifiedTransfers = globalTransfers
        )
    }

    // ==========================================
    // 1. Single Message Parsing Operations
    // ==========================================

    fun onMessageInputChange(text: String) {
        _uiState.value = _uiState.value.copy(
            messageInputText = text,
            parseErrorMessage = null
        )
    }

    fun parseCurrentMessage() {
        val text = _uiState.value.messageInputText.trim()
        if (text.isBlank()) return

        _uiState.value = _uiState.value.copy(isParsingMessage = true, parseErrorMessage = null)

        viewModelScope.launch {
            try {
                val result = MessageParserEngine.parse(text)
                _uiState.value = _uiState.value.copy(
                    isParsingMessage = false,
                    activeParsedResult = result
                )

                // Save to history
                repository.insertParsedMessage(
                    ParsedMessageEntity(
                        rawMessage = text,
                        parsedTitle = result.title,
                        parsedAmount = result.amount,
                        parsedCurrency = result.currency,
                        parsedCategory = result.category,
                        parsedMerchant = result.merchant,
                        parsedDateMillis = result.dateMillis,
                        suggestedSplitCount = result.suggestedSplitCount,
                        status = "NEW"
                    )
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isParsingMessage = false,
                    parseErrorMessage = "Parsing error: ${e.localizedMessage ?: "Unknown"}"
                )
            }
        }
    }

    fun selectSampleTemplate(template: SampleMessageTemplate) {
        _uiState.value = _uiState.value.copy(messageInputText = template.rawText)
        parseCurrentMessage()
    }

    fun clearParsedResult() {
        _uiState.value = _uiState.value.copy(activeParsedResult = null, messageInputText = "")
    }

    fun convertParsedResultToExpense(result: ParsedExpenseResult) {
        _uiState.value = _uiState.value.copy(
            isAddExpenseOpen = true,
            expensePrefill = result
        )
    }

    // ==========================================
    // 2. Batch / Multi-Notification Parsing
    // ==========================================

    fun onBatchInputChange(text: String) {
        _uiState.value = _uiState.value.copy(batchInputText = text)
    }

    fun parseBatchNotifications() {
        val text = _uiState.value.batchInputText.trim()
        if (text.isBlank()) return

        _uiState.value = _uiState.value.copy(isParsingBatch = true)

        viewModelScope.launch {
            try {
                val results = MessageParserEngine.parseMultipleMessages(text)
                val allIndices = results.indices.toSet()
                _uiState.value = _uiState.value.copy(
                    isParsingBatch = false,
                    batchParsedResults = results,
                    selectedBatchIndexes = allIndices
                )

                // Save each to parsed messages history
                results.forEach { res ->
                    repository.insertParsedMessage(
                        ParsedMessageEntity(
                            rawMessage = res.rawMessage,
                            parsedTitle = res.title,
                            parsedAmount = res.amount,
                            parsedCurrency = res.currency,
                            parsedCategory = res.category,
                            parsedMerchant = res.merchant,
                            parsedDateMillis = res.dateMillis,
                            suggestedSplitCount = res.suggestedSplitCount,
                            status = "NEW"
                        )
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isParsingBatch = false)
            }
        }
    }

    fun selectSampleBatchTemplate(template: SampleBatchTemplate) {
        _uiState.value = _uiState.value.copy(batchInputText = template.rawBatch)
        parseBatchNotifications()
    }

    fun toggleBatchItemSelection(index: Int) {
        val current = _uiState.value.selectedBatchIndexes.toMutableSet()
        if (current.contains(index)) {
            current.remove(index)
        } else {
            current.add(index)
        }
        _uiState.value = _uiState.value.copy(selectedBatchIndexes = current)
    }

    fun selectAllBatchItems(selectAll: Boolean) {
        val indices = if (selectAll) _uiState.value.batchParsedResults.indices.toSet() else emptySet()
        _uiState.value = _uiState.value.copy(selectedBatchIndexes = indices)
    }

    fun addSelectedBatchToExpenses(targetGroupId: Long? = null, targetAccountId: Long? = null) {
        val selected = _uiState.value.selectedBatchIndexes
        val results = _uiState.value.batchParsedResults
        val myId = _uiState.value.currentUserId
        val users = _uiState.value.users

        viewModelScope.launch {
            val expensesToAdd = mutableListOf<ExpenseEntity>()

            selected.forEach { idx ->
                if (idx in results.indices) {
                    val item = results[idx]
                    val splitMap = mutableMapOf<Long, Double>()

                    // Determine participants
                    val splitCount = item.suggestedSplitCount.coerceIn(1, 10)
                    if (targetGroupId != null) {
                        val group = _uiState.value.groups.find { it.id == targetGroupId }
                        val members = group?.memberIds?.split(",")?.mapNotNull { it.trim().toLongOrNull() } ?: listOf(myId)
                        val perPerson = item.amount / members.size.coerceAtLeast(1)
                        members.forEach { splitMap[it] = perPerson }
                    } else {
                        // Split between You and friends or personal
                        if (splitCount > 1) {
                            val perPerson = item.amount / splitCount
                            splitMap[myId] = perPerson
                            val otherUsers = users.filter { it.id != myId }.take(splitCount - 1)
                            otherUsers.forEach { splitMap[it.id] = perPerson }
                        } else {
                            splitMap[myId] = item.amount
                        }
                    }

                    expensesToAdd.add(
                        ExpenseEntity(
                            title = item.title,
                            amount = item.amount,
                            currency = item.currency,
                            category = item.category,
                            groupId = targetGroupId,
                            payerId = myId,
                            splitType = "EQUAL",
                            splitDetailsJson = DebtEngine.formatSplitDetails(splitMap),
                            dateMillis = item.dateMillis,
                            notes = "Batch imported from notification",
                            sourceMessage = item.rawMessage,
                            accountId = targetAccountId
                        )
                    )
                }
            }

            if (expensesToAdd.isNotEmpty()) {
                repository.insertExpenses(expensesToAdd)
            }

            _uiState.value = _uiState.value.copy(
                batchParsedResults = emptyList(),
                selectedBatchIndexes = emptySet(),
                batchInputText = ""
            )
        }
    }

    // ==========================================
    // 3. Accounts & Statement Operations
    // ==========================================

    fun addAccount(
        name: String,
        type: String,
        institution: String,
        last4: String,
        initialBalance: Double,
        currency: String = "$",
        colorHex: String = "#6750A4",
        iconName: String = "credit_card"
    ) {
        viewModelScope.launch {
            val account = AccountEntity(
                name = name,
                type = type,
                institution = institution,
                accountNumberLast4 = last4.ifBlank { "0000" },
                balance = initialBalance,
                currency = currency,
                colorHex = colorHex,
                iconName = iconName
            )
            repository.insertAccount(account)
            closeAddAccount()
        }
    }

    fun deleteAccount(accountId: Long) {
        viewModelScope.launch {
            repository.deleteAccountById(accountId)
        }
    }

    fun onStatementInputChange(text: String) {
        _uiState.value = _uiState.value.copy(statementInputText = text)
    }

    fun selectAccountForStatement(account: AccountEntity?) {
        _uiState.value = _uiState.value.copy(selectedStatementAccount = account)
    }

    fun parseCurrentStatement() {
        val text = _uiState.value.statementInputText.trim()
        if (text.isBlank()) return

        _uiState.value = _uiState.value.copy(isParsingStatement = true)

        viewModelScope.launch {
            try {
                val transactions = StatementParserEngine.parseStatement(
                    rawText = text,
                    defaultCurrency = _uiState.value.selectedStatementAccount?.currency ?: "$"
                )
                _uiState.value = _uiState.value.copy(
                    isParsingStatement = false,
                    parsedStatementTransactions = transactions
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isParsingStatement = false)
            }
        }
    }

    fun selectSampleStatement(sample: SampleStatement) {
        _uiState.value = _uiState.value.copy(statementInputText = sample.rawContent)
        val matchedAccount = _uiState.value.accounts.find { it.name.contains(sample.accountName, ignoreCase = true) }
        if (matchedAccount != null) {
            _uiState.value = _uiState.value.copy(selectedStatementAccount = matchedAccount)
        }
        parseCurrentStatement()
    }

    fun toggleStatementTxSelection(txId: String) {
        val updated = _uiState.value.parsedStatementTransactions.map {
            if (it.id == txId) it.copy(isSelected = !it.isSelected) else it
        }
        _uiState.value = _uiState.value.copy(parsedStatementTransactions = updated)
    }

    fun selectAllStatementTransactions(selectAll: Boolean) {
        val updated = _uiState.value.parsedStatementTransactions.map {
            it.copy(isSelected = selectAll)
        }
        _uiState.value = _uiState.value.copy(parsedStatementTransactions = updated)
    }

    fun importStatementExpenses(targetGroupId: Long? = null) {
        val selectedTxs = _uiState.value.parsedStatementTransactions.filter { it.isSelected }
        if (selectedTxs.isEmpty()) return

        val account = _uiState.value.selectedStatementAccount
        val myId = _uiState.value.currentUserId

        viewModelScope.launch {
            // Save Statement Record
            val totalExpenseAmt = selectedTxs.sumOf { it.amount }
            val statementEntity = StatementEntity(
                accountId = account?.id ?: 1L,
                fileName = "${account?.name ?: "Account"} Statement ${System.currentTimeMillis() % 10000}",
                rawContent = _uiState.value.statementInputText,
                parsedTransactionsCount = selectedTxs.size,
                totalExpensesAmount = totalExpenseAmt
            )
            repository.insertStatement(statementEntity)

            // Convert to ExpenseEntities
            val expenses = selectedTxs.map { tx ->
                val splitMap = mutableMapOf<Long, Double>()
                if (targetGroupId != null) {
                    val group = _uiState.value.groups.find { it.id == targetGroupId }
                    val members = group?.memberIds?.split(",")?.mapNotNull { it.trim().toLongOrNull() } ?: listOf(myId)
                    val perPerson = tx.amount / members.size.coerceAtLeast(1)
                    members.forEach { splitMap[it] = perPerson }
                } else {
                    splitMap[myId] = tx.amount
                }

                ExpenseEntity(
                    title = tx.cleanMerchant.ifBlank { tx.description },
                    amount = tx.amount,
                    currency = tx.currency,
                    category = tx.category,
                    groupId = targetGroupId,
                    payerId = myId,
                    splitType = "EQUAL",
                    splitDetailsJson = DebtEngine.formatSplitDetails(splitMap),
                    dateMillis = tx.dateMillis,
                    notes = "Imported from ${account?.name ?: "Bank Statement"}: ${tx.description}",
                    sourceMessage = tx.rawLine,
                    accountId = account?.id
                )
            }

            repository.insertExpenses(expenses)

            // Update Account Balance
            if (account != null) {
                val newBal = (account.balance - totalExpenseAmt).coerceAtLeast(0.0)
                repository.updateAccount(account.copy(balance = newBal))
            }

            closeUploadStatement()
            _uiState.value = _uiState.value.copy(
                statementInputText = "",
                parsedStatementTransactions = emptyList()
            )
        }
    }

    // ==========================================
    // 4. Invoice Parsing, Translation & Storage
    // ==========================================

    fun onInvoiceInputChange(text: String) {
        _uiState.value = _uiState.value.copy(invoiceInputText = text)
    }

    fun setInvoiceBitmap(bitmap: Bitmap?) {
        _uiState.value = _uiState.value.copy(invoiceBitmap = bitmap)
    }

    fun setInvoiceTargetLanguage(lang: String) {
        _uiState.value = _uiState.value.copy(invoiceTargetLanguage = lang)
    }

    fun parseAndTranslateInvoice() {
        val text = _uiState.value.invoiceInputText
        val bitmap = _uiState.value.invoiceBitmap
        val lang = _uiState.value.invoiceTargetLanguage

        if (text.isBlank() && bitmap == null) return

        _uiState.value = _uiState.value.copy(isParsingInvoice = true)

        viewModelScope.launch {
            try {
                val result = InvoiceParserAndTranslator.parseAndTranslate(
                    rawText = text,
                    bitmap = bitmap,
                    targetLanguage = lang
                )
                _uiState.value = _uiState.value.copy(
                    isParsingInvoice = false,
                    activeParsedInvoice = result
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isParsingInvoice = false)
            }
        }
    }

    fun selectSampleInvoice(sample: SampleInvoice) {
        _uiState.value = _uiState.value.copy(
            invoiceInputText = sample.rawText,
            invoiceBitmap = null
        )
        parseAndTranslateInvoice()
    }

    fun saveParsedInvoiceAndAddToExpense(
        invoiceResult: ParsedInvoiceResult,
        splitWithGroupId: Long? = null,
        payerId: Long? = null
    ) {
        val myId = payerId ?: _uiState.value.currentUserId

        viewModelScope.launch {
            // 1. Serialize line items
            val lineItemsJsonArr = JSONArray()
            invoiceResult.lineItems.forEach { item ->
                val obj = JSONObject().apply {
                    put("originalName", item.originalName)
                    put("translatedName", item.translatedName)
                    put("quantity", item.quantity)
                    put("unitPrice", item.unitPrice)
                    put("totalPrice", item.totalPrice)
                }
                lineItemsJsonArr.put(obj)
            }

            // 2. Insert Invoice
            val invoiceEntity = InvoiceEntity(
                invoiceNumber = invoiceResult.invoiceNumber,
                vendorName = invoiceResult.vendorName,
                totalAmount = invoiceResult.totalAmount,
                subtotalAmount = invoiceResult.subtotalAmount,
                taxAmount = invoiceResult.taxAmount,
                tipAmount = invoiceResult.tipAmount,
                currency = invoiceResult.currency,
                dateMillis = invoiceResult.dateMillis,
                originalLanguage = invoiceResult.originalLanguage,
                translatedLanguage = invoiceResult.translatedLanguage,
                rawOcrText = invoiceResult.rawOcrText,
                translatedSummary = invoiceResult.translatedSummary,
                lineItemsJson = lineItemsJsonArr.toString(),
                category = invoiceResult.category
            )
            val invoiceId = repository.insertInvoice(invoiceEntity)

            // 3. Create linked expense
            val splitMap = mutableMapOf<Long, Double>()
            if (splitWithGroupId != null) {
                val group = _uiState.value.groups.find { it.id == splitWithGroupId }
                val members = group?.memberIds?.split(",")?.mapNotNull { it.trim().toLongOrNull() } ?: listOf(myId)
                val perPerson = invoiceResult.totalAmount / members.size.coerceAtLeast(1)
                members.forEach { splitMap[it] = perPerson }
            } else {
                splitMap[myId] = invoiceResult.totalAmount
            }

            val expense = ExpenseEntity(
                title = "${invoiceResult.vendorName} (Translated)",
                amount = invoiceResult.totalAmount,
                currency = invoiceResult.currency,
                category = invoiceResult.category,
                groupId = splitWithGroupId,
                payerId = myId,
                splitType = "EQUAL",
                splitDetailsJson = DebtEngine.formatSplitDetails(splitMap),
                dateMillis = invoiceResult.dateMillis,
                notes = "Translated from ${invoiceResult.originalLanguage}: ${invoiceResult.translatedSummary}",
                sourceMessage = invoiceResult.rawOcrText,
                invoiceId = invoiceId
            )
            val expenseId = repository.insertExpense(expense)

            // Update linked expense ID on invoice
            repository.updateInvoice(invoiceEntity.copy(id = invoiceId, linkedExpenseId = expenseId))

            _uiState.value = _uiState.value.copy(
                activeParsedInvoice = null,
                invoiceInputText = "",
                invoiceBitmap = null
            )
        }
    }

    fun openInvoiceDetail(invoice: InvoiceEntity) {
        _uiState.value = _uiState.value.copy(selectedInvoiceDetail = invoice, isInvoiceDetailOpen = true)
    }

    fun closeInvoiceDetail() {
        _uiState.value = _uiState.value.copy(selectedInvoiceDetail = null, isInvoiceDetailOpen = false)
    }

    fun deleteInvoice(invoiceId: Long) {
        viewModelScope.launch {
            repository.deleteInvoiceById(invoiceId)
            closeInvoiceDetail()
        }
    }

    // ==========================================
    // 5. Expense Operations
    // ==========================================

    fun addExpense(
        title: String,
        amount: Double,
        currency: String,
        category: String,
        groupId: Long?,
        payerId: Long,
        splitType: String,
        splitDetails: Map<Long, Double>,
        notes: String?,
        sourceMessage: String? = null,
        accountId: Long? = null
    ) {
        viewModelScope.launch {
            val expense = ExpenseEntity(
                title = title.ifBlank { "Expense" },
                amount = amount,
                currency = currency,
                category = category,
                groupId = groupId,
                payerId = payerId,
                splitType = splitType,
                splitDetailsJson = DebtEngine.formatSplitDetails(splitDetails),
                dateMillis = System.currentTimeMillis(),
                notes = notes,
                sourceMessage = sourceMessage,
                accountId = accountId
            )
            repository.insertExpense(expense)

            // Deduct balance from account if attached
            if (accountId != null) {
                val acc = _uiState.value.accounts.find { it.id == accountId }
                if (acc != null) {
                    val newBal = (acc.balance - amount).coerceAtLeast(0.0)
                    repository.updateAccount(acc.copy(balance = newBal))
                }
            }

            closeAddExpense()
        }
    }

    fun deleteExpense(id: Long) {
        viewModelScope.launch {
            repository.deleteExpenseById(id)
        }
    }

    // ==========================================
    // 6. Settle Up Operations
    // ==========================================

    fun settleDebt(
        fromUserId: Long,
        toUserId: Long,
        amount: Double,
        groupId: Long? = null,
        paymentNote: String = "Paid via Settle Up",
        accountId: Long? = null
    ) {
        viewModelScope.launch {
            val fromName = _uiState.value.users.find { it.id == fromUserId }?.name ?: "User"
            val toName = _uiState.value.users.find { it.id == toUserId }?.name ?: "User"

            val settlementExpense = ExpenseEntity(
                title = "$fromName paid $toName",
                amount = amount,
                currency = "$",
                category = "GENERAL",
                groupId = groupId,
                payerId = fromUserId,
                splitType = "EXACT",
                splitDetailsJson = """{"$toUserId":$amount}""",
                dateMillis = System.currentTimeMillis(),
                notes = paymentNote,
                isSettlement = true,
                settlementFromId = fromUserId,
                settlementToId = toUserId,
                accountId = accountId
            )
            repository.insertExpense(settlementExpense)

            if (accountId != null) {
                val acc = _uiState.value.accounts.find { it.id == accountId }
                if (acc != null) {
                    val newBal = (acc.balance - amount).coerceAtLeast(0.0)
                    repository.updateAccount(acc.copy(balance = newBal))
                }
            }

            closeSettleUp()
        }
    }

    // ==========================================
    // 7. Automatic Device SMS Sync & Parsing
    // ==========================================

    fun setSmsPermissionGranted(granted: Boolean, context: android.content.Context) {
        _uiState.value = _uiState.value.copy(hasSmsPermission = granted)
        if (granted) {
            scanDeviceSms(context)
        }
    }

    fun scanDeviceSms(context: android.content.Context) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isScanningDeviceSms = true, smsSyncSuccessMessage = null)
            val hasPerm = DeviceSmsReader.hasSmsPermission(context)
            _uiState.value = _uiState.value.copy(hasSmsPermission = hasPerm)

            val rawSms = if (hasPerm) {
                DeviceSmsReader.readDeviceSms(context, limit = 50)
            } else {
                emptyList()
            }

            // If empty or emulator fallback
            val finalList = if (rawSms.isEmpty()) {
                DeviceSmsReader.getSimulatedDeviceInbox()
            } else {
                rawSms
            }

            val defaultSelected = finalList.map { it.id }.toSet()

            _uiState.value = _uiState.value.copy(
                isScanningDeviceSms = false,
                deviceSmsList = finalList,
                selectedDeviceSmsIds = defaultSelected
            )
        }
    }

    fun loadSimulatedDeviceSms() {
        val simulated = DeviceSmsReader.getSimulatedDeviceInbox()
        _uiState.value = _uiState.value.copy(
            deviceSmsList = simulated,
            selectedDeviceSmsIds = simulated.map { it.id }.toSet(),
            smsSyncSuccessMessage = "Loaded ${simulated.size} simulated device bank & card SMS alerts."
        )
    }

    fun toggleDeviceSmsSelection(smsId: Long) {
        val current = _uiState.value.selectedDeviceSmsIds.toMutableSet()
        if (current.contains(smsId)) {
            current.remove(smsId)
        } else {
            current.add(smsId)
        }
        _uiState.value = _uiState.value.copy(selectedDeviceSmsIds = current)
    }

    fun selectAllDeviceSms(select: Boolean) {
        val newSelection = if (select) {
            _uiState.value.deviceSmsList.map { it.id }.toSet()
        } else {
            emptySet()
        }
        _uiState.value = _uiState.value.copy(selectedDeviceSmsIds = newSelection)
    }

    fun importSelectedDeviceSms(targetGroupId: Long?) {
        viewModelScope.launch {
            val selectedIds = _uiState.value.selectedDeviceSmsIds
            val itemsToImport = _uiState.value.deviceSmsList.filter { selectedIds.contains(it.id) }

            if (itemsToImport.isEmpty()) return@launch

            val currentUserId = _uiState.value.currentUserId
            val group = targetGroupId?.let { gId -> _uiState.value.groups.find { it.id == gId } }
            val memberIds = if (group != null) {
                group.memberIds.split(",").mapNotNull { it.trim().toLongOrNull() }
            } else {
                listOf(currentUserId)
            }

            var importedCount = 0
            for (sms in itemsToImport) {
                val parsed = sms.parsedResult
                val amount = parsed.amount
                val title = parsed.title.ifBlank { "SMS: ${sms.sender}" }
                val category = parsed.category.ifBlank { "General" }
                val currency = parsed.currency.ifBlank { "USD" }

                val splitMap = mutableMapOf<Long, Double>()
                if (memberIds.size > 1) {
                    val share = (amount / memberIds.size)
                    memberIds.forEach { mId ->
                        splitMap[mId] = Math.round(share * 100.0) / 100.0
                    }
                } else {
                    splitMap[currentUserId] = amount
                }

                val splitJson = JSONObject(splitMap.mapKeys { it.key.toString() }).toString()

                val expense = ExpenseEntity(
                    title = title,
                    amount = amount,
                    currency = currency,
                    category = category,
                    groupId = targetGroupId,
                    payerId = currentUserId,
                    splitType = if (memberIds.size > 1) "EQUAL" else "EXACT",
                    splitDetailsJson = splitJson,
                    dateMillis = sms.dateMillis,
                    notes = "Auto-parsed from SMS (${sms.sender}): \"${sms.body.take(60)}...\"",
                    sourceMessage = sms.body
                )

                repository.insertExpense(expense)
                importedCount++
            }

            _uiState.value = _uiState.value.copy(
                selectedDeviceSmsIds = emptySet(),
                smsSyncSuccessMessage = "Successfully imported $importedCount SMS expense${if (importedCount > 1) "s" else ""} directly to your records!"
            )
        }
    }

    fun clearSmsSyncMessage() {
        _uiState.value = _uiState.value.copy(smsSyncSuccessMessage = null)
    }

    // ==========================================
    // 8. Groups & Friends Operations
    // ==========================================

    fun addGroup(name: String, category: String, coverColorHex: String, memberIds: List<Long>) {
        viewModelScope.launch {
            val group = GroupEntity(
                name = name,
                category = category,
                coverColorHex = coverColorHex,
                memberIds = memberIds.joinToString(",")
            )
            repository.insertGroup(group)
            closeAddGroup()
        }
    }

    fun addFriend(name: String, phone: String, avatarColorHex: String) {
        viewModelScope.launch {
            val user = UserEntity(
                name = name,
                phone = phone,
                avatarColorHex = avatarColorHex,
                isCurrentUser = false
            )
            repository.insertUser(user)
            closeAddFriend()
        }
    }

    // Dialog handlers
    fun selectGroup(groupId: Long?) { _uiState.value = _uiState.value.copy(selectedGroupId = groupId) }
    fun selectFriend(friendId: Long?) { _uiState.value = _uiState.value.copy(selectedFriendId = friendId) }
    fun selectAccount(accountId: Long?) { _uiState.value = _uiState.value.copy(selectedAccountId = accountId) }

    fun openAddExpense(prefill: ParsedExpenseResult? = null) { _uiState.value = _uiState.value.copy(isAddExpenseOpen = true, expensePrefill = prefill) }
    fun closeAddExpense() { _uiState.value = _uiState.value.copy(isAddExpenseOpen = false, expensePrefill = null) }

    fun openSettleUp(prefill: SettlePrefill? = null) { _uiState.value = _uiState.value.copy(isSettleUpOpen = true, settlePrefill = prefill) }
    fun closeSettleUp() { _uiState.value = _uiState.value.copy(isSettleUpOpen = false, settlePrefill = null) }

    fun openAddGroup() { _uiState.value = _uiState.value.copy(isAddGroupOpen = true) }
    fun closeAddGroup() { _uiState.value = _uiState.value.copy(isAddGroupOpen = false) }

    fun openAddFriend() { _uiState.value = _uiState.value.copy(isAddFriendOpen = true) }
    fun closeAddFriend() { _uiState.value = _uiState.value.copy(isAddFriendOpen = false) }

    fun openAddAccount() { _uiState.value = _uiState.value.copy(isAddAccountOpen = true) }
    fun closeAddAccount() { _uiState.value = _uiState.value.copy(isAddAccountOpen = false) }

    fun openUploadStatement(account: AccountEntity? = null) { _uiState.value = _uiState.value.copy(isUploadStatementOpen = true, selectedStatementAccount = account) }
    fun closeUploadStatement() { _uiState.value = _uiState.value.copy(isUploadStatementOpen = false) }

    fun openSimplifyDebts() { _uiState.value = _uiState.value.copy(isSimplifyDebtsOpen = true) }
    fun closeSimplifyDebts() { _uiState.value = _uiState.value.copy(isSimplifyDebtsOpen = false) }

    // ==========================================
    // 8. Voice-to-Text Gemini Expense Parsing
    // ==========================================

    fun openVoiceExpense(initialTranscript: String? = null) {
        _uiState.value = _uiState.value.copy(
            isVoiceExpenseOpen = true,
            voiceTranscript = initialTranscript ?: "",
            activeParsedVoiceResult = null,
            voiceErrorMessage = null,
            voiceSuccessToast = null
        )
        if (!initialTranscript.isNullOrBlank()) {
            parseVoiceTranscript(initialTranscript)
        }
    }

    fun closeVoiceExpense() {
        _uiState.value = _uiState.value.copy(
            isVoiceExpenseOpen = false,
            isListeningVoice = false,
            isParsingVoice = false,
            activeParsedVoiceResult = null,
            voiceErrorMessage = null
        )
    }

    fun clearVoiceSuccessToast() {
        _uiState.value = _uiState.value.copy(voiceSuccessToast = null)
    }

    fun updateVoiceTranscript(transcript: String) {
        _uiState.value = _uiState.value.copy(voiceTranscript = transcript, voiceErrorMessage = null)
    }

    fun setListeningVoice(isListening: Boolean) {
        _uiState.value = _uiState.value.copy(isListeningVoice = isListening)
    }

    fun parseVoiceTranscript(
        spokenText: String,
        autoSave: Boolean = false,
        targetGroupId: Long? = null,
        payerId: Long? = null
    ) {
        val trimmed = spokenText.trim()
        if (trimmed.isBlank()) {
            _uiState.value = _uiState.value.copy(voiceErrorMessage = "Please speak an expense description first.")
            return
        }

        _uiState.value = _uiState.value.copy(
            voiceTranscript = trimmed,
            isParsingVoice = true,
            voiceErrorMessage = null,
            activeParsedVoiceResult = null
        )

        viewModelScope.launch {
            try {
                val parsedResult = MessageParserEngine.parseVoiceTranscript(trimmed)
                if (parsedResult.amount > 0.0) {
                    _uiState.value = _uiState.value.copy(
                        isParsingVoice = false,
                        activeParsedVoiceResult = parsedResult
                    )

                    if (autoSave) {
                        saveVoiceExpenseToDatabase(
                            parsedResult = parsedResult,
                            targetGroupId = targetGroupId,
                            payerId = payerId
                        )
                    }
                } else {
                    _uiState.value = _uiState.value.copy(
                        isParsingVoice = false,
                        voiceErrorMessage = "Could not detect expense amount. Try saying 'Spent 20 dollars on lunch'."
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isParsingVoice = false,
                    voiceErrorMessage = "Error parsing voice: ${e.localizedMessage ?: "Unknown error"}"
                )
            }
        }
    }

    fun saveVoiceExpenseToDatabase(
        parsedResult: ParsedExpenseResult,
        targetGroupId: Long? = null,
        payerId: Long? = null
    ) {
        val myId = payerId ?: _uiState.value.currentUserId
        val users = _uiState.value.users

        viewModelScope.launch {
            val splitMap = mutableMapOf<Long, Double>()

            if (targetGroupId != null) {
                val group = _uiState.value.groups.find { it.id == targetGroupId }
                val members = group?.memberIds?.split(",")?.mapNotNull { it.trim().toLongOrNull() } ?: listOf(myId)
                val perPerson = parsedResult.amount / members.size.coerceAtLeast(1)
                members.forEach { splitMap[it] = perPerson }
            } else {
                // If participants specified in voice
                if (parsedResult.participantNames.isNotEmpty()) {
                    val matchedUsers = users.filter { u ->
                        parsedResult.participantNames.any { it.equals(u.name, ignoreCase = true) || u.name.contains(it, ignoreCase = true) }
                    }
                    val allSplitUsers = (matchedUsers.map { it.id } + listOf(myId)).distinct()
                    val perPerson = parsedResult.amount / allSplitUsers.size.coerceAtLeast(1)
                    allSplitUsers.forEach { splitMap[it] = perPerson }
                } else if (parsedResult.suggestedSplitCount > 1) {
                    val splitCount = parsedResult.suggestedSplitCount.coerceIn(1, 10)
                    val perPerson = parsedResult.amount / splitCount
                    splitMap[myId] = perPerson
                    val otherUsers = users.filter { it.id != myId }.take(splitCount - 1)
                    otherUsers.forEach { splitMap[it.id] = perPerson }
                } else {
                    // Personal expense
                    splitMap[myId] = parsedResult.amount
                }
            }

            val expense = ExpenseEntity(
                title = parsedResult.title.ifBlank { "Voice Expense" },
                amount = parsedResult.amount,
                currency = parsedResult.currency,
                category = parsedResult.category,
                groupId = targetGroupId,
                payerId = myId,
                splitType = "EQUAL",
                splitDetailsJson = DebtEngine.formatSplitDetails(splitMap),
                dateMillis = parsedResult.dateMillis,
                notes = parsedResult.notes ?: "Added via Voice Recognition",
                sourceMessage = "Voice: \"${parsedResult.rawMessage}\""
            )

            val expenseId = repository.insertExpense(expense)

            _uiState.value = _uiState.value.copy(
                isVoiceExpenseOpen = false,
                activeParsedVoiceResult = null,
                voiceTranscript = "",
                voiceSuccessToast = "Saved \"${expense.title}\" (${expense.currency}${String.format(java.util.Locale.US, "%.2f", expense.amount)}) to database!"
            )
        }
    }
}
