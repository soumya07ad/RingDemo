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
import com.dkgs.innerpulse.ui.components.NeuralOrbAnimation
import androidx.compose.foundation.layout.fillMaxSize

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
        showCircularProgress = false,
        animationContent = { progress ->
            NeuralOrbAnimation(
                modifier = Modifier.fillMaxSize(),
                progress = progress,
                isFinished = state.isFinished
            )
        }
    )
}
