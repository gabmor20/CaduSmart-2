package com.cadusmart.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cadusmart.data.repository.AuthRepository
import com.cadusmart.data.session.SessionStore
import com.cadusmart.domain.Role
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface SessionState {
    data object Loading : SessionState
    data object LoggedOut : SessionState
    data class LoggedIn(val role: Role, val mustChangePassword: Boolean) : SessionState
}

@HiltViewModel
class MainViewModel @Inject constructor(
    sessionStore: SessionStore,
    private val auth: AuthRepository
) : ViewModel() {

    val session: StateFlow<SessionState> =
        combine<Role?, Boolean, SessionState>(sessionStore.role, sessionStore.mustChangePassword) { role, must ->
            if (role == null) SessionState.LoggedOut else SessionState.LoggedIn(role, must)
        }.stateIn(viewModelScope, SharingStarted.Eagerly, SessionState.Loading)

    fun logout() {
        viewModelScope.launch { auth.logout() }
    }
}
