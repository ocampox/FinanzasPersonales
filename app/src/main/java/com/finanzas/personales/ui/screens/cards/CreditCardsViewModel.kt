package com.finanzas.personales.ui.screens.cards

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finanzas.personales.data.repository.FinanceRepository
import com.finanzas.personales.domain.models.CreditCard
import com.finanzas.personales.domain.models.Purchase
import com.finanzas.personales.domain.models.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.Month
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import javax.inject.Inject

/**
 * ViewModel para la pantalla de lista de tarjetas de crédito
 *
 * Gestiona el estado de la UI y coordina la obtención de la lista
 * de tarjetas de crédito del repositorio.
 *
 * Requirements: 1.5, 8.4
 */
@HiltViewModel
class CreditCardsViewModel @Inject constructor(
    private val repository: FinanceRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CreditCardsUiState())
    val uiState: StateFlow<CreditCardsUiState> = _uiState.asStateFlow()

    init {
        loadCreditCards()
    }

    /**
     * Carga la lista de tarjetas de crédito y enriquece cada una con:
     * - El pago mínimo estimado del próximo corte
     * - El número de cuotas activas (pendientes de pago)
     */
    private fun loadCreditCards() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            combine(
                repository.getAllCreditCards(),
                repository.getAllPurchases()
            ) { cards, allPurchases ->
                val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date

                // Para cada tarjeta calcular su pago mínimo y cuotas activas
                val minimumPayments = mutableMapOf<Long, Double>()
                val pendingInstallmentCounts = mutableMapOf<Long, Int>()

                for (card in cards) {
                    val nextCutoffDate = calculateNextCutoffDate(card.cutoffDay, now)
                    val cardPurchases = allPurchases.filter { it.cardId == card.id }

                    // Contar cuotas activas (compras con cuotas pendientes)
                    val activePurchasesCount = cardPurchases.count { !it.isFullyPaid() }
                    pendingInstallmentCounts[card.id] = activePurchasesCount

                    // Calcular pago mínimo proyectado del próximo corte
                    val minPayment = when (val result = repository.calculateMinimumPayment(card.id, nextCutoffDate)) {
                        is Result.Success -> result.data
                        else -> null
                    }
                    if (minPayment != null) minimumPayments[card.id] = minPayment
                }

                Triple(cards, minimumPayments, pendingInstallmentCounts)
            }
                .catch { exception ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = exception.message ?: "Error al cargar tarjetas"
                        )
                    }
                }
                .collect { (cards, minimumPayments, pendingInstallmentCounts) ->
                    _uiState.update {
                        it.copy(
                            creditCards = cards,
                            minimumPayments = minimumPayments,
                            pendingInstallmentCounts = pendingInstallmentCounts,
                            isLoading = false,
                            error = null
                        )
                    }
                }
        }
    }

    /** Calcula la fecha del próximo corte para un día de corte dado */
    private fun calculateNextCutoffDate(cutoffDay: Int, today: LocalDate): LocalDate {
        val month = if (today.dayOfMonth >= cutoffDay) {
            today.monthNumber + 1
        } else {
            today.monthNumber
        }
        var year = today.year
        var actualMonth = month
        if (actualMonth > 12) { actualMonth = 1; year++ }
        val daysInMonth = Month(actualMonth).length(isLeapYear(year))
        return LocalDate(year, actualMonth, minOf(cutoffDay, daysInMonth))
    }

    private fun isLeapYear(year: Int) = (year % 4 == 0 && year % 100 != 0) || (year % 400 == 0)

    /** Limpia el mensaje de error */
    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    /** Recarga la lista de tarjetas */
    fun refresh() {
        loadCreditCards()
    }
}

/**
 * Estado de la UI para la pantalla de lista de tarjetas
 */
data class CreditCardsUiState(
    val creditCards: List<CreditCard> = emptyList(),
    /** Mapa de cardId → pago mínimo estimado del próximo corte */
    val minimumPayments: Map<Long, Double> = emptyMap(),
    /** Mapa de cardId → número de compras con cuotas aún pendientes */
    val pendingInstallmentCounts: Map<Long, Int> = emptyMap(),
    val isLoading: Boolean = false,
    val error: String? = null
)
