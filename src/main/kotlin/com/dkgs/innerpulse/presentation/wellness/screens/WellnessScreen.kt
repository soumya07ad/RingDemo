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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
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
import com.dkgs.innerpulse.data.repository.MoodDayAggregate
import androidx.compose.foundation.lazy.staggeredgrid.*

// ═══════════════════════════════════════════════════════════════════════
// WELLNESS HUB — Bento Architecture
// ═══════════════════════════════════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WellnessScreen(
    viewModel: WellnessViewModel,
    onMeditationClick: (String) -> Unit,
    onBackClick: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

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
                    painter = painterResource(id = com.dkgs.innerpulse.R.drawable.logo_ring),
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

                            // Horizontal Emotion Picker
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
                        BentoScoreTile(
                            modifier = Modifier.weight(1.2f),
                            score = uiState.wellnessScore
                        )
                        
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            BentoActionTile(
                                icon = Icons.Rounded.EditNote,
                                label = "JOURNAL",
                                color = NeonCyan,
                                onClick = { /* Open Journal */ }
                            )
                            BentoActionTile(
                                icon = Icons.Rounded.Timeline,
                                label = "STATS",
                                color = NeonGreen,
                                onClick = { /* View Stats */ }
                            )
                        }
                    }
                }

                // ── MOOD TRENDS BENTO ──
                item {
                    NeonGlassCard(
                        glowColor = NeonBlue,
                        cornerRadius = 24.dp
                    ) {
                        Column {
                            Text(
                                text = "WEEKLY MOOD TRENDS",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.2.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(20.dp))
                            
                            Box(modifier = Modifier.height(140.dp).fillMaxWidth()) {
                                StackedMoodBarChart(data = uiState.weeklyMoodAggregates)
                            }
                        }
                    }
                }

                // ── MEDITATIONS SECTION ──
                item {
                    Text(
                        text = "MIND GUIDANCE",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.2.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                }

                item {
                    LazyVerticalStaggeredGrid(
                        columns = StaggeredGridCells.Fixed(1),
                        modifier = Modifier.heightIn(max = 600.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalItemSpacing = 16.dp,
                        userScrollEnabled = false
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

                // Active Timer Overlay
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

// ═══════════════════════════════════════════════════════════════════════
// HELPER COMPONENTS
// ═══════════════════════════════════════════════════════════════════════

@Composable
private fun EmotionPill(
    emotion: Emotion,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor by animateColorAsState(
        if (isSelected) NeonPink.copy(alpha = 0.2f) else Color.Transparent
    )
    val borderColor by animateColorAsState(
        if (isSelected) NeonPink else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
    )

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = backgroundColor,
        border = BorderStroke(1.dp, borderColor),
        modifier = Modifier.height(44.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = emotion.icon,
                contentDescription = null,
                tint = if (isSelected) NeonPink else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = emotion.name,
                style = MaterialTheme.typography.labelLarge,
                color = if (isSelected) NeonPink else MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
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
    icon: ImageVector,
    label: String,
    color: Color,
    onClick: () -> Unit
) {
    NeonGlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp)
            .clickable { onClick() },
        glowColor = color,
        cornerRadius = 20.dp
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(color.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = color, modifier = Modifier.size(20.dp))
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun BentoMeditationTile(
    meditation: MeditationItem,
    isActive: Boolean,
    onClick: () -> Unit
) {
    val borderColor = if (isActive) PrimaryPurple else MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)
    
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
        border = BorderStroke(1.dp, borderColor)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(PrimaryPurple.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = meditation.icon,
                    contentDescription = null,
                    tint = PrimaryPurple,
                    modifier = Modifier.size(28.dp)
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = meditation.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = meditation.duration,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (isActive) {
                Icon(
                    imageVector = Icons.Rounded.GraphicEq,
                    contentDescription = null,
                    tint = PrimaryPurple,
                    modifier = Modifier.size(24.dp)
                )
            } else {
                Icon(
                    imageVector = Icons.Rounded.PlayArrow,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(24.dp)
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
        glowColor = ErrorRed,
        cornerRadius = 24.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "ACTIVE SESSION",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Black,
                    color = ErrorRed
                )
                Text(
                    text = formattedTime,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                IconButton(
                    onClick = if (timer.isPaused) onResume else onPause,
                    modifier = Modifier.background(MaterialTheme.colorScheme.surface, CircleShape)
                ) {
                    Icon(
                        imageVector = if (timer.isPaused) Icons.Rounded.PlayArrow else Icons.Rounded.Pause,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
                IconButton(
                    onClick = onStop,
                    modifier = Modifier.background(ErrorRed.copy(alpha = 0.2f), CircleShape)
                ) {
                    Icon(Icons.Rounded.Stop, null, tint = ErrorRed)
                }
            }
        }
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
