package com.example.herretols.ui.catalog

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.example.herretols.data.model.CartItem
import com.example.herretols.data.model.Product
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class CartViewModel : ViewModel() {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    // Lista reactiva de elementos en el carrito
    private val _cartItems = MutableStateFlow<List<CartItem>>(emptyList())
    val cartItems: StateFlow<List<CartItem>> = _cartItems

    // Calcula el precio total acumulado de todos los productos del carrito
    val totalPrice: Double
        get() = _cartItems.value.sumOf { it.subtotal }

//    // 1. AGREGAR PRODUCTO O INCREMENTAR CANTIDAD
//    fun addProduct(product: Product) {
//        val currentList = _cartItems.value.toMutableList()
//        val existingItem = currentList.find { it.product.id == product.id }
//
//        if (existingItem != null) {
//            // Validamos que no exceda el stock disponible en la ferretería
//            if (existingItem.quantity < product.stock) {
//                existingItem.quantity++
//            }
//        } else {
//            currentList.add(CartItem(product = product, quantity = 1))
//        }
//        _cartItems.value = currentList
//    }
//
//    // 2. DISMINUIR CANTIDAD O REMOVER
//    fun removeProduct(product: Product) {
//        val currentList = _cartItems.value.toMutableList()
//        val existingItem = currentList.find { it.product.id == product.id }
//
//        if (existingItem != null) {
//            if (existingItem.quantity > 1) {
//                existingItem.quantity--
//            } else {
//                currentList.remove(existingItem)
//            }
//        }
//        _cartItems.value = currentList
//    }

    // 1. AGREGAR O INCREMENTAR
    fun addProduct(product: Product) {
        val currentList = _cartItems.value.toMutableList()
        val index = currentList.indexOfFirst { it.product.id == product.id }

        if (index != -1) {
            val item = currentList[index]
            if (item.quantity < product.stock) {
                // Reemplazamos con una copia modificada
                currentList[index] = item.copy(quantity = item.quantity + 1)
            }
        } else {
            currentList.add(CartItem(product = product, quantity = 1))
        }
        _cartItems.value = currentList // Al asignar la nueva lista, Compose se redibuja al instante
    }

    // 2. DISMINUIR O REMOVER
    fun removeProduct(product: Product) {
        val currentList = _cartItems.value.toMutableList()
        val index = currentList.indexOfFirst { it.product.id == product.id }

        if (index != -1) {
            val item = currentList[index]
            if (item.quantity > 1) {
                currentList[index] = item.copy(quantity = item.quantity - 1)
            } else {
                currentList.removeAt(index)
            }
        }
        _cartItems.value = currentList
    }

    // 3. PROCESAR LA ORDEN DE COMPRA (Guardar en Firestore)
    fun checkout(onSuccess: () -> Unit, onError: (String) -> Unit) {
        val userId = auth.currentUser?.uid ?: return
        val items = _cartItems.value

        if (items.isEmpty()) {
            onError("El carrito está vacío")
            return
        }

        viewModelScope.launch {
            try {
                // Estructura del pedido para el panel del administrador de la ferretería
                val orderData = mapOf(
                    "clienteId" to userId,
                    "fecha" to System.currentTimeMillis(),
                    "total" to totalPrice,
                    "estado" to "Pendiente", // Estados: Pendiente, Preparando, Entregado
                    "productos" to items.map { mapOf(
                        "productoId" to it.product.id,
                        "nombre" to it.product.nombre,
                        "cantidad" to it.quantity,
                        "precioUnitario" to it.product.precio
                    )}
                )

                // Creamos un documento con ID automático en la colección "pedidos"
                db.collection("pedidos").add(orderData).await()

                // Compra exitosa: limpiamos el carrito local
                _cartItems.value = emptyList()
                onSuccess()
            } catch (e: Exception) {
                onError(e.localizedMessage ?: "Error al procesar el pedido")
            }
        }
    }
}