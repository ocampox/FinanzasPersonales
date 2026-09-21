package com.finanzas.personales

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.work.WorkManager
import com.finanzas.personales.di.WorkManagerModule
import com.finanzas.personales.ui.navigation.AppScaffold
import com.finanzas.personales.ui.navigation.Screen
import com.finanzas.personales.ui.theme.FinanceTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    
    @Inject
    lateinit var workManager: WorkManager
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Programar el worker de recordatorios de pago
        WorkManagerModule.schedulePaymentReminderWorker(workManager)
        
        // Manejar navegación desde notificaciones
        handleNotificationIntent(intent)
        
        setContent {
            FinanceTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppScaffold(
                        initialRoute = getInitialRoute(intent)
                    )
                }
            }
        }
    }
    
    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        handleNotificationIntent(intent)
    }
    
    /**
     * Maneja el intent de notificación para navegar a la pantalla correcta
     */
    private fun handleNotificationIntent(intent: Intent?) {
        intent?.let {
            val cardId = it.getLongExtra("cardId", -1L)
            val navigateTo = it.getStringExtra("navigateTo")
            
            if (cardId != -1L && navigateTo == "cardDetail") {
                // La navegación se manejará en AppScaffold usando el initialRoute
            }
        }
    }
    
    /**
     * Obtiene la ruta inicial basada en el intent
     */
    private fun getInitialRoute(intent: Intent?): String {
        intent?.let {
            val cardId = it.getLongExtra("cardId", -1L)
            val navigateTo = it.getStringExtra("navigateTo")
            
            if (cardId != -1L && navigateTo == "cardDetail") {
                return Screen.CardDetail.createRoute(cardId)
            }
        }
        return Screen.Home.route
    }
}
