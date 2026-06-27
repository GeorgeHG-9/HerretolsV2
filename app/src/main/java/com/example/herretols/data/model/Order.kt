package com.example.herretols.data.model

data class Order(
    val id: String = "",
    val clienteId: String = "",
    val fecha: Long = 0L,
    val total: Double = 0.0,
    val estado: String = "Pendiente", // "Pendiente", "Entregado"
    val productos: List<OrderProduct> = emptyList()
)

data class OrderProduct(
    val productoId: String = "",
    val nombre: String = "",
    val cantidad: Int = 0,
    val precioUnitario: Double = 0.0
)