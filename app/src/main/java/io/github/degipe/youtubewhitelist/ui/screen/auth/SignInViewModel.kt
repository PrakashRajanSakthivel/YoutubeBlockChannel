package io.github.degipe.youtubewhitelist.ui.screen.auth

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.degipe.youtubewhitelist.core.data.repository.AuthRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import javax.inject.Inject

sealed interface SignInUiState {
    data object Idle : SignInUiState
    data object Loading : SignInUiState
    data object Success : SignInUiState
    data class Error(val message: String) : SignInUiState
}

@HiltViewModel
class SignInViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<SignInUiState>(SignInUiState.Idle)
    val uiState: StateFlow<SignInUiState> = _uiState.asStateFlow()

    private val _secondsRemaining = MutableStateFlow<Int?>(null)
    val secondsRemaining: StateFlow<Int?> = _secondsRemaining.asStateFlow()

    private var signInJob: Job? = null

    companion object {
        private const val TIMEOUT_SECONDS = 90
    }

    fun signIn(activityContext: Context) {
        signInJob?.cancel()
        signInJob = viewModelScope.launch {
            _uiState.value = SignInUiState.Loading
            _secondsRemaining.value = TIMEOUT_SECONDS

            val countdownJob = launch {
                for (i in TIMEOUT_SECONDS downTo 0) {
                    _secondsRemaining.value = i
                    delay(1000L)
                }
            }

            try {
                withTimeout(TIMEOUT_SECONDS * 1000L) {
                    authRepository.signIn(activityContext)
                }
                countdownJob.cancel()
                _secondsRemaining.value = null
                _uiState.value = SignInUiState.Success
            } catch (e: TimeoutCancellationException) {
                countdownJob.cancel()
                _secondsRemaining.value = null
                _uiState.value = SignInUiState.Error("Sign in timed out. Please try again.")
            } catch (e: Exception) {
                countdownJob.cancel()
                _secondsRemaining.value = null
                _uiState.value = SignInUiState.Error(e.message ?: "Unknown error")
            }
        }
    }

    fun cancelSignIn() {
        signInJob?.cancel()
        _secondsRemaining.value = null
        _uiState.value = SignInUiState.Idle
    }
}
