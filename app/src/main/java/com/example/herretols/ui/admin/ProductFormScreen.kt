package com.example.herretols.ui.admin

import android.Manifest
import android.graphics.Bitmap
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.herretols.data.model.Product

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductFormScreen(
    adminViewModel: AdminViewModel,
    productToEdit: Product?, // Si es null, el formulario actúa como "Crear"
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current

    // Inicializar los campos con los datos del producto (si se va a editar) o vacíos (si es nuevo)
    var nombre by remember { mutableStateOf(productToEdit?.nombre ?: "") }
    var descripcion by remember { mutableStateOf(productToEdit?.descripcion ?: "") }
    var precio by remember { mutableStateOf(productToEdit?.precio?.toString() ?: "") }
    var stock by remember { mutableStateOf(productToEdit?.stock?.toString() ?: "") }
    var imagenUrl by remember { mutableStateOf(productToEdit?.imagenUrl ?: "") }
    var categoria by remember { mutableStateOf(productToEdit?.categoria ?: "Herramientas") }
    var codigoBarras by remember { mutableStateOf(productToEdit?.codigoBarras ?: "") }
    var capturedBitmap by remember { mutableStateOf<Bitmap?>(null) }


    //IMAGEN IA
    var descripcionIA by remember { mutableStateOf("") }
    val isAnalyzing by adminViewModel.isAnalyzingImage.collectAsState()
    val iaResult by adminViewModel.generatedIaDescription.collectAsState()

    // Escuchamos cuando la IA termine de escribir para actualizar la caja de texto local
    LaunchedEffect(iaResult) {
        if (iaResult.isNotBlank()) {
            descripcionIA = iaResult
        }
    }


    // Lanzador nativo para capturar la foto como un objeto Bitmap
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap: Bitmap? ->
        if (bitmap != null) {
            capturedBitmap = bitmap // <--- Guardamos la referencia
            // Pasamos el Bitmap directo a Gemini
            adminViewModel.generarDescripcionPorIA(bitmap)
        }
    }

    // Lanzador para pedir permiso de cámara
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->

        if (granted) {
            cameraLauncher.launch(null)
        } else {
            Toast.makeText(
                context,
                "Se requiere permiso de cámara",
                Toast.LENGTH_SHORT
            ).show()
        }
    }


    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (productToEdit == null) "Nuevo Producto" else "Editar Producto") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Atrás")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            OutlinedTextField(value = nombre, onValueChange = { nombre = it }, label = { Text("Nombre del Producto") }, modifier = Modifier.fillMaxWidth())

            OutlinedTextField(value = descripcion, onValueChange = { descripcion = it }, label = { Text("Descripción") }, modifier = Modifier.fillMaxWidth(), maxLines = 3)

            // BOTÓN PARA TOMAR LA FOTO CON LA CÁMARA
            Button(
                onClick = {
                    permissionLauncher.launch(
                        Manifest.permission.CAMERA
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
            ) {
                Icon(Icons.Default.CameraAlt, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Tomar Foto para Descripción IA")
            }

            if (isAnalyzing) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Gemini está examinando el producto...", style = MaterialTheme.typography.bodySmall)
                }
            }

            // CAMPO DE TEXTO DE LA DESCRIPCIÓN GENERADA POR IA
            OutlinedTextField(
                value = descripcionIA,
                onValueChange = { descripcionIA = it },
                label = { Text("Descripción Técnica (Autogenerada por IA)") },
                modifier = Modifier.fillMaxWidth().height(120.dp),
                maxLines = 4
            )

            OutlinedTextField(
                value = precio,
                onValueChange = { precio = it },
                label = { Text("Precio (S/.)") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )

            OutlinedTextField(
                value = stock,
                onValueChange = { stock = it },
                label = { Text("Stock Disponible") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )

            OutlinedTextField(value = imagenUrl, onValueChange = { imagenUrl = it }, label = { Text("URL de la Imagen") }, modifier = Modifier.fillMaxWidth())

            OutlinedTextField(value = categoria, onValueChange = { categoria = it }, label = { Text("Categoría (Ej: Herramientas, Gasfitería)") }, modifier = Modifier.fillMaxWidth())

            OutlinedTextField(value =codigoBarras, onValueChange = {codigoBarras = it}, label = {Text("Codigo del producto (Ej: 1234)")}, modifier = Modifier.fillMaxWidth())

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    val precioDouble = precio.toDoubleOrNull()
                    val stockInt = stock.toIntOrNull()

                    if (nombre.isNotBlank() && precioDouble != null && stockInt != null) {
                        // Construir el objeto producto manteniendo el ID original si es una edición
                        val producto = Product(
                            id = productToEdit?.id ?: "",
                            nombre = nombre,
                            descripcion = descripcion,
                            precio = precioDouble,
                            stock = stockInt,
                            imagenUrl = imagenUrl,
                            categoria = categoria,
                            descripcionIA = descripcionIA, // Viene del análisis de Gemini
                        )

                        // SI HAY UNA FOTO CAPTURADA, SUBIR A CLOUDFLARE
                        if (capturedBitmap != null) {
                            adminViewModel.guardarProductoConImagen(
                                product = producto,
                                bitmap = capturedBitmap!!,
                                onSuccess = {
                                    Toast.makeText(context, "Producto e imagen guardados", Toast.LENGTH_SHORT).show()
                                    onNavigateBack()
                                },
                                onError = { error ->
                                    Toast.makeText(context, "Error: $error", Toast.LENGTH_SHORT).show()
                                }
                            )
                        } else {
                            // SI NO HAY FOTO, GUARDAR SOLO TEXTO (comportamiento actual)
                            adminViewModel.saveProduct(
                                product = producto,
                                onSuccess = { onNavigateBack() },
                                onError = { /*...*/ }
                            )
                        }

                        adminViewModel.saveProduct(
                            product = producto,
                            onSuccess = {
                                Toast.makeText(context, "Producto guardado correctamente", Toast.LENGTH_SHORT).show()
                                onNavigateBack()
                            },
                            onError = { error ->
                                Toast.makeText(context, "Error: $error", Toast.LENGTH_SHORT).show()
                            }
                        )
                    } else {
                        Toast.makeText(context, "Por favor, llene los campos numéricos correctamente", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Guardar Producto")
            }

            // Si estamos editando, mostrar un botón adicional para eliminar
            if (productToEdit != null) {
                TextButton(
                    onClick = {
                        adminViewModel.deleteProduct(
                            productId = productToEdit.id,
                            onSuccess = {
                                Toast.makeText(context, "Producto eliminado", Toast.LENGTH_SHORT).show()
                                onNavigateBack()
                            },
                            onError = { error ->
                                Toast.makeText(context, "Error: $error", Toast.LENGTH_SHORT).show()
                            }
                        )
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Eliminar Producto")
                }
            }
        }
    }
}