package com.curio.app.ui.screens.journal

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.curio.app.data.local.EntryType
import com.curio.app.viewmodel.JournalViewModel
import com.curio.app.viewmodel.TaskItem
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// ── Moleskine-inspired notebook palette ──
private val notebookPaper = Color(0xFF1A1A18)
private val notebookLine = Color(0xFF2E2E2B)
private val bindingStrip = Color(0xFF222220)
private val bindingWire = Color(0xFF8A8A80)
private val bindingWireShadow = Color(0xFF3A3A35)
private val inkColor = Color(0xFFD4D0C8)
private val inkMuted = Color(0xFF8A867E)
private val accent = Color(0xFFD4A373)          // warm copper/amber (replaces gold)
private val accentDim = Color(0xFFB8895A)         // darker accent
private val pageShadowRight = Color(0xFF0A0A09).copy(alpha = 0.35f)
private val pageShadowBottom = Color(0xFF0A0A09).copy(alpha = 0.25f)

// ── Font setting mappings ──

private fun resolveFontFamily(key: String): FontFamily = when (key) {
    "sans_serif" -> FontFamily.SansSerif
    "monospace" -> FontFamily.Monospace
    "cursive" -> FontFamily.Cursive
    "default" -> FontFamily.Default
    else -> FontFamily.Serif
}

private val fontFamilyLabels = mapOf(
    "serif" to "Serif",
    "sans_serif" to "Sans",
    "monospace" to "Mono",
    "cursive" to "Cur.",
    "default" to "Syst."
)

private fun resolveBodySize(key: String): androidx.compose.ui.unit.TextUnit = when (key) {
    "small" -> 14.sp
    "large" -> 18.sp
    "xlarge" -> 20.sp
    else -> 16.sp
}

private fun resolveTitleSize(key: String): androidx.compose.ui.unit.TextUnit = when (key) {
    "small" -> 18.sp
    "large" -> 24.sp
    "xlarge" -> 28.sp
    else -> 22.sp
}

private fun resolveSmallSize(key: String): androidx.compose.ui.unit.TextUnit = when (key) {
    "small" -> 12.sp
    "large" -> 14.sp
    "xlarge" -> 15.sp
    else -> 13.sp
}

private val fontSizeLabels = listOf(
    "small" to "S",
    "medium" to "M",
    "large" to "L",
    "xlarge" to "XL"
)

private val lineSpacingOptions = listOf(
    1.2f to "Tight",
    1.5f to "Normal",
    1.8f to "Relaxed"
)

// ── Main composable ──

@Composable
fun JournalEditorScreen(
    viewModel: JournalViewModel,
    onBack: () -> Unit = {}
) {
    val state by viewModel.uiState.collectAsState()
    val dateFormat = SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.getDefault())

    // Resolve font settings
    val currentFamily = resolveFontFamily(viewModel.fontFamily)
    val currentBodySize = resolveBodySize(viewModel.fontSize)
    val currentTitleSize = resolveTitleSize(viewModel.fontSize)
    val currentSmallSize = resolveSmallSize(viewModel.fontSize)
    val currentLineHeight = currentBodySize * viewModel.lineSpacing
    val currentTitleLineHeight = currentTitleSize * (viewModel.lineSpacing * 0.85f)

    val notebookTextStyle = TextStyle(
        fontSize = currentBodySize,
        fontFamily = currentFamily,
        color = inkColor,
        lineHeight = currentLineHeight
    )

    val notebookTitleStyle = TextStyle(
        fontSize = currentTitleSize,
        fontFamily = currentFamily,
        color = inkColor,
        fontWeight = FontWeight.Bold,
        lineHeight = currentTitleLineHeight
    )

    val notebookLabelStyle = TextStyle(
        fontSize = currentSmallSize,
        fontFamily = currentFamily,
        color = inkMuted.copy(alpha = 0.8f),
        lineHeight = currentBodySize * 1.8f
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(notebookPaper)
    ) {
        // ── Notebook Canvas ──
        val lineSpacingDp = 32.dp
        val marginOffsetDp = 28.dp
        val bindingHeightDp = 36.dp

        Canvas(modifier = Modifier.fillMaxSize()) {
            val lineSpacing = lineSpacingDp.toPx()
            val marginOffset = marginOffsetDp.toPx()
            val bindingH = bindingHeightDp.toPx()
            val paperW = size.width
            val paperH = size.height

            // Ruled lines
            var y = bindingH
            while (y <= paperH) {
                drawLine(
                    color = notebookLine,
                    start = Offset(marginOffset, y),
                    end = Offset(paperW, y),
                    strokeWidth = 0.7f
                )
                y += lineSpacing
            }

            // Binding strip
            drawRect(
                color = bindingStrip,
                topLeft = Offset(0f, 0f),
                size = Size(paperW, bindingH)
            )

            // Gold foil separator
            drawLine(
                color = accent.copy(alpha = 0.25f),
                start = Offset(marginOffset, bindingH),
                end = Offset(paperW - 20.dp.toPx(), bindingH),
                strokeWidth = 0.5f
            )

            // Wire spiral binding rings
            val wireCount = 7
            val wireSpacing = (paperW - 40.dp.toPx()) / (wireCount + 1)
            val wireStartX = 20.dp.toPx()
            val wireRadius = 5.dp.toPx()
            val wireInset = 3.dp.toPx()

            for (i in 1..wireCount) {
                val cx = wireStartX + wireSpacing * i
                drawCircle(
                    color = bindingWireShadow,
                    radius = wireRadius + 0.5f,
                    center = Offset(cx, bindingH / 2 + 1.dp.toPx())
                )
                drawCircle(
                    color = bindingWire,
                    radius = wireRadius,
                    center = Offset(cx, bindingH / 2),
                    style = Stroke(width = 1.8f)
                )
                drawCircle(
                    color = Color.White.copy(alpha = 0.15f),
                    radius = wireRadius * 0.35f,
                    center = Offset(cx - wireRadius * 0.3f, bindingH / 2 - wireRadius * 0.3f)
                )
                val path = Path().apply {
                    moveTo(cx - wireRadius * 0.6f, bindingH / 2 - wireRadius * 0.2f)
                    quadraticBezierTo(
                        cx - wireInset, bindingH / 2 - wireRadius * 1.2f,
                        cx + wireRadius * 0.6f, bindingH / 2 - wireRadius * 0.2f
                    )
                }
                drawPath(path, bindingWire, style = Stroke(width = 1.2f))
            }

            // Page shadow (right edge)
            val shadowWidth = 12.dp.toPx()
            drawRect(
                brush = Brush.horizontalGradient(
                    colors = listOf(Color.Transparent, pageShadowRight),
                    startX = paperW - shadowWidth,
                    endX = paperW
                ),
                topLeft = Offset(paperW - shadowWidth, bindingH),
                size = Size(shadowWidth, paperH - bindingH)
            )

            // Page shadow (bottom edge)
            val shadowHeight = 8.dp.toPx()
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(Color.Transparent, pageShadowBottom),
                    startY = paperH - shadowHeight,
                    endY = paperH
                ),
                topLeft = Offset(0f, paperH - shadowHeight),
                size = Size(paperW, shadowHeight)
            )

        }

        // ── Content overlay ──
        Column(modifier = Modifier.fillMaxSize()) {
            // ── Notebook top bar ──
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(bindingStrip)
                    .padding(horizontal = 4.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = {
                    viewModel.cancelEditing()
                    onBack()
                }) {
                    Icon(Icons.Filled.ArrowBack, "Back", tint = inkMuted)
                }

                // Wire ring top exposures (decorative)
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.Center
                ) {
                    for (i in 0 until 7) {
                        Box(
                            modifier = Modifier
                                .padding(horizontal = if (i == 0) 12.dp else 8.dp)
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(notebookPaper.copy(alpha = 0.5f))
                        )
                    }
                }

                // ── Aa font settings toggle ──
                Box(
                    modifier = Modifier
                        .padding(end = 2.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(
                            if (viewModel.showFontSettings) accent.copy(alpha = 0.18f)
                            else Color.Transparent
                        )
                        .clickable { viewModel.toggleFontSettings() }
                        .padding(horizontal = 6.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "Aa",
                        style = TextStyle(
                            fontSize = 14.sp,
                            fontFamily = FontFamily.Serif,
                            color = if (viewModel.showFontSettings) accent else inkMuted,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                    )
                }

                // Save button
                val hasContent = when (state.editType) {
                    "gratitude" -> state.editGratitudeItems.any { it.isNotBlank() }
                    "task_list" -> state.editTaskItems.any { it.text.isNotBlank() }
                    "reflection" -> state.editReflectionAnswers.any { it.isNotBlank() }
                    else -> state.editTitle.isNotBlank() || state.editContent.isNotBlank()
                }
                Box(
                    modifier = Modifier
                        .padding(end = 8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(accent.copy(alpha = 0.12f))
                        .clickable(enabled = !state.isSaving) { viewModel.saveEntry() }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Filled.Check, "Save",
                            tint = if (hasContent) accent else inkMuted.copy(alpha = 0.4f),
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Save",
                            style = TextStyle(
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Serif,
                                color = if (hasContent) accent else inkMuted.copy(alpha = 0.4f),
                                fontWeight = FontWeight.Medium,
                                letterSpacing = 0.5.sp
                            )
                        )
                    }
                }
            }

            // ── Font Settings Panel ──
            AnimatedVisibility(
                visible = viewModel.showFontSettings,
                enter = expandVertically(expandFrom = Alignment.Top),
                exit = shrinkVertically(shrinkTowards = Alignment.Top)
            ) {
                FontSettingsPanel(
                    currentFamily = viewModel.fontFamily,
                    currentSize = viewModel.fontSize,
                    currentSpacing = viewModel.lineSpacing,
                    onFamilyChange = { viewModel.updateFontFamily(it) },
                    onSizeChange = { viewModel.updateFontSize(it) },
                    onSpacingChange = { viewModel.updateLineSpacing(it) }
                )
            }

            // Error
            if (state.error != null) {
                Text(
                    text = state.error!!,
                    style = TextStyle(fontSize = 11.sp, fontFamily = FontFamily.Serif, color = Color(0xFFFF5252)),
                    modifier = Modifier.padding(start = 36.dp, end = 16.dp, top = 4.dp)
                )
            }

            // ── Scrollable notebook content ──
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(start = 36.dp, end = 36.dp)
            ) {
                Spacer(modifier = Modifier.height(8.dp))

                // Date
                Text(
                    text = dateFormat.format(Date(state.selectedDate)),
                    style = TextStyle(
                        fontSize = currentSmallSize,
                        fontFamily = FontFamily.Serif,
                        color = accent.copy(alpha = 0.6f),
                        letterSpacing = 0.8.sp,
                        fontWeight = FontWeight.Medium
                    )
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Gold foil header divider
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(0.5.dp)
                        .background(accent.copy(alpha = 0.15f))
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Entry type chips
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    EntryType.entries.forEach { type ->
                        val isSelected = state.editType == type.key
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(3.dp))
                                .background(
                                    if (isSelected) accent.copy(alpha = 0.15f)
                                    else notebookLine.copy(alpha = 0.4f)
                                )
                                .clickable { viewModel.updateType(type.key) }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "${type.icon} ${type.displayName}",
                                style = TextStyle(
                                    fontSize = currentSmallSize * 0.77f,
                                    fontFamily = FontFamily.Serif,
                                    color = if (isSelected) accent else inkMuted.copy(alpha = 0.5f),
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    letterSpacing = 0.3.sp
                                )
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Type-specific content
                when (state.editType) {
                    "gratitude" -> GratitudeEditor(state.editGratitudeItems, state.editGratitudePrompts, viewModel, notebookTextStyle, notebookTitleStyle, notebookLabelStyle)
                    "task_list" -> TaskListEditor(state.editTaskItems, viewModel, notebookTextStyle, notebookTitleStyle, notebookLabelStyle)
                    "reflection" -> ReflectionEditor(state.editReflectionAnswers, state.editReflectionPrompts, viewModel, notebookTextStyle, notebookTitleStyle, notebookLabelStyle)
                    else -> FreeWriteEditor(state.editTitle, state.editContent, state.editTags, viewModel, notebookTextStyle, notebookTitleStyle, notebookLabelStyle)
                }

                // Gold foil page number
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = "— 1 —",
                    style = TextStyle(
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Serif,
                        color = accent.copy(alpha = 0.3f),
                        letterSpacing = 1.sp
                    ),
                    modifier = Modifier.padding(start = 8.dp)
                )

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

// ── Font Settings Panel ──

@Composable
private fun FontSettingsPanel(
    currentFamily: String,
    currentSize: String,
    currentSpacing: Float,
    onFamilyChange: (String) -> Unit,
    onSizeChange: (String) -> Unit,
    onSpacingChange: (Float) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF222220))
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        // Font family row
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "Font",
                style = TextStyle(
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Serif,
                    color = accent.copy(alpha = 0.5f),
                    letterSpacing = 0.5.sp,
                    fontWeight = FontWeight.Medium
                ),
                modifier = Modifier.width(36.dp)
            )
            fontFamilyLabels.entries.forEach { (key, label) ->
                val isActive = currentFamily == key
                Box(
                    modifier = Modifier
                        .padding(end = 6.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(
                            if (isActive) accent.copy(alpha = 0.15f)
                            else notebookLine.copy(alpha = 0.3f)
                        )
                        .clickable { onFamilyChange(key) }
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = label,
                        style = TextStyle(
                            fontSize = 11.sp,
                            fontFamily = resolveFontFamily(key),
                            color = if (isActive) accent else inkMuted,
                            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal
                        )
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Font size row
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "Size",
                style = TextStyle(
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Serif,
                    color = accent.copy(alpha = 0.5f),
                    letterSpacing = 0.5.sp,
                    fontWeight = FontWeight.Medium
                ),
                modifier = Modifier.width(36.dp)
            )
            fontSizeLabels.forEach { (key, label) ->
                val isActive = currentSize == key
                Box(
                    modifier = Modifier
                        .padding(end = 6.dp)
                        .clip(CircleShape)
                        .size(if (isActive) 30.dp else 26.dp)
                        .background(
                            if (isActive) accent.copy(alpha = 0.15f)
                            else Color.Transparent
                        )
                        .clickable { onSizeChange(key) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        style = TextStyle(
                            fontSize = when (key) {
                                "small" -> 10.sp
                                "medium" -> 12.sp
                                "large" -> 14.sp
                                else -> 15.sp
                            },
                            fontFamily = FontFamily.Serif,
                            color = if (isActive) accent else inkMuted,
                            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal
                        )
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Line spacing row
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "Space",
                style = TextStyle(
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Serif,
                    color = accent.copy(alpha = 0.5f),
                    letterSpacing = 0.5.sp,
                    fontWeight = FontWeight.Medium
                ),
                modifier = Modifier.width(36.dp)
            )
            lineSpacingOptions.forEach { (value, label) ->
                val isActive = currentSpacing == value
                Box(
                    modifier = Modifier
                        .padding(end = 6.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(
                            if (isActive) accent.copy(alpha = 0.15f)
                            else notebookLine.copy(alpha = 0.3f)
                        )
                        .clickable { onSpacingChange(value) }
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = label,
                        style = TextStyle(
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Serif,
                            color = if (isActive) accent else inkMuted,
                            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal
                        )
                    )
                }
            }
        }
    }
}

// ── Free Write ──

@Composable
private fun FreeWriteEditor(
    title: String,
    content: String,
    tags: String,
    viewModel: JournalViewModel,
    notebookTextStyle: TextStyle,
    notebookTitleStyle: TextStyle,
    notebookLabelStyle: TextStyle
) {
    BasicTextField(
        value = title,
        onValueChange = { viewModel.updateTitle(it) },
        textStyle = notebookTitleStyle,
        cursorBrush = SolidColor(accent),
        modifier = Modifier.fillMaxWidth(),
        decorationBox = { innerTextField ->
            Box {
                if (title.isEmpty()) {
                    Text(
                        text = "Title",
                        style = notebookTitleStyle.copy(color = inkMuted.copy(alpha = 0.25f))
                    )
                }
                innerTextField()
            }
        },
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
    )

    Spacer(modifier = Modifier.height(4.dp))

    BasicTextField(
        value = content,
        onValueChange = { viewModel.updateContent(it) },
        textStyle = notebookTextStyle,
        cursorBrush = SolidColor(accent),
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 280.dp),
        decorationBox = { innerTextField ->
            Box {
                if (content.isEmpty()) {
                    Text(
                        text = "Write whatever comes to mind...",
                        style = notebookTextStyle.copy(color = inkMuted.copy(alpha = 0.25f))
                    )
                }
                innerTextField()
            }
        },
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Default)
    )

    Spacer(modifier = Modifier.height(12.dp))

    Text(
        text = "Tags:",
        style = notebookLabelStyle
    )
    BasicTextField(
        value = tags,
        onValueChange = { viewModel.updateTags(it) },
        textStyle = notebookTextStyle.copy(fontSize = notebookTextStyle.fontSize * 0.875f, color = inkMuted),
        cursorBrush = SolidColor(accent),
        modifier = Modifier.fillMaxWidth(),
        decorationBox = { innerTextField ->
            Box {
                if (tags.isEmpty()) {
                    Text(
                        text = "comma-separated tags",
                        style = notebookTextStyle.copy(fontSize = notebookTextStyle.fontSize * 0.875f, color = inkMuted.copy(alpha = 0.25f))
                    )
                }
                innerTextField()
            }
        },
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done)
    )
}

// ── Gratitude ──

@Composable
private fun GratitudeEditor(
    items: List<String>,
    prompts: List<String>,
    viewModel: JournalViewModel,
    notebookTextStyle: TextStyle,
    notebookTitleStyle: TextStyle,
    notebookLabelStyle: TextStyle
) {
    Text(
        text = "What are you grateful for today?",
        style = notebookTitleStyle.copy(fontSize = notebookTitleStyle.fontSize * 0.82f)
    )

    Spacer(modifier = Modifier.height(16.dp))

    items.forEachIndexed { index, text ->
        val prompt = prompts.getOrElse(index) { "" }
        Row(verticalAlignment = Alignment.Top) {
            Box(
                modifier = Modifier
                    .padding(top = 8.dp)
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(accent.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "${index + 1}",
                    style = TextStyle(
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Serif,
                        color = accent,
                        fontWeight = FontWeight.Bold
                    )
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = prompt,
                    style = notebookLabelStyle
                )
                BasicTextField(
                    value = text,
                    onValueChange = { viewModel.updateGratitudeItem(index, it) },
                    textStyle = notebookTextStyle.copy(fontSize = notebookTextStyle.fontSize * 0.94f),
                    cursorBrush = SolidColor(accent),
                    modifier = Modifier.fillMaxWidth(),
                    decorationBox = { innerTextField ->
                        Box {
                            if (text.isEmpty()) {
                                Text(
                                    text = "Write something...",
                                    style = notebookTextStyle.copy(fontSize = notebookTextStyle.fontSize * 0.94f, color = inkMuted.copy(alpha = 0.25f))
                                )
                            }
                            innerTextField()
                        }
                    },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
                )
            }
        }
        Spacer(modifier = Modifier.height(20.dp))
    }
}

// ── Task List ──

@Composable
private fun TaskListEditor(
    items: List<TaskItem>,
    viewModel: JournalViewModel,
    notebookTextStyle: TextStyle,
    notebookTitleStyle: TextStyle,
    notebookLabelStyle: TextStyle
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Task List",
            style = notebookTitleStyle.copy(fontSize = notebookTitleStyle.fontSize * 0.82f)
        )
        Text(
            text = "${items.count { it.done }}/${items.size} done",
            style = notebookLabelStyle
        )
    }

    Spacer(modifier = Modifier.height(12.dp))

    if (items.isEmpty()) {
        Text(
            text = "No tasks yet. Tap the + below to add one.",
            style = notebookTextStyle.copy(color = inkMuted.copy(alpha = 0.35f)),
            modifier = Modifier.padding(vertical = 16.dp)
        )
    } else {
        items.forEach { task ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(
                            if (task.done) accent.copy(alpha = 0.15f)
                            else notebookLine.copy(alpha = 0.4f)
                        )
                        .clickable { viewModel.toggleTaskDone(task.id) },
                    contentAlignment = Alignment.Center
                ) {
                    if (task.done) {
                        Text(
                            "\u2713",
                            style = TextStyle(fontSize = 12.sp, color = accent, fontWeight = FontWeight.Bold)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(10.dp))

                BasicTextField(
                    value = task.text,
                    onValueChange = { viewModel.updateTaskText(task.id, it) },
                    textStyle = notebookTextStyle.copy(
                        fontSize = notebookTextStyle.fontSize * 0.94f,
                        color = if (task.done) inkMuted.copy(alpha = 0.5f) else inkColor,
                        textDecoration = if (task.done) TextDecoration.LineThrough else TextDecoration.None
                    ),
                    cursorBrush = SolidColor(accent),
                    modifier = Modifier.weight(1f),
                    decorationBox = { innerTextField ->
                        Box {
                            if (task.text.isEmpty()) {
                                Text(
                                    text = "What needs to be done?",
                                    style = notebookTextStyle.copy(fontSize = notebookTextStyle.fontSize * 0.94f, color = inkMuted.copy(alpha = 0.25f))
                                )
                            }
                            innerTextField()
                        }
                    },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
                )

                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .clickable { viewModel.deleteTaskItem(task.id) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "\u2715",
                        style = TextStyle(fontSize = 12.sp, color = inkMuted.copy(alpha = 0.25f))
                    )
                }
            }
        }
    }

    Spacer(modifier = Modifier.height(8.dp))

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(3.dp))
            .clickable { viewModel.addTaskItem() }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(22.dp)
                .clip(CircleShape)
                .background(accent.copy(alpha = 0.08f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "+",
                style = TextStyle(fontSize = 16.sp, color = accent, fontWeight = FontWeight.Bold)
            )
        }
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = "Add task",
            style = notebookLabelStyle.copy(color = accent.copy(alpha = 0.7f))
        )
    }
}

// ── Reflection ──

@Composable
private fun ReflectionEditor(
    answers: List<String>,
    prompts: List<String>,
    viewModel: JournalViewModel,
    notebookTextStyle: TextStyle,
    notebookTitleStyle: TextStyle,
    notebookLabelStyle: TextStyle
) {
    Text(
        text = "Daily Reflection",
        style = notebookTitleStyle.copy(fontSize = notebookTitleStyle.fontSize * 0.82f)
    )
    Text(
        text = "Take a moment to reflect on your day",
        style = notebookLabelStyle,
        modifier = Modifier.padding(bottom = 16.dp)
    )

    answers.forEachIndexed { index, text ->
        val prompt = prompts.getOrElse(index) { "" }
        val emoji = when (index) {
            0 -> "\uD83C\uDF1F"
            1 -> "\uD83D\uDCAA"
            2 -> "\uD83D\uDCDA"
            else -> "\uD83D\uDCAD"
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = emoji, fontSize = 15.sp)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = prompt,
                style = notebookLabelStyle.copy(
                    color = accent.copy(alpha = 0.85f),
                    fontWeight = FontWeight.SemiBold
                )
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        BasicTextField(
            value = text,
            onValueChange = { viewModel.updateReflectionAnswer(index, it) },
            textStyle = notebookTextStyle.copy(fontSize = notebookTextStyle.fontSize * 0.94f),
            cursorBrush = SolidColor(accent),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 80.dp),
            decorationBox = { innerTextField ->
                Box {
                    if (text.isEmpty()) {
                        Text(
                            text = "Write your thoughts...",
                            style = notebookTextStyle.copy(fontSize = notebookTextStyle.fontSize * 0.94f, color = inkMuted.copy(alpha = 0.25f))
                        )
                    }
                    innerTextField()
                }
            },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
        )

        Spacer(modifier = Modifier.height(24.dp))
    }
}
