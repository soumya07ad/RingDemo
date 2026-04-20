package com.dkgs.innerpulse.domain.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.ui.graphics.vector.ImageVector

data class Emotion(
    val name: String,
    val icon: ImageVector,
    val score: Int
)

data class MeditationItem(
    val id: String,
    val title: String,
    val duration: String,
    val durationMinutes: Int,
    val icon: ImageVector
)

data class ActiveTimer(
    val meditationId: String,
    val title: String,
    val remainingSeconds: Int,
    val totalSeconds: Int,
    val isRunning: Boolean = true,
    val isCompleted: Boolean = false
) {
    val progress: Float
        get() = if (totalSeconds > 0) (totalSeconds - remainingSeconds).toFloat() / totalSeconds else 0f
}
