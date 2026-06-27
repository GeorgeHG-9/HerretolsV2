package com.example.herretols.ui.catalog

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.herretols.data.model.Order
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerOrdersScreen(
    historyViewModel: OrdersHistoryViewModel,
    onNavigateBack: () -> Unit
) {
    val myOrders by historyViewModel.myOrders.collectAsState()
    val isLoading by historyViewModel.isLoading.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mis Pedidos Realizados") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Regresar")
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            contentAlignment = Alignment.Center
        ) {
            if (isLoading) {
                CircularProgressIndicator()
            } else if (myOrders.isEmpty()) {
                Text("Aún no has realizado ninguna compra.")
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(myOrders) { order ->
                        //CustomerOrderCard(order = order)
                        CustomerOrderCard(
                            order = order,
                            onCancelClick = {
                                historyViewModel.cancelOrder(
                                    orderId = order.id,
                                    onSuccess = { /* Firestore actualiza en tiempo real de forma automática */ },
                                    onError = { /* Opcional: mostrar un Toast de error */ }
                                )
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CustomerOrderCard(order: Order, onCancelClick: () -> Unit) {
    val sdf = remember { SimpleDateFormat("dd/MM/yyyy hh:mm a", Locale.getDefault()) }
    val fechaFormateada = remember(order.fecha) { sdf.format(Date(order.fecha)) }

    // Cambiar el color de fondo dinámicamente según los 4 estados posibles
    val containerColor = when(order.estado) {
        "Listo para recoger" -> MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.4f)
        "Entregado" -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
        "Cancelado" -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f)
        else -> MaterialTheme.colorScheme.surfaceVariant // Pendiente
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
//        colors = CardDefaults.cardColors(
//            containerColor = if (order.estado == "Pendiente")
//                MaterialTheme.colorScheme.surfaceVariant
//            else
//                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
//        )
        colors = CardDefaults.cardColors(containerColor = containerColor)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Código: ...${order.id.takeLast(6)}", fontWeight = FontWeight.Bold)
                    Text(fechaFormateada, style = MaterialTheme.typography.bodySmall)
                }

                // Badge de Estado dinámico
                SuggestionChip(
                    onClick = {},
                    label = {
                        Text(
                            text = order.estado,
                            fontWeight = FontWeight.Bold,
                            color = if (order.estado == "Pendiente") MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                        )
                    }
                )
            }

            Divider(modifier = Modifier.padding(vertical = 8.dp))

            order.productos.forEach { prod ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("• ${prod.nombre} x${prod.cantidad}", style = MaterialTheme.typography.bodyMedium)
                    Text("S/. ${String.format("%.2f", prod.precioUnitario * prod.cantidad)}")
                }
            }

            Divider(modifier = Modifier.padding(vertical = 8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Total a pagar en tienda:", fontWeight = FontWeight.Bold)
                    Text(
                        "S/. ${String.format("%.2f", order.total)}",
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                // MOSTRAR BOTÓN DE CANCELAR SOLO SI ESTÁ PENDIENTE
                if (order.estado == "Pendiente") {
                    OutlinedButton(
                        onClick = onCancelClick,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.error),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Text("Cancelar Pedido", style = MaterialTheme.typography.bodySmall)
                    }
                } else if (order.estado == "Listo para recoger") {
                    Text("¡Listo en tienda!", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.tertiary)
                }
            }

        }
    }
}