package com.example.herretols.data.model

data class Product(
    val id: String = "",
    val nombre: String = "",
    val descripcion: String = "",
    val precio: Double = 0.0,
    val stock: Int = 0,
    val imagenUrl: String = "",
    val categoria: String = "",
    val codigoBarras: String = "",
    val descripcionIA: String = ""

) {
    // Convierte el documento de Firestore directamente a nuestro objeto Kotlin
    fun toMap(): Map<String, Any> = mapOf(
        "id" to id,
        "nombre" to nombre,
        "descripcion" to descripcion,
        "precio" to precio,
        "stock" to stock,
        "imagenUrl" to imagenUrl,
        "categoria" to categoria,
        "descripcionIA" to descripcionIA,
        "codigoBarras" to codigoBarras,
    )
}