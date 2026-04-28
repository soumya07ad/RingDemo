package com.dkgs.innerpulse.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dkgs.innerpulse.domain.model.RingConnectionState
import com.dkgs.innerpulse.ui.theme.*

// ═══════════════════════════════════════════════════════════════════════
// SMART RING CARD — Connection management on Dashboard
// ═══════════════════════════════════════════════════════════════════════

@Composable
fun SmartRingCard(
    connectionState: RingConnectionState,
    ringName: String = "Smart Ring",
    batteryLevel: Int? = null,
    onConnectClick: () -> Unit,
    onDisconnectClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isConnected = connectionState == RingConnectionState.CONNECTED

    // Animated glow color
    val glowColor by animateColorAsState(
        targetValue = if (isConnected) {
            if (AppColors.isDark) NeonGreen else LightSuccess
        } else {
            if (AppColors.isDark) NeonCyan else LightWarning
        },
        animationSpec = tween(600),
        label = "ringCardGlow"
    )

    // Subtle pulse for connected state
    val infiniteTransition = rememberInfiniteTransition(label = "ringPulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = EaseInOutCubic),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    MetricGlassCard(
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Ring icon with pulse
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(glowColor.copy(alpha = 0.2f), Color.Transparent)
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                // Outer pulsing ring
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .scale(if (isConnected) pulseAlpha else 1f)
                        .border(2.dp, glowColor.copy(alpha = 0.3f), CircleShape)
                )
                
                Icon(
                    imageVector = Icons.Default.Adjust,
                    contentDescription = "Ring",
                    tint = glowColor,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Title + status
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = ringName.uppercase(),
                    style = MaterialTheme.typography.titleMedium.copy(
                        letterSpacing = 1.sp,
                        fontWeight = FontWeight.Black
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(glowColor)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isConnected) "CONNECTED" else "DISCONNECTED",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        ),
                        color = glowColor
                    )
                    
                    if (isConnected && batteryLevel != null) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "• $batteryLevel%",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Medium
                            ),
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }
            }

            // Action Button
            if (isConnected) {
                OutlinedButton(
                    onClick = onDisconnectClick,
                    modifier = Modifier.height(36.dp),
                    shape = RoundedCornerShape(10.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, ErrorRed.copy(alpha = 0.5f)),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = ErrorRed
                    ),
                    contentPadding = PaddingValues(horizontal = 12.dp)
                ) {
                    Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("DISCONNECT", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            } else {
                val buttonShape = RoundedCornerShape(10.dp)
                Button(
                    onClick = onConnectClick,
                    modifier = Modifier
                        .height(36.dp)
                        .shadow(
                            10.dp,
                            buttonShape,
                            ambientColor = (if (AppColors.isDark) SkyBlue else LightPrimary).copy(alpha = 0.3f),
                            spotColor = (if (AppColors.isDark) SkyBlue else LightPrimary).copy(alpha = 0.3f)
                        ),
                    shape = buttonShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (AppColors.isDark) SkyBlue else LightPrimary,
                        contentColor = Color.White
                    ),
                    contentPadding = PaddingValues(horizontal = 12.dp)
                ) {
                    Text("CONNECT", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
