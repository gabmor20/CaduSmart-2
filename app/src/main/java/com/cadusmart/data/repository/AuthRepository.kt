package com.cadusmart.data.repository

import com.cadusmart.data.api.AuthApi
import com.cadusmart.data.dto.ErrorResponse
import com.cadusmart.data.dto.LoginRequest
import com.cadusmart.data.dto.RegisterRequest
import com.cadusmart.data.dto.UserDto
import com.cadusmart.data.session.SessionStore
import com.cadusmart.domain.Role
import com.google.gson.Gson
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import retrofit2.HttpException

@Singleton
class AuthRepository @Inject constructor(
    private val api: AuthApi,
    private val session: SessionStore
) {
    suspend fun login(email: String, password: String): Result<UserDto> = call {
        val r = api.login(LoginRequest(email.trim(), password))
        session.save(r.token, Role.valueOf(r.user.role))
        r.user
    }

    suspend fun register(request: RegisterRequest): Result<UserDto> = call {
        val r = api.register(request)
        session.save(r.token, Role.valueOf(r.user.role))
        r.user
    }

    suspend fun me(): Result<UserDto> = call { api.me().user }

    suspend fun logout() = session.clear()

    private suspend fun <T> call(block: suspend () -> T): Result<T> = try {
        Result.success(block())
    } catch (e: HttpException) {
        Result.failure(Exception(parseError(e)))
    } catch (e: IOException) {
        android.util.Log.e("AuthRepository", "Error de red", e)
        Result.failure(Exception("No se pudo conectar con el servidor. Revisa tu conexión."))
    }

    private fun parseError(e: HttpException): String {
        val body = runCatching { e.response()?.errorBody()?.string() }.getOrNull()
        val message = runCatching { Gson().fromJson(body, ErrorResponse::class.java)?.error }.getOrNull()
        return message ?: "Ocurrió un error inesperado (${e.code()})"
    }
}
