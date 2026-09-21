package com.finanzas.personales.work

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.finanzas.personales.domain.models.PaymentDue
import com.finanzas.personales.domain.usecases.GetUpcomingPaymentsUseCase
import com.finanzas.personales.utils.NotificationHelper
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import java.text.SimpleDateFormat
import java.util.*

/**
 * Worker que verifica pagos próximos y muestra notificaciones
 * 
 * Se ejecuta diariamente para verificar si hay pagos que vencen en los próximos 3 días
 * y muestra notificaciones de recordatorio.
 * 
 * Requirements: 10.1, 10.2, 10.3, 10.4
 */
@HiltWorker
class PaymentReminderWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val getUpcomingPaymentsUseCase: GetUpcomingPaymentsUseCase
) : CoroutineWorker(context, params) {
    
    override suspend fun doWork(): Result {
        return try {
            // Obtener fecha actual
            val currentDate = Clock.System.now()
                .toLocalDateTime(TimeZone.currentSystemDefault())
                .date
            
            // Obtener pagos próximos (próximos 3 días)
            val result = getUpcomingPaymentsUseCase(daysAhead = 3)
            
            when (result) {
                is com.finanzas.personales.domain.models.Result.Success -> {
                    val upcomingPayments = result.data
                    
                    // Filtrar solo los pagos que están exactamente a 3 días o menos
                    val paymentsToNotify = upcomingPayments.filter { payment ->
                        payment.isUpcoming(currentDate, daysThreshold = 3)
                    }
                    
                    // Mostrar notificaciones para cada pago próximo
                    paymentsToNotify.forEach { payment ->
                        val formattedDate = formatDate(payment.dueDate)
                        NotificationHelper.showPaymentReminderNotification(
                            context = applicationContext,
                            cardId = payment.card.id,
                            cardName = payment.card.name,
                            amount = payment.amount,
                            dueDate = formattedDate
                        )
                    }
                    
                    Result.success()
                }
                is com.finanzas.personales.domain.models.Result.Error -> {
                    // Log error pero no fallar el worker
                    Result.retry()
                }
            }
        } catch (e: Exception) {
            // En caso de error, reintentar más tarde
            Result.retry()
        }
    }
    
    /**
     * Formatea una fecha LocalDate a string legible
     */
    private fun formatDate(date: LocalDate): String {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.YEAR, date.year)
            set(Calendar.MONTH, date.monthNumber - 1)
            set(Calendar.DAY_OF_MONTH, date.dayOfMonth)
        }
        val format = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        return format.format(calendar.time)
    }
}

