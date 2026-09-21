package com.finanzas.personales.domain.usecases

import com.finanzas.personales.data.repository.FinanceRepository
import com.finanzas.personales.domain.models.FinanceError
import com.finanzas.personales.domain.models.Result
import kotlinx.datetime.LocalDate
import javax.inject.Inject

/**
 * Use case para calcular el monto a pagar en el próximo corte de una tarjeta
 * 
 * Este use case calcula la suma de todas las cuotas que vencen en el próximo
 * corte de una tarjeta específica, considerando compras diferidas.
 * 
 * Requirements: 4.1, 4.2, 4.3, 4.4
 */
class CalculateCardPaymentUseCase @Inject constructor(
    private val repository: FinanceRepository
) {
    /**
     * Ejecuta el cálculo del pago para un corte específico
     * 
     * @param cardId ID de la tarjeta de crédito
     * @param cutoffDate Fecha del corte para el cual calcular el pago
     * @return Result con el monto a pagar si es exitoso, o FinanceError si falla
     */
    suspend operator fun invoke(cardId: Long, cutoffDate: LocalDate): Result<Double> {
        // Validar que el cardId sea válido
        if (cardId <= 0) {
            return Result.Error(
                FinanceError.ValidationError("ID de tarjeta inválido")
            )
        }
        
        // Verificar que la tarjeta existe
        val cardResult = repository.getCreditCardById(cardId)
        if (cardResult is Result.Error) {
            return cardResult
        }
        
        // Calcular el pago para el corte
        return repository.calculatePaymentForCutoff(cardId, cutoffDate)
    }
}
