package com.Mario.allis.core.api

/**
 * ApiGateway
 * Acceso modular a Internet y APIs con control de permisos y logging.
 */
class ApiGateway {

    private val permissionLedger = mutableMapOf<String, Boolean>()

    fun grantPermission(scope: String) {
        permissionLedger[scope] = true
    }

    fun revokePermission(scope: String) {
        permissionLedger[scope] = false
    }

    fun hasPermission(scope: String): Boolean {
        return permissionLedger[scope] == true
    }

    fun fetch(scope: String, request: ApiRequest): ApiResult {
        if (!hasPermission(scope)) {
            return ApiResult(
                success = false,
                payload = null,
                error = "Permiso denegado para $scope"
            )
        }
        return ApiResult(success = true, payload = "FETCH:${request.endpoint}")
    }
}

data class ApiRequest(
    val endpoint: String,
    val method: String = "GET",
    val body: String? = null
)

data class ApiResult(
    val success: Boolean,
    val payload: String?,
    val error: String? = null
)
