package com.curio.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.curio.app.CurioApp
import com.curio.app.data.model.Category
import com.curio.app.data.model.L1Group
import com.curio.app.data.repository.ContentRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// ── Fallback L1 data for instant display ──
// Shown immediately while the API call is in flight.
// The 4 L1 groups and their subcategories are essentially fixed.
private fun fallbackL1Groups(): List<L1Group> = listOf(
    L1Group(
        name = "Facts", icon = "menu_book", colorHex = "#00f4fe",
        categories = listOf(
            Category(1, "Science", "biotech", "#00f4fe", 1, l1Category = "Facts"),
            Category(2, "Space", "rocket_launch", "#a8cec8", 2, l1Category = "Facts"),
            Category(3, "History", "history_edu", "#e9c400", 3, l1Category = "Facts"),
            Category(4, "Biology", "eco", "#63f7ff", 4, l1Category = "Facts"),
            Category(5, "Psychology", "psychology", "#c3eae4", 5, l1Category = "Facts"),
            Category(6, "Philosophy", "balance_scale", "#ffe16d", 6, l1Category = "Facts"),
            Category(7, "Physics", "atom", "#00dce5", 7, l1Category = "Facts"),
            Category(8, "Startups", "lightbulb", "#e9c400", 8, l1Category = "Facts"),
            Category(9, "AI", "smart_toy", "#00f4fe", 9, l1Category = "Facts"),
            Category(10, "Economics", "account_balance", "#63f7ff", 10, l1Category = "Facts"),
            Category(11, "Nature", "forest", "#a8cec8", 11, l1Category = "Facts"),
            Category(12, "Technology", "computer", "#00dce5", 12, l1Category = "Facts"),
            Category(13, "Movies", "movie", "#fb923c", 14, l1Category = "Facts"),
            Category(14, "Neuroscience", "microscope", "#a78bfa", 15, l1Category = "Facts"),
            Category(15, "Literature", "menu_book", "#fbbf24", 16, l1Category = "Facts"),
            Category(16, "Geography", "public", "#34d399", 17, l1Category = "Facts"),
            Category(17, "Music", "music_note", "#f472b6", 18, l1Category = "Facts"),
            Category(18, "Sports", "sports_soccer", "#fb923c", 19, l1Category = "Facts"),
            Category(19, "Food", "ramen_dining", "#f59e0b", 20, l1Category = "Facts"),
        )
    ),
    L1Group(
        name = "Poems", icon = "auto_stories", colorHex = "#f472b6",
        categories = listOf(
            Category(20, "English Poems", "auto_stories", "#f472b6", 13, l1Category = "Poems"),
            Category(21, "Shayari", "edit_note", "#d946ef", 21, l1Category = "Poems"),
            Category(22, "Hindi Poems", "auto_stories", "#f472b6", 24, l1Category = "Poems"),
            Category(23, "Classics", "menu_book", "#fbbf24", 32, l1Category = "Poems"),
            Category(24, "Modern", "brush", "#a78bfa", 33, l1Category = "Poems"),
        )
    ),
    L1Group(
        name = "Short Stories", icon = "article", colorHex = "#06b6d4",
        categories = listOf(
            Category(25, "Short Stories", "article", "#06b6d4", 23, l1Category = "Short Stories"),
            Category(26, "Classic Fiction", "menu_book", "#06b6d4", 29, l1Category = "Short Stories"),
            Category(27, "Micro Stories", "auto_stories", "#34d399", 30, l1Category = "Short Stories"),
            Category(28, "Serialized Stories", "library_books", "#6366f1", 31, l1Category = "Short Stories"),
        )
    ),
    L1Group(
        name = "Puzzles", icon = "extension", colorHex = "#f97316",
        categories = listOf(
            Category(29, "Mixed Puzzles", "extension", "#f97316", 22, l1Category = "Puzzles"),
            Category(30, "Sudoku", "grid_on", "#22d3ee", 25, l1Category = "Puzzles"),
            Category(31, "Math Puzzles", "calculate", "#fb923c", 26, l1Category = "Puzzles"),
            Category(32, "Logic Puzzles", "psychology", "#a78bfa", 27, l1Category = "Puzzles"),
            Category(33, "Word Puzzles", "abc", "#fbbf24", 28, l1Category = "Puzzles"),
        )
    ),
    L1Group(
        name = "Novels", icon = "auto_stories", colorHex = "#8b5cf6",
        novelCount = 0,
        categories = listOf(
            Category(34, "Classic Novels", "menu_book", "#8b5cf6", 34, l1Category = "Novels"),
            Category(35, "Fiction", "auto_stories", "#a78bfa", 35, l1Category = "Novels"),
        )
    ),
)

data class OnboardingUiState(
    val l1Groups: List<L1Group> = fallbackL1Groups(),
    val selectedInterest: String? = null,
    val canProceed: Boolean = false,
    val isRefreshing: Boolean = true
)

class OnboardingViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = (application as CurioApp).prefs
    private val repository = ContentRepository()

    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    init {
        // Don't block the UI - show immediately, populate when data arrives
        loadL1Groups()
    }

    private fun loadL1Groups() {
        viewModelScope.launch {
            repository.getL1Categories()
                .onSuccess { response ->
                    _uiState.value = _uiState.value.copy(
                        l1Groups = response.groups,
                        isRefreshing = false
                    )
                }
                .onFailure {
                    _uiState.value = _uiState.value.copy(isRefreshing = false)
                }
        }
    }

    fun toggleInterest(l1Name: String) {
        val current = _uiState.value.selectedInterest
        val newSelection = if (current == l1Name) null else l1Name
        _uiState.value = _uiState.value.copy(
            selectedInterest = newSelection,
            canProceed = newSelection != null
        )
    }

    /** Returns the list of subcategories for the currently selected L1 interest. */
    fun getSelectedL1Subcategories(): List<Category> {
        val selectedL1Name = _uiState.value.selectedInterest ?: return emptyList()
        val group = _uiState.value.l1Groups.find { it.name == selectedL1Name }
        return group?.categories ?: emptyList()
    }

    /** Mark onboarding as complete and save the L1 selection name. */
    fun saveInterests() {
        prefs.hasCompletedOnboarding = true
    }

    /** Save the final set of selected subcategory names (called from L2SelectionScreen). */
    fun saveFinalSelection(categoryNames: Set<String>) {
        prefs.selectedCategories = categoryNames
    }
}
