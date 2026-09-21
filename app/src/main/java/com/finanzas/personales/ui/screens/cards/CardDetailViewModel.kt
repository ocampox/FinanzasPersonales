package com.finanzas.personales.ui.screens.cards

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finanzas.personales.data.repository.FinanceRepository
import com.finanzas.personales.domain.models.CardPayment
import com.finanzas.personales.domain.models.CreditCard
import com.finanzas.personales.domain.models.FinanceError
import com.finanzas.personales.domain.models.Purchase
import com.finanzas.personales.domain.models.Result
import com.finanzas.personales.domain.usecases.AddPurchaseUseCase
import com.finanzas.personales.domain.usecases.AddPaymentUseCase
import com.finanzas.personales.domain.usecases.CalculateMinimumPaymentUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.datetime.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import javax.inject.Inject

/**
 * ViewModel para la pantalla de detalle de tarjeta de crédito
 * 
 * Gestiona el estado de la UI y coordina la obtención de datos
 * de la tarjeta y sus compras asociadas.
 * 
 * Requirements: 1.4, 3.5, 7.2
 */
@HiltViewModel
class CardDetailViewModel @Inject constructor(
    private val repository: FinanceRepository,
    private val addPurchaseUseCase: AddPurchaseUseCase,
    private val addPaymentUseCase: AddPaymentUseCase,
    private val calculateMinimumPaymentUseCase: CalculateMinimumPaymentUseCase,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val cardId: Long = savedStateHandle.get<Long>("cardId")
        ?: throw IllegalArgumentException("cardId is required")

    private val _uiState = MutableStateFlow(CardDetailUiState())
    val uiState: StateFlow<CardDetailUiState> = _uiState.asStateFlow()

    private val _purchaseFilter = MutableStateFlow(PurchaseFilter.ALL)

    init {
        loadCardDetails()
        observePurchases()
        observePayments()
        loadMinimumPayment()
    }

    /**
     * Carga los detalles de la tarjeta
     */
    private fun loadCardDetails() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            when (val result = repository.getCreditCardById(cardId)) {
                is Result.Success -> {
                    val card = result.data
                    _uiState.update { 
                        it.copy(
                            creditCard = card,
                            isLoading = false
                        )
                    }
                    // Cargar el pago mínimo después de cargar la tarjeta
                    loadMinimumPayment()
                }
                is Result.Error -> {
                    _uiState.update { 
                        it.copy(
                            isLoading = false,
                            error = "Error al cargar la tarjeta"
                        )
                    }
                }
            }
        }
    }
    
    /**
     * Calcula y carga el pago mínimo para el próximo corte
     */
    private fun loadMinimumPayment() {
        viewModelScope.launch {
            val card = _uiState.value.creditCard ?: return@launch
            
            // Calcular la fecha del próximo corte
            val nextCutoffDate = calculateNextCutoffDate(card.cutoffDay)
            
            when (val result = calculateMinimumPaymentUseCase(cardId, nextCutoffDate)) {
                is Result.Success -> {
                    _uiState.update { 
                        it.copy(
                            minimumPayment = result.data,
                            nextCutoffDate = nextCutoffDate
                        )
                    }
                }
                is Result.Error -> {
                    // No mostrar error, solo no actualizar el pago mínimo
                    _uiState.update { 
                        it.copy(
                            minimumPayment = null,
                            nextCutoffDate = nextCutoffDate
                        )
                    }
                }
            }
        }
    }
    
    /**
     * Calcula la fecha del próximo corte basándose en el día de corte de la tarjeta
     */
    private fun calculateNextCutoffDate(cutoffDay: Int): LocalDate {
        val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
        val currentDay = now.dayOfMonth
        
        // Si ya pasó el día de corte este mes, el próximo corte es el próximo mes
        val nextCutoffMonth = if (currentDay >= cutoffDay) {
            now.monthNumber + 1
        } else {
            now.monthNumber
        }
        
        var nextCutoffYear = now.year
        var actualMonth = nextCutoffMonth
        
        // Ajustar año si el mes excede 12
        if (actualMonth > 12) {
            actualMonth = 1
            nextCutoffYear += 1
        }
        
        // Asegurar que el día de corte no exceda los días del mes
        val daysInMonth = Month(actualMonth).length(isLeapYear(nextCutoffYear))
        val actualCutoffDay = minOf(cutoffDay, daysInMonth)
        
        return LocalDate(nextCutoffYear, actualMonth, actualCutoffDay)
    }
    
    /**
     * Verifica si un año es bisiesto
     */
    private fun isLeapYear(year: Int): Boolean {
        return (year % 4 == 0 && year % 100 != 0) || (year % 400 == 0)
    }

    /**
     * Observa las compras de la tarjeta en tiempo real y aplica el filtro activo
     */
    private fun observePurchases() {
        viewModelScope.launch {
            combine(
                repository.getPurchasesByCard(cardId),
                _purchaseFilter
            ) { purchases, filter ->
                val filtered = when (filter) {
                    PurchaseFilter.ALL -> purchases
                    PurchaseFilter.PENDING -> purchases.filter { !it.isFullyPaid() }
                    PurchaseFilter.PAID -> purchases.filter { it.isFullyPaid() }
                    PurchaseFilter.INSTALLMENTS -> purchases.filter { it.installments > 1 }
                    PurchaseFilter.INSTALLMENTS_PENDING -> purchases.filter {
                        it.installments > 1 && !it.isFullyPaid()
                    }
                }
                Pair(purchases, filtered)
            }
                .catch { exception ->
                    _uiState.update {
                        it.copy(error = exception.message ?: "Error al cargar compras")
                    }
                }
                .collect { (all, filtered) ->
                    val purchasesChanged = _uiState.value.purchases != all
                    _uiState.update {
                        it.copy(
                            purchases = all,
                            filteredPurchases = filtered,
                            purchaseFilter = _purchaseFilter.value
                        )
                    }

                    // La reparación de cuotas puede ejecutarse desde Ajustes
                    // mientras esta pantalla permanece en el back stack. Cuando
                    // cambian las compras, se vuelve a leer la tarjeta y su pago
                    // mínimo para no mostrar una proyección anterior al ajuste.
                    if (purchasesChanged) {
                        loadCardDetails()
                    }
                }
        }
    }

    /**
     * Actualiza el filtro del historial de compras
     */
    fun updatePurchaseFilter(filter: PurchaseFilter) {
        _purchaseFilter.value = filter
    }

    /**
     * Limpia el filtro del historial (vuelve a "Todas")
     */
    fun clearPurchaseFilter() {
        _purchaseFilter.value = PurchaseFilter.ALL
    }

    /**
     * Muestra el diálogo para agregar una compra
     */
    fun showAddPurchaseDialog() {
        _uiState.update { it.copy(showAddPurchaseDialog = true) }
    }

    /**
     * Oculta el diálogo de agregar compra
     */
    fun hideAddPurchaseDialog() {
        _uiState.update { it.copy(showAddPurchaseDialog = false) }
    }

    /**
     * Muestra el diálogo para agregar un abono
     */
    fun showAddPaymentDialog() {
        _uiState.update { it.copy(showAddPaymentDialog = true) }
    }

    /**
     * Oculta el diálogo de agregar abono
     */
    fun hideAddPaymentDialog() {
        _uiState.update { it.copy(showAddPaymentDialog = false) }
    }

    /**
     * Muestra el diálogo de confirmación para eliminar la tarjeta
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
     * Elimina la tarjeta
     */
    fun deleteCard() {
        viewModelScope.launch {
            val card = _uiState.value.creditCard ?: return@launch
            
            _uiState.update { it.copy(isDeleting = true) }

            when (val result = repository.deleteCreditCard(card)) {
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
                            error = "Error al eliminar la tarjeta"
                        )
                    }
                }
            }
        }
    }

    /**
     * Recarga los datos
     */
    fun refresh() {
        loadCardDetails()
    }

    /**
     * Agrega una nueva compra a la tarjeta
     */
    fun addPurchase(
        description: String,
        totalAmount: Double,
        installments: Int,
        installmentAmount: Double
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSavingPurchase = true) }

            val purchase = Purchase(
                cardId = cardId,
                description = description,
                totalAmount = totalAmount,
                installments = installments,
                installmentAmount = installmentAmount,
                purchaseDate = Clock.System.now()
            )

            when (val result = addPurchaseUseCase(purchase)) {
                is Result.Success -> {
                    _uiState.update { 
                        it.copy(
                            isSavingPurchase = false,
                            showAddPurchaseDialog = false
                        )
                    }
                    // Recargar los datos de la tarjeta para actualizar el cupo disponible
                    loadCardDetails()
                    // Recargar el pago mínimo para reflejar la nueva compra
                    loadMinimumPayment()
                }
                is Result.Error -> {
                    val errorMessage = when (result.error) {
                        is FinanceError.InsufficientCredit -> {
                            val error = result.error as FinanceError.InsufficientCredit
                            "Cupo insuficiente. Disponible: ${formatAmount(error.available)}, Requerido: ${formatAmount(error.required)}"
                        }
                        is FinanceError.InvalidAmount -> {
                            (result.error as FinanceError.InvalidAmount).message
                        }
                        is FinanceError.ValidationError -> {
                            (result.error as FinanceError.ValidationError).message
                        }
                        else -> "Error al agregar la compra"
                    }
                    _uiState.update { 
                        it.copy(
                            isSavingPurchase = false,
                            error = errorMessage
                        )
                    }
                }
            }
        }
    }

    /**
     * Formatea un monto para mostrar en mensajes
     */
    private fun formatAmount(amount: Double): String {
        return java.text.NumberFormat.getCurrencyInstance(java.util.Locale("es", "CO")).format(amount)
    }

    /**
     * Agrega un abono a la tarjeta
     */
    fun addPayment(amount: Double) {
        viewModelScope.launch {
            _uiState.update { it.copy(isAddingPayment = true) }

            when (val result = addPaymentUseCase(cardId, amount)) {
                is Result.Success -> {
                    _uiState.update { 
                        it.copy(
                            isAddingPayment = false,
                            showAddPaymentDialog = false
                        )
                    }
                    // Recargar los datos de la tarjeta para actualizar el cupo disponible y las cuotas
                    loadCardDetails()
                    observePurchases() // Recargar compras para actualizar cuotas pagadas
                    loadMinimumPayment() // Recargar el pago mínimo
                }
                is Result.Error -> {
                    val errorMessage = when (result.error) {
                        is FinanceError.InvalidAmount -> {
                            (result.error as FinanceError.InvalidAmount).message
                        }
                        is FinanceError.ValidationError -> {
                            (result.error as FinanceError.ValidationError).message
                        }
                        is FinanceError.NotFound -> {
                            "Tarjeta no encontrada"
                        }
                        else -> "Error al agregar el abono"
                    }
                    _uiState.update { 
                        it.copy(
                            isAddingPayment = false,
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
     * Muestra el diálogo de edición de compra
     */
    fun showEditPurchaseDialog(purchase: Purchase) {
        _uiState.update { 
            it.copy(
                showEditPurchaseDialog = true,
                purchaseToEdit = purchase
            ) 
        }
    }

    /**
     * Oculta el diálogo de edición de compra
     */
    fun hideEditPurchaseDialog() {
        _uiState.update { 
            it.copy(
                showEditPurchaseDialog = false,
                purchaseToEdit = null
            ) 
        }
    }

    /**
     * Actualiza una compra existente
     */
    fun updatePurchase(
        purchaseId: Long,
        description: String,
        totalAmount: Double,
        installments: Int,
        installmentAmount: Double
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSavingPurchase = true) }

            val purchase = Purchase(
                id = purchaseId,
                cardId = cardId,
                description = description,
                totalAmount = totalAmount,
                installments = installments,
                installmentAmount = installmentAmount,
                purchaseDate = _uiState.value.purchaseToEdit?.purchaseDate ?: Clock.System.now(),
                paidInstallments = _uiState.value.purchaseToEdit?.paidInstallments ?: 0
            )

            when (val result = repository.updatePurchase(purchase)) {
                is Result.Success -> {
                    _uiState.update { 
                        it.copy(
                            isSavingPurchase = false,
                            showEditPurchaseDialog = false,
                            purchaseToEdit = null
                        )
                    }
                    // Recargar los datos de la tarjeta para actualizar el cupo disponible
                    loadCardDetails()
                    // Recargar el pago mínimo para reflejar la compra actualizada
                    loadMinimumPayment()
                }
                is Result.Error -> {
                    val errorMessage = when (result.error) {
                        is FinanceError.InsufficientCredit -> {
                            val error = result.error as FinanceError.InsufficientCredit
                            "Cupo insuficiente. Disponible: ${formatAmount(error.available)}, Requerido: ${formatAmount(error.required)}"
                        }
                        is FinanceError.InvalidAmount -> {
                            (result.error as FinanceError.InvalidAmount).message
                        }
                        is FinanceError.ValidationError -> {
                            (result.error as FinanceError.ValidationError).message
                        }
                        else -> "Error al actualizar la compra"
                    }
                    _uiState.update { 
                        it.copy(
                            isSavingPurchase = false,
                            error = errorMessage
                        )
                    }
                }
            }
        }
    }

    /**
     * Muestra el diálogo de confirmación para eliminar una compra
     */
    fun showDeletePurchaseConfirmation(purchase: Purchase) {
        _uiState.update { 
            it.copy(
                showDeletePurchaseConfirmation = true,
                purchaseToDelete = purchase
            ) 
        }
    }

    /**
     * Oculta el diálogo de confirmación de eliminación de compra
     */
    fun hideDeletePurchaseConfirmation() {
        _uiState.update { 
            it.copy(
                showDeletePurchaseConfirmation = false,
                purchaseToDelete = null
            ) 
        }
    }

    /**
     * Elimina una compra
     */
    fun deletePurchase() {
        viewModelScope.launch {
            val purchase = _uiState.value.purchaseToDelete ?: return@launch
            _uiState.update { it.copy(isDeleting = true) }

            when (val result = repository.deletePurchase(purchase)) {
                is Result.Success -> {
                    _uiState.update { 
                        it.copy(
                            isDeleting = false,
                            showDeletePurchaseConfirmation = false,
                            purchaseToDelete = null
                        )
                    }
                    // Recargar los datos de la tarjeta para actualizar el cupo disponible
                    loadCardDetails()
                    // Recargar el pago mínimo
                    loadMinimumPayment()
                }
                is Result.Error -> {
                    _uiState.update { 
                        it.copy(
                            isDeleting = false,
                            error = "Error al eliminar la compra"
                        )
                    }
                }
            }
        }
    }

    /**
     * Observa el historial de pagos de la tarjeta en tiempo real
     */
    private fun observePayments() {
        viewModelScope.launch {
            repository.getPaymentsByCard(cardId)
                .catch { exception ->
                    _uiState.update {
                        it.copy(error = exception.message ?: "Error al cargar historial de pagos")
                    }
                }
                .collect { payments ->
                    _uiState.update { it.copy(payments = payments) }
                }
        }
    }

}

/**
 * Estado de la UI para la pantalla de detalle de tarjeta
 */
data class CardDetailUiState(
    val creditCard: CreditCard? = null,
    val purchases: List<Purchase> = emptyList(),
    val filteredPurchases: List<Purchase> = emptyList(),
    val purchaseFilter: PurchaseFilter = PurchaseFilter.ALL,
    val payments: List<CardPayment> = emptyList(),
    val showAddPurchaseDialog: Boolean = false,
    val showEditPurchaseDialog: Boolean = false,
    val showAddPaymentDialog: Boolean = false,
    val showDeleteConfirmation: Boolean = false,
    val showDeletePurchaseConfirmation: Boolean = false,
    val purchaseToEdit: Purchase? = null,
    val purchaseToDelete: Purchase? = null,
    val isLoading: Boolean = false,
    val isDeleting: Boolean = false,
    val isSavingPurchase: Boolean = false,
    val isAddingPayment: Boolean = false,
    val deleteSuccess: Boolean = false,
    val minimumPayment: Double? = null,
    val nextCutoffDate: LocalDate? = null,
    val error: String? = null
)

/** Opciones de filtro para el historial de compras */
enum class PurchaseFilter(val label: String) {
    ALL("Todas"),
    PENDING("Con cuotas pendientes"),
    PAID("Pagadas"),
    INSTALLMENTS("Diferidas (>1 cuota)"),
    INSTALLMENTS_PENDING("Diferidas y pendientes")
}
