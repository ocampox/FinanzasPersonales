package com.finanzas.personales.domain.models

/**
 * Sealed class para representar diferentes tipos de errores en la aplicación
 */
sealed class FinanceError {
    /**
     * Error cuando no hay fondos suficientes en una cuenta de ahorros
     */
    data class InsufficientFunds(
        val available: Double,
        val required: Double
    ) : FinanceError() {
        val shortfall: Double
            get() = required - available
    }

    /**
     * Error cuando no hay cupo suficiente en una tarjeta de crédito
     */
    data class InsufficientCredit(
        val available: Double,
        val required: Double
    ) : FinanceError() {
        val shortfall: Double
            get() = required - available
    }

    /**
     * Error cuando un monto es inválido (negativo, cero, etc.)
     */
    data class InvalidAmount(val message: String) : FinanceError()

    /**
     * Error de base de datos
     */
    data class DatabaseError(val message: String) : FinanceError()

    /**
     * Error cuando no se encuentra una entidad
     */
    data class NotFound(val entityType: String, val id: Long) : FinanceError()

    /**
     * Error de validación general
     */
    data class ValidationError(val message: String) : FinanceError()
}
