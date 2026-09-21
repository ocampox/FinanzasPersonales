package com.finanzas.personales.domain.usecases

import com.finanzas.personales.data.repository.FinanceRepository
import com.finanzas.personales.domain.models.FinanceError
import com.finanzas.personales.domain.models.Income
import com.finanzas.personales.domain.models.Result
import javax.inject.Inject

/**
 * Use case para agregar un ingreso a una cuenta de ahorros
 * 
 * Este use case valida los datos del ingreso y actualiza automáticamente
 * el saldo de la cuenta de ahorros asociada.
 */
class AddIncomeUseCase @Inject constructor(
    private val repository: FinanceRepository
) {
    /**
     * Ejecuta la adición del ingreso
     * 
     * @param income Ingreso a agregar
     * @return Result con el ID del ingreso creado si es exitoso, o FinanceError si falla
     */
    suspend operator fun invoke(income: Income): Result<Long> {
        // Validar que el monto sea positivo
        if (income.amount <= 0) {
            return Result.Error(
                FinanceError.InvalidAmount("El monto del ingreso debe ser mayor a cero")
            )
        }
        
        // Validar que la descripción no esté vacía
        if (income.description.isBlank()) {
            return Result.Error(
                FinanceError.ValidationError("La descripción del ingreso no puede estar vacía")
            )
        }
        
        // Agregar el ingreso usando el repository
        return repository.addIncome(income)
    }
}