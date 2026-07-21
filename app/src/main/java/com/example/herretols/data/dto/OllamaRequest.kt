package com.example.herretols.data.dto

data class OllamaRequest(
    val model: String,
    val prompt: String,
    val images: List<String>? = null, // Para modelos con visión como Llava
    val stream: Boolean = false
)