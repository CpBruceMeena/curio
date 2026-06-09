package com.curio.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.curio.app.CurioApp
import com.curio.app.data.model.L1Group
import com.curio.app.data.repository.ContentRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class OnboardingUiState(
    val l1Groups: List<L1Group> = emptyList(),
    val isLoading: Boolean = true,
    val selectedInterest: String? = null,
    val canProceed: Boolean = false
)

class OnboardingViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = (application as CurioApp).prefs
    private val repository = ContentRepository()

    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    init {
        loadL1Groups()
    }

    private fun loadL1Groups() {
        viewModelScope.launch {
            repository.getL1Categories().onSuccess { response ->
                _uiState.value = _uiState.value.copy(
                    l1Groups = response.groups,
                    isLoading = false
                )
            }.onFailure {
                _uiState.value = _uiState.value.copy(isLoading = false)
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

    fun saveInterests() {
        val selectedL1Name = _uiState.value.selectedInterest
        // Expand selected L1 group into its subcategory names for the feed filter
        val expandedCategories = mutableSetOf<String>()
        if (selectedL1Name != null) {
            val l1Group = _uiState.value.l1Groups.find { it.name == selectedL1Name }
            if (l1Group != null) {
                expandedCategories.addAll(l1Group.categories.map { it.name })
            }
        }
        prefs.selectedCategories = expandedCategories
        prefs.hasCompletedOnboarding = true
    }
}
