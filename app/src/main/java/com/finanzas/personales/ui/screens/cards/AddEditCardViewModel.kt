package com.finanzas.personales.ui.screens.cards

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finanzas.personales.data.repository.FinanceRepository
import com.finanzas.personales.domain.models.CreditCard
import com.finanzas.personales.domain.models.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import javax.inject.Inject
import kotlin.math.roundToLong

@HiltViewModel
class AddEditCardViewModel @Inject constructor(
    private val repository: FinanceRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val cardId: Long? = savedStateHandle.get<Long>("cardId")?.takeIf { it > 0 }
    private val isEditMode = cardId != null
    private var existingCard: CreditCard? = null

    private val _uiState = MutableStateFlow(AddEditCardUiState(isEditMode = isEditMode))
    val uiState: StateFlow<AddEditCardUiState> = _uiState.asStateFlow()

    init {
        if (isEditMode && cardId != null) loadCard(cardId)
    }

    private fun loadCard(id: Long) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            when (val result = repository.getCreditCardById(id)) {
                is Result.Success -> {
                    val card = result.data
                    existingCard = card
                    _uiState.update {
                        it.copy(
                            name = card.name,
                            totalLimit = card.totalLimit.toString(),
                            cutoffDay = card.cutoffDay.toString(),
                            paymentDay = card.paymentDay.toString(),
                            selectedColor = card.color,
                            isLoading = false
                        )
                    }
                }
                is Result.Error -> {
                    existingCard = null
                    _uiState.update { it.copy(isLoading = false, error = "Error al cargar la tarjeta") }
                }
            }
        }
    }

    fun onNameChange(name: String) = _uiState.update { it.copy(name = name, nameError = null) }
    fun onTotalLimitChange(limit: String) = _uiState.update { it.copy(totalLimit = limit, totalLimitError = null) }
    fun onCutoffDayChange(day: String) = _uiState.update { it.copy(cutoffDay = day, cutoffDayError = null) }
    fun onPaymentDayChange(day: String) = _uiState.update { it.copy(paymentDay = day, paymentDayError = null) }
    fun onColorSelect(color: String) = _uiState.update { it.copy(selectedColor = color) }

    fun saveCard() {
        if (!validateForm()) return
        val currentState = _uiState.value

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, error = null) }

            val now = Clock.System.now()
            val newTotalLimit = currentState.totalLimit.toDouble()

            val card = if (isEditMode) {
                val id = cardId ?: 0L
                val loaded = existingCard ?: when (val r = repository.getCreditCardById(id)) {
                    is Result.Success -> r.data
                    is Result.Error -> null
                }
                if (loaded != null) {
                    val usedLimit = (loaded.totalLimit - loaded.availableLimit).coerceAtLeast(0.0)
                    loaded.copy(
                        name = currentState.name.trim(),
                        totalLimit = newTotalLimit,
                        availableLimit = (newTotalLimit - usedLimit).coerceIn(0.0, newTotalLimit),
                        cutoffDay = currentState.cutoffDay.toInt(),
                        paymentDay = currentState.paymentDay.toInt(),
                        color = currentState.selectedColor,
                        updatedAt = now
                    )
                } else {
                    _uiState.update { it.copy(isSaving = false, error = "No se pudo cargar la tarjeta para editar") }
                    return@launch
                }
            } else {
                CreditCard(
                    id = 0,
                    name = currentState.name.trim(),
                    totalLimit = newTotalLimit,
                    availableLimit = newTotalLimit,
                    cutoffDay = currentState.cutoffDay.toInt(),
                    paymentDay = currentState.paymentDay.toInt(),
                    color = currentState.selectedColor,
                    createdAt = now,
                    updatedAt = now
                )
            }

            when (val result = if (isEditMode) repository.updateCreditCard(card) else repository.addCreditCard(card)) {
                is Result.Success -> _uiState.update { it.copy(isSaving = false, saveSuccess = true) }
                is Result.Error -> _uiState.update { it.copy(isSaving = false, error = result.error.toString()) }
            }
        }
    }

    private fun validateForm(): Boolean {
        val s = _uiState.value
        var valid = true

        if (s.name.isBlank()) {
            _uiState.update { it.copy(nameError = "El nombre es requerido") }
            valid = false
        }

        val totalLimit = s.totalLimit.toDoubleOrNull()
        if (totalLimit == null || totalLimit <= 0) {
            _uiState.update { it.copy(totalLimitError = "Ingresa un cupo válido mayor a 0") }
            valid = false
        } else if (isEditMode) {
            val card = existingCard
            if (card != null) {
                val usedLimit = (card.totalLimit - card.availableLimit).coerceAtLeast(0.0)
                if (totalLimit + 1e-6 < usedLimit) {
                    _uiState.update { it.copy(totalLimitError = "El cupo total no puede ser menor a la deuda actual (${usedLimit.roundToLong()})") }
                    valid = false
                }
            }
        }

        if (s.cutoffDay.toIntOrNull()?.let { it < 1 || it > 31 } != false) {
            _uiState.update { it.copy(cutoffDayError = "Ingresa un día entre 1 y 31") }
            valid = false
        }

        if (s.paymentDay.toIntOrNull()?.let { it < 1 || it > 31 } != false) {
            _uiState.update { it.copy(paymentDayError = "Ingresa un día entre 1 y 31") }
            valid = false
        }

        return valid
    }

    fun clearError() = _uiState.update { it.copy(error = null) }
}

data class AddEditCardUiState(
    val isEditMode: Boolean = false,
    val name: String = "",
    val totalLimit: String = "",
    val cutoffDay: String = "",
    val paymentDay: String = "",
    val selectedColor: String = "#006C4C",
    val nameError: String? = null,
    val totalLimitError: String? = null,
    val cutoffDayError: String? = null,
    val paymentDayError: String? = null,
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val saveSuccess: Boolean = false,
    val error: String? = null
)
