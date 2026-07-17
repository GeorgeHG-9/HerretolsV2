package com.example.herretols.ui.shop

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.herretols.ui.shop.CartViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CartScreen(
    cartViewModel: CartViewModel,
    onNavigateBack: () -> Unit,
    onCheckoutSuccess: () -> Unit
) {
    val cartItems by cartViewModel.cartItems.collectAsState()
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Tu Carrito de Compras") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Regresar")
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
        ) {
            if (cartItems.isEmpty()) {
                Box(modifier = Modifier.weight(1.0f), contentAlignment = Alignment.Center) {
                    Text("Tu carrito está vacío. ¡Agrega herramientas o materiales!")
                }
            } else {
                // Lista de productos en el carrito (Scroll eficiente)
                LazyColumn(modifier = Modifier.weight(1.0f)) {
                    items(cartItems) { item ->
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp).fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(item.product.nombre, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                                    Text("Precio unitario: S/. ${String.format("%.2f", item.product.precio)}")
                                    Text("Subtotal: S/. ${String.format("%.2f", item.subtotal)}", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                                }

                                // Controladores de cantidad (+ / -)
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(onClick = { cartViewModel.removeProduct(item.product) }) {
                                        Icon(if(item.quantity == 1) Icons.Default.Delete else Icons.Default.Delete, contentDescription = "Disminuir", tint = MaterialTheme.colorScheme.error)
                                    }
                                    Text("${item.quantity}", modifier = Modifier.padding(horizontal = 8.dp), style = MaterialTheme.typography.titleMedium)
                                    IconButton(onClick = { cartViewModel.addProduct(item.product) }) {
                                        Text("+", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary)
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Sección de resumen de pago
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp).fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Total General:", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text("S/. ${String.format("%.2f", cartViewModel.totalPrice)}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Botón final de compra
                Button(
                    onClick = {
                        cartViewModel.checkout(
                            onSuccess = {
                                Toast.makeText(context, "¡Pedido registrado con éxito!", Toast.LENGTH_LONG).show()
                                onCheckoutSuccess()
                            },
                            onError = { error ->
                                Toast.makeText(context, "Error: $error", Toast.LENGTH_SHORT).show()
                            }
                        )
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Confirmar Pedido (Pago en Tienda)")
                }
            }
        }
    }
}