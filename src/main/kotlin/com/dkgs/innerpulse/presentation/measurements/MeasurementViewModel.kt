package com.dkgs.innerpulse.presentation.measurements

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dkgs.innerpulse.domain.repository.IRingRepository
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

enum class MeasurementType {
    HEART_RATE, SPO2, STRESS, HEALTH_SCORE
}

data class MeasurementUiState(
    val type: MeasurementType,
    val progress: Float = 0f,
    val isFinished: Boolean = false,
    val resultValue: String = "",
    val currentValue: String = "",
    val unit: String = "",
    val statusText: String = ""
)

class MeasurementViewModel(
    private val ringRepository: IRingRepository,
    private val type: MeasurementType
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        MeasurementUiState(
            type = type,
            statusText = getInitialStatus(type),
            unit = getUnit(type)
        )
    )
    val uiState: StateFlow<MeasurementUiState> = _uiState.asStateFlow()

    private var simulationJob: Job? = null

    init {
        startMeasurement()
        observeResults()
    }

    private fun startMeasurement() {
        when (type) {
            MeasurementType.HEART_RATE -> ringRepository.startHeartRateMeasurement()
            MeasurementType.SPO2 -> ringRepository.startSpO2Measurement()
            MeasurementType.STRESS -> ringRepository.startStressMeasurement()
            MeasurementType.HEALTH_SCORE -> ringRepository.requestSleepHistory() // Triggers computeAllSleepScore
        }
        startProgressSimulation()
    }

    private fun startProgressSimulation() {
        val duration = when (type) {
            MeasurementType.HEART_RATE -> 12_000L
            MeasurementType.SPO2 -> 15_000L
            MeasurementType.STRESS -> 20_000L
            MeasurementType.HEALTH_SCORE -> 3_000L
        }

        simulationJob = viewModelScope.launch {
            val startTime = System.currentTimeMillis()
            while (isActive) {
                val elapsed = System.currentTimeMillis() - startTime
                val progress = (elapsed.toFloat() / duration).coerceAtMost(0.95f) // Stop at 95% until result arrives
                _uiState.update { it.copy(progress = progress) }
                if (progress >= 0.95f) break
                delay(100)
            }
        }
    }

    private fun observeResults() {
        viewModelScope.launch {
            ringRepository.ringData.collect { data ->
                val current = when (type) {
                    MeasurementType.HEART_RATE -> if (data.heartRate > 0) data.heartRate.toString() else null
                    MeasurementType.SPO2 -> if (data.spO2 > 0) data.spO2.toInt().toString() else null
                    MeasurementType.STRESS -> if (data.stress > 0) data.stress.toString() else null
                    MeasurementType.HEALTH_SCORE -> if (data.healthScore > 0) data.healthScore.toString() else null
                }

                if (current != null) {
                    _uiState.update { it.copy(currentValue = current) }
                }

                val isFinishedResult = when (type) {
                    MeasurementType.HEART_RATE -> !data.heartRateMeasuring && data.heartRate > 0
                    MeasurementType.SPO2 -> !data.spO2Measuring && data.spO2 > 0
                    MeasurementType.STRESS -> !data.stressMeasuring && data.stress > 0
                    MeasurementType.HEALTH_SCORE -> data.healthScore > 0
                }

                // Or finish if we have a value and progress is high enough
                val forcedFinish = current != null && _uiState.value.progress >= 0.4f

                if (isFinishedResult || forcedFinish) {
                    finishMeasurement(current ?: _uiState.value.currentValue)
                }
            }
        }
    }

    private fun finishMeasurement(result: String) {
        simulationJob?.cancel()
        _uiState.update { 
            it.copy(
                progress = 1.0f,
                isFinished = true,
                resultValue = result,
                statusText = "Measurement Complete"
            )
        }
    }

    fun cancelMeasurement() {
        simulationJob?.cancel()
        when (type) {
            MeasurementType.HEART_RATE -> ringRepository.stopHeartRateMeasurement()
            MeasurementType.SPO2 -> ringRepository.stopSpO2Measurement()
            MeasurementType.STRESS -> ringRepository.stopStressMeasurement()
            else -> {}
        }
    }

    private fun getInitialStatus(type: MeasurementType) = when (type) {
        MeasurementType.HEART_RATE -> "Measuring Heart Rate..."
        MeasurementType.SPO2 -> "Analyzing Blood Oxygen..."
        MeasurementType.STRESS -> "Assessing Stress Levels..."
        MeasurementType.HEALTH_SCORE -> "Calculating Health Score..."
    }

    private fun getUnit(type: MeasurementType) = when (type) {
        MeasurementType.HEART_RATE -> "bpm"
        MeasurementType.SPO2 -> "%"
        MeasurementType.STRESS -> ""
        MeasurementType.HEALTH_SCORE -> ""
    }
}
