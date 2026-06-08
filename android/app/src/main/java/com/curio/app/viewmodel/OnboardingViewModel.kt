package com.curio.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.curio.app.CurioApp
import com.curio.app.data.model.Category
import com.curio.app.data.repository.ContentRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class OnboardingUiState(
    val categories: List<Category> = emptyList(),
    val isLoading: Boolean = true,
    val selectedInterests: Set<String> = emptySet(),
    val canProceed: Boolean = false
)

class OnboardingViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = (application as CurioApp).prefs
    private val repository = ContentRepository()

    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    private val minSelection = 3

    init {
        loadCategories()
    }

    private fun loadCategories() {
        viewModelScope.launch {
            repository.getCategories().onSuccess { response ->
                _uiState.value = _uiState.value.copy(
                    categories = response.categories,
                    isLoading = false
                )
            }.onFailure {
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }
    }

    fun toggleInterest(interest: String) {
        val current = _uiState.value.selectedInterests.toMutableSet()
        if (current.contains(interest)) {
            current.remove(interest)
        } else {
            current.add(interest)
        }
        _uiState.value = _uiState.value.copy(
            selectedInterests = current,
            canProceed = current.size >= minSelection
        )
    }

    fun saveInterests() {
        prefs.selectedCategories = _uiState.value.selectedInterests
        prefs.hasCompletedOnboarding = true
    }
}
