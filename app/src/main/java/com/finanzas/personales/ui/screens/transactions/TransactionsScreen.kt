package com.finanzas.personales.ui.screens.transactions

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.finanzas.personales.data.local.entities.ExpenseCategory
import com.finanzas.personales.ui.components.AddExpenseDialog
import com.finanzas.personales.ui.components.AddPurchaseDialog
import com.finanzas.personales.ui.components.AnimatedListItem
import com.finanzas.personales.ui.components.TransactionItem
import com.finanzas.personales.ui.components.TransactionType
import java.text.NumberFormat
import java.util.Locale

/**
 * Pantalla de transacciones
 * 
 * Muestra una lista unificada de todas las compras y gastos, con capacidades de:
 * - Búsqueda por descripción
 * - Filtrado por tipo (compra/gasto)
 * - Filtrado por categoría (para gastos)
 * - Filtrado por fecha
 * - Diferenciación visual entre compras y gastos
 * 
 * Requirements: 3.5, 6.4
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionsScreen(
    viewModel: TransactionsViewModel = hiltViewModel(),
    onNavigateToCardDetail: (Long) -> Unit = {},
    onNavigateToAccountDetail: (Long) -> Unit = {},
    onNavigateToSettings: () -> Unit = {}
) {
    val transactions by viewModel.transactions.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedType by viewModel.selectedType.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val dateFilter by viewModel.dateFilter.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    val showTransactionActions by viewModel.showTransactionActions.collectAsState()
    val selectedTransaction by viewModel.selectedTransaction.collectAsState()
    val selectedPurchase by viewModel.selectedPurchase.collectAsState()
    val selectedExpense by viewModel.selectedExpense.collectAsState()
    val selectedCard by viewModel.selectedCard.collectAsState()
    val selectedAccount by viewModel.selectedAccount.collectAsState()
    val showEditPurchaseDialog by viewModel.showEditPurchaseDialog.collectAsState()
    val showEditExpenseDialog by viewModel.showEditExpenseDialog.collectAsState()
    val showDeleteConfirmation by viewModel.showDeleteConfirmation.collectAsState()

    var showFilters by remember { mutableStateOf(false) }

    if (showTransactionActions && selectedTransaction != null) {
        TransactionActionsDialog(
            transaction = selectedTransaction!!,
            purchase = selectedPurchase,
            expense = selectedExpense,
            onDismiss = { viewModel.dismissTransactionActions() },
            onViewDetail = {
                when (selectedTransaction!!.type) {
                    TransactionType.PURCHASE -> onNavigateToCardDetail(selectedTransaction!!.accountOrCardId)
                    TransactionType.EXPENSE -> onNavigateToAccountDetail(selectedTransaction!!.accountOrCardId)
                }
                viewModel.dismissTransactionActions()
            },
            onEdit = { viewModel.showEditSelectedTransaction() },
            onDelete = { viewModel.showDeleteConfirmation() }
        )
    }

    if (showEditPurchaseDialog && selectedPurchase != null && selectedCard != null) {
        AddPurchaseDialog(
            cardName = selectedCard!!.name,
            availableCredit = selectedCard!!.availableLimit,
            purchase = selectedPurchase,
            onDismiss = { viewModel.hideEditDialogs() },
            onConfirm = { data -> viewModel.updateSelectedPurchase(data) }
        )
    }

    if (showEditExpenseDialog && selectedExpense != null && selectedAccount != null) {
        AddExpenseDialog(
            accountName = selectedAccount!!.name,
            availableBalance = selectedAccount!!.balance,
            expense = selectedExpense,
            onDismiss = { viewModel.hideEditDialogs() },
            onConfirm = { data -> viewModel.updateSelectedExpense(data) }
        )
    }

    if (showDeleteConfirmation && selectedTransaction != null) {
        AlertDialog(
            onDismissRequest = { viewModel.hideDeleteConfirmation() },
            title = {
                Text(
                    text = if (selectedTransaction!!.type == TransactionType.PURCHASE) {
                        "Eliminar compra"
                    } else {
                        "Eliminar gasto"
                    }
                )
            },
            text = { Text("¿Seguro que deseas eliminar esta transacción?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        when (selectedTransaction!!.type) {
                            TransactionType.PURCHASE -> viewModel.deleteSelectedPurchase()
                            TransactionType.EXPENSE -> viewModel.deleteSelectedExpense()
                        }
                    }
                ) { Text("Eliminar") }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.hideDeleteConfirmation() }) { Text("Cancelar") }
            }
        )
    }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = { Text("Movimientos") },
                    actions = {
                        // Botón para mostrar/ocultar filtros
                        IconButton(onClick = { showFilters = !showFilters }) {
                            Icon(
                                imageVector = if (showFilters) Icons.Default.FilterListOff else Icons.Default.FilterList,
                                contentDescription = if (showFilters) "Ocultar filtros" else "Mostrar filtros"
                            )
                        }
                        
                        // Botón para limpiar filtros
                        if (selectedType != null || selectedCategory != null || 
                            dateFilter != DateFilter.ALL || searchQuery.isNotBlank()) {
                            IconButton(onClick = { viewModel.clearFilters() }) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "Limpiar filtros"
                                )
                            }
                        }
                        
                        // Botón de ajustes
                        IconButton(onClick = onNavigateToSettings) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Ajustes"
                            )
                        }
                    }
                )
                
                // Barra de búsqueda
                SearchBar(
                    query = searchQuery,
                    onQueryChange = { viewModel.updateSearchQuery(it) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                )
                
                // Panel de filtros expandible
                AnimatedVisibility(
                    visible = showFilters,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    FiltersPanel(
                        selectedType = selectedType,
                        selectedCategory = selectedCategory,
                        dateFilter = dateFilter,
                        onTypeSelected = { viewModel.updateTypeFilter(it) },
                        onCategorySelected = { viewModel.updateCategoryFilter(it) },
                        onDateFilterSelected = { viewModel.updateDateFilter(it) }
                    )
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                isLoading -> {
                    LoadingContent()
                }
                error != null -> {
                    ErrorContent(
                        error = error!!,
                        onDismiss = { viewModel.clearError() }
                    )
                }
                transactions.isEmpty() -> {
                    EmptyContent(
                        hasFilters = selectedType != null || selectedCategory != null || 
                                   dateFilter != DateFilter.ALL || searchQuery.isNotBlank()
                    )
                }
                else -> {
                    TransactionsList(
                        transactions = transactions,
                        onTransactionClick = { transaction -> viewModel.onTransactionClick(transaction) }
                    )
                }
            }
        }
    }
}

@Composable
private fun TransactionActionsDialog(
    transaction: TransactionUiModel,
    purchase: com.finanzas.personales.domain.models.Purchase?,
    expense: com.finanzas.personales.domain.models.Expense?,
    onDismiss: () -> Unit,
    onViewDetail: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (transaction.type == TransactionType.PURCHASE) {
                    "Compra"
                } else {
                    "Gasto"
                }
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(text = transaction.description, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = "${transaction.accountOrCardName} • ${formatCurrency(transaction.amount)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (transaction.type == TransactionType.PURCHASE && purchase != null) {
                    Text(
                        text = "Cuotas: ${purchase.installments}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "Valor cuota: ${formatCurrency(purchase.installmentAmount)}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "Pagadas: ${purchase.paidInstallments}/${purchase.installments}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (transaction.type == TransactionType.EXPENSE && expense?.category != null) {
                    Text(
                        text = "Categoría: ${formatCategory(expense.category)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = onViewDetail) { Text("Ver") }
                TextButton(onClick = onEdit) { Text("Editar") }
                TextButton(
                    onClick = onDelete,
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) { Text("Eliminar") }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cerrar") }
        }
    )
}

private fun formatCurrency(amount: Double): String {
    return NumberFormat.getCurrencyInstance(Locale("es", "CO")).format(amount)
}

private fun formatCategory(category: ExpenseCategory): String {
    return when (category) {
        ExpenseCategory.FOOD -> "Alimentación"
        ExpenseCategory.TRANSPORT -> "Transporte"
        ExpenseCategory.ENTERTAINMENT -> "Entretenimiento"
        ExpenseCategory.SERVICES -> "Servicios"
        ExpenseCategory.SHOPPING -> "Compras"
        ExpenseCategory.HEALTH -> "Salud"
        ExpenseCategory.OTHER -> "Otro"
    }
}

/**
 * Barra de búsqueda
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier,
        placeholder = { Text("Buscar por descripción...") },
        leadingIcon = {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = null
            )
        },
        trailingIcon = {
            if (query.isNotBlank()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Limpiar búsqueda"
                    )
                }
            }
        },
        singleLine = true,
        shape = MaterialTheme.shapes.medium
    )
}

/**
 * Panel de filtros
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FiltersPanel(
    selectedType: TransactionType?,
    selectedCategory: ExpenseCategory?,
    dateFilter: DateFilter,
    onTypeSelected: (TransactionType?) -> Unit,
    onCategorySelected: (ExpenseCategory?) -> Unit,
    onDateFilterSelected: (DateFilter) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Filtro por tipo
        Text(
            text = "Tipo de transacción",
            style = MaterialTheme.typography.labelLarge
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = selectedType == null,
                onClick = { onTypeSelected(null) },
                label = { Text("Todas") }
            )
            FilterChip(
                selected = selectedType == TransactionType.PURCHASE,
                onClick = { 
                    onTypeSelected(
                        if (selectedType == TransactionType.PURCHASE) null 
                        else TransactionType.PURCHASE
                    )
                },
                label = { Text("Compras") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.CreditCard,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                }
            )
            FilterChip(
                selected = selectedType == TransactionType.EXPENSE,
                onClick = { 
                    onTypeSelected(
                        if (selectedType == TransactionType.EXPENSE) null 
                        else TransactionType.EXPENSE
                    )
                },
                label = { Text("Gastos") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.AccountBalance,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                }
            )
        }

        // Filtro por categoría (solo para gastos)
        if (selectedType == TransactionType.EXPENSE || selectedType == null) {
            Text(
                text = "Categoría",
                style = MaterialTheme.typography.labelLarge
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = selectedCategory == null,
                    onClick = { onCategorySelected(null) },
                    label = { Text("Todas") }
                )
            }
            // Categorías en filas
            CategoryFilters(
                selectedCategory = selectedCategory,
                onCategorySelected = onCategorySelected
            )
        }

        // Filtro por fecha
        Text(
            text = "Período",
            style = MaterialTheme.typography.labelLarge
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = dateFilter == DateFilter.ALL,
                onClick = { onDateFilterSelected(DateFilter.ALL) },
                label = { Text("Todo") }
            )
            FilterChip(
                selected = dateFilter == DateFilter.TODAY,
                onClick = { onDateFilterSelected(DateFilter.TODAY) },
                label = { Text("Hoy") }
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = dateFilter == DateFilter.THIS_WEEK,
                onClick = { onDateFilterSelected(DateFilter.THIS_WEEK) },
                label = { Text("Esta semana") }
            )
            FilterChip(
                selected = dateFilter == DateFilter.THIS_MONTH,
                onClick = { onDateFilterSelected(DateFilter.THIS_MONTH) },
                label = { Text("Este mes") }
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = dateFilter == DateFilter.THIS_YEAR,
                onClick = { onDateFilterSelected(DateFilter.THIS_YEAR) },
                label = { Text("Este año") }
            )
        }
    }
}

/**
 * Filtros de categorías
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategoryFilters(
    selectedCategory: ExpenseCategory?,
    onCategorySelected: (ExpenseCategory?) -> Unit
) {
    val categories = listOf(
        ExpenseCategory.FOOD to "Alimentación",
        ExpenseCategory.TRANSPORT to "Transporte",
        ExpenseCategory.ENTERTAINMENT to "Entretenimiento",
        ExpenseCategory.SERVICES to "Servicios",
        ExpenseCategory.SHOPPING to "Compras",
        ExpenseCategory.HEALTH to "Salud",
        ExpenseCategory.OTHER to "Otro"
    )

    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        categories.chunked(3).forEach { rowCategories ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                rowCategories.forEach { (category, label) ->
                    FilterChip(
                        selected = selectedCategory == category,
                        onClick = { 
                            onCategorySelected(
                                if (selectedCategory == category) null else category
                            )
                        },
                        label = { Text(label) }
                    )
                }
            }
        }
    }
}

/**
 * Lista de transacciones
 */
@Composable
private fun TransactionsList(
    transactions: List<TransactionUiModel>,
    onTransactionClick: (TransactionUiModel) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(
            items = transactions,
            key = { "${it.type.name}_${it.id}" }
        ) { transaction ->
            AnimatedListItem(item = transaction) { animatedTransaction ->
                TransactionItem(
                    type = animatedTransaction.type,
                    description = animatedTransaction.description,
                    amount = animatedTransaction.amount,
                    date = animatedTransaction.date,
                    category = animatedTransaction.category,
                    installmentInfo = animatedTransaction.installmentInfo,
                    accountOrCardName = animatedTransaction.accountOrCardName,
                    onClick = { onTransactionClick(animatedTransaction) }
                )
            }
        }
    }
}

/**
 * Contenido mostrado mientras se cargan los datos
 */
@Composable
private fun LoadingContent() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            CircularProgressIndicator()
            Text(
                text = "Cargando transacciones...",
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}

/**
 * Contenido mostrado cuando hay un error
 */
@Composable
private fun ErrorContent(
    error: String,
    onDismiss: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.errorContainer
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Error",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
                Text(
                    text = error,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    textAlign = TextAlign.Center
                )
                Button(onClick = onDismiss) {
                    Text("Cerrar")
                }
            }
        }
    }
}

/**
 * Contenido mostrado cuando no hay transacciones
 */
@Composable
private fun EmptyContent(hasFilters: Boolean) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(32.dp)
        ) {
            Icon(
                imageVector = if (hasFilters) Icons.Default.SearchOff else Icons.Default.Receipt,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = if (hasFilters) "No se encontraron transacciones" else "No hay transacciones",
                style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Center
            )
            Text(
                text = if (hasFilters) {
                    "Intenta ajustar los filtros de búsqueda"
                } else {
                    "Las compras y gastos que registres aparecerán aquí"
                },
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
