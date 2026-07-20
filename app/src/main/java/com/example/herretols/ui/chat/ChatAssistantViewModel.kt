package com.example.herretols.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import com.example.herretols.data.model.ChatMessage
import com.example.herretols.data.model.Product
import com.example.herretols.config.Secrets.GEMINI_API_KEY
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class ChatAssistantViewModel : ViewModel() {

    // Lista en memoria de la conversación actual en la interfaz
    private val _messages = MutableStateFlow<List<ChatMessage>>(
        listOf(ChatMessage("¡Hola! Soy tu asistente ferretero inteligente. ¿En qué proyecto o reparación te puedo ayudar hoy?", isUser = false))
    )
    val messages: StateFlow<List<ChatMessage>> = _messages

    private val _isResponding = MutableStateFlow(false)
    val isResponding: StateFlow<Boolean> = _isResponding

    // LLAVE DE GOOGLE AI STUDIO
    private val apiKey = GEMINI_API_KEY

    fun sendMessage(userText: String, currentProducts: List<Product>) {
        if (userText.isBlank() || _isResponding.value) return

        // 1. Pintar el mensaje del usuario inmediatamente en la pantalla
        val userMessage = ChatMessage(text = userText, isUser = true)
        _messages.value = _messages.value + userMessage
        _isResponding.value = true

        viewModelScope.launch {
            try {
                // 2. CONSTRUIR EL CONTEXTO EN TIEMPO REAL: Convertimos el inventario a texto plano
                val productContext = currentProducts.joinToString(separator = "\n") { prod ->
                    "- ${prod.nombre}: S/. ${prod.precio} | Categoría: ${prod.categoria} | Descripción: ${prod.descripcion} | Stock disponible: ${prod.stock} unidades."
                }

                // 3. INYECTAR LAS REGLAS DEL SISTEMA (System Prompt)
                val systemInstruction = """
                    Eres un maestro experto en ferretería, construcción y bricolaje que atiende en la ventanilla de atención al cliente. 
                    Tu objetivo es asesorar con amabilidad y guiar técnicamente a las personas sobre qué materiales o herramientas necesitan para sus proyectos domésticos o profesionales.
                    
                    CRÍTICO: Solo puedes recomendar, cotizar o mencionar la existencia de los productos que se encuentran estrictamente en la siguiente lista del inventario real del negocio. Si un cliente te pide algo que no está en esta lista, dile de forma educada que no contamos con stock por el momento pero ofrécele una alternativa parecida de la lista si es viable:
                    
                    $productContext
                    
                    Instrucciones adicionales:
                    - Responde de forma concisa, clara y enfocada en soluciones prácticas.
                    - Habla siempre en Soles Peruanos (S/.).
                    - Sé muy profesional, infunde confianza técnica.
                """.trimIndent()

                // 4. INICIALIZAR EL MODELO CON EL PROMPT DEL SISTEMA
                val generativeModel = GenerativeModel(
                    modelName = "gemini-1.5-flash",
                    apiKey = apiKey,
                    systemInstruction = content { text(systemInstruction) }
                )

                // 5. ENVIAR TODO EL HISTORIAL DE CONVERSACIÓN PARA MANTENER EL HILO
                // Mapeamos los mensajes locales al formato histórico que requiere el SDK de Google
                val history = _messages.value.drop(1).dropLast(1).map { msg ->
                    content(role = if (msg.isUser) "user" else "model") { text(msg.text) }
                }

                val chatSession = generativeModel.startChat(history = history)

                // 6. SOLICITAR RESPUESTA A LA IA
                val response = chatSession.sendMessage(userText)
                val botText = response.text ?: "Disculpa, no logré procesar la recomendación. ¿Podrías repetirme la pregunta?"

                _messages.value = _messages.value + ChatMessage(text = botText, isUser = false)
            } catch (e: Exception) {
                val errorMsg = e.message ?: ""
                val friendlyMessage = when {
                    errorMsg.contains("503") || errorMsg.contains("high demand") ->
                        "El servicio de IA está muy solicitado ahora mismo. Por favor, espera un momento y vuelve a intentarlo."
                    errorMsg.contains("MissingFieldException") ->
                        "Hubo un problema procesando la respuesta de la IA. Reintenta en unos segundos."
                    else -> "Error de conexión con la red de IA: ${e.localizedMessage}. Por favor, verifica tu conexión."
                }
                _messages.value = _messages.value + ChatMessage(text = friendlyMessage, isUser = false)
            } finally {
                _isResponding.value = false
            }
        }
    }
}