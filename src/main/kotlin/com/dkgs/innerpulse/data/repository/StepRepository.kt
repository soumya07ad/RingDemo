package com.dkgs.innerpulse.data.repository

import com.dkgs.innerpulse.data.source.PhoneStepDataSource
import com.dkgs.innerpulse.domain.model.ConnectionStatus
import com.dkgs.innerpulse.domain.repository.IRingRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

class StepRepository(
    private val ringRepository: IRingRepository,
    val phoneStepDataSource: PhoneStepDataSource
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val _steps = MutableStateFlow(0)
    val steps: StateFlow<Int> = _steps.asStateFlow()

    private val _distance = MutableStateFlow(0f)
    val distance: StateFlow<Float> = _distance.asStateFlow()
    
    private val _isUsingPhone = MutableStateFlow(false)
    val isUsingPhone: StateFlow<Boolean> = _isUsingPhone.asStateFlow()

    init {
        scope.launch {
            combine(
                ringRepository.connectionStatus,
                ringRepository.ringData,
                phoneStepDataSource.steps
            ) { connStatus, ringData, phoneSteps ->
                val isConnected = connStatus is ConnectionStatus.Connected
                Log.d("StepRepository", "Sync check: isConnected=$isConnected, ringSteps=${ringData.steps}, phoneSteps=$phoneSteps")
                
                // If ring is connected and providing steps, use them. Otherwise use phone.
                if (isConnected && ringData.steps > 0) {
                    Log.i("StepRepository", "→ Using RING steps: ${ringData.steps}")
                    _isUsingPhone.value = false
                    _steps.value = ringData.steps
                    _distance.value = ringData.distance.toFloat()
                } else {
                    Log.i("StepRepository", "→ Using PHONE steps: $phoneSteps")
                    _isUsingPhone.value = true
                    _steps.value = phoneSteps
                    _distance.value = phoneSteps * 0.75f // 0.75m per step
                }
            }.collect { }
        }
    }
}
