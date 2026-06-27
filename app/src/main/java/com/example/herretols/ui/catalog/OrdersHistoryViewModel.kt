package com.example.herretols.ui.catalog

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.example.herretols.data.model.Order
import com.example.herretols.data.model.OrderProduct
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class OrdersHistoryViewModel : ViewModel() {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val _myOrders = MutableStateFlow<List<Order>>(emptyList())
    val myOrders: StateFlow<List<Order>> = _myOrders

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    init {
        fetchMyOrders()
    }

    fun fetchMyOrders() {
        val userId = auth.currentUser?.uid ?: return
        _isLoading.value = true

        // Hacemos una consulta filtrando por "clienteId" e igualando al UID del usuario actual
        db.collection("pedidos")
            .whereEqualTo("clienteId", userId)
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
                }?.filter{it.estado != "Cancelado"} //Filtro para no ver cancelados
                ?.sortedByDescending { it.fecha } ?: emptyList() // Los más recientes primero

                _myOrders.value = orderList
                _isLoading.value = false
            }
    }


    fun cancelOrder(orderId: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                // Regla de validación: Cambiamos el estado a "Cancelado" directamente en Firestore
                db.collection("pedidos").document(orderId)
                    .update("estado", "Cancelado")
                    .addOnSuccessListener { onSuccess() }
                    .addOnFailureListener { e -> onError(e.localizedMessage ?: "Error") }
            } catch (e: Exception) {
                onError(e.localizedMessage ?: "Error de red")
            }
        }
    }
}