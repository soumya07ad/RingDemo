package com.dkgs.innerpulse

import android.app.Application
import android.util.Log
import com.dkgs.innerpulse.core.di.AppContainer
import com.gps.track.jmring.ble.RingBleUtils

/**
 * Application class for one-time initialization
 * 
 * JMRing SDK APPROACH:
 * 1. Official SDK initialized via RingBleUtils.initBle(this)
 * 2. BLE operations handled through JMRing SDK
 */
class FitnessApplication : Application() {

    companion object {
        private const val TAG = "FitnessApplication"
        
        @Volatile
        private var instance: FitnessApplication? = null
        
        fun getInstance(): FitnessApplication = instance!!
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        
        // Initialize MVVM DI Container
        AppContainer.initialize(this)
        Log.i(TAG, "✓ MVVM DI Container initialized")
        
        // Initialize JMRing SDK
        RingBleUtils.initBle(this)
        Log.i(TAG, "✓ JMRing SDK initialized")
        
        Log.i(TAG, "═══════════════════════════════════")
        Log.i(TAG, "✓ App started — JMRing SDK mode")
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
