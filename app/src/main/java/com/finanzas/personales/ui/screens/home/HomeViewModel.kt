package com.finanzas.personales.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finanzas.personales.data.repository.FinanceRepository
import com.finanzas.personales.domain.models.CreditCard
import com.finanzas.personales.domain.models.FinancialSummary
import com.finanzas.personales.domain.models.PaymentDue
import com.finanzas.personales.domain.models.Purchase
import com.finanzas.personales.domain.models.Result
import com.finanzas.personales.domain.models.SavingsAccount
import com.finanzas.personales.domain.usecases.GetUpcomingPaymentsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.datetime.*
import javax.inject.Inject

/**
 * ViewModel para la pantalla principal (Home).
 *
 * Observa los Flows del repositorio directamente con combine, de modo que
 * cualquier cambio en tarjetas, cuentas o compras se refleja automáticamente
 * sin necesidad de recargar manualmente.
 *
 * Requirements: 5.4, 5.5, 7.1, 7.4
 */
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: FinanceRepository,
    private val getUpcomingPaymentsUseCase: GetUpcomingPaymentsUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        observeData()
    }

    /**
     * Suscripción reactiva: recalcula el resumen y los próximos pagos
     * cada vez que cambia cualquier dato en la BD (tarjetas, cuentas, compras).
     */
    private fun observeData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            combine(
                repository.getAllCreditCards(),
                repository.getAllSavingsAccounts(),
                repository.getAllPurchases()
            ) { cards, accounts, purchases ->
                Triple(cards, accounts, purchases)
            }
                .catch { e ->
                    _uiState.update {
                        it.copy(isLoading = false, error = e.message ?: "Error al cargar datos")
                    }
                }
                .collect { (cards, accounts, purchases) ->
                    val totalAvailable = accounts.sumOf { it.balance }
                    val totalDebt = cards.sumOf { it.usedLimit }

                    // Pago mínimo proyectado = suma de cuotas que vencen en el próximo corte
                    val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
                    val totalMinimumPayments = cards.sumOf { card ->
                        calculateNextCutoffPayment(card, purchases, now)
                    }

                    val realAvailable = totalAvailable - totalMinimumPayments

                    val summary = FinancialSummary(
                        totalAvailable = totalAvailable,
                        totalDebt = totalDebt,
                        realAvailable = realAvailable,
                        totalMinimumPayments = totalMinimumPayments,
                        savingsAccounts = accounts,
                        creditCards = cards
                    )

                    // Próximos pagos — se recalcula también al cambiar los datos
                    val upcomingPayments = when (val r = getUpcomingPaymentsUseCase()) {
                        is Result.Success -> r.data
                        is Result.Error -> emptyList()
                    }

                    _uiState.update {
                        it.copy(
                            financialSummary = summary,
                            upcomingPayments = upcomingPayments,
                            isLoading = false,
                            error = null
                        )
                    }
                }
        }
    }

    /**
     * Calcula el monto de cuotas que vencen en el próximo corte de una tarjeta.
     * Duplica la lógica de ReportsViewModel para no añadir dependencias cruzadas.
     */
    private fun calculateNextCutoffPayment(
        card: CreditCard,
        allPurchases: List<Purchase>,
        today: LocalDate
    ): Double {
        val cutoffDate = nextCutoffDate(card.cutoffDay, today)
        val cardPurchases = allPurchases.filter { it.cardId == card.id }
        return cardPurchases.sumOf { purchase ->
            val due = purchase.getInstallmentsDueInCutoff(cutoffDate, card.cutoffDay)
            val unpaid = due.filter { it > purchase.paidInstallments }
            unpaid.size * purchase.installmentAmount
        }
    }

    private fun nextCutoffDate(cutoffDay: Int, today: LocalDate): LocalDate {
        val month = if (today.dayOfMonth >= cutoffDay) today.monthNumber + 1 else today.monthNumber
        var year = today.year
        var actualMonth = month
        if (actualMonth > 12) { actualMonth = 1; year++ }
        val days = Month(actualMonth).length(isLeapYear(year))
        return LocalDate(year, actualMonth, minOf(cutoffDay, days))
    }

    private fun isLeapYear(year: Int) = (year % 4 == 0 && year % 100 != 0) || (year % 400 == 0)

    /** Mantiene compatibilidad con el botón de recarga manual del HomeScreen */
    fun loadData() {
        // No hace nada — la suscripción reactiva se encarga sola.
        // Se conserva para no romper la llamada desde la UI.
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}

data class HomeUiState(
    val financialSummary: FinancialSummary? = null,
    val upcomingPayments: List<PaymentDue> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)
