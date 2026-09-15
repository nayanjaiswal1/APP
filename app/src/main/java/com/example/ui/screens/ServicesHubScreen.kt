package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CallSplit
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.CurrencyExchange
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.remote.FmsClientManager
import com.example.data.remote.FmsConnectionState
import com.example.ui.theme.NegativeRed
import com.example.ui.theme.PositiveGreen
import com.example.ui.theme.PositiveGreenBg
import com.example.ui.theme.PurpleContainer
import com.example.ui.theme.PurplePrimary
import com.example.ui.theme.PurplePrimaryDark
import com.example.ui.theme.TextMuted

data class HubServiceItem(
    val title: String,
    val description: String,
    val icon: ImageVector,
    val badge: String? = null,
    val iconBgColor: Color,
    val iconTint: Color,
    val testTag: String,
    val onClick: () -> Unit
)

@Composable
fun ServicesHubScreen(
    clientManager: FmsClientManager,
    onNavigateToBudgets: () -> Unit,
    onNavigateToLendBorrow: () -> Unit,
    onNavigateToReminders: () -> Unit,
    onNavigateToInvestments: () -> Unit,
    onNavigateToAiAdvisor: () -> Unit,
    onNavigateToAccounts: () -> Unit,
    onNavigateToAnalytics: () -> Unit,
    onNavigateToParser: () -> Unit,
    onOpenBackendSettings: () -> Unit,
    onOpenSimplifyDebts: () -> Unit,
    modifier: Modifier = Modifier
) {
    val backendStatus by clientManager.status.collectAsState()

    val services = listOf(
        HubServiceItem(
            title = "Budgets & Limits",
            description = "Monthly limits & category overspending alerts",
            icon = Icons.Default.AccountBalanceWallet,
            badge = "Active",
            iconBgColor = PurpleContainer,
            iconTint = PurplePrimaryDark,
            testTag = "hub_item_budgets",
            onClick = onNavigateToBudgets
        ),
        HubServiceItem(
            title = "Lend & Borrow",
            description = "Track money given/taken with due dates",
            icon = Icons.AutoMirrored.Filled.CallSplit,
            badge = "2 Pending",
            iconBgColor = PositiveGreenBg,
            iconTint = PositiveGreen,
            testTag = "hub_item_lend_borrow",
            onClick = onNavigateToLendBorrow
        ),
        HubServiceItem(
            title = "Bills & Reminders",
            description = "Upcoming subscriptions, credit cards & utilities",
            icon = Icons.Default.NotificationsActive,
            badge = "Upcoming",
            iconBgColor = Color(0xFFFFF3E0),
            iconTint = Color(0xFFF57C00),
            testTag = "hub_item_reminders",
            onClick = onNavigateToReminders
        ),
        HubServiceItem(
            title = "Investments",
            description = "Stocks, crypto, mutual funds & net returns",
            icon = Icons.Default.ShowChart,
            badge = "Portfolio",
            iconBgColor = Color(0xFFE8F5E9),
            iconTint = Color(0xFF2E7D32),
            testTag = "hub_item_investments",
            onClick = onNavigateToInvestments
        ),
        HubServiceItem(
            title = "AI Advisor",
            description = "Personalized financial analysis & savings advice",
            icon = Icons.Default.AutoAwesome,
            badge = "Gemini AI",
            iconBgColor = PurpleContainer,
            iconTint = PurplePrimary,
            testTag = "hub_item_ai_advisor",
            onClick = onNavigateToAiAdvisor
        ),
        HubServiceItem(
            title = "Bank Accounts",
            description = "Checking, savings, credit cards & statements",
            icon = Icons.Default.AccountBalance,
            badge = null,
            iconBgColor = Color(0xFFE3F2FD),
            iconTint = Color(0xFF1976D2),
            testTag = "hub_item_accounts",
            onClick = onNavigateToAccounts
        ),
        HubServiceItem(
            title = "Spending Analytics",
            description = "Category breakdowns & monthly visual charts",
            icon = Icons.Default.PieChart,
            badge = null,
            iconBgColor = Color(0xFFEDE7F6),
            iconTint = PurplePrimaryDark,
            testTag = "hub_item_analytics",
            onClick = onNavigateToAnalytics
        ),
        HubServiceItem(
            title = "Smart SMS Parser",
            description = "Scan device SMS & receipts to extract expenses",
            icon = Icons.Default.ReceiptLong,
            badge = "AI Camera",
            iconBgColor = Color(0xFFFBE9E7),
            iconTint = Color(0xFFD84315),
            testTag = "hub_item_parser",
            onClick = onNavigateToParser
        ),
        HubServiceItem(
            title = "Simplify Debts",
            description = "Algorithmically minimize pairwise group debt transfers",
            icon = Icons.Default.CurrencyExchange,
            badge = "Auto Settle",
            iconBgColor = PositiveGreenBg,
            iconTint = PositiveGreen,
            testTag = "hub_item_simplify_debts",
            onClick = onOpenSimplifyDebts
        )
    )

    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .testTag("services_hub_screen"),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item(span = { GridItemSpan(2) }) {
            Spacer(modifier = Modifier.height(4.dp))
            Column {
                Text(
                    text = "Financial Hub & Services",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "All integrated financial tools and management modules",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted
                )
            }
        }

        // FMS Backend Banner
        item(span = { GridItemSpan(2) }) {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (backendStatus.connectionState == FmsConnectionState.CONNECTED) PositiveGreenBg else PurpleContainer
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .clickable { onOpenBackendSettings() }
                    .testTag("hub_fms_status_banner")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = Color.White,
                            modifier = Modifier.size(38.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = if (backendStatus.connectionState == FmsConnectionState.CONNECTED) Icons.Default.CloudDone else Icons.Default.CloudOff,
                                    contentDescription = null,
                                    tint = if (backendStatus.connectionState == FmsConnectionState.CONNECTED) PositiveGreen else PurplePrimary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "FMS Cloud Sync: ${if (backendStatus.connectionState == FmsConnectionState.CONNECTED) "Connected" else "Configurable"}",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (backendStatus.connectionState == FmsConnectionState.CONNECTED) PositiveGreen else PurplePrimaryDark
                            )
                            Text(
                                text = backendStatus.baseUrl,
                                fontSize = 11.sp,
                                color = TextMuted
                            )
                        }
                    }

                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = TextMuted
                    )
                }
            }
        }

        items(services) { service ->
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .clickable { service.onClick() }
                    .testTag(service.testTag)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = service.iconBgColor,
                            modifier = Modifier.size(42.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = service.icon,
                                    contentDescription = null,
                                    tint = service.iconTint,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }

                        if (service.badge != null) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = PurpleContainer
                            ) {
                                Text(
                                    text = service.badge,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = PurplePrimaryDark,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }

                    Column {
                        Text(
                            text = service.title,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = service.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMuted,
                            maxLines = 2,
                            fontSize = 11.sp,
                            lineHeight = 14.sp
                        )
                    }
                }
            }
        }

        item(span = { GridItemSpan(2) }) {
            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}
