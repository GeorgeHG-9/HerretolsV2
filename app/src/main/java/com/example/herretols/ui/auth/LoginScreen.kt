package com.example.herretols.ui.auth


import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.herretols.data.model.AuthState
import com.example.herretols.ui.navigation.Screen
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException

@Composable
fun LoginScreen(
    viewModel: AuthViewModel,
    onNavigateToRegister: () -> Unit,
    onNavigateToCompleteProfile: () -> Unit,
    onLoginSuccess: (String) -> Unit,
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var showResetDialog by remember { mutableStateOf(false) }
    var resetEmail by remember { mutableStateOf("") }

    val authState by viewModel.authState.collectAsState()
    val context = LocalContext.current

    // 1. Configurar las opciones de Google Sign-In
    val gso = remember {
        GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken("684268195349-uobbpqk0id6a656s6f3fljtvdjf037co.apps.googleusercontent.com")
            .requestEmail()
            .build()
    }
    val googleSignInClient = remember { GoogleSignIn.getClient(context, gso) }

    // 2. Lanzador para capturar el resultado de la ventana de Google
    val googleLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            val idToken = account?.idToken
            if (idToken != null) {
                viewModel.loginWithGoogle(idToken) // Enviamos el token al ViewModel
            } else {
                Toast.makeText(context, "No se pudo obtener el token de Google", Toast.LENGTH_SHORT)
                    .show()
            }
        }catch (e: ApiException) {
            Log.e(
                "GOOGLE_LOGIN",
                "statusCode=${e.statusCode}",
                e
            )
        }
//        } catch (e: ApiException) {
//            Toast.makeText(context, "Error: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
//        }
    }

    // Escuchar cambios de estado
    LaunchedEffect(authState) {
        when (authState) {
            is AuthState.NewGoogleUser -> {
               // navController.navigate(Screen.CompleteProfile.route) // ◄ Mandar al formulario intermedio
                onNavigateToCompleteProfile()
                viewModel.resetState()
            }
            is AuthState.Success -> {
                onLoginSuccess((authState as AuthState.Success).rol)
                viewModel.resetState()
            }
            is AuthState.Error -> {
                Toast.makeText(context, (authState as AuthState.Error).message, Toast.LENGTH_LONG).show()
                viewModel.resetState()
            }
            is AuthState.ResetPasswordEmailSent -> {
                Toast.makeText(context, "Correo de recuperación enviado", Toast.LENGTH_SHORT).show()
                showResetDialog = false
                viewModel.resetState()
            }
            else -> {}
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "Ferretería - Iniciar Sesión", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Correo electrónico") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Contraseña") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))

        if (authState is AuthState.Loading) {
            CircularProgressIndicator()
        } else {
            Button(
                onClick = { viewModel.loginWithEmail(email, password) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Ingresar")
            }

            // Separador el Login tradicional de Google
            Text(text = "ó", style = MaterialTheme.typography.bodyMedium)
            Spacer(modifier = Modifier.height(16.dp))

            // 3. Botón de Google
            OutlinedButton(
                onClick = {
                    // Abre de forma nativa el selector de cuentas de Google de Android
                    googleLauncher.launch(googleSignInClient.signInIntent)
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Iniciar sesión con Google")
            }


            TextButton(onClick = { showResetDialog = true }) {
                Text("¿Olvidaste tu contraseña?")
            }

            TextButton(onClick = onNavigateToRegister) {
                Text("¿No tienes cuenta? Regístrate aquí")
            }
        }
    }

    // Diálogo para recuperar contraseña
    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text("Recuperar Contraseña") },
            text = {
                OutlinedTextField(
                    value = resetEmail,
                    onValueChange = { resetEmail = it },
                    label = { Text("Introduce tu correo") }
                )
            },
            confirmButton = {
                Button(onClick = { viewModel.sendPasswordReset(resetEmail) }) {
                    Text("Enviar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}