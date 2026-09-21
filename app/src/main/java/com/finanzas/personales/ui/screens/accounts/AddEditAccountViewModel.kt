package com.finanzas.personales.ui.screens.accounts

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finanzas.personales.data.repository.FinanceRepository
import com.finanzas.personales.domain.models.FinanceError
import com.finanzas.personales.domain.models.Result
import com.finanzas.personales.domain.models.SavingsAccount
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import javax.inject.Inject

/**
 * ViewModel para la pantalla de agregar/editar cuenta de ahorros
 * 
 * Gestiona el estado del formulario, validaciones y operaciones
 * de guardado/actualización de cuentas.
 * 
 * Requirements: 2.1, 2.3
 */
@HiltViewModel
class AddEditAccountViewModel @Inject constructor(
    private val repository: FinanceRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val accountId: Long? = savedStateHandle.get<Long>("accountId")?.takeIf { it > 0 }
    private val isEditMode = accountId != null

    private val _uiState = MutableStateFlow(AddEditAccountUiState(isEditMode = isEditMode))
    val uiState: StateFlow<AddEditAccountUiState> = _uiState.asStateFlow()

    init {
        if (accountId != null) {
            loadAccount(accountId)
        }
    }

    /**
     * Carga los datos de una cuenta existente para edición
     */
    private fun loadAccount(id: Long) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, isEditMode = true) }

            when (val result = repository.getSavingsAccountById(id)) {
                is Result.Success -> {
                    val account = result.data
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            name = account.name,
                            balance = account.balance.toString(),
                            selectedColor = account.color
                        )
                    }
                }
                is Result.Error -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = "Error al cargar la cuenta"
                        )
                    }
                }
            }
        }
    }

    /**
     * Actualiza el nombre de la cuenta
     */
    fun onNameChange(name: String) {
        _uiState.update {
            it.copy(
                name = name,
                nameError = null
            )
        }
    }

    /**
     * Actualiza el saldo de la cuenta
     */
    fun onBalanceChange(balance: String) {
        // Permitir solo números y punto decimal
        if (balance.isEmpty() || balance.matches(Regex("^-?\\d*\\.?\\d*$"))) {
            _uiState.update {
                it.copy(
                    balance = balance,
                    balanceError = null
                )
            }
        }
    }

    /**
     * Actualiza el color seleccionado
     */
    fun onColorSelect(color: String) {
        _uiState.update { it.copy(selectedColor = color) }
    }

    /**
     * Valida y guarda la cuenta
     */
    fun saveAccount() {
        val currentState = _uiState.value

        // Validar nombre
        if (currentState.name.isBlank()) {
            _uiState.update { it.copy(nameError = "El nombre es requerido") }
            return
        }

        // Validar saldo
        val balanceValue = currentState.balance.toDoubleOrNull()
        if (balanceValue == null) {
            _uiState.update { it.copy(balanceError = "Ingrese un saldo válido") }
            return
        }

        // Guardar cuenta
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, error = null) }

            val now = Clock.System.now()
            val account = SavingsAccount(
                id = accountId ?: 0,
                name = currentState.name.trim(),
                balance = balanceValue,
                color = currentState.selectedColor,
                createdAt = now,
                updatedAt = now
            )

            val result = if (accountId != null) {
                repository.updateSavingsAccount(account)
            } else {
                repository.addSavingsAccount(account)
            }

            when (result) {
                is Result.Success -> {
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            saveSuccess = true
                        )
                    }
                }
                is Result.Error -> {
                    val errorMessage = when (result.error) {
                        is FinanceError.InvalidAmount -> {
                            (result.error as FinanceError.InvalidAmount).message
                        }
                        is FinanceError.DatabaseError -> {
                            (result.error as FinanceError.DatabaseError).message
                        }
                        is FinanceError.ValidationError -> {
                            (result.error as FinanceError.ValidationError).message
                        }
                        is FinanceError.NotFound -> {
                            val error = result.error as FinanceError.NotFound
                            "${error.entityType} no encontrado"
                        }
                        else -> "Error al guardar la cuenta"
                    }
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            error = errorMessage
                        )
                    }
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
}

/**
 * Estado de la UI para la pantalla de agregar/editar cuenta
 */
data class AddEditAccountUiState(
    val isLoading: Boolean = false,
    val isEditMode: Boolean = false,
    val name: String = "",
    val balance: String = "",
    val selectedColor: String = "#006C4C",
    val nameError: String? = null,
    val balanceError: String? = null,
    val isSaving: Boolean = false,
    val saveSuccess: Boolean = false,
    val error: String? = null
)
