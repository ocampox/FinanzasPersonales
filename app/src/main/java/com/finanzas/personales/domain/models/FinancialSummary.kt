package com.finanzas.personales.domain.models

/**
 * Modelo de dominio que representa un resumen financiero completo
 */
data class FinancialSummary(
    val totalAvailable: Double,
    val totalDebt: Double,
    val realAvailable: Double,
    val totalMinimumPayments: Double,
    val savingsAccounts: List<SavingsAccount>,
    val creditCards: List<CreditCard>
) {
    /**
     * Calcula el total de saldos en cuentas de ahorros
     */
    val totalSavings: Double
        get() = savingsAccounts.sumOf { it.balance }

    /**
     * Calcula el total de cupo usado en tarjetas de crédito
     */
    val totalCreditUsed: Double
        get() = creditCards.sumOf { it.usedLimit }

    /**
     * Calcula el total de cupo disponible en tarjetas de crédito
     */
    val totalCreditAvailable: Double
        get() = creditCards.sumOf { it.availableLimit }

    /**
     * Verifica si el usuario tiene deudas
     */
    val hasDebt: Boolean
        get() = totalDebt > 0

    /**
     * Verifica si el disponible real es positivo
     */
    val hasPositiveRealAvailable: Boolean
        get() = realAvailable > 0
}
