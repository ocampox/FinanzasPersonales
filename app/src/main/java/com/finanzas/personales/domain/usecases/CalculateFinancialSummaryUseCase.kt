package com.finanzas.personales.domain.usecases

import com.finanzas.personales.data.repository.FinanceRepository
import com.finanzas.personales.domain.models.FinancialSummary
import com.finanzas.personales.domain.models.Result
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/**
 * Use case para calcular el resumen financiero completo del usuario
 * 
 * Este use case obtiene todas las cuentas de ahorros y tarjetas de crédito,
 * y calcula el disponible total, deuda total y disponible real.
 * 
 * Requirements: 5.1, 5.2, 5.3, 5.4
 */
class CalculateFinancialSummaryUseCase @Inject constructor(
    private val repository: FinanceRepository
) {
    /**
     * Ejecuta el cálculo del resumen financiero
     * 
     * @return Result con FinancialSummary si es exitoso, o FinanceError si falla
     */
    suspend operator fun invoke(): Result<FinancialSummary> {
        return try {
            // Obtener todas las cuentas de ahorros
            val savingsAccounts = repository.getAllSavingsAccounts().first()
            
            // Obtener todas las tarjetas de crédito
            val creditCards = repository.getAllCreditCards().first()
            
            // Calcular disponible total (suma de saldos en cuentas)
            val totalAvailableResult = repository.calculateTotalAvailable()
            if (totalAvailableResult is Result.Error) {
                return totalAvailableResult
            }
            val totalAvailable = (totalAvailableResult as Result.Success).data
            
            // Calcular deuda total (suma de cupo usado en tarjetas)
            val totalDebtResult = repository.calculateTotalDebt()
            if (totalDebtResult is Result.Error) {
                return totalDebtResult
            }
            val totalDebt = (totalDebtResult as Result.Success).data
            
            // Calcular total de pagos mínimos
            val totalMinimumPaymentsResult = repository.calculateTotalMinimumPayments()
            val totalMinimumPayments = if (totalMinimumPaymentsResult is Result.Success) {
                totalMinimumPaymentsResult.data
            } else {
                0.0
            }
            
            // Calcular disponible real (disponible - pagos mínimos)
            val realAvailableResult = repository.calculateRealAvailable()
            if (realAvailableResult is Result.Error) {
                return realAvailableResult
            }
            val realAvailable = (realAvailableResult as Result.Success).data
            
            // Crear el resumen financiero
            val summary = FinancialSummary(
                totalAvailable = totalAvailable,
                totalDebt = totalDebt,
                realAvailable = realAvailable,
                totalMinimumPayments = totalMinimumPayments,
                savingsAccounts = savingsAccounts,
                creditCards = creditCards
            )
            
            Result.Success(summary)
        } catch (e: Exception) {
            Result.Error(
                com.finanzas.personales.domain.models.FinanceError.DatabaseError(
                    e.message ?: "Error al calcular resumen financiero"
                )
            )
        }
    }
}
