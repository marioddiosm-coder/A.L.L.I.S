package com.Mario.allis.voice

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.speech.tts.TextToSpeech
import android.util.Log
import androidx.core.app.NotificationCompat
import com.Mario.allis.R
import com.Mario.allis.brain.AllisBrain
import com.Mario.allis.brain.CognitiveState
import com.Mario.allis.core.input.InputSource
import com.Mario.allis.core.input.SimulatedInputSource
import com.Mario.allis.core.input.VoiceInputSource
import kotlinx.coroutines.*
import java.util.Locale

/**
 * VoiceService v0.4
 * =================
 * Funcionalidades:
 * - Escucha activa 24/7 con voz real (SpeechRecognizer)
 * - Fallback a input simulado si micrófono no disponible
 * - Responde con TTS
 * - Maneja estados cognitivos y confirmaciones
 * - Logs detallados
 * - Preparado para reacciones visuales (esfera)
 */
class VoiceService : Service() {

    private val TAG = "VoiceService"
    private var serviceJob: Job? = null

    // --------------------------
    // Cerebro central
    // --------------------------
    private val brain = AllisBrain()

    // --------------------------
    // Fuente de input
    // --------------------------
    private val inputSource: InputSource by lazy {
        try {
            VoiceInputSource(this)
        } catch (e: Exception) {
            Log.w(TAG, "Micrófono no disponible, usando input simulado")
            SimulatedInputSource()
        }
    }

    // --------------------------
    // TTS
    // --------------------------
    private lateinit var tts: TextToSpeech

    // --------------------------
    // Estado del servicio
    // --------------------------
    private var isActive = true
    private val channelId = "allis_voice_service"

    override fun onBind(intent: Intent?): IBinder? = null

    // --------------------------
    // Inicialización
    // --------------------------
    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(1, buildForegroundNotification())
        Log.d(TAG, "VoiceService iniciado en foreground")

        // Inicializar TTS
        tts = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts.language = Locale("es", "ES")
                tts.setPitch(1.0f)
                tts.setSpeechRate(1.0f)
                Log.d(TAG, "TTS inicializado correctamente")
            } else {
                Log.e(TAG, "Error inicializando TTS")
            }
        }

        // Arrancar loop de escucha
        startListeningLoop()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    // --------------------------
    // Loop principal de escucha
    // --------------------------
    private fun startListeningLoop() {
        serviceJob = CoroutineScope(Dispatchers.Default).launch {
            while (isActive) {
                try {
                    // 🔹 Obtener input (voz o simulado)
                    val input = inputSource.getInput()

                    // 🔹 Ejecutar mantenimiento aunque no haya input
                    brain.autoPromote()
                    brain.pruneMemory()

                    // 🔹 Procesar input si existe
                    if (!input.isNullOrBlank()) {
                        val state = brain.getCognitiveState()

                        if (state == CognitiveState.WAITING_CONFIRMATION ||
                            state == CognitiveState.EXECUTING
                        ) {
                            Log.d(TAG, "ALLIS ocupado ($state), input ignorado: $input")
                        } else {
                            Log.d(TAG, "Input detectado: $input")
                            val response = brain.processInput(input)
                            Log.d(TAG, "ALLIS responde: ${response.text}")

                            // 🔹 Indicar que Allis empieza a hablar
                            brain.setSpeaking(true)

                            // 🔹 Hablar respuesta con TTS
                            if (::tts.isInitialized) {
                                tts.speak(response.text, TextToSpeech.QUEUE_ADD, null, "ALLIS_TTS")
                            }

                            // 🔹 Estimar fin de habla (ajustable)
                            CoroutineScope(Dispatchers.Default).launch {
                                delay((response.text.length * 60).toLong())
                                brain.setSpeaking(false)
                            }
                        }
                    }

                    delay(500)
                } catch (e: Exception) {
                    Log.e(TAG, "Error en VoiceService: ${e.message}")
                    delay(1000)
                }
            }
        }
    }

    // --------------------------
    // Activar / desactivar escucha
    // --------------------------
    fun setActive(active: Boolean) {
        isActive = active
        Log.d(TAG, "VoiceService activo = $isActive")
    }

    // --------------------------
    // Canal de notificación
    // --------------------------
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Allis Voice Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply { description = "Servicio de voz activo 24/7" }

            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private fun buildForegroundNotification(): Notification {
        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("Allis escuchando")
            .setContentText("Servicio de voz activo 24/7")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setOngoing(true)
            .build()
    }

    // --------------------------
    // Destrucción del servicio
    // --------------------------
    override fun onDestroy() {
        super.onDestroy()
        serviceJob?.cancel()
        if (::tts.isInitialized) {
            tts.stop()
            tts.shutdown()
        }
        Log.d(TAG, "VoiceService detenido")
    }

    // --------------------------
    // FUNCIONES FUTURAS / EXTENSIONES
    // --------------------------
    /**
     * Aquí se pueden añadir:
     * - Interfaz de esfera con animaciones
     * - Reacciones visuales a voz o emociones
     * - Logging avanzado por tipo de input
     * - Guardado de audio histórico para análisis
     */
}
