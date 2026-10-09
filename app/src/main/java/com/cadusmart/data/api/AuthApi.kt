package com.cadusmart.data.api

import com.cadusmart.data.dto.AuthResponse
import com.cadusmart.data.dto.ChangePasswordRequest
import com.cadusmart.data.dto.LoginRequest
import com.cadusmart.data.dto.MeResponse
import com.cadusmart.data.dto.RegisterRequest
import com.cadusmart.data.dto.SettingsRequest
import com.cadusmart.data.dto.UpdateProfileRequest
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT

interface AuthApi {
    @POST("auth/register")
    suspend fun register(@Body body: RegisterRequest): AuthResponse

    @POST("auth/login")
    suspend fun login(@Body body: LoginRequest): AuthResponse

    @GET("auth/me")
    suspend fun me(): MeResponse

    @PUT("auth/me")
    suspend fun updateProfile(@Body body: UpdateProfileRequest): MeResponse

    @PUT("auth/me/settings")
    suspend fun updateSettings(@Body body: SettingsRequest): MeResponse

    @POST("auth/change-password")
    suspend fun changePassword(@Body body: ChangePasswordRequest): MeResponse
}
