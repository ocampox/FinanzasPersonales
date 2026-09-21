package com.finanzas.personales.di

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.finanzas.personales.work.PaymentReminderWorker
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

/**
 * Módulo de Hilt para configurar WorkManager
 * 
 * Proporciona instancias de WorkManager y configura los workers periódicos.
 * 
 * Requirements: 10.4
 */
@Module
@InstallIn(SingletonComponent::class)
object WorkManagerModule {
    
    /**
     * Proporciona una instancia de WorkManager
     */
    @Provides
    @Singleton
    fun provideWorkManager(@ApplicationContext context: Context): WorkManager {
        return WorkManager.getInstance(context)
    }
    
    /**
     * Configura y programa el PaymentReminderWorker para ejecutarse diariamente
     * 
     * El worker se ejecuta una vez al día aproximadamente a las 9:00 AM
     * 
     * Requirements: 10.4
     */
    fun schedulePaymentReminderWorker(workManager: WorkManager) {
        // Constraints: requiere conexión a internet (opcional) y dispositivo cargando (opcional)
        val constraints = Constraints.Builder()
            .setRequiresCharging(false)
            .build()
        
        // Crear trabajo periódico que se ejecuta cada 24 horas (con flexibilidad)
        val workRequest = PeriodicWorkRequestBuilder<PaymentReminderWorker>(
            24, TimeUnit.HOURS,
            1, TimeUnit.HOURS // Flexibilidad de 1 hora
        )
            .setConstraints(constraints)
            .build()
        
        // Programar el trabajo con política de mantener el existente
        workManager.enqueueUniquePeriodicWork(
            "payment_reminder_work",
            ExistingPeriodicWorkPolicy.KEEP,
            workRequest
        )
    }
}

