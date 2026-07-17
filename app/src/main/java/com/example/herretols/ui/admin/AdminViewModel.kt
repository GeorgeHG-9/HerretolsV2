package com.example.herretols.ui.admin

import android.R.attr.content
import android.R.id.content
import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.example.herretols.data.model.Order
import com.example.herretols.data.model.OrderProduct
import com.example.herretols.data.model.Product
import com.example.herretols.config.Secrets

import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.io.ByteArrayOutputStream
import java.util.UUID

class AdminViewModel : ViewModel() {

    private val db = FirebaseFirestore.getInstance()

    private val _orders = MutableStateFlow<List<Order>>(emptyList())
    val orders: StateFlow<List<Order>> = _orders

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    // Estados para los KPIs
    val totalPedidos = MutableStateFlow(0)
    val pedidosPendientes = MutableStateFlow(0)
    val valorTotalStock = MutableStateFlow(0.0)

    //Para imagen IA
    val isAnalyzingImage = MutableStateFlow(false)
    val generatedIaDescription = MutableStateFlow("")

    val apiKey = Secrets.GEMINI_API_KEY

    init {
        fetchOrders() // Escucha los pedidos desde que abre el panel
    }

    // Estado para el Top 5: Par de (Nombre del Producto, Cantidad Total Vendida)
    private val _topProducts = MutableStateFlow<List<Pair<String, Int>>>(emptyList())
    val topProducts: StateFlow<List<Pair<String, Int>>> = _topProducts

    // Lógica para procesar las métricas cada vez que cambien los pedidos o los productos
    fun calcularMetricas(pedidos: List<Order>, productos: List<Product>) {
        viewModelScope.launch {
            // Filtramos para obtener solo los pedidos activos (omitimos los cancelados)
            val pedidosActivos = pedidos.filter { it.estado != "Cancelado" }

            // 1. Tarjetas KPI Básicas
            totalPedidos.value = pedidos.size
            pedidosPendientes.value = pedidos.count { it.estado == "Pendiente" }
            valorTotalStock.value = productos.sumOf { it.precio * it.stock }

            // 2. Calcular Top 5 Productos más vendidos
            // Convertimos todos los productos de todas las órdenes en una sola lista plana
            val todosLosProductosVendidos = pedidosActivos.flatMap { it.productos }

            // Agrupamos por nombre de producto y sumamos sus cantidades
            val conteoPorProducto = todosLosProductosVendidos
                .groupBy { it.nombre }
                .mapValues { entry -> entry.value.sumOf { it.cantidad } }

            // Ordenamos de mayor a menor y nos quedamos con los 5 primeros
            val top5 = conteoPorProducto.toList()
                .sortedByDescending { it.second }
                .take(5)

            _topProducts.value = top5
        }
    }


    fun generarDescripcionPorIA(bitmap: Bitmap) {
        isAnalyzingImage.value = true
        viewModelScope.launch {
            try {
                // Usamos el mismo modelo Gemini 2.5 Flash
                val generativeModel = GenerativeModel(
                    modelName = "gemini-2.5-flash",
                    apiKey = apiKey
                )

                // Empaquetamos la imagen física y el texto de instrucción (Prompt de Visión)
                val inputContent = content {
                    image(bitmap)
                    text("""
                    Analiza detenidamente la fotografía de este producto de ferretería. 
                    Genera una descripción técnica, comercial y profesional de máximo 2 líneas. 
                    Identifica qué tipo de herramienta u objeto es, resalta sus características visuales más importantes (como material, color, posibles usos o resistencia) y redacta en español de forma atractiva para un catálogo de ventas.
                """.trimIndent())
                }

                // Solicitamos la respuesta multimodal
                val response = generativeModel.generateContent(inputContent)
                generatedIaDescription.value = response.text ?: "No se logró procesar la imagen."

            } catch (e: Exception) {
                generatedIaDescription.value = "Error al analizar con IA: ${e.localizedMessage}"
            } finally {
                isAnalyzingImage.value = false
            }
        }
    }


    private fun fetchOrders() {
        _isLoading.value = true
        // Escucha en tiempo real ordenando por fecha descendente
        db.collection("pedidos")
            .orderBy("fecha", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    _isLoading.value = false
                    return@addSnapshotListener
                }

                val orderList = snapshot?.documents?.mapNotNull { doc ->
                    val id = doc.id
                    val clienteId = doc.getString("clienteId") ?: ""
                    val fecha = doc.getLong("fecha") ?: 0L
                    val total = doc.getDouble("total") ?: 0.0
                    val estado = doc.getString("estado") ?: "Pendiente"

                    // Mapear la lista interna de productos del pedido
                    val productosRaw = doc.get("productos") as? List<Map<String, Any>> ?: emptyList()
                    val productos = productosRaw.map { map ->
                        OrderProduct(
                            productoId = map["productoId"] as? String ?: "",
                            nombre = map["nombre"] as? String ?: "",
                            cantidad = (map["cantidad"] as? Long)?.toInt() ?: 0,
                            precioUnitario = map["precioUnitario"] as? Double ?: 0.0
                        )
                    }

                    Order(id, clienteId, fecha, total, estado, productos)
                } ?.filter {it.estado != "Cancelado"}//No mostrar pedidos cancelados
                    ?: emptyList()

                _orders.value = orderList
                _isLoading.value = false
            }
    }

    // Función para cambiar el estado del pedido (Ej: de Pendiente a Entregado)
    fun updateOrderStatus(orderId: String, nuevoEstado: String) {
        viewModelScope.launch {
            try {
                db.collection("pedidos").document(orderId)
                    .update("estado", nuevoEstado)
            } catch (e: Exception) {
                // Manejar error de actualización opcionalmente
            }
        }
    }


    // --- SECCIÓN CRUD DE PRODUCTOS ---

    // Función para Crear o Editar un producto
    fun saveProduct(product: Product, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                if (product.id.isBlank()) {
                    // 1. CREAR: Si no tiene ID, generamos un documento nuevo con ID automático
                    val newDoc = db.collection("productos").document()
                    val productWithId = product.copy(id = newDoc.id)
                    newDoc.set(productWithId.toMap()).await()
                } else {
                    // 2. EDITAR: Si ya tiene ID, sobreescribimos el documento existente
                    db.collection("productos").document(product.id).set(product.toMap()).await()
                }
                onSuccess()
            } catch (e: Exception) {
                onError(e.localizedMessage ?: "Error al guardar el producto")
            }
        }
    }

    // Función para Eliminar un producto
    fun deleteProduct(productId: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                db.collection("productos").document(productId).delete().await()
                onSuccess()
            } catch (e: Exception) {
                onError(e.localizedMessage ?: "Error al eliminar el producto")
            }
        }
    }
}