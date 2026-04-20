package com.dkgs.innerpulse.core.di

import android.content.Context
import com.dkgs.innerpulse.FitnessAPI
import com.dkgs.innerpulse.data.repository.FitnessRepositoryImpl
import com.dkgs.innerpulse.data.repository.MeditationRepositoryImpl
import com.dkgs.innerpulse.data.repository.RingRepositoryImpl
import com.dkgs.innerpulse.domain.repository.IFitnessRepository
import com.dkgs.innerpulse.domain.repository.IMeditationRepository
import com.dkgs.innerpulse.domain.repository.IRingRepository
import com.dkgs.innerpulse.domain.usecase.ConnectRingUseCase
import com.dkgs.innerpulse.domain.usecase.DisconnectRingUseCase
import com.dkgs.innerpulse.domain.usecase.GetRingDataUseCase
import com.dkgs.innerpulse.domain.usecase.ScanDevicesUseCase
import com.dkgs.innerpulse.network.client.RetrofitClient
import com.dkgs.innerpulse.network.repository.FitnessRepository as NetworkFitnessRepository
import androidx.room.Room
import com.dkgs.innerpulse.data.local.db.AppDatabase
import com.dkgs.innerpulse.data.repository.SleepRepositoryImpl
import com.dkgs.innerpulse.domain.repository.ICoachRepository
import com.dkgs.innerpulse.data.repository.CoachRepositoryImpl
import com.dkgs.innerpulse.domain.repository.SleepRepository
import com.dkgs.innerpulse.data.repository.StreakRepository
import com.dkgs.innerpulse.data.repository.SettingsRepository
import com.dkgs.innerpulse.data.repository.ThemeManager
import com.dkgs.innerpulse.domain.repository.IStreakRepository
import com.dkgs.innerpulse.domain.repository.ISettingsRepository
import com.dkgs.innerpulse.data.repository.MoodRepository
import com.dkgs.innerpulse.data.repository.JournalRepository
import com.dkgs.innerpulse.data.repository.StepRepository
import com.dkgs.innerpulse.data.source.PhoneStepDataSource

/**
 * Manual Dependency Injection Container
 * 
 * Provides singleton instances of repositories, use cases, and services.
 * All repositories are exposed via their interfaces for testability.
 * 
 * Usage:
 * ```
 * val container = AppContainer.getInstance(context)
 * val factory   = container.viewModelFactory
 * ```
 */
class AppContainer private constructor(private val context: Context) {

    val application: android.app.Application
        get() = context.applicationContext as android.app.Application

    // ── Core Services ──────────────────────────────────────────────

    val fitnessAPI: FitnessAPI by lazy { FitnessAPI(context) }

    val retrofitClient: RetrofitClient by lazy { RetrofitClient.getInstance(context) }

    val networkRepository: NetworkFitnessRepository by lazy {
        NetworkFitnessRepository(
            retrofitClient.getApiService(),
            retrofitClient.getTokenManager()
        )
    }

    // ── Database ────────────────────────────────────────────────────

    val appDatabase: AppDatabase by lazy {
        Room.databaseBuilder(
            context.applicationContext,
            AppDatabase::class.java,
            "fitness_app_database"
        ).fallbackToDestructiveMigration().build()
    }

    // ── Repositories (all exposed via interface) ────────────────────

    val authRepository: com.dkgs.innerpulse.domain.repository.IAuthRepository by lazy { 
        com.dkgs.innerpulse.data.repository.AuthRepositoryImpl() 
    }

    val ringRepository: IRingRepository by lazy { RingRepositoryImpl(context) }

    val phoneStepDataSource: PhoneStepDataSource by lazy { PhoneStepDataSource(context) }

    val stepRepository: StepRepository by lazy { StepRepository(ringRepository, phoneStepDataSource) }


    val sleepRepository: SleepRepository by lazy { SleepRepositoryImpl(appDatabase.sleepDao()) }

    val coachRepository: ICoachRepository by lazy { CoachRepositoryImpl(appDatabase.coachDao()) }

    val streakRepository: IStreakRepository by lazy { StreakRepository(appDatabase.streakDao()) }

    val moodRepository: MoodRepository by lazy { MoodRepository(appDatabase.moodDao()) }

    val journalRepository: JournalRepository by lazy { JournalRepository(appDatabase.journalDao()) }

    val settingsRepository: ISettingsRepository by lazy { SettingsRepository(context) }

    val meditationLocalRepository: IMeditationRepository by lazy { MeditationRepositoryImpl() }

    val healthConnectManager: com.dkgs.innerpulse.data.source.HealthConnectManager by lazy {
        com.dkgs.innerpulse.data.source.HealthConnectManager(context)
    }

    val fitnessHistoryRepository: com.dkgs.innerpulse.data.repository.FitnessHistoryRepository by lazy {
        com.dkgs.innerpulse.data.repository.FitnessHistoryRepository(
            healthConnectManager,
            appDatabase.dailyFitnessDao(),
            phoneStepDataSource
        )
    }

    val fitnessLocalRepository: IFitnessRepository by lazy { FitnessRepositoryImpl(fitnessAPI) }

    val themeManager: ThemeManager by lazy { ThemeManager(context) }

    // ── Use Cases ──────────────────────────────────────────────────

    val scanDevicesUseCase: ScanDevicesUseCase by lazy { ScanDevicesUseCase(ringRepository) }
    val connectRingUseCase: ConnectRingUseCase by lazy { ConnectRingUseCase(ringRepository) }
    val disconnectRingUseCase: DisconnectRingUseCase by lazy { DisconnectRingUseCase(ringRepository) }
    val getRingDataUseCase: GetRingDataUseCase by lazy { GetRingDataUseCase(ringRepository) }

    // ── ViewModel Factory ──────────────────────────────────────────

    val viewModelFactory: AppViewModelFactory by lazy { AppViewModelFactory(this) }

    // ── Singleton ──────────────────────────────────────────────────

    companion object {
        @Volatile
        private var INSTANCE: AppContainer? = null

        fun getInstance(context: Context): AppContainer {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: AppContainer(context.applicationContext).also {
                    INSTANCE = it
                }
            }
        }

        fun initialize(context: Context) {
            getInstance(context)
        }
    }
}
