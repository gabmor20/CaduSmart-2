package com.cadusmart.data.api

import com.cadusmart.data.session.SessionStore
import javax.inject.Inject
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response

/** Agrega el JWT a cada petición y cierra la sesión si el servidor responde 401 con un token guardado. */
class AuthInterceptor @Inject constructor(
    private val session: SessionStore
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val token = runBlocking { session.token.first() }
        val request = if (token != null) {
            chain.request().newBuilder()
                .header("Authorization", "Bearer $token")
                .build()
        } else chain.request()

        val response = chain.proceed(request)
        if (response.code == 401 && token != null) {
            runBlocking { session.clear() }
        }
        return response
    }
}
