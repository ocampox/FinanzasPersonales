package com.finanzas.personales.ui.screens.transactions

import com.finanzas.personales.data.local.entities.ExpenseCategory
import com.finanzas.personales.ui.components.TransactionType
import kotlinx.datetime.Instant

/**
 * Modelo unificado de transacción para la UI
 * 
 * Combina compras y gastos en una sola estructura para mostrar
 * en la lista de transacciones.
 */
data class TransactionUiModel(
    val id: Long,
    val type: TransactionType,
    val description: String,
    val amount: Double,
    val date: Instant,
    val accountOrCardName: String,
    val accountOrCardId: Long,
    val category: ExpenseCategory? = null,
    val installmentInfo: String? = null
) {
    companion object {
        /**
         * Crea un TransactionUiModel desde una compra
         */
        fun fromPurchase(
            purchase: com.finanzas.personales.domain.models.Purchase,
            cardName: String
        ): TransactionUiModel {
            val installmentInfo = if (purchase.installments > 1) {
                "${purchase.installments} cuotas"
            } else {
                null
            }
            
            return TransactionUiModel(
                id = purchase.id,
                type = TransactionType.PURCHASE,
                description = purchase.description,
                amount = purchase.totalAmount,
                date = purchase.purchaseDate,
                accountOrCardName = cardName,
                accountOrCardId = purchase.cardId,
                installmentInfo = installmentInfo
            )
        }
        
        /**
         * Crea un TransactionUiModel desde un gasto
         */
        fun fromExpense(
            expense: com.finanzas.personales.domain.models.Expense,
            accountName: String
        ): TransactionUiModel {
            return TransactionUiModel(
                id = expense.id,
                type = TransactionType.EXPENSE,
                description = expense.description,
                amount = expense.amount,
                date = expense.expenseDate,
                accountOrCardName = accountName,
                accountOrCardId = expense.accountId,
                category = expense.category
            )
        }
    }
}
