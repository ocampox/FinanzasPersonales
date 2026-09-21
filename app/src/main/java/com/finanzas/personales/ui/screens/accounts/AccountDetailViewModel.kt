package com.finanzas.personales.ui.screens.accounts

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finanzas.personales.data.repository.FinanceRepository
import com.finanzas.personales.domain.models.Expense
import com.finanzas.personales.domain.models.FinanceError
import com.finanzas.personales.domain.models.Income
import com.finanzas.personales.domain.models.Result
import com.finanzas.personales.domain.models.SavingsAccount
import com.finanzas.personales.domain.usecases.AddExpenseUseCase
import com.finanzas.personales.domain.usecases.AddIncomeUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel para la pantalla de detalle de cuenta de ahorros
 * 
 * Gestiona el estado de la UI, carga los datos de la cuenta y sus gastos,
 * y coordina las operaciones de agregar gastos, editar y eliminar cuenta.
 * 
 * Requirements: 2.4, 6.4, 7.3
 */
@HiltViewModel
class AccountDetailViewModel @Inject constructor(
    private val repository: FinanceRepository,
    private val addExpenseUseCase: AddExpenseUseCase,
    private val addIncomeUseCase: AddIncomeUseCase,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val accountId: Long = checkNotNull(savedStateHandle.get<Long>("accountId")) {
        "accountId is required"
    }

    private val _uiState = MutableStateFlow(AccountDetailUiState())
    val uiState: StateFlow<AccountDetailUiState> = _uiState.asStateFlow()

    init {
        observeAccountData()
    }

    /**
     * Observa la cuenta, sus gastos e ingresos en tiempo real.
     * Cualquier cambio (nuevo gasto, ingreso, actualización de saldo) se refleja
     * automáticamente en la UI sin necesidad de recargar manualmente.
     */
    private fun observeAccountData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            combine(
                repository.getSavingsAccountByIdFlow(accountId),
                repository.getExpensesByAccount(accountId),
                repository.getIncomesByAccount(accountId)
            ) { account, expenses, incomes ->
                Triple(account, expenses, incomes)
            }
                .catch { exception ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = exception.message ?: "Error al cargar datos de la cuenta"
                        )
                    }
                }
                .collect { (account, expenses, incomes) ->
                    _uiState.update {
                        it.copy(
                            savingsAccount = account,
                            expenses = expenses,
                            incomes = incomes,
                            isLoading = false,
                            error = null
                        )
                    }
                }
        }
    }

    /**
     * Muestra el diálogo de confirmación de eliminación
     */
    fun showDeleteConfirmation() {
        _uiState.update { it.copy(showDeleteConfirmation = true) }
    }

    /**
     * Oculta el diálogo de confirmación de eliminación
     */
    fun hideDeleteConfirmation() {
        _uiState.update { it.copy(showDeleteConfirmation = false) }
    }

    /**
     * Elimina la cuenta
     */
    fun deleteAccount() {
        val account = _uiState.value.savingsAccount ?: return

        viewModelScope.launch {
            _uiState.update { it.copy(isDeleting = true) }

            when (repository.deleteSavingsAccount(account)) {
                is Result.Success -> {
                    _uiState.update {
                        it.copy(
                            isDeleting = false,
                            deleteSuccess = true
                        )
                    }
                }
                is Result.Error -> {
                    _uiState.update {
                        it.copy(
                            isDeleting = false,
                            showDeleteConfirmation = false,
                            error = "Error al eliminar la cuenta"
                        )
                    }
                }
            }
        }
    }

    /**
     * Muestra el diálogo de agregar gasto
     */
    fun showAddExpenseDialog() {
        _uiState.update { it.copy(showAddExpenseDialog = true) }
    }

    /**
     * Oculta el diálogo de agregar gasto
     */
    fun hideAddExpenseDialog() {
        _uiState.update { it.copy(showAddExpenseDialog = false) }
    }

    /**
     * Agrega un nuevo gasto a la cuenta
     */
    fun addExpense(expense: Expense) {
        viewModelScope.launch {
            when (val result = addExpenseUseCase(expense)) {
                is Result.Success -> {
                    _uiState.update { it.copy(showAddExpenseDialog = false) }
                }
                is Result.Error -> {
                    val errorMessage = when (result.error) {
                        is FinanceError.InsufficientFunds -> {
                            val error = result.error as FinanceError.InsufficientFunds
                            "Saldo insuficiente. Disponible: ${formatCurrency(error.available)}, Requerido: ${formatCurrency(error.required)}"
                        }
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
                        else -> "Error al agregar el gasto"
                    }
                    _uiState.update {
                        it.copy(
                            showAddExpenseDialog = false,
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

    /**
     * Formatea un monto como moneda
     */
    private fun formatCurrency(amount: Double): String {
        return java.text.NumberFormat.getCurrencyInstance(java.util.Locale("es", "CO")).format(amount)
    }

    /**
     * Muestra el diálogo de edición de gasto
     */
    fun showEditExpenseDialog(expense: Expense) {
        _uiState.update { 
            it.copy(
                showEditExpenseDialog = true,
                expenseToEdit = expense
            ) 
        }
    }

    /**
     * Oculta el diálogo de edición de gasto
     */
    fun hideEditExpenseDialog() {
        _uiState.update { 
            it.copy(
                showEditExpenseDialog = false,
                expenseToEdit = null
            ) 
        }
    }

    /**
     * Actualiza un gasto existente
     */
    fun updateExpense(
        expenseId: Long,
        description: String,
        amount: Double,
        category: com.finanzas.personales.data.local.entities.ExpenseCategory
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSavingExpense = true) }

            val expense = Expense(
                id = expenseId,
                accountId = accountId,
                description = description,
                amount = amount,
                category = category,
                expenseDate = _uiState.value.expenseToEdit?.expenseDate ?: kotlinx.datetime.Clock.System.now()
            )

            when (val result = repository.updateExpense(expense)) {
                is Result.Success -> {
                    _uiState.update { 
                        it.copy(
                            isSavingExpense = false,
                            showEditExpenseDialog = false,
                            expenseToEdit = null
                        )
                    }
                }
                is Result.Error -> {
                    val errorMessage = when (result.error) {
                        is FinanceError.InsufficientCredit -> {
                            val error = result.error as FinanceError.InsufficientCredit
                            "Saldo insuficiente. Disponible: ${formatCurrency(error.available)}, Requerido: ${formatCurrency(error.required)}"
                        }
                        is FinanceError.InvalidAmount -> {
                            (result.error as FinanceError.InvalidAmount).message
                        }
                        is FinanceError.ValidationError -> {
                            (result.error as FinanceError.ValidationError).message
                        }
                        else -> "Error al actualizar el gasto"
                    }
                    _uiState.update { 
                        it.copy(
                            isSavingExpense = false,
                            error = errorMessage
                        )
                    }
                }
            }
        }
    }

    /**
     * Muestra el diálogo de confirmación para eliminar un gasto
     */
    fun showDeleteExpenseConfirmation(expense: Expense) {
        _uiState.update { 
            it.copy(
                showDeleteExpenseConfirmation = true,
                expenseToDelete = expense
            ) 
        }
    }

    /**
     * Oculta el diálogo de confirmación de eliminación de gasto
     */
    fun hideDeleteExpenseConfirmation() {
        _uiState.update { 
            it.copy(
                showDeleteExpenseConfirmation = false,
                expenseToDelete = null
            ) 
        }
    }

    /**
     * Elimina un gasto
     */
    fun deleteExpense() {
        viewModelScope.launch {
            val expense = _uiState.value.expenseToDelete ?: return@launch
            _uiState.update { it.copy(isDeleting = true) }

            when (val result = repository.deleteExpense(expense)) {
                is Result.Success -> {
                    _uiState.update { 
                        it.copy(
                            isDeleting = false,
                            showDeleteExpenseConfirmation = false,
                            expenseToDelete = null
                        )
                    }
                }
                is Result.Error -> {
                    _uiState.update { 
                        it.copy(
                            isDeleting = false,
                            error = "Error al eliminar el gasto"
                        )
                    }
                }
            }
        }
    }

    // ==================== Métodos para Ingresos ====================

    /**
     * Muestra el diálogo de agregar ingreso
     */
    fun showAddIncomeDialog() {
        _uiState.update { it.copy(showAddIncomeDialog = true) }
    }

    /**
     * Oculta el diálogo de agregar ingreso
     */
    fun hideAddIncomeDialog() {
        _uiState.update { it.copy(showAddIncomeDialog = false) }
    }

    /**
     * Agrega un nuevo ingreso a la cuenta
     */
    fun addIncome(income: Income) {
        viewModelScope.launch {
            when (val result = addIncomeUseCase(income)) {
                is Result.Success -> {
                    _uiState.update { it.copy(showAddIncomeDialog = false) }
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
                        else -> "Error al agregar el ingreso"
                    }
                    _uiState.update {
                        it.copy(
                            showAddIncomeDialog = false,
                            error = errorMessage
                        )
                    }
                }
            }
        }
    }
}

/**
 * Estado de la UI para la pantalla de detalle de cuenta
 */
data class AccountDetailUiState(
    val savingsAccount: SavingsAccount? = null,
    val expenses: List<Expense> = emptyList(),
    val incomes: List<Income> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val showDeleteConfirmation: Boolean = false,
    val showDeleteExpenseConfirmation: Boolean = false,
    val showEditExpenseDialog: Boolean = false,
    val expenseToEdit: Expense? = null,
    val expenseToDelete: Expense? = null,
    val isDeleting: Boolean = false,
    val isSavingExpense: Boolean = false,
    val deleteSuccess: Boolean = false,
    val showAddExpenseDialog: Boolean = false,
    val showAddIncomeDialog: Boolean = false
)
