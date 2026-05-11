package com.dkgs.innerpulse.presentation.wellness.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.window.Dialog
import com.dkgs.innerpulse.data.repository.MoodDayAggregate
import com.dkgs.innerpulse.data.local.entity.JournalEntry
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.dkgs.innerpulse.presentation.wellness.*
import com.dkgs.innerpulse.domain.model.Emotion
import com.dkgs.innerpulse.domain.model.MeditationItem
import com.dkgs.innerpulse.domain.model.ActiveTimer
import com.dkgs.innerpulse.ui.theme.*
import com.dkgs.innerpulse.ui.components.*
import androidx.compose.foundation.lazy.staggeredgrid.*

// ═══════════════════════════════════════════════════════════════════════
// WELLNESS SCREEN
// ═══════════════════════════════════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WellnessScreen(
    viewModel: WellnessViewModel,
    onBack: () -> Unit = {},
    onMeditationClick: (String) -> Unit = {},
    onJournalClick: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    // Runtime RECORD_AUDIO permission
    var hasAudioPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasAudioPermission = granted
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.backgroundGradient)
    ) {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                BrandedTopBar(
                    painter = androidx.compose.ui.res.painterResource(id = com.dkgs.innerpulse.R.drawable.logo_ring),
                    title = "Wellness Hub",
                    modifier = Modifier.statusBarsPadding()
                )
            }
        ) { paddingValues ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // ── HERO: EMOTION CHECK-IN ──
                item {
                    NeonGlassCard(
                        glowColor = NeonPink,
                        cornerRadius = 32.dp
                    ) {
                        Column {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(CircleShape)
                                        .background(NeonPink.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Face,
                                        contentDescription = null,
                                        tint = NeonPink,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                                Column {
                                    Text(
                                        text = "How are you feeling?",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "Sync your mind with your body",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(20.dp))

                            // Horizontal Emotion Picker (Premium Pills)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                uiState.emotions.forEach { emotion ->
                                    val isSelected = uiState.selectedEmotion?.id == emotion.id
                                    EmotionPill(
                                        emotion = emotion,
                                        isSelected = isSelected,
                                        onClick = { viewModel.selectEmotion(emotion) }
                                    )
                                }
                            }
                        }
                    }
                }

                // ── BENTO GRID: METRICS & SCORE ──
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Wellness Score Tile (Square-ish)
                        BentoScoreTile(
                            modifier = Modifier.weight(1f),
                            score = uiState.wellnessScore
                        )

                        // Quick Action: Journal Tile
                        BentoActionTile(
                            modifier = Modifier.weight(1f),
                            title = "Journal",
                            subtitle = "Voice log",
                            icon = Icons.Rounded.Mic,
                            color = NeonOrange,
                            onClick = { viewModel.saveJournalEntry("") /* Opens dialog if handled in VM */ }
                        )
                    }
                }

            // Journal Input Dialog
            if (uiState.showJournalDialog) {
                item {
                    JournalInputDialog(
                        uiState = uiState,
                        onDismiss = { viewModel.dismissDialog() },
                        onSave = { msg -> viewModel.saveJournalEntry(msg) },
                        onStartRecording = { viewModel.startRecording() },
                        onStopRecording = { viewModel.stopRecording() },
                        onPlayPreview = { viewModel.playPreview() },
                        onPausePlayback = { viewModel.pausePlayback() },
                        onResumePlayback = { viewModel.resumePlayback() },
                        onSeekTo = { viewModel.seekTo(it) },
                        onStopPlayback = { viewModel.stopPlayback() },
                        onDiscardRecording = { viewModel.discardRecording() },
                        hasAudioPermission = hasAudioPermission,
                        onRequestPermission = {
                            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        }
                    )
                }
            }

                // ── SECTION: MOOD INSIGHTS ──
                item {
                    SectionHeader(title = "Mind State Insights", dotColor = NeonCyan)
                    Spacer(modifier = Modifier.height(12.dp))
                    MoodBentoTile(uiState = uiState, viewModel = viewModel)
                }

                // ── SECTION: MEDITATION GRID ──
                item {
                    SectionHeader(title = "Restorative Practice", dotColor = PrimaryPurple)
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // 2-Column Staggered or Grid for meditations
                    LazyVerticalStaggeredGrid(
                        columns = StaggeredGridCells.Fixed(2),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(420.dp), // Fixed height or adjust based on content
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalItemSpacing = 16.dp,
                        userScrollEnabled = false // Nested scroll handling
                    ) {
                        items(uiState.meditations) { meditation ->
                            BentoMeditationTile(
                                meditation = meditation,
                                isActive = uiState.activeTimer?.meditationId == meditation.id,
                                onClick = {
                                    val category = when (meditation.id) {
                                        "1" -> "morning_calm"
                                        "2" -> "breathing"
                                        "3" -> "sleep"
                                        else -> "morning_calm"
                                    }
                                    onMeditationClick(category)
                                }
                            )
                        }
                    }
                }

                // Active Timer (Floating Bento Overlay or Bottom Item)
                uiState.activeTimer?.let { activeTimer ->
                    item {
                        ActiveTimerBento(
                            timer = activeTimer,
                            formattedTime = viewModel.formatTime(activeTimer.remainingSeconds),
                            onPause = { viewModel.pauseTimer() },
                            onResume = { viewModel.resumeTimer() },
                            onStop = { viewModel.stopTimer() }
                        )
                    }
                }
            }
        }
    }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════
// SECTION HEADER
// ═══════════════════════════════════════════════════════════════════════

@Composable
private fun SectionHeader(title: String, dotColor: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(dotColor)
        )
        Spacer(Modifier.width(10.dp))
        Text(
            title.uppercase(),
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            letterSpacing = 1.sp
        )
    }
}

// ═══════════════════════════════════════════════════════════════════════
// EMOTION GRID
// ═══════════════════════════════════════════════════════════════════════

@Composable
private fun EmotionGrid(
    emotions: List<Emotion>,
    selectedEmotion: String?,
    onSelect: (String) -> Unit
) {
    Column(modifier = Modifier.padding(horizontal = 20.dp)) {
        emotions.chunked(2).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                row.forEach { emotion ->
                    EmotionCard(
                        modifier = Modifier.weight(1f),
                        emotion = emotion,
                        isSelected = emotion.name == selectedEmotion,
                        onSelect = { onSelect(emotion.name) }
                    )
                }
                if (row.size < 2) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

@Composable
private fun EmotionCard(
    modifier: Modifier = Modifier,
    emotion: Emotion,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    val isDark = AppColors.isDark
    val shape = RoundedCornerShape(16.dp)

    val borderColor by animateColorAsState(
        targetValue = if (isSelected) {
            if (isDark) NeonCyan else SkyBlue
        } else {
            if (isDark) AppColors.dividerColor else PremiumGlassBorder
        },
        animationSpec = tween(300),
        label = "emotionBorder"
    )

    val bgBrush = if (isSelected) {
        Brush.horizontalGradient(listOf(SkyBlue, HighlighterGreen))
    } else {
        if (isDark) Brush.verticalGradient(listOf(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.surfaceVariant))
        else Brush.verticalGradient(
            listOf(
                PremiumGlassHighlight,
                PremiumGlassWhite
            )
        )
    }

    Box(
        modifier = modifier
            .shadow(
                elevation = if (isSelected) 10.dp else 6.dp,
                shape = shape,
                ambientColor = if (isSelected) SkyBlue.copy(alpha = 0.35f) else PremiumShadowColor,
                spotColor = if (isSelected) SkyBlue.copy(alpha = 0.35f) else PremiumShadowColor
            )
            .clip(shape)
            .background(bgBrush)
            .then(
                if (!isSelected) Modifier.border(1.dp, borderColor, shape)
                else Modifier
            )
            .clickable { onSelect() }
            .padding(vertical = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = emotion.icon,
                contentDescription = emotion.name,
                modifier = Modifier.size(28.dp),
                tint = if (isSelected) Color.White else if (isDark) NeonCyan else SkyBlue
            )
            Spacer(Modifier.height(8.dp))
            Text(
                emotion.name,
                fontSize = 13.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                color = if (isSelected) Color.White
                    else if (isDark) MaterialTheme.colorScheme.onSurfaceVariant
                    else DarkGrayText
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════
// DROPDOWN EMOTION SELECTOR
// ═══════════════════════════════════════════════════════════════════════

@Composable
private fun EmotionDropdownSelector(
    emotions: List<Emotion>,
    selectedEmotion: String?,
    onSelect: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val displayText = if (selectedEmotion != null) {
        val em = emotions.find { it.name == selectedEmotion }
        em?.name ?: selectedEmotion
    } else {
        "Select Emotion"
    }

    Column(modifier = Modifier.padding(horizontal = 20.dp)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(8.dp, RoundedCornerShape(16.dp), ambientColor = SkyBlue.copy(alpha = 0.15f), spotColor = SkyBlue.copy(alpha = 0.15f))
                .clip(RoundedCornerShape(16.dp))
                .background(
                    Brush.verticalGradient(
                        listOf(
                            PremiumGlassHighlight,
                            PremiumGlassWhite
                        )
                    )
                )
                .border(1.dp, PremiumGlassBorder, RoundedCornerShape(16.dp))
                .clickable { expanded = !expanded }
                .padding(horizontal = 20.dp, vertical = 16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = displayText,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Icon(
                    imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = "Expand",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Dropdown panel
        if (expanded) {
            Spacer(Modifier.height(8.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                PremiumGlassHighlight,
                                PremiumGlassWhite
                            )
                        )
                    )
                    .border(1.dp, PremiumGlassBorder, RoundedCornerShape(16.dp))
            ) {
                emotions.forEachIndexed { index, emotion ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                expanded = false
                                onSelect(emotion.name)
                            }
                            .padding(horizontal = 20.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = emotion.icon,
                            contentDescription = emotion.name,
                            modifier = Modifier.size(24.dp),
                            tint = if (AppColors.isDark) NeonCyan else SkyBlue
                        )
                        Spacer(Modifier.width(14.dp))
                        Text(
                            emotion.name,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    if (index < emotions.lastIndex) {
                        Divider(
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                            modifier = Modifier.padding(horizontal = 14.dp)
                        )
                    }
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════
// JOURNAL INPUT DIALOG
// ═══════════════════════════════════════════════════════════════════════

@Composable
private fun JournalInputDialog(
    uiState: WellnessUiState,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
    onStartRecording: () -> Unit,
    onStopRecording: () -> Unit,
    onPlayPreview: () -> Unit,
    onPausePlayback: () -> Unit,
    onResumePlayback: () -> Unit,
    onSeekTo: (Int) -> Unit,
    onStopPlayback: () -> Unit,
    onDiscardRecording: () -> Unit,
    hasAudioPermission: Boolean,
    onRequestPermission: () -> Unit
) {
    var message by remember { mutableStateOf("") }
    val context = LocalContext.current

    // Pulse animation for recording
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(
                    if (AppColors.isDark) androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.surface)
                    else Brush.verticalGradient(listOf(Color(0xFFF0F8FF), Color.White))
                )
                .border(1.dp, PremiumGlassBorder, RoundedCornerShape(24.dp))
                .padding(24.dp)
        ) {
            Column {
                // Title
                Text(
                    text = "What made you feel ${uiState.dialogEmotion.lowercase()}?",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (AppColors.isDark) MaterialTheme.colorScheme.onSurface else Color(0xFF1A1A1A)
                )
                Spacer(Modifier.height(20.dp))

                // Text Input
                OutlinedTextField(
                    value = message,
                    onValueChange = { message = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    placeholder = {
                        Text(
                            "Write about how you feel...",
                            color = Color(0xFF94A3B8)
                        )
                    },
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = SkyBlue,
                        unfocusedBorderColor = Color(0xFFE2E8F0)
                    )
                )
                Spacer(Modifier.height(16.dp))

                // Audio section label
                Text(
                    "Or record a voice note",
                    fontSize = 13.sp,
                    color = Color(0xFF6B6B6B),
                    fontWeight = FontWeight.Medium
                )
                Spacer(Modifier.height(12.dp))

                // --- Recording indicator ---
                if (uiState.isRecording) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(ErrorRed.copy(alpha = 0.08f))
                            .border(1.dp, ErrorRed.copy(alpha = 0.3f), RoundedCornerShape(14.dp))
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(16.dp)
                                .clip(CircleShape)
                                .background(ErrorRed)
                                .graphicsLayer(alpha = pulseAlpha)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Recording...",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = ErrorRed
                        )
                        Spacer(Modifier.width(12.dp))
                        val m = uiState.recordingSeconds / 60
                        val s = uiState.recordingSeconds % 60
                        Text(
                            "%02d:%02d".format(m, s),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1A1A1A)
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                }

                // --- Unified Record Button ---
                if (!uiState.hasRecording) {
                    val buttonColor by animateColorAsState(
                        targetValue = if (uiState.isRecording) ErrorRed else SkyBlue,
                        label = "buttonColor"
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(buttonColor)
                            .pointerInput(hasAudioPermission) {
                                detectTapGestures(
                                    onPress = {
                                        if (!hasAudioPermission) {
                                            onRequestPermission()
                                        } else {
                                            onStartRecording()
                                            tryAwaitRelease()
                                            onStopRecording()
                                        }
                                    }
                                )
                            }
                            .padding(vertical = 14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Mic,
                                contentDescription = if (uiState.isRecording) "Recording" else "Record",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = when {
                                    !hasAudioPermission -> "Tap to grant permission"
                                    uiState.isRecording -> "Release to stop"
                                    else -> "Hold to record"
                                },
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                    }
                }

                // --- Playback controls ---
                if (uiState.hasRecording) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (AppColors.isDark) MaterialTheme.colorScheme.surfaceVariant else Color(0xFFF1F5F9))
                            .padding(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            // Play/Pause toggle
                            IconButton(
                                onClick = {
                                    if (uiState.isPlayingPreview) onPausePlayback() else {
                                        if (uiState.playbackPositionMs > 0 && uiState.playbackPositionMs < uiState.audioDurationMs)
                                            onResumePlayback()
                                        else
                                            onPlayPreview()
                                    }
                                },
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(SkyBlue)
                            ) {
                                Icon(
                                    if (uiState.isPlayingPreview) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = "Play/Pause",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            Spacer(Modifier.width(8.dp))

                            // Position label
                            val posSec = uiState.playbackPositionMs / 1000
                            val durSec = uiState.audioDurationMs / 1000
                            Text(
                                "%d:%02d / %d:%02d".format(posSec / 60, posSec % 60, durSec / 60, durSec % 60),
                                fontSize = 12.sp,
                                color = Color(0xFF6B6B6B),
                                fontWeight = FontWeight.Medium
                            )
                        }

                        // Seek slider
                        Slider(
                            value = if (uiState.audioDurationMs > 0) uiState.playbackPositionMs.toFloat() / uiState.audioDurationMs else 0f,
                            onValueChange = { frac ->
                                onSeekTo((frac * uiState.audioDurationMs).toInt())
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = SliderDefaults.colors(
                                thumbColor = SkyBlue,
                                activeTrackColor = SkyBlue,
                                inactiveTrackColor = Color(0xFFCBD5E1)
                            )
                        )

                        Spacer(Modifier.height(8.dp))

                        // Record Again button
                        OutlinedButton(
                            onClick = onDiscardRecording,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, ErrorRed.copy(alpha = 0.5f))
                        ) {
                            Icon(
                                Icons.Default.Refresh,
                                contentDescription = "Record Again",
                                tint = ErrorRed,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                "Record Again",
                                color = ErrorRed,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp
                            )
                        }
                    }
                }

                Spacer(Modifier.height(24.dp))

                // Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text("Cancel")
                    }
                    Button(
                        onClick = { onSave(message) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SkyBlue
                        )
                    ) {
                        Text("Save", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════
// JOURNAL SECTION
// ═══════════════════════════════════════════════════════════════════════

@Composable
private fun JournalSection(
    entries: List<JournalEntry>,
    uiState: WellnessUiState,
    onPlayAudio: (String) -> Unit,
    onPauseAudio: () -> Unit,
    onResumeAudio: () -> Unit,
    onSeekAudio: (Int) -> Unit
) {
    Column(modifier = Modifier.padding(horizontal = 20.dp)) {
        SectionHeader(title = "Journal", dotColor = NeonOrange)
        Spacer(Modifier.height(16.dp))

        if (entries.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        Brush.verticalGradient(
                            listOf(PremiumGlassHighlight, PremiumGlassWhite)
                        )
                    )
                    .border(1.dp, PremiumGlassBorder, RoundedCornerShape(16.dp))
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "No journal entries yet.\nSelect an emotion to write your first entry.",
                    fontSize = 13.sp,
                    color = Color(0xFF94A3B8),
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp
                )
            }
        } else {
            entries.forEach { entry ->
                JournalEntryCard(
                    entry = entry,
                    uiState = uiState,
                    onPlayAudio = onPlayAudio,
                    onPauseAudio = onPauseAudio,
                    onResumeAudio = onResumeAudio,
                    onSeekAudio = onSeekAudio
                )
                Spacer(Modifier.height(12.dp))
            }
        }
    }
}

@Composable
private fun JournalEntryCard(
    entry: JournalEntry,
    uiState: WellnessUiState,
    onPlayAudio: (String) -> Unit,
    onPauseAudio: () -> Unit,
    onResumeAudio: () -> Unit,
    onSeekAudio: (Int) -> Unit
) {
    val isPlayingThis = entry.audioPath != null && entry.audioPath == uiState.playingAudioPath
    
    val emoji = when (entry.emotion) {
        "Happy" -> "😊"
        "Calm" -> "😌"
        "Excited" -> "🤩"
        "Grateful" -> "🙏"
        "Anxious" -> "😰"
        "Sad" -> "😢"
        "Frustrated" -> "😤"
        "Peaceful" -> "🕊️"
        else -> "😶"
    }

    val timeFormatter = SimpleDateFormat("hh:mm a", Locale.getDefault())
    val dateFormatter = SimpleDateFormat("MMM dd", Locale.getDefault())
    val date = Date(entry.createdAt)
    val timeStr = timeFormatter.format(date)
    val dateStr = dateFormatter.format(date)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(8.dp, RoundedCornerShape(18.dp), ambientColor = PremiumShadowColor, spotColor = PremiumShadowColor)
            .clip(RoundedCornerShape(18.dp))
            .background(
                Brush.verticalGradient(
                    listOf(PremiumGlassHighlight, PremiumGlassWhite)
                )
            )
            .border(1.dp, PremiumGlassBorder, RoundedCornerShape(18.dp))
            .padding(16.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Emoji
                Text(emoji, fontSize = 28.sp)
                Spacer(Modifier.width(12.dp))

                // Content
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        entry.emotion,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1A1A1A)
                    )
                    if (!entry.message.isNullOrBlank()) {
                        Spacer(Modifier.height(2.dp))
                        Text(
                            "\"${entry.message}\"",
                            fontSize = 13.sp,
                            color = Color(0xFF6B6B6B),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Audio Play/Pause button
                if (!entry.audioPath.isNullOrBlank()) {
                    IconButton(
                        onClick = {
                            if (isPlayingThis) {
                                if (uiState.isPlayingPreview) onPauseAudio() else onResumeAudio()
                            } else {
                                onPlayAudio(entry.audioPath)
                            }
                        }
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(SkyBlue),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                if (isPlayingThis && uiState.isPlayingPreview) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = "Play/Pause",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }

            // Seeker for the active entry
            if (isPlayingThis) {
                Spacer(Modifier.height(12.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFF1F5F9))
                        .padding(8.dp)
                ) {
                    val posSec = uiState.playbackPositionMs / 1000
                    val durSec = uiState.audioDurationMs / 1000
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "%d:%02d".format(posSec / 60, posSec % 60),
                            fontSize = 10.sp,
                            color = Color(0xFF6B6B6B)
                        )
                        Text(
                            "%d:%02d".format(durSec / 60, durSec % 60),
                            fontSize = 10.sp,
                            color = Color(0xFF6B6B6B)
                        )
                    }
                    
                    Slider(
                        value = if (uiState.audioDurationMs > 0) uiState.playbackPositionMs.toFloat() / uiState.audioDurationMs else 0f,
                        onValueChange = { frac ->
                            onSeekAudio((frac * uiState.audioDurationMs).toInt())
                        },
                        modifier = Modifier.height(24.dp),
                        colors = SliderDefaults.colors(
                            thumbColor = SkyBlue,
                            activeTrackColor = SkyBlue,
                            inactiveTrackColor = Color(0xFFCBD5E1)
                        )
                    )
                }
            }

            // Timestamp
            Spacer(Modifier.height(8.dp))
            Text(
                "$dateStr  •  $timeStr",
                fontSize = 11.sp,
                color = Color(0xFF94A3B8)
            )
        }
    }
}
// BENTO HELPER COMPONENTS
// ═══════════════════════════════════════════════════════════════════════

@Composable
private fun EmotionPill(
    emotion: Emotion,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val color = getEmotionColor(emotion.name)
    val scale by animateFloatAsState(if (isSelected) 1.05f else 1f)
    
    Surface(
        onClick = onClick,
        modifier = Modifier
            .scale(scale)
            .width(100.dp),
        shape = RoundedCornerShape(20.dp),
        color = if (isSelected) color.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        border = BorderStroke(
            width = if (isSelected) 2.dp else 1.dp,
            brush = if (isSelected) Brush.linearGradient(listOf(color, color.copy(alpha = 0.5f)))
                    else SolidColor(MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
        )
    ) {
        Column(
            modifier = Modifier.padding(vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(text = emotion.emoji, fontSize = 28.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = emotion.name,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) color else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun BentoScoreTile(
    modifier: Modifier = Modifier,
    score: Int
) {
    NeonGlassCard(
        modifier = modifier.aspectRatio(1f),
        glowColor = PrimaryPurple,
        cornerRadius = 28.dp
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "WELLNESS\nSCORE",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.2.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth()) {
                CircularProgressIndicator(
                    progress = score / 100f,
                    modifier = Modifier.size(80.dp),
                    color = PrimaryPurple,
                    strokeWidth = 8.dp,
                    trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)
                )
                Text(
                    text = "$score",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            
            Text(
                text = "Optimal",
                style = MaterialTheme.typography.labelSmall,
                color = NeonGreen,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun BentoActionTile(
    modifier: Modifier = Modifier,
    title: String,
    subtitle: String,
    icon: ImageVector,
    color: Color,
    onClick: () -> Unit
) {
    NeonGlassCard(
        modifier = modifier
            .aspectRatio(1f)
            .clickable { onClick() },
        glowColor = color,
        cornerRadius = 28.dp
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
            }
            
            Column {
                Text(
                    text = title.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun MoodBentoTile(
    uiState: WellnessUiState,
    viewModel: WellnessViewModel
) {
    NeonGlassCard(
        glowColor = NeonCyan,
        cornerRadius = 28.dp
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "MOOD TRENDS",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp
                )
                
                // Compact Tabs
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .padding(2.dp)
                ) {
                    listOf("D", "W", "M").forEachIndexed { index, label ->
                        val isSelected = uiState.selectedTab == index
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSelected) NeonCyan.copy(alpha = 0.2f) else Color.Transparent)
                                .clickable { viewModel.setTab(index) }
                                .padding(horizontal = 12.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = label,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) NeonCyan else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Reusing existing chart but making it feel more integrated
            Box(modifier = Modifier.height(140.dp)) {
                StackedMoodBarChart(uiState.chartData)
            }
        }
    }
}

@Composable
private fun BentoMeditationTile(
    meditation: MeditationItem,
    isActive: Boolean,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(if (isPressed) 0.95f else 1f)

    NeonGlassCard(
        modifier = Modifier
            .scale(scale)
            .clickable(interactionSource = interactionSource, indication = null) { onClick() },
        glowColor = PrimaryPurple,
        cornerRadius = 24.dp,
        showGlow = isActive
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.Start
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(PrimaryPurple.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = meditation.icon,
                    contentDescription = null,
                    tint = PrimaryPurple,
                    modifier = Modifier.size(24.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = meditation.title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            
            Text(
                text = meditation.duration,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            if (isActive) {
                StatusBadge(text = "Active", color = NeonCyan)
            } else {
                Text(
                    text = "START →",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Black,
                    color = PrimaryPurple
                )
            }
        }
    }
}

@Composable
private fun ActiveTimerBento(
    timer: ActiveTimer,
    formattedTime: String,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onStop: () -> Unit
) {
    NeonGlassCard(
        glowColor = NeonCyan,
        cornerRadius = 32.dp
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Radial Progress Mini
            Box(contentAlignment = Alignment.Center) {
                CircularProgressIndicator(
                    progress = timer.progress,
                    modifier = Modifier.size(64.dp),
                    color = NeonCyan,
                    strokeWidth = 6.dp
                )
                Icon(
                    imageVector = Icons.Rounded.SelfImprovement,
                    contentDescription = null,
                    tint = NeonCyan,
                    modifier = Modifier.size(24.dp)
                )
            }
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = timer.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = formattedTime,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Black,
                    color = NeonCyan,
                    letterSpacing = 2.sp
                )
            }
            
            // Compact Controls
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IconButton(
                    onClick = { if (timer.isRunning) onPause() else onResume() },
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Icon(
                        imageVector = if (timer.isRunning) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
                IconButton(
                    onClick = onStop,
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(ErrorRed.copy(alpha = 0.1f))
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Stop,
                        contentDescription = null,
                        tint = ErrorRed
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String, dotColor: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(horizontal = 4.dp)
    ) {
        Box(
            Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(dotColor)
        )
        Spacer(Modifier.width(12.dp))
        Text(
            title.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            letterSpacing = 1.5.sp
        )
    }
}

@Composable
fun StackedMoodBarChart(data: List<MoodDayAggregate>) {
    val dividerColor = AppColors.dividerColor
    
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight()
    ) {
        val w = size.width
        val h = size.height
        val padBottom = 20f
        val chartH = h - padBottom
        
        if (data.isEmpty()) return@Canvas
        
        val slotW = w / data.size
        val barW = (slotW * 0.4f).coerceAtMost(20f)
        
        // Draw horizontal grid lines
        for (i in 0..4) {
            val y = chartH * i / 4f
            drawLine(dividerColor.copy(alpha = 0.2f), Offset(0f, y), Offset(w, y), strokeWidth = 1f)
        }

        data.forEachIndexed { i, day ->
            val cx = slotW * i + slotW / 2f
            var currentBottom = chartH
            
            if (day.entries.isNotEmpty()) {
                val unitH = chartH / 5 // Assuming max 5 entries per day for visualization
                
                day.entries.forEach { entry ->
                    val color = getEmotionColor(entry.emotion)
                    val top = (currentBottom - unitH).coerceAtLeast(0f)
                    
                    drawRoundRect(
                        color = color,
                        topLeft = Offset(cx - barW / 2f, top),
                        size = Size(barW, currentBottom - top),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(4f, 4f)
                    )
                    currentBottom = top
                }
            }
        }
    }
}

private fun getEmotionColor(emotion: String): Color = when (emotion) {
    "Happy" -> Color(0xFFFFD60A)
    "Calm" -> NeonBlue
    "Excited" -> NeonOrange
    "Grateful" -> NeonPurple
    "Peaceful" -> NeonGreen
    "Anxious" -> Color(0xFF64D2FF)
    "Sad" -> Color(0xFF0040DD)
    "Frustrated" -> ErrorRed
    else -> Color.Gray
}
RoundedCornerShape(16.dp),
                ambientColor = if (isActive) PrimaryPurple.copy(alpha = 0.2f) else Color.Transparent,
                spotColor = if (isActive) PrimaryPurple.copy(alpha = 0.2f) else Color.Transparent
            )
            .clip(RoundedCornerShape(16.dp))
            .background(Brush.verticalGradient(listOf(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.surface)))
            .border(1.dp, borderColor, RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(PrimaryPurple.copy(alpha = 0.12f))
                    .border(1.dp, PrimaryPurple.copy(alpha = 0.25f), RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = meditation.icon,
                    contentDescription = meditation.title,
                    modifier = Modifier.size(24.dp),
                    tint = PrimaryPurple
                )
            }

            Spacer(Modifier.width(14.dp))

            // Title + Duration
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    meditation.title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    meditation.duration,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Start Button
            Box(
                modifier = Modifier
                    .shadow(
                        8.dp,
                        RoundedCornerShape(28.dp),
                        ambientColor = SkyBlue.copy(alpha = 0.3f),
                        spotColor = SkyBlue.copy(alpha = 0.3f)
                    )
                    .clip(RoundedCornerShape(28.dp))
                    .background(
                        Brush.horizontalGradient(listOf(SkyBlue, SoftHighlighterGreen))
                    )
                    .clickable { onStart() }
                    .padding(horizontal = 18.dp, vertical = 10.dp)
            ) {
                Text(
                    if (isActive) "Active" else "Start",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}
