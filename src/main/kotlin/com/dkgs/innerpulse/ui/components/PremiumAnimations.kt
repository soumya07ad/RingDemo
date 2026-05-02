package com.dkgs.innerpulse.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dkgs.innerpulse.ui.theme.*
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

// ═══════════════════════════════════════════════════════════════════════
// SPO2: OXYGEN FLOW ANIMATION
// ═══════════════════════════════════════════════════════════════════════

@Composable
fun OxygenFlowAnimation(
    modifier: Modifier = Modifier,
    progress: Float,
    isFinished: Boolean = false
) {
    val infiniteTransition = rememberInfiniteTransition(label = "oxygen")
    
    // Wave phase
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2f * PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase"
    )

    // Pulse for glow
    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = EaseInOutCubic),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    // Bubbles
    val bubbleCount = 15
    val bubbleStates = remember {
        List(bubbleCount) {
            val startX = Random.nextFloat()
            val speed = 0.5f + Random.nextFloat() * 1f
            val delay = Random.nextInt(3000)
            Triple(startX, speed, delay)
        }
    }

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.size(240.dp)) {
            val w = size.width
            val h = size.height
            val chamberW = w * 0.45f
            val chamberH = h * 0.85f
            val chamberX = (w - chamberW) / 2
            val chamberY = (h - chamberH) / 2

            val fillHeight = chamberH * progress.coerceIn(0.02f, 1f)
            val surfaceY = chamberY + chamberH - fillHeight

            // 1. Draw Chamber Outline
            val chamberPath = Path().apply {
                addRoundRect(
                    androidx.compose.ui.geometry.RoundRect(
                        Rect(chamberX, chamberY, chamberX + chamberW, chamberY + chamberH),
                        CornerRadius(chamberW / 2, chamberW / 2)
                    )
                )
            }

            drawPath(
                path = chamberPath,
                color = Color.White.copy(alpha = 0.1f),
                style = Stroke(width = 2.dp.toPx())
            )

            // 2. Draw Liquid with Wave
            clipPath(chamberPath) {
                // Background Liquid
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(NeonCyan.copy(alpha = 0.4f), NeonBlue.copy(alpha = 0.7f)),
                        startY = surfaceY,
                        endY = chamberY + chamberH
                    ),
                    topLeft = Offset(chamberX, surfaceY),
                    size = Size(chamberW, fillHeight)
                )

                // Wave surface
                val wavePath = Path()
                wavePath.moveTo(chamberX, surfaceY)
                val segments = 40
                for (i in 0..segments) {
                    val x = chamberX + (i.toFloat() / segments) * chamberW
                    val waveY = surfaceY + sin(i.toFloat() / segments * 4 * PI.toFloat() + phase) * 8.dp.toPx()
                    wavePath.lineTo(x, waveY)
                }
                wavePath.lineTo(chamberX + chamberW, chamberY + chamberH)
                wavePath.lineTo(chamberX, chamberY + chamberH)
                wavePath.close()

                drawPath(
                    path = wavePath,
                    brush = Brush.verticalGradient(
                        colors = listOf(NeonCyan.copy(alpha = 0.8f), NeonBlue.copy(alpha = 1f)),
                        startY = surfaceY - 10.dp.toPx(),
                        endY = chamberY + chamberH
                    )
                )

                // Surface Glow
                drawPath(
                    path = wavePath,
                    color = Color.White.copy(alpha = 0.3f * pulse),
                    style = Stroke(width = 2.dp.toPx())
                )

                // 3. Bubbles
                if (!isFinished) {
                    bubbleStates.forEachIndexed { index, (startX, speed, delay) ->
                        val bubbleProgress = ((System.currentTimeMillis() + delay) % 3000) / 3000f
                        val bY = (chamberY + chamberH) - (bubbleProgress * chamberH)
                        
                        // Only draw if below surface
                        if (bY > surfaceY) {
                            val bX = chamberX + (startX * chamberW)
                            val bSize = 3.dp.toPx() * (1f - bubbleProgress * 0.5f)
                            val bAlpha = (1f - bubbleProgress) * 0.4f
                            
                            drawCircle(
                                color = Color.White.copy(alpha = bAlpha),
                                radius = bSize,
                                center = Offset(bX, bY)
                            )
                        }
                    }
                }
            }

            // Chamber Glass Shine
            drawArc(
                color = Color.White.copy(alpha = 0.05f),
                startAngle = 180f,
                sweepAngle = 90f,
                useCenter = false,
                topLeft = Offset(chamberX + 5.dp.toPx(), chamberY + 5.dp.toPx()),
                size = Size(chamberW - 10.dp.toPx(), chamberH - 10.dp.toPx()),
                style = Stroke(width = 4.dp.toPx())
            )
        }

        // Percentage Text
        if (!isFinished) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "${(progress * 100).toInt()}",
                    style = MaterialTheme.typography.displayMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Black,
                    fontSize = 48.sp
                )
                Text(
                    text = "%",
                    style = MaterialTheme.typography.titleMedium,
                    color = NeonCyan,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════
// STRESS: NEURAL ORB ANIMATION
// ═══════════════════════════════════════════════════════════════════════

@Composable
fun NeuralOrbAnimation(
    modifier: Modifier = Modifier,
    progress: Float,
    isFinished: Boolean = false
) {
    val infiniteTransition = rememberInfiniteTransition(label = "stress")
    
    val rotation1 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rot1"
    )

    val rotation2 by infiniteTransition.animateFloat(
        initialValue = 360f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(6000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rot2"
    )

    val scalePulse by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    // Jitter amount decreases as progress increases
    val jitterAmount = (1f - progress) * 15f

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.size(280.dp)) {
            val cx = size.width / 2
            val cy = size.height / 2
            val baseRadius = size.minDimension * 0.3f * scalePulse

            // 1. Outer Aura Glow
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(PrimaryPurple.copy(alpha = 0.2f), Color.Transparent),
                    center = Offset(cx, cy),
                    radius = baseRadius * 2f
                ),
                radius = baseRadius * 2f,
                center = Offset(cx, cy)
            )

            // 2. Rotating Neural Rings
            val ringCount = 3
            for (i in 0 until ringCount) {
                val rot = if (i % 2 == 0) rotation1 else rotation2
                val speedFactor = 1f + i * 0.2f
                val currentRot = rot * speedFactor
                
                // Jitter effect
                val jX = if (isFinished) 0f else (Random.nextFloat() - 0.5f) * jitterAmount
                val jY = if (isFinished) 0f else (Random.nextFloat() - 0.5f) * jitterAmount

                withTransform({
                    rotate(currentRot, Offset(cx + jX, cy + jY))
                }) {
                    drawArc(
                        brush = Brush.sweepGradient(
                            colors = listOf(PrimaryPurple, NeonPink, PrimaryPurple)
                        ),
                        startAngle = 0f,
                        sweepAngle = 280f,
                        useCenter = false,
                        topLeft = Offset(cx - baseRadius * (1f + i * 0.2f) + jX, cy - baseRadius * (1f + i * 0.2f) + jY),
                        size = Size(baseRadius * 2 * (1f + i * 0.2f), baseRadius * 2 * (1f + i * 0.2f)),
                        style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round),
                        alpha = 0.6f
                    )
                }
            }

            // 3. Glowing Core
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color.White, PrimaryPurple, Color.Transparent),
                    center = Offset(cx, cy),
                    radius = baseRadius * 0.8f
                ),
                radius = baseRadius * 0.8f,
                center = Offset(cx, cy),
                alpha = 0.8f
            )

            // 4. Particle Field (Mind fragments settling)
            if (!isFinished) {
                val random = Random(42)
                repeat(20) {
                    val angle = random.nextFloat() * 360f
                    val dist = baseRadius * (1.2f + random.nextFloat() * 0.8f)
                    val rad = (angle * PI / 180.0).toFloat()
                    val px = cx + cos(rad.toDouble()).toFloat() * dist
                    val py = cy + sin(rad.toDouble()).toFloat() * dist
                    
                    drawCircle(
                        color = NeonPink.copy(alpha = 0.3f),
                        radius = 2.dp.toPx(),
                        center = Offset(px, py)
                    )
                }
            }
        }

        // Percentage Text
        if (!isFinished) {
            Text(
                text = "${(progress * 100).toInt()}%",
                style = MaterialTheme.typography.displaySmall,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
