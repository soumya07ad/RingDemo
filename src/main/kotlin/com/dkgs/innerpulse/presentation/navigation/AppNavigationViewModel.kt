package com.dkgs.innerpulse.presentation.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dkgs.innerpulse.network.auth.TokenManager
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

/**
 * UI State for app-level navigation
 */
data class AppNavigationUiState(
    val userLoggedIn: Boolean = false,
    val permissionsGranted: Boolean = false,
    val permissionsSkipped: Boolean = false,
    val setupComplete: Boolean = false,
    val isLoading: Boolean = true
)

/**
 * ViewModel managing app-level navigation state.
 * Replaces the legacy AppState singleton with proper MVVM state management.
 */
class AppNavigationViewModel(
    private val tokenManager: TokenManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(AppNavigationUiState())
    val uiState: StateFlow<AppNavigationUiState> = _uiState.asStateFlow()

    init {
        // Load persistent state on startup
        viewModelScope.launch {
            val storedToken = tokenManager.getToken()
            val firebaseUser = FirebaseAuth.getInstance().currentUser
            
            // Auto-recovery: If firebase is logged in but storage is empty
            if (storedToken.isNullOrEmpty() && firebaseUser != null) {
                try {
                    val tokenResult = firebaseUser.getIdToken(true).await()
                    tokenResult.token?.let { newToken ->
                        tokenManager.saveToken(newToken)
                        tokenManager.saveUserInfo(firebaseUser.uid, firebaseUser.email ?: "")
                    }
                } catch (e: Exception) {
                    // Recovery failed, stay on login
                }
            }
            
            val finalToken = tokenManager.getToken()
            val setupCompleteStatus = tokenManager.setupCompleteFlow.first()
            val skippedStatus = tokenManager.permissionsSkippedFlow.first()
            val permissionsStatus = checkAllPermissions()
            
            _uiState.update { 
                it.copy(
                    userLoggedIn = !finalToken.isNullOrEmpty(),
                    permissionsGranted = permissionsStatus,
                    permissionsSkipped = skippedStatus,
                    setupComplete = setupCompleteStatus,
                    isLoading = false
                )
            }
        }
    }

    /**
     * Define all dangerous permissions required by the app across all features.
     */
    fun getGlobalPermissions(): Array<String> {
        val list = mutableListOf(
            android.Manifest.permission.ACCESS_FINE_LOCATION,
            android.Manifest.permission.ACCESS_COARSE_LOCATION,
            android.Manifest.permission.ACTIVITY_RECOGNITION,
            android.Manifest.permission.RECORD_AUDIO
        )

        // Android 16 (Baklava) Granular Health Permissions
        // We request these alongside BODY_SENSORS to satisfy the new platform requirements
        if (android.os.Build.VERSION.SDK_INT >= 36) {
            list.add("android.permission.health.READ_HEART_RATE")
            list.add("android.permission.health.READ_OXYGEN_SATURATION")
            list.add("android.permission.health.READ_SKIN_TEMPERATURE")
        }

        // Always include BODY_SENSORS for hardware sensor access required by the Ring SDK
        list.add(android.Manifest.permission.BODY_SENSORS)
        
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            list.add(android.Manifest.permission.BLUETOOTH_SCAN)
            list.add(android.Manifest.permission.BLUETOOTH_CONNECT)
            list.add(android.Manifest.permission.BLUETOOTH_ADVERTISE)
        }
        
        // Notifications permission for Android 13+
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            list.add(android.Manifest.permission.POST_NOTIFICATIONS)
        }

        // Storage permissions for older Android versions
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.TIRAMISU) {
            list.add(android.Manifest.permission.READ_EXTERNAL_STORAGE)
            list.add(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }

        return list.toTypedArray()
    }

    fun checkAllPermissions(): Boolean {
        val context = com.dkgs.innerpulse.FitnessApplication.getInstance()
        
        val permissions = getGlobalPermissions()
        val results = mutableMapOf<String, Boolean>()
        
        permissions.forEach { perm ->
            val granted = androidx.core.content.ContextCompat.checkSelfPermission(context, perm) == 
                    android.content.pm.PackageManager.PERMISSION_GRANTED
            results[perm] = granted
            android.util.Log.d("AppNavVM", "Permission Check: $perm -> ${if (granted) "GRANTED" else "DENIED"}")
        }

        // 1. CRITICAL: Location (Needed for Bluetooth)
        val locationGranted = results[android.Manifest.permission.ACCESS_COARSE_LOCATION] == true ||
                             results[android.Manifest.permission.ACCESS_FINE_LOCATION] == true

        // 2. CRITICAL: Bluetooth (Android 12+)
        val bluetoothGranted = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            results[android.Manifest.permission.BLUETOOTH_SCAN] == true &&
            results[android.Manifest.permission.BLUETOOTH_CONNECT] == true
        } else true

        // 3. Sensors (Optional for BLE rings)
        val sensorsGranted = results[android.Manifest.permission.BODY_SENSORS] == true

        // Only require Location and Bluetooth to proceed to dashboard
        val allCriticalGranted = locationGranted && bluetoothGranted
        
        android.util.Log.d("AppNavVM", "All Critical Granted: $allCriticalGranted (Loc: $locationGranted, BT: $bluetoothGranted, Sensors: $sensorsGranted)")
        
        return allCriticalGranted
    }

    fun onPermissionsResult(results: Map<String, Boolean>) {
        val allGranted = checkAllPermissions()
        _uiState.update { it.copy(permissionsGranted = allGranted) }
    }

    fun skipPermissions() {
        viewModelScope.launch {
            tokenManager.setPermissionsSkipped(true)
            _uiState.update { it.copy(permissionsSkipped = true) }
        }
    }

    fun refreshPermissionStatus() {
        val status = checkAllPermissions()
        _uiState.update { it.copy(permissionsGranted = status) }
    }

    fun onLoginSuccess() {
        viewModelScope.launch {
            val isSetup = tokenManager.setupCompleteFlow.first()
            _uiState.update { it.copy(userLoggedIn = true, setupComplete = isSetup) }
        }
    }

    fun onSetupComplete() {
        viewModelScope.launch {
            tokenManager.setSetupComplete(true)
            _uiState.update { it.copy(setupComplete = true) }
        }
    }

    fun onSkip() {
        viewModelScope.launch {
            tokenManager.setSetupComplete(true)
            _uiState.update { it.copy(setupComplete = true) }
        }
    }

    fun onLogout() {
        viewModelScope.launch {
            tokenManager.clearToken()
            com.google.firebase.auth.FirebaseAuth.getInstance().signOut()
            _uiState.update { AppNavigationUiState(isLoading = false) }
        }
    }
}
