package com.dkgs.innerpulse.core.di

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.dkgs.innerpulse.presentation.coach.CoachViewModel
import com.dkgs.innerpulse.presentation.dashboard.DashboardViewModel
import com.dkgs.innerpulse.presentation.dashboard.SleepTrackerViewModel
import com.dkgs.innerpulse.presentation.dashboard.SmartRingViewModel
import com.dkgs.innerpulse.presentation.settings.SettingsViewModel
import com.dkgs.innerpulse.presentation.streaks.StreakViewModel
import com.dkgs.innerpulse.presentation.theme.ThemeViewModel
import com.dkgs.innerpulse.presentation.wellness.WellnessViewModel

/**
 * Unified ViewModel factory for the entire app.
 *
 * All ViewModels receive their dependencies through constructor injection,
 * resolved from the AppContainer singleton.
 *
 * Usage in composable:
 * ```
 * val vm: DashboardViewModel = viewModel(factory = container.viewModelFactory)
 * ```
 */
class AppViewModelFactory(private val container: AppContainer) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(DashboardViewModel::class.java) ->
                DashboardViewModel(
                    ringRepository = container.ringRepository,
                    fitnessRepository = container.fitnessLocalRepository,
                    stepRepository = container.stepRepository,
                    settingsRepository = container.settingsRepository,
                    moodRepository = container.moodRepository,
                    meditationRepository = container.meditationLocalRepository
                ) as T


            modelClass.isAssignableFrom(SleepTrackerViewModel::class.java) ->
                SleepTrackerViewModel(
                    repository = container.sleepRepository
                ) as T

            modelClass.isAssignableFrom(CoachViewModel::class.java) ->
                CoachViewModel(
                    coachRepository = container.coachRepository
                ) as T

            modelClass.isAssignableFrom(WellnessViewModel::class.java) ->
                WellnessViewModel(
                    moodRepository = container.moodRepository,
                    journalRepository = container.journalRepository,
                    application = container.application
                ) as T

            modelClass.isAssignableFrom(StreakViewModel::class.java) ->
                StreakViewModel(
                    streakRepository = container.streakRepository
                ) as T

            modelClass.isAssignableFrom(SettingsViewModel::class.java) ->
                SettingsViewModel(
                    settingsRepository = container.settingsRepository,
                    authRepository = container.authRepository,
                    tokenManager = container.retrofitClient.getTokenManager()
                ) as T

            modelClass.isAssignableFrom(SmartRingViewModel::class.java) ->
                SmartRingViewModel(
                    ringRepository = container.ringRepository,
                    settingsRepository = container.settingsRepository
                ) as T

            modelClass.isAssignableFrom(ThemeViewModel::class.java) ->
                ThemeViewModel(
                    themeManager = container.themeManager
                ) as T

            modelClass.isAssignableFrom(com.dkgs.innerpulse.presentation.auth.AuthViewModel::class.java) ->
                com.dkgs.innerpulse.presentation.auth.AuthViewModel(
                    authRepository = container.authRepository,
                    tokenManager = container.retrofitClient.getTokenManager()
                ) as T

            modelClass.isAssignableFrom(com.dkgs.innerpulse.presentation.dashboard.FitnessHistoryViewModel::class.java) ->
                com.dkgs.innerpulse.presentation.dashboard.FitnessHistoryViewModel(
                    fitnessHistoryRepository = container.fitnessHistoryRepository
                ) as T

            modelClass.isAssignableFrom(com.dkgs.innerpulse.presentation.navigation.AppNavigationViewModel::class.java) ->
                com.dkgs.innerpulse.presentation.navigation.AppNavigationViewModel(
                    tokenManager = container.retrofitClient.getTokenManager()
                ) as T

            else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}
