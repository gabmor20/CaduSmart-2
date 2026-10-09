package com.cadusmart.data.repository

import com.cadusmart.data.api.UserApi
import com.cadusmart.data.dto.CreateUserRequest
import com.cadusmart.data.dto.ManagedUserDto
import com.cadusmart.data.dto.ResetPasswordRequest
import com.cadusmart.data.dto.UpdateUserRequest
import com.cadusmart.data.session.SessionStore
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

/** Usuarios del comercio (solo Admin). Misma idea que ProductRepository: caché compartida. */
@Singleton
class UserRepository @Inject constructor(
    private val api: UserApi,
    session: SessionStore
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _users = MutableStateFlow<List<ManagedUserDto>>(emptyList())
    val users: StateFlow<List<ManagedUserDto>> = _users.asStateFlow()

    private val _loaded = MutableStateFlow(false)
    val loaded: StateFlow<Boolean> = _loaded.asStateFlow()

    init {
        scope.launch {
            session.token.map { it != null }.distinctUntilChanged().collect { hasSession ->
                if (!hasSession) {
                    _users.value = emptyList()
                    _loaded.value = false
                }
            }
        }
    }

    suspend fun refresh(): Result<Unit> = apiCall {
        _users.value = api.list().items
        _loaded.value = true
        Unit
    }

    suspend fun create(
        nombre: String, email: String, password: String, role: String, active: Boolean
    ): Result<ManagedUserDto> =
        apiCall { api.create(CreateUserRequest(nombre.trim(), email.trim(), password, role, active)).item }
            .also { if (it.isSuccess) refresh() }

    /** Si [newPassword] no está vacía, además restablece la contraseña (queda como temporal). */
    suspend fun update(
        id: Int, nombre: String, email: String, role: String, active: Boolean, newPassword: String?
    ): Result<ManagedUserDto> = apiCall {
        val item = api.update(id, UpdateUserRequest(nombre.trim(), email.trim(), role, active)).item
        if (!newPassword.isNullOrBlank()) api.resetPassword(id, ResetPasswordRequest(newPassword))
        item
    }.also { if (it.isSuccess) refresh() }

    /** Devuelve cuántos productos se borraron junto con la cuenta. */
    suspend fun delete(id: Int): Result<Int> =
        apiCall { api.delete(id).deletedProducts }.also { if (it.isSuccess) refresh() }
}
