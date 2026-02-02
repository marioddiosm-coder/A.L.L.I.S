# Prompt de continuidad para ALLIS (Pre-Alpha v0.4)

## Objetivo general
Construir una IA estilo JARVIS/FRIDAY como unidad de inteligencia autónoma, persistente y modular. Debe escuchar 24/7, reaccionar por intención y contexto, y presentar una interfaz visual tipo esfera (Jarvis) con animaciones reactivas. El sistema está diseñado para evolucionar durante años sin recortar funciones existentes.

## Estado actual del proyecto
- **Núcleo cognitivo**: `AllisBrain` gestiona estado, confirmaciones y flujo de intenciones (chat/acciones/recordatorios/búsquedas/llamadas). Incluye hooks para memoria futura y speaking state.
- **Motor de intención**: `IntentionEngine` puntúa patrones, detecta intención principal, sub-intenciones y aplica boost por contexto.
- **Memoria**: `MemoryStore` con capas fast/short/mid/long, persistencia en memoria, relevancia y priorización de tareas/acciones.
- **Input de voz**: `VoiceInputSource` usa `SpeechRecognizer` para escucha activa.
- **Servicio en foreground**: `VoiceService` mantiene escucha 24/7, usa TTS, y coordina con el cerebro.
- **Interfaz**: `MainActivity` en Compose con esfera animada y chat con input desplegable.
- **Motores base**: `ActionEngine`, `ApiGateway`, `AllisInterface`, `VoiceEngine` listos para integración modular.

## Personalidad de ALLIS
Asistente cercana y confiable, con tono profesional pero cálido. Debe responder con seguridad, pedir confirmaciones cuando hay acciones críticas, y guiar la conversación con empatía.

## Objetivos técnicos
1. **Persistencia real**: memoria a largo plazo con almacenamiento local (Room/SQLite) y cache caliente en RAM.
2. **Rendimiento**: rutas críticas de detección y respuesta optimizadas.
3. **Modularidad**: capas desacopladas (Núcleo > Memoria > Intención > Contexto > UI).
4. **Evolución escalable**: añadir módulos sin romper el núcleo.
5. **Interfaz reactiva**: esfera cambia por estado (speaking/listening/idle) y muestra feedback visual.

## Instrucciones para quien continúe
- **No eliminar funciones ni módulos existentes**.
- **Mantener compatibilidad hacia atrás**.
- **Cada nuevo módulo debe venir completo y funcional**, listo para integrarse.
- **Si algo falta, se añade en paralelo**, no sustituyendo.
- **Documentar cada decisión de arquitectura**.

## Módulos previstos (futuros)
- Orquestador de tareas (colas, scheduling y prioridades).
- Memoria episódica y semántica con embeddings.
- Capa de seguridad y permisos.
- Persistencia de contexto entre sesiones.
- Motor de aprendizaje por refuerzo de hábitos.
- Panel visual de estado cognitivo.

## Interfaz actual (resumen)
- Esfera central con pulsos en azul/verde.
- Brillo adicional cuando habla o responde.
- Historial de mensajes con input desplegable.

## Pregunta de arranque
"¿Qué módulo construimos ahora?"
