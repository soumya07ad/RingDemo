package com.dkgs.innerpulse.presentation.measurements

import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.dkgs.innerpulse.ui.theme.NeonGreen
import com.dkgs.innerpulse.ui.theme.WarningAmber

@Composable
fun HealthScoreCalculationScreen(
    viewModel: MeasurementViewModel,
    onNavigateBack: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()

    BaseMeasurementScreen(
        state = state,
        onCancel = {
            viewModel.cancelMeasurement()
            onNavigateBack()
        },
        onDone = onNavigateBack,
        accentColor = NeonGreen,
        gradientColors = listOf(NeonGreen, WarningAmber),
        animationContent = { progress ->
            val infiniteTransition = rememberInfiniteTransition(label = "Scan")
            val scale by infiniteTransition.animateFloat(
                initialValue = 0.8f,
                targetValue = 1.2f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1000, easing = LinearEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "Scale"
            )

            Icon(
                imageVector = Icons.Default.Star,
                contentDescription = null,
                modifier = Modifier
                    .size(120.dp)
                    .scale(if (state.isFinished) 1f else scale),
                tint = NeonGreen
            )
        }
    )
}
