package com.finanzas.personales

import android.app.Application
import com.finanzas.personales.utils.NotificationHelper
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class FinanceApplication : Application() {
    
    override fun onCreate() {
        super.onCreate()
        
        // Crear canales de notificación
        NotificationHelper.createNotificationChannels(this)
        
        // Programar el worker de recordatorios de pago
        // Nota: La inyección de WorkManager se hace después de onCreate,
        // por lo que programamos el worker desde MainActivity
    }
}
