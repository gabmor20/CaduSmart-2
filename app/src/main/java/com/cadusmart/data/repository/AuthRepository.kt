package com.cadusmart.data.repository

import com.cadusmart.data.api.AuthApi
import com.cadusmart.data.dto.ChangePasswordRequest
import com.cadusmart.data.dto.LoginRequest
import com.cadusmart.data.dto.RegisterRequest
import com.cadusmart.data.dto.SettingsRequest
import com.cadusmart.data.dto.UpdateProfileRequest
import com.cadusmart.data.dto.UserDto
import com.cadusmart.data.session.SessionStore
import com.cadusmart.domain.Role
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

@Singleton
class AuthRepository @Inject constructor(
    private val api: AuthApi,
    private val session: SessionStore
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _user = MutableStateFlow<UserDto?>(null)
    /** Usuario con sesión iniciada (se mantiene al día al editar perfil o ajustes) */
    val user: StateFlow<UserDto?> = _user.asStateFlow()

    init {
        scope.launch {
            session.token.map { it != null }.distinctUntilChanged().collect { hasSession ->
                if (!hasSession) _user.value = null
            }
        }
    }

    suspend fun login(email: String, password: String): Result<UserDto> = apiCall {
        val r = api.login(LoginRequest(email.trim(), password))
        session.save(r.token, Role.valueOf(r.user.role), r.user.mustChangePassword)
        _user.value = r.user
        r.user
    }

    suspend fun register(request: RegisterRequest): Result<UserDto> = apiCall {
        val r = api.register(request)
        session.save(r.token, Role.valueOf(r.user.role), r.user.mustChangePassword)
        _user.value = r.user
        r.user
    }

    suspend fun me(): Result<UserDto> = apiCall {
        val u = api.me().user
        _user.value = u
        u
    }

    suspend fun updateProfile(nombre: String, email: String): Result<UserDto> = apiCall {
        val u = api.updateProfile(UpdateProfileRequest(nombre.trim(), email.trim())).user
        _user.value = u
        u
    }

    suspend fun updateSettings(alertDays: Int, notificationsEnabled: Boolean): Result<UserDto> = apiCall {
        val u = api.updateSettings(SettingsRequest(alertDays, notificationsEnabled)).user
        _user.value = u
        u
    }

    suspend fun changePassword(current: String, new: String): Result<UserDto> = apiCall {
        val u = api.changePassword(ChangePasswordRequest(current, new)).user
        session.setMustChangePassword(false)
        _user.value = u
        u
    }

    suspend fun logout() = session.clear()
}
