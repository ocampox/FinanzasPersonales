package com.finanzas.personales.domain.models

import kotlinx.datetime.Instant

/**
 * Modelo de dominio para una tarjeta de crédito
 */
data class CreditCard(
    val id: Long = 0,
    val name: String,
    val totalLimit: Double,
    val availableLimit: Double,
    val cutoffDay: Int,
    val paymentDay: Int,
    val color: String,
    /**
     * Pago mínimo del corte anterior que el usuario aún no ha registrado como pagado.
     * Se usa solo como referencia en la UI — no se acumula de corte en corte.
     */
    val pendingMinimumPayment: Double = 0.0,
    /** Fecha del último corte procesado. */
    val lastCutoffDate: Instant? = null,
    /**
     * Valor histórico del pago mínimo que generó el corte que acaba de cerrar.
     * Solo informativo: el importe exacto que se debe pagar antes de la fecha de pago.
     * No cambia con las compras nuevas del período siguiente.
     */
    val lastCutoffPayment: Double = 0.0,
    /** Porcentaje del saldo usado aplicado como pago mínimo (0.0–1.0). */
    val minimumPaymentPercentage: Double = 0.0,
    val createdAt: Instant,
    val updatedAt: Instant
) {
    /**
     * Calcula el cupo usado de la tarjeta
     */
    val usedLimit: Double
        get() = totalLimit - availableLimit

    /**
     * Calcula el porcentaje de uso del cupo
     */
    val usagePercentage: Float
        get() = if (totalLimit > 0) {
            (usedLimit / totalLimit * 100).toFloat()
        } else {
            0f
        }

    /**
     * Verifica si hay cupo disponible para un monto específico
     */
    fun hasAvailableCredit(amount: Double): Boolean {
        return availableLimit >= amount
    }
}
