package com.finanzas.personales.data.repository

import com.finanzas.personales.data.local.dao.CardPaymentDao
import com.finanzas.personales.data.local.dao.CreditCardDao
import com.finanzas.personales.data.local.dao.ExpenseDao
import com.finanzas.personales.data.local.dao.IncomeDao
import com.finanzas.personales.data.local.dao.PurchaseDao
import com.finanzas.personales.data.local.dao.SavingsAccountDao
import androidx.room.withTransaction
import com.finanzas.personales.domain.models.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.datetime.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementación del repositorio de finanzas
 */
@Singleton
class FinanceRepositoryImpl @Inject constructor(
    private val creditCardDao: CreditCardDao,
    private val savingsAccountDao: SavingsAccountDao,
    private val purchaseDao: PurchaseDao,
    private val expenseDao: ExpenseDao,
    private val incomeDao: IncomeDao,
    private val cardPaymentDao: CardPaymentDao,
    private val database: com.finanzas.personales.data.local.database.FinanceDatabase
) : FinanceRepository {

    // ==================== Credit Cards ====================

    override fun getAllCreditCards(): Flow<List<CreditCard>> {
        return creditCardDao.getAllCards().map { entities ->
            entities.toDomain()
        }
    }

    override suspend fun getCreditCardById(id: Long): Result<CreditCard> {
        return try {
            val entity = creditCardDao.getCardById(id)
            if (entity != null) {
                var card = entity.toDomain()
                
                // Validación: si no hay último corte procesado, pendingMinimumPayment debe ser 0
                // Esto corrige datos incorrectos que puedan existir
                if (card.lastCutoffDate == null && card.pendingMinimumPayment > 0) {
                    // Limpiar el pendingMinimumPayment incorrecto
                    creditCardDao.updatePendingMinimumPayment(
                        cardId = card.id,
                        pendingPayment = 0.0,
                        updatedAt = System.currentTimeMillis()
                    )
                    // Actualizar el objeto card para reflejar el cambio
                    card = card.copy(pendingMinimumPayment = 0.0)
                }
                
                Result.Success(card)
            } else {
                Result.Error(FinanceError.NotFound("CreditCard", id))
            }
        } catch (e: Exception) {
            Result.Error(FinanceError.DatabaseError(e.message ?: "Error al obtener tarjeta"))
        }
    }

    override suspend fun addCreditCard(card: CreditCard): Result<Long> {
        return try {
            // Validar que el cupo total sea positivo
            if (card.totalLimit <= 0) {
                return Result.Error(FinanceError.InvalidAmount("El cupo total debe ser mayor a cero"))
            }
            
            // Validar que el cupo disponible no sea mayor al total
            if (card.availableLimit > card.totalLimit) {
                return Result.Error(FinanceError.InvalidAmount("El cupo disponible no puede ser mayor al cupo total"))
            }
            
            val id = creditCardDao.insertCard(card.toEntity())
            Result.Success(id)
        } catch (e: Exception) {
            Result.Error(FinanceError.DatabaseError(e.message ?: "Error al agregar tarjeta"))
        }
    }

    override suspend fun updateCreditCard(card: CreditCard): Result<Unit> {
        return try {
            // Validar que el cupo total sea positivo
            if (card.totalLimit <= 0) {
                return Result.Error(FinanceError.InvalidAmount("El cupo total debe ser mayor a cero"))
            }
            
            // Validar que el cupo disponible no sea mayor al total
            if (card.availableLimit > card.totalLimit) {
                return Result.Error(FinanceError.InvalidAmount("El cupo disponible no puede ser mayor al cupo total"))
            }
            
            creditCardDao.updateCard(card.toEntity())
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(FinanceError.DatabaseError(e.message ?: "Error al actualizar tarjeta"))
        }
    }

    override suspend fun deleteCreditCard(card: CreditCard): Result<Unit> {
        return try {
            creditCardDao.deleteCard(card.toEntity())
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(FinanceError.DatabaseError(e.message ?: "Error al eliminar tarjeta"))
        }
    }

    override suspend fun addPayment(cardId: Long, amount: Double): Result<Unit> {
        return try {
            if (amount <= 0) {
                return Result.Error(FinanceError.InvalidAmount("El monto del abono debe ser mayor a cero"))
            }

            val cardEntity = creditCardDao.getCardById(cardId)
                ?: return Result.Error(FinanceError.NotFound("CreditCard", cardId))

            val card = cardEntity.toDomain()
            val now = Clock.System.now()
            val currentDate = now.toLocalDateTime(TimeZone.currentSystemDefault()).date
            val updatedAt = System.currentTimeMillis()

            val usedLimit = card.usedLimit.coerceAtLeast(0.0)
            if (amount > usedLimit) {
                return Result.Error(FinanceError.InvalidAmount("El abono no puede superar la deuda registrada"))
            }

            val newAvailableLimit = (card.availableLimit + amount).coerceAtMost(card.totalLimit)
            database.withTransaction {
                val purchaseUpdates = calculateInstallmentsToPay(cardId, card, amount)
                creditCardDao.updateAvailableLimit(card.id, newAvailableLimit, updatedAt)
                if (purchaseUpdates.isNotEmpty()) {
                    purchaseDao.updateMultiplePaidInstallments(purchaseUpdates)
                }
                cardPaymentDao.insertPayment(
                    CardPayment(
                        cardId = cardId,
                        amount = amount,
                        paymentType = PaymentType.CAPITAL,
                        cutoffDate = null,
                        paymentDate = now,
                        note = "Abono registrado"
                    ).toEntity()
                )
            }

            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(FinanceError.DatabaseError(e.message ?: "Error al agregar abono"))
        }
    }

    /**
     * Calcula qué cuotas pueden marcarse como pagadas dado un monto disponible.
     *
     * Recorre todas las cuotas pendientes, desde la más antigua, y las va "comprando"
     * con el monto disponible. Solo se marca como pagada una cuota si el monto disponible
     * alcanza a cubrir su valor completo.
     *
     * @param cardId      ID de la tarjeta
     * @param card        Datos de la tarjeta (para obtener cutoffDay)
     * @param amount      Monto disponible para pagar cuotas
     * @return Mapa de purchaseId → nuevo valor de paidInstallments
     */
    private suspend fun calculateInstallmentsToPay(
        cardId: Long,
        card: CreditCard,
        amount: Double
    ): Map<Long, Int> {
        val allPurchases = purchaseDao.getPurchasesByCard(cardId).first().map { it.toDomain() }

        // Recopilar todas las cuotas pendientes y priorizar las más antiguas.
        data class InstallmentToPay(
            val purchase: Purchase,
            val installmentNumber: Int,
            val installmentAmount: Double,
            val dueDate: LocalDate
        )
        val installmentsToPay = mutableListOf<InstallmentToPay>()

        for (purchase in allPurchases) {
            for (installmentNumber in (purchase.paidInstallments + 1)..purchase.installments) {
                installmentsToPay.add(
                    InstallmentToPay(
                        purchase = purchase,
                        installmentNumber = installmentNumber,
                        installmentAmount = purchase.installmentAmount,
                        dueDate = purchase.getInstallmentDueDate(installmentNumber, card.cutoffDay)
                    )
                )
            }
        }

        installmentsToPay.sortWith(compareBy({ it.dueDate }, { it.purchase.id }, { it.installmentNumber }))

        // Ir "comprando" cuotas con el monto disponible
        var remaining = amount
        val purchaseMaxPaid = mutableMapOf<Long, Int>() // purchaseId → mayor cuota pagada en esta operación

        for (item in installmentsToPay) {
            if (remaining < item.installmentAmount) break // No alcanza para esta cuota
            remaining -= item.installmentAmount
            val currentMax = purchaseMaxPaid[item.purchase.id] ?: item.purchase.paidInstallments
            purchaseMaxPaid[item.purchase.id] = maxOf(currentMax, item.installmentNumber)
        }

        return purchaseMaxPaid
    }

    // ==================== Savings Accounts ====================

    override fun getAllSavingsAccounts(): Flow<List<SavingsAccount>> {
        return savingsAccountDao.getAllAccounts().map { entities ->
            entities.toSavingsAccountDomain()
        }
    }

    override fun getSavingsAccountByIdFlow(id: Long): Flow<SavingsAccount?> {
        return savingsAccountDao.getAccountByIdFlow(id).map { entity ->
            entity?.toDomain()
        }
    }

    override suspend fun getSavingsAccountById(id: Long): Result<SavingsAccount> {
        return try {
            val entity = savingsAccountDao.getAccountById(id)
            if (entity != null) {
                Result.Success(entity.toDomain())
            } else {
                Result.Error(FinanceError.NotFound("SavingsAccount", id))
            }
        } catch (e: Exception) {
            Result.Error(FinanceError.DatabaseError(e.message ?: "Error al obtener cuenta"))
        }
    }

    override suspend fun addSavingsAccount(account: SavingsAccount): Result<Long> {
        return try {
            val id = savingsAccountDao.insertAccount(account.toEntity())
            Result.Success(id)
        } catch (e: Exception) {
            Result.Error(FinanceError.DatabaseError(e.message ?: "Error al agregar cuenta"))
        }
    }

    override suspend fun updateSavingsAccount(account: SavingsAccount): Result<Unit> {
        return try {
            savingsAccountDao.updateAccount(account.toEntity())
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(FinanceError.DatabaseError(e.message ?: "Error al actualizar cuenta"))
        }
    }

    override suspend fun deleteSavingsAccount(account: SavingsAccount): Result<Unit> {
        return try {
            savingsAccountDao.deleteAccount(account.toEntity())
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(FinanceError.DatabaseError(e.message ?: "Error al eliminar cuenta"))
        }
    }

    // ==================== Purchases ====================

    override fun getPurchasesByCard(cardId: Long): Flow<List<Purchase>> {
        return purchaseDao.getPurchasesByCard(cardId).map { entities ->
            entities.toPurchaseDomain()
        }
    }

    override fun getAllPurchases(): Flow<List<Purchase>> {
        return purchaseDao.getAllPurchases().map { entities ->
            entities.toPurchaseDomain()
        }
    }

    override suspend fun addPurchase(purchase: Purchase): Result<Long> {
        return try {
            // Validar que el monto sea positivo
            if (purchase.totalAmount <= 0) {
                return Result.Error(FinanceError.InvalidAmount("El monto debe ser mayor a cero"))
            }
            
            // Validar que el número de cuotas sea válido
            if (purchase.installments < 1 || purchase.installments > 36) {
                return Result.Error(FinanceError.ValidationError("El número de cuotas debe estar entre 1 y 36"))
            }
            
            // Obtener la tarjeta
            val card = creditCardDao.getCardById(purchase.cardId)
                ?: return Result.Error(FinanceError.NotFound("CreditCard", purchase.cardId))
            
            // Verificar que haya cupo disponible
            if (card.availableLimit < purchase.totalAmount) {
                return Result.Error(
                    FinanceError.InsufficientCredit(
                        available = card.availableLimit,
                        required = purchase.totalAmount
                    )
                )
            }
            
            // Actualizar el cupo disponible de la tarjeta
            val newAvailableLimit = card.availableLimit - purchase.totalAmount
            val updatedAt = System.currentTimeMillis()
            
            // Transacción: insertar compra y actualizar cupo en una sola operación
            val purchaseId = database.withTransaction {
                val id = purchaseDao.insertPurchase(purchase.toEntity())
                creditCardDao.updateAvailableLimit(
                    cardId = card.id,
                    newLimit = newAvailableLimit,
                    updatedAt = updatedAt
                )
                id
            }
            
            Result.Success(purchaseId)
        } catch (e: Exception) {
            Result.Error(FinanceError.DatabaseError(e.message ?: "Error al agregar compra"))
        }
    }

    override suspend fun updatePurchase(purchase: Purchase): Result<Unit> {
        return try {
            // Validar que el monto sea positivo
            if (purchase.totalAmount <= 0) {
                return Result.Error(FinanceError.InvalidAmount("El monto debe ser mayor a cero"))
            }
            
            // Validar que el número de cuotas sea válido
            if (purchase.installments < 1 || purchase.installments > 36) {
                return Result.Error(FinanceError.ValidationError("El número de cuotas debe estar entre 1 y 36"))
            }
            
            // Obtener la compra original
            val originalPurchaseEntity = purchaseDao.getPurchaseById(purchase.id)
                ?: return Result.Error(FinanceError.NotFound("Purchase", purchase.id))
            
            val originalPurchase = originalPurchaseEntity.toDomain()
            
            // Obtener la tarjeta
            val card = creditCardDao.getCardById(purchase.cardId)
                ?: return Result.Error(FinanceError.NotFound("CreditCard", purchase.cardId))
            
            // Calcular la diferencia en el monto
            val amountDifference = purchase.totalAmount - originalPurchase.totalAmount
            
            // Verificar que haya cupo disponible si el monto aumentó
            if (amountDifference > 0 && card.availableLimit < amountDifference) {
                return Result.Error(
                    FinanceError.InsufficientCredit(
                        available = card.availableLimit,
                        required = amountDifference
                    )
                )
            }
            
            // Actualizar el cupo disponible de la tarjeta
            val newAvailableLimit = card.availableLimit - amountDifference
            val updatedAt = System.currentTimeMillis()
            
            // Transacción: actualizar compra y ajustar cupo en una sola operación
            database.withTransaction {
                purchaseDao.updatePurchase(purchase.toEntity())
                creditCardDao.updateAvailableLimit(
                    cardId = card.id,
                    newLimit = newAvailableLimit,
                    updatedAt = updatedAt
                )
            }
            
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(FinanceError.DatabaseError(e.message ?: "Error al actualizar compra"))
        }
    }

    override suspend fun deletePurchase(purchase: Purchase): Result<Unit> {
        return try {
            // Obtener la tarjeta
            val card = creditCardDao.getCardById(purchase.cardId)
                ?: return Result.Error(FinanceError.NotFound("CreditCard", purchase.cardId))
            
            // Restaurar el cupo disponible de la tarjeta
            val newAvailableLimit = card.availableLimit + purchase.totalAmount
            val updatedAt = System.currentTimeMillis()
            
            // Transacción: eliminar compra y restaurar cupo en una sola operación
            database.withTransaction {
                purchaseDao.deletePurchase(purchase.toEntity())
                creditCardDao.updateAvailableLimit(
                    cardId = card.id,
                    newLimit = newAvailableLimit,
                    updatedAt = updatedAt
                )
            }
            
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(FinanceError.DatabaseError(e.message ?: "Error al eliminar compra"))
        }
    }

    override suspend fun repairInstallments(): Result<Int> {
        return try {
            val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date

            val cards = creditCardDao.getAllCards().first()
                .associate { it.id to it.toDomain() }

            // Solo compras que aún tienen cuotas sin pagar
            val allPurchases = purchaseDao.getAllPurchases().first()
                .map { it.toDomain() }
                .filter { it.paidInstallments < it.installments }

            val updates = mutableMapOf<Long, Int>()
            // Tarjetas para las que la reparación avanzó al menos una cuota. Al
            // normalizarlas, el último corte completado también queda conciliado:
            // no puede conservar un pago pendiente por cuotas que acabamos de
            // marcar como pagadas.
            val repairedCardCutoffs = mutableMapOf<Long, LocalDate>()

            for (purchase in allPurchases) {
                val card = cards[purchase.cardId] ?: continue

                // La referencia es el último corte que ya terminó. Si hoy es el
                // día de corte o posterior, se usa el corte del mes actual; de lo
                // contrario, el corte del mes anterior. Así, por ejemplo, el 20
                // de septiembre una tarjeta con corte el 15 normaliza también las
                // cuotas que vencieron el 15 de septiembre.
                val lastCompletedCutoff = calculateLastCompletedCutoffDate(card.cutoffDay, today)

                // Avanzar paidInstallments hasta la última cuota cuyo corte sea
                // igual o anterior al último corte completado. Solo sube, nunca baja.
                var lastPaidInstallment = purchase.paidInstallments
                for (i in (purchase.paidInstallments + 1)..purchase.installments) {
                    val dueDate = purchase.getInstallmentDueDate(i, card.cutoffDay)
                    val alreadyDue = dueDate <= lastCompletedCutoff
                    if (alreadyDue) {
                        lastPaidInstallment = i
                    } else {
                        break
                    }
                }

                if (lastPaidInstallment > purchase.paidInstallments) {
                    updates[purchase.id] = lastPaidInstallment
                    repairedCardCutoffs[purchase.cardId] = lastCompletedCutoff
                }
            }

            if (updates.isNotEmpty()) {
                database.withTransaction {
                    purchaseDao.updateMultiplePaidInstallments(updates)

                    // La reparación representa que las cuotas hasta este corte
                    // ya fueron conciliadas. Se limpia cualquier importe pendiente
                    // persistido para evitar que Inicio o los recordatorios sigan
                    // mostrando como deuda cuotas ya normalizadas. El cupo se deja
                    // intacto: esta herramienta no registra un abono de dinero.
                    repairedCardCutoffs.forEach { (cardId, cutoffDate) ->
                        val card = cards[cardId] ?: return@forEach
                        creditCardDao.updateCardPaymentInfo(
                            cardId = cardId,
                            newAvailableLimit = card.availableLimit,
                            newPendingMinimumPayment = 0.0,
                            newLastCutoffDate = cutoffDate
                                .atStartOfDayIn(TimeZone.currentSystemDefault())
                                .toEpochMilliseconds(),
                            newLastCutoffPayment = 0.0,
                            updatedAt = System.currentTimeMillis()
                        )
                    }
                }
            }

            Result.Success(updates.size)
        } catch (e: Exception) {
            Result.Error(FinanceError.DatabaseError(e.message ?: "Error al reparar cuotas"))
        }
    }

    // ==================== Expenses ====================

    override fun getExpensesByAccount(accountId: Long): Flow<List<Expense>> {
        return expenseDao.getExpensesByAccount(accountId).map { entities ->
            entities.toExpenseDomain()
        }
    }

    override fun getAllExpenses(): Flow<List<Expense>> {
        return expenseDao.getAllExpenses().map { entities ->
            entities.toExpenseDomain()
        }
    }

    override suspend fun addExpense(expense: Expense): Result<Long> {
        return try {
            // Validar que el monto sea positivo
            if (expense.amount <= 0) {
                return Result.Error(FinanceError.InvalidAmount("El monto debe ser mayor a cero"))
            }
            
            // Obtener la cuenta
            val account = savingsAccountDao.getAccountById(expense.accountId)
                ?: return Result.Error(FinanceError.NotFound("SavingsAccount", expense.accountId))
            
            // Verificar que haya saldo disponible
            if (account.balance < expense.amount) {
                return Result.Error(
                    FinanceError.InsufficientFunds(
                        available = account.balance,
                        required = expense.amount
                    )
                )
            }
            
            // Insertar el gasto
            val expenseId = expenseDao.insertExpense(expense.toEntity())
            
            // Actualizar el saldo de la cuenta
            val newBalance = account.balance - expense.amount
            savingsAccountDao.updateBalance(
                accountId = account.id,
                newBalance = newBalance,
                updatedAt = System.currentTimeMillis()
            )
            
            Result.Success(expenseId)
        } catch (e: Exception) {
            Result.Error(FinanceError.DatabaseError(e.message ?: "Error al agregar gasto"))
        }
    }

    override suspend fun updateExpense(expense: Expense): Result<Unit> {
        return try {
            // Validar que el monto sea positivo
            if (expense.amount <= 0) {
                return Result.Error(FinanceError.InvalidAmount("El monto debe ser mayor a cero"))
            }
            
            // Obtener el gasto original
            val originalExpenseEntity = expenseDao.getExpenseById(expense.id)
                ?: return Result.Error(FinanceError.NotFound("Expense", expense.id))
            
            val originalExpense = originalExpenseEntity.toDomain()
            
            // Obtener la cuenta
            val account = savingsAccountDao.getAccountById(expense.accountId)
                ?: return Result.Error(FinanceError.NotFound("SavingsAccount", expense.accountId))
            
            // Calcular la diferencia en el monto
            val amountDifference = expense.amount - originalExpense.amount
            
            // Verificar que haya saldo disponible si el monto aumentó
            if (amountDifference > 0 && account.balance < amountDifference) {
                return Result.Error(
                    FinanceError.InsufficientCredit(
                        available = account.balance,
                        required = amountDifference
                    )
                )
            }
            
            // Actualizar el saldo de la cuenta
            val newBalance = account.balance - amountDifference
            val updatedAt = System.currentTimeMillis()
            
            // Transacción: actualizar gasto y ajustar saldo en una sola operación
            database.withTransaction {
                expenseDao.updateExpense(expense.toEntity())
                savingsAccountDao.updateBalance(
                    accountId = account.id,
                    newBalance = newBalance,
                    updatedAt = updatedAt
                )
            }
            
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(FinanceError.DatabaseError(e.message ?: "Error al actualizar gasto"))
        }
    }

    override suspend fun deleteExpense(expense: Expense): Result<Unit> {
        return try {
            // Obtener la cuenta
            val account = savingsAccountDao.getAccountById(expense.accountId)
                ?: return Result.Error(FinanceError.NotFound("SavingsAccount", expense.accountId))
            
            // Eliminar el gasto
            expenseDao.deleteExpense(expense.toEntity())
            
            // Restaurar el saldo de la cuenta
            val newBalance = account.balance + expense.amount
            savingsAccountDao.updateBalance(
                accountId = account.id,
                newBalance = newBalance,
                updatedAt = System.currentTimeMillis()
            )
            
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(FinanceError.DatabaseError(e.message ?: "Error al eliminar gasto"))
        }
    }

    // ==================== Financial Calculations ====================

    override suspend fun calculateTotalAvailable(): Result<Double> {
        return try {
            val total = savingsAccountDao.getTotalBalance() ?: 0.0
            Result.Success(total)
        } catch (e: Exception) {
            Result.Error(FinanceError.DatabaseError(e.message ?: "Error al calcular disponible total"))
        }
    }

    override suspend fun calculateTotalDebt(): Result<Double> {
        return try {
            val total = creditCardDao.getTotalDebt() ?: 0.0
            Result.Success(total)
        } catch (e: Exception) {
            Result.Error(FinanceError.DatabaseError(e.message ?: "Error al calcular deuda total"))
        }
    }

    override suspend fun calculateRealAvailable(): Result<Double> {
        return try {
            val totalAvailable = savingsAccountDao.getTotalBalance() ?: 0.0
            // Usar el total de pagos mínimos en lugar de la deuda total
            val totalMinimumPaymentsResult = calculateTotalMinimumPayments()
            val totalMinimumPayments = if (totalMinimumPaymentsResult is Result.Success) {
                totalMinimumPaymentsResult.data
            } else {
                0.0
            }
            val realAvailable = totalAvailable - totalMinimumPayments
            Result.Success(realAvailable)
        } catch (e: Exception) {
            Result.Error(FinanceError.DatabaseError(e.message ?: "Error al calcular disponible real"))
        }
    }


    override suspend fun calculatePaymentForCutoff(cardId: Long, cutoffDate: LocalDate): Result<Double> {
        return try {
            // Verificar que la tarjeta existe
            val cardEntity = creditCardDao.getCardById(cardId)
                ?: return Result.Error(FinanceError.NotFound("CreditCard", cardId))
            
            val card = cardEntity.toDomain()
            
            // Obtener todas las compras de la tarjeta
            val allPurchases = purchaseDao.getPurchasesByCard(cardId).first()
                .map { it.toDomain() }
            
            // Calcular el total a pagar usando la nueva lógica del modelo Purchase
            var totalPayment = 0.0
            
            for (purchase in allPurchases) {
                // Obtener las cuotas que vencen en este corte usando la lógica corregida
                val installmentsDue = purchase.getInstallmentsDueInCutoff(cutoffDate, card.cutoffDay)
                
                // Filtrar solo las cuotas que aún no han sido pagadas
                val unpaidInstallments = installmentsDue.filter { installmentNumber ->
                    installmentNumber > purchase.paidInstallments
                }
                
                // Si hay cuotas no pagadas que vencen en este corte, sumar el monto
                if (unpaidInstallments.isNotEmpty()) {
                    totalPayment += purchase.installmentAmount * unpaidInstallments.size
                }
            }
            
            Result.Success(totalPayment)
        } catch (e: Exception) {
            Result.Error(FinanceError.DatabaseError(e.message ?: "Error al calcular pago de corte"))
        }
    }
    
    /**
     * Obtiene el desglose detallado de pagos para un corte específico
     * 
     * Requirements: 4.5
     */
    override suspend fun getPaymentBreakdownForCutoff(cardId: Long, cutoffDate: LocalDate): Result<CutoffPaymentBreakdown> {
        return try {
            // Verificar que la tarjeta existe
            val cardEntity = creditCardDao.getCardById(cardId)
                ?: return Result.Error(FinanceError.NotFound("CreditCard", cardId))
            
            val card = cardEntity.toDomain()
            
            // Obtener todas las compras de la tarjeta
            val allPurchases = purchaseDao.getPurchasesByCard(cardId).first()
                .map { it.toDomain() }
            
            // Obtener todas las cuotas que vencen en este corte usando la lógica corregida
            val installments = mutableListOf<InstallmentDetail>()
            
            for (purchase in allPurchases) {
                // Obtener los números de cuota que vencen en este corte
                val installmentNumbers = purchase.getInstallmentsDueInCutoff(cutoffDate, card.cutoffDay)
                
                // Filtrar solo las cuotas que aún no han sido pagadas
                val unpaidInstallments = installmentNumbers.filter { it > purchase.paidInstallments }
                
                // Crear InstallmentDetail para cada cuota no pagada que vence
                for (installmentNumber in unpaidInstallments) {
                    val dueDate = purchase.getInstallmentDueDate(installmentNumber, card.cutoffDay)
                    installments.add(
                        InstallmentDetail(
                            purchase = purchase,
                            installmentNumber = installmentNumber,
                            installmentAmount = purchase.installmentAmount,
                            dueDate = dueDate
                        )
                    )
                }
            }
            
            // Calcular el total
            val totalAmount = installments.sumOf { it.installmentAmount }
            
            // Crear el desglose
            val breakdown = CutoffPaymentBreakdown(
                cardId = cardId,
                cutoffDate = cutoffDate,
                installments = installments,
                totalAmount = totalAmount
            )
            
            Result.Success(breakdown)
        } catch (e: Exception) {
            Result.Error(FinanceError.DatabaseError(e.message ?: "Error al obtener desglose de pago"))
        }
    }

    override suspend fun calculateMinimumPayment(cardId: Long, cutoffDate: LocalDate): Result<Double> {
        return try {
            // Proyección del próximo pago = suma de cuotas pendientes que vencen en ese corte.
            val result = calculatePaymentForCutoff(cardId, cutoffDate)
            if (result is Result.Error) return result
            Result.Success((result as Result.Success).data)
        } catch (e: Exception) {
            Result.Error(FinanceError.DatabaseError(e.message ?: "Error al calcular pago mínimo"))
        }
    }

    override suspend fun calculateTotalMinimumPayments(): Result<Double> {
        return try {
            val allCards = creditCardDao.getAllCards().first().map { it.toDomain() }
            var total = 0.0
            val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
            for (card in allCards) {
                val nextCutoffDate = calculateNextCutoffDate(card.cutoffDay, now)
                val result = calculateMinimumPayment(card.id, nextCutoffDate)
                if (result is Result.Success) total += result.data
            }
            Result.Success(total)
        } catch (e: Exception) {
            Result.Error(FinanceError.DatabaseError(e.message ?: "Error al calcular total de pagos mínimos"))
        }
    }
    
    /**
     * Calcula la fecha del próximo corte basándose en el día de corte
     */
    private fun calculateNextCutoffDate(cutoffDay: Int, currentDate: LocalDate): LocalDate {
        val currentDay = currentDate.dayOfMonth
        
        // Si ya pasó el día de corte este mes, el próximo corte es el próximo mes
        val nextCutoffMonth = if (currentDay >= cutoffDay) {
            currentDate.monthNumber + 1
        } else {
            currentDate.monthNumber
        }
        
        var nextCutoffYear = currentDate.year
        var actualMonth = nextCutoffMonth
        
        // Ajustar año si el mes excede 12
        if (actualMonth > 12) {
            actualMonth = 1
            nextCutoffYear += 1
        }
        
        // Asegurar que el día de corte no exceda los días del mes
        val daysInMonth = Month(actualMonth).length(isLeapYear(nextCutoffYear))
        val actualCutoffDay = minOf(cutoffDay, daysInMonth)
        
        return LocalDate(nextCutoffYear, actualMonth, actualCutoffDay)
    }

    /** Obtiene el último corte que ya ocurrió, respetando meses con menos días. */
    private fun calculateLastCompletedCutoffDate(cutoffDay: Int, currentDate: LocalDate): LocalDate {
        val currentMonthLastDay = Month(currentDate.monthNumber).length(isLeapYear(currentDate.year))
        val currentMonthCutoffDay = minOf(cutoffDay, currentMonthLastDay)

        if (currentDate.dayOfMonth >= currentMonthCutoffDay) {
            return LocalDate(currentDate.year, currentDate.monthNumber, currentMonthCutoffDay)
        }

        val previousMonth = if (currentDate.monthNumber == 1) 12 else currentDate.monthNumber - 1
        val previousYear = if (currentDate.monthNumber == 1) currentDate.year - 1 else currentDate.year
        val previousMonthLastDay = Month(previousMonth).length(isLeapYear(previousYear))
        return LocalDate(previousYear, previousMonth, minOf(cutoffDay, previousMonthLastDay))
    }
    
    /**
     * Verifica si un año es bisiesto
     */
    private fun isLeapYear(year: Int): Boolean {
        return (year % 4 == 0 && year % 100 != 0) || (year % 400 == 0)
    }

    // ==================== Incomes ====================

    override fun getIncomesByAccount(accountId: Long): Flow<List<Income>> {
        return incomeDao.getIncomesByAccount(accountId).map { entities ->
            entities.toIncomeDomain()
        }
    }

    override fun getAllIncomes(): Flow<List<Income>> {
        return incomeDao.getAllIncomes().map { entities ->
            entities.toIncomeDomain()
        }
    }

    override suspend fun addIncome(income: Income): Result<Long> {
        return try {
            // Validar que el monto sea positivo
            if (income.amount <= 0) {
                return Result.Error(FinanceError.InvalidAmount("El monto debe ser mayor a cero"))
            }
            
            // Obtener la cuenta
            val account = savingsAccountDao.getAccountById(income.accountId)
                ?: return Result.Error(FinanceError.NotFound("SavingsAccount", income.accountId))
            
            // Insertar el ingreso
            val incomeId = incomeDao.insertIncome(income.toEntity())
            
            // Actualizar el saldo de la cuenta (sumar el ingreso)
            val newBalance = account.balance + income.amount
            savingsAccountDao.updateBalance(
                accountId = account.id,
                newBalance = newBalance,
                updatedAt = System.currentTimeMillis()
            )
            
            Result.Success(incomeId)
        } catch (e: Exception) {
            Result.Error(FinanceError.DatabaseError(e.message ?: "Error al agregar ingreso"))
        }
    }

    override suspend fun updateIncome(income: Income): Result<Unit> {
        return try {
            // Validar que el monto sea positivo
            if (income.amount <= 0) {
                return Result.Error(FinanceError.InvalidAmount("El monto debe ser mayor a cero"))
            }
            
            // Obtener el ingreso original
            val originalIncome = incomeDao.getIncomeById(income.id)
                ?: return Result.Error(FinanceError.NotFound("Income", income.id))
            
            // Obtener la cuenta
            val account = savingsAccountDao.getAccountById(income.accountId)
                ?: return Result.Error(FinanceError.NotFound("SavingsAccount", income.accountId))
            
            // Calcular la diferencia de montos
            val amountDifference = income.amount - originalIncome.amount
            
            // Actualizar el ingreso
            incomeDao.updateIncome(income.toEntity())
            
            // Actualizar el saldo de la cuenta
            val newBalance = account.balance + amountDifference
            savingsAccountDao.updateBalance(
                accountId = account.id,
                newBalance = newBalance,
                updatedAt = System.currentTimeMillis()
            )
            
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(FinanceError.DatabaseError(e.message ?: "Error al actualizar ingreso"))
        }
    }

    override suspend fun deleteIncome(income: Income): Result<Unit> {
        return try {
            // Obtener la cuenta
            val account = savingsAccountDao.getAccountById(income.accountId)
                ?: return Result.Error(FinanceError.NotFound("SavingsAccount", income.accountId))
            
            // Eliminar el ingreso
            incomeDao.deleteIncome(income.toEntity())
            
            // Restar el monto del ingreso del saldo de la cuenta
            val newBalance = account.balance - income.amount
            savingsAccountDao.updateBalance(
                accountId = account.id,
                newBalance = newBalance,
                updatedAt = System.currentTimeMillis()
            )
            
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(FinanceError.DatabaseError(e.message ?: "Error al eliminar ingreso"))
        }
    }

    // ==================== Card Payments (historial) ====================

    override fun getPaymentsByCard(cardId: Long): Flow<List<CardPayment>> {
        return cardPaymentDao.getPaymentsByCard(cardId).map { it.toCardPaymentDomain() }
    }

    override fun getAllCardPayments(): Flow<List<CardPayment>> {
        return cardPaymentDao.getAllPayments().map { it.toCardPaymentDomain() }
    }

    override suspend fun insertCardPaymentRecord(payment: CardPayment): Result<Long> {
        return try {
            val id = cardPaymentDao.insertPayment(payment.toEntity())
            Result.Success(id)
        } catch (e: Exception) {
            Result.Error(FinanceError.DatabaseError(e.message ?: "Error al insertar registro de pago"))
        }
    }

    override suspend fun recalculateNextCutoffPayment(cardId: Long): Result<Double> {
        return try {
            val cardEntity = creditCardDao.getCardById(cardId)
                ?: return Result.Error(FinanceError.NotFound("CreditCard", cardId))
            val card = cardEntity.toDomain()
            val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date

            val lastCutoff = card.lastCutoffDate
                ?.toLocalDateTime(TimeZone.currentSystemDefault())?.date

            // Este botón no repara ni modifica cuotas. Usa el estado ya conciliado
            // por "Reparar cuotas" y calcula exclusivamente la proyección del
            // siguiente corte.
            val nextCutoffDate = if (lastCutoff == null) {
                calculateNextCutoffDate(card.cutoffDay, today)
            } else {
                val nextMonth = if (lastCutoff.monthNumber == 12) 1 else lastCutoff.monthNumber + 1
                val nextYear = if (lastCutoff.monthNumber == 12) lastCutoff.year + 1 else lastCutoff.year
                val daysInNextMonth = Month(nextMonth).length(isLeapYear(nextYear))
                LocalDate(nextYear, nextMonth, minOf(card.cutoffDay, daysInNextMonth))
            }

            val newMinimumResult = calculateMinimumPayment(cardId, nextCutoffDate)
            if (newMinimumResult is Result.Error) return newMinimumResult
            val newMinimum = (newMinimumResult as Result.Success).data

            creditCardDao.updatePendingMinimumPayment(
                cardId = cardId,
                pendingPayment = newMinimum,
                updatedAt = System.currentTimeMillis()
            )

            Result.Success(newMinimum)
        } catch (e: Exception) {
            Result.Error(FinanceError.DatabaseError(e.message ?: "Error al recalcular pago mínimo"))
        }
    }
}
