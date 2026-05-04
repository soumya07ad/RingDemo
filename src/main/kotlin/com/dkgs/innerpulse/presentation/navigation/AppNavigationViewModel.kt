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
            val permissionsStatus = checkAllPermissions()
            
            _uiState.update { 
                it.copy(
                    userLoggedIn = !finalToken.isNullOrEmpty(),
                    permissionsGranted = permissionsStatus,
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
            android.Manifest.permission.BODY_SENSORS,
            android.Manifest.permission.ACTIVITY_RECOGNITION,
            android.Manifest.permission.RECORD_AUDIO
        )
        
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
        
        // On Android 12+, ACCESS_FINE_LOCATION might be denied while ACCESS_COARSE_LOCATION is granted (Approximate location).
        // We consider it "granted enough" to proceed, though some features might be limited.
        val locationGranted = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_COARSE_LOCATION) == 
                    android.content.pm.PackageManager.PERMISSION_GRANTED
        } else {
            androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_FINE_LOCATION) == 
                    android.content.pm.PackageManager.PERMISSION_GRANTED
        }

        val othersGranted = permissions.filter { 
            it != android.Manifest.permission.ACCESS_FINE_LOCATION && 
            it != android.Manifest.permission.ACCESS_COARSE_LOCATION 
        }.all {
            androidx.core.content.ContextCompat.checkSelfPermission(context, it) == 
                    android.content.pm.PackageManager.PERMISSION_GRANTED
        }

        return locationGranted && othersGranted
    }

    fun onPermissionsResult(results: Map<String, Boolean>) {
        val allGranted = checkAllPermissions()
        _uiState.update { it.copy(permissionsGranted = allGranted) }
    }

    fun skipPermissions() {
        _uiState.update { it.copy(permissionsSkipped = true) }
    }

    fun refreshPermissionStatus() {
        val status = checkAllPermissions()
        _uiState.update { it.copy(permissionsGranted = status) }
    }

    fun onLoginSuccess() {
        _uiState.update { it.copy(userLoggedIn = true, setupComplete = false) }
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
            _uiState.update { AppNavigationUiState(isLoading = false) }
        }
    }
}
