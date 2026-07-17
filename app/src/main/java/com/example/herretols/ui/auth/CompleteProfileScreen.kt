package com.example.herretols.ui.auth

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.herretols.data.model.AuthState

@Composable
fun CompleteProfileScreen(
    viewModel: AuthViewModel,
    onCompleteSuccess: () -> Unit
) {
    var telefono by remember { mutableStateOf("") }
    var direccion by remember { mutableStateOf("") }
    val authState by viewModel.authState.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(authState) {
        if (authState is AuthState.Success) {
            onCompleteSuccess()
            viewModel.resetState()
        } else if (authState is AuthState.Error) {
            Toast.makeText(context, (authState as AuthState.Error).message, Toast.LENGTH_LONG).show()
            viewModel.resetState()
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("¡Ya casi listo!", style = MaterialTheme.typography.headlineMedium)
        Text("Completa tus datos para los envíos de la ferretería", style = MaterialTheme.typography.bodyMedium)
        Spacer(modifier = Modifier.height(24.dp))

        OutlinedTextField(value = telefono, onValueChange = { telefono = it }, label = { Text("Teléfono Celular") }, modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(value = direccion, onValueChange = { direccion = it }, label = { Text("Dirección de Casa") }, modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.height(16.dp))

        if (authState is AuthState.Loading) {
            CircularProgressIndicator()
        } else {
            Button(
                onClick = {
                    if (telefono.isNotBlank() && direccion.isNotBlank()) {
                        viewModel.completeGoogleUserProfile(telefono, direccion)
                    } else {
                        Toast.makeText(context, "Todos los campos son obligatorios", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Finalizar Registro")
            }
        }
    }
}