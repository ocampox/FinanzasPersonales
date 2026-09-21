package com.finanzas.personales.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.finanzas.personales.MainActivity
import com.finanzas.personales.R

/**
 * Helper para gestionar canales y notificaciones
 * 
 * Crea y gestiona los canales de notificación y proporciona métodos
 * para mostrar notificaciones de recordatorios de pago.
 * 
 * Requirements: 10.1, 10.2, 10.3
 */
object NotificationHelper {
    // IDs de canales
    const val CHANNEL_CUTOFF_ID = "channel_cutoff"
    const val CHANNEL_PAYMENT_ID = "channel_payment"
    
    // IDs de notificaciones
    private const val NOTIFICATION_ID_BASE = 1000
    
    /**
     * Crea los canales de notificación necesarios
     * 
     * Requirements: 10.1
     */
    fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            
            // Canal para notificaciones de corte
            val cutoffChannel = NotificationChannel(
                CHANNEL_CUTOFF_ID,
                "Recordatorios de Corte",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notificaciones sobre fechas de corte de tarjetas de crédito"
                enableVibration(true)
            }
            
            // Canal para notificaciones de pago
            val paymentChannel = NotificationChannel(
                CHANNEL_PAYMENT_ID,
                "Recordatorios de Pago",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notificaciones sobre pagos próximos de tarjetas de crédito"
                enableVibration(true)
                enableLights(true)
            }
            
            notificationManager.createNotificationChannel(cutoffChannel)
            notificationManager.createNotificationChannel(paymentChannel)
        }
    }
    
    /**
     * Muestra una notificación de recordatorio de pago
     * 
     * @param context Contexto de la aplicación
     * @param cardId ID de la tarjeta de crédito
     * @param cardName Nombre de la tarjeta
     * @param amount Monto a pagar
     * @param dueDate Fecha de vencimiento
     * 
     * Requirements: 10.2, 10.3, 10.5
     */
    fun showPaymentReminderNotification(
        context: Context,
        cardId: Long,
        cardName: String,
        amount: Double,
        dueDate: String
    ) {
        // Crear intent para abrir el detalle de la tarjeta
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("cardId", cardId)
            putExtra("navigateTo", "cardDetail")
        }
        
        val pendingIntent = PendingIntent.getActivity(
            context,
            cardId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        // Formatear el monto
        val formattedAmount = formatCurrency(amount)
        
        // Crear la notificación
        val notification = NotificationCompat.Builder(context, CHANNEL_PAYMENT_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Pago próximo - $cardName")
            .setContentText("Debes pagar $formattedAmount el $dueDate")
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText("Recordatorio: Tienes un pago pendiente de $formattedAmount para la tarjeta $cardName que vence el $dueDate"))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()
        
        // Mostrar la notificación
        val notificationManager = NotificationManagerCompat.from(context)
        val notificationId = NOTIFICATION_ID_BASE + cardId.toInt()
        notificationManager.notify(notificationId, notification)
    }
    
    /**
     * Muestra una notificación de recordatorio de corte
     * 
     * @param context Contexto de la aplicación
     * @param cardId ID de la tarjeta de crédito
     * @param cardName Nombre de la tarjeta
     * @param cutoffDate Fecha de corte
     * 
     * Requirements: 10.2, 10.3
     */
    fun showCutoffReminderNotification(
        context: Context,
        cardId: Long,
        cardName: String,
        cutoffDate: String
    ) {
        // Crear intent para abrir el detalle de la tarjeta
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("cardId", cardId)
            putExtra("navigateTo", "cardDetail")
        }
        
        val pendingIntent = PendingIntent.getActivity(
            context,
            (cardId + 10000).toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        // Crear la notificación
        val notification = NotificationCompat.Builder(context, CHANNEL_CUTOFF_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Corte próximo - $cardName")
            .setContentText("El corte de tu tarjeta será el $cutoffDate")
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText("Recordatorio: El corte de tu tarjeta $cardName será el $cutoffDate. Revisa tus compras."))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()
        
        // Mostrar la notificación
        val notificationManager = NotificationManagerCompat.from(context)
        val notificationId = NOTIFICATION_ID_BASE + 5000 + cardId.toInt()
        notificationManager.notify(notificationId, notification)
    }
    
    /**
     * Formatea un monto como moneda
     */
    private fun formatCurrency(amount: Double): String {
        return String.format("$%,.2f", amount)
    }
}

