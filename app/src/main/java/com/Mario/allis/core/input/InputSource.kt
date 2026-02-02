package com.Mario.allis.core.input

interface InputSource {
    suspend fun getInput(): String?
}
