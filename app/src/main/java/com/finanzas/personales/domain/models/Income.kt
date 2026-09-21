package com.finanzas.personales.domain.models

import kotlinx.datetime.Instant

/**
 * Modelo de dominio para un ingreso a cuenta de ahorros
 */
data class Income(
    val id: Long = 0,
    val accountId: Long,
    val description: String,
    val amount: Double,
    val category: IncomeCategory,
    val incomeDate: Instant
)

/**
 * Categorías de ingresos
 */
enum class IncomeCategory {
    SALARY,      // Salario
    BONUS,       // Bonificación
    FREELANCE,   // Trabajo independiente
    INVESTMENT,  // Inversiones
    GIFT,        // Regalo/Donación
    REFUND,      // Reembolso
    SALE,        // Venta
    OTHER        // Otros
}