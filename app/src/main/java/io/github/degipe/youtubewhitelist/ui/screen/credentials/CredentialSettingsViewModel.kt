package io.github.degipe.youtubewhitelist.ui.screen.credentials

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.degipe.youtubewhitelist.core.auth.credential.CredentialStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

data class CredentialSettingsUiState(
    val youtubeApiKey: String = "",
    val googleClientId: String = "",
    val googleClientSecret: String = "",
    val isSaved: Boolean = false,
    val hasExistingCredentials: Boolean = false
)

@HiltViewModel
class CredentialSettingsViewModel @Inject constructor(
    private val credentialStore: CredentialStore
) : ViewModel() {

    private val _uiState = MutableStateFlow(CredentialSettingsUiState())
    val uiState: StateFlow<CredentialSettingsUiState> = _uiState.asStateFlow()

    init {
        val existing = credentialStore.hasCredentials()
        _uiState.value = CredentialSettingsUiState(
            youtubeApiKey = credentialStore.getYouTubeApiKey(),
            googleClientId = credentialStore.getGoogleClientId(),
            googleClientSecret = credentialStore.getGoogleClientSecret(),
            hasExistingCredentials = existing
        )
    }

    fun onYouTubeApiKeyChanged(value: String) {
        _uiState.value = _uiState.value.copy(youtubeApiKey = value.trim(), isSaved = false)
    }

    fun onGoogleClientIdChanged(value: String) {
        _uiState.value = _uiState.value.copy(googleClientId = value.trim(), isSaved = false)
    }

    fun onGoogleClientSecretChanged(value: String) {
        _uiState.value = _uiState.value.copy(googleClientSecret = value.trim(), isSaved = false)
    }

    fun save() {
        val state = _uiState.value
        credentialStore.saveCredentials(
            youtubeApiKey = state.youtubeApiKey,
            googleClientId = state.googleClientId,
            googleClientSecret = state.googleClientSecret
        )
        _uiState.value = state.copy(isSaved = true, hasExistingCredentials = true)
    }

    fun canSave(): Boolean {
        val state = _uiState.value
        return state.googleClientId.isNotEmpty() && state.googleClientSecret.isNotEmpty()
    }
}
