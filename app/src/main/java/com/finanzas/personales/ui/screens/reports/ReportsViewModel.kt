package com.finanzas.personales.ui.screens.reports

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finanzas.personales.data.local.entities.ExpenseCategory
import com.finanzas.personales.data.repository.FinanceRepository
import com.finanzas.personales.domain.models.CreditCard
import com.finanzas.personales.domain.models.Expense
import com.finanzas.personales.domain.models.PaymentDue
import com.finanzas.personales.domain.models.Purchase
import com.finanzas.personales.domain.models.Result
import com.finanzas.personales.domain.usecases.GetUpcomingPaymentsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.datetime.*
import javax.inject.Inject

/**
 * ViewModel para la pantalla de reportes.
 *
 * Calcula y expone:
 * - KPIs financieros: deuda total, cupo libre, compromisos mensuales
 * - Uso de tarjetas: deuda vs cupo por tarjeta
 * - Proyección de cuotas: compromisos de pago en los próximos 6 meses
 * - Gastos por categoría: del período seleccionado (cuentas de ahorro)
 * - Ingresos del período seleccionado
 * - Próximos pagos con urgencia
 *
 * Requirements: 7.1, 7.2, 7.3, 7.4, 7.5
 */
@HiltViewModel
class ReportsViewModel @Inject constructor(
    private val repository: FinanceRepository,
    private val getUpcomingPaymentsUseCase: GetUpcomingPaymentsUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(ReportsUiState())
    val uiState: StateFlow<ReportsUiState> = _uiState.asStateFlow()

    private val _dateFilter = MutableStateFlow(DateFilter.THIS_MONTH)
    val dateFilter: StateFlow<DateFilter> = _dateFilter.asStateFlow()

    init {
        loadData()
    }

    fun loadData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                combine(
                    repository.getAllExpenses(),
                    repository.getAllIncomes(),
                    repository.getAllCreditCards(),
                    repository.getAllPurchases(),
                    _dateFilter
                ) { expenses, incomes, cards, purchases, filter ->
                    Data(expenses, incomes, cards, purchases, filter)
                }.collect { data ->
                    val dateRange = data.filter.getDateRange()

                    // ── Gastos e ingresos filtrados por período ──────────────────
                    val filteredExpenses = data.expenses.filter { e ->
                        val d = e.expenseDate.toLocalDateTime(TimeZone.currentSystemDefault()).date
                        d >= dateRange.first && d <= dateRange.second
                    }
                    val filteredIncomes = data.incomes.filter { i ->
                        val d = i.incomeDate.toLocalDateTime(TimeZone.currentSystemDefault()).date
                        d >= dateRange.first && d <= dateRange.second
                    }

                    // ── KPIs generales ───────────────────────────────────────────
                    val totalDebt = data.cards.sumOf { it.usedLimit }
                    val totalLimit = data.cards.sumOf { it.totalLimit }
                    val totalAvailableCredit = data.cards.sumOf { it.availableLimit }
                    val overallUsagePercentage = if (totalLimit > 0) (totalDebt / totalLimit * 100).toFloat() else 0f

                    val activePurchases = data.purchases.filter { !it.isFullyPaid() }

                    // ── Uso por tarjeta ───────────────────────────────────────────
                    val cardUsageData = data.cards.map { card ->
                        val cardPurchases = data.purchases.filter { it.cardId == card.id && !it.isFullyPaid() }
                        CardUsage(
                            card = card,
                            usedAmount = card.usedLimit,
                            availableAmount = card.availableLimit,
                            usagePercentage = card.usagePercentage,
                            activePurchasesCount = cardPurchases.size,
                            remainingDebt = cardPurchases.sumOf { it.getRemainingAmount() },
                            nextCutoffPayment = calculateNextCutoffPayment(card, data.purchases)
                        )
                    }.sortedByDescending { it.usedAmount }

                    // totalNextCutoffPayment: suma de las cuotas que vencen en el próximo
                    // corte de cada tarjeta — el compromiso real del mes siguiente.
                    val totalNextCutoffPayment = cardUsageData.sumOf { it.nextCutoffPayment }

                    // ── Proyección mensual de cuotas (próximos 6 meses) ──────────
                    val monthlyProjection = calculateMonthlyProjection(data.cards, data.purchases)

                    // ── Gastos por categoría ──────────────────────────────────────
                    val expensesByCategory = filteredExpenses
                        .groupBy { it.category }
                        .map { (cat, list) -> CategoryExpense(cat, list.sumOf { it.amount }, list.size) }
                        .sortedByDescending { it.amount }

                    // ── Próximos pagos ────────────────────────────────────────────
                    val upcomingPayments = when (val r = getUpcomingPaymentsUseCase()) {
                        is Result.Success -> r.data
                        is Result.Error -> emptyList()
                    }

                    _uiState.update {
                        it.copy(
                            // KPIs
                            totalDebt = totalDebt,
                            totalLimit = totalLimit,
                            totalAvailableCredit = totalAvailableCredit,
                            overallUsagePercentage = overallUsagePercentage,
                            totalNextCutoffPayment = totalNextCutoffPayment,
                            // Período
                            periodTotalExpenses = filteredExpenses.sumOf { e -> e.amount },
                            periodTotalIncomes = filteredIncomes.sumOf { i -> i.amount },
                            periodTransactionCount = filteredExpenses.size,
                            // Visualizaciones
                            cardUsageData = cardUsageData,
                            monthlyProjection = monthlyProjection,
                            expensesByCategory = expensesByCategory,
                            upcomingPayments = upcomingPayments,
                            isLoading = false,
                            error = null
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = "Error al cargar datos: ${e.message}") }
            }
        }
    }

    fun updateDateFilter(filter: DateFilter) {
        _dateFilter.value = filter
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    // ── Cálculos privados ─────────────────────────────────────────────────────

    /**
     * Calcula el monto de cuotas que vencen en el próximo corte de una tarjeta.
     */
    private fun calculateNextCutoffPayment(card: CreditCard, allPurchases: List<Purchase>): Double {
        val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
        val cutoffDate = nextCutoffDate(card.cutoffDay, now)
        val cardPurchases = allPurchases.filter { it.cardId == card.id }
        return cardPurchases.sumOf { purchase ->
            val due = purchase.getInstallmentsDueInCutoff(cutoffDate, card.cutoffDay)
            val unpaid = due.filter { it > purchase.paidInstallments }
            unpaid.size * purchase.installmentAmount
        }
    }

    /**
     * Proyecta cuánto se debe pagar en cuotas de tarjeta por cada uno de los
     * próximos 6 meses calendario. Útil para planear liquidez futura.
     *
     * @return Lista de [MonthlyInstallmentTotal] ordenada por mes ascendente.
     */
    private fun calculateMonthlyProjection(
        cards: List<CreditCard>,
        purchases: List<Purchase>
    ): List<MonthlyInstallmentTotal> {
        val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
        return (0 until 6).map { offset ->
            // Calcular el mes objetivo (now + offset meses)
            var targetMonth = now.monthNumber + offset
            var targetYear = now.year
            while (targetMonth > 12) { targetMonth -= 12; targetYear++ }

            // Para cada tarjeta, calcular cuotas que caen en ese mes/año
            var total = 0.0
            for (card in cards) {
                val daysInMonth = Month(targetMonth).length(isLeapYear(targetYear))
                val cutoffDay = minOf(card.cutoffDay, daysInMonth)
                val cutoffDate = LocalDate(targetYear, targetMonth, cutoffDay)

                val cardPurchases = purchases.filter { it.cardId == card.id }
                for (purchase in cardPurchases) {
                    val due = purchase.getInstallmentsDueInCutoff(cutoffDate, card.cutoffDay)
                    val unpaid = due.filter { it > purchase.paidInstallments }
                    total += unpaid.size * purchase.installmentAmount
                }
            }

            MonthlyInstallmentTotal(
                year = targetYear,
                month = targetMonth,
                total = total,
                isCurrentMonth = offset == 0
            )
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

    /** Contenedor auxiliar para el combine de 5 flujos */
    private data class Data(
        val expenses: List<Expense>,
        val incomes: List<com.finanzas.personales.domain.models.Income>,
        val cards: List<CreditCard>,
        val purchases: List<Purchase>,
        val filter: DateFilter
    )
}

// ── Modelos de UI ─────────────────────────────────────────────────────────────

data class ReportsUiState(
    // KPIs de deuda
    val totalDebt: Double = 0.0,
    val totalLimit: Double = 0.0,
    val totalAvailableCredit: Double = 0.0,
    val overallUsagePercentage: Float = 0f,
    /** Suma de cuotas que vencen en el próximo corte de cada tarjeta */
    val totalNextCutoffPayment: Double = 0.0,
    // Período (gastos de cuentas de ahorro)
    val periodTotalExpenses: Double = 0.0,
    val periodTotalIncomes: Double = 0.0,
    val periodTransactionCount: Int = 0,
    // Visualizaciones
    val cardUsageData: List<CardUsage> = emptyList(),
    val monthlyProjection: List<MonthlyInstallmentTotal> = emptyList(),
    val expensesByCategory: List<CategoryExpense> = emptyList(),
    val upcomingPayments: List<PaymentDue> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

/** Uso y deuda de una tarjeta específica */
data class CardUsage(
    val card: CreditCard,
    val usedAmount: Double,
    val availableAmount: Double,
    val usagePercentage: Float,
    /** Número de compras diferidas activas (con cuotas pendientes) */
    val activePurchasesCount: Int,
    /** Deuda total restante (todas las cuotas futuras pendientes) */
    val remainingDebt: Double,
    /** Cuotas que vencen en el próximo corte */
    val nextCutoffPayment: Double
)

/** Total de cuotas que vencen en un mes/año específico (proyección) */
data class MonthlyInstallmentTotal(
    val year: Int,
    val month: Int,
    val total: Double,
    val isCurrentMonth: Boolean
)

/** Gasto agrupado por categoría (cuentas de ahorro) */
data class CategoryExpense(
    val category: ExpenseCategory,
    val amount: Double,
    val count: Int
)

// ── DateFilter ────────────────────────────────────────────────────────────────

enum class DateFilter(val displayName: String) {
    THIS_WEEK("Esta semana"),
    THIS_MONTH("Este mes"),
    LAST_MONTH("Mes pasado"),
    LAST_3_MONTHS("Últimos 3 meses"),
    THIS_YEAR("Este año");

    fun getDateRange(): Pair<LocalDate, LocalDate> {
        val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
        return when (this) {
            THIS_WEEK -> {
                val startOfWeek = now.minus(now.dayOfWeek.ordinal, DateTimeUnit.DAY)
                startOfWeek to now
            }
            THIS_MONTH -> LocalDate(now.year, now.month, 1) to now
            LAST_MONTH -> {
                val last = now.minus(1, DateTimeUnit.MONTH)
                val start = LocalDate(last.year, last.month, 1)
                start to start.plus(1, DateTimeUnit.MONTH).minus(1, DateTimeUnit.DAY)
            }
            LAST_3_MONTHS -> now.minus(3, DateTimeUnit.MONTH) to now
            THIS_YEAR -> LocalDate(now.year, 1, 1) to now
        }
    }
}
