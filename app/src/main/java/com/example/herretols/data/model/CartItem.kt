package com.example.herretols.data.model


data class CartItem(
    val product: Product,
    val quantity: Int = 1
) {
    // Calcula el subtotal de esta línea de producto
    val subtotal: Double get() = product.precio * quantity
}