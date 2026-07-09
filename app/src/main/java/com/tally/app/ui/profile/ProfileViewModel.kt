package com.tally.app.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tally.app.data.auth.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.jan.supabase.auth.status.SessionStatus
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface AuthState {
    data object Loading : AuthState
    data object SignedOut : AuthState
    data class SignedIn(val userId: String) : AuthState
}

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _authState = MutableStateFlow<AuthState>(AuthState.Loading)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    private val _error = MutableSharedFlow<String>()
    val error: SharedFlow<String> = _error.asSharedFlow()

    init {
        viewModelScope.launch {
            authRepository.sessionStatus.collect { status ->
                _authState.value = when (status) {
                    is SessionStatus.Initializing -> AuthState.Loading
                    is SessionStatus.NotAuthenticated -> AuthState.SignedOut
                    is SessionStatus.Authenticated -> AuthState.SignedIn(
                        userId = status.session.user?.id ?: return@collect
                    )
                    is SessionStatus.RefreshFailure -> AuthState.SignedOut
                }
            }
        }
    }

    fun signInWithGoogle(idToken: String) {
        viewModelScope.launch {
            try {
                authRepository.signInWithGoogle(idToken)
            } catch (e: Exception) {
                _authState.value = AuthState.SignedOut
                _error.emit(e.message ?: "Sign in failed")
            }
        }
    }

    fun signOut() {
        viewModelScope.launch {
            authRepository.signOut()
        }
    }
}
