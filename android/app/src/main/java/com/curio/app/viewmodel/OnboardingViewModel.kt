package com.curio.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.curio.app.CurioApp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class OnboardingUiState(
    val selectedInterests: Set<String> = emptySet(),
    val canProceed: Boolean = false
)

class OnboardingViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = (application as CurioApp).prefs

    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    private val minSelection = 3

    val interestOptions = listOf(
        "Science" to "biotech",
        "Space" to "rocket_launch",
        "History" to "history_edu",
        "Biology" to "psychology",
        "Startups" to "lightbulb",
        "Philosophy" to "psychology",
        "Physics" to "atom",
        "Economics" to "account_balance",
        "Psychology" to "psychology",
        "AI" to "neurology",
        "Nature" to "forest",
        "Technology" to "memory"
    )

    fun toggleInterest(interest: String) {
        val current = _uiState.value.selectedInterests.toMutableSet()
        if (current.contains(interest)) {
            current.remove(interest)
        } else {
            current.add(interest)
        }
        _uiState.value = OnboardingUiState(
            selectedInterests = current,
            canProceed = current.size >= minSelection
        )
    }

    fun saveInterests() {
        prefs.selectedCategories = _uiState.value.selectedInterests
        prefs.hasCompletedOnboarding = true
    }
}
