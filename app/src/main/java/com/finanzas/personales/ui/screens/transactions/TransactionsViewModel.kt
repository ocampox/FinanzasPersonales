package com.finanzas.personales.ui.screens.transactions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finanzas.personales.data.local.entities.ExpenseCategory
import com.finanzas.personales.data.repository.FinanceRepository
import com.finanzas.personales.domain.models.CreditCard
import com.finanzas.personales.domain.models.Expense
import com.finanzas.personales.domain.models.Purchase
import com.finanzas.personales.domain.models.Result
import com.finanzas.personales.domain.models.SavingsAccount
import com.finanzas.personales.ui.components.TransactionType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.datetime.*
import javax.inject.Inject

/**
 * ViewModel para la pantalla de transacciones
 * 
 * Gestiona la lista unificada de compras y gastos, con capacidades de
 * filtrado por tipo, fecha y categoría, así como búsqueda por descripción.
 * 
 * Requirements: 3.5, 6.4
 */
@HiltViewModel
class TransactionsViewModel @Inject constructor(
    private val repository: FinanceRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedType = MutableStateFlow<TransactionType?>(null)
    val selectedType: StateFlow<TransactionType?> = _selectedType.asStateFlow()

    private val _selectedCategory = MutableStateFlow<ExpenseCategory?>(null)
    val selectedCategory: StateFlow<ExpenseCategory?> = _selectedCategory.asStateFlow()

    private val _dateFilter = MutableStateFlow<DateFilter>(DateFilter.ALL)
    val dateFilter: StateFlow<DateFilter> = _dateFilter.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _selectedTransaction = MutableStateFlow<TransactionUiModel?>(null)
    val selectedTransaction: StateFlow<TransactionUiModel?> = _selectedTransaction.asStateFlow()

    private val _selectedPurchase = MutableStateFlow<Purchase?>(null)
    val selectedPurchase: StateFlow<Purchase?> = _selectedPurchase.asStateFlow()

    private val _selectedExpense = MutableStateFlow<Expense?>(null)
    val selectedExpense: StateFlow<Expense?> = _selectedExpense.asStateFlow()

    private val _selectedCard = MutableStateFlow<CreditCard?>(null)
    val selectedCard: StateFlow<CreditCard?> = _selectedCard.asStateFlow()

    private val _selectedAccount = MutableStateFlow<SavingsAccount?>(null)
    val selectedAccount: StateFlow<SavingsAccount?> = _selectedAccount.asStateFlow()

    private val _showTransactionActions = MutableStateFlow(false)
    val showTransactionActions: StateFlow<Boolean> = _showTransactionActions.asStateFlow()

    private val _showEditPurchaseDialog = MutableStateFlow(false)
    val showEditPurchaseDialog: StateFlow<Boolean> = _showEditPurchaseDialog.asStateFlow()

    private val _showEditExpenseDialog = MutableStateFlow(false)
    val showEditExpenseDialog: StateFlow<Boolean> = _showEditExpenseDialog.asStateFlow()

    private val _showDeleteConfirmation = MutableStateFlow(false)
    val showDeleteConfirmation: StateFlow<Boolean> = _showDeleteConfirmation.asStateFlow()

    /**
     * Flow combinado de todas las transacciones (compras + gastos)
     * ordenadas por fecha descendente
     */
    val transactions: StateFlow<List<TransactionUiModel>> = combine(
        repository.getAllPurchases(),
        repository.getAllExpenses(),
        repository.getAllCreditCards(),
        repository.getAllSavingsAccounts(),
        _searchQuery,
        _selectedType,
        _selectedCategory,
        _dateFilter
    ) { values ->
        val purchases = values[0] as List<com.finanzas.personales.domain.models.Purchase>
        val expenses = values[1] as List<com.finanzas.personales.domain.models.Expense>
        val cards = values[2] as List<com.finanzas.personales.domain.models.CreditCard>
        val accounts = values[3] as List<com.finanzas.personales.domain.models.SavingsAccount>
        val query = values[4] as String
        val type = values[5] as TransactionType?
        val category = values[6] as ExpenseCategory?
        val dateFilter = values[7] as DateFilter
        
        // Crear mapa de IDs a nombres para búsqueda rápida
        val cardNames = cards.associate { it.id to it.name }
        val accountNames = accounts.associate { it.id to it.name }
        
        // Convertir compras a TransactionUiModel
        val purchaseTransactions = purchases.mapNotNull { purchase ->
            val cardName = cardNames[purchase.cardId] ?: return@mapNotNull null
            TransactionUiModel.fromPurchase(purchase, cardName)
        }
        
        // Convertir gastos a TransactionUiModel
        val expenseTransactions = expenses.mapNotNull { expense ->
            val accountName = accountNames[expense.accountId] ?: return@mapNotNull null
            TransactionUiModel.fromExpense(expense, accountName)
        }
        
        // Combinar y ordenar por fecha (más reciente primero)
        val allTransactions = (purchaseTransactions + expenseTransactions)
            .sortedByDescending { it.date }
        
        // Aplicar filtros
        allTransactions
            .filter { transaction ->
                // Filtro de búsqueda por descripción
                if (query.isNotBlank()) {
                    transaction.description.contains(query, ignoreCase = true) ||
                    transaction.accountOrCardName.contains(query, ignoreCase = true)
                } else {
                    true
                }
            }
            .filter { transaction ->
                // Filtro por tipo
                type == null || transaction.type == type
            }
            .filter { transaction ->
                // Filtro por categoría (solo aplica a gastos)
                category == null || transaction.category == category
            }
            .filter { transaction ->
                // Filtro por fecha
                when (dateFilter) {
                    DateFilter.ALL -> true
                    DateFilter.TODAY -> isToday(transaction.date)
                    DateFilter.THIS_WEEK -> isThisWeek(transaction.date)
                    DateFilter.THIS_MONTH -> isThisMonth(transaction.date)
                    DateFilter.THIS_YEAR -> isThisYear(transaction.date)
                }
            }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    /**
     * Actualiza la consulta de búsqueda
     */
    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    /**
     * Actualiza el filtro de tipo de transacción
     */
    fun updateTypeFilter(type: TransactionType?) {
        _selectedType.value = type
    }

    /**
     * Actualiza el filtro de categoría
     */
    fun updateCategoryFilter(category: ExpenseCategory?) {
        _selectedCategory.value = category
    }

    /**
     * Actualiza el filtro de fecha
     */
    fun updateDateFilter(filter: DateFilter) {
        _dateFilter.value = filter
    }

    /**
     * Limpia todos los filtros
     */
    fun clearFilters() {
        _searchQuery.value = ""
        _selectedType.value = null
        _selectedCategory.value = null
        _dateFilter.value = DateFilter.ALL
    }

    /**
     * Limpia el mensaje de error
     */
    fun clearError() {
        _error.value = null
    }

    fun onTransactionClick(transaction: TransactionUiModel) {
        viewModelScope.launch {
            _selectedTransaction.value = transaction
            _selectedPurchase.value = null
            _selectedExpense.value = null
            _selectedCard.value = null
            _selectedAccount.value = null
            _showDeleteConfirmation.value = false
            _showEditPurchaseDialog.value = false
            _showEditExpenseDialog.value = false
            _showTransactionActions.value = true

            when (transaction.type) {
                TransactionType.PURCHASE -> {
                    val purchase = repository.getAllPurchases().first().firstOrNull { it.id == transaction.id }
                    if (purchase == null) {
                        _error.value = "No se encontró la compra"
                        _showTransactionActions.value = false
                        return@launch
                    }
                    _selectedPurchase.value = purchase

                    when (val cardResult = repository.getCreditCardById(transaction.accountOrCardId)) {
                        is Result.Success -> _selectedCard.value = cardResult.data
                        is Result.Error -> _error.value = "No se pudo cargar la tarjeta"
                    }
                }

                TransactionType.EXPENSE -> {
                    val expense = repository.getAllExpenses().first().firstOrNull { it.id == transaction.id }
                    if (expense == null) {
                        _error.value = "No se encontró el gasto"
                        _showTransactionActions.value = false
                        return@launch
                    }
                    _selectedExpense.value = expense

                    when (val accountResult = repository.getSavingsAccountById(transaction.accountOrCardId)) {
                        is Result.Success -> _selectedAccount.value = accountResult.data
                        is Result.Error -> _error.value = "No se pudo cargar la cuenta"
                    }
                }
            }
        }
    }

    fun dismissTransactionActions() {
        _showTransactionActions.value = false
        _showDeleteConfirmation.value = false
        _showEditPurchaseDialog.value = false
        _showEditExpenseDialog.value = false
    }

    fun showEditSelectedTransaction() {
        when (_selectedTransaction.value?.type) {
            TransactionType.PURCHASE -> {
                _showTransactionActions.value = false
                _showDeleteConfirmation.value = false
                _showEditPurchaseDialog.value = true
            }
            TransactionType.EXPENSE -> {
                _showTransactionActions.value = false
                _showDeleteConfirmation.value = false
                _showEditExpenseDialog.value = true
            }
            null -> Unit
        }
    }

    fun hideEditDialogs() {
        _showEditPurchaseDialog.value = false
        _showEditExpenseDialog.value = false
    }

    fun showDeleteConfirmation() {
        _showTransactionActions.value = false
        _showDeleteConfirmation.value = true
    }

    fun hideDeleteConfirmation() {
        _showDeleteConfirmation.value = false
    }

    fun updateSelectedPurchase(data: com.finanzas.personales.ui.components.PurchaseData) {
        val purchase = _selectedPurchase.value ?: return
        val card = _selectedCard.value ?: return

        if (data.installments < purchase.paidInstallments) {
            _error.value = "Las cuotas no pueden ser menores a las cuotas ya pagadas (${purchase.paidInstallments})"
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            val updatedPurchase = purchase.copy(
                description = data.description,
                totalAmount = data.totalAmount,
                installments = data.installments,
                installmentAmount = data.installmentAmount
            )

            when (val result = repository.updatePurchase(updatedPurchase)) {
                is Result.Success -> {
                    _selectedPurchase.value = updatedPurchase
                    // Refrescar tarjeta por si cambió cupo al modificar valor
                    when (val cardResult = repository.getCreditCardById(card.id)) {
                        is Result.Success -> _selectedCard.value = cardResult.data
                        is Result.Error -> Unit
                    }
                    _showEditPurchaseDialog.value = false
                    _showTransactionActions.value = false
                }
                is Result.Error -> _error.value = result.error.toString()
            }
            _isLoading.value = false
        }
    }

    fun deleteSelectedPurchase() {
        val purchase = _selectedPurchase.value ?: return
        viewModelScope.launch {
            _isLoading.value = true
            when (val result = repository.deletePurchase(purchase)) {
                is Result.Success -> {
                    _showDeleteConfirmation.value = false
                    _showTransactionActions.value = false
                }
                is Result.Error -> _error.value = result.error.toString()
            }
            _isLoading.value = false
        }
    }

    fun updateSelectedExpense(data: com.finanzas.personales.ui.components.ExpenseData) {
        val expense = _selectedExpense.value ?: return
        val account = _selectedAccount.value ?: return

        viewModelScope.launch {
            _isLoading.value = true
            val updatedExpense = expense.copy(
                description = data.description,
                amount = data.amount,
                category = data.category
            )

            when (val result = repository.updateExpense(updatedExpense)) {
                is Result.Success -> {
                    _selectedExpense.value = updatedExpense
                    // Refrescar cuenta por si cambió saldo al modificar valor
                    when (val accountResult = repository.getSavingsAccountById(account.id)) {
                        is Result.Success -> _selectedAccount.value = accountResult.data
                        is Result.Error -> Unit
                    }
                    _showEditExpenseDialog.value = false
                    _showTransactionActions.value = false
                }
                is Result.Error -> _error.value = result.error.toString()
            }
            _isLoading.value = false
        }
    }

    fun deleteSelectedExpense() {
        val expense = _selectedExpense.value ?: return
        viewModelScope.launch {
            _isLoading.value = true
            when (val result = repository.deleteExpense(expense)) {
                is Result.Success -> {
                    _showDeleteConfirmation.value = false
                    _showTransactionActions.value = false
                }
                is Result.Error -> _error.value = result.error.toString()
            }
            _isLoading.value = false
        }
    }

    // Funciones auxiliares para filtros de fecha
    
    private fun isToday(instant: Instant): Boolean {
        val now = kotlinx.datetime.Clock.System.now()
        val nowDate = now.toLocalDateTime(kotlinx.datetime.TimeZone.currentSystemDefault()).date
        val transactionDate = instant.toLocalDateTime(kotlinx.datetime.TimeZone.currentSystemDefault()).date
        return transactionDate == nowDate
    }

    private fun isThisWeek(instant: Instant): Boolean {
        val now = kotlinx.datetime.Clock.System.now()
        val nowDate = now.toLocalDateTime(kotlinx.datetime.TimeZone.currentSystemDefault()).date
        val transactionDate = instant.toLocalDateTime(kotlinx.datetime.TimeZone.currentSystemDefault()).date
        
        // Calcular el inicio de la semana (lunes)
        // dayOfWeek.ordinal: 0 = Monday, 6 = Sunday
        val weekStart = nowDate.minus(nowDate.dayOfWeek.ordinal, kotlinx.datetime.DateTimeUnit.DAY)
        
        return transactionDate >= weekStart && transactionDate <= nowDate
    }

    private fun isThisMonth(instant: Instant): Boolean {
        val now = kotlinx.datetime.Clock.System.now()
        val nowDate = now.toLocalDateTime(kotlinx.datetime.TimeZone.currentSystemDefault()).date
        val transactionDate = instant.toLocalDateTime(kotlinx.datetime.TimeZone.currentSystemDefault()).date
        
        return transactionDate.year == nowDate.year && 
               transactionDate.monthNumber == nowDate.monthNumber
    }

    private fun isThisYear(instant: Instant): Boolean {
        val now = kotlinx.datetime.Clock.System.now()
        val nowDate = now.toLocalDateTime(kotlinx.datetime.TimeZone.currentSystemDefault()).date
        val transactionDate = instant.toLocalDateTime(kotlinx.datetime.TimeZone.currentSystemDefault()).date
        
        return transactionDate.year == nowDate.year
    }
}

/**
 * Filtros de fecha disponibles
 */
enum class DateFilter {
    ALL,
    TODAY,
    THIS_WEEK,
    THIS_MONTH,
    THIS_YEAR
}
