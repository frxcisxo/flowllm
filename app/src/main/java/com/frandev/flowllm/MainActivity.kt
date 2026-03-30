package com.frandev.flowllm

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.frandev.flowllm.compose.rememberLLMStream
import com.frandev.flowllm.providers.*
import com.frandev.flowllm.providers.model.GeminiModel
import com.frandev.flowllm.retry.RetryPolicy

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // setContent reemplaza los XMLs de toda la vida
        // a partir de aquí todo es Compose
        setContent {
            // MaterialTheme aplica colores, tipografía y formas
            // a todos los composables dentro de él
            MaterialTheme {
                DemoScreen()
            }
        }
    }
}

@Composable
fun DemoScreen() {
    // Lista de providers disponibles para mostrar en los chips
    val providers = listOf("Gemini", "OpenAI", "Anthropic", "Ollama")

    // remember = sobrevive recomposiciones
    // mutableStateOf = cuando cambia, Compose redibuja automáticamente
    var selectedProvider by remember { mutableStateOf("Gemini") }
    var apiKey by remember { mutableStateOf("") }
    var prompt by remember { mutableStateOf("") }

    // submitted es diferente a prompt — solo cambia cuando el usuario
    // presiona Enviar, evitando llamadas innecesarias a la API
    var submitted by remember { mutableStateOf("") }

    // remember(selectedProvider, apiKey) recrea el provider
    // solo cuando cambia el provider seleccionado o la API key
    val provider = remember(selectedProvider, apiKey) {
        when (selectedProvider) {
            "OpenAI" -> OpenAIProvider(apiKey)
            "Anthropic" -> AnthropicProvider(apiKey)
            "Ollama" -> OllamaProvider()
            else -> GeminiProvider(apiKey, GeminiModel.PRO_2_5)
        }
    }

    // Aquí usamos nuestra librería — retorna el estado actual del stream
    val state = rememberLLMStream(
        provider = provider,
        prompt = submitted,
        retryPolicy = RetryPolicy(maxRetries = 3, initialDelayMs = 500)
    )


    val scrollState = rememberScrollState()

    // Column = equivalente a LinearLayout vertical en XML
    Column(
        modifier = Modifier // ocupa toda la pantalla
            .fillMaxSize()
            .verticalScroll(scrollState) // ← agrega esto
            .padding(46.dp),      // margen interno de 16dp
        verticalArrangement = Arrangement.spacedBy(12.dp) // espacio entre hijos
    ) {

        Spacer(Modifier.padding(16.dp))
        // Texto con estilo predefinido de Material3
        Text("FlowLLM", style = MaterialTheme.typography.headlineMedium)

        // Row = LinearLayout horizontal
        // Aquí mostramos los chips de selección de provider
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            providers.forEach { name ->
                // FilterChip = chip seleccionable tipo toggle
                FilterChip(
                    selected = selectedProvider == name,
                    onClick = { selectedProvider = name },
                    label = { Text(name) }
                )
            }
        }

        // Ocultamos el campo de API key para Ollama
        // porque corre local y no necesita autenticación
        if (selectedProvider != "Ollama") {
            OutlinedTextField(
                value = apiKey,
                onValueChange = { apiKey = it }, // actualiza el estado en cada tecla
                label = { Text("API Key") },
                modifier = Modifier.fillMaxWidth()
            )
        }

        OutlinedTextField(
            value = prompt,
            onValueChange = { prompt = it },
            label = { Text("Escribe tu prompt") },
            modifier = Modifier.fillMaxWidth()
        )

        // Al presionar Enviar, copiamos prompt a submitted
        // esto dispara rememberLLMStream via LaunchedEffect
        Button(
            onClick = { submitted = prompt },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Enviar")
        }

        // when en Kotlin = switch pero más poderoso
        // Compose redibuja este bloque cada vez que cambia state
        when (val s = state) {
            is LLMState.Idle ->
                Text("👋 Selecciona un provider y escribe un prompt")
            is LLMState.Loading ->
                CircularProgressIndicator() // spinner mientras conecta
            is LLMState.Streaming ->
                // s.text se actualiza chunk por chunk en tiempo real
                Text(s.text)
            is LLMState.Done ->
                Text("✅ ${s.text}")
            is LLMState.Error ->
                Text("❌ ${s.exception.message}")
        }
    }
}