package com.example.herretols.ui.navigation

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.herretols.ui.auth.AuthViewModel
import com.example.herretols.ui.auth.LoginScreen
import com.example.herretols.ui.auth.RegisterScreen
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.example.herretols.data.model.Product
import com.example.herretols.ui.admin.AdminDashboardScreen
import com.example.herretols.ui.admin.AdminViewModel
import com.example.herretols.ui.admin.ProductFormScreen
import com.example.herretols.ui.auth.CompleteProfileScreen
import com.example.herretols.ui.auth.SplashScreen
import com.example.herretols.ui.shop.CartScreen
import com.example.herretols.ui.shop.CartViewModel
import com.example.herretols.ui.shop.CatalogViewModel
import com.example.herretols.ui.shop.CustomerCatalogScreen
import com.example.herretols.ui.orders.OrdersHistoryViewModel
import com.example.herretols.ui.orders.CustomerOrdersScreen
import com.example.herretols.ui.chat.ChatAssistantScreen
import com.example.herretols.ui.chat.ChatAssistantViewModel
import com.example.herretols.ui.scanner.BarcodeScannerScreen

import com.example.herretols.ui.shop.ProductDetailContent

// 1. Definición de las rutas del sistema
sealed class Screen(val route: String) {
    object Splash : Screen("splash") // ◄ Nueva pantalla inicial
    object Login : Screen("login")
    object CompleteProfile : Screen("complete_profile") // ◄ Nueva pantalla
    object Register : Screen("register")
    object AdminDashboard : Screen("admin_dashboard")
    object CustomerCatalog : Screen("customer_catalog")
    object Cart : Screen("cart")
    object ProductForm : Screen("product_form")
    object CustomerOrders : Screen("customer_orders")
    object Scanner : Screen("scanner")
    object ChatAssistant : Screen("chat_assistant")

    object ProductDetail : Screen("product_detail")
}

@Composable
fun AppNavigation() {

    val navController = rememberNavController()
    // Compartimos el mismo AuthViewModel para el flujo de autenticación
    val authViewModel: AuthViewModel = viewModel()
    val cartViewModel: CartViewModel = viewModel()
    val adminViewModel: AdminViewModel = viewModel() // ViewModel del Admin global para la navegación
    val catalogViewModel: CatalogViewModel = viewModel()
    val historyViewModel: OrdersHistoryViewModel = viewModel()
    val chatViewModel: ChatAssistantViewModel = viewModel()

    // Estado temporal para pasar el producto a editar entre pantallas sin romper la arquitectura
    var selectedProductToEdit by remember { mutableStateOf<Product?>(null) }

    var selectedProductDetail by remember {
        mutableStateOf<Product?>(null)
    }

    NavHost(
        navController = navController,
        startDestination = Screen.Splash.route // ◄ Arranca aquí ahora
    ) {
        // PANTALLA SPLASH: Verifica persistencia
        composable(Screen.Splash.route) {
            SplashScreen(
                viewModel = authViewModel,
                onNavigateToLogin = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                },
                onNavigateToHome = { rol ->
                    val destino = if (rol == "admin") Screen.AdminDashboard.route else Screen.CustomerCatalog.route
                    navController.navigate(destino) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                }
            )
        }
        // Pantalla de Login (Modifica el popUpTo para evitar retrocesos al Login)
        composable(Screen.Login.route) {
            LoginScreen(
                viewModel = authViewModel,
                onNavigateToRegister = { navController.navigate(Screen.Register.route) },
                onNavigateToCompleteProfile = {
                    navController.navigate(Screen.CompleteProfile.route)
                },
                onLoginSuccess = { rol ->
                    val destino = if (rol == "admin") Screen.AdminDashboard.route else Screen.CustomerCatalog.route
                    navController.navigate(destino) {
                        popUpTo(Screen.Login.route) { inclusive = true } // ◄ Evita que al dar "atrás" vuelva al Login
                    }
                }
            )
        }
        // Pantalla de Registro
        composable(Screen.Register.route) {
            RegisterScreen(
                viewModel = authViewModel,
                onNavigateToLogin = { navController.popBackStack() }
            )
        }

        composable(Screen.CustomerCatalog.route) {
            CustomerCatalogScreen(
                viewModel = authViewModel,
                cartViewModel = cartViewModel,
                onNavigateToCart = { navController.navigate(Screen.Cart.route) },
                // ◄ NUEVO: Callback para navegar al historial desde el catálogo
                onNavigateToHistory = { navController.navigate(Screen.CustomerOrders.route) },
                onNavigateToScanner = { navController.navigate(Screen.Scanner.route) },
                onNavigateToChat = { navController.navigate(Screen.ChatAssistant.route) },
                onNavigateToProductDetail = { product ->
                    selectedProductDetail = product
                    navController.navigate(
                        Screen.ProductDetail.route
                    )
                },
                onLogout = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.CustomerCatalog.route) { inclusive = true }
                    }
                }
            )
        }

        // Pantalla de Historial del Cliente
        composable(Screen.CustomerOrders.route) {
            CustomerOrdersScreen(
                historyViewModel = historyViewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // Nueva ruta para la Pantalla del Carrito de Compras
        composable(Screen.Cart.route) {
            CartScreen(
                cartViewModel = cartViewModel,
                onNavigateBack = { navController.popBackStack() },
                onCheckoutSuccess = {
                    // Si la compra tiene éxito, lo regresamos al catálogo limpio
                    navController.popBackStack(Screen.CustomerCatalog.route, false)
                }
            )
        }

        // Panel de Administrador principal
        composable(Screen.AdminDashboard.route) {
            AdminDashboardScreen(
                viewModel = authViewModel,
                adminViewModel = adminViewModel,
                onLogout = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.AdminDashboard.route) { inclusive = true }
                    }
                },
                onNavigateToForm = { producto ->
                    selectedProductToEdit = producto // Guardamos el producto (o null)
                    navController.navigate(Screen.ProductForm.route)
                }
            )
        }

        // Pantalla del Formulario de Productos (CRUD)
        composable(Screen.ProductForm.route) {
            ProductFormScreen(
                adminViewModel = adminViewModel,
                productToEdit = selectedProductToEdit,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // Ruta para la cámara del escáner
        composable(Screen.Scanner.route) {
            BarcodeScannerScreen(
                catalogViewModel = catalogViewModel,
                cartViewModel = cartViewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.ChatAssistant.route) {
            ChatAssistantScreen(
                chatViewModel = chatViewModel,
                catalogViewModel = catalogViewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // Pantalla intermedia de Google
        composable(Screen.CompleteProfile.route) {
            CompleteProfileScreen(
                viewModel = authViewModel,
                onCompleteSuccess = {
                    navController.navigate(Screen.CustomerCatalog.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.ProductDetail.route) {
            selectedProductDetail?.let { product ->
                ProductDetailContent(
                    product = product,
                    onNavigateBack = {
                        navController.popBackStack()
                    }
                )
            }
        }
    }
}

