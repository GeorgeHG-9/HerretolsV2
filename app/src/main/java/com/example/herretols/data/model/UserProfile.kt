package com.example.herretols.data.model

data class UserProfile(
    val uid: String = "",
    val nombre: String = "",
    val email: String = "",
    val telefono: String = "",
    val direccion: String = "",
    val rol: String = "cliente"
) {
    fun toMap(): Map<String, Any> = mapOf(
        "uid" to uid,
        "nombre" to nombre,
        "email" to email,
        "telefono" to telefono,
        "direccion" to direccion,
        "rol" to rol
    )
}

sealed interface AuthState {
    object Idle : AuthState
    object Loading : AuthState
    data class Success(val rol: String) : AuthState
    data class Error(val message: String) : AuthState
    object EmailVerificationSent : AuthState
    object ResetPasswordEmailSent : AuthState
    object NewGoogleUser : AuthState // Avisa que es un usuario de Google sin datos
}