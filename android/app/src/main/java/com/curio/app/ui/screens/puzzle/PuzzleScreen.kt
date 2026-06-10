package com.curio.app.ui.screens.puzzle

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.curio.app.data.model.Puzzle
import com.curio.app.data.repository.ContentRepository
import com.curio.app.ui.theme.Error
import com.curio.app.ui.theme.OnSurface
import com.curio.app.ui.theme.OnSurfaceVariant
import com.curio.app.ui.theme.Primary
import com.curio.app.ui.theme.SecondaryContainer
import com.curio.app.ui.theme.Surface
import com.curio.app.ui.theme.SurfaceContainerHigh
import kotlinx.coroutines.launch

@Composable
fun PuzzleScreen(
    categoryId: Long,
    puzzleType: String,
    onBack: () -> Unit,
    onAllDone: () -> Unit
) {
    val repository = remember { ContentRepository() }
    val scope = rememberCoroutineScope()
    var puzzles by remember { mutableStateOf<List<Puzzle>>(emptyList()) }
    var currentIndex by remember { mutableIntStateOf(0) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    // Fetch puzzles on launch
    LaunchedEffect(categoryId, puzzleType) {
        isLoading = true
        val typeParam = if (puzzleType.isNotEmpty()) puzzleType else null
        val catParam = if (categoryId > 0) categoryId else null
        repository.getPuzzles(
            puzzleType = typeParam,
            categoryId = catParam,
            limit = 30
        ).onSuccess { response ->
            puzzles = response.puzzles
            isLoading = false
        }.onFailure { e ->
            error = e.message ?: "Failed to load puzzles"
            isLoading = false
        }
    }

    if (isLoading) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Surface)
                .statusBarsPadding(),
            contentAlignment = Alignment.Center
        ) {
            Text(text = "Loading puzzles...", style = MaterialTheme.typography.bodyLarge, color = OnSurfaceVariant)
        }
        return
    }

    if (error != null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Surface)
                .statusBarsPadding(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = "\u26A0\uFE0F", fontSize = 48.sp)
                Spacer(modifier = Modifier.height(16.dp))
                Text(text = "Couldn't load puzzles", style = MaterialTheme.typography.bodyLarge, color = Error)
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(onClick = { onBack() }) {
                    Text("Go Back")
                }
            }
        }
        return
    }

    if (puzzles.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Surface)
                .statusBarsPadding(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = "\uD83E\uDDE9", fontSize = 48.sp)
                Spacer(modifier = Modifier.height(16.dp))
                Text(text = "No puzzles yet", style = MaterialTheme.typography.bodyLarge, color = OnSurfaceVariant)
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(onClick = { onBack() }) {
                    Text("Go Back")
                }
            }
        }
        return
    }

    // Show current puzzle
    val puzzle = puzzles.getOrNull(currentIndex) ?: puzzles.last()
    InteractivePuzzleView(
        puzzle = puzzle,
        puzzleNumber = currentIndex + 1,
        totalPuzzles = puzzles.size,
        onBack = onBack,
        onNext = {
            if (currentIndex + 1 < puzzles.size) {
                currentIndex++
            } else {
                onAllDone()
            }
        }
    )
}

@Composable
private fun InteractivePuzzleView(
    puzzle: Puzzle,
    puzzleNumber: Int,
    totalPuzzles: Int,
    onBack: () -> Unit,
    onNext: () -> Unit
) {
    val repository = remember { ContentRepository() }
    val scope = rememberCoroutineScope()
    var userAnswer by remember { mutableStateOf("") }
    var result by remember { mutableStateOf<Boolean?>(null) }
    var explanation by remember { mutableStateOf("") }
    var isChecking by remember { mutableStateOf(false) }
    var showHint by remember { mutableStateOf(false) }

    // For Sudoku: track grid state
    val sudokuGrid = remember { parseSudokuGrid(puzzle.question) }
    var sudokuState by remember { mutableStateOf(sudokuGrid.toMutableList()) }
    var selectedCell by remember { mutableIntStateOf(-1) }

    // Reset state when puzzle changes
    LaunchedEffect(puzzle.id) {
        userAnswer = ""
        result = null
        explanation = ""
        isChecking = false
        showHint = false
        sudokuState = parseSudokuGrid(puzzle.question).toMutableList()
        selectedCell = -1
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Surface)
            .statusBarsPadding()
    ) {
        // Top bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back",
                    tint = OnSurfaceVariant.copy(alpha = 0.7f))
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = puzzle.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = OnSurface,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Start
                )
                Text(
                    text = "Puzzle $puzzleNumber of $totalPuzzles",
                    style = MaterialTheme.typography.labelSmall,
                    color = OnSurfaceVariant.copy(alpha = 0.6f)
                )
            }

            IconButton(onClick = { showHint = !showHint }) {
                Text("\uD83D\uDCA1", fontSize = 20.sp)
            }
        }

        // Progress bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .background(SurfaceContainerHigh.copy(alpha = 0.3f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction = puzzleNumber.toFloat() / totalPuzzles)
                    .height(4.dp)
                    .background(SecondaryContainer.copy(alpha = 0.6f))
            )
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Puzzle type badge
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(SecondaryContainer.copy(alpha = 0.2f))
                    .padding(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Text(
                    text = puzzle.puzzleType.uppercase(),
                    style = MaterialTheme.typography.labelMedium,
                    color = SecondaryContainer,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Question
            Text(
                text = puzzle.question,
                style = MaterialTheme.typography.bodyLarge,
                color = OnSurfaceVariant.copy(alpha = 0.9f),
                textAlign = TextAlign.Start,
                lineHeight = 26.sp
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Interactive area based on puzzle type
            when (puzzle.puzzleType) {
                "sudoku" -> SudokuGrid(
                    grid = sudokuState,
                    selectedCell = selectedCell,
                    initialGrid = sudokuGrid,
                    onCellClick = { index -> selectedCell = index },
                    enabled = result == null
                )
                "math" -> MathInput(
                    value = userAnswer,
                    onValueChange = { userAnswer = it },
                    enabled = result == null
                )
                else -> TextInput(
                    value = userAnswer,
                    onValueChange = { userAnswer = it },
                    enabled = result == null
                )
            }

            // Number pad for Sudoku
            if (puzzle.puzzleType == "sudoku" && selectedCell >= 0 && result == null) {
                Spacer(modifier = Modifier.height(12.dp))
                NumberPad(
                    onNumber = { num ->
                        if (selectedCell in 0..80 && sudokuGrid[selectedCell] == 0) {
                            val list = sudokuState.toMutableList()
                            list[selectedCell] = num
                            sudokuState = list
                        }
                    }
                )
            }

            // Hint section
            AnimatedVisibility(visible = showHint && puzzle.hint.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(SecondaryContainer.copy(alpha = 0.1f))
                        .padding(16.dp)
                ) {
                    Text(
                        text = "\uD83D\uDCA1 ${puzzle.hint}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = OnSurfaceVariant.copy(alpha = 0.8f)
                    )
                }
            }

            // Result feedback
            AnimatedVisibility(visible = result != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            if (result == true) Color(0x1A00C853)
                            else Color(0x1AFF5252)
                        )
                        .padding(20.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        val isCorrect = result == true
                        Text(
                            text = if (isCorrect) "\u2705 Correct!" else "\u274C Not quite right",
                            style = MaterialTheme.typography.titleLarge,
                            color = if (isCorrect) Color(0xFF00C853) else Error,
                            fontWeight = FontWeight.Bold
                        )
                        if (explanation.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = explanation,
                                style = MaterialTheme.typography.bodyMedium,
                                color = OnSurfaceVariant.copy(alpha = 0.85f),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }

        // Bottom action buttons
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (result == true) {
                OutlinedButton(
                    onClick = onNext,
                    modifier = Modifier.weight(1f).height(52.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(
                        text = if (puzzleNumber < totalPuzzles) "Next \u2192" else "Done \u2192",
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Button(
                    onClick = {
                        result = null
                        explanation = ""
                        userAnswer = ""
                        sudokuState = parseSudokuGrid(puzzle.question).toMutableList()
                        selectedCell = -1
                    },
                    modifier = Modifier.weight(1f).height(52.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SecondaryContainer,
                        contentColor = Color(0xFF002021)
                    )
                ) {
                    Text("Play Again", fontWeight = FontWeight.Bold)
                }
            } else if (result == false) {
                OutlinedButton(
                    onClick = {
                        result = null
                        explanation = ""
                    },
                    modifier = Modifier.weight(1f).height(52.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text("Try Again", fontWeight = FontWeight.SemiBold)
                }
            } else {
                val hasInput = (puzzle.puzzleType == "sudoku" && sudokuState.any { it != 0 }) ||
                    (puzzle.puzzleType != "sudoku" && userAnswer.isNotBlank())
                Button(
                    onClick = {
                        scope.launch {
                            isChecking = true
                            val answer = when (puzzle.puzzleType) {
                                "sudoku" -> sudokuState.joinToString("")
                                else -> userAnswer.trim()
                            }
                            repository.validatePuzzle(puzzle.id, answer)
                                .onSuccess { response ->
                                    result = response.correct
                                    explanation = response.explanation
                                }
                                .onFailure {
                                    result = false
                                    explanation = "Could not validate. Try again."
                                }
                            isChecking = false
                        }
                    },
                    enabled = !isChecking && hasInput,
                    modifier = Modifier.weight(1f).height(52.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SecondaryContainer,
                        contentColor = Color(0xFF002021),
                        disabledContainerColor = SurfaceContainerHigh.copy(alpha = 0.3f)
                    )
                ) {
                    Text(
                        text = if (isChecking) "Checking..." else "Check Answer",
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun SudokuGrid(
    grid: List<Int>,
    selectedCell: Int,
    initialGrid: List<Int>,
    onCellClick: (Int) -> Unit,
    enabled: Boolean
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        for (row in 0 until 9) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                for (col in 0 until 9) {
                    val index = row * 9 + col
                    val value = grid.getOrElse(index) { 0 }
                    val isInitial = initialGrid.getOrElse(index) { 0 } != 0
                    val isSelected = selectedCell == index
                    val bgColor = if (isSelected) SecondaryContainer.copy(alpha = 0.15f) else Color.Transparent

                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .border(0.5.dp, OnSurfaceVariant.copy(alpha = 0.2f))
                            .then(
                                if ((col + 1) % 3 == 0 && col < 8) Modifier.border(
                                    0.dp, Color.Transparent,
                                    RoundedCornerShape(0.dp)
                                ).border(
                                    2.dp, OnSurfaceVariant.copy(alpha = 0.5f),
                                    RoundedCornerShape(0.dp)
                                ) else Modifier
                            )
                            .background(bgColor)
                            .clickable(enabled = enabled) { onCellClick(index) },
                        contentAlignment = Alignment.Center
                    ) {
                        if (value > 0) {
                            Text(
                                text = value.toString(),
                                style = MaterialTheme.typography.bodyLarge,
                                color = if (isInitial) OnSurface else Primary,
                                fontWeight = if (isInitial) FontWeight.Bold else FontWeight.Medium,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NumberPad(onNumber: (Int) -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        for (row in 0 until 3) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                for (col in 1..3) {
                    val num = row * 3 + col
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .padding(2.dp)
                            .clip(CircleShape)
                            .background(SecondaryContainer.copy(alpha = 0.2f))
                            .clickable { onNumber(num) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = num.toString(),
                            style = MaterialTheme.typography.titleMedium,
                            color = SecondaryContainer,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MathInput(value: String, onValueChange: (String) -> Unit, enabled: Boolean) {
    OutlinedTextField(
        value = value,
        onValueChange = { if (it.all { c -> c.isDigit() || c == '-' || c == '.' }) onValueChange(it) },
        label = { Text("Your answer") },
        enabled = enabled,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = SecondaryContainer,
            unfocusedBorderColor = SurfaceContainerHigh,
            focusedContainerColor = SurfaceContainerHigh.copy(alpha = 0.2f),
            unfocusedContainerColor = SurfaceContainerHigh.copy(alpha = 0.1f)
        )
    )
}

@Composable
private fun TextInput(value: String, onValueChange: (String) -> Unit, enabled: Boolean) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text("Type your answer") },
        enabled = enabled,
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = SecondaryContainer,
            unfocusedBorderColor = SurfaceContainerHigh,
            focusedContainerColor = SurfaceContainerHigh.copy(alpha = 0.2f),
            unfocusedContainerColor = SurfaceContainerHigh.copy(alpha = 0.1f)
        )
    )
}

private fun parseSudokuGrid(question: String): List<Int> {
    return question.take(81).map { it - '0' }.toList()
}
