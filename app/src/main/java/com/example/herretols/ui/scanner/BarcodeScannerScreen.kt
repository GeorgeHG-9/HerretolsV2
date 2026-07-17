package com.example.herretols.ui.scanner

import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.herretols.data.model.Product
import com.example.herretols.ui.shop.CartViewModel
import com.example.herretols.ui.shop.CatalogViewModel
import java.util.concurrent.Executors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BarcodeScannerScreen(
    catalogViewModel: CatalogViewModel,
    cartViewModel: CartViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val products by catalogViewModel.filteredProducts.collectAsState()

    // Estados de control
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }
    var scannedCode by remember { mutableStateOf<String?>(null) }
    var detectedProduct by remember { mutableStateOf<Product?>(null) }
    var showBottomSheet by remember { mutableStateOf(false) }
    var isProcessing by remember { mutableStateOf(false) }

    // Lanzador para solicitar permisos de la cámara en ejecución
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    // Buscar el producto localmente en la lista descargada una vez detectado el código
    LaunchedEffect(scannedCode) {
        if (scannedCode != null && !isProcessing) {
            isProcessing = true
            val productFound = products.find { it.codigoBarras == scannedCode }
            if (productFound != null) {
                detectedProduct = productFound
                showBottomSheet = true
            } else {
                Toast.makeText(context, "Código no registrado: $scannedCode", Toast.LENGTH_SHORT).show()
                // Pequeño delay para evitar lecturas consecutivas fallidas del mismo código
                kotlinx.coroutines.delay(2000)
                scannedCode = null
                isProcessing = false
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Escanear Código") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Regresar")
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (hasCameraPermission) {
                // Renderizador de la cámara nativa de Android dentro de Compose
                AndroidView(
                    factory = { ctx ->
                        val previewView = PreviewView(ctx)
                        val executor = Executors.newSingleThreadExecutor()
                        val cameraProviderFuture = androidx.camera.lifecycle.ProcessCameraProvider.getInstance(ctx)

                        cameraProviderFuture.addListener({
                            val cameraProvider = cameraProviderFuture.get()

                            val preview = Preview.Builder().build().also {
                                it.setSurfaceProvider(previewView.surfaceProvider)
                            }

                            val imageAnalysis = ImageAnalysis.Builder()
                                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                .build()

                            // Enganchamos nuestro analizador con ML Kit
                            imageAnalysis.setAnalyzer(executor, BarcodeAnalyzer { code ->
                                if (!showBottomSheet && !isProcessing) {
                                    scannedCode = code
                                }
                            })

                            try {
                                cameraProvider.unbindAll()
                                cameraProvider.bindToLifecycle(
                                    lifecycleOwner,
                                    CameraSelector.DEFAULT_BACK_CAMERA,
                                    preview,
                                    imageAnalysis
                                )
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }, ContextCompat.getMainExecutor(ctx))

                        previewView
                    },
                    modifier = Modifier.fillMaxSize()
                )

                // Marco visual de guía central
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Alinea el código de barras aquí",
                        color = MaterialTheme.colorScheme.onPrimary,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.align(Alignment.TopCenter)
                    )
                }
            } else {
                Text(
                    text = "Se requiere acceso a la cámara para usar el escáner.",
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            // --- PANEL INFERIOR MODAL (RESULTADOS DEL PRODUCTO) ---
            if (showBottomSheet && detectedProduct != null) {
                ModalBottomSheet(
                    onDismissRequest = {
                        showBottomSheet = false
                        scannedCode = null
                        isProcessing = false
                    }
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = detectedProduct!!.nombre,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Categoría: ${detectedProduct!!.categoria}",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            text = detectedProduct!!.descripcion,
                            style = MaterialTheme.typography.bodyMedium
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Precio: S/. ${String.format("%.2f", detectedProduct!!.precio)}",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Stock: ${detectedProduct!!.stock} und.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (detectedProduct!!.stock > 0) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.error
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = {
                                if (detectedProduct!!.stock > 0) {
                                    cartViewModel.addProduct(detectedProduct!!)
                                    Toast.makeText(context, "${detectedProduct!!.nombre} agregado", Toast.LENGTH_SHORT).show()
                                    showBottomSheet = false
                                    scannedCode = null
                                    isProcessing = false
                                }
                            },
                            enabled = detectedProduct!!.stock > 0,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Añadir al Carrito")
                        }
                    }
                }
            }
        }
    }
}