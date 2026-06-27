package com.example.herretols.ui.catalog

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.FirebaseFirestore
import com.example.herretols.data.model.Product
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

class CatalogViewModel : ViewModel() {
    private val db = FirebaseFirestore.getInstance()
    // Estado original de la base de datos
    private val _allProducts = MutableStateFlow<List<Product>>(emptyList())
    // Estados de los filtros de la interfaz
    val searchQuery = MutableStateFlow("")
    val selectedCategory = MutableStateFlow("Todos")

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading
    // Lista combinada y filtrada automáticamente en tiempo real
    val filteredProducts: StateFlow<List<Product>> = combine(
        _allProducts, searchQuery, selectedCategory
    ) { products, query, category ->
        products.filter { product ->
            val matchesQuery = product.nombre.contains(query, ignoreCase = true) ||
                    product.descripcion.contains(query, ignoreCase = true)
            val matchesCategory = category == "Todos" || product.categoria == category
            matchesQuery && matchesCategory
        }
    }.stateIn(viewModelScope, kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5000), emptyList())

    // Lista fija de categorías para la ferretería (puedes adaptarla)
    val categories = listOf("Todos", "Herramientas Eléctricas", "Herramientas Manuales","Pinturas")
    init {
        fetchProducts()
    }

    private fun fetchProducts() {
        _isLoading.value = true
        db.collection("productos").addSnapshotListener { snapshot, error ->
            if (error != null) return@addSnapshotListener

            val productList = snapshot?.documents?.mapNotNull { doc ->
                doc.toObject(Product::class.java)?.copy(id = doc.id)
            } ?: emptyList()

            _allProducts.value = productList
            _isLoading.value = false
        }
    }
}