package com.dkgs.innerpulse.presentation.navigation

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dkgs.innerpulse.ui.components.CinematicBackground
import com.dkgs.innerpulse.ui.components.NeonButton
import com.dkgs.innerpulse.ui.components.NeonGlassCard
import com.dkgs.innerpulse.ui.theme.NeonCyan
import com.dkgs.innerpulse.ui.theme.NeonPurple
import com.dkgs.innerpulse.ui.theme.PrimaryPurple

@Composable
fun GlobalPermissionScreen(
    viewModel: AppNavigationViewModel,
    onAllPermissionsGranted: () -> Unit
) {
    val permissions = viewModel.getGlobalPermissions()
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        viewModel.onPermissionsResult(results)
        if (viewModel.checkAllPermissions()) onAllPermissionsGranted()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        CinematicBackground()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(40.dp))

            // Shield Icon with Pulse
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(NeonCyan.copy(alpha = 0.2f), Color.Transparent)
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Shield,
                    contentDescription = null,
                    tint = NeonCyan,
                    modifier = Modifier.size(56.dp)
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "APP PERMISSIONS",
                style = MaterialTheme.typography.labelLarge,
                color = NeonCyan,
                letterSpacing = 4.sp
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "Unlock Full Potential",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "InnerPulse needs access to your phone's sensors and Bluetooth to track your health metrics and sync with your smart ring.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(40.dp))

            // Permission Cards
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                PermissionSummaryItem(
                    icon = Icons.Default.Bluetooth,
                    title = "Ring Connection",
                    description = "Bluetooth & Location to sync your ring"
                )
                
                PermissionSummaryItem(
                    icon = Icons.Default.DirectionsRun,
                    title = "Activity Tracking",
                    description = "Step counting & movement detection"
                )
                
                PermissionSummaryItem(
                    icon = Icons.Default.Favorite,
                    title = "Health Sensors",
                    description = "Heart rate & SpO2 monitoring"
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            NeonButton(
                text = "GRANT ALL PERMISSIONS",
                onClick = { launcher.launch(permissions) },
                colors = listOf(PrimaryPurple, NeonPurple)
            )

            Spacer(modifier = Modifier.height(16.dp))

            TextButton(onClick = { viewModel.skipPermissions() }) {
                Text(
                    "I'll do this later",
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
private fun PermissionSummaryItem(
    icon: ImageVector,
    title: String,
    description: String
) {
    NeonGlassCard(
        glowColor = NeonCyan,
        showGlow = false,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(NeonCyan.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = NeonCyan,
                    modifier = Modifier.size(20.dp)
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                )
            }
        }
    }
}
