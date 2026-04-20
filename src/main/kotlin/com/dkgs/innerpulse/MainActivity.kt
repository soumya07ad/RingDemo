package com.dkgs.innerpulse

import android.content.Context
import android.os.Bundle
import android.webkit.WebSettings
import android.webkit.WebView
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.dkgs.innerpulse.core.di.AppContainer
import com.dkgs.innerpulse.network.sync.SyncWorker
import com.dkgs.innerpulse.presentation.navigation.AppNavigationViewModel
import com.dkgs.innerpulse.presentation.dashboard.screens.DashboardRoute
import com.dkgs.innerpulse.presentation.dashboard.screens.FitnessHistoryRoute
import com.dkgs.innerpulse.presentation.dashboard.screens.SleepTrackerScreen
import com.dkgs.innerpulse.presentation.coach.screens.CoachScreen
import com.dkgs.innerpulse.presentation.dashboard.FitnessHistoryViewModel
import com.dkgs.innerpulse.presentation.wellness.screens.WellnessScreen
import com.dkgs.innerpulse.presentation.wellness.screens.MeditationListScreen
import com.dkgs.innerpulse.presentation.wellness.screens.MeditationTimerScreen
import com.dkgs.innerpulse.presentation.wellness.MeditationViewModel
import com.dkgs.innerpulse.presentation.navigation.Screen
import com.dkgs.innerpulse.presentation.dashboard.DashboardViewModel
import com.dkgs.innerpulse.presentation.dashboard.SleepTrackerViewModel
import com.dkgs.innerpulse.presentation.dashboard.SmartRingViewModel
import com.dkgs.innerpulse.presentation.coach.CoachViewModel
import com.dkgs.innerpulse.presentation.wellness.WellnessViewModel
import com.dkgs.innerpulse.presentation.streaks.StreakViewModel
import com.dkgs.innerpulse.presentation.settings.SettingsViewModel
import com.dkgs.innerpulse.presentation.settings.screens.SettingsScreen
import com.dkgs.innerpulse.presentation.settings.screens.SupportScreen
import com.dkgs.innerpulse.presentation.settings.screens.PrivacyPolicyScreen
import com.dkgs.innerpulse.presentation.theme.ThemeViewModel
import com.dkgs.innerpulse.domain.model.AppTheme
import com.dkgs.innerpulse.ui.theme.*
import com.dkgs.innerpulse.ui.components.AmbientPulseGlow
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.RoundedCornerShape

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        // Install splash screen BEFORE super.onCreate
        installSplashScreen()

        super.onCreate(savedInstanceState)

        // Initialize DI container (creates FitnessAPI, Retrofit, etc.)
        AppContainer.initialize(this)

        // Start background sync
        SyncWorker.scheduleSyncWorker(this)

        setContent {
            val factory = remember { AppContainer.getInstance(this).viewModelFactory }
            val themeViewModel: ThemeViewModel = viewModel(factory = factory)
            val appTheme by themeViewModel.themeState.collectAsState()

            val isDark = when (appTheme) {
                AppTheme.DARK -> true
                AppTheme.LIGHT -> false
                AppTheme.SYSTEM -> androidx.compose.foundation.isSystemInDarkTheme()
            }

            FitnessAppTheme(darkTheme = isDark) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavigationFlow(themeViewModel = themeViewModel)
                }
            }
        }
    }
}

@Composable
fun AppNavigationFlow(
    themeViewModel: ThemeViewModel = viewModel()
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val factory = remember { AppContainer.getInstance(context).viewModelFactory }
    val navViewModel: AppNavigationViewModel = viewModel(factory = factory)
    val navState by navViewModel.uiState.collectAsState()
    val navController = rememberNavController()

    // Don't show anything while loading persistent state
    if (navState.isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = NeonCyan)
        }
        return
    }

    when {
        !navState.userLoggedIn -> {
            com.dkgs.innerpulse.presentation.auth.screens.AuthNavGraph(
                viewModel = viewModel(factory = factory),
                onAuthSuccess = {
                    navViewModel.onLoginSuccess()
                }
            )
        }
        !navState.setupComplete -> {
            com.dkgs.innerpulse.presentation.ring.screens.RingSetupRoute(
                onSetupComplete = { navViewModel.onSetupComplete() },
                onSkip = { navViewModel.onSkip() }
            )
        }
        else -> {
            Scaffold(
                containerColor = MaterialTheme.colorScheme.background,
                bottomBar = { AppBottomNav(navController = navController) }
            ) { paddingValues ->
                NavHost(
                    navController = navController,
                    startDestination = Screen.Dashboard.route,
                    modifier = Modifier.padding(paddingValues)
                ) {
                    composable(Screen.Dashboard.route) {
                        DashboardRoute(
                            viewModel = viewModel(factory = factory),
                            smartRingViewModel = viewModel(factory = factory),
                            navController = navController,
                            themeViewModel = themeViewModel
                        )
                    }

                    composable(Screen.Sleep.route) {
                        SleepTrackerScreen(
                            viewModel = viewModel(factory = factory),
                            onBack = { navController.popBackStack() }
                        )
                    }

                    composable(Screen.Coach.route) {
                        CoachScreen(
                            viewModel = viewModel(factory = factory),
                            onBack = { navController.popBackStack() }
                        )
                    }

                    composable(Screen.Wellness.route) {
                        WellnessScreen(
                            viewModel = viewModel(factory = factory),
                            onBack = { navController.popBackStack() },
                            onMeditationClick = { category ->
                                val route = when (category) {
                                    "morning_calm" -> Screen.MorningCalm.route
                                    "breathing" -> Screen.BreathingExercise.route
                                    "sleep" -> Screen.SleepMeditation.route
                                    else -> Screen.MorningCalm.route
                                }
                                navController.navigate(route)
                            },
                            onJournalClick = { navController.navigate(Screen.Journal.route) }
                        )
                    }

                    composable(Screen.Journal.route) {
                        com.dkgs.innerpulse.presentation.wellness.screens.JournalScreen(
                            viewModel = viewModel(factory = factory),
                            onBack = { navController.popBackStack() }
                        )
                    }

                    // Meditation category screens
                    composable(Screen.MorningCalm.route) {
                        MeditationListScreen(
                            category = "morning_calm",
                            onExerciseClick = { exercise ->
                                navController.navigate(
                                    Screen.MeditationTimer.createRoute(exercise.id, exercise.category)
                                )
                            },
                            onBack = { navController.popBackStack() }
                        )
                    }

                    composable(Screen.BreathingExercise.route) {
                        MeditationListScreen(
                            category = "breathing",
                            onExerciseClick = { exercise ->
                                navController.navigate(
                                    Screen.MeditationTimer.createRoute(exercise.id, exercise.category)
                                )
                            },
                            onBack = { navController.popBackStack() }
                        )
                    }

                    composable(Screen.SleepMeditation.route) {
                        MeditationListScreen(
                            category = "sleep",
                            onExerciseClick = { exercise ->
                                navController.navigate(
                                    Screen.MeditationTimer.createRoute(exercise.id, exercise.category)
                                )
                            },
                            onBack = { navController.popBackStack() }
                        )
                    }

                    composable(
                        Screen.MeditationTimer.route,
                        arguments = listOf(
                            androidx.navigation.navArgument("exerciseId") { type = androidx.navigation.NavType.StringType },
                            androidx.navigation.navArgument("category") { type = androidx.navigation.NavType.StringType }
                        )
                    ) { backStackEntry ->
                        val exerciseId = backStackEntry.arguments?.getString("exerciseId") ?: ""
                        val category = backStackEntry.arguments?.getString("category") ?: ""
                        MeditationTimerScreen(
                            exerciseId = exerciseId,
                            category = category,
                            viewModel = viewModel(),
                            onBack = { navController.popBackStack() }
                        )
                    }

                    composable(Screen.Streaks.route) {
                        com.dkgs.innerpulse.presentation.streaks.screens.StreaksScreen(
                            viewModel = viewModel(factory = factory),
                            onBack = { navController.popBackStack() }
                        )
                    }

                    composable(Screen.Settings.route) {
                        SettingsScreen(
                            viewModel = viewModel(factory = factory),
                            onBack = { navController.popBackStack() },
                            onLogout = { navViewModel.onLogout() },
                            onNavigateToSupport = { navController.navigate(Screen.Support.route) },
                            onNavigateToPrivacy = { navController.navigate(Screen.PrivacyPolicy.route) }
                        )
                    }

                    composable(Screen.Support.route) {
                        SupportScreen(
                            onBack = { navController.popBackStack() }
                        )
                    }

                    composable(Screen.PrivacyPolicy.route) {
                        PrivacyPolicyScreen(
                            onBack = { navController.popBackStack() }
                        )
                    }

                    composable(Screen.FitnessHistory.route) {
                        FitnessHistoryRoute(
                            viewModel = viewModel(factory = factory),
                            onBack = { navController.popBackStack() }
                        )
                    }

                    composable("ringSetup") {
                        com.dkgs.innerpulse.presentation.ring.screens.RingSetupRoute(
                            onSetupComplete = { navController.popBackStack() },
                            onSkip = { navController.popBackStack() }
                        )
                    }
                }
            }
        }
    }
}




@Composable
fun PlaceholderScreen(screen: Screen) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = screen.icon,
                contentDescription = screen.label,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(16.dp))
            Text("${screen.label} coming soon...", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 18.sp)
        }
    }
}

@Composable
fun AppBottomNav(navController: NavHostController) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val isCoachSelected = currentRoute == Screen.Coach.route
    val isDark = AppColors.isDark

    val leftItems = listOf(Screen.Dashboard, Screen.Sleep)
    val rightItems = listOf(Screen.Wellness, Screen.Streaks)

    // Selection color
    val accentColor = if (isDark) NeonCyan else SkyBlue

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
    ) {
        // ── GLASS BOTTOM BAR ──
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(72.dp)
                .align(Alignment.BottomCenter)
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .shadow(
                    elevation = 16.dp,
                    shape = RoundedCornerShape(24.dp),
                    ambientColor = if (isDark) Color.Black else Color.Gray.copy(alpha = 0.2f)
                ),
            shape = RoundedCornerShape(24.dp),
            color = if (isDark) Color(0xE60A0A14) else Color(0xCCFFFFFF),
            border = BorderStroke(
                width = 0.5.dp,
                brush = Brush.linearGradient(
                    listOf(
                        Color.White.copy(alpha = 0.2f),
                        Color.Transparent,
                        accentColor.copy(alpha = 0.2f)
                    )
                )
            )
        ) {
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left items
                leftItems.forEach { screen ->
                    BottomNavItem(
                        screen = screen,
                        isSelected = currentRoute == screen.route,
                        onClick = {
                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }

                // Gap for the FAB
                Spacer(modifier = Modifier.width(60.dp))

                // Right items
                rightItems.forEach { screen ->
                    BottomNavItem(
                        screen = screen,
                        isSelected = currentRoute == screen.route,
                        onClick = {
                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        }

        // ── CENTER COACH FAB (AURA) ──
        val infiniteTransition = rememberInfiniteTransition(label = "auraGlow")
        val fabScale by animateFloatAsState(
            targetValue = if (isCoachSelected) 1.1f else 1f,
            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
            label = "fabScale"
        )

        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = (-12).dp)
                .size(64.dp),
            contentAlignment = Alignment.Center
        ) {
            // Ambient Aura Glow
            AmbientPulseGlow(
                color = if (isDark) NeonPurple else PrimaryPurple,
                size = 80.dp
            )

            Surface(
                modifier = Modifier
                    .size(56.dp)
                    .scale(fabScale)
                    .shadow(elevation = 12.dp, shape = CircleShape)
                    .clickable {
                        navController.navigate(Screen.Coach.route) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                shape = CircleShape,
                color = Color.Transparent
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(AppColors.accentGradient)
                        .border(1.5.dp, Color.White.copy(alpha = 0.4f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Screen.Coach.icon,
                            contentDescription = "AURA",
                            modifier = Modifier.size(22.dp),
                            tint = Color.White
                        )
                        Text(
                            text = "AURA",
                            fontSize = 8.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White,
                            letterSpacing = 0.5.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BottomNavItem(
    screen: Screen,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val isDark = AppColors.isDark
    val accentColor = if (isDark) NeonCyan else SkyBlue
    
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.1f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "navScale"
    )

    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 4.dp)
            .scale(scale),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(contentAlignment = Alignment.Center) {
            if (isSelected) {
                // Background glow for selected item
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(accentColor.copy(alpha = 0.15f), CircleShape)
                )
            }
            Icon(
                imageVector = screen.icon,
                contentDescription = screen.label,
                modifier = Modifier
                    .size(22.dp)
                    .alpha(if (isSelected) 1f else 0.6f),
                tint = if (isSelected) accentColor else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        Text(
            text = screen.label,
            fontSize = 10.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = if (isSelected) accentColor else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            letterSpacing = 0.2.sp
        )
    }
}

@Composable
fun FitnessWebView(onLoginSuccess: () -> Unit) {
    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { context ->
            WebView(context).apply {
                settings.apply {
                    javaScriptEnabled = true
                    mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                    domStorageEnabled = true
                    databaseEnabled = true
                    useWideViewPort = true
                    loadWithOverviewMode = true
                }
                // Add JavaScript bridge for login callback
                addJavascriptInterface(LoginBridge(onLoginSuccess), "LoginBridge")
                setBackgroundColor(0xFF050508.toInt())
                loadUrl("file:///android_asset/index.html?page=login")
            }
        }
    )
}

// Bridge class for communication between WebView and Kotlin
class LoginBridge(private val onLoginSuccess: () -> Unit) {
    @android.webkit.JavascriptInterface
    fun notifyLoginSuccess() {
        onLoginSuccess()
    }
}
