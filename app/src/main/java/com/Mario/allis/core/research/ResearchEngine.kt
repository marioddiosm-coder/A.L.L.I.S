package com.Mario.allis.core.research

import com.Mario.allis.core.api.ApiGateway
import com.Mario.allis.core.api.ApiRequest

data class ResearchResult(
    val query: String,
    val summary: String,
    val sources: List<String>
)

class ResearchEngine(private val apiGateway: ApiGateway) {

    fun research(query: String): ResearchResult {
        val request = ApiRequest(endpoint = "https://www.google.com/search?q=$query")
        val result = apiGateway.fetch("web.search", request)
        val summary = if (result.success) {
            "Resumen preliminar para \"$query\" basado en búsqueda autorizada."
        } else {
            "No tengo permiso para buscar en internet. Confirma el acceso."
        }
        return ResearchResult(query, summary, sources = listOf(request.endpoint))
    }
}
