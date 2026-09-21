package com.finanzas.personales.domain.models

import kotlinx.datetime.*
import kotlin.math.max
import kotlin.math.min

/**
 * Modelo de dominio para una compra con tarjeta de crédito
 */
data class Purchase(
    val id: Long = 0,
    val cardId: Long,
    val description: String,
    val totalAmount: Double,
    val installments: Int,
    val installmentAmount: Double,
    val paidInstallments: Int = 0,
    val purchaseDate: Instant
) {
    /**
     * Calcula el número de cuotas restantes
     *
     * Requirements: 4.1
     */
    fun getRemainingInstallments(): Int {
        return max(0, installments - paidInstallments)
    }

    /**
     * Calcula el monto total pendiente de pago
     */
    fun getRemainingAmount(): Double {
        return getRemainingInstallments() * installmentAmount
    }

    /**
     * Verifica si la compra ya está completamente pagada
     */
    fun isFullyPaid(): Boolean {
        return getRemainingInstallments() == 0
    }

    /**
     * Calcula en qué corte vence una cuota específica
     * 
     * @param installmentNumber Número de la cuota (1 a N)
     * @param cutoffDay Día del mes en que ocurre el corte
     * @return Fecha del corte en que vence esta cuota
     * 
     * Requirements: 4.2, 4.3
     */
    fun getInstallmentDueDate(installmentNumber: Int, cutoffDay: Int): LocalDate {
        require(installmentNumber in 1..installments) {
            "Número de cuota inválido: $installmentNumber (debe estar entre 1 y $installments)"
        }
        
        val purchaseLocalDate = purchaseDate.toLocalDateTime(TimeZone.currentSystemDefault()).date
        
        // REGLA CORRECTA: Para efectos de corte se usa la fecha de compra
        // Simplificado para mayor consistencia con datos reales
        val processDate = getProcessDate(purchaseLocalDate)
        
        // Determinar en qué mes vence la primera cuota basándose en la fecha de proceso
        val firstInstallmentMonth: Int
        val firstInstallmentYear: Int
        
        if (processDate.dayOfMonth < cutoffDay) {
            // Compra procesada antes del día de corte: va al corte del mismo mes
            firstInstallmentMonth = processDate.monthNumber
            firstInstallmentYear = processDate.year
        } else {
            // Compra procesada en el día de corte o después: va al siguiente corte
            firstInstallmentMonth = if (processDate.monthNumber == 12) 1 else processDate.monthNumber + 1
            firstInstallmentYear = if (processDate.monthNumber == 12) processDate.year + 1 else processDate.year
        }
        
        // Calcular el mes y año del corte para esta cuota
        // La cuota N vence N-1 meses después de la primera cuota
        val monthsToAdd = installmentNumber - 1
        var dueMonth = firstInstallmentMonth + monthsToAdd
        var dueYear = firstInstallmentYear
        
        // Ajustar año si el mes excede 12
        while (dueMonth > 12) {
            dueMonth -= 12
            dueYear += 1
        }
        
        // Crear la fecha de corte
        val daysInMonth = Month(dueMonth).length(isLeapYear(dueYear))
        val actualCutoffDay = min(cutoffDay, daysInMonth)
        
        return LocalDate(dueYear, dueMonth, actualCutoffDay)
    }

    /**
     * Calcula la fecha de proceso de la compra
     * Basado en el análisis de datos reales: las compras se procesan el mismo día
     * independientemente del día de la semana para efectos de corte.
     * 
     * @param purchaseDate Fecha original de la compra
     * @return Fecha de proceso para efectos de corte (misma fecha)
     */
    private fun getProcessDate(purchaseDate: LocalDate): LocalDate {
        // Simplificado: usar la misma fecha de compra para efectos de corte
        // Esto es más consistente con los datos reales observados
        return purchaseDate
    }

    /**
     * Obtiene todas las cuotas que vencen en un corte específico
     *
     * Compara solo año y mes (no el día exacto) para tolerar ajustes cuando el día
     * de corte configurado supera los días del mes (ej. día 31 en febrero).
     *
     * @param cutoffDate Fecha del corte
     * @param cutoffDay Día del mes en que ocurre el corte
     * @return Lista de números de cuota que vencen en este corte
     *
     * Requirements: 4.3
     */
    fun getInstallmentsDueInCutoff(cutoffDate: LocalDate, cutoffDay: Int): List<Int> {
        val dueInstallments = mutableListOf<Int>()
        for (i in 1..installments) {
            val installmentDueDate = getInstallmentDueDate(i, cutoffDay)
            if (installmentDueDate.year == cutoffDate.year &&
                installmentDueDate.monthNumber == cutoffDate.monthNumber) {
                dueInstallments.add(i)
            }
        }
        return dueInstallments
    }

    /**
     * Calcula el detalle de todas las cuotas de esta compra
     * 
     * @param cutoffDay Día del mes en que ocurre el corte
     * @return Lista de detalles de cada cuota
     * 
     * Requirements: 4.2, 4.5
     */
    fun getInstallmentDetails(cutoffDay: Int): List<InstallmentDetail> {
        return (1..installments).map { installmentNumber ->
            InstallmentDetail(
                purchase = this,
                installmentNumber = installmentNumber,
                installmentAmount = installmentAmount,
                dueDate = getInstallmentDueDate(installmentNumber, cutoffDay)
            )
        }
    }

    /**
     * Calcula los meses entre dos fechas
     */
    private fun calculateMonthsBetween(start: LocalDate, end: LocalDate): Int {
        val yearsDiff = end.year - start.year
        val monthsDiff = end.monthNumber - start.monthNumber
        return yearsDiff * 12 + monthsDiff
    }

    /**
     * Verifica si un año es bisiesto
     */
    private fun isLeapYear(year: Int): Boolean {
        return (year % 4 == 0 && year % 100 != 0) || (year % 400 == 0)
    }
}
