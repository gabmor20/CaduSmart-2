package com.cadusmart.data.dto

data class RegisterRequest(
    val tipo: String,              // "PERSONAL" | "COMERCIO"
    val nombre: String,
    val email: String,
    val password: String,
    val razonSocial: String? = null
)

data class LoginRequest(val email: String, val password: String)

data class UserDto(
    val id: Int,
    val nombre: String,
    val email: String,
    val role: String,              // PERSONAL | COMERCIO_ADMIN | COMERCIO_EMPLEADO
    val commerceId: Int?,
    val alertDays: Int,
    val notificationsEnabled: Boolean,
    val mustChangePassword: Boolean
)

data class AuthResponse(val token: String, val user: UserDto)

data class MeResponse(val user: UserDto)

data class ErrorResponse(val error: String?)
