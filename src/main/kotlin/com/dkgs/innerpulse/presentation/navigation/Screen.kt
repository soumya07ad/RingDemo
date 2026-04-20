package com.dkgs.innerpulse.presentation.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String, val label: String, val icon: ImageVector) {
    object Dashboard : Screen("dashboard", "Dashboard", Icons.Rounded.Home)
    object Sleep : Screen("sleep", "Sleep", Icons.Rounded.Bedtime)
    object Wellness : Screen("wellness", "Wellness", Icons.Rounded.SelfImprovement) 
    object Streaks : Screen("streaks", "Streaks", Icons.Rounded.LocalFireDepartment)
    object Coach : Screen("coach", "AURA", Icons.Rounded.Psychology)
    object Settings : Screen("settings", "Settings", Icons.Rounded.Settings)
    object Journal : Screen("journal", "Journal", Icons.Rounded.MenuBook)
    object FitnessHistory : Screen("fitnessHistory", "Fitness History", Icons.Rounded.BarChart)
    object Support : Screen("support", "Support", Icons.Rounded.HelpOutline)
    object PrivacyPolicy : Screen("privacyPolicy", "Privacy Policy", Icons.Rounded.Shield)

    // Meditation sub-screens
    object MorningCalm : Screen("meditation/morning_calm", "Morning Calm", Icons.Rounded.WbSunny)
    object BreathingExercise : Screen("meditation/breathing", "Breathing Exercise", Icons.Rounded.Air)
    object SleepMeditation : Screen("meditation/sleep", "Sleep Meditation", Icons.Rounded.NightsStay)
    object MeditationTimer : Screen("meditation/timer/{exerciseId}/{category}", "Timer", Icons.Rounded.Timer) {
        fun createRoute(exerciseId: String, category: String) = "meditation/timer/$exerciseId/$category"
    }

    companion object {
        val bottomNavItems = listOf(Dashboard, Sleep, Wellness, Streaks)
    }
}
