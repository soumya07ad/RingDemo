package com.dkgs.innerpulse.presentation.ring.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dkgs.innerpulse.ui.theme.NeonBlue
import com.dkgs.innerpulse.ui.theme.NeonCyan
import com.dkgs.innerpulse.ui.theme.PrimaryPurple

@Composable
fun RingTypeSelector(
    selectedType: Int,
    onTypeChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "SELECT RING TYPE",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            letterSpacing = 1.sp
        )
        
        Spacer(modifier = Modifier.height(10.dp))
        
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                    shape = RoundedCornerShape(16.dp)
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TypeOption(
                label = "研强 (Type 1)",
                isSelected = selectedType == 1,
                onClick = { onTypeChange(1) },
                modifier = Modifier.weight(1f)
            )
            
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .fillMaxHeight(0.5f)
                    .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
            )
            
            TypeOption(
                label = "小七 (Type 2)",
                isSelected = selectedType == 2,
                onClick = { onTypeChange(2) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun TypeOption(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bgColor by animateColorAsState(
        targetValue = if (isSelected) NeonCyan.copy(alpha = 0.15f) else Color.Transparent,
        animationSpec = tween(300),
        label = "bgColor"
    )
    
    val textColor by animateColorAsState(
        targetValue = if (isSelected) NeonCyan else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = tween(300),
        label = "textColor"
    )

    Box(
        modifier = modifier
            .fillMaxHeight()
            .clickable(onClick = onClick)
            .background(bgColor),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = textColor,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
        )
        
        if (isSelected) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth(0.4f)
                    .height(3.dp)
                    .background(
                        brush = Brush.horizontalGradient(
                            colors = listOf(NeonCyan.copy(alpha = 0f), NeonCyan, NeonCyan.copy(alpha = 0f))
                        )
                    )
            )
        }
    }
}
