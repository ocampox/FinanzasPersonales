package com.finanzas.personales.domain.usecases

import com.finanzas.personales.data.repository.FinanceRepository
import com.finanzas.personales.domain.models.FinanceError
import com.finanzas.personales.domain.models.Result
import kotlinx.datetime.LocalDate
import javax.inject.Inject

/**
 * Use case para calcular el pago mínimo de una tarjeta para un corte específico
 * 
 * El pago mínimo se calcula como el mayor entre:
 * - 3% del saldo total de la tarjeta
 * - Un monto mínimo fijo ($10,000 COP)
 * - No puede exceder el saldo total ni el pago total del corte
 */
class CalculateMinimumPaymentUseCase @Inject constructor(
    private val repository: FinanceRepository
) {
    /**
     * Ejecuta el cálculo del pago mínimo para un corte específico
     * 
     * @param cardId ID de la tarjeta de crédito
     * @param cutoffDate Fecha del corte para el cual calcular el pago mínimo
     * @return Result con el pago mínimo calculado si es exitoso, o FinanceError si falla
     */
    suspend operator fun invoke(cardId: Long, cutoffDate: LocalDate): Result<Double> {
        // Validar que el cardId sea válido
        if (cardId <= 0) {
            return Result.Error(
                FinanceError.ValidationError("ID de tarjeta inválido")
            )
        }
        
        // Calcular el pago mínimo para el corte
        return repository.calculateMinimumPayment(cardId, cutoffDate)
    }
}

