package com.finanzas.personales.data.repository

import com.finanzas.personales.domain.models.*
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalDate

/**
 * Interfaz del repositorio principal para operaciones financieras.
 */
interface FinanceRepository {

    // ==================== Credit Cards ====================

    fun getAllCreditCards(): Flow<List<CreditCard>>
    suspend fun getCreditCardById(id: Long): Result<CreditCard>
    suspend fun addCreditCard(card: CreditCard): Result<Long>
    suspend fun updateCreditCard(card: CreditCard): Result<Unit>
    suspend fun deleteCreditCard(card: CreditCard): Result<Unit>

    /**
     * Registra un abono realizado a la tarjeta.
     * Aumenta el cupo disponible por el importe indicado y aplica el monto
     * a las cuotas pendientes priorizando las más antiguas por fecha de corte.
     */
    suspend fun addPayment(cardId: Long, amount: Double): Result<Unit>

    // ==================== Savings Accounts ====================

    fun getAllSavingsAccounts(): Flow<List<SavingsAccount>>
    fun getSavingsAccountByIdFlow(id: Long): Flow<SavingsAccount?>
    suspend fun getSavingsAccountById(id: Long): Result<SavingsAccount>
    suspend fun addSavingsAccount(account: SavingsAccount): Result<Long>
    suspend fun updateSavingsAccount(account: SavingsAccount): Result<Unit>
    suspend fun deleteSavingsAccount(account: SavingsAccount): Result<Unit>

    // ==================== Purchases ====================

    fun getPurchasesByCard(cardId: Long): Flow<List<Purchase>>
    fun getAllPurchases(): Flow<List<Purchase>>
    suspend fun addPurchase(purchase: Purchase): Result<Long>
    suspend fun updatePurchase(purchase: Purchase): Result<Unit>
    suspend fun deletePurchase(purchase: Purchase): Result<Unit>

    /**
     * Herramienta de normalización excepcional: avanza paidInstallments
     * para cuotas que ya vencieron según el historial de pagos de cada tarjeta.
     * @return número de compras corregidas
     */
    suspend fun repairInstallments(): Result<Int>

    // ==================== Expenses ====================

    fun getExpensesByAccount(accountId: Long): Flow<List<Expense>>
    fun getAllExpenses(): Flow<List<Expense>>
    suspend fun addExpense(expense: Expense): Result<Long>
    suspend fun updateExpense(expense: Expense): Result<Unit>
    suspend fun deleteExpense(expense: Expense): Result<Unit>

    // ==================== Incomes ====================

    fun getIncomesByAccount(accountId: Long): Flow<List<Income>>
    fun getAllIncomes(): Flow<List<Income>>
    suspend fun addIncome(income: Income): Result<Long>
    suspend fun updateIncome(income: Income): Result<Unit>
    suspend fun deleteIncome(income: Income): Result<Unit>

    // ==================== Financial Calculations ====================

    suspend fun calculateTotalAvailable(): Result<Double>
    suspend fun calculateTotalDebt(): Result<Double>
    suspend fun calculateRealAvailable(): Result<Double>
    suspend fun calculatePaymentForCutoff(cardId: Long, cutoffDate: LocalDate): Result<Double>
    suspend fun getPaymentBreakdownForCutoff(cardId: Long, cutoffDate: LocalDate): Result<CutoffPaymentBreakdown>

    /**
     * Proyección del pago mínimo para un corte: suma de cuotas pendientes que
     * vencen en ese período. Si minimumPaymentPercentage > 0, se toma el mayor
     * entre esa proyección y el porcentaje del saldo usado.
     */
    suspend fun calculateMinimumPayment(cardId: Long, cutoffDate: LocalDate): Result<Double>

    /**
     * Suma las proyecciones de pago mínimo del próximo corte de todas las tarjetas.
     */
    suspend fun calculateTotalMinimumPayments(): Result<Double>

    // ==================== Card Payments (historial) ====================

    fun getPaymentsByCard(cardId: Long): Flow<List<CardPayment>>
    fun getAllCardPayments(): Flow<List<CardPayment>>

    /**
     * Inserta un registro de pago histórico sin modificar el estado de la tarjeta.
     * Usado exclusivamente durante la restauración de backups.
     */
    suspend fun insertCardPaymentRecord(payment: CardPayment): Result<Long>

    /**
     * Herramienta de normalización excepcional: corrige paidInstallments y
     * recalcula la proyección del próximo corte para una tarjeta.
     */
    suspend fun recalculateNextCutoffPayment(cardId: Long): Result<Double>
}
