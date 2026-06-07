package com.curio.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.curio.app.ui.theme.GlassBorder
import com.curio.app.ui.theme.GlassWhite
import com.curio.app.ui.theme.OnSurfaceVariant
import com.curio.app.ui.theme.SecondaryContainer
import com.curio.app.ui.theme.SurfaceContainer

@Composable
fun InterestChip(
    name: String,
    icon: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bgColor by animateColorAsState(
        targetValue = if (isSelected) Color(0x1A00F4FE) else Color(0x00FFFFFF),
        animationSpec = spring(),
        label = "bg"
    )

    val borderColor by animateColorAsState(
        targetValue = if (isSelected) SecondaryContainer else GlassBorder,
        animationSpec = spring(),
        label = "border"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(SurfaceContainer.copy(alpha = 0.6f))
            .border(1.dp, borderColor, RoundedCornerShape(16.dp))
            .background(bgColor)
            .clickable { onClick() }
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .padding(bottom = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = getIconEmoji(icon),
                style = MaterialTheme.typography.headlineLarge
            )
        }

        Text(
            text = name,
            style = MaterialTheme.typography.labelLarge,
            color = OnSurfaceVariant,
            fontWeight = FontWeight.SemiBold
        )

        if (isSelected) {
            Icon(
                imageVector = Icons.Filled.CheckCircle,
                contentDescription = "Selected",
                modifier = Modifier
                    .size(20.dp)
                    .padding(top = 4.dp),
                tint = SecondaryContainer
            )
        }
    }
}

private fun getIconEmoji(iconName: String): String {
    return when (iconName) {
        "neurology" -> "🧠"
        "history_edu" -> "📜"
        "rocket_launch" -> "🚀"
        "biotech" -> "🧬"
        "lightbulb" -> "💡"
        "psychology" -> "🧠"
        "atom" -> "⚛️"
        "account_balance" -> "🏛️"
        "palette" -> "🎨"
        "forest" -> "🌲"
        "memory" -> "💾"
        "design" -> "🎨"
        "nature" -> "🌲"
        "technology" -> "💻"
        else -> "✨"
    }
}
