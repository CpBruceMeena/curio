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

/**
 * Get emoji for a category name (used in discover and onboarding).
 */
fun getCategoryEmoji(categoryName: String): String {
    val iconMap = mapOf(
        "Science" to "\uD83E\uDDEC",
        "Space" to "\uD83D\uDE80",
        "History" to "\uD83D\uDCDC",
        "Biology" to "\uD83C\uDF3F",
        "Psychology" to "\uD83E\uDDE0",
        "Philosophy" to "\u2696\uFE0F",
        "Physics" to "\u269B\uFE0F",
        "Startups" to "\uD83D\uDCA1",
        "AI" to "\uD83E\uDD16",
        "Economics" to "\uD83C\uDFDB\uFE0F",
        "Nature" to "\uD83C\uDF32",
        "Technology" to "\uD83D\uDCBB",
        "Poetry" to "\uD83D\uDCD6",
        "Movies" to "\uD83C\uDFAC",
        "Neuroscience" to "\uD83D\uDD2C",
        "Literature" to "\uD83D\uDCDA",
        "Geography" to "\uD83C\uDF0D",
        "Music" to "\uD83C\uDFB5",
        "Sports" to "\u26BD",
        "Food" to "\uD83C\uDF5C",
        "Shayari" to "\u270D\uFE0F",
        "Puzzles" to "\uD83E\uDDE9",
        "Short Stories" to "\uD83D\uDCC4",
        "Facts" to "\uD83D\uDCA1",
        "Poems" to "\uD83C\uDF3B",
        "Sudoku" to "\uD83D\uDD22",
        "Math Puzzles" to "\u2797",
        "Logic Puzzles" to "\uD83E\uDDE0",
        "Word Puzzles" to "\uD83D\uDCDD",
        "Mixed Puzzles" to "\uD83E\uDDE9",
        "English Poems" to "\uD83D\uDCDD",
        "Hindi Poems" to "\uD83C\uDFB5",
        "Classics" to "\uD83C\uDFDB\uFE0F",
        "Modern" to "\uD83C\uDFA8",
        "Classic Fiction" to "\uD83D\uDCD6",
        "Micro Stories" to "\uD83D\uDCD0",
        "Serialized Stories" to "\uD83D\uDCCA"
    )
    return iconMap[categoryName] ?: getIconEmoji(categoryName.lowercase().replace(" ", "_")) ?: ""
}

private fun getIconEmoji(iconName: String): String {
    return when (iconName) {
        "biotech" -> "\uD83E\uDDEC"       // 🧬 Science
        "rocket_launch" -> "\uD83D\uDE80" // 🚀 Space
        "history_edu" -> "\uD83D\uDCDC"  // 📜 History
        "eco" -> "\uD83C\uDF3F"          // 🌿 Biology
        "psychology" -> "\uD83E\uDDE0"   // 🧠 Psychology
        "balance_scale" -> "\u2696\uFE0F" // ⚖️ Philosophy
        "atom" -> "\u269B\uFE0F"         // ⚛️ Physics
        "lightbulb" -> "\uD83D\uDCA1"    // 💡 Startups
        "smart_toy" -> "\uD83E\uDD16"    // 🤖 AI
        "account_balance" -> "\uD83C\uDFDB\uFE0F" // 🏛️ Economics
        "forest" -> "\uD83C\uDF32"       // 🌲 Nature
        "computer" -> "\uD83D\uDCBB"     // 💻 Technology
        "auto_stories" -> "\uD83D\uDCD6" // 📖 Poetry
        "movie" -> "\uD83C\uDFAC"        // 🎬 Movies
        "microscope" -> "\uD83D\uDD2C"   // 🔬 Neuroscience
        "menu_book" -> "\uD83D\uDCDA"    // 📚 Literature
        "public" -> "\uD83C\uDF0D"       // 🌍 Geography
        "music_note" -> "\uD83C\uDFB5"   // 🎵 Music
        "sports_soccer" -> "\u26BD"      // ⚽ Sports
        "ramen_dining" -> "\uD83C\uDF5C" // 🍜 Food
        "edit_note" -> "\u270D\uFE0F"    // ✍️ Shayari
        "extension" -> "\uD83E\uDDE9"   // 🧩 Puzzles
        "article" -> "\uD83D\uDCC4"     // 📄 Short Stories
        "english_poems" -> "\uD83D\uDCDD"   // 📝 English Poems
        "hindi_poems" -> "\uD83C\uDFB5"    // 🎵 Hindi Poems
        "classics" -> "\uD83C\uDFDB\uFE0F"  // 🏛️ Classics
        "modern" -> "\uD83C\uDFA8"         // 🎨 Modern
        "classic_fiction" -> "\uD83D\uDCD6"  // 📖 Classic Fiction
        "micro_stories" -> "\uD83D\uDCD0"   // 📐 Micro Stories
        "serialized_stories" -> "\uD83D\uDCCA" // 📊 Serialized Stories
        else -> "\u2728"                 // ✨ fallback
    }
}
