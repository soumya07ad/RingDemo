package com.dkgs.innerpulse.presentation.measurements

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Done
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dkgs.innerpulse.ui.theme.*

@Composable
fun BaseMeasurementScreen(
    state: MeasurementUiState,
    onCancel: () -> Unit,
    onDone: () -> Unit,
    accentColor: Color,
    gradientColors: List<Color>,
    showCircularProgress: Boolean = true,
    animationContent: @Composable (Float) -> Unit
) {
    val isDark = AppColors.isDark
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Decorative background glow
        Box(
            modifier = Modifier
                .size(400.dp)
                .align(Alignment.Center)
                .graphicsLayer(alpha = 0.15f)
                .drawBehind {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(accentColor, Color.Transparent)
                        )
                    )
                }
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "VITAL SCAN",
                    style = MaterialTheme.typography.labelLarge,
                    color = accentColor,
                    letterSpacing = 2.sp
                )
                
                if (!state.isFinished) {
                    IconButton(onClick = onCancel) {
                        Icon(
                            Icons.Default.Close, 
                            contentDescription = "Cancel", 
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.weight(0.5f))

            // Central Animation Area
            Box(
                modifier = Modifier.size(280.dp),
                contentAlignment = Alignment.Center
            ) {
                val animatedProgress by animateFloatAsState(
                    targetValue = state.progress,
                    animationSpec = tween(durationMillis = 500, easing = LinearEasing),
                    label = "Progress"
                )

                if (showCircularProgress) {
                    // Circular Progress Background
                    CircularProgressIndicator(
                        progress = { 1f },
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f),
                        strokeWidth = 8.dp,
                        trackColor = Color.Transparent,
                    )

                    // Active Progress
                    CircularProgressIndicator(
                        progress = { animatedProgress },
                        modifier = Modifier.fillMaxSize(),
                        color = accentColor,
                        strokeWidth = 8.dp,
                        strokeCap = androidx.compose.ui.graphics.StrokeCap.Round,
                    )
                }

                // Custom Animation Content
                animationContent(animatedProgress)
            }

            Spacer(modifier = Modifier.height(48.dp))

            // Status & Result
            AnimatedContent(
                targetState = state.isFinished,
                transitionSpec = {
                    fadeIn() + slideInVertically() togetherWith fadeOut()
                },
                label = "StatusContent"
            ) { finished ->
                if (finished) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(
                                text = state.resultValue,
                                style = MaterialTheme.typography.displayLarge,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Black
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = state.unit,
                                style = MaterialTheme.typography.headlineMedium,
                                color = accentColor,
                                modifier = Modifier.padding(bottom = 12.dp)
                            )
                        }
                        Text(
                            text = "HEALTH DATA RECORDED",
                            style = MaterialTheme.typography.labelMedium,
                            color = NeonGreen,
                            letterSpacing = 1.sp
                        )
                    }
                } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = state.statusText,
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Medium
                        )
                        if (showCircularProgress) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "${(state.progress * 100).toInt()}%",
                                style = MaterialTheme.typography.titleMedium,
                                color = accentColor.copy(alpha = 0.8f)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Footer Button
            if (state.isFinished) {
                Button(
                    onClick = onDone,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(Icons.Default.Done, contentDescription = null)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "DONE",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    }
}
