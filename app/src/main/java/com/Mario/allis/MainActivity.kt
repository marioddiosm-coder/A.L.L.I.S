package com.Mario.allis

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.Mario.allis.brain.AllisBrain
import com.Mario.allis.brain.CognitiveState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val brain = AllisBrain()

        setContent {
            var userInput by remember { mutableStateOf("") }
            var messages by remember { mutableStateOf(listOf<Pair<String, Boolean>>()) }
            var showInputField by remember { mutableStateOf(false) }
            var memoryHighlights by remember { mutableStateOf(listOf<String>()) }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp),
                verticalArrangement = Arrangement.Bottom
            ) {
                TopAppBar(
                    title = { Text("ALLIS :: Unidad Cognitiva") },
                    actions = {
                        FilledIconButton(onClick = {}) {
                            Icon(Icons.Filled.Settings, contentDescription = "Configuración")
                        }
                    }
                )

                SphereAnimation(
                    brain = brain,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp)
                        .padding(8.dp)
                )

                StatusPanel(
                    state = brain.getCognitiveState(),
                    speaking = brain.isSpeaking(),
                    recent = brain.hasRecentResponse()
                )

                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    reverseLayout = true
                ) {
                    items(messages.reversed()) { msg ->
                        MessageBubble(msg.first, msg.second)
                    }
                }

                MemoryPanel(memoryHighlights = memoryHighlights)

                Spacer(modifier = Modifier.height(8.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    Button(onClick = { showInputField = !showInputField }) {
                        Text(if (showInputField) "Cerrar teclado" else "Escribir")
                    }
                }

                ControlRow(
                    onMicPressed = {},
                    onSpeedBoost = {},
                    onFocusMode = {}
                )

                if (showInputField) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Start
                    ) {
                        TextField(
                            value = userInput,
                            onValueChange = { userInput = it },
                            label = { Text("Escribe a Allis") },
                            modifier = Modifier.weight(1f)
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        Button(onClick = {
                            if (userInput.isNotBlank()) {
                                val input = userInput
                                userInput = ""
                                messages = messages + Pair(input, true)

                                CoroutineScope(Dispatchers.Default).launch {
                                    val response = brain.processInput(input)
                                    withContext(Dispatchers.Main) {
                                        messages = messages + Pair(response.text, false)
                                        memoryHighlights = memoryHighlights +
                                            "Última intención: ${response.text.take(42)}"
                                    }
                                    brain.autoPromote()
                                    brain.pruneMemory()
                                    brain.debugMemory()
                                }
                            }
                        }) {
                            Text("Enviar")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SphereAnimation(brain: AllisBrain, modifier: Modifier = Modifier) {
    val pulse = rememberInfiniteTransition()
    val scale by pulse.animateFloat(
        initialValue = 1f,
        targetValue = if (brain.hasRecentResponse() || brain.isSpeaking()) 1.4f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    Canvas(modifier = modifier) {
        val centerX = size.width / 2
        val centerY = size.height / 2
        val radius = 50f * scale

        drawCircle(color = Color(0xFF00BCD4), radius = radius, center = Offset(centerX, centerY))
        drawCircle(
            color = Color(0xFF4CAF50).copy(alpha = 0.4f),
            radius = radius * 0.7f,
            center = Offset(centerX, centerY)
        )

        if (brain.hasRecentResponse()) {
            drawCircle(
                color = Color(0xFF81D4FA),
                radius = radius * 0.3f,
                center = Offset(centerX + radius / 2, centerY - radius / 2)
            )
        }

        if (brain.isSpeaking()) {
            drawCircle(
                color = Color(0xFFBBDEFB).copy(alpha = 0.6f),
                radius = radius * 0.5f,
                center = Offset(centerX - radius / 3, centerY + radius / 3)
            )
        }
    }
}

@Composable
fun MessageBubble(text: String, isUser: Boolean) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(4.dp),
        contentAlignment = if (isUser) Alignment.CenterEnd else Alignment.CenterStart
    ) {
        Surface(
            tonalElevation = 2.dp,
            shape = MaterialTheme.shapes.medium
        ) {
            Text(
                text = text,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
            )
        }
    }
}

@Composable
fun StatusPanel(state: CognitiveState, speaking: Boolean, recent: Boolean) {
    val cards = listOf(
        StatusCardData("Estado", state.name, Icons.Filled.Psychology),
        StatusCardData("Voz", if (speaking) "Hablando" else "En silencio", Icons.Filled.Mic),
        StatusCardData("Pulso", if (recent) "Activo" else "Calma", Icons.Filled.Speed)
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        cards.forEach { data ->
            Card(
                modifier = Modifier
                    .weight(1f)
                    .padding(4.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier
                        .padding(12.dp)
                        .wrapContentHeight(),
                    horizontalAlignment = Alignment.Start
                ) {
                    Icon(data.icon, contentDescription = data.title)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(data.title, style = MaterialTheme.typography.labelMedium)
                    Text(data.value, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}

@Composable
fun MemoryPanel(memoryHighlights: List<String>) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 6.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text("Memoria Activa", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(6.dp))
            LinearProgressIndicator(progress = 0.6f, modifier = Modifier.fillMaxWidth())
            Spacer(modifier = Modifier.height(6.dp))
            LazyColumn(modifier = Modifier.height(100.dp)) {
                itemsIndexed(memoryHighlights.takeLast(5)) { index, item ->
                    Text("${index + 1}. $item", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@Composable
fun ControlRow(onMicPressed: () -> Unit, onSpeedBoost: () -> Unit, onFocusMode: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        FilledIconButton(onClick = onMicPressed) {
            Icon(Icons.Filled.Mic, contentDescription = "Escuchar")
        }
        Button(onClick = onSpeedBoost) {
            Text("Impulso")
        }
        Button(onClick = onFocusMode) {
            Text("Foco")
        }
    }
}

data class StatusCardData(
    val title: String,
    val value: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
)
