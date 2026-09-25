package ar.net.dahool.upsmonitor.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ar.net.dahool.upsmonitor.ui.theme.UpsColors

@Composable
fun StatusBadge(status: String, modifier: Modifier = Modifier) {
    val (color, label) = when {
        status.contains("Online", ignoreCase = true) -> UpsColors.online to "ONLINE"
        status.contains("Low Battery", ignoreCase = true) -> UpsColors.critical to "LOW BATTERY"
        status.contains("Battery", ignoreCase = true) -> UpsColors.battery to "ON BATTERY"
        status.contains("Error", ignoreCase = true) ||
        status.contains("Disconnected", ignoreCase = true) -> UpsColors.critical to "ERROR"
        else -> UpsColors.unknown to status.uppercase()
    }

    val animatedColor by animateColorAsState(
        targetValue = color,
        animationSpec = tween(500),
        label = "statusColor"
    )

    // Pulse animation for non-online states
    val shouldPulse = !status.contains("Online", ignoreCase = true)
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (shouldPulse) 1.5f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.9f,
        targetValue = if (shouldPulse) 0.2f else 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(animatedColor.copy(alpha = 0.12f))
            .padding(horizontal = 14.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Animated dot
        Box(contentAlignment = Alignment.Center) {
            // Outer ring (pulse)
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .scale(pulseScale)
                    .clip(CircleShape)
                    .background(animatedColor.copy(alpha = pulseAlpha))
            )
            // Inner solid dot
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(animatedColor)
            )
        }

        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.W700,
                letterSpacing = 1.sp,
                color = animatedColor
            )
        )
    }
}
