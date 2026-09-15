package com.example.ui.components

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.GroupEntity
import com.example.data.local.entity.UserEntity
import com.example.data.parser.ParsedExpenseResult
import com.example.ui.theme.CatFoodBg
import com.example.ui.theme.CatFoodIcon
import com.example.ui.theme.CatTransportBg
import com.example.ui.theme.CatTransportIcon
import com.example.ui.theme.PositiveGreen
import com.example.ui.theme.PositiveGreenBg
import com.example.ui.theme.PurpleContainer
import com.example.ui.theme.PurpleLightSurface
import com.example.ui.theme.PurpleOutlineVariant
import com.example.ui.theme.PurplePrimary
import com.example.ui.theme.PurplePrimaryDark
import com.example.ui.theme.PurpleSecondaryContainer
import com.example.ui.theme.PurpleSurfaceVariant
import com.example.ui.theme.SettledGray
import com.example.ui.theme.TextDark
import com.example.ui.theme.TextMuted
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoiceExpenseBottomSheet(
    transcript: String,
    isListening: Boolean,
    isParsing: Boolean,
    parsedResult: ParsedExpenseResult?,
    errorMessage: String?,
    groups: List<GroupEntity>,
    users: List<UserEntity>,
    currentUserId: Long,
    onTranscriptChanged: (String) -> Unit,
    onStartListening: () -> Unit,
    onStopListening: () -> Unit,
    onParseVoice: (String, Boolean, Long?) -> Unit,
    onSaveToDatabase: (ParsedExpenseResult, Long?) -> Unit,
    onOpenInFullEditor: (ParsedExpenseResult) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val context = LocalContext.current
    var selectedGroupId by remember { mutableStateOf<Long?>(null) }
    var autoSaveOnParse by remember { mutableStateOf(false) }

    // Android Speech Recognition Activity Result Launcher
    val speechLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            val spokenMatches = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            val spokenText = spokenMatches?.firstOrNull() ?: ""
            if (spokenText.isNotBlank()) {
                onTranscriptChanged(spokenText)
                onParseVoice(spokenText, autoSaveOnParse, selectedGroupId)
            }
        }
        onStopListening()
    }

    // Permission launcher for Record Audio
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            startSpeechIntent(speechLauncher, context)
        } else {
            Toast.makeText(context, "Microphone permission required for speech recognition", Toast.LENGTH_SHORT).show()
        }
    }

    fun launchVoiceRecognition() {
        onStartListening()
        try {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak your expense (e.g. 'Spent 20 dollars on lunch')")
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            }
            speechLauncher.launch(intent)
        } catch (e: Exception) {
            // If Google Speech recognizer intent is not supported on container/device, request permission or fallback to inline
            permissionLauncher.launch(android.Manifest.permission.RECORD_AUDIO)
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = PurpleLightSurface,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        modifier = Modifier.testTag("voice_expense_bottom_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    listOf(PurplePrimary, Color(0xFF9C27B0))
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = "Voice Expense",
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Voice Expense AI",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = TextDark
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = PurpleContainer
                            ) {
                                Text(
                                    text = "Gemini",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = PurplePrimaryDark,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            text = "Speak naturally to log & split expenses",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMuted
                        )
                    }
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.testTag("btn_close_voice_sheet")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = TextMuted
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Pulsating Microphone Action Box
            VoiceRecordingHeroBox(
                isListening = isListening,
                isParsing = isParsing,
                onMicClick = { launchVoiceRecognition() }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Quick Voice Presets Chips
            Text(
                text = "OR TAP A VOICE SAMPLE TO TEST:",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = PurplePrimaryDark,
                letterSpacing = 0.8.sp,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))

            val quickVoicePrompts = listOf(
                "Spent 20 dollars on lunch",
                "Paid 45 dollars for dinner with Alex",
                "Bought 85 dollars groceries at Trader Joe's",
                "Uber ride to airport cost 32 dollars",
                "Paid 120 dollars electricity bill",
                "Coffee at Starbucks 6 dollars"
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                quickVoicePrompts.forEach { prompt ->
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = Color.White,
                        border = BorderStroke(1.dp, PurpleOutlineVariant),
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .clickable {
                                onTranscriptChanged(prompt)
                                onParseVoice(prompt, autoSaveOnParse, selectedGroupId)
                            }
                            .testTag("voice_sample_chip_${prompt.take(10)}")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Mic,
                                contentDescription = null,
                                tint = PurplePrimary,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = prompt,
                                style = MaterialTheme.typography.labelMedium,
                                color = TextDark,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Spoken Transcript Input & Parse Button Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, PurpleOutlineVariant)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "Spoken Expense Transcript",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = PurplePrimaryDark
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    OutlinedTextField(
                        value = transcript,
                        onValueChange = { onTranscriptChanged(it) },
                        placeholder = {
                            Text(
                                text = "e.g., 'Spent 20 dollars on lunch with Sarah'",
                                style = MaterialTheme.typography.bodyMedium,
                                color = SettledGray
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_voice_transcript"),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PurplePrimary,
                            unfocusedBorderColor = PurpleOutlineVariant,
                            focusedContainerColor = PurpleLightSurface,
                            unfocusedContainerColor = PurpleLightSurface
                        ),
                        trailingIcon = {
                            IconButton(onClick = { launchVoiceRecognition() }) {
                                Icon(
                                    imageVector = Icons.Default.Mic,
                                    contentDescription = "Speak",
                                    tint = PurplePrimary
                                )
                            }
                        },
                        minLines = 2,
                        maxLines = 3
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = {
                                if (transcript.isNotBlank()) {
                                    onParseVoice(transcript, autoSaveOnParse, selectedGroupId)
                                }
                            },
                            enabled = transcript.isNotBlank() && !isParsing,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = PurplePrimary,
                                contentColor = Color.White
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("btn_parse_spoken_expense")
                        ) {
                            if (isParsing) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Parsing with Gemini...")
                            } else {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Parse with Gemini AI",
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        if (transcript.isNotBlank()) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = PurpleSurfaceVariant,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { onTranscriptChanged("") }
                            ) {
                                Text(
                                    text = "Clear",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = TextMuted,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Error Message Banner if any
            if (errorMessage != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFFFEBEE),
                    border = BorderStroke(1.dp, Color(0xFFFFCDD2)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = errorMessage,
                        color = Color(0xFFC62828),
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            // AI Parsed Expense Result Preview Card
            AnimatedVisibility(
                visible = parsedResult != null,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                if (parsedResult != null) {
                    Column {
                        Spacer(modifier = Modifier.height(16.dp))

                        VoiceParsedResultCard(
                            result = parsedResult,
                            groups = groups,
                            selectedGroupId = selectedGroupId,
                            onSelectGroup = { selectedGroupId = it },
                            onSaveExpense = { onSaveToDatabase(parsedResult, selectedGroupId) },
                            onEditInFull = { onOpenInFullEditor(parsedResult) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun VoiceRecordingHeroBox(
    isListening: Boolean,
    isParsing: Boolean,
    onMicClick: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "mic_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isListening) 1.25f else 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isListening) PurpleSecondaryContainer.copy(alpha = 0.4f) else PurpleSurfaceVariant
        ),
        border = BorderStroke(
            width = if (isListening) 2.dp else 1.dp,
            color = if (isListening) PurplePrimary else PurpleOutlineVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 24.dp, horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Pulsing Mic Circle
            Box(
                modifier = Modifier
                    .size(90.dp)
                    .scale(if (isListening) pulseScale else 1f)
                    .clip(CircleShape)
                    .background(
                        if (isListening) {
                            Brush.radialGradient(
                                listOf(Color(0xFFE91E63), PurplePrimary)
                            )
                        } else {
                            Brush.linearGradient(
                                listOf(PurplePrimary, PurplePrimaryDark)
                            )
                        }
                    )
                    .clickable { onMicClick() }
                    .testTag("btn_voice_mic_record"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isListening) Icons.Default.Mic else Icons.Default.Mic,
                    contentDescription = "Record Spoken Expense",
                    tint = Color.White,
                    modifier = Modifier.size(44.dp)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = if (isListening) "Listening to your voice..." else if (isParsing) "Gemini is parsing expense details..." else "Tap Microphone & Speak Expense",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = if (isListening) Color(0xFFC2185B) else TextDark
            )

            Text(
                text = if (isListening) "e.g., 'Spent 20 dollars on lunch'" else "Say merchant, amount, category, or split participants",
                style = MaterialTheme.typography.bodySmall,
                color = TextMuted,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            // Animated Waveform when listening
            if (isListening) {
                Spacer(modifier = Modifier.height(12.dp))
                SpeechWaveformVisualizer()
            }
        }
    }
}

@Composable
private fun SpeechWaveformVisualizer() {
    val infiniteTransition = rememberInfiniteTransition(label = "waveform")
    val bar1Height by infiniteTransition.animateFloat(
        initialValue = 6f,
        targetValue = 24f,
        animationSpec = infiniteRepeatable(tween(300, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "b1"
    )
    val bar2Height by infiniteTransition.animateFloat(
        initialValue = 18f,
        targetValue = 8f,
        animationSpec = infiniteRepeatable(tween(250, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "b2"
    )
    val bar3Height by infiniteTransition.animateFloat(
        initialValue = 10f,
        targetValue = 30f,
        animationSpec = infiniteRepeatable(tween(380, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "b3"
    )
    val bar4Height by infiniteTransition.animateFloat(
        initialValue = 26f,
        targetValue = 12f,
        animationSpec = infiniteRepeatable(tween(290, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "b4"
    )
    val bar5Height by infiniteTransition.animateFloat(
        initialValue = 8f,
        targetValue = 22f,
        animationSpec = infiniteRepeatable(tween(340, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "b5"
    )

    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.height(32.dp)
    ) {
        listOf(bar1Height, bar2Height, bar3Height, bar4Height, bar5Height, bar2Height, bar1Height).forEach { height ->
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(height.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(PurplePrimary)
            )
        }
    }
}

@Composable
private fun VoiceParsedResultCard(
    result: ParsedExpenseResult,
    groups: List<GroupEntity>,
    selectedGroupId: Long?,
    onSelectGroup: (Long?) -> Unit,
    onSaveExpense: () -> Unit,
    onEditInFull: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("card_voice_parsed_result"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.5.dp, PositiveGreen.copy(alpha = 0.5f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            // Header: Success AI Extraction Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = PositiveGreenBg
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = PositiveGreen,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Parsed with Gemini 3.5 Flash",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = PositiveGreen
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = PurpleContainer
                ) {
                    Text(
                        text = result.category,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = PurplePrimaryDark,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Main Details Row: Title, Amount, Merchant
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = result.title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = TextDark
                    )
                    if (result.merchant.isNotBlank()) {
                        Text(
                            text = "Merchant: ${result.merchant}",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMuted
                        )
                    }
                    if (result.notes != null) {
                        Text(
                            text = result.notes,
                            style = MaterialTheme.typography.bodySmall,
                            color = SettledGray
                        )
                    }
                }

                Text(
                    text = "${result.currency}${String.format(Locale.US, "%.2f", result.amount)}",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = PositiveGreen
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Group / Split Option
            Text(
                text = "ASSIGN TO / SPLIT WITH:",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = PurplePrimaryDark,
                letterSpacing = 0.8.sp
            )
            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Personal Expense chip
                FilterChip(
                    selected = selectedGroupId == null,
                    onClick = { onSelectGroup(null) },
                    label = { Text("Personal (You paid)") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = PurpleContainer,
                        selectedLabelColor = PurplePrimaryDark
                    )
                )

                // Group Chips
                groups.forEach { grp ->
                    FilterChip(
                        selected = selectedGroupId == grp.id,
                        onClick = { onSelectGroup(grp.id) },
                        label = { Text(grp.name) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Group,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = PurpleContainer,
                            selectedLabelColor = PurplePrimaryDark
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // 1-Tap Save to Database Button
                Button(
                    onClick = onSaveExpense,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PositiveGreen,
                        contentColor = Color.White
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("btn_save_voice_expense_db")
                ) {
                    Icon(
                        imageVector = Icons.Default.Save,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Save to Database",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.labelLarge
                    )
                }

                // Edit in full form
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = PurpleSurfaceVariant,
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .clickable { onEditInFull() }
                        .testTag("btn_edit_voice_in_form")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit Details",
                            tint = PurplePrimaryDark,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Edit",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = PurplePrimaryDark
                        )
                    }
                }
            }
        }
    }
}

private fun startSpeechIntent(
    launcher: androidx.activity.result.ActivityResultLauncher<Intent>,
    context: android.content.Context
) {
    try {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak your expense (e.g. 'Spent 20 dollars on lunch')")
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }
        launcher.launch(intent)
    } catch (e: Exception) {
        Toast.makeText(context, "Speech recognizer is initializing", Toast.LENGTH_SHORT).show()
    }
}
