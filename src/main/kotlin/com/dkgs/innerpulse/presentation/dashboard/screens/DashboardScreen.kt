package com.dkgs.innerpulse.presentation.dashboard.screens

import android.content.res.Configuration
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.dkgs.innerpulse.MockData
import com.dkgs.innerpulse.PreviewData
import com.dkgs.innerpulse.presentation.dashboard.DashboardUiState
import com.dkgs.innerpulse.presentation.dashboard.DashboardViewModel
import com.dkgs.innerpulse.presentation.dashboard.SmartRingViewModel
import com.dkgs.innerpulse.domain.model.Ring
import com.dkgs.innerpulse.domain.model.RingConnectionState
import com.dkgs.innerpulse.domain.model.AppTheme
import com.dkgs.innerpulse.presentation.navigation.Screen
import com.dkgs.innerpulse.presentation.theme.ThemeViewModel
import com.dkgs.innerpulse.ui.components.*
import com.dkgs.innerpulse.ui.theme.*
import com.dkgs.innerpulse.R

// ═══════════════════════════════════════════════════════════════════════
// PREMIUM DASHBOARD — Cinematic Silicon Valley Health-Tech
// ═══════════════════════════════════════════════════════════════════════

@Composable
fun DashboardRoute(
    viewModel: DashboardViewModel = viewModel(),
    smartRingViewModel: SmartRingViewModel = viewModel(),
    navController: NavController? = null,
    themeViewModel: ThemeViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val ringConnectionState by smartRingViewModel.connectionState.collectAsState()
    val currentTheme by themeViewModel.themeState.collectAsState()
    
    val isUsingPhone by viewModel.isUsingPhone.collectAsState()
    val isSupported by viewModel.stepTrackingSupported.collectAsState()

    val lifecycleOwner = LocalLifecycleOwner.current

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.startPhoneStepTracking()
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    permissionLauncher.launch(Manifest.permission.ACTIVITY_RECOGNITION)
                } else {
                    viewModel.startPhoneStepTracking()
                }
            } else if (event == Lifecycle.Event.ON_PAUSE) {
                viewModel.stopPhoneStepTracking()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            viewModel.stopPhoneStepTracking()
        }
    }

    DashboardScreenWithHeader(
        state = state,
        ringConnectionState = ringConnectionState,
        onConnectClick = {
            navController?.navigate("ringSetup")
        },
        onDisconnectClick = {
            smartRingViewModel.disconnectRing()
        },
        onSettingsClick = {
            navController?.navigate(Screen.Settings.route)
        },
        onFitnessHistoryClick = {
            navController?.navigate(Screen.FitnessHistory.route)
        },
        onMeasureHeartRate = {
            if (state.heartRateMeasuring) viewModel.stopHeartRateMeasurement()
            else viewModel.startHeartRateMeasurement()
        },
        onMeasureSpO2 = {
            if (state.spO2Measuring) viewModel.stopSpO2Measurement()
            else viewModel.startSpO2Measurement()
        },
        onMeasureBloodPressure = {
            if (state.bloodPressureMeasuring) viewModel.stopBloodPressureMeasurement()
            else viewModel.startBloodPressureMeasurement()
        },
        onMeasureStress = {
            if (state.stressMeasuring) viewModel.stopStressMeasurement()
            else viewModel.startStressMeasurement()
        },
        onSyncSleep = {
            viewModel.requestSleepHistory()
        },
        onSyncAllData = {
            viewModel.syncAllData()
        },
        currentTheme = currentTheme,
        onThemeChange = { themeViewModel.setTheme(it) },
        isUsingPhone = isUsingPhone,
        isSupported = isSupported
    )
}

@Composable
fun DashboardScreenWithHeader(
    state: DashboardUiState,
    ringConnectionState: RingConnectionState = if (state.isConnected) RingConnectionState.CONNECTED else RingConnectionState.DISCONNECTED,
    onConnectClick: () -> Unit = {},
    onDisconnectClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
    onFitnessHistoryClick: () -> Unit = {},
    onMeasureHeartRate: () -> Unit = {},
    onMeasureSpO2: () -> Unit = {},
    onMeasureBloodPressure: () -> Unit = {},
    onMeasureStress: () -> Unit = {},
    onSyncSleep: () -> Unit = {},
    onSyncAllData: () -> Unit = {},
    currentTheme: AppTheme = AppTheme.SYSTEM,
    onThemeChange: (AppTheme) -> Unit = {},
    isUsingPhone: Boolean = false,
    isSupported: Boolean = true
) {
    val stressLevel = state.stressLevel.coerceIn(0, 100)
    val pairedRing = state.connectedRing
    val isConnected = ringConnectionState == RingConnectionState.CONNECTED

    Box(modifier = Modifier.fillMaxSize()) {
        if (AppColors.isDark) {
            CinematicBackground()
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFFF8FAFF),  // very light blue-white at top
                                Color(0xFFF1F5F9)   // light cool grey at bottom
                            )
                        )
                    )
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .verticalScroll(rememberScrollState())
        ) {
            // Branded Top Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left-aligned Brand Identity
                BrandedTopBar(
                    painter = painterResource(id = R.drawable.ic_premium_logo),
                    title = "INNERPULSE",
                    isConnected = isConnected,
                    batteryLevel = state.batteryLevel,
                    modifier = Modifier.weight(1f)
                )

                // Settings & Theme Utility Bar (Top Right)
                Row(
                    modifier = Modifier.padding(end = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Theme toggle
                    Box {
                        var themeMenuExpanded by remember { mutableStateOf(false) }

                        IconButton(onClick = { themeMenuExpanded = true }) {
                            Icon(
                                imageVector = when (currentTheme) {
                                    AppTheme.DARK -> Icons.Default.DarkMode
                                    AppTheme.LIGHT -> Icons.Default.LightMode
                                    AppTheme.SYSTEM -> Icons.Default.BrightnessAuto
                                },
                                contentDescription = "Theme",
                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        DropdownMenu(
                            expanded = themeMenuExpanded,
                            onDismissRequest = { themeMenuExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Dark Mode") },
                                onClick = {
                                    onThemeChange(AppTheme.DARK)
                                    themeMenuExpanded = false
                                },
                                leadingIcon = { Icon(Icons.Default.DarkMode, contentDescription = null) }
                            )
                            DropdownMenuItem(
                                text = { Text("Light Mode") },
                                onClick = {
                                    onThemeChange(AppTheme.LIGHT)
                                    themeMenuExpanded = false
                                },
                                leadingIcon = { Icon(Icons.Default.LightMode, contentDescription = null) }
                            )
                            DropdownMenuItem(
                                text = { Text("System Default") },
                                onClick = {
                                    onThemeChange(AppTheme.SYSTEM)
                                    themeMenuExpanded = false
                                },
                                leadingIcon = { Icon(Icons.Default.BrightnessAuto, contentDescription = null) }
                            )
                        }
                    }

                    IconButton(onClick = onSettingsClick) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }

            // Connectivity Status Banner
            if (isConnected) {
                ConnectivityBanner(
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                )
            } else if (ringConnectionState == RingConnectionState.CONNECTING) {
                ConnectivityBanner(
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                    isConnecting = true
                )
            }

            // Hero Section
            HeroDashboardHeader(
                pairedRing = pairedRing,
                isConnected = isConnected,
                batteryLevel = state.batteryLevel,
                ringConnectionState = ringConnectionState,
                onConnectClick = onConnectClick,
                onDisconnectClick = onDisconnectClick
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Health Metrics Grid
            Column(
                modifier = Modifier.padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Section header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "HEALTH METRICS",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    StatusBadge(
                        text = if (isConnected) "Live" else "Offline",
                        color = if (isConnected) NeonGreen else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                }

                // 2-column metric grid
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    FloatingMetricTile(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.Favorite,
                        label = "HEART RATE",
                        value = if (!isConnected) "--" else if (state.heartRate > 0) "${state.heartRate}" else "--",
                        unit = "bpm",
                        progress = if (isConnected) (state.heartRate / 200f).coerceIn(0f, 1f) else 0f,
                        gradientColors = listOf(ErrorRed, NeonPink),
                        glowColor = ErrorRed,
                        iconBgColor = HeartRateIconBg
                    )
                    FloatingMetricTile(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.FavoriteBorder,
                        label = "BLOOD O₂",
                        value = if (!isConnected) "--" else if (state.spO2 > 0) "${state.spO2.toInt()}" else "--",
                        unit = "%",
                        progress = if (isConnected) (state.spO2 / 100f).coerceIn(0f, 1f) else 0f,
                        gradientColors = listOf(NeonCyan, NeonBlue),
                        glowColor = NeonCyan,
                        iconBgColor = BloodOxygenIconBg
                    )
                }

                if (isConnected) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        MeasurementButton(
                            text = if (state.heartRateMeasuring) "Measuring..." else "Measure HR",
                            icon = Icons.Default.Favorite,
                            color = ErrorRed,
                            onClick = onMeasureHeartRate,
                            enabled = true,
                            modifier = Modifier.weight(1f)
                        )
                        MeasurementButton(
                            text = if (state.spO2Measuring) "Measuring..." else "Measure SpO2",
                            icon = Icons.Default.FavoriteBorder,
                            color = NeonCyan,
                            onClick = onMeasureSpO2,
                            enabled = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                if (isConnected) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        FloatingMetricTile(
                            modifier = Modifier.weight(1f),
                            icon = Icons.Default.MonitorHeart,
                            label = "BLOOD PRESSURE",
                            value = if (state.bloodPressureMeasuring && state.bloodPressureHeartRate > 0) "HR: ${state.bloodPressureHeartRate}" else "${state.bloodPressureSystolic}/${state.bloodPressureDiastolic}",
                            unit = "mmHg",
                            progress = 0.7f,
                            gradientColors = listOf(NeonOrange, ErrorRed),
                            glowColor = NeonOrange,
                            iconBgColor = HeartRateIconBg
                        )
                        
                        // Placeholder for Body Temp if Type 1
                        if (state.ringType == 1) {
                            FloatingMetricTile(
                                modifier = Modifier.weight(1f),
                                icon = Icons.Default.Thermostat,
                                label = "BODY TEMP",
                                value = "36.5", // Static for now as SDK integration for temp is future
                                unit = "°C",
                                progress = 0.5f,
                                gradientColors = listOf(NeonCyan, NeonGreen),
                                glowColor = NeonCyan,
                                iconBgColor = BloodOxygenIconBg
                            )
                        } else {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        MeasurementButton(
                            text = if (state.bloodPressureMeasuring) (if (state.bloodPressureHeartRate > 0) "Measuring (${state.bloodPressureHeartRate})..." else "Measuring...") else "Measure BP",
                            icon = Icons.Default.MonitorHeart,
                            color = NeonOrange,
                            onClick = onMeasureBloodPressure,
                            enabled = true,
                            modifier = Modifier.weight(1f)
                        )
                        if (state.ringType == 1) {
                            Spacer(modifier = Modifier.weight(1f)) // Temp measurement not implemented
                        } else {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    val stepsValueStr = when {
                        isConnected -> "${state.steps}"
                        isUsingPhone && isSupported -> "${state.steps}"
                        isUsingPhone && !isSupported -> "N/A"
                        else -> "0"
                    }
                    val distanceValueStr = when {
                        isConnected -> "${state.distance}"
                        isUsingPhone && isSupported -> "${state.distance}"
                        isUsingPhone && !isSupported -> "N/A"
                        else -> "0"
                    }
                    val stepsLabel = if (isUsingPhone) "STEPS (PHONE)" else "STEPS"
                    val distanceLabel = if (isUsingPhone) "DISTANCE (PHONE)" else "DISTANCE"

                    FloatingMetricTile(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.Star,
                        label = stepsLabel,
                        value = stepsValueStr,
                        unit = "",
                        progress = (state.steps / 10000f).coerceIn(0f, 1f),
                        gradientColors = listOf(PrimaryPurple, NeonPink),
                        glowColor = PrimaryPurple,
                        iconBgColor = StepsIconBg,
                        onClick = onFitnessHistoryClick
                    )
                    FloatingMetricTile(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.Build,
                        label = distanceLabel,
                        value = distanceValueStr,
                        unit = "m",
                        progress = (state.distance / 5000f).coerceIn(0f, 1f),
                        gradientColors = listOf(NeonOrange, WarningAmber),
                        glowColor = NeonOrange,
                        iconBgColor = DistanceIconBg,
                        onClick = onFitnessHistoryClick
                    )
                }

                // Stress Card
                MetricGlassCard(modifier = Modifier.padding(horizontal = 0.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Left: Icon in circular background
                        Box(
                            modifier = Modifier.size(56.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            if (!AppColors.isDark) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(CircleShape)
                                        .background(StressIconBg)
                                )
                            }
                            Icon(
                                imageVector = Icons.Default.Face,
                                contentDescription = null,
                                tint = getStressColor(stressLevel),
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        
                        Spacer(modifier = Modifier.width(16.dp))
                        
                        // Middle: Text section
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "STRESS LEVEL",
                                style = MaterialTheme.typography.labelMedium,
                                color = if (AppColors.isDark) MaterialTheme.colorScheme.onSurfaceVariant else MetricLabelGray,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "$stressLevel",
                                style = MaterialTheme.typography.headlineMedium,
                                color = if (AppColors.isDark) MaterialTheme.colorScheme.onSurface else MetricValueDark,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        
                        // Right: Status chip
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = if (AppColors.isDark) getStressColor(stressLevel).copy(alpha = 0.15f) else StressStatusBg
                        ) {
                            Text(
                                text = getStressStatus(stressLevel),
                                style = MaterialTheme.typography.labelMedium,
                                color = if (AppColors.isDark) getStressColor(stressLevel) else StressStatusText,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                            )
                        }
                    }
                    
                    if (isConnected) {
                        Spacer(modifier = Modifier.height(12.dp))
                        MeasurementButton(
                            text = if (state.stressMeasuring) "Measuring Stress..." else "Measure Stress Level",
                            icon = Icons.Default.PlayArrow,
                            color = NeonCyan,
                            onClick = onMeasureStress,
                            enabled = true
                        )
                    }
                }

                // Sleep Card
                SleepCard(
                    sleepData = state.sleepData,
                    onRequestSleep = onSyncSleep
                )

                // Battery card
                state.batteryLevel?.let { battery ->
                    PremiumBatteryCard(
                        battery = battery,
                        isCharging = state.isCharging
                    )
                }

                // Daily Summary
                DailySummaryCard()

                Spacer(modifier = Modifier.height(20.dp))

                // Weekly Emotions Chart
                WeeklyEmotionsChart()

                Spacer(modifier = Modifier.height(16.dp))

                // Daily Insights
                DailyInsightsCard()

                Spacer(modifier = Modifier.height(16.dp))

                // Firmware Info
                state.firmwareInfo?.let { 
                    FirmwareCard(firmwareInfo = it)
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                NeonButton(
                    text = "SYNC ALL DATA / GET SCORES",
                    onClick = onSyncAllData,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    colors = listOf(PrimaryPurple, NeonBlue)
                )

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════
// HERO SECTION — Animated ring + status
// ═══════════════════════════════════════════════════════════════════════

@Composable
private fun HeroDashboardHeader(
    pairedRing: Ring?,
    isConnected: Boolean,
    batteryLevel: Int?,
    ringConnectionState: RingConnectionState = if (isConnected) RingConnectionState.CONNECTED else RingConnectionState.DISCONNECTED,
    onConnectClick: () -> Unit = {},
    onDisconnectClick: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Small ring animation
        AnimatedRing3D(
            modifier = Modifier.size(100.dp),
            primaryColor = if (isConnected) (if (AppColors.isDark) NeonCyan else LightSecondary) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            secondaryColor = if (isConnected) (if (AppColors.isDark) PrimaryPurple else LightPrimary) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f).copy(alpha = 0.5f),
            isConnected = isConnected
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Smart Ring Card with connection management
        SmartRingCard(
            connectionState = ringConnectionState,
            ringName = pairedRing?.name ?: "Smart Ring",
            batteryLevel = batteryLevel,
            onConnectClick = onConnectClick,
            onDisconnectClick = onDisconnectClick,
            modifier = Modifier.padding(horizontal = 0.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        HorizontalDivider(
            color = if (AppColors.isDark) SkyBlue.copy(alpha = 0.35f) else LightBorderSubtle,
            thickness = 1.dp
        )
    }
}


// ═══════════════════════════════════════════════════════════════════════
// WEEKLY EMOTIONS CHART — Canvas bar + line chart
// ═══════════════════════════════════════════════════════════════════════

@Composable
fun WeeklyEmotionsChart() {
    val moodData = listOf(
        Triple("Mon", 65f, "Good"),
        Triple("Tue", 72f, "Great"),
        Triple("Wed", 58f, "Okay"),
        Triple("Thu", 85f, "Exc."),
        Triple("Fri", 78f, "Great"),
        Triple("Sat", 92f, "Exc."),
        Triple("Sun", 75f, "Great")
    )

    val animProgress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        animProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(1400, easing = FastOutSlowInEasing)
        )
    }

    // 3D Glass card shell
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .shadow(elevation = 16.dp, shape = RoundedCornerShape(24.dp), ambientColor = NeonCyan.copy(alpha = 0.25f), spotColor = NeonCyan.copy(alpha = 0.3f))
            .clip(RoundedCornerShape(24.dp))
            .background(
                AppColors.sectionGradient(NeonCyan)
            )
            .border(
                1.5.dp,
                AppColors.sectionBorder(NeonCyan),
                RoundedCornerShape(24.dp)
            )
            .padding(20.dp)
    ) {
        Column {
            // Header
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(NeonCyan))
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "WEEKLY EMOTIONS TREND",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 1.sp
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
            ) {
                val w = size.width
                val h = size.height
                val padBottom = 36f
                val padTop = 16f
                val chartH = h - padBottom - padTop
                val slotW = w / moodData.size
                val barW = slotW * 0.5f

                // Grid lines
                for (i in 0..4) {
                    val y = padTop + chartH * i / 4f
                    drawLine(NeonCyan.copy(alpha = 0.1f), Offset(0f, y), Offset(w, y), strokeWidth = 1.5f)
                }

                val points = mutableListOf<Offset>()
                moodData.forEachIndexed { i, (_, value, _) ->
                    val x = slotW * i + slotW / 2f
                    val barH = chartH * (value / 100f) * animProgress.value
                    val top = padTop + chartH - barH
                    val bottom = padTop + chartH

                    // Bar with rounded top
                    drawRect(
                        brush = Brush.verticalGradient(
                            colors = if (AppColors.isDark) {
                                listOf(NeonCyan.copy(alpha = 0.7f), NeonBlue.copy(alpha = 0.3f))
                            } else {
                                listOf(LightSecondary.copy(alpha = 0.7f), LightPrimary.copy(alpha = 0.3f))
                            },
                            startY = top, endY = bottom
                        ),
                        topLeft = Offset(x - barW / 2f, top),
                        size = androidx.compose.ui.geometry.Size(barW, barH)
                    )

                    val dotY = padTop + chartH * (1f - value / 100f * animProgress.value)
                    points.add(Offset(x, dotY))
                }

                // Trend line
                if (points.size > 1) {
                    val path = Path().apply {
                        moveTo(points[0].x, points[0].y)
                        for (j in 1 until points.size) {
                            val cx = (points[j - 1].x + points[j].x) / 2f
                            cubicTo(cx, points[j - 1].y, cx, points[j].y, points[j].x, points[j].y)
                        }
                    }
                    drawPath(path, NeonCyan, style = Stroke(width = 3f, cap = StrokeCap.Round))
                }

                // Dots
                points.forEach { pt ->
                    drawCircle(NeonCyan, radius = 6f, center = pt)
                    drawCircle(Color(0xFF000000), radius = 3f, center = pt)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Day labels with mood %
            Row(modifier = Modifier.fillMaxWidth()) {
                moodData.forEach { (day, value, _) ->
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = day,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "${value.toInt()}%",
                            fontSize = 10.sp,
                            color = NeonCyan.copy(alpha = 0.8f),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Legend
            Row(
                horizontalArrangement = Arrangement.spacedBy(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(NeonCyan))
                    Text(text = "Mood Level", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(modifier = Modifier
                        .height(3.dp)
                        .width(24.dp)
                        .background(Brush.horizontalGradient(listOf(NeonCyan, NeonBlue)))
                    )
                    Text(text = "Trend", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
fun DailyInsightsCard() {
    data class Insight(val icon: ImageVector, val label: String, val value: String, val color: Color)
    val insights = listOf(
        Insight(Icons.Rounded.ElectricBolt, "Active Time",     "45 mins",  NeonCyan),
        Insight(Icons.Rounded.LocalFireDepartment, "Calories Burned",  "524 kcal", NeonOrange),
        Insight(Icons.Rounded.SelfImprovement, "Meditation",      "12 mins",  PrimaryPurple),
        Insight(Icons.Rounded.DirectionsRun, "Steps Taken",     "8,432",    NeonGreen),
        Insight(Icons.Rounded.Favorite, "Heart Rate Avg",  "72 bpm",   NeonPink)
    )

    // 3D Glass card
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .shadow(elevation = 16.dp, shape = RoundedCornerShape(24.dp), ambientColor = PrimaryPurple.copy(alpha = 0.25f), spotColor = PrimaryPurple.copy(alpha = 0.3f))
            .clip(RoundedCornerShape(24.dp))
            .background(
                AppColors.sectionGradient(PrimaryPurple)
            )
            .border(
                1.5.dp,
                AppColors.sectionBorder(PrimaryPurple),
                RoundedCornerShape(24.dp)
            )
            .padding(20.dp)
    ) {
        Column {
            // Header
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(PrimaryPurple))
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "DAILY INSIGHTS",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 1.sp
                )
            }
            Spacer(modifier = Modifier.height(16.dp))

            insights.forEachIndexed { idx, insight ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Color dot
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(insight.color)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Icon(
                        imageVector = insight.icon,
                        contentDescription = insight.label,
                        modifier = Modifier.size(20.dp),
                        tint = insight.color
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = insight.label,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f),
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = insight.value,
                        fontSize = 16.sp,
                        color = insight.color,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
                if (idx < insights.lastIndex) {
                    HorizontalDivider(
                        color = AppColors.dividerColor,
                        thickness = 0.5.dp
                    )
                }
            }
        }
    }
}


// ═══════════════════════════════════════════════════════════════════════
// PREMIUM BATTERY CARD — Circular gauge + glow
// ═══════════════════════════════════════════════════════════════════════

@Composable
fun PremiumBatteryCard(battery: Int, isCharging: Boolean = false) {
    val batteryColor = when {
        isCharging -> NeonGreen
        battery > 50 -> NeonGreen
        battery > 20 -> NeonOrange
        else -> ErrorRed
    }

    NeonGlassCard(glowColor = batteryColor) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(56.dp),
                contentAlignment = Alignment.Center
            ) {
                AnimatedRadialChart(
                    modifier = Modifier.fillMaxSize(),
                    progress = battery / 100f,
                    gradientColors = listOf(batteryColor, batteryColor.copy(alpha = 0.5f)),
                    strokeWidth = 6f,
                    glowRadius = 4f
                )
                Text(
                    text = "$battery",
                    style = MaterialTheme.typography.titleSmall,
                    color = batteryColor,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "RING BATTERY",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = "$battery%",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = when {
                            isCharging -> "Charging..."
                            battery > 80 -> "Fully charged"
                            battery > 50 -> "Good"
                            battery > 20 -> "Low"
                            else -> "Critical"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = batteryColor,
                        modifier = Modifier.padding(bottom = 3.dp)
                    )
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════
// DAILY SUMMARY CARD — Activity overview
// ═══════════════════════════════════════════════════════════════════════

@Composable
fun DailySummaryCard() {
    MetricGlassCard(modifier = Modifier.padding(horizontal = 0.dp)) {
        Text(
            text = "DAILY SUMMARY",
            style = MaterialTheme.typography.labelMedium,
            color = if (AppColors.isDark) MaterialTheme.colorScheme.onSurfaceVariant else MetricLabelGray,
            fontWeight = FontWeight.Medium
        )
        Spacer(modifier = Modifier.height(16.dp))
        
        SummaryRow(icon = Icons.Rounded.DirectionsRun, label = "Distance", value = "5.2 km")
        DividerRow()
        SummaryRow(icon = Icons.Rounded.Timer, label = "Active Time", value = "1h 23m")
        DividerRow()
        SummaryRow(icon = Icons.Rounded.Favorite, label = "Avg Heart Rate", value = "68 bpm")
        DividerRow()
        SummaryRow(icon = Icons.Rounded.Bedtime, label = "Sleep Score", value = "85%")
    }
}

@Composable
private fun DividerRow() {
    HorizontalDivider(
        modifier = Modifier.padding(vertical = 12.dp),
        thickness = 1.dp,
        color = if (AppColors.isDark) AppColors.dividerColor else MetricDividerColor
    )
}

@Composable
private fun SummaryRow(icon: ImageVector, label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Icon + Label
        Icon(
            imageVector = icon,
            contentDescription = label,
            modifier = Modifier.size(20.dp),
            tint = if (AppColors.isDark) NeonCyan else PrimaryPurple
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = if (AppColors.isDark) MaterialTheme.colorScheme.onSurfaceVariant else MetricLabelGray,
            modifier = Modifier.weight(1f),
            fontWeight = FontWeight.Medium
        )
        // Value Text
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            color = if (AppColors.isDark) MaterialTheme.colorScheme.onSurface else MetricValueDark,
            fontWeight = FontWeight.Bold
        )
    }
}

// ═══════════════════════════════════════════════════════════════════════
// HELPER FUNCTIONS
// ═══════════════════════════════════════════════════════════════════════

fun getStressColor(level: Int): Color = when {
    level <= 30 -> NeonGreen
    level <= 60 -> NeonOrange
    else -> ErrorRed
}

fun getStressStatus(level: Int): String = when {
    level <= 30 -> "Low"
    level <= 60 -> "Moderate"
    else -> "High"
}

// ═══════════════════════════════════════════════════════════════════════
// NEW COMPONENTS FROM SETUP SCREEN
// ═══════════════════════════════════════════════════════════════════════

@Composable
fun MeasurementButton(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    onClick: () -> Unit,
    enabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(44.dp),
        enabled = enabled,
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = color.copy(alpha = 0.15f),
            disabledContainerColor = color.copy(alpha = 0.08f)
        ),
        contentPadding = PaddingValues(horizontal = 16.dp)
    ) {
        if (!enabled) {
            CircularProgressIndicator(
                modifier = Modifier.size(16.dp),
                color = color,
                strokeWidth = 2.dp
            )
            Spacer(modifier = Modifier.width(8.dp))
        } else {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = color
            )
            Spacer(modifier = Modifier.width(8.dp))
        }
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            color = if (enabled) color else color.copy(alpha = 0.5f)
        )
    }
}

@Composable
fun SleepCard(
    sleepData: com.dkgs.innerpulse.domain.model.SleepData?,
    onRequestSleep: () -> Unit
) {
    NeonGlassCard(glowColor = NeonPurple.copy(alpha = 0.6f)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(NeonPurple.copy(alpha = 0.2f), NeonPurple.copy(alpha = 0.03f))
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.DateRange,
                    contentDescription = null,
                    tint = NeonPurple,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "SLEEP",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                if (sleepData != null && sleepData.totalMinutes > 0) {
                    val hours = sleepData.totalMinutes / 60
                    val mins = sleepData.totalMinutes % 60
                    Text(
                        text = "${hours}h ${mins}m",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        SleepMetric("Deep", "${sleepData.deepMinutes}m", NeonPurple)
                        SleepMetric("Light", "${sleepData.lightMinutes}m", NeonCyan)
                        SleepMetric("Awake", "${sleepData.awakeMinutes}m", NeonOrange)
                    }
                    if (sleepData.quality > 0) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Quality: ",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            StatusBadge(
                                text = "${sleepData.quality}%",
                                color = if (sleepData.quality >= 70) NeonGreen else NeonOrange
                            )
                        }
                    }
                } else {
                    Text(
                        text = "No data",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            FilledTonalIconButton(
                onClick = onRequestSleep,
                colors = IconButtonDefaults.filledTonalIconButtonColors(
                    containerColor = NeonPurple.copy(alpha = 0.1f),
                    contentColor = NeonPurple
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Refresh Sleep",
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
fun SleepMetric(
    label: String,
    value: String,
    color: Color
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleSmall,
            color = color,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
        )
    }
}

@Composable
fun FirmwareCard(firmwareInfo: com.dkgs.innerpulse.domain.model.FirmwareInfo) {
    NeonGlassCard(
        glowColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
        showGlow = false
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f).copy(alpha = 1f), Color.Transparent)
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Build,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "FIRMWARE",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = firmwareInfo.displayText,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold
                )
                if (firmwareInfo.type.isNotEmpty()) {
                    Text(
                        text = "Type: ${firmwareInfo.type}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════
// ROUTE — ViewModel-owning wrapper (use in navigation)
// ═══════════════════════════════════════════════════════════════════════
// DashboardRoute is already defined above at line 41

// Legacy compatibility
@Composable
fun DashboardScreen() {
    DashboardScreenWithHeader(
        state = DashboardUiState(),
        ringConnectionState = RingConnectionState.DISCONNECTED
    )
}

// ═══════════════════════════════════════════════════════════════════════
// PREVIEWS — Multi-state, ViewModel-free
// ═══════════════════════════════════════════════════════════════════════

@Preview(name = "Connected", showBackground = true, backgroundColor = 0xFF050508, device = Devices.PIXEL_6)
@Composable
private fun DashboardConnectedPreview() {
    FitnessAppTheme(darkTheme = true) {
        DashboardScreenWithHeader(state = PreviewData.connectedDashboardState)
    }
}

@Preview(name = "Disconnected", showBackground = true, backgroundColor = 0xFF050508, device = Devices.PIXEL_6)
@Composable
private fun DashboardDisconnectedPreview() {
    FitnessAppTheme(darkTheme = true) {
        DashboardScreenWithHeader(state = PreviewData.disconnectedDashboardState)
    }
}

@Preview(name = "High Stress", showBackground = true, backgroundColor = 0xFF050508)
@Composable
private fun DashboardHighStressPreview() {
    FitnessAppTheme(darkTheme = true) {
        DashboardScreenWithHeader(state = PreviewData.highStressDashboardState)
    }
}

@Preview(name = "Low Battery", showBackground = true, backgroundColor = 0xFF050508)
@Composable
private fun DashboardLowBatteryPreview() {
    FitnessAppTheme(darkTheme = true) {
        DashboardScreenWithHeader(state = PreviewData.lowBatteryDashboardState)
    }
}

@Preview(name = "Dark Mode", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Composable
private fun DashboardDarkModePreview() {
    FitnessAppTheme(darkTheme = true) {
        DashboardScreenWithHeader(state = PreviewData.connectedDashboardState)
    }
}

@Preview(name = "Tablet", showBackground = true, backgroundColor = 0xFF050508, device = Devices.TABLET)
@Composable
private fun DashboardTabletPreview() {
    FitnessAppTheme(darkTheme = true) {
        DashboardScreenWithHeader(state = PreviewData.connectedDashboardState)
    }
}

@Preview(name = "Landscape", showBackground = true, backgroundColor = 0xFF050508, widthDp = 800, heightDp = 400)
@Composable
private fun DashboardLandscapePreview() {
    FitnessAppTheme(darkTheme = true) {
        DashboardScreenWithHeader(state = PreviewData.connectedDashboardState)
    }
}

@Preview(name = "Loading", showBackground = true, backgroundColor = 0xFF050508)
@Composable
private fun DashboardLoadingPreview() {
    FitnessAppTheme(darkTheme = true) {
        DashboardScreenWithHeader(state = PreviewData.loadingDashboardState)
    }
}
