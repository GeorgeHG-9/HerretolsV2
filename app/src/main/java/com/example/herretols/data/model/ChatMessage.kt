package com.example.herretols.data.model

data class ChatMessage(
    val text: String,
    val isUser: Boolean, // true si lo escribió el cliente, false si responde Gemini
    val timestamp: Long = System.currentTimeMillis()
)