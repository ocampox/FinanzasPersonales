package com.finanzas.personales.domain.usecases

import com.finanzas.personales.data.repository.FinanceRepository
import com.finanzas.personales.domain.models.Expense
import com.finanzas.personales.domain.models.FinanceError
import com.finanzas.personales.domain.models.Result
import javax.inject.Inject

/**
 * Use case para agregar un gasto con validación de saldo disponible
 * 
 * Este use case valida que la cuenta tenga saldo suficiente antes de
 * registrar el gasto y actualizar el saldo disponible.
 * 
 * Requirements: 6.2
 */
class AddExpenseUseCase @Inject constructor(
    private val repository: FinanceRepository
) {
    /**
     * Ejecuta la adición de un gasto con validaciones
     * 
     * @param expense Gasto a registrar
     * @return Result con el ID del gasto si es exitoso, o FinanceError si falla
     */
    suspend operator fun invoke(expense: Expense): Result<Long> {
        // Validar que el monto sea positivo
        if (expense.amount <= 0) {
            return Result.Error(
                FinanceError.InvalidAmount("El monto del gasto debe ser mayor a cero")
            )
        }
        
        // Validar que la descripción no esté vacía
        if (expense.description.isBlank()) {
            return Result.Error(
                FinanceError.ValidationError("La descripción del gasto no puede estar vacía")
            )
        }
        
        // Obtener la cuenta para validar saldo disponible
        val accountResult = repository.getSavingsAccountById(expense.accountId)
        if (accountResult is Result.Error) {
            return accountResult
        }
        
        val account = (accountResult as Result.Success).data
        
        // Validar que haya saldo disponible
        if (!account.hasAvailableBalance(expense.amount)) {
            return Result.Error(
                FinanceError.InsufficientFunds(
                    available = account.balance,
                    required = expense.amount
                )
            )
        }
        
        // Agregar el gasto (el repository se encarga de actualizar el saldo)
        return repository.addExpense(expense)
    }
}
