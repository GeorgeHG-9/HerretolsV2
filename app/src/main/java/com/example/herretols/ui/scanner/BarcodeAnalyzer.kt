package com.example.herretols.ui.scanner

import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode

class BarcodeAnalyzer(
    private val onBarcodeDetected: (String) -> Unit
) : ImageAnalysis.Analyzer {

    // Inicializamos el cliente oficial de escaneo de ML Kit
    private val scanner = BarcodeScanning.getClient()

    @OptIn(ExperimentalGetImage::class)
    override fun analyze(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        if (mediaImage != null) {
            // Convertimos el frame físico de la cámara al formato nativo de ML Kit
            val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)

            scanner.process(image)
                .addOnSuccessListener { barcodes ->
                    for (barcode in barcodes) {
                        // Validamos que el código contenga texto numérico legible
                        val rawValue = barcode.rawValue
                        if (!rawValue.isNullOrBlank()) {
                            onBarcodeDetected(rawValue)
                            break // Detener el bucle en la primera detección exitosa
                        }
                    }
                }
                .addOnCompleteListener {
                    // CRÍTICO: Cerramos el frame para indicarle a CameraX que libere la memoria y mande el siguiente cuadro
                    imageProxy.close()
                }
        } else {
            imageProxy.close()
        }
    }
}