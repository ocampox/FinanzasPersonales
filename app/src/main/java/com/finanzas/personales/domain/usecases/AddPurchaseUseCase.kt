package com.finanzas.personales.domain.usecases

import com.finanzas.personales.data.repository.FinanceRepository
import com.finanzas.personales.domain.models.FinanceError
import com.finanzas.personales.domain.models.Purchase
import com.finanzas.personales.domain.models.Result
import javax.inject.Inject

/**
 * Use case para agregar una compra con validación de cupo disponible
 * 
 * Este use case valida que la tarjeta tenga cupo suficiente antes de
 * registrar la compra y actualizar el cupo disponible.
 * 
 * Requirements: 3.2, 3.4
 */
class AddPurchaseUseCase @Inject constructor(
    private val repository: FinanceRepository
) {
    /**
     * Ejecuta la adición de una compra con validaciones
     * 
     * @param purchase Compra a registrar
     * @return Result con el ID de la compra si es exitoso, o FinanceError si falla
     */
    suspend operator fun invoke(purchase: Purchase): Result<Long> {
        // Validar que el monto sea positivo
        if (purchase.totalAmount <= 0) {
            return Result.Error(
                FinanceError.InvalidAmount("El monto de la compra debe ser mayor a cero")
            )
        }
        
        // Validar que el número de cuotas sea válido (1-36)
        if (purchase.installments < 1 || purchase.installments > 36) {
            return Result.Error(
                FinanceError.ValidationError("El número de cuotas debe estar entre 1 y 36")
            )
        }
        
        // Validar que la descripción no esté vacía
        if (purchase.description.isBlank()) {
            return Result.Error(
                FinanceError.ValidationError("La descripción de la compra no puede estar vacía")
            )
        }
        
        // Obtener la tarjeta para validar cupo disponible
        val cardResult = repository.getCreditCardById(purchase.cardId)
        if (cardResult is Result.Error) {
            return cardResult
        }
        
        val card = (cardResult as Result.Success).data
        
        // Validar que haya cupo disponible
        if (!card.hasAvailableCredit(purchase.totalAmount)) {
            return Result.Error(
                FinanceError.InsufficientCredit(
                    available = card.availableLimit,
                    required = purchase.totalAmount
                )
            )
        }
        
        // Agregar la compra (el repository se encarga de actualizar el cupo)
        return repository.addPurchase(purchase)
    }
}
