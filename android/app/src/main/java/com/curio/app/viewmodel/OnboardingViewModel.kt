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
