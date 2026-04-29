package com.dkgs.innerpulse.presentation.measurements

import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Face
import androidx.compose.material3.Icon
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.dkgs.innerpulse.ui.theme.PrimaryPurple
import com.dkgs.innerpulse.ui.theme.NeonPink

@Composable
fun StressMeasurementScreen(
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
        accentColor = PrimaryPurple,
        gradientColors = listOf(PrimaryPurple, NeonPink),
        animationContent = { progress ->
            val infiniteTransition = rememberInfiniteTransition(label = "Zen")
            val alpha by infiniteTransition.animateFloat(
                initialValue = 0.3f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(2000, easing = LinearEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "Alpha"
            )

            Icon(
                imageVector = Icons.Default.Face,
                contentDescription = null,
                modifier = Modifier
                    .size(120.dp)
                    .alpha(if (state.isFinished) 1f else alpha),
                tint = PrimaryPurple
            )
        }
    )
}
