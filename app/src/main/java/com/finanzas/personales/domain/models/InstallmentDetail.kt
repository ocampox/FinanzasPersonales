package com.finanzas.personales.domain.models

import kotlinx.datetime.LocalDate

/**
 * Representa el detalle de una cuota específica de una compra
 * 
 * Este modelo se usa para desglosar las cuotas que vencen en un corte específico,
 * permitiendo mostrar información detallada sobre qué compras componen el pago.
 * 
 * Requirements: 4.5
 */
data class InstallmentDetail(
    val purchase: Purchase,
    val installmentNumber: Int, // Número de la cuota (1 a N)
    val installmentAmount: Double,
    val dueDate: LocalDate // Fecha en que vence esta cuota
)

/**
 * Representa el desglose completo de pagos para un corte específico
 * 
 * Requirements: 4.5
 */
data class CutoffPaymentBreakdown(
    val cardId: Long,
    val cutoffDate: LocalDate,
    val installments: List<InstallmentDetail>,
    val totalAmount: Double
) {
    /**
     * Agrupa las cuotas por compra para mostrar un resumen
     */
    fun groupByPurchase(): Map<Purchase, List<InstallmentDetail>> {
        return installments.groupBy { it.purchase }
    }
    
    /**
     * Obtiene el número total de cuotas que vencen en este corte
     */
    val totalInstallments: Int
        get() = installments.size
}
