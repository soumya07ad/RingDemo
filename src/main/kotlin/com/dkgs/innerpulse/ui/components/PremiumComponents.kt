package com.dkgs.innerpulse.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import com.dkgs.innerpulse.ui.theme.*

// ═══════════════════════════════════════════════════════════════════════
// NEON GLASS CARD — Floating card with neon edge glow + depth
// ═══════════════════════════════════════════════════════════════════════

@Composable
fun NeonGlassCard(
    modifier: Modifier = Modifier,
    glowColor: Color = NeonCyan,
    showGlow: Boolean = true,
    cornerRadius: Dp = 24.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = RoundedCornerShape(cornerRadius)
    val isDark = AppColors.isDark

    Box(modifier = modifier) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(shape)
                .then(
                    if (isDark) {
                        if (showGlow) {
                            Modifier.border(
                                width = 1.dp,
                                brush = Brush.linearGradient(
                                    colors = listOf(
                                        glowColor.copy(alpha = 0.4f),
                                        glowColor.copy(alpha = 0.05f),
                                        Color.Transparent,
                                        glowColor.copy(alpha = 0.1f)
                                    )
                                ),
                                shape = shape
                            )
                        } else {
                            Modifier.border(1.dp, MaterialTheme.colorScheme.outline, shape)
                        }
                    } else {
                        // Glass border for light mode
                        Modifier.border(1.dp, LightBorderSubtle, shape)
                    }
                ),
            shape = shape,
            color = if (isDark) MaterialTheme.colorScheme.surface else LightCard,
            shadowElevation = if (isDark) 0.dp else 4.dp
        ) {
            Column(
                modifier = Modifier
                    .background(if (isDark) CardGlassBrush else Brush.verticalGradient(
                        listOf(
                            Color(0xFFFFFFFF),  // Pure white top
                            Color(0xFFF8FAFF)   // Very light blue-white bottom
                        )
                    ))
                    .padding(20.dp),
                content = content
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════
// FLOATING METRIC TILE — Radial chart + counter in glass card
// ═══════════════════════════════════════════════════════════════════════

@Composable
fun FloatingMetricTile(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    label: String,
    value: String,
    unit: String = "",
    progress: Float = 0f,
    gradientColors: List<Color> = listOf(NeonCyan, NeonBlue),
    glowColor: Color = gradientColors.first(),
    iconBgColor: Color = Color.Transparent,
    onClick: (() -> Unit)? = null
) {
    val isDark = AppColors.isDark
    val shape = RoundedCornerShape(18.dp)

    Surface(
        modifier = modifier
            .fillMaxWidth()
            // Outer 3D shadow (Light mode)
            .shadow(
                elevation = if (isDark) 0.dp else 6.dp,
                shape = shape,
                ambientColor = if (isDark) Color.Transparent else LightCardShadow,
                spotColor = if (isDark) Color.Transparent else LightCardShadow
            )
            .clip(shape)
            .then(
                if (isDark) {
                    Modifier.border(1.dp, MaterialTheme.colorScheme.outline, shape)
                } else {
                    Modifier.border(1.dp, LightBorderSubtle, shape)
                }
            )
            .then(
                if (onClick != null) Modifier.clickable { onClick() } else Modifier
            ),
        shape = shape,
        color = if (isDark) MaterialTheme.colorScheme.surface else LightCard,
        shadowElevation = 0.dp
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .background(if (isDark) CardGlassBrush else Brush.verticalGradient(
                    listOf(
                        LightSurface,        // Pure white top
                        LightSurfaceVariant  // Subtle blue-tint white bottom
                    )
                ))
                .padding(16.dp)
        ) {
            // Icon inside a colored circle
            Box(
                modifier = Modifier.size(48.dp),
                contentAlignment = Alignment.Center
            ) {
                // Pastel circle bg (light mode) or radial chart (dark mode)
                if (!isDark && iconBgColor != Color.Transparent) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                            .background(iconBgColor)
                    )
                } else {
                    AnimatedRadialChart(
                        modifier = Modifier.fillMaxSize(),
                        progress = progress,
                        gradientColors = gradientColors,
                        strokeWidth = 6f,
                        glowRadius = 4f
                    )
                }
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = gradientColors.first(),
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    color = if (isDark) MaterialTheme.colorScheme.onSurfaceVariant else LightTextSecondary,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.Bottom
                ) {
                    Text(
                        text = value,
                        style = MaterialTheme.typography.headlineMedium,
                        color = if (isDark) MaterialTheme.colorScheme.onSurface else LightTextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    if (unit.isNotEmpty()) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = unit,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isDark) MaterialTheme.colorScheme.onSurfaceVariant else LightTextSecondary,
                            modifier = Modifier.padding(bottom = 3.dp)
                        )
                    }
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════
// METRIC GLASS CARD — Unified dashboard glass container without double border
// ═══════════════════════════════════════════════════════════════════════

@Composable
fun MetricGlassCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    val isDark = AppColors.isDark
    val shape = RoundedCornerShape(20.dp)

    Surface(
        modifier = modifier
            .fillMaxWidth()
            // Outer 3D shadow (Light mode)
            .shadow(
                elevation = if (isDark) 0.dp else 8.dp,
                shape = shape,
                ambientColor = if (isDark) Color.Transparent else LightCardShadow,
                spotColor = if (isDark) Color.Transparent else LightCardShadow
            )
            .clip(shape)
            .then(
                if (isDark) {
                    Modifier.border(1.dp, MaterialTheme.colorScheme.outline, shape)
                } else {
                    Modifier.border(1.dp, LightBorderSubtle, shape)
                }
            ),
        shape = shape,
        color = if (isDark) MaterialTheme.colorScheme.surface else LightCard,
        shadowElevation = 0.dp
    ) {
        Column(
            modifier = Modifier
                .background(if (isDark) CardGlassBrush else Brush.verticalGradient(
                    listOf(
                        LightSurface,        // Pure white top
                        LightSurfaceVariant  // Subtle blue-tint white bottom
                    )
                ))
                .padding(20.dp),
            content = content
        )
    }
}

// ═══════════════════════════════════════════════════════════════════════
// NEON BUTTON — Magnetic press effect with glow
// ═══════════════════════════════════════════════════════════════════════

@Composable
fun NeonButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isLoading: Boolean = false,
    icon: ImageVector? = null,
    colors: List<Color> = listOf(PrimaryPurple, NeonPurple)
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "btnScale"
    )

    val shape = RoundedCornerShape(16.dp)

    Button(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .scale(scale),
        enabled = enabled,
        shape = shape,
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.Transparent,
            disabledContainerColor = Color.Transparent
        ),
        contentPadding = PaddingValues(0.dp),
        interactionSource = interactionSource
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = if (enabled) Brush.horizontalGradient(colors)
                    else Brush.horizontalGradient(
                        colors.map { it.copy(alpha = 0.3f) }
                    ),
                    shape = shape
                ),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onSurface,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                }
                icon?.let {
                    if (!isLoading) {
                        Icon(
                            imageVector = it,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                }
                Text(
                    text = text,
                    style = MaterialTheme.typography.labelLarge,
                    color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    letterSpacing = 1.sp
                )
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════
// NEON SECONDARY BUTTON
// ═══════════════════════════════════════════════════════════════════════

@Composable
fun NeonSecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    borderColor: Color = AppColors.dividerColor
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "secBtnScale"
    )

    OutlinedButton(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .scale(scale),
        shape = RoundedCornerShape(16.dp),
        border = ButtonDefaults.outlinedButtonBorder.copy(
            brush = Brush.horizontalGradient(
                listOf(borderColor, borderColor.copy(alpha = 0.5f))
            )
        ),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        interactionSource = interactionSource
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            letterSpacing = 1.sp
        )
    }
}

// ═══════════════════════════════════════════════════════════════════════
// GLOW DIVIDER — Neon horizontal divider
// ═══════════════════════════════════════════════════════════════════════

@Composable
fun GlowDivider(
    modifier: Modifier = Modifier,
    color: Color = NeonCyan
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        Color.Transparent,
                        color.copy(alpha = 0.3f),
                        color.copy(alpha = 0.5f),
                        color.copy(alpha = 0.3f),
                        Color.Transparent
                    )
                )
            )
    )
}

// ═══════════════════════════════════════════════════════════════════════
// STATUS BADGE — Small glowing status pill
// ═══════════════════════════════════════════════════════════════════════

@Composable
fun StatusBadge(
    text: String,
    color: Color = NeonGreen,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        color = color.copy(alpha = 0.15f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(color)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = text.uppercase(),
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.8.sp
                ),
                color = color,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════
// BRANDED TOP BAR — Premium left-aligned identity
// ═══════════════════════════════════════════════════════════════════════

@Composable
fun BrandedTopBar(
    painter: androidx.compose.ui.graphics.painter.Painter,
    title: String,
    modifier: Modifier = Modifier,
    isConnected: Boolean = false,
    batteryLevel: Int? = null
) {
    val isDark = AppColors.isDark
    
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 3D Ring Logo (Smaller, refined)
        Surface(
            modifier = Modifier
                .size(48.dp)
                .shadow(
                    elevation = 8.dp,
                    shape = CircleShape,
                    ambientColor = if (isDark) NeonCyan.copy(alpha = 0.3f) else Color.Black.copy(alpha = 0.1f)
                )
                .border(
                    width = 1.dp,
                    brush = Brush.linearGradient(
                        listOf(Color.White.copy(alpha = 0.5f), Color.Transparent)
                    ),
                    shape = CircleShape
                ),
            shape = CircleShape,
            color = if (isDark) Color.Black.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.4f)
        ) {
            androidx.compose.foundation.Image(
                painter = painter,
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp)
                    .clip(CircleShape),
                contentScale = androidx.compose.ui.layout.ContentScale.Fit
            )
        }
        
        Spacer(modifier = Modifier.width(12.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.2.sp
                ),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                softWrap = false,
                overflow = androidx.compose.ui.text.style.TextOverflow.Visible
            )
            
            // Subtle underline glow for the brand
            Box(
                modifier = Modifier
                    .width(30.dp)
                    .height(2.dp)
                    .background(
                        brush = Brush.horizontalGradient(
                            listOf(if (isDark) NeonCyan else PrimaryPurple, Color.Transparent)
                        ),
                        shape = CircleShape
                    )
            )
        }

        if (isConnected) {
            StatusBadge(
                text = "Connected${batteryLevel?.let { " • $it%" } ?: ""}",
                color = NeonGreen
            )
        } else {
            StatusBadge(
                text = "Disconnected",
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════
// DASHBOARD HERO HEADER — Cinematic logo composition (Legacy/Removed)
// ═══════════════════════════════════════════════════════════════════════

@Composable
fun DashboardHeroHeader(
    painter: androidx.compose.ui.graphics.painter.Painter,
    title: String,
    modifier: Modifier = Modifier
) {
    val isDark = AppColors.isDark
    
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.padding(bottom = 16.dp)
        ) {
            // Background Ambient Glow
            AmbientPulseGlow(
                color = if (isDark) NeonCyan else PrimaryPurple,
                size = 140.dp
            )
            
            // Logo in Glass Container
            Surface(
                modifier = Modifier
                    .size(90.dp)
                    .shadow(
                        elevation = 20.dp,
                        shape = CircleShape,
                        ambientColor = if (isDark) NeonCyan.copy(alpha = 0.4f) else Color.Black.copy(alpha = 0.2f),
                        spotColor = if (isDark) NeonCyan.copy(alpha = 0.6f) else Color.Black.copy(alpha = 0.3f)
                    )
                    .border(
                        width = 1.5.dp,
                        brush = Brush.linearGradient(
                            listOf(
                                Color.White.copy(alpha = 0.6f),
                                Color.Transparent,
                                if (isDark) NeonCyan.copy(alpha = 0.4f) else PrimaryPurple.copy(alpha = 0.4f)
                            )
                        ),
                        shape = CircleShape
                    ),
                shape = CircleShape,
                color = if (isDark) Color.Black.copy(alpha = 0.3f) else Color.White.copy(alpha = 0.6f)
            ) {
                androidx.compose.foundation.Image(
                    painter = painter,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp)
                        .clip(CircleShape),
                    contentScale = androidx.compose.ui.layout.ContentScale.Fit
                )
            }
        }
        
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall.copy(
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 0.5.sp
            ),
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        
        // Subtle underline glow
        Box(
            modifier = Modifier
                .padding(top = 8.dp)
                .width(40.dp)
                .height(3.dp)
                .background(
                    brush = Brush.horizontalGradient(
                        listOf(Color.Transparent, if (isDark) NeonCyan else PrimaryPurple, Color.Transparent)
                    ),
                    shape = CircleShape
                )
        )
    }
}

@Composable
fun AmbientPulseGlow(
    color: Color,
    size: Dp,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "glow")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.15f,
        targetValue = 0.4f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )
    
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    Box(
        modifier = modifier
            .size(size)
            .scale(scale)
            .background(
                brush = Brush.radialGradient(
                    colors = listOf(
                        color.copy(alpha = alpha),
                        color.copy(alpha = alpha * 0.5f),
                        Color.Transparent
                    )
                ),
                shape = CircleShape
            )
    )
}

// ═══════════════════════════════════════════════════════════════════════
// CONNECTIVITY BANNER — Premium status notification
// ═══════════════════════════════════════════════════════════════════════

@Composable
fun ConnectivityBanner(
    modifier: Modifier = Modifier,
    isConnecting: Boolean = false
) {
    val infiniteTransition = rememberInfiniteTransition(label = "bannerGlow")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.1f,
        targetValue = 0.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )
    val isDark = AppColors.isDark
    val baseColor = if (isDark) NeonGreen else LightSuccess
    val bgColor = if (isDark) NeonGreen.copy(alpha = alpha) else LightSuccessBg
    val borderColor = if (isDark) NeonGreen.copy(alpha = 0.5f) else LightSuccess

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = if (isDark) 12.dp else 4.dp,
                shape = RoundedCornerShape(16.dp),
                ambientColor = baseColor.copy(alpha = 0.4f),
                spotColor = baseColor.copy(alpha = 0.4f)
            ),
        shape = RoundedCornerShape(16.dp),
        color = bgColor,
        border = BorderStroke(
            1.dp,
            Brush.horizontalGradient(listOf(borderColor.copy(alpha = 0.5f), Color.Transparent))
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(baseColor)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = if (isConnecting) "CONNECTING TO SMART RING..." else "SMART RING ACTIVE",
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.2.sp
                ),
                color = baseColor
            )
            Spacer(modifier = Modifier.weight(1f))
            if (isConnecting) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    color = baseColor,
                    strokeWidth = 2.dp
                )
            } else {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = baseColor,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════
// PREVIEWS
// ═══════════════════════════════════════════════════════════════════════

@Preview(name = "Neon Glass Card", showBackground = true, backgroundColor = 0xFF050508)
@Composable
private fun NeonGlassCardPreview() {
    FitnessAppTheme(darkTheme = true) {
        NeonGlassCard(glowColor = NeonCyan) {
            Text("Glass Card Content", color = MaterialTheme.colorScheme.onSurface)
        }
    }
}

@Preview(name = "Floating Metric Tile", showBackground = true, backgroundColor = 0xFF050508)
@Composable
private fun FloatingMetricTilePreview() {
    FitnessAppTheme(darkTheme = true) {
        FloatingMetricTile(
            icon = Icons.Filled.Favorite,
            label = "Heart Rate",
            value = "72",
            unit = "bpm",
            progress = 0.72f,
            gradientColors = listOf(NeonPink, ErrorRed)
        )
    }
}

@Preview(name = "Neon Button", showBackground = true, backgroundColor = 0xFF050508)
@Composable
private fun NeonButtonPreview() {
    FitnessAppTheme(darkTheme = true) {
        NeonButton(
            text = "CONNECT",
            onClick = {},
            icon = Icons.Filled.Add // Using Add as generic icon if BluetoothSearching not found
        )
    }
}

@Preview(name = "Neon Button Loading", showBackground = true, backgroundColor = 0xFF050508)
@Composable
private fun NeonButtonLoadingPreview() {
    FitnessAppTheme(darkTheme = true) {
        NeonButton(text = "CONNECTING...", onClick = {}, isLoading = true)
    }
}

@Preview(name = "Status Badge", showBackground = true, backgroundColor = 0xFF050508)
@Composable
private fun StatusBadgePreview() {
    FitnessAppTheme(darkTheme = true) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatusBadge(text = "Connected", color = NeonGreen)
            StatusBadge(text = "Scanning", color = NeonCyan)
            StatusBadge(text = "Error", color = ErrorRed)
        }
    }
}

@Preview(name = "Glow Divider", showBackground = true, backgroundColor = 0xFF050508)
@Composable
private fun GlowDividerPreview() {
    FitnessAppTheme(darkTheme = true) {
        Column {
            Spacer(modifier = Modifier.height(16.dp))
            GlowDivider()
            Spacer(modifier = Modifier.height(16.dp))
            GlowDivider(color = NeonPink)
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
