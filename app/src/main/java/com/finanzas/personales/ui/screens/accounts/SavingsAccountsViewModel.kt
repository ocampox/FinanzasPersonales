package com.finanzas.personales.ui.screens.accounts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finanzas.personales.data.repository.FinanceRepository
import com.finanzas.personales.domain.models.SavingsAccount
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel para la pantalla de lista de cuentas de ahorros
 * 
 * Gestiona el estado de la UI y coordina la obtención de la lista
 * de cuentas de ahorros del repositorio.
 * 
 * Requirements: 2.1, 2.2
 */
@HiltViewModel
class SavingsAccountsViewModel @Inject constructor(
    private val repository: FinanceRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SavingsAccountsUiState())
    val uiState: StateFlow<SavingsAccountsUiState> = _uiState.asStateFlow()

    init {
        loadSavingsAccounts()
    }

    /**
     * Carga la lista de cuentas de ahorros desde el repositorio
     */
    private fun loadSavingsAccounts() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            
            repository.getAllSavingsAccounts()
                .catch { exception ->
                    _uiState.update { 
                        it.copy(
                            isLoading = false,
                            error = exception.message ?: "Error al cargar cuentas"
                        )
                    }
                }
                .collect { accounts ->
                    _uiState.update { 
                        it.copy(
                            savingsAccounts = accounts,
                            isLoading = false,
                            error = null
                        )
                    }
                }
        }
    }

    /**
     * Limpia el mensaje de error
     */
    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    /**
     * Recarga la lista de cuentas
     */
    fun refresh() {
        loadSavingsAccounts()
    }
}

/**
 * Estado de la UI para la pantalla de lista de cuentas
 */
data class SavingsAccountsUiState(
    val savingsAccounts: List<SavingsAccount> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)
