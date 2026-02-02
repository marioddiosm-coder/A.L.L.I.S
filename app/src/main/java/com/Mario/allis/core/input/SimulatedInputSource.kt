package com.Mario.allis.core.input

class SimulatedInputSource : InputSource {

    override suspend fun getInput(): String? {
        val random = (0..10).random()
        return if (random > 8) "Allis dime algo" else null
    }
}
