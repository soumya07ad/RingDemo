package com.dkgs.innerpulse.presentation.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dkgs.innerpulse.domain.model.ConnectionStatus
import com.dkgs.innerpulse.domain.repository.IFitnessRepository
import com.dkgs.innerpulse.domain.repository.IRingRepository
import com.dkgs.innerpulse.data.repository.StepRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel for the Dashboard screen.
 *
 * Combines data from IRingRepository (live ring hardware data)
 * and IFitnessRepository (local fitness data) into a single DashboardUiState.
 *
 * Dependencies are constructor-injected via AppViewModelFactory.
 */
class DashboardViewModel(
    private val ringRepository: IRingRepository,
    private val fitnessRepository: IFitnessRepository,
    private val stepRepository: StepRepository,
    private val settingsRepository: com.dkgs.innerpulse.domain.repository.ISettingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        observeRingState()
        loadFitnessData()
    }

    /**
     * Observe ring repository flows and merge into dashboard state.
     */
    private fun observeRingState() {
        viewModelScope.launch {
            ringRepository.connectionStatus.collect { status ->
                _uiState.update { state ->
                    when (status) {
                        is ConnectionStatus.Connected -> state.copy(
                            isConnected = true,
                            connectedRing = status.ring
                        )
                        is ConnectionStatus.Disconnected -> state.copy(
                            isConnected = false,
                            connectedRing = null
                        )
                        is ConnectionStatus.Connecting -> state.copy(
                            isConnected = false
                        )
                        is ConnectionStatus.Error -> state.copy(
                            isConnected = false,
                            errorMessage = status.message
                        )
                        is ConnectionStatus.Timeout -> state.copy(
                            isConnected = false,
                            errorMessage = "Connection timed out"
                        )
                    }
                }
            }
        }

        viewModelScope.launch {
            ringRepository.ringData.collect { ringData ->
                _uiState.update { state ->
                    state.copy(
                        heartRate = ringData.heartRate,
                        heartRateMeasuring = ringData.heartRateMeasuring,
                        spO2 = ringData.spO2,
                        spO2Measuring = ringData.spO2Measuring,
                        calories = ringData.calories,
                        stressLevel = ringData.stress,
                        stressMeasuring = ringData.stressMeasuring,
                        healthScore = ringData.healthScore,
                        batteryLevel = ringData.battery,
                        isCharging = ringData.isCharging,
                        sleepData = ringData.sleepData,
                        firmwareInfo = ringData.firmwareInfo
                    )
                }
            }
        }

        viewModelScope.launch {
            settingsRepository.ringType.collect { type ->
                _uiState.update { it.copy(ringType = type) }
            }
        }

        viewModelScope.launch {
            stepRepository.steps.collect { stepsValue ->
                _uiState.update { state ->
                    state.copy(steps = stepsValue)
                }
            }
        }

        viewModelScope.launch {
            stepRepository.distance.collect { distanceValue ->
                _uiState.update { state ->
                    state.copy(distance = distanceValue.toInt())
                }
            }
        }
    }

    val stepTrackingSupported: StateFlow<Boolean> = stepRepository.phoneStepDataSource.isSupported
    val isUsingPhone: StateFlow<Boolean> = stepRepository.isUsingPhone

    fun startPhoneStepTracking() {
        stepRepository.phoneStepDataSource.startListening()
    }

    fun stopPhoneStepTracking() {
        stepRepository.phoneStepDataSource.stopListening()
    }

    /**
     * Load local fitness data (daily summary, etc.)
     */
    private fun loadFitnessData() {
        viewModelScope.launch {
            try {
                val summary = fitnessRepository.getDailySummary()
                _uiState.update { state ->
                    state.copy(dailySummary = summary)
                }
            } catch (e: Exception) {
                // Non-critical, dashboard still works with ring data
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    // ==================== Measurements ====================

    fun startHeartRateMeasurement() {
        ringRepository.startHeartRateMeasurement()
    }

    fun stopHeartRateMeasurement() {
        ringRepository.stopHeartRateMeasurement()
    }

    fun startSpO2Measurement() {
        ringRepository.startSpO2Measurement()
    }

    fun stopSpO2Measurement() {
        ringRepository.stopSpO2Measurement()
    }

    fun startStressMeasurement() {
        ringRepository.startStressMeasurement()
    }

    fun stopStressMeasurement() {
        ringRepository.stopStressMeasurement()
    }

    fun requestSleepHistory() {
        ringRepository.requestSleepHistory()
    }

    fun syncAllData() {
        // We'll call fetchCachedData in the JMRingManager via a refresh method or requestSleepHistory
        // which internally fetches all history in the latest implementation
        ringRepository.requestSleepHistory() 
    }
}
