package com.finanzas.personales.data.repository

import android.content.Context
import com.finanzas.personales.data.local.entities.ExpenseCategory
import com.finanzas.personales.data.models.*
import com.finanzas.personales.domain.models.*
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import kotlinx.coroutines.flow.first
import kotlinx.datetime.Instant
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

/**
 * Servicio de exportación e importación de datos.
 *
 * Versión 2.0 del formato: exporta e importa el estado financiero completo,
 * incluyendo el ciclo de corte de cada tarjeta, cuotas pagadas, ingresos
 * e historial de pagos. Una restauración desde un backup v2.0 reconstruye
 * el estado exacto sin necesidad de recálculos manuales posteriores.
 *
 * Compatibilidad hacia atrás: si se importa un backup v1.0 (sin incomes,
 * cardPayments ni paidInstallments), esos campos se restauran como vacíos/cero.
 *
 * Requirements: 9.5
 */
class BackupService(
    private val repository: FinanceRepository,
    private val context: Context
) {
    private val gson: Gson = GsonBuilder().setPrettyPrinting().create()

    // ─────────────────────────────────────────────────────────────────────────
    // Exportación
    // ─────────────────────────────────────────────────────────────────────────

    suspend fun exportData(): Result<String> {
        return try {
            val creditCards   = repository.getAllCreditCards().first()
            val savingsAccounts = repository.getAllSavingsAccounts().first()
            val purchases     = repository.getAllPurchases().first()
            val expenses      = repository.getAllExpenses().first()
            val incomes       = repository.getAllIncomes().first()
            val cardPayments  = repository.getAllCardPayments().first()

            val backupData = BackupData(
                version    = "2.0",
                exportDate = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault()).format(Date()),

                creditCards = creditCards.map { card ->
                    CreditCardBackup(
                        id                       = card.id,
                        name                     = card.name,
                        totalLimit               = card.totalLimit,
                        availableLimit           = card.availableLimit,
                        cutoffDay                = card.cutoffDay,
                        paymentDay               = card.paymentDay,
                        color                    = card.color,
                        pendingMinimumPayment    = card.pendingMinimumPayment,
                        lastCutoffDate           = card.lastCutoffDate?.toEpochMilliseconds(),
                        lastCutoffPayment        = card.lastCutoffPayment,
                        minimumPaymentPercentage = card.minimumPaymentPercentage,
                        createdAt                = card.createdAt.toEpochMilliseconds(),
                        updatedAt                = card.updatedAt.toEpochMilliseconds()
                    )
                },

                savingsAccounts = savingsAccounts.map { account ->
                    SavingsAccountBackup(
                        id        = account.id,
                        name      = account.name,
                        balance   = account.balance,
                        color     = account.color,
                        createdAt = account.createdAt.toEpochMilliseconds(),
                        updatedAt = account.updatedAt.toEpochMilliseconds()
                    )
                },

                purchases = purchases.map { purchase ->
                    PurchaseBackup(
                        id                = purchase.id,
                        cardId            = purchase.cardId,
                        description       = purchase.description,
                        totalAmount       = purchase.totalAmount,
                        installments      = purchase.installments,
                        installmentAmount = purchase.installmentAmount,
                        paidInstallments  = purchase.paidInstallments,
                        purchaseDate      = purchase.purchaseDate.toEpochMilliseconds(),
                        createdAt         = purchase.purchaseDate.toEpochMilliseconds()
                    )
                },

                expenses = expenses.map { expense ->
                    ExpenseBackup(
                        id          = expense.id,
                        accountId   = expense.accountId,
                        description = expense.description,
                        amount      = expense.amount,
                        category    = expense.category.name,
                        expenseDate = expense.expenseDate.toEpochMilliseconds(),
                        createdAt   = expense.expenseDate.toEpochMilliseconds()
                    )
                },

                incomes = incomes.map { income ->
                    IncomeBackup(
                        id          = income.id,
                        accountId   = income.accountId,
                        description = income.description,
                        amount      = income.amount,
                        category    = income.category.name,
                        incomeDate  = income.incomeDate.toEpochMilliseconds(),
                        createdAt   = income.incomeDate.toEpochMilliseconds()
                    )
                },

                cardPayments = cardPayments.map { payment ->
                    CardPaymentBackup(
                        id          = payment.id,
                        cardId      = payment.cardId,
                        amount      = payment.amount,
                        paymentType = payment.paymentType.name,
                        cutoffDate  = payment.cutoffDate?.toEpochMilliseconds(),
                        paymentDate = payment.paymentDate.toEpochMilliseconds(),
                        note        = payment.note
                    )
                }
            )

            val json = gson.toJson(backupData)
            val dir  = context.getExternalFilesDir(null)?.let {
                File(it, "Backups").apply { mkdirs() }
            } ?: File(context.filesDir, "Backups").apply { mkdirs() }

            val fileName = "finanzas_backup_${
                SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            }.json"
            val file = File(dir, fileName)
            FileOutputStream(file).use { it.write(json.toByteArray(Charsets.UTF_8)) }

            Result.Success(file.absolutePath)
        } catch (e: Exception) {
            Result.Error(FinanceError.DatabaseError("Error al exportar: ${e.message}"))
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Importación
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Importa datos desde un archivo JSON.
     *
     * @param filePath Ruta del archivo JSON.
     * @param merge    false = borra todo y restaura. true = añade sobre lo existente.
     */
    suspend fun importData(filePath: String, merge: Boolean = false): Result<Unit> {
        return try {
            val file = File(filePath)
            if (!file.exists()) {
                return Result.Error(FinanceError.NotFound("Archivo de backup", 0))
            }

            val json = FileInputStream(file).use { it.bufferedReader(Charsets.UTF_8).readText() }
            val backupData = gson.fromJson(json, BackupData::class.java)

            validateBackupData(backupData)?.let { errorMsg ->
                return Result.Error(FinanceError.InvalidAmount(errorMsg))
            }

            // ── Si no es merge, borrar los datos existentes en orden seguro ──
            if (!merge) {
                repository.getAllPurchases().first().forEach  { repository.deletePurchase(it) }
                repository.getAllExpenses().first().forEach   { repository.deleteExpense(it) }
                repository.getAllIncomes().first().forEach    { repository.deleteIncome(it) }
                repository.getAllCreditCards().first().forEach { repository.deleteCreditCard(it) }
                repository.getAllSavingsAccounts().first().forEach { repository.deleteSavingsAccount(it) }
                // card_payments se eliminan en cascada al borrar las tarjetas
            }

            // ── Tarjetas ──────────────────────────────────────────────────────
            // El availableLimit se restaura directamente del backup — no se recalcula.
            // paidInstallments de las compras y el estado de corte ya están en el backup.
            val cardIdMap = mutableMapOf<Long, Long>() // backupId → nuevaId

            for (c in backupData.creditCards) {
                val card = CreditCard(
                    id                       = if (merge) 0L else c.id,
                    name                     = c.name,
                    totalLimit               = c.totalLimit,
                    availableLimit           = c.availableLimit.coerceIn(0.0, c.totalLimit),
                    cutoffDay                = c.cutoffDay,
                    paymentDay               = c.paymentDay,
                    color                    = c.color,
                    pendingMinimumPayment    = c.pendingMinimumPayment,
                    lastCutoffDate           = c.lastCutoffDate?.let { Instant.fromEpochMilliseconds(it) },
                    lastCutoffPayment        = c.lastCutoffPayment,
                    minimumPaymentPercentage = c.minimumPaymentPercentage,
                    createdAt                = Instant.fromEpochMilliseconds(c.createdAt),
                    updatedAt                = Instant.fromEpochMilliseconds(c.updatedAt)
                )
                when (val r = repository.addCreditCard(card)) {
                    is Result.Success -> cardIdMap[c.id] = r.data
                    is Result.Error   -> { /* continuar con las demás */ }
                }
            }

            // En merge, buscar las tarjetas recién creadas por nombre para actualizar el mapa
            if (merge) {
                val all = repository.getAllCreditCards().first()
                backupData.creditCards.forEach { c ->
                    all.find { it.name == c.name }?.let { cardIdMap[c.id] = it.id }
                }
            }

            // ── Cuentas de ahorros ────────────────────────────────────────────
            // El balance se restaura directamente — no se recalcula desde gastos/ingresos.
            val accountIdMap = mutableMapOf<Long, Long>()

            for (a in backupData.savingsAccounts) {
                val account = SavingsAccount(
                    id        = if (merge) 0L else a.id,
                    name      = a.name,
                    balance   = a.balance,
                    color     = a.color,
                    createdAt = Instant.fromEpochMilliseconds(a.createdAt),
                    updatedAt = Instant.fromEpochMilliseconds(a.updatedAt)
                )
                when (val r = repository.addSavingsAccount(account)) {
                    is Result.Success -> accountIdMap[a.id] = r.data
                    is Result.Error   -> { }
                }
            }

            if (merge) {
                val all = repository.getAllSavingsAccounts().first()
                backupData.savingsAccounts.forEach { a ->
                    all.find { it.name == a.name }?.let { accountIdMap[a.id] = it.id }
                }
            }

            // ── Compras — con paidInstallments ────────────────────────────────
            // addPurchase descuenta del availableLimit, por eso en modo reemplazo
            // el availableLimit del backup ya debe ser el correcto (sin compras descontadas).
            // Para evitar doble descuento restauramos el estado directamente vía updatePurchase
            // después de insertar con totalAmount=0 no — mejor: insertamos con el monto real
            // y confiamos en que el availableLimit del backup ya lo refleja.
            // NOTA: en modo replace el availableLimit se restauró exacto desde backup.
            //       addPurchase volvería a decrementarlo. Para evitarlo, lo insertamos
            //       directamente vía updateCreditCard después de restaurar las compras.
            //
            // Estrategia limpia: restaurar compras sin tocar el availableLimit.
            // Se usa repository.addPurchase() que sí descuenta, por eso ajustamos el
            // availableLimit de la tarjeta al valor del backup AL FINAL.
            val purchaseIdMap = mutableMapOf<Long, Long>()
            for (p in backupData.purchases) {
                val newCardId = cardIdMap[p.cardId] ?: continue
                val purchase = Purchase(
                    id                = if (merge) 0L else p.id,
                    cardId            = newCardId,
                    description       = p.description,
                    totalAmount       = p.totalAmount,
                    installments      = p.installments,
                    installmentAmount = p.installmentAmount,
                    paidInstallments  = p.paidInstallments,
                    purchaseDate      = Instant.fromEpochMilliseconds(p.purchaseDate)
                )
                when (val r = repository.addPurchase(purchase)) {
                    is Result.Success -> purchaseIdMap[p.id] = r.data
                    is Result.Error   -> { }
                }
            }

            // Restaurar availableLimit exacto de cada tarjeta (addPurchase lo decrementó)
            if (!merge) {
                val allCards = repository.getAllCreditCards().first()
                for (c in backupData.creditCards) {
                    val currentCard = allCards.find { it.id == (cardIdMap[c.id] ?: c.id) } ?: continue
                    repository.updateCreditCard(
                        currentCard.copy(
                            availableLimit        = c.availableLimit.coerceIn(0.0, c.totalLimit),
                            pendingMinimumPayment = c.pendingMinimumPayment,
                            lastCutoffDate        = c.lastCutoffDate?.let { Instant.fromEpochMilliseconds(it) },
                            lastCutoffPayment     = c.lastCutoffPayment,
                            minimumPaymentPercentage = c.minimumPaymentPercentage
                        )
                    )
                }
            }

            // ── Gastos ────────────────────────────────────────────────────────
            for (e in backupData.expenses) {
                val newAccountId = accountIdMap[e.accountId] ?: continue
                val category = runCatching { ExpenseCategory.valueOf(e.category) }
                    .getOrDefault(ExpenseCategory.OTHER)
                val expense = Expense(
                    id          = if (merge) 0L else e.id,
                    accountId   = newAccountId,
                    description = e.description,
                    amount      = e.amount,
                    category    = category,
                    expenseDate = Instant.fromEpochMilliseconds(e.expenseDate)
                )
                repository.addExpense(expense)
            }

            // Restaurar balance exacto de cada cuenta (addExpense lo decrementó)
            if (!merge) {
                val allAccounts = repository.getAllSavingsAccounts().first()
                for (a in backupData.savingsAccounts) {
                    val current = allAccounts.find { it.id == (accountIdMap[a.id] ?: a.id) } ?: continue
                    repository.updateSavingsAccount(current.copy(balance = a.balance))
                }
            }

            // ── Ingresos ──────────────────────────────────────────────────────
            for (i in backupData.incomes) {
                val newAccountId = accountIdMap[i.accountId] ?: continue
                val category = runCatching {
                    com.finanzas.personales.domain.models.IncomeCategory.valueOf(i.category)
                }.getOrDefault(com.finanzas.personales.domain.models.IncomeCategory.OTHER)
                val income = Income(
                    id          = if (merge) 0L else i.id,
                    accountId   = newAccountId,
                    description = i.description,
                    amount      = i.amount,
                    category    = category,
                    incomeDate  = Instant.fromEpochMilliseconds(i.incomeDate)
                )
                repository.addIncome(income)
            }

            // Restaurar balance exacto después de addIncome (que lo incrementó)
            if (!merge) {
                val allAccounts = repository.getAllSavingsAccounts().first()
                for (a in backupData.savingsAccounts) {
                    val current = allAccounts.find { it.id == (accountIdMap[a.id] ?: a.id) } ?: continue
                    repository.updateSavingsAccount(current.copy(balance = a.balance))
                }
            }

            // ── Historial de pagos a tarjetas ─────────────────────────────────
            // Solo en modo replace o si el backup es v2.0
            if (backupData.version.startsWith("2")) {
                for (cp in backupData.cardPayments) {
                    val newCardId = cardIdMap[cp.cardId] ?: continue
                    val paymentType = runCatching { PaymentType.valueOf(cp.paymentType) }
                        .getOrDefault(PaymentType.MINIMUM_PAYMENT)
                    val cardPayment = CardPayment(
                        id          = 0L,          // siempre nuevo ID
                        cardId      = newCardId,
                        amount      = cp.amount,
                        paymentType = paymentType,
                        cutoffDate  = cp.cutoffDate?.let { Instant.fromEpochMilliseconds(it) },
                        paymentDate = Instant.fromEpochMilliseconds(cp.paymentDate),
                        note        = cp.note
                    )
                    // Guardar directamente vía addPayment no aplica (modifica el estado de la tarjeta).
                    // Usamos el repositorio interno para insertar solo el registro histórico.
                    repository.insertCardPaymentRecord(cardPayment)
                }
            }

            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(FinanceError.DatabaseError("Error al importar: ${e.message}"))
        }
    }

    private fun validateBackupData(data: BackupData): String? {
        if (data.version.isBlank()) return "Versión de backup no válida"
        if (data.exportDate.isBlank()) return "Fecha de exportación no válida"
        return null
    }
}
