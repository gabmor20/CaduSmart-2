package com.cadusmart.data.api

import com.cadusmart.data.dto.AuthResponse
import com.cadusmart.data.dto.LoginRequest
import com.cadusmart.data.dto.MeResponse
import com.cadusmart.data.dto.RegisterRequest
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface AuthApi {
    @POST("auth/register")
    suspend fun register(@Body body: RegisterRequest): AuthResponse

    @POST("auth/login")
    suspend fun login(@Body body: LoginRequest): AuthResponse

    @GET("auth/me")
    suspend fun me(): MeResponse
}
