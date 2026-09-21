package com.finanzas.personales.domain.models

import kotlinx.datetime.LocalDate

/**
 * Modelo de dominio que representa un pago pendiente de tarjeta de crédito
 */
data class PaymentDue(
    val card: CreditCard,
    val amount: Double,
    val dueDate: LocalDate,
    val purchases: List<Purchase>
) {
    /**
     * Verifica si el pago está vencido
     */
    fun isOverdue(currentDate: LocalDate): Boolean {
        return currentDate > dueDate
    }

    /**
     * Calcula los días hasta el vencimiento (negativo si está vencido)
     */
    fun daysUntilDue(currentDate: LocalDate): Int {
        return (dueDate.toEpochDays() - currentDate.toEpochDays())
    }

    /**
     * Verifica si el pago está próximo (dentro de los próximos N días)
     */
    fun isUpcoming(currentDate: LocalDate, daysThreshold: Int = 3): Boolean {
        val daysUntil = daysUntilDue(currentDate)
        return daysUntil in 0..daysThreshold
    }
}
