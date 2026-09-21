package com.finanzas.personales.domain.usecases

import com.finanzas.personales.data.repository.FinanceRepository
import com.finanzas.personales.domain.models.PaymentDue
import com.finanzas.personales.domain.models.Result
import kotlinx.coroutines.flow.first
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import javax.inject.Inject

/**
 * Use case para obtener los próximos pagos de tarjetas de crédito
 * 
 * Este use case obtiene todas las tarjetas y calcula los pagos próximos
 * basándose en las fechas de pago y los montos adeudados.
 * 
 * Requirements: 5.4
 */
class GetUpcomingPaymentsUseCase @Inject constructor(
    private val repository: FinanceRepository
) {
    /**
     * Ejecuta la obtención de pagos próximos
     * 
     * @param daysAhead Número de días hacia adelante para considerar pagos próximos (default: 30)
     * @return Result con lista de PaymentDue ordenada por fecha, o FinanceError si falla
     */
    suspend operator fun invoke(daysAhead: Int = 30): Result<List<PaymentDue>> {
        return try {
            // Obtener fecha actual
            val currentDate = Clock.System.now()
                .toLocalDateTime(TimeZone.currentSystemDefault())
                .date
            
            // Obtener todas las tarjetas de crédito
            val creditCards = repository.getAllCreditCards().first()
            
            // Obtener todas las compras
            val allPurchases = repository.getAllPurchases().first()
            
            // Crear lista de pagos próximos
            val upcomingPayments = mutableListOf<PaymentDue>()
            
            for (card in creditCards) {
                // Calcular la próxima fecha de pago
                val nextPaymentDate = calculateNextPaymentDate(card.paymentDay, currentDate)

                // Verificar si está dentro del rango de días
                if (nextPaymentDate.toEpochDays() - currentDate.toEpochDays() <= daysAhead) {
                    // El corte correspondiente a esta fecha de pago
                    val cutoffDate = calculateCutoffDateForPayment(
                        card.cutoffDay,
                        card.paymentDay,
                        nextPaymentDate
                    )

                    // Proyección: suma de cuotas pendientes que vencen en ese corte
                    val paymentAmount: Double = run {
                        val result = repository.calculatePaymentForCutoff(card.id, cutoffDate)
                        if (result is Result.Success) result.data else 0.0
                    }

                    if (paymentAmount > 0) {
                        val cardPurchases = allPurchases.filter { it.cardId == card.id }
                        upcomingPayments.add(
                            PaymentDue(
                                card = card,
                                amount = paymentAmount,
                                dueDate = nextPaymentDate,
                                purchases = cardPurchases
                            )
                        )
                    }
                }
            }
            
            // Ordenar por fecha de pago (más próximo primero)
            val sortedPayments = upcomingPayments.sortedBy { it.dueDate }
            
            Result.Success(sortedPayments)
        } catch (e: Exception) {
            Result.Error(
                com.finanzas.personales.domain.models.FinanceError.DatabaseError(
                    e.message ?: "Error al obtener pagos próximos"
                )
            )
        }
    }
    
    /**
     * Calcula la próxima fecha de pago basándose en el día de pago y la fecha actual
     */
    private fun calculateNextPaymentDate(paymentDay: Int, currentDate: LocalDate): LocalDate {
        val currentYear = currentDate.year
        val currentMonth = currentDate.monthNumber
        val currentDayOfMonth = currentDate.dayOfMonth
        
        // Si el día de pago aún no ha pasado este mes, usar este mes
        return if (currentDayOfMonth < paymentDay) {
            LocalDate(currentYear, currentMonth, paymentDay.coerceAtMost(getLastDayOfMonth(currentYear, currentMonth)))
        } else {
            // Si ya pasó, usar el próximo mes
            val nextMonth = if (currentMonth == 12) 1 else currentMonth + 1
            val nextYear = if (currentMonth == 12) currentYear + 1 else currentYear
            LocalDate(nextYear, nextMonth, paymentDay.coerceAtMost(getLastDayOfMonth(nextYear, nextMonth)))
        }
    }
    
    /**
     * Calcula la fecha de corte correspondiente a una fecha de pago
     */
    private fun calculateCutoffDateForPayment(
        cutoffDay: Int,
        paymentDay: Int,
        paymentDate: LocalDate
    ): LocalDate {
        // El corte es típicamente en el mes anterior al pago
        val cutoffMonth = if (paymentDate.monthNumber == 1) 12 else paymentDate.monthNumber - 1
        val cutoffYear = if (paymentDate.monthNumber == 1) paymentDate.year - 1 else paymentDate.year
        
        return LocalDate(
            cutoffYear,
            cutoffMonth,
            cutoffDay.coerceAtMost(getLastDayOfMonth(cutoffYear, cutoffMonth))
        )
    }
    
    /**
     * Obtiene el último día de un mes específico
     */
    private fun getLastDayOfMonth(year: Int, month: Int): Int {
        return when (month) {
            1, 3, 5, 7, 8, 10, 12 -> 31
            4, 6, 9, 11 -> 30
            2 -> if (isLeapYear(year)) 29 else 28
            else -> 31
        }
    }
    
    /**
     * Verifica si un año es bisiesto
     */
    private fun isLeapYear(year: Int): Boolean {
        return (year % 4 == 0 && year % 100 != 0) || (year % 400 == 0)
    }
}
