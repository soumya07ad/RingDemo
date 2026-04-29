package com.dkgs.innerpulse.presentation.ring.screens

import android.app.Activity
import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dkgs.innerpulse.PreviewData
import com.dkgs.innerpulse.domain.model.Ring
import com.dkgs.innerpulse.domain.model.SignalQuality
import com.dkgs.innerpulse.presentation.ring.PermissionUiState
import com.dkgs.innerpulse.presentation.ring.RingUiState
import com.dkgs.innerpulse.presentation.ring.RingViewModel
import com.dkgs.innerpulse.presentation.ring.components.*
import com.dkgs.innerpulse.ui.components.*
import com.dkgs.innerpulse.ui.theme.*
import androidx.compose.ui.unit.sp

// ═══════════════════════════════════════════════════════════════════════
// RING SETUP — Route / Screen / Preview
// ═══════════════════════════════════════════════════════════════════════

// ═══════════════════════════════════════════════════════════════════════
// ROUTE — ViewModel-owning wrapper (use in navigation)
// ═══════════════════════════════════════════════════════════════════════

@Composable
fun RingSetupRoute(
    onSetupComplete: () -> Unit,
    onSkip: () -> Unit,
    viewModel: RingViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    // Permission launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        viewModel.onPermissionResult(allGranted)
    }

    // Check permissions on launch
    LaunchedEffect(Unit) {
        viewModel.checkPermissions(context)
    }

    RingSetupScreen(
        uiState = uiState,
        onSetupComplete = onSetupComplete,
        onSkip = onSkip,
        onRequestPermission = {
            permissionLauncher.launch(viewModel.getRequiredPermissions())
        },
        onOpenSettings = {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", context.packageName, null)
            }
            (context as Activity).startActivity(intent)
        },
        onStartScan = { viewModel.startScan() },
        onStopScan = { viewModel.stopScan() },
        onRingTypeChange = { viewModel.updateRingType(it) },
        onManualEntry = { viewModel.toggleManualEntry() },
        onMacChange = { viewModel.updateManualMacAddress(it) },
        onConnectByMac = { viewModel.connectByMacAddress(uiState.manualMacAddress) },
        onDisconnect = { viewModel.disconnect() },
        onMeasureHeartRate = {
            if (uiState.ringData.heartRateMeasuring) viewModel.stopHeartRateMeasurement()
            else viewModel.startHeartRateMeasurement()
        },
        onMeasureSpO2 = {
            if (uiState.ringData.spO2Measuring) viewModel.stopSpO2Measurement()
            else viewModel.startSpO2Measurement()
        },
        onMeasureStress = { viewModel.startStressMeasurement() },
        onRequestSleep = { viewModel.requestSleepHistory() },
        onClearError = { viewModel.clearError() },
        onRefresh = { viewModel.refreshStatus(context) },
        onRemovePairedRing = { viewModel.removePairedRing(it) },
        onTogglePairedRingsList = { viewModel.togglePairedRingsList() },
        onDeviceSelected = { ring, type -> viewModel.connectToDevice(ring, type) },
        onShowManualEntry = { viewModel.showManualEntry() }
    )
}

// ═══════════════════════════════════════════════════════════════════════
// SCREEN — Pure UI composable (previewable, no ViewModel)
// ═══════════════════════════════════════════════════════════════════════

@Composable
fun RingSetupScreen(
    uiState: RingUiState,
    onSetupComplete: () -> Unit = {},
    onSkip: () -> Unit = {},
    onRequestPermission: () -> Unit = {},
    onOpenSettings: () -> Unit = {},
    onStartScan: () -> Unit = {},
    onStopScan: () -> Unit = {},
    onRingTypeChange: (Int) -> Unit = {},
    onDeviceSelected: (Ring, Int) -> Unit = { _, _ -> },
    onManualEntry: () -> Unit = {},
    onMacChange: (String) -> Unit = {},
    onConnectByMac: () -> Unit = {},
    onDisconnect: () -> Unit = {},
    onMeasureHeartRate: () -> Unit = {},
    onMeasureSpO2: () -> Unit = {},
    onMeasureStress: () -> Unit = {},
    onRequestSleep: () -> Unit = {},
    onClearError: () -> Unit = {},
    onRefresh: () -> Unit = {},
    onRemovePairedRing: (String) -> Unit = {},
    onTogglePairedRingsList: () -> Unit = {},
    onShowManualEntry: () -> Unit = {}
) {
    Box(modifier = Modifier.fillMaxSize()) {
        // Cinematic background
        CinematicBackground()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            // Hero header
            PremiumSetupHeader()

            Spacer(modifier = Modifier.height(24.dp))

            // Main content based on state
            when {
                !uiState.hasPermissions -> {
                    PermissionContent(
                        permissionState = uiState.permissionState,
                        isBluetoothEnabled = uiState.isBluetoothEnabled,
                        isLocationEnabled = uiState.isLocationEnabled,
                        onRequestPermission = onRequestPermission,
                        onOpenSettings = onOpenSettings,
                        onRefresh = onRefresh,
                        selectedRingType = uiState.selectedRingType,
                        onRingTypeChange = onRingTypeChange
                    )
                }
                uiState.showPairedRingsList && uiState.pairedRings.isNotEmpty() -> {
                    PairedDevicesContent(
                        pairedRings = uiState.pairedRings,
                        onRingSelected = { pairedRing -> 
                            onDeviceSelected(
                                Ring(macAddress = pairedRing.macAddress, name = pairedRing.name),
                                pairedRing.type
                            )
                        },
                        onRemoveDevice = onRemovePairedRing,
                        onAddNew = onShowManualEntry
                    )
                }
                uiState.showManualEntry -> {
                    ManualEntryContent(
                        macAddress = uiState.manualMacAddress,
                        selectedRingType = uiState.selectedRingType,
                        onMacChange = onMacChange,
                        onRingTypeChange = onRingTypeChange,
                        onConnect = onConnectByMac,
                        onBack = if (uiState.pairedRings.isNotEmpty()) onTogglePairedRingsList else onManualEntry
                    )
                }
                uiState.isConnecting -> {
                    ConnectingContent()
                }
                uiState.isConnected -> {
                    ConnectedContent(
                        uiState = uiState,
                        onDisconnect = onDisconnect,
                        onDone = onSetupComplete,
                        onMeasureHeartRate = onMeasureHeartRate,
                        onMeasureSpO2 = onMeasureSpO2,
                        onMeasureStress = onMeasureStress,
                        onRequestSleep = onRequestSleep
                    )
                }
                else -> {
                    ScanContent(
                        uiState = uiState,
                        onStartScan = onStartScan,
                        onStopScan = onStopScan,
                        onRingTypeChange = onRingTypeChange,
                        onDeviceSelected = { ring -> onDeviceSelected(ring, uiState.selectedRingType) },
                        onManualEntry = onManualEntry,
                        onSkip = onSkip,
                        onShowPaired = onTogglePairedRingsList
                    )
                }
            }
        }

        // Error snackbar
        uiState.errorMessage?.let { error ->
            Snackbar(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp),
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface,
                action = {
                    TextButton(onClick = onClearError) {
                        Text("DISMISS", color = NeonCyan)
                    }
                }
            ) {
                Text(error)
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════
// HERO HEADER — Animated ring + title
// ═══════════════════════════════════════════════════════════════════════

@Composable
private fun PremiumSetupHeader() {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 3D Ring Animation
        AnimatedRing3D(
            modifier = Modifier.size(140.dp),
            primaryColor = NeonCyan,
            secondaryColor = PrimaryPurple
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "SMART RING",
            style = MaterialTheme.typography.labelLarge,
            color = NeonCyan,
            letterSpacing = 4.sp
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Setup",
            style = MaterialTheme.typography.displaySmall,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Connect your ring to unlock health insights",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

// ═══════════════════════════════════════════════════════════════════════
// PERMISSION CONTENT
// ═══════════════════════════════════════════════════════════════════════

@Composable
private fun PermissionContent(
    permissionState: PermissionUiState,
    isBluetoothEnabled: Boolean,
    isLocationEnabled: Boolean,
    onRequestPermission: () -> Unit,
    onOpenSettings: () -> Unit,
    onRefresh: () -> Unit,
    selectedRingType: Int,
    onRingTypeChange: (Int) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(32.dp))

        // Ring Type Selector (Added to match Demo SDK logic)
        RingTypeSelector(
            selectedType = selectedRingType,
            onTypeChange = onRingTypeChange
        )

        Spacer(modifier = Modifier.height(24.dp))

        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = when (permissionState) {
                            PermissionUiState.PermanentlyDenied -> listOf(ErrorRed.copy(alpha = 0.2f), ErrorRed.copy(alpha = 0.03f))
                            PermissionUiState.Denied -> listOf(NeonOrange.copy(alpha = 0.2f), NeonOrange.copy(alpha = 0.03f))
                            else -> listOf(NeonCyan.copy(alpha = 0.2f), NeonCyan.copy(alpha = 0.03f))
                        }
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = when (permissionState) {
                    PermissionUiState.PermanentlyDenied -> Icons.Default.Lock
                    PermissionUiState.Denied -> Icons.Default.Warning
                    else -> Icons.Default.Settings
                },
                contentDescription = null,
                tint = when (permissionState) {
                    PermissionUiState.PermanentlyDenied -> ErrorRed
                    PermissionUiState.Denied -> NeonOrange
                    else -> NeonCyan
                },
                modifier = Modifier.size(32.dp)
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = when (permissionState) {
                    PermissionUiState.PermanentlyDenied -> "Permissions Required"
                    PermissionUiState.Denied -> "Bluetooth Access Denied"
                    else -> "Allow Bluetooth Access"
                },
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(onClick = onRefresh) {
                Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = NeonCyan)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = when (permissionState) {
                PermissionUiState.PermanentlyDenied ->
                    "Bluetooth permissions were permanently denied. Please enable them in Settings → App → Permissions → Nearby devices."
                PermissionUiState.Denied ->
                    "Bluetooth access is needed to find and connect to your smart ring. Please grant the permission to continue."
                else ->
                    "Your ring communicates via Bluetooth. Tap below to allow access so we can scan for and connect to your ring."
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Permission details
        if (permissionState != PermissionUiState.PermanentlyDenied) {
            NeonGlassCard(
                glowColor = NeonCyan,
                showGlow = false
            ) {
                PermissionItem(
                    icon = Icons.Default.Settings,
                    title = "Nearby Devices",
                    description = "Scan for & connect to your ring"
                )
                if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.S) {
                    Spacer(modifier = Modifier.height(8.dp))
                    PermissionItem(
                        icon = if (isLocationEnabled) Icons.Default.LocationOn else Icons.Default.Warning,
                        title = "Location Services",
                        description = if (isLocationEnabled) "Enabled" else "REQUIRED - Please turn ON GPS",
                        isError = !isLocationEnabled
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    PermissionItem(
                        icon = if (isBluetoothEnabled) Icons.Default.BluetoothDrive else Icons.Default.Warning,
                        title = "Bluetooth",
                        description = if (isBluetoothEnabled) "Enabled" else "REQUIRED - Please turn ON Bluetooth",
                        isError = !isBluetoothEnabled
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        NeonButton(
            text = if (permissionState == PermissionUiState.PermanentlyDenied)
                "OPEN SETTINGS" else "GRANT BLUETOOTH ACCESS",
            onClick = if (permissionState == PermissionUiState.PermanentlyDenied)
                onOpenSettings else onRequestPermission
        )
    }
}

@Composable
private fun PermissionItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String,
    isError: Boolean = false
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (isError) ErrorRed else NeonCyan,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════
// SCAN CONTENT — Energy pulse + floating device cards
// ═══════════════════════════════════════════════════════════════════════

@Composable
private fun ScanContent(
    uiState: RingUiState,
    onStartScan: () -> Unit,
    onStopScan: () -> Unit,
    onRingTypeChange: (Int) -> Unit,
    onDeviceSelected: (Ring) -> Unit,
    onManualEntry: () -> Unit,
    onSkip: () -> Unit,
    onShowPaired: () -> Unit = {}
) {
    var selectedDevice by remember { mutableStateOf<Ring?>(null) }

    Column(modifier = Modifier.fillMaxSize()) {
        // Ring Type Selector
        RingTypeSelector(
            selectedType = uiState.selectedRingType,
            onTypeChange = onRingTypeChange
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Scan button
        NeonButton(
            text = if (uiState.isScanning) "STOP SCANNING" else "SCAN FOR DEVICES",
            onClick = if (uiState.isScanning) onStopScan else onStartScan,
            isLoading = uiState.isScanning,
            icon = if (uiState.isScanning) null else Icons.Default.Search,
            colors = if (uiState.isScanning)
                listOf(ErrorRed.copy(alpha = 0.8f), ErrorRed)
            else
                listOf(PrimaryPurple, NeonPurple)
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Device list or animated state
        if (uiState.scannedDevices.isNotEmpty()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "DISCOVERED DEVICES",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.weight(1f))
                StatusBadge(
                    text = "${uiState.scannedDevices.size} found",
                    color = NeonGreen
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(uiState.scannedDevices) { device ->
                    DeviceCard(
                        ring = device,
                        isSelected = selectedDevice?.macAddress == device.macAddress,
                        onClick = { selectedDevice = device }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            NeonButton(
                text = "CONNECT",
                onClick = { selectedDevice?.let { onDeviceSelected(it) } },
                enabled = selectedDevice != null,
                icon = Icons.Default.Check,
                colors = listOf(NeonCyan.copy(alpha = 0.9f), NeonBlue)
            )
        } else if (uiState.isScanning) {
            // Scanning animation
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                // Energy pulse waves
                EnergyPulseWave(
                    modifier = Modifier.size(200.dp),
                    color = NeonCyan
                )

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    // Pulsing ring icon
                    val infiniteTransition = rememberInfiniteTransition(label = "scanPulse")
                    val pulse by infiniteTransition.animateFloat(
                        initialValue = 0.95f,
                        targetValue = 1.05f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(1000),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "scanPulseScale"
                    )

                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .scale(pulse)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(
                                        NeonCyan.copy(alpha = 0.2f),
                                        NeonCyan.copy(alpha = 0.05f)
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(40.dp),
                            color = NeonCyan,
                            strokeWidth = 3.dp
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Text(
                        text = "Scanning",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Keep your ring nearby",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            // Empty state
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(
                                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f).copy(alpha = 0.15f),
                                        Color.Transparent
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.size(32.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Tap 'Scan for Devices' to find your ring",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Bottom buttons
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                NeonSecondaryButton(
                    text = "ENTER MAC",
                    onClick = onManualEntry,
                    modifier = Modifier.weight(1f)
                )
                NeonSecondaryButton(
                    text = "SKIP",
                    onClick = onSkip,
                    modifier = Modifier.weight(1f)
                )
            }
            
            if (uiState.pairedRings.isNotEmpty()) {
                TextButton(onClick = onShowPaired) {
                    Icon(Icons.Default.History, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("MY PAIRED RINGS", color = NeonCyan, style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════
// PAIRED DEVICES CONTENT
// ═══════════════════════════════════════════════════════════════════════

@Composable
private fun PairedDevicesContent(
    pairedRings: List<com.dkgs.innerpulse.domain.model.PairedRing>,
    onRingSelected: (com.dkgs.innerpulse.domain.model.PairedRing) -> Unit,
    onRemoveDevice: (String) -> Unit,
    onAddNew: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "MY PAIRED DEVICES",
                style = MaterialTheme.typography.labelLarge,
                color = NeonCyan,
                letterSpacing = 2.sp
            )
            Spacer(modifier = Modifier.weight(1f))
            StatusBadge(
                text = "${pairedRings.size} saved",
                color = NeonCyan
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(pairedRings) { ring ->
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
                    border = BorderStroke(1.dp, NeonCyan.copy(alpha = 0.2f))
                ) {
                    Row(
                        modifier = Modifier
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(NeonCyan.copy(alpha = 0.1f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.BluetoothConnected, contentDescription = null, tint = NeonCyan)
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = ring.name,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = ring.macAddress,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        IconButton(onClick = { onRemoveDevice(ring.macAddress) }) {
                            Icon(Icons.Default.Delete, contentDescription = "Remove", tint = ErrorRed.copy(alpha = 0.6f))
                        }

                        Button(
                            onClick = { onRingSelected(ring) },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = NeonCyan.copy(alpha = 0.1f)),
                            border = BorderStroke(1.dp, NeonCyan.copy(alpha = 0.5f)),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                        ) {
                            Text("CONNECT", color = NeonCyan, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        NeonButton(
            text = "CONNECT NEW DEVICE",
            onClick = onAddNew,
            icon = Icons.Default.Add,
            colors = listOf(PrimaryPurple, NeonPurple)
        )
        
        Spacer(modifier = Modifier.height(16.dp))
    }
}


// ═══════════════════════════════════════════════════════════════════════
// CONNECTING CONTENT — Full-screen cinematic ring animation
// ═══════════════════════════════════════════════════════════════════════

@Composable
private fun ConnectingContent() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        // Pulse waves background
        EnergyPulseWave(
            modifier = Modifier.size(300.dp),
            color = NeonCyan.copy(alpha = 0.5f),
            pulseCount = 4
        )

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            // Rotating ring
            val infiniteTransition = rememberInfiniteTransition(label = "connect")
            val scale by infiniteTransition.animateFloat(
                initialValue = 0.9f,
                targetValue = 1.1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1500, easing = EaseInOutCubic),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "scale"
            )

            Box(
                modifier = Modifier.scale(scale),
                contentAlignment = Alignment.Center
            ) {
                AnimatedRing3D(
                    modifier = Modifier.size(160.dp),
                    primaryColor = NeonCyan,
                    secondaryColor = PrimaryPurple,
                    isScanning = true
                )
            }

            Spacer(modifier = Modifier.height(40.dp))

            Text(
                text = "Connecting",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Establishing secure link to your ring",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Progress dots
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                repeat(3) { index ->
                    val dotAlpha by infiniteTransition.animateFloat(
                        initialValue = 0.3f,
                        targetValue = 1f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(600, delayMillis = index * 200),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "dot_$index"
                    )
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(NeonCyan.copy(alpha = dotAlpha))
                    )
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════
// CONNECTED CONTENT — Floating metrics dashboard
// ═══════════════════════════════════════════════════════════════════════

@Composable
private fun ConnectedContent(
    uiState: RingUiState,
    onDisconnect: () -> Unit,
    onDone: () -> Unit,
    onMeasureHeartRate: () -> Unit,
    onMeasureSpO2: () -> Unit = {},
    onMeasureStress: () -> Unit = {},
    onRequestSleep: () -> Unit = {}
) {
    // Particle explosion on first render
    var showParticles by remember { mutableStateOf(true) }
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(1500)
        showParticles = false
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Success card with particle overlay
        Box {
            NeonGlassCard(glowColor = NeonGreen) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(NeonGreen, NeonGreen.copy(alpha = 0.6f))
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.background,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column {
                        Text(
                            text = "Ring Connected",
                            style = MaterialTheme.typography.titleMedium,
                            color = NeonGreen,
                            fontWeight = FontWeight.Bold
                        )
                        uiState.connectedRing?.let { ring ->
                            Text(
                                text = "${ring.name} • ${ring.macAddress}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Particle celebration
            ParticleExplosion(
                modifier = Modifier.matchParentSize(),
                isActive = showParticles,
                colors = listOf(NeonGreen, NeonCyan, PrimaryPurple)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Scrollable health data
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Battery
            item {
                BatteryCard(
                    batteryLevel = uiState.batteryLevel,
                    isCharging = uiState.isCharging
                )
            }

            // Heart Rate
            item {
                HeartRateCard(
                    heartRate = uiState.heartRate,
                    isMeasuring = uiState.ringData.heartRateMeasuring
                )
            }
            item {
                MeasurementButton(
                    text = if (uiState.ringData.heartRateMeasuring) "Measuring HR..." else "Measure Heart Rate",
                    icon = Icons.Default.Favorite,
                    color = ErrorRed,
                    onClick = onMeasureHeartRate,
                    enabled = !uiState.ringData.heartRateMeasuring
                )
            }

            // Blood Oxygen (SpO2)
            item {
                SpO2Card(spO2 = uiState.ringData.spO2.toInt())
            }
            item {
                MeasurementButton(
                    text = if (uiState.ringData.spO2Measuring) "Measuring SpO2..." else "Measure Blood Oxygen",
                    icon = Icons.Default.ThumbUp,
                    color = NeonCyan,
                    onClick = onMeasureSpO2,
                    enabled = !uiState.ringData.spO2Measuring
                )
            }

            // Stress Level
            item {
                StressCard(
                    stress = uiState.ringData.stress,
                    onMeasureClick = onMeasureStress
                )
            }

            // Sleep Data
            item {
                SleepCard(
                    sleepData = uiState.ringData.sleepData,
                    onRequestSleep = onRequestSleep
                )
            }

            // Firmware Info
            item {
                FirmwareCard(firmwareInfo = uiState.ringData.firmwareInfo)
            }

            // Steps
            item {
                StepsCard(steps = uiState.steps)
            }

            // Bottom spacer
            item {
                Spacer(modifier = Modifier.height(8.dp))
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            NeonSecondaryButton(
                text = "DISCONNECT",
                onClick = onDisconnect,
                modifier = Modifier.weight(1f),
                borderColor = ErrorRed.copy(alpha = 0.4f)
            )
            NeonButton(
                text = "DONE",
                onClick = onDone,
                modifier = Modifier.weight(1f),
                colors = listOf(NeonGreen.copy(alpha = 0.9f), NeonCyan)
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════
// MEASUREMENT BUTTON — Neon styled
// ═══════════════════════════════════════════════════════════════════════

@Composable
private fun MeasurementButton(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    onClick: () -> Unit,
    enabled: Boolean = true
) {
    Button(
        onClick = onClick,
        modifier = Modifier
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

// ═══════════════════════════════════════════════════════════════════════
// STRESS CARD
// ═══════════════════════════════════════════════════════════════════════

@Composable
private fun StressCard(stress: Int, onMeasureClick: () -> Unit = {}) {
    val stressColor = when {
        stress <= 30 -> NeonGreen
        stress <= 60 -> NeonOrange
        else -> ErrorRed
    }
    val stressLabel = when {
        stress <= 30 -> "Low"
        stress <= 60 -> "Medium"
        else -> "High"
    }

    NeonGlassCard(glowColor = stressColor.copy(alpha = 0.6f)) {
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
                            colors = listOf(stressColor.copy(alpha = 0.2f), stressColor.copy(alpha = 0.03f))
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Face,
                    contentDescription = null,
                    tint = stressColor,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "STRESS LEVEL",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (stress > 0) "$stress" else "--",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold
                    )
                    if (stress > 0) {
                        Spacer(modifier = Modifier.width(8.dp))
                        StatusBadge(text = stressLabel, color = stressColor)
                    }
                }
            }
            // Measure button
            FilledTonalIconButton(
                onClick = onMeasureClick,
                colors = IconButtonDefaults.filledTonalIconButtonColors(
                    containerColor = NeonCyan.copy(alpha = 0.1f),
                    contentColor = NeonCyan
                )
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Measure Stress",
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════
// MANUAL ENTRY CONTENT
// ═══════════════════════════════════════════════════════════════════════

@Composable
private fun ManualEntryContent(
    macAddress: String,
    selectedRingType: Int,
    onMacChange: (String) -> Unit,
    onRingTypeChange: (Int) -> Unit,
    onConnect: () -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .imePadding(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(32.dp))

        // Ring Type Selector (Added to match Demo SDK logic)
        RingTypeSelector(
            selectedType = selectedRingType,
            onTypeChange = onRingTypeChange
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Manual MAC Entry",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Enter the MAC address of your ring",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(32.dp))

        OutlinedTextField(
            value = macAddress,
            onValueChange = onMacChange,
            label = { Text("MAC Address") },
            placeholder = { Text("XX:XX:XX:XX:XX:XX or XXXXXXXXXXXX") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = NeonCyan,
                unfocusedBorderColor = AppColors.dividerColor,
                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                cursorColor = NeonCyan,
                focusedLabelColor = NeonCyan
            ),
            shape = RoundedCornerShape(14.dp)
        )

        Spacer(modifier = Modifier.weight(1f))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            NeonSecondaryButton(
                text = "BACK",
                onClick = onBack,
                modifier = Modifier.weight(1f)
            )
            NeonButton(
                text = "CONNECT",
                onClick = onConnect,
                modifier = Modifier.weight(1f),
                enabled = macAddress.isNotEmpty(), // Allow any length so the user can always attempt connection
                colors = listOf(NeonCyan.copy(alpha = 0.9f), NeonBlue)
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════
// SLEEP CARD
// ═══════════════════════════════════════════════════════════════════════

@Composable
private fun SleepCard(
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
private fun SleepMetric(
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

// ═══════════════════════════════════════════════════════════════════════
// FIRMWARE CARD
// ═══════════════════════════════════════════════════════════════════════

@Composable
private fun FirmwareCard(firmwareInfo: com.dkgs.innerpulse.domain.model.FirmwareInfo) {
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
                            colors = listOf(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f).copy(alpha = 0.15f), Color.Transparent)
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
// PREVIEWS — Multi-state, ViewModel-free
// ═══════════════════════════════════════════════════════════════════════

@Preview(name = "Permissions", showBackground = true, backgroundColor = 0xFF050508, device = Devices.PIXEL_7)
@Composable
private fun SetupPermissionsPreview() {
    FitnessAppTheme(darkTheme = true) {
        RingSetupScreen(uiState = PreviewData.permissionsNeededState)
    }
}

@Preview(name = "Scanning", showBackground = true, backgroundColor = 0xFF050508, device = Devices.PIXEL_7)
@Composable
private fun SetupScanningPreview() {
    FitnessAppTheme(darkTheme = true) {
        RingSetupScreen(uiState = PreviewData.scanningState)
    }
}

@Preview(name = "Connecting", showBackground = true, backgroundColor = 0xFF050508)
@Composable
private fun SetupConnectingPreview() {
    FitnessAppTheme(darkTheme = true) {
        RingSetupScreen(uiState = PreviewData.connectingState)
    }
}

@Preview(name = "Connected", showBackground = true, backgroundColor = 0xFF050508, device = Devices.PIXEL_7)
@Composable
private fun SetupConnectedPreview() {
    FitnessAppTheme(darkTheme = true) {
        RingSetupScreen(uiState = PreviewData.connectedState)
    }
}

@Preview(name = "Dark Mode", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Composable
private fun SetupDarkModePreview() {
    FitnessAppTheme(darkTheme = true) {
        RingSetupScreen(uiState = PreviewData.scanningState)
    }
}
