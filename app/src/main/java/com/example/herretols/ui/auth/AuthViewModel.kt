package com.example.herretols.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.example.herretols.data.model.AuthState
import com.example.herretols.data.model.UserProfile
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class AuthViewModel : ViewModel() {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()

    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState

    fun loginWithGoogle(idToken: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            try {
                val credential = GoogleAuthProvider.getCredential(idToken, null)
                val authResult = auth.signInWithCredential(credential).await()
                val user = authResult.user ?: throw Exception("Error con Google")

                // Verificar si ya existe en Firestore
                val document = db.collection("usuarios").document(user.uid).get().await()

                if (document.exists()) {
                    val rol = document.getString("rol") ?: "cliente"
                    _authState.value = AuthState.Success(rol)
                } else {
                    // ◄ CAMBIO: Si no existe, NO lo creamos aún. Pasamos al estado NewGoogleUser
                    _authState.value = AuthState.NewGoogleUser
                }
            } catch (e: Exception) {
                _authState.value = AuthState.Error(e.localizedMessage ?: "Error")
            }
        }
    }

    // NUEVA FUNCIÓN: Para guardar los datos cuando termine de rellenar el formulario
    fun completeGoogleUserProfile(telefono: String, direccion: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            try {
                val user = auth.currentUser ?: throw Exception("No hay sesión activa")

                val nuevoPerfil = UserProfile(
                    uid = user.uid,
                    nombre = user.displayName ?: "Usuario Google",
                    email = user.email ?: "",
                    telefono = telefono,
                    direccion = direccion,
                    rol = "cliente"
                )

                db.collection("usuarios").document(user.uid).set(nuevoPerfil.toMap()).await()
                _authState.value = AuthState.Success("cliente")
            } catch (e: Exception) {
                _authState.value = AuthState.Error(e.localizedMessage ?: "Error al guardar perfil")
            }
        }
    }

    // 1. REGISTRO CON EMAIL, PASSWORD Y DATOS ADICIONALES
    fun registerWithEmail(userProfile: UserProfile, password: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            try {
                // Crear usuario en Firebase Auth
                val authResult = auth.createUserWithEmailAndPassword(userProfile.email, password).await()
                val uid = authResult.user?.uid ?: throw Exception("No se pudo obtener el UID")

                // Enviar correo de verificación
                authResult.user?.sendEmailVerification()?.await()

                // Guardar datos adicionales en Firestore usando el UID como ID del documento
                val finalProfile = userProfile.copy(uid = uid)
                db.collection("usuarios").document(uid).set(finalProfile.toMap()).await()

                _authState.value = AuthState.EmailVerificationSent
                auth.signOut() // Cerramos sesión hasta que verifique su correo
            } catch (e: Exception) {
                _authState.value = AuthState.Error(e.localizedMessage ?: "Error en el registro")
            }
        }
    }

    // 2. LOGIN + VERIFICACIÓN DE EMAIL + OBTENER ROL
    fun loginWithEmail(email: String, password: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            try {
                val authResult = auth.signInWithEmailAndPassword(email, password).await()
                val user = authResult.user

                if (user != null && user.isEmailVerified) {
                    // Si está verificado, buscamos su rol en Firestore
                    val document = db.collection("usuarios").document(user.uid).get().await()
                    val rol = document.getString("rol") ?: "cliente"

                    _authState.value = AuthState.Success(rol)
                } else {
                    _authState.value = AuthState.Error("Por favor, verifica tu correo electrónico antes de ingresar.")
                    auth.signOut()
                }
            } catch (e: Exception) {
                _authState.value = AuthState.Error(e.localizedMessage ?: "Credenciales incorrectas")
            }
        }
    }

    // 3. RECUPERAR CONTRASEÑA
    fun sendPasswordReset(email: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            try {
                auth.sendPasswordResetEmail(email).await()
                _authState.value = AuthState.ResetPasswordEmailSent
            } catch (e: Exception) {
                _authState.value = AuthState.Error(e.localizedMessage ?: "Error al enviar correo")
            }
        }
    }

    // 1. Verifica si hay usuario guardado en el teléfono y obtiene su rol
    fun checkUserSession(onResult: (String?) -> Unit) {
        val currentUser = auth.currentUser
        if (currentUser != null && currentUser.isEmailVerified) {
            viewModelScope.launch {
                try {
                    val document = db.collection("usuarios").document(currentUser.uid).get().await()
                    val rol = document.getString("rol") ?: "cliente"
                    onResult(rol)
                } catch (e: Exception) {
                    onResult(null) // Si hay error de red, lo manda al login por seguridad
                }
            }
        } else {
            onResult(null) // No hay sesión activa o no está verificado
        }
    }

    // 2. Función para cerrar sesión de Firebase de forma definitiva
    fun logout() {
        auth.signOut()
        _authState.value = AuthState.Idle
    }

    fun resetState() {
        _authState.value = AuthState.Idle
    }
}