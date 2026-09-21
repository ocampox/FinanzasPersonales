package com.finanzas.personales.domain.models

import kotlinx.datetime.Instant

/**
 * Modelo de dominio para una cuenta de ahorros
 */
data class SavingsAccount(
    val id: Long = 0,
    val name: String,
    val balance: Double,
    val color: String,
    val createdAt: Instant,
    val updatedAt: Instant
) {
    /**
     * Verifica si hay saldo disponible para un monto específico
     */
    fun hasAvailableBalance(amount: Double): Boolean {
        return balance >= amount
    }

    /**
     * Verifica si la cuenta tiene saldo positivo
     */
    val hasPositiveBalance: Boolean
        get() = balance > 0
}
