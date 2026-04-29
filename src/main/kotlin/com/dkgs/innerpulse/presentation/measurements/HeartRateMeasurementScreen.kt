package com.dkgs.innerpulse.presentation.measurements

import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.Icon
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.dkgs.innerpulse.ui.theme.ErrorRed
import com.dkgs.innerpulse.ui.theme.NeonPink

@Composable
fun HeartRateMeasurementScreen(
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
        accentColor = ErrorRed,
        gradientColors = listOf(ErrorRed, NeonPink),
        animationContent = { progress ->
            val infiniteTransition = rememberInfiniteTransition(label = "HeartBeat")
            val heartScale by infiniteTransition.animateFloat(
                initialValue = 1f,
                targetValue = 1.2f,
                animationSpec = infiniteRepeatable(
                    animation = tween(600, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "Scale"
            )

            Icon(
                imageVector = Icons.Default.Favorite,
                contentDescription = null,
                modifier = Modifier
                    .size(120.dp)
                    .scale(if (state.isFinished) 1f else heartScale),
                tint = ErrorRed
            )
        }
    )
}
