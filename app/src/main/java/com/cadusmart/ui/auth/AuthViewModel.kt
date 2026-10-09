package com.cadusmart.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cadusmart.data.dto.RegisterRequest
import com.cadusmart.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AuthUiState(val loading: Boolean = false, val error: String? = null)

private val EMAIL_RE = Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val repo: AuthRepository
) : ViewModel() {

    private val _state = MutableStateFlow(AuthUiState())
    val state: StateFlow<AuthUiState> = _state.asStateFlow()

    fun clearError() {
        if (_state.value.error != null) _state.update { it.copy(error = null) }
    }

    fun login(email: String, password: String) {
        if (email.isBlank() || password.isBlank()) {
            _state.value = AuthUiState(error = "Escribe tu correo y tu contraseña")
            return
        }
        launch { repo.login(email, password) }
    }

    fun register(tipo: String, nombre: String, email: String, password: String, razonSocial: String) {
        val error = when {
            tipo == "COMERCIO" && razonSocial.isBlank() -> "Escribe la razón social"
            nombre.isBlank() -> "Escribe tu nombre"
            !EMAIL_RE.matches(email.trim()) -> "El correo no es válido"
            password.length < 8 -> "La contraseña debe tener mínimo 8 caracteres"
            else -> null
        }
        if (error != null) {
            _state.value = AuthUiState(error = error)
            return
        }
        launch {
            repo.register(
                RegisterRequest(
                    tipo = tipo,
                    nombre = nombre.trim(),
                    email = email.trim(),
                    password = password,
                    razonSocial = if (tipo == "COMERCIO") razonSocial.trim() else null
                )
            )
        }
    }

    private fun launch(block: suspend () -> Result<*>) {
        viewModelScope.launch {
            _state.value = AuthUiState(loading = true)
            val result = block()
            // Si salió bien, la sesión cambia y la navegación sale sola de esta pantalla
            _state.value = AuthUiState(error = result.exceptionOrNull()?.message)
        }
    }
}
