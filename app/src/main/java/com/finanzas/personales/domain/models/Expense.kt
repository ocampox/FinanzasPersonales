package com.finanzas.personales.domain.models

import com.finanzas.personales.data.local.entities.ExpenseCategory
import kotlinx.datetime.Instant

/**
 * Modelo de dominio para un gasto desde cuenta de ahorros
 */
data class Expense(
    val id: Long = 0,
    val accountId: Long,
    val description: String,
    val amount: Double,
    val category: ExpenseCategory,
    val expenseDate: Instant
)
