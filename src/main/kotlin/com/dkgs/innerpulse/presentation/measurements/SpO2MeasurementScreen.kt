package com.dkgs.innerpulse.presentation.measurements

import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.Icon
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.dkgs.innerpulse.ui.theme.NeonBlue
import com.dkgs.innerpulse.ui.theme.NeonCyan

@Composable
fun SpO2MeasurementScreen(
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
        accentColor = NeonCyan,
        gradientColors = listOf(NeonCyan, NeonBlue),
        animationContent = { progress ->
            val infiniteTransition = rememberInfiniteTransition(label = "Wave")
            val rotation by infiniteTransition.animateFloat(
                initialValue = 0f,
                targetValue = 360f,
                animationSpec = infiniteRepeatable(
                    animation = tween(3000, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart
                ),
                label = "Rotation"
            )

            Icon(
                imageVector = Icons.Default.FavoriteBorder,
                contentDescription = null,
                modifier = Modifier
                    .size(120.dp)
                    .rotate(if (state.isFinished) 0f else rotation),
                tint = NeonCyan
            )
        }
    )
}
