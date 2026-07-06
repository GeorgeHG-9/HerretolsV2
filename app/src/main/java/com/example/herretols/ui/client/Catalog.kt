package com.example.herretols.ui.client

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.herretols.ui.auth.AuthViewModel
import com.example.herretols.ui.catalog.CatalogViewModel

// Importaciones necesarias para actualizar el CustomerCatalogScreen
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.unit.dp
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBar
import androidx.compose.ui.platform.LocalContext
import com.example.herretols.ui.catalog.CartViewModel
import com.example.herretols.ui.catalog.ProductCard
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.ListAlt
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.ListAlt
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.foundation.lazy.items as lazyRowItems

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerCatalogScreen(
    viewModel: AuthViewModel,
    catalogViewModel: CatalogViewModel = viewModel(),
    cartViewModel: CartViewModel, // ◄ Recibe el carrito compartido
    onNavigateToCart: () -> Unit,  // ◄ Callback de navegación
    onNavigateToHistory: () -> Unit, // ◄ Añadido a los parámetros
    onNavigateToScanner: () -> Unit,
    onNavigateToChat: () -> Unit,
    onLogout: () -> Unit
) {
    val products by catalogViewModel.filteredProducts.collectAsState()
    val searchQuery by catalogViewModel.searchQuery.collectAsState()
    val selectedCategory by catalogViewModel.selectedCategory.collectAsState()
    val isLoading by catalogViewModel.isLoading.collectAsState()
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Ferretería") },
                actions = {
                    IconButton(onClick = onNavigateToScanner) {
                        Icon(Icons.Default.QrCodeScanner, contentDescription = "Escanear")
                    }

                    IconButton(onClick = onNavigateToHistory) {
                        Icon(Icons.Default.ListAlt, contentDescription = "Mis Pedidos")
                    }
                    IconButton(onClick = onNavigateToCart) {
                        Icon(Icons.Default.ShoppingCart, contentDescription = "Carrito")
                    }

                    IconButton(onClick = { viewModel.logout(); onLogout() }) {
                        Icon(Icons.Default.ExitToApp, contentDescription = "Salir")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateToChat
            ) {
                Icon(
                    imageVector = Icons.Default.Chat,
                    contentDescription = "Chat IA"
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // 1. BARRA DE BÚSQUEDA
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { catalogViewModel.searchQuery.value = it },
                label = { Text("Buscar producto...") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                singleLine = true
            )

            // 2. FILTRO DE CATEGORÍAS (Fila deslizable)
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                lazyRowItems(catalogViewModel.categories) { categoria ->
                    FilterChip(
                        selected = selectedCategory == categoria,
                        onClick = { catalogViewModel.selectedCategory.value = categoria },
                        label = { Text(categoria) }
                    )
                }
            }

            // 3. CUADRÍCULA DE PRODUCTOS
            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.Center
            ) {
                if (isLoading) {
                    CircularProgressIndicator()
                } else if (products.isEmpty()) {
                    Text("No se encontraron productos.")
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        modifier = Modifier.fillMaxSize().padding(horizontal = 4.dp)
                    ) {
                        items(products) { producto ->
                            ProductCard(
                                product = producto,
                                onAddToCart = { prod ->
                                    cartViewModel.addProduct(prod)
                                    Toast.makeText(context, "${prod.nombre} añadido", Toast.LENGTH_SHORT).show()
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
