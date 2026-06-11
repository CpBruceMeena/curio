package com.curio.app.viewmodel

import android.app.Application
import android.os.Build
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.curio.app.BuildConfig
import com.curio.app.CurioApp
import com.curio.app.data.model.DeviceInfoRequest
import com.curio.app.data.model.Profile
import com.curio.app.data.model.ProfileRequest
import com.curio.app.data.repository.DeviceInfoRepository
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

    private val profileRepository = ProfileRepository()
    private val deviceInfoRepository = DeviceInfoRepository()

    /**
     * Persistent device UUID generated once and stored in SharedPreferences.
     * Used as the [device_id] for all API calls.
     */
    private val deviceId: String = CurioApp.instance.prefs.deviceUuid

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        loadProfile()
        submitDeviceInfo()
    }

    fun loadProfile() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            profileRepository.getProfile(deviceId).onSuccess { profile ->
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

    /**
     * Collect device metadata (OS version, app version, model, etc.) and
     * submit it to the backend. Runs once per device — subsequent launches
     * are no-ops (tracked via [PreferencesHelper.deviceInfoSubmitted]).
     */
    private fun submitDeviceInfo() {
        val prefs = CurioApp.instance.prefs
        if (prefs.deviceInfoSubmitted) return

        viewModelScope.launch {
            val infoRequest = DeviceInfoRequest(
                osVersion = "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})",
                appVersion = "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
                deviceModel = "${Build.MANUFACTURER} ${Build.MODEL}",
                manufacturer = Build.MANUFACTURER,
                screenSize = "${getApplication<Application>().resources.configuration.screenWidthDp}x${getApplication<Application>().resources.configuration.screenHeightDp}dp",
                language = java.util.Locale.getDefault().toLanguageTag(),
                timezone = java.util.TimeZone.getDefault().id
            )

            deviceInfoRepository.submitDeviceInfo(deviceId, infoRequest)
                .onSuccess { prefs.deviceInfoSubmitted = true }
                // Silently ignore failures — retry on next ProfileScreen launch
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
            profileRepository.saveProfile(
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

    /**
     * Exposed for testing / manual retry: resets the flag so device info
     * will be submitted again on next [ProfileScreen] visit.
     */
    fun resetDeviceInfoFlag() {
        CurioApp.instance.prefs.deviceInfoSubmitted = false
    }
}
