package com.finanzas.personales.ui.screens.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finanzas.personales.data.repository.BackupService
import com.finanzas.personales.data.repository.FinanceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel para la pantalla de configuración
 * 
 * Gestiona el estado de las configuraciones de la aplicación,
 * incluyendo las preferencias de notificaciones y backup/restauración.
 * 
 * Requirements: 9.5, 10.5
 */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repository: FinanceRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {
    
    private val backupService = BackupService(repository, context)
    
    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()
    
    init {
        loadSettings()
    }
    
    /**
     * Carga las configuraciones guardadas
     */
    private fun loadSettings() {
        viewModelScope.launch {
            // Por ahora, las notificaciones están habilitadas por defecto
            // En el futuro, esto se puede guardar en SharedPreferences o en la base de datos
            _uiState.update { it.copy(notificationsEnabled = true) }
        }
    }
    
    /**
     * Activa o desactiva las notificaciones
     */
    fun toggleNotifications(enabled: Boolean) {
        viewModelScope.launch {
            _uiState.update { it.copy(notificationsEnabled = enabled) }
            // Aquí se podría guardar la preferencia en SharedPreferences
            // Por ahora, solo actualizamos el estado
        }
    }
    
    /**
     * Exporta todos los datos a un archivo JSON
     */
    fun exportData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isExporting = true, exportError = null) }
            
            val result = backupService.exportData()
            
            when (result) {
                is com.finanzas.personales.domain.models.Result.Success -> {
                    _uiState.update {
                        it.copy(
                            isExporting = false,
                            exportMessage = "Datos exportados exitosamente a: ${result.data}"
                        )
                    }
                }
                is com.finanzas.personales.domain.models.Result.Error -> {
                    _uiState.update {
                        it.copy(
                            isExporting = false,
                            exportError = result.error.toString()
                        )
                    }
                }
            }
        }
    }
    
    /**
     * Importa datos desde un archivo JSON
     */
    fun importData(filePath: String, merge: Boolean = false) {
        viewModelScope.launch {
            _uiState.update { it.copy(isImporting = true, importError = null) }
            
            val result = backupService.importData(filePath, merge)
            
            when (result) {
                is com.finanzas.personales.domain.models.Result.Success -> {
                    _uiState.update {
                        it.copy(
                            isImporting = false,
                            importMessage = if (merge) "Datos fusionados exitosamente" else "Datos importados exitosamente"
                        )
                    }
                }
                is com.finanzas.personales.domain.models.Result.Error -> {
                    _uiState.update {
                        it.copy(
                            isImporting = false,
                            importError = result.error.toString()
                        )
                    }
                }
            }
        }
    }
    
    /**
     * Limpia los mensajes de exportación
     */
    fun clearExportMessage() {
        _uiState.update { it.copy(exportMessage = null, exportError = null) }
    }
    
    /**
     * Limpia los mensajes de importación
     */
    fun clearImportMessage() {
        _uiState.update { it.copy(importMessage = null, importError = null) }
    }

    /**
     * Repara el campo paidInstallments según el último corte completado de cada tarjeta.
     * Corrige datos corruptos generados por el bug de abonos a capital.
     */
    fun repairInstallments() {
        viewModelScope.launch {
            _uiState.update { it.copy(isRepairing = true, repairMessage = null, repairError = null) }

            when (val result = repository.repairInstallments()) {
                is com.finanzas.personales.domain.models.Result.Success -> {
                    val fixed = result.data
                    _uiState.update {
                        it.copy(
                            isRepairing = false,
                            repairMessage = if (fixed > 0)
                                "$fixed compra${if (fixed != 1) "s corregidas" else " corregida"} exitosamente"
                            else
                                "No se encontraron compras que corregir"
                        )
                    }
                }
                is com.finanzas.personales.domain.models.Result.Error -> {
                    _uiState.update {
                        it.copy(
                            isRepairing = false,
                            repairError = result.error.toString()
                        )
                    }
                }
            }
        }
    }

    /** Limpia los mensajes de reparación */
    fun clearRepairMessage() {
        _uiState.update { it.copy(repairMessage = null, repairError = null) }
    }

    /**
     * Recalcula la proyección del próximo corte para todas las tarjetas usando
     * las cuotas actuales, sin modificar cuotas ni cupos.
     */
    fun recalculateAllMinimumPayments() {
        viewModelScope.launch {
            _uiState.update { it.copy(isRecalculating = true, recalculateMessage = null, recalculateError = null) }

            try {
                val cards = repository.getAllCreditCards().first()
                var updated = 0
                for (card in cards) {
                    when (repository.recalculateNextCutoffPayment(card.id)) {
                        is com.finanzas.personales.domain.models.Result.Success -> updated++
                        is com.finanzas.personales.domain.models.Result.Error -> Unit
                    }
                }
                _uiState.update {
                    it.copy(
                        isRecalculating = false,
                        recalculateMessage = if (updated > 0)
                            "Pago mínimo recalculado para $updated tarjeta${if (updated != 1) "s" else ""}"
                        else
                            "No había tarjetas que actualizar"
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isRecalculating = false,
                        recalculateError = e.message ?: "Error al recalcular"
                    )
                }
            }
        }
    }

    fun clearRecalculateMessage() {
        _uiState.update { it.copy(recalculateMessage = null, recalculateError = null) }
    }

    /** Carga las tarjetas disponibles para el ajuste de cupo */
    fun loadCards() {
        viewModelScope.launch {
            val cards = repository.getAllCreditCards().first()
            _uiState.update { it.copy(cards = cards) }
        }
    }

    /**
     * Ajusta el cupo disponible de una tarjeta al valor real del banco.
     * Solo modifica availableLimit — no toca lastCutoffDate, paidInstallments ni nada más.
     */
    fun adjustAvailableLimit(cardId: Long, newAvailableLimit: Double) {
        viewModelScope.launch {
            _uiState.update { it.copy(isAdjusting = true, adjustMessage = null, adjustError = null) }
            val card = repository.getAllCreditCards().first().find { it.id == cardId }
            if (card == null) {
                _uiState.update { it.copy(isAdjusting = false, adjustError = "Tarjeta no encontrada") }
                return@launch
            }
            val clamped = newAvailableLimit.coerceIn(0.0, card.totalLimit)
            when (val result = repository.updateCreditCard(card.copy(availableLimit = clamped))) {
                is com.finanzas.personales.domain.models.Result.Success ->
                    _uiState.update {
                        it.copy(
                            isAdjusting = false,
                            adjustMessage = "Cupo actualizado a ${
                                java.text.NumberFormat.getCurrencyInstance(java.util.Locale("es", "CO")).format(clamped)
                            }"
                        )
                    }
                is com.finanzas.personales.domain.models.Result.Error ->
                    _uiState.update {
                        it.copy(isAdjusting = false, adjustError = result.error.toString())
                    }
            }
        }
    }

    fun clearAdjustMessage() {
        _uiState.update { it.copy(adjustMessage = null, adjustError = null) }
    }
}

/**
 * Estado de la UI para la pantalla de configuración
 */
data class SettingsUiState(
    val notificationsEnabled: Boolean = true,
    val isExporting: Boolean = false,
    val isImporting: Boolean = false,
    val isRepairing: Boolean = false,
    val isRecalculating: Boolean = false,
    val isAdjusting: Boolean = false,
    val exportMessage: String? = null,
    val exportError: String? = null,
    val importMessage: String? = null,
    val importError: String? = null,
    val repairMessage: String? = null,
    val repairError: String? = null,
    val recalculateMessage: String? = null,
    val recalculateError: String? = null,
    val adjustMessage: String? = null,
    val adjustError: String? = null,
    val cards: List<com.finanzas.personales.domain.models.CreditCard> = emptyList()
)

