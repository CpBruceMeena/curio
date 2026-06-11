package com.curio.app.viewmodel

import android.app.Application
import android.provider.Settings
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.curio.app.data.model.Profile
import com.curio.app.data.model.ProfileRequest
import com.curio.app.data.repository.ProfileRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ProfileUiState(
    val isLoading: Boolean = false,
    val profile: Profile = Profile(),
    val isSaved: Boolean = false,
    val error: String? = null,
    val name: String = "",
    val age: String = "",
    val gender: String = "",
    val likes: String = "",
    val dislikes: String = ""
)

class ProfileViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = ProfileRepository()
    private val deviceId: String = Settings.Secure.getString(
        application.contentResolver, Settings.Secure.ANDROID_ID
    ) ?: "unknown"

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        loadProfile()
    }

    fun loadProfile() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            repository.getProfile(deviceId).onSuccess { profile ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    profile = profile,
                    name = profile.name,
                    age = if (profile.age > 0) profile.age.toString() else "",
                    gender = profile.gender,
                    likes = profile.likes,
                    dislikes = profile.dislikes
                )
            }.onFailure {
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }
    }

    fun updateName(value: String) { _uiState.value = _uiState.value.copy(name = value) }
    fun updateAge(value: String) { _uiState.value = _uiState.value.copy(age = value.filter { it.isDigit() }.take(3)) }
    fun updateGender(value: String) { _uiState.value = _uiState.value.copy(gender = value) }
    fun updateLikes(value: String) { _uiState.value = _uiState.value.copy(likes = value) }
    fun updateDislikes(value: String) { _uiState.value = _uiState.value.copy(dislikes = value) }

    fun saveProfile() {
        val state = _uiState.value
        val ageVal = state.age.toIntOrNull() ?: 0

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, isSaved = false, error = null)
            repository.saveProfile(
                deviceId = deviceId,
                request = ProfileRequest(
                    name = state.name,
                    age = ageVal,
                    gender = state.gender,
                    likes = state.likes,
                    dislikes = state.dislikes
                )
            ).onSuccess { profile ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    profile = profile,
                    isSaved = true
                )
            }.onFailure { e ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to save profile"
                )
            }
        }
    }
}
