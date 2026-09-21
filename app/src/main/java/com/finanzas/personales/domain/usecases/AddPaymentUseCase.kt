package com.finanzas.personales.domain.usecases

import com.finanzas.personales.data.repository.FinanceRepository
import com.finanzas.personales.domain.models.FinanceError
import com.finanzas.personales.domain.models.Result
import javax.inject.Inject

/**
 * Use case para agregar un abono a una tarjeta de crédito
 * 
 * Este use case valida el monto del abono y actualiza el cupo disponible
 * de la tarjeta y las cuotas pendientes más antiguas.
 */
class AddPaymentUseCase @Inject constructor(
    private val repository: FinanceRepository
) {
    /**
     * Ejecuta la adición de un abono con validaciones
     * 
     * @param cardId ID de la tarjeta de crédito
     * @param amount Monto del abono a agregar
     * @return Result con Unit si es exitoso, o FinanceError si falla
     */
    suspend operator fun invoke(cardId: Long, amount: Double): Result<Unit> {
        // Validar que el monto sea positivo
        if (amount <= 0) {
            return Result.Error(
                FinanceError.InvalidAmount("El monto del abono debe ser mayor a cero")
            )
        }
        
        // Validar que la tarjeta exista
        val cardResult = repository.getCreditCardById(cardId)
        if (cardResult is Result.Error) {
            return cardResult
        }
        
        // Agregar el abono (el repository se encarga de actualizar el cupo disponible y las cuotas)
        return repository.addPayment(cardId, amount)
    }
}

