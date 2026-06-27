package com.example.herretols.ui.auth

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

@Composable
fun SplashScreen(viewModel: AuthViewModel, onNavigateToLogin: () -> Unit, onNavigateToHome: (String) -> Unit) {
    LaunchedEffect(Unit) {
        viewModel.checkUserSession { rol ->
            if (rol != null) {
                onNavigateToHome(rol)
            } else {
                onNavigateToLogin()
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator() // Puedes cambiarlo por el logo de tu ferretería luego
    }
}