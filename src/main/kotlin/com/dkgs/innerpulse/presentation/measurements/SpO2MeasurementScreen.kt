package com.dkgs.innerpulse.presentation.measurements

import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.Icon
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.dkgs.innerpulse.ui.theme.NeonBlue
import com.dkgs.innerpulse.ui.theme.NeonCyan
import com.dkgs.innerpulse.ui.components.OxygenFlowAnimation
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxSize

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
        showCircularProgress = false,
        animationContent = { progress ->
            OxygenFlowAnimation(
                modifier = Modifier.fillMaxSize(),
                progress = progress,
                isFinished = state.isFinished
            )
        }
    )
}
