package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.outlined.PieChart
import androidx.compose.material.icons.automirrored.filled.CallSplit
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.ShowChart
import com.example.ui.components.AddAccountDialog
import com.example.ui.components.AddExpenseBottomSheet
import com.example.ui.components.AddFriendDialog
import com.example.ui.components.AddGroupDialog
import com.example.ui.components.DebtSimplificationDialog
import com.example.ui.components.InvoiceDetailDialog
import com.example.ui.components.SettleUpBottomSheet
import com.example.ui.components.UploadStatementDialog
import com.example.ui.components.VoiceExpenseBottomSheet
import com.example.ui.screens.AccountsScreen
import com.example.ui.screens.ActivityScreen
import com.example.ui.screens.AiAdvisorScreen
import com.example.ui.screens.BudgetsScreen
import com.example.ui.screens.CategoryExpenseScreen
import com.example.ui.screens.GroupsScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.InvestmentsScreen
import com.example.ui.screens.LendBorrowScreen
import com.example.ui.screens.MessageParserScreen
import com.example.ui.screens.RemindersScreen
import com.example.ui.screens.ServicesHubScreen
import com.example.ui.theme.PurpleContainer
import com.example.ui.theme.PurplePrimary
import com.example.ui.theme.PurplePrimaryDark
import com.example.ui.theme.SplitExpenseTheme
import com.example.ui.theme.TextMuted
import com.example.ui.viewmodel.SplitExpenseViewModel
import androidx.compose.runtime.LaunchedEffect

enum class NavigationTab(
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
) {
    HOME("Friends", Icons.Filled.Person, Icons.Outlined.Person, "nav_friends"),
    GROUPS("Groups", Icons.Filled.Group, Icons.Outlined.Group, "nav_groups"),
    HUB("Hub", Icons.Filled.Dashboard, Icons.Outlined.Dashboard, "nav_hub"),
    ANALYTICS("Analytics", Icons.Filled.PieChart, Icons.Outlined.PieChart, "nav_analytics"),
    PARSER("Smart Parse", Icons.Filled.AutoAwesome, Icons.Outlined.AutoAwesome, "nav_parser"),

    // Sub-screens for complete backend API coverage
    BUDGETS("Budgets", Icons.Filled.AccountBalanceWallet, Icons.Outlined.AccountBalanceWallet, "nav_budgets"),
    LEND_BORROW("Lend & Borrow", Icons.AutoMirrored.Filled.CallSplit, Icons.AutoMirrored.Filled.CallSplit, "nav_lend_borrow"),
    REMINDERS("Reminders", Icons.Filled.NotificationsActive, Icons.Outlined.NotificationsActive, "nav_reminders"),
    INVESTMENTS("Investments", Icons.Filled.ShowChart, Icons.Outlined.ShowChart, "nav_investments"),
    AI_ADVISOR("AI Advisor", Icons.Filled.AutoAwesome, Icons.Outlined.AutoAwesome, "nav_ai_advisor"),
    ACCOUNTS("Accounts", Icons.Filled.AccountBalance, Icons.Outlined.AccountBalance, "nav_accounts")
}

class MainActivity : ComponentActivity() {

    private val viewModel: SplitExpenseViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SplitExpenseTheme {
                SplitExpenseApp(viewModel)
            }
        }
    }
}

@Composable
fun SplitExpenseApp(viewModel: SplitExpenseViewModel) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    var currentTab by remember { mutableStateOf(NavigationTab.HOME) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            val backendStatus by viewModel.clientManager.status.collectAsState()
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "SplitExpense AI",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "Finance Management System",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMuted
                    )
                }

                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = when (backendStatus.connectionState) {
                        com.example.data.remote.FmsConnectionState.CONNECTED -> com.example.ui.theme.PositiveGreenBg
                        com.example.data.remote.FmsConnectionState.ERROR -> com.example.ui.theme.NegativeRedBg
                        else -> PurpleContainer
                    },
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .clickable { viewModel.openBackendSettings() }
                        .testTag("top_backend_status_button")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = when (backendStatus.connectionState) {
                                com.example.data.remote.FmsConnectionState.CONNECTED -> Icons.Default.CloudDone
                                com.example.data.remote.FmsConnectionState.ERROR -> Icons.Default.CloudOff
                                else -> Icons.Default.Cloud
                            },
                            contentDescription = null,
                            tint = when (backendStatus.connectionState) {
                                com.example.data.remote.FmsConnectionState.CONNECTED -> com.example.ui.theme.PositiveGreen
                                com.example.data.remote.FmsConnectionState.ERROR -> com.example.ui.theme.NegativeRed
                                else -> PurplePrimary
                            },
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (backendStatus.connectionState == com.example.data.remote.FmsConnectionState.CONNECTED) "FMS Sync" else "FMS Cloud",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = when (backendStatus.connectionState) {
                                com.example.data.remote.FmsConnectionState.CONNECTED -> com.example.ui.theme.PositiveGreen
                                com.example.data.remote.FmsConnectionState.ERROR -> com.example.ui.theme.NegativeRed
                                else -> PurplePrimaryDark
                            }
                        )
                    }
                }
            }
        },
        bottomBar = {
            val mainTabs = listOf(
                NavigationTab.HOME,
                NavigationTab.GROUPS,
                NavigationTab.HUB,
                NavigationTab.ANALYTICS,
                NavigationTab.PARSER
            )
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 2.dp
            ) {
                mainTabs.forEach { tab ->
                    val isSelected = when (tab) {
                        NavigationTab.HUB -> currentTab == NavigationTab.HUB ||
                                currentTab == NavigationTab.BUDGETS ||
                                currentTab == NavigationTab.LEND_BORROW ||
                                currentTab == NavigationTab.REMINDERS ||
                                currentTab == NavigationTab.INVESTMENTS ||
                                currentTab == NavigationTab.AI_ADVISOR ||
                                currentTab == NavigationTab.ACCOUNTS
                        else -> currentTab == tab
                    }
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = { currentTab = tab },
                        icon = {
                            Icon(
                                imageVector = if (isSelected) tab.selectedIcon else tab.unselectedIcon,
                                contentDescription = tab.title,
                                modifier = Modifier.size(24.dp)
                            )
                        },
                        label = {
                            Text(
                                text = tab.title,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = PurplePrimaryDark,
                            selectedTextColor = PurplePrimaryDark,
                            unselectedIconColor = TextMuted,
                            unselectedTextColor = TextMuted,
                            indicatorColor = PurpleContainer
                        ),
                        modifier = Modifier.testTag(tab.testTag)
                    )
                }
            }
        },
        floatingActionButton = {
            AnimatedVisibility(
                visible = currentTab == NavigationTab.HOME || currentTab == NavigationTab.GROUPS,
                enter = fadeIn() + slideInVertically { it / 2 },
                exit = fadeOut() + slideOutVertically { it / 2 }
            ) {
                FloatingActionButton(
                    onClick = { viewModel.openAddExpense() },
                    containerColor = PurplePrimary,
                    contentColor = Color.White,
                    shape = CircleShape,
                    elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 3.dp),
                    modifier = Modifier.testTag("fab_add_expense")
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add Expense",
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentTab) {
                NavigationTab.HOME -> {
                    HomeScreen(
                        users = uiState.users,
                        groups = uiState.groups,
                        expenses = uiState.expenses,
                        currentUserId = uiState.currentUserId,
                        netBalance = uiState.overallNetBalance,
                        totalYouAreOwed = uiState.totalYouAreOwed,
                        totalYouOwe = uiState.totalYouOwe,
                        friendSummaries = uiState.friendSummaries,
                        onAddExpenseClick = { viewModel.openAddExpense() },
                        onVoiceExpenseClick = { viewModel.openVoiceExpense() },
                        onParseMessageClick = { currentTab = NavigationTab.PARSER },
                        onSettleUpClick = { prefill -> viewModel.openSettleUp(prefill) },
                        onSimplifyDebtsClick = { viewModel.openSimplifyDebts() },
                        onAddFriendClick = { viewModel.openAddFriend() },
                        onDeleteExpense = { id -> viewModel.deleteExpense(id) },
                        onNavigateToBudgets = { currentTab = NavigationTab.BUDGETS },
                        onNavigateToLendBorrow = { currentTab = NavigationTab.LEND_BORROW },
                        onNavigateToReminders = { currentTab = NavigationTab.REMINDERS },
                        onNavigateToInvestments = { currentTab = NavigationTab.INVESTMENTS },
                        onNavigateToAiAdvisor = { currentTab = NavigationTab.AI_ADVISOR },
                        onNavigateToMoreHub = { currentTab = NavigationTab.HUB }
                    )
                }
                NavigationTab.GROUPS -> {
                    GroupsScreen(
                        groups = uiState.groups,
                        users = uiState.users,
                        expenses = uiState.expenses,
                        currentUserId = uiState.currentUserId,
                        groupSummaries = uiState.groupSummaries,
                        selectedGroupId = uiState.selectedGroupId,
                        onSelectGroup = { id -> viewModel.selectGroup(id) },
                        onCreateGroupClick = { viewModel.openAddGroup() },
                        onAddExpenseInGroup = { viewModel.openAddExpense() },
                        onSimplifyGroupDebts = { viewModel.openSimplifyDebts() },
                        onDeleteExpense = { id -> viewModel.deleteExpense(id) }
                    )
                }
                NavigationTab.HUB -> {
                    ServicesHubScreen(
                        clientManager = viewModel.clientManager,
                        onNavigateToBudgets = { currentTab = NavigationTab.BUDGETS },
                        onNavigateToLendBorrow = { currentTab = NavigationTab.LEND_BORROW },
                        onNavigateToReminders = { currentTab = NavigationTab.REMINDERS },
                        onNavigateToInvestments = { currentTab = NavigationTab.INVESTMENTS },
                        onNavigateToAiAdvisor = { currentTab = NavigationTab.AI_ADVISOR },
                        onNavigateToAccounts = { currentTab = NavigationTab.ACCOUNTS },
                        onNavigateToAnalytics = { currentTab = NavigationTab.ANALYTICS },
                        onNavigateToParser = { currentTab = NavigationTab.PARSER },
                        onOpenBackendSettings = { viewModel.openBackendSettings() },
                        onOpenSimplifyDebts = { viewModel.openSimplifyDebts() }
                    )
                }
                NavigationTab.BUDGETS -> {
                    BudgetsScreen(
                        expenses = uiState.expenses,
                        clientManager = viewModel.clientManager,
                        onBack = { currentTab = NavigationTab.HUB }
                    )
                }
                NavigationTab.LEND_BORROW -> {
                    LendBorrowScreen(
                        clientManager = viewModel.clientManager,
                        onBack = { currentTab = NavigationTab.HUB }
                    )
                }
                NavigationTab.REMINDERS -> {
                    RemindersScreen(
                        clientManager = viewModel.clientManager,
                        onBack = { currentTab = NavigationTab.HUB }
                    )
                }
                NavigationTab.INVESTMENTS -> {
                    InvestmentsScreen(
                        clientManager = viewModel.clientManager,
                        onBack = { currentTab = NavigationTab.HUB }
                    )
                }
                NavigationTab.AI_ADVISOR -> {
                    AiAdvisorScreen(
                        expenses = uiState.expenses,
                        clientManager = viewModel.clientManager,
                        onBack = { currentTab = NavigationTab.HUB }
                    )
                }
                NavigationTab.ACCOUNTS -> {
                    AccountsScreen(
                        accounts = uiState.accounts,
                        statements = uiState.statements,
                        expenses = uiState.expenses,
                        selectedAccountId = uiState.selectedAccountId,
                        onSelectAccount = { id -> viewModel.selectAccount(id) },
                        onAddAccountClick = { viewModel.openAddAccount() },
                        onUploadStatementClick = { acc -> viewModel.openUploadStatement(acc) },
                        onDeleteAccount = { id -> viewModel.deleteAccount(id) }
                    )
                }
                NavigationTab.PARSER -> {
                    MessageParserScreen(
                        inputText = uiState.messageInputText,
                        isParsing = uiState.isParsingMessage,
                        activeResult = uiState.activeParsedResult,
                        parsedHistory = uiState.parsedMessages,
                        groups = uiState.groups,
                        batchInputText = uiState.batchInputText,
                        isParsingBatch = uiState.isParsingBatch,
                        batchParsedResults = uiState.batchParsedResults,
                        selectedBatchIndexes = uiState.selectedBatchIndexes,
                        invoiceInputText = uiState.invoiceInputText,
                        isParsingInvoice = uiState.isParsingInvoice,
                        activeParsedInvoice = uiState.activeParsedInvoice,
                        invoices = uiState.invoices,
                        deviceSmsList = uiState.deviceSmsList,
                        isScanningDeviceSms = uiState.isScanningDeviceSms,
                        hasSmsPermission = uiState.hasSmsPermission,
                        selectedDeviceSmsIds = uiState.selectedDeviceSmsIds,
                        smsSyncSuccessMessage = uiState.smsSyncSuccessMessage,
                        onInputChange = { text -> viewModel.onMessageInputChange(text) },
                        onParseClick = { viewModel.parseCurrentMessage() },
                        onSelectTemplate = { template -> viewModel.selectSampleTemplate(template) },
                        onClearResult = { viewModel.clearParsedResult() },
                        onConvertToExpense = { result -> viewModel.convertParsedResultToExpense(result) },
                        onBatchInputChange = { text -> viewModel.onBatchInputChange(text) },
                        onParseBatchClick = { viewModel.parseBatchNotifications() },
                        onSelectBatchTemplate = { template -> viewModel.selectSampleBatchTemplate(template) },
                        onToggleBatchIndex = { idx -> viewModel.toggleBatchItemSelection(idx) },
                        onSelectAllBatch = { selectAll -> viewModel.selectAllBatchItems(selectAll) },
                        onAddSelectedBatchToExpenses = { groupId -> viewModel.addSelectedBatchToExpenses(targetGroupId = groupId) },
                        onInvoiceInputChange = { text -> viewModel.onInvoiceInputChange(text) },
                        onSetInvoiceBitmap = { bmp -> viewModel.setInvoiceBitmap(bmp) },
                        onParseInvoiceClick = { viewModel.parseAndTranslateInvoice() },
                        onSelectSampleInvoice = { sample -> viewModel.selectSampleInvoice(sample) },
                        onSaveInvoiceAndAddToExpense = { inv, groupId -> viewModel.saveParsedInvoiceAndAddToExpense(inv, splitWithGroupId = groupId) },
                        onOpenInvoiceDetail = { inv -> viewModel.openInvoiceDetail(inv) },
                        onScanDeviceSms = { viewModel.scanDeviceSms(context) },
                        onSetSmsPermissionGranted = { granted -> viewModel.setSmsPermissionGranted(granted, context) },
                        onToggleDeviceSmsSelection = { id -> viewModel.toggleDeviceSmsSelection(id) },
                        onSelectAllDeviceSms = { selectAll -> viewModel.selectAllDeviceSms(selectAll) },
                        onImportSelectedDeviceSms = { groupId -> viewModel.importSelectedDeviceSms(targetGroupId = groupId) },
                        onLoadSimulatedDeviceSms = { viewModel.loadSimulatedDeviceSms() },
                        onClearSmsSyncMessage = { viewModel.clearSmsSyncMessage() }
                    )
                }
                NavigationTab.ANALYTICS -> {
                    CategoryExpenseScreen(
                        expenses = uiState.expenses,
                        users = uiState.users,
                        groups = uiState.groups,
                        currentUserId = uiState.currentUserId,
                        onDeleteExpense = { id -> viewModel.deleteExpense(id) }
                    )
                }
            }
        }
    }

    // Modal Dialogs & Sheets
    if (uiState.isAddExpenseOpen) {
        AddExpenseBottomSheet(
            users = uiState.users,
            groups = uiState.groups,
            currentUserId = uiState.currentUserId,
            prefill = uiState.expensePrefill,
            onDismiss = { viewModel.closeAddExpense() },
            onSaveExpense = { title, amount, currency, category, groupId, payerId, splitType, splitDetails, notes, sourceMessage ->
                viewModel.addExpense(title, amount, currency, category, groupId, payerId, splitType, splitDetails, notes, sourceMessage)
            }
        )
    }

    if (uiState.isSettleUpOpen) {
        SettleUpBottomSheet(
            users = uiState.users,
            groups = uiState.groups,
            currentUserId = uiState.currentUserId,
            prefill = uiState.settlePrefill,
            onDismiss = { viewModel.closeSettleUp() },
            onConfirmSettle = { fromId, toId, amount, groupId, note ->
                viewModel.settleDebt(fromId, toId, amount, groupId, note)
            }
        )
    }

    if (uiState.isSimplifyDebtsOpen) {
        DebtSimplificationDialog(
            transfers = uiState.globalSimplifiedTransfers,
            users = uiState.users,
            currentUserId = uiState.currentUserId,
            onDismiss = { viewModel.closeSimplifyDebts() },
            onSettleTransfer = { fromId, toId, amount ->
                viewModel.settleDebt(fromId, toId, amount)
            }
        )
    }

    if (uiState.isAddGroupOpen) {
        AddGroupDialog(
            users = uiState.users,
            currentUserId = uiState.currentUserId,
            onDismiss = { viewModel.closeAddGroup() },
            onSaveGroup = { name, category, colorHex, memberIds ->
                viewModel.addGroup(name, category, colorHex, memberIds)
            }
        )
    }

    if (uiState.isAddFriendOpen) {
        AddFriendDialog(
            onDismiss = { viewModel.closeAddFriend() },
            onSaveFriend = { name, phone, colorHex ->
                viewModel.addFriend(name, phone, colorHex)
            }
        )
    }

    if (uiState.isAddAccountOpen) {
        AddAccountDialog(
            onDismiss = { viewModel.closeAddAccount() },
            onAddAccount = { name, type, institution, last4, balance, currency, colorHex, iconName ->
                viewModel.addAccount(name, type, institution, last4, balance, currency, colorHex, iconName)
            }
        )
    }

    if (uiState.isUploadStatementOpen) {
        UploadStatementDialog(
            accounts = uiState.accounts,
            groups = uiState.groups,
            selectedAccount = uiState.selectedStatementAccount,
            statementInputText = uiState.statementInputText,
            isParsing = uiState.isParsingStatement,
            parsedTransactions = uiState.parsedStatementTransactions,
            onDismiss = { viewModel.closeUploadStatement() },
            onSelectAccount = { acc -> viewModel.selectAccountForStatement(acc) },
            onInputChange = { text -> viewModel.onStatementInputChange(text) },
            onParseClick = { viewModel.parseCurrentStatement() },
            onSelectSample = { sample -> viewModel.selectSampleStatement(sample) },
            onToggleTransaction = { txId -> viewModel.toggleStatementTxSelection(txId) },
            onSelectAll = { selectAll -> viewModel.selectAllStatementTransactions(selectAll) },
            onImportExpenses = { targetGroupId -> viewModel.importStatementExpenses(targetGroupId) }
        )
    }

    if (uiState.isInvoiceDetailOpen && uiState.selectedInvoiceDetail != null) {
        InvoiceDetailDialog(
            invoice = uiState.selectedInvoiceDetail!!,
            onDismiss = { viewModel.closeInvoiceDetail() },
            onDelete = { id -> viewModel.deleteInvoice(id) }
        )
    }

    if (uiState.isVoiceExpenseOpen) {
        VoiceExpenseBottomSheet(
            transcript = uiState.voiceTranscript,
            isListening = uiState.isListeningVoice,
            isParsing = uiState.isParsingVoice,
            parsedResult = uiState.activeParsedVoiceResult,
            errorMessage = uiState.voiceErrorMessage,
            groups = uiState.groups,
            users = uiState.users,
            currentUserId = uiState.currentUserId,
            onTranscriptChanged = { text -> viewModel.updateVoiceTranscript(text) },
            onStartListening = { viewModel.setListeningVoice(true) },
            onStopListening = { viewModel.setListeningVoice(false) },
            onParseVoice = { spoken, autoSave, groupId -> viewModel.parseVoiceTranscript(spoken, autoSave, groupId) },
            onSaveToDatabase = { result, groupId -> viewModel.saveVoiceExpenseToDatabase(result, groupId) },
            onOpenInFullEditor = { result ->
                viewModel.closeVoiceExpense()
                viewModel.openAddExpense(result)
            },
            onDismiss = { viewModel.closeVoiceExpense() }
        )
    }

    if (uiState.isBackendSettingsOpen) {
        com.example.ui.components.FmsBackendDialog(
            clientManager = viewModel.clientManager,
            syncManager = viewModel.syncManager,
            database = viewModel.appDatabase,
            onDismiss = { viewModel.closeBackendSettings() }
        )
    }

    LaunchedEffect(uiState.voiceSuccessToast) {
        uiState.voiceSuccessToast?.let { msg ->
            android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_LONG).show()
            viewModel.clearVoiceSuccessToast()
        }
    }
}
