package com.dkgs.innerpulse.presentation.dashboard

import com.dkgs.innerpulse.domain.model.Ring
import com.dkgs.innerpulse.domain.model.RingHealthData
import com.dkgs.innerpulse.domain.model.ConnectionStatus
import com.dkgs.innerpulse.domain.model.DailyHealthSummary

import com.dkgs.innerpulse.domain.model.FirmwareInfo
import com.dkgs.innerpulse.domain.model.SleepData
import com.dkgs.innerpulse.data.repository.MoodDayAggregate

/**
 * UI State for the Dashboard screen.
 * 
 * Combines ring health data with local fitness data into a single
 * state object for the dashboard to observe.
 */
data class DashboardUiState(
    // Ring connection
    val isConnected: Boolean = false,
    val connectedRing: Ring? = null,
    val batteryLevel: Int? = null,
    val isCharging: Boolean = false,
    val ringType: Int = 2, // 1 for 研强, 2 for 小七

    // Ring health metrics
    val heartRate: Int = 0,
    val heartRateMeasuring: Boolean = false,
    val spO2: Float = 0f,
    val spO2Measuring: Boolean = false,
    val bloodPressureSystolic: Int = 0,
    val bloodPressureDiastolic: Int = 0,
    val bloodPressureHeartRate: Int = 0,
    val bloodPressureMeasuring: Boolean = false,
    val steps: Int = 0,
    val distance: Int = 0,
    val calories: Int = 0,
    val stressLevel: Int = 0,
    val stressMeasuring: Boolean = false,
    val healthScore: Int = 0,
    
    // Additional ring metrics
    val sleepData: SleepData? = null,
    val firmwareInfo: FirmwareInfo? = null,

    // Daily summary
    val dailySummary: DailyHealthSummary = DailyHealthSummary(),
    
    // Insights & Mood
    val weeklyMoodTrend: List<MoodDayAggregate> = emptyList(),
    val meditationMinutes: Int = 0,

    // Loading / error
    val isLoading: Boolean = false,
    val errorMessage: String? = null
) {
    val hasHeartRate: Boolean get() = heartRate > 0
    val hasSpO2: Boolean get() = spO2 > 0
    val hasSteps: Boolean get() = steps > 0
}
