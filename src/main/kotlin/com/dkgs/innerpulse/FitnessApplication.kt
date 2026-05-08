package com.dkgs.innerpulse

import android.app.Application
import android.util.Log
import com.dkgs.innerpulse.core.di.AppContainer
import com.crrepa.ble.CRPBleClient

/**
 * Application class for one-time initialization
 * 
 * CRREPA SDK APPROACH:
 * 1. Official SDK initialized via CRPBleClient.create(this)
 * 2. BLE operations handled through CrrepaRingManager
 */
class FitnessApplication : Application() {

    private var bleClient: CRPBleClient? = null

    companion object {
        private const val TAG = "FitnessApplication"
        
        @Volatile
        private var instance: FitnessApplication? = null
        
        fun getInstance(): FitnessApplication = instance!!
    }

    fun getBleClient(): CRPBleClient? = bleClient

    override fun onCreate() {
        super.onCreate()
        instance = this
        
        // Initialize MVVM DI Container
        AppContainer.initialize(this)
        Log.i(TAG, "✓ MVVM DI Container initialized")
        
        // Initialize Crrepa SDK
        bleClient = CRPBleClient.create(this)
        Log.i(TAG, "✓ Crrepa Smart Ring SDK initialized")
        
        Log.i(TAG, "═══════════════════════════════════")
        Log.i(TAG, "✓ App started — Crrepa SDK mode")
        Log.i(TAG, "═══════════════════════════════════")

        setupBackgroundSync()
    }

    private fun setupBackgroundSync() {
        val constraints = androidx.work.Constraints.Builder()
            // Require ANY form of internet (Wi-Fi or Mobile Data) as per user request
            .setRequiredNetworkType(androidx.work.NetworkType.CONNECTED)
            .build()

        val syncRequest = androidx.work.PeriodicWorkRequestBuilder<com.dkgs.innerpulse.network.sync.BackendSyncWorker>(
            15, java.util.concurrent.TimeUnit.MINUTES // Minimum allowed periodic interval is 15 minutes
        )
            .setConstraints(constraints)
            .build()

        androidx.work.WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "BackendSyncWorker",
            androidx.work.ExistingPeriodicWorkPolicy.KEEP,
            syncRequest
        )
        Log.i(TAG, "✓ Backend Sync Worker Scheduled")
    }
}
