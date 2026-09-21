package com.finanzas.personales.data.models

/**
 * Formato de exportación/importación JSON — versión 2.0
 *
 * Incluye el estado financiero completo:
 * - Tarjetas de crédito con todos los campos del ciclo de corte
 * - Cuentas de ahorros
 * - Compras con cuotas pagadas
 * - Gastos
 * - Ingresos
 * - Historial de pagos a tarjetas
 *
 * Requirements: 9.5
 */
data class BackupData(
    /** Versión del formato. "1.0" = legacy (sin incomes/card_payments). "2.0" = completo. */
    val version: String = "2.0",
    val exportDate: String,
    val creditCards: List<CreditCardBackup>,
    val savingsAccounts: List<SavingsAccountBackup>,
    val purchases: List<PurchaseBackup>,
    val expenses: List<ExpenseBackup>,
    val incomes: List<IncomeBackup> = emptyList(),
    val cardPayments: List<CardPaymentBackup> = emptyList()
)

/** Tarjeta de crédito — estado completo del ciclo de corte */
data class CreditCardBackup(
    val id: Long,
    val name: String,
    val totalLimit: Double,
    val availableLimit: Double,
    val cutoffDay: Int,
    val paymentDay: Int,
    val color: String,
    /** Pago mínimo del corte anterior que aún figura como pendiente */
    val pendingMinimumPayment: Double = 0.0,
    /** Timestamp del último corte procesado (ms) */
    val lastCutoffDate: Long? = null,
    /** Importe histórico del último corte — lo que hay que pagar antes del día de pago */
    val lastCutoffPayment: Double = 0.0,
    /** Porcentaje del saldo usado para calcular el pago mínimo (0.0–1.0) */
    val minimumPaymentPercentage: Double = 0.0,
    val createdAt: Long,
    val updatedAt: Long
)

/** Cuenta de ahorros */
data class SavingsAccountBackup(
    val id: Long,
    val name: String,
    val balance: Double,
    val color: String,
    val createdAt: Long,
    val updatedAt: Long
)

/** Compra con tarjeta — incluye estado de cuotas */
data class PurchaseBackup(
    val id: Long,
    val cardId: Long,
    val description: String,
    val totalAmount: Double,
    val installments: Int,
    val installmentAmount: Double,
    /** Cuotas ya pagadas — crítico para reconstruir el estado correcto */
    val paidInstallments: Int = 0,
    val purchaseDate: Long,
    val createdAt: Long
)

/** Gasto de cuenta de ahorros */
data class ExpenseBackup(
    val id: Long,
    val accountId: Long,
    val description: String,
    val amount: Double,
    val category: String,
    val expenseDate: Long,
    val createdAt: Long
)

/** Ingreso a cuenta de ahorros */
data class IncomeBackup(
    val id: Long,
    val accountId: Long,
    val description: String,
    val amount: Double,
    val category: String,
    val incomeDate: Long,
    val createdAt: Long
)

/** Registro histórico de un abono a tarjeta */
data class CardPaymentBackup(
    val id: Long,
    val cardId: Long,
    val amount: Double,
    val paymentType: String,
    val cutoffDate: Long?,
    val paymentDate: Long,
    val note: String? = null
)
