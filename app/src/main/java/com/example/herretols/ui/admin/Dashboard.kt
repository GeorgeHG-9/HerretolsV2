package com.example.herretols.ui.admin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.herretols.ui.auth.AuthViewModel

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Build
import androidx.compose.foundation.lazy.items as lazyItems
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.herretols.data.model.Order
import com.example.herretols.data.model.Product
import com.example.herretols.ui.catalog.CatalogViewModel
import com.example.herretols.ui.catalog.ProductCard
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

//@Composable
//fun AdminDashboardScreen(viewModel: AuthViewModel, onLogout: () -> Unit) {
//    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
//        Column(horizontalAlignment = Alignment.CenterHorizontally) {
//            Text("Bienvenido, Administrador (CRUD de Productos)")
//            Spacer(modifier = Modifier.height(16.dp))
//            Button(onClick = {
//                viewModel.logout()
//                onLogout()
//            }) {
//                Text("Cerrar Sesión")
//            }
//        }
//    }
//}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboardScreen(
    viewModel: AuthViewModel,
    adminViewModel: AdminViewModel = AdminViewModel(), // Inyectamos el ViewModel del Admin
    catalogViewModel: CatalogViewModel = viewModel(), // Reutilizamos el lector de productos
    onLogout: () -> Unit,
    onNavigateToForm: (Product?) -> Unit // Callback para abrir el formulario
) {
    // Estado para controlar qué pestaña está activa (0 = Pedidos, 1 = Inventario)
    var selectedTab by remember { mutableStateOf(0) }

    val orders by adminViewModel.orders.collectAsState()
    val isOrdersLoading by adminViewModel.isLoading.collectAsState()

    val products by catalogViewModel.filteredProducts.collectAsState()
    val isProductsLoading by catalogViewModel.isLoading.collectAsState()

    // Escuchamos las variables numéricas calculadas en el ViewModel
    val totalPedidos by adminViewModel.totalPedidos.collectAsState()
    val pedidosPendientes by adminViewModel.pedidosPendientes.collectAsState()
    val valorTotalStock by adminViewModel.valorTotalStock.collectAsState()
    val topProducts by adminViewModel.topProducts.collectAsState()

    // REACCIÓN EN TIEMPO REAL: Cada vez que varíen los pedidos o productos, recalculamos la analítica
    LaunchedEffect(orders, products) {
        if (orders.isNotEmpty() || products.isNotEmpty()) {
            adminViewModel.calcularMetricas(orders, products)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Panel Admin - Pedidos") },
                actions = {
                    IconButton(onClick = { viewModel.logout(); onLogout() }) {
                        Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = "Cerrar Sesión")
                    }
                }
            )
        },
        // Botón flotante (+) que solo se muestra si estamos en la pestaña de Inventario
        floatingActionButton = {
            if (selectedTab == 1) {
                FloatingActionButton(
                    onClick = { onNavigateToForm(null) }, // Mandamos null para indicar "Nuevo"
                    containerColor = MaterialTheme.colorScheme.primary
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Agregar Producto", tint = MaterialTheme.colorScheme.onPrimary)
                }
            }
        }

    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // 1. BARRA DE PESTAÑAS (TABS)
            TabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Pedidos Recientes") },
                    icon = { Icon(Icons.Default.List, contentDescription = null) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Inventario CRUD") },
                    icon = { Icon(Icons.Default.Build, contentDescription = null) }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text("Métricas") },
                    icon = { Icon(Icons.Default.BarChart, contentDescription = null) }
                )
            }

            // 2. CONTENIDO SEGÚN LA PESTAÑA SELECCIONADA
            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.Center
            ) {
                when (selectedTab) {
                    0 -> {
                        // --- PESTAÑA PEDIDOS (Código previo) ---
                        if (isOrdersLoading) {
                            CircularProgressIndicator()
                        } else if (orders.isEmpty()) {
                            Text("No hay pedidos registrados.")
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize().padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                lazyItems(orders) { pedido ->
                                    OrderCard(order = pedido, onUpdateStatus = { nuevoEstado ->
                                        adminViewModel.updateOrderStatus(pedido.id, nuevoEstado)
                                    })
                                }
                            }
                        }
                    }
                    1 -> {
                        // --- PESTAÑA INVENTARIO (CRUD) ---
                        if (isProductsLoading) {
                            CircularProgressIndicator()
                        } else if (products.isEmpty()) {
                            Text("No hay productos en el inventario.")
                        } else {
                            // Cuadrícula que muestra los productos vigentes
                            LazyVerticalGrid(
                                columns = GridCells.Fixed(2),
                                modifier = Modifier.fillMaxSize().padding(4.dp)
                            ) {
                                items(products) { producto ->
                                    // Reutilizamos la ProductCard pero interceptamos el click para editar
                                    Box(modifier = Modifier.padding(4.dp)) {
                                        ProductCard(
                                            product = producto,
                                            onAddToCart = { onNavigateToForm(producto) } // Redefinimos la acción del botón para "Editar"
                                        )
                                    }
                                }
                            }
                        }
                    }
                    2 -> {
                        // --- PESTAÑA MÉTRICAS (NUEVO) ---
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp)
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Text("Indicadores Clave (KPIs)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

                            // Fila de tarjetas analíticas
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Card(modifier = Modifier.weight(1f)) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text("Pedidos Totales", style = MaterialTheme.typography.bodySmall)
                                        Text("$totalPedidos", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                                    }
                                }
                                Card(modifier = Modifier.weight(1f)) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text("Por Despachar", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                                        Text("$pedidosPendientes", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                                    }
                                }
                            }

                            // Tarjeta de Valorización Financiera
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text("Inversión Total en Inventario (Costo de Activos)", style = MaterialTheme.typography.bodyMedium)
                                    Text("S/. ${String.format("%.2f", valorTotalStock)}", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onSecondaryContainer)
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Gráfico del Top 5
                            Text("Top 5 Productos Más Vendidos", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            if (topProducts.isEmpty()) {
                                Text("Aún no hay ventas suficientes para tabular tendencias.", style = MaterialTheme.typography.bodyMedium)
                            } else {
                                Card(modifier = Modifier.fillMaxWidth()) {
                                    Box(modifier = Modifier.padding(12.dp)) {
                                        BarChartCustom(data = topProducts)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun OrderCard(order: Order, onUpdateStatus: (String) -> Unit) {
    // Formatear la fecha/hora en milisegundos a algo legible
    val sdf = remember { SimpleDateFormat("dd/MM/yyyy hh:mm a", Locale.getDefault()) }
    val fechaFormateada = remember(order.fecha) { sdf.format(Date(order.fecha)) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (order.estado == "Pendiente")
                MaterialTheme.colorScheme.surfaceVariant
            else
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Cabecera de la orden
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(text = "Pedido ID: ...${order.id.takeLast(6)}", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    Text(text = fechaFormateada, style = MaterialTheme.typography.bodySmall)
                }
                // Etiqueta de estado visual
                SuggestionChip(
                    onClick = { },
                    label = { Text(order.estado) }
                )
            }

            Divider(modifier = Modifier.padding(vertical = 8.dp))

            // Lista de productos dentro de este pedido específico
            Text(text = "Productos:", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
            order.productos.forEach { prod ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "• ${prod.nombre} x${prod.cantidad}", style = MaterialTheme.typography.bodyMedium)
                    Text(text = "S/. ${String.format("%.2f", prod.precioUnitario * prod.cantidad)}", style = MaterialTheme.typography.bodyMedium)
                }
            }

            Divider(modifier = Modifier.padding(vertical = 8.dp))

            // Cambiar el bloque de botones inferior dentro de la función OrderCard:
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Total: S/. ${String.format("%.2f", order.total)}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary
                )

                // Flujo de botones según el estado actual
                when (order.estado) {
                    "Pendiente" -> {
                        Button(
                            onClick = { onUpdateStatus("Listo para recoger") },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)
                        ) {
                            Text("Marcar: Listo para Recojo")
                        }
                    }
                    "Listo para recoger" -> {
                        Button(
                            onClick = { onUpdateStatus("Entregado") },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Entregar Pedido")
                        }
                    }
                    "Entregado" -> {
                        Text("✔ Entregado", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    }
                    "Cancelado" -> {
                        Text("❌ Cancelado por Cliente", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }


            // Fila inferior con el Total General y el botón de acción
//            Row(
//                modifier = Modifier.fillMaxWidth(),
//                horizontalArrangement = Arrangement.SpaceBetween,
//                verticalAlignment = Alignment.CenterVertically
//            ) {
//                Text(
//                    text = "Total: S/. ${String.format("%.2f", order.total)}",
//                    style = MaterialTheme.typography.titleMedium,
//                    fontWeight = FontWeight.ExtraBold,
//                    color = MaterialTheme.colorScheme.primary
//                )
//
//                // Si está pendiente, muestra el botón para despacharlo/entregarlo
//                if (order.estado == "Pendiente") {
//                    Button(
//                        onClick = { onUpdateStatus("Entregado") },
//                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
//                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp)
//                    ) {
//                        Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
//                        Spacer(modifier = Modifier.width(4.dp))
//                        Text("Entregar")
//                    }
//                } else {
//                    // Si ya fue entregado, muestra la opción para revertirlo por si hubo un error táctil
//                    TextButton(onClick = { onUpdateStatus("Pendiente") }) {
//                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp))
//                        Spacer(modifier = Modifier.width(4.dp))
//                        Text("Revertir a Pendiente", style = MaterialTheme.typography.bodySmall)
//                    }
//                }
//            }
        }
    }
}