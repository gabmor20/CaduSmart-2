package com.cadusmart.data.repository

import com.cadusmart.data.dto.ErrorResponse
import com.google.gson.Gson
import java.io.IOException
import retrofit2.HttpException

/** Ejecuta una llamada de red y traduce los errores a mensajes en español. */
suspend fun <T> apiCall(block: suspend () -> T): Result<T> = try {
    Result.success(block())
} catch (e: HttpException) {
    val body = runCatching { e.response()?.errorBody()?.string() }.getOrNull()
    val message = runCatching { Gson().fromJson(body, ErrorResponse::class.java)?.error }.getOrNull()
    Result.failure(Exception(message ?: "Ocurrió un error inesperado (${e.code()})"))
} catch (e: IOException) {
    Result.failure(Exception("No se pudo conectar con el servidor. Revisa tu conexión."))
}
