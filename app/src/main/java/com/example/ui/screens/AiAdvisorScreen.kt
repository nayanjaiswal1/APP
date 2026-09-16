package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.ExpenseEntity
import com.example.data.remote.FmsApiResult
import com.example.data.remote.FmsClientManager
import com.example.data.remote.dto.SendMessageDto
import com.example.ui.components.BackendStatusBanner
import com.example.ui.theme.PositiveGreen
import com.example.ui.theme.PurpleContainer
import com.example.ui.theme.PurplePrimary
import com.example.ui.theme.PurplePrimaryDark
import com.example.ui.theme.TextMuted
import kotlinx.coroutines.launch
import java.util.Locale

data class ChatMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val isUser: Boolean,
    val text: String,
    val timestamp: Long = System.currentTimeMillis()
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiAdvisorScreen(
    expenses: List<ExpenseEntity>,
    clientManager: FmsClientManager,
    onBack: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    var messages by remember {
        mutableStateOf<List<ChatMessage>>(
            listOf(
                ChatMessage(
                    isUser = false,
                    text = "Hello! I am your AI Financial Advisor. You can ask me about your spending patterns, budgeting suggestions, debt settlements, or investment insights. How can I help you today?"
                )
            )
        )
    }

    var inputText by remember { mutableStateOf("") }
    var isSending by remember { mutableStateOf(false) }

    val samplePrompts = listOf(
        "Analyze my monthly spending",
        "How can I cut expenses by 15%?",
        "What is my highest expense category?",
        "Suggest a budget for dining out",
        "Summarize my recent transactions"
    )

    fun sendUserMessage(text: String) {
        if (text.isBlank() || isSending) return
        val query = text.trim()
        val userMsg = ChatMessage(isUser = true, text = query)
        messages = messages + userMsg
        inputText = ""
        isSending = true

        coroutineScope.launch {
            try {
                var answer: String? = null
                val result = clientManager.executeSafely("Ask AI Advisor") { service ->
                    service.sendChatMessage(SendMessageDto(message = query))
                }

                if (result is FmsApiResult.Success && !result.data.response.isNullOrBlank()) {
                    answer = result.data.response
                }

                // Fallback smart analysis if remote service didn't answer or is offline
                if (answer == null) {
                    val totalSpend = expenses.filter { !it.isSettlement }.sumOf { it.amount }
                    val categoryGroup = expenses.filter { !it.isSettlement }
                        .groupBy { it.category }
                        .mapValues { entry -> entry.value.sumOf { it.amount } }
                        .toList()
                        .sortedByDescending { it.second }

                    val topCategory = categoryGroup.firstOrNull()?.first ?: "Food & Dining"
                    val topCatAmount = categoryGroup.firstOrNull()?.second ?: 0.0

                    val baseInsight = when {
                        query.contains("spending", ignoreCase = true) || query.contains("expense", ignoreCase = true) -> {
                            "Based on your recorded transactions, your total spend is $${String.format(Locale.US, "%.2f", totalSpend)}. Your largest spending category is **$topCategory** at $${String.format(Locale.US, "%.2f", topCatAmount)}. Keeping track of smaller daily payments will yield the most immediate savings."
                        }
                        query.contains("cut", ignoreCase = true) || query.contains("save", ignoreCase = true) -> {
                            "To reduce your outflow by 15% ($${String.format(Locale.US, "%.2f", totalSpend * 0.15)}):\n1. Review your recurring subscriptions in the Reminders tab.\n2. Set a strict weekly ceiling on **$topCategory**.\n3. Use group settlements promptly to minimize outstanding balances."
                        }
                        query.contains("category", ignoreCase = true) -> {
                            "Your top spending categories are:\n" + categoryGroup.take(3).joinToString("\n") { (cat, amt) ->
                                "• **$cat**: $${String.format(Locale.US, "%.2f", amt)}"
                            }
                        }
                        else -> {
                            "I reviewed your portfolio and ledger: you have ${expenses.size} tracked transactions totaling $${String.format(Locale.US, "%.2f", totalSpend)}. Maintaining steady cash reserves while investing surplus in diversified index funds is currently recommended."
                        }
                    }

                    val offlinePrefix = when (result) {
                        is FmsApiResult.Unreachable -> {
                            if (result.isRenderColdStart)
                                "⚡ *[Render Cloud Server is waking up (~30s cold start). Using On-Device Financial Intelligence]:*\n\n"
                            else
                                "📡 *[Cloud Server Unreachable — Using On-Device Financial Intelligence]:*\n\n"
                        }
                        is FmsApiResult.Unauthorized ->
                            "🔒 *[Session Expired or Unauthorized — Using On-Device Financial Intelligence]:*\n\n"
                        is FmsApiResult.ServerError ->
                            "⚠️ *[Cloud Server Error — Using On-Device Financial Intelligence]:*\n\n"
                        else -> ""
                    }

                    answer = offlinePrefix + baseInsight
                }

                messages = messages + ChatMessage(isUser = false, text = answer)
            } finally {
                isSending = false
            }
        }
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("ai_advisor_screen")
    ) {
        // Header
        Surface(
            tonalElevation = 2.dp,
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (onBack != null) {
                        IconButton(
                            onClick = onBack,
                            modifier = Modifier.testTag("ai_advisor_back_button")
                        ) {
                            Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                    Surface(
                        shape = CircleShape,
                        color = PurpleContainer,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = PurplePrimaryDark,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "AI Financial Advisor",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(PositiveGreen)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Gemini Financial Intelligence",
                                fontSize = 11.sp,
                                color = TextMuted
                            )
                        }
                    }
                }

                IconButton(
                    onClick = {
                        messages = listOf(
                            ChatMessage(
                                isUser = false,
                                text = "Conversation refreshed. How may I assist you with your finances?"
                            )
                        )
                    }
                ) {
                    Icon(imageVector = Icons.Default.Delete, contentDescription = "Clear Chat", tint = TextMuted)
                }
            }
        }

        // Backend Status Banner
        BackendStatusBanner(
            clientManager = clientManager,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
        )

        // Suggestions row
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(samplePrompts) { prompt ->
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = PurpleContainer.copy(alpha = 0.6f),
                    border = BorderStroke(1.dp, PurplePrimary.copy(alpha = 0.2f)),
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { sendUserMessage(prompt) }
                        .testTag("prompt_chip_${prompt.hashCode()}")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Psychology,
                            contentDescription = null,
                            tint = PurplePrimaryDark,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = prompt, fontSize = 12.sp, color = PurplePrimaryDark, fontWeight = FontWeight.Medium)
                    }
                }
            }
        }

        // Chat Message List
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(messages, key = { it.id }) { msg ->
                if (msg.isUser) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Surface(
                            shape = RoundedCornerShape(topStart = 18.dp, topEnd = 4.dp, bottomStart = 18.dp, bottomEnd = 18.dp),
                            color = PurplePrimary,
                            modifier = Modifier.widthIn(max = 300.dp)
                        ) {
                            Text(
                                text = msg.text,
                                color = Color.White,
                                fontSize = 14.sp,
                                modifier = Modifier.padding(14.dp)
                            )
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Start
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = PurpleContainer,
                            modifier = Modifier
                                .size(32.dp)
                                .align(Alignment.Top)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = PurplePrimaryDark,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            shape = RoundedCornerShape(topStart = 4.dp, topEnd = 18.dp, bottomStart = 18.dp, bottomEnd = 18.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant),
                            modifier = Modifier.widthIn(max = 320.dp)
                        ) {
                            Text(
                                text = msg.text,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 14.sp,
                                lineHeight = 20.sp,
                                modifier = Modifier.padding(14.dp)
                            )
                        }
                    }
                }
            }

            if (isSending) {
                item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = PurplePrimary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "Analyzing financial intelligence...", fontSize = 12.sp, color = TextMuted)
                    }
                }
            }
        }

        // Input row
        Surface(
            tonalElevation = 4.dp,
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    placeholder = { Text("Ask your financial advisor...") },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("ai_advisor_input_field"),
                    shape = RoundedCornerShape(24.dp),
                    singleLine = true
                )

                Spacer(modifier = Modifier.width(8.dp))

                IconButton(
                    onClick = { sendUserMessage(inputText) },
                    enabled = inputText.isNotBlank() && !isSending,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(if (inputText.isNotBlank()) PurplePrimary else PurpleContainer)
                        .testTag("ai_advisor_send_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Send",
                        tint = if (inputText.isNotBlank()) Color.White else TextMuted
                    )
                }
            }
        }
    }
}
