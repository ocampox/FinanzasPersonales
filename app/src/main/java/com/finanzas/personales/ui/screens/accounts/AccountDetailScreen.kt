package com.finanzas.personales.ui.screens.accounts

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.finanzas.personales.data.local.entities.ExpenseCategory
import com.finanzas.personales.domain.models.Expense
import com.finanzas.personales.domain.models.SavingsAccount
import com.finanzas.personales.ui.components.AddExpenseDialog
import com.finanzas.personales.ui.components.AddIncomeDialog
import com.finanzas.personales.ui.components.ChartCard
import kotlinx.datetime.Clock
import com.patrykandpatrick.vico.compose.axis.horizontal.rememberBottomAxis
import com.patrykandpatrick.vico.compose.axis.vertical.rememberStartAxis
import com.patrykandpatrick.vico.compose.chart.Chart
import com.patrykandpatrick.vico.compose.chart.line.lineChart
import com.patrykandpatrick.vico.compose.m3.style.m3ChartStyle
import com.patrykandpatrick.vico.compose.style.ProvideChartStyle
import com.patrykandpatrick.vico.core.entry.entryModelOf
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import java.text.NumberFormat
import java.util.*

/**
 * Pantalla de detalle de cuenta de ahorros
 * 
 * Muestra información completa de la cuenta:
 * - Información básica (nombre, saldo)
 * - Gráfico de evolución del saldo
 * - Historial de gastos por categoría
 * - Opciones para agregar gasto, editar y eliminar
 * 
 * Requirements: 2.4, 6.4, 7.3
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountDetailScreen(
    viewModel: AccountDetailViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit = {},
    onNavigateToEdit: (Long) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    // Navegar de vuelta cuando se elimina exitosamente
    LaunchedEffect(uiState.deleteSuccess) {
        if (uiState.deleteSuccess) {
            onNavigateBack()
        }
    }

    // Mostrar errores
    LaunchedEffect(uiState.error) {
        uiState.error?.let { error ->
            snackbarHostState.showSnackbar(
                message = error,
                duration = SnackbarDuration.Short
            )
            viewModel.clearError()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(uiState.savingsAccount?.name ?: "Detalle de Cuenta") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
                    }
                },
                actions = {
                    IconButton(
                        onClick = { uiState.savingsAccount?.let { onNavigateToEdit(it.id) } }
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = "Editar")
                    }
                    IconButton(onClick = { viewModel.showDeleteConfirmation() }) {
                        Icon(Icons.Default.Delete, contentDescription = "Eliminar")
                    }
                }
            )
        },
        floatingActionButton = {
            // FAB expandible para agregar ingreso o gasto
            var expanded by remember { mutableStateOf(false) }
            
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // FABs secundarios (solo visibles cuando está expandido)
                if (expanded) {
                    // FAB para agregar ingreso
                    FloatingActionButton(
                        onClick = { 
                            expanded = false
                            viewModel.showAddIncomeDialog() 
                        },
                        containerColor = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            Icons.Default.Add, 
                            contentDescription = "Agregar ingreso",
                            tint = Color.White
                        )
                    }
                    
                    // FAB para agregar gasto
                    FloatingActionButton(
                        onClick = { 
                            expanded = false
                            viewModel.showAddExpenseDialog() 
                        },
                        containerColor = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            Icons.Default.Remove, 
                            contentDescription = "Agregar gasto",
                            tint = Color.White
                        )
                    }
                }
                
                // FAB principal
                FloatingActionButton(
                    onClick = { expanded = !expanded }
                ) {
                    Icon(
                        if (expanded) Icons.Default.Close else Icons.Default.Add,
                        contentDescription = if (expanded) "Cerrar menú" else "Agregar transacción"
                    )
                }
            }
        }
    ) { paddingValues ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            uiState.savingsAccount?.let { account ->
                AccountDetailContent(
                    account = account,
                    expenses = uiState.expenses,
                    incomes = uiState.incomes,
                    viewModel = viewModel,
                    modifier = Modifier.padding(paddingValues)
                )
            }
        }
    }

    // Diálogo de confirmación de eliminación
    if (uiState.showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = { viewModel.hideDeleteConfirmation() },
            title = { Text("Eliminar cuenta") },
            text = { Text("¿Estás seguro de que deseas eliminar esta cuenta? Esta acción no se puede deshacer.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.hideDeleteConfirmation()
                        viewModel.deleteAccount()
                    },
                    enabled = !uiState.isDeleting
                ) {
                    if (uiState.isDeleting) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp))
                    } else {
                        Text("Eliminar")
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { viewModel.hideDeleteConfirmation() },
                    enabled = !uiState.isDeleting
                ) {
                    Text("Cancelar")
                }
            }
        )
    }

    // Diálogo de agregar gasto
    uiState.savingsAccount?.let { account ->
        if (uiState.showAddExpenseDialog) {
            AddExpenseDialog(
                accountName = account.name,
                availableBalance = account.balance,
                onDismiss = { viewModel.hideAddExpenseDialog() },
                onConfirm = { expenseData ->
                    val expense = Expense(
                        id = 0,
                        accountId = account.id,
                        description = expenseData.description,
                        amount = expenseData.amount,
                        category = expenseData.category,
                        expenseDate = kotlinx.datetime.Clock.System.now()
                    )
                    viewModel.addExpense(expense)
                }
            )
        }

        // Diálogo de agregar ingreso
        if (uiState.showAddIncomeDialog) {
            AddIncomeDialog(
                accountName = account.name,
                onDismiss = { viewModel.hideAddIncomeDialog() },
                onConfirm = { incomeData ->
                    val income = com.finanzas.personales.domain.models.Income(
                        id = 0,
                        accountId = account.id,
                        description = incomeData.description,
                        amount = incomeData.amount,
                        category = incomeData.category,
                        incomeDate = kotlinx.datetime.Clock.System.now()
                    )
                    viewModel.addIncome(income)
                }
            )
        }

        // Diálogo de editar gasto
        if (uiState.showEditExpenseDialog && uiState.expenseToEdit != null) {
            AddExpenseDialog(
                accountName = account.name,
                availableBalance = account.balance,
                expense = uiState.expenseToEdit,
                onDismiss = { viewModel.hideEditExpenseDialog() },
                onConfirm = { expenseData ->
                    viewModel.updateExpense(
                        expenseId = uiState.expenseToEdit!!.id,
                        description = expenseData.description,
                        amount = expenseData.amount,
                        category = expenseData.category
                    )
                }
            )
        }

        // Diálogo de confirmación de eliminación de gasto
        if (uiState.showDeleteExpenseConfirmation) {
            AlertDialog(
                onDismissRequest = { viewModel.hideDeleteExpenseConfirmation() },
                title = { Text("Eliminar gasto") },
                text = { Text("¿Estás seguro de que deseas eliminar este gasto? Esta acción no se puede deshacer.") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            viewModel.deleteExpense()
                        },
                        enabled = !uiState.isDeleting
                    ) {
                        if (uiState.isDeleting) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp))
                        } else {
                            Text("Eliminar")
                        }
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { viewModel.hideDeleteExpenseConfirmation() },
                        enabled = !uiState.isDeleting
                    ) {
                        Text("Cancelar")
                    }
                }
            )
        }
    }
}

@Composable
private fun AccountDetailContent(
    account: SavingsAccount,
    expenses: List<Expense>,
    incomes: List<com.finanzas.personales.domain.models.Income>,
    viewModel: AccountDetailViewModel,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Información básica de la cuenta
        item {
            AccountInfoSection(account = account)
        }

        // Gráfico de evolución del saldo
        item {
            BalanceEvolutionChart(account = account, expenses = expenses, incomes = incomes)
        }

        // Historial de transacciones
        item {
            Text(
                text = "Historial de Transacciones",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        }

        // Crear lista combinada de transacciones ordenadas por fecha
        val allTransactions = (expenses.map { TransactionItem.ExpenseItem(it) } + 
                              incomes.map { TransactionItem.IncomeItem(it) })
            .sortedByDescending { 
                when (it) {
                    is TransactionItem.ExpenseItem -> it.expense.expenseDate
                    is TransactionItem.IncomeItem -> it.income.incomeDate
                }
            }

        if (allTransactions.isEmpty()) {
            item {
                EmptyTransactionsState()
            }
        } else {
            items(allTransactions) { transaction ->
                when (transaction) {
                    is TransactionItem.ExpenseItem -> {
                        ExpenseItem(
                            expense = transaction.expense,
                            onEdit = { viewModel.showEditExpenseDialog(transaction.expense) },
                            onDelete = { viewModel.showDeleteExpenseConfirmation(transaction.expense) }
                        )
                    }
                    is TransactionItem.IncomeItem -> {
                        IncomeItem(income = transaction.income)
                    }
                }
            }
        }
    }
}

@Composable
private fun AccountInfoSection(account: SavingsAccount) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color(android.graphics.Color.parseColor(account.color))
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = account.name,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Saldo disponible
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Saldo Disponible",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.9f)
                )
                Text(
                    text = formatCurrency(account.balance),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}

@Composable
private fun BalanceEvolutionChart(
    account: SavingsAccount,
    expenses: List<Expense>,
    incomes: List<com.finanzas.personales.domain.models.Income>
) {
    ChartCard(
        title = "Evolución del Saldo",
        modifier = Modifier.fillMaxWidth()
    ) {
        // Combinar y ordenar todas las transacciones por fecha
        val allTransactions = (expenses.map { TransactionItem.ExpenseItem(it) } + 
                              incomes.map { TransactionItem.IncomeItem(it) })
            .sortedBy { 
                when (it) {
                    is TransactionItem.ExpenseItem -> it.expense.expenseDate
                    is TransactionItem.IncomeItem -> it.income.incomeDate
                }
            }
        
        val balanceHistory = mutableListOf<Float>()
        
        // Saldo inicial (actual - suma de ingresos + suma de gastos)
        val totalIncomes = incomes.sumOf { it.amount }
        val totalExpenses = expenses.sumOf { it.amount }
        val initialBalance = account.balance - totalIncomes + totalExpenses
        balanceHistory.add(initialBalance.toFloat())
        
        // Calcular saldo después de cada transacción
        var currentBalance = initialBalance
        allTransactions.forEach { transaction ->
            when (transaction) {
                is TransactionItem.ExpenseItem -> {
                    currentBalance -= transaction.expense.amount
                }
                is TransactionItem.IncomeItem -> {
                    currentBalance += transaction.income.amount
                }
            }
            balanceHistory.add(currentBalance.toFloat())
        }

        if (balanceHistory.size > 1) {
            val chartEntryModel = entryModelOf(*balanceHistory.map { it as Number }.toTypedArray())

            ProvideChartStyle(m3ChartStyle()) {
                Chart(
                    chart = lineChart(),
                    model = chartEntryModel,
                    startAxis = rememberStartAxis(),
                    bottomAxis = rememberBottomAxis(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                )
            }
        } else {
            // Mostrar mensaje cuando no hay suficientes datos
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No hay suficientes datos para mostrar el gráfico",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun ExpenseItem(
    expense: Expense,
    onEdit: () -> Unit = {},
    onDelete: () -> Unit = {}
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Icono y detalles
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Surface(
                        modifier = Modifier.size(48.dp),
                        shape = RoundedCornerShape(8.dp),
                        color = getCategoryColor(expense.category)
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = getCategoryIcon(expense.category),
                                contentDescription = "Categoría: ${getCategoryName(expense.category)}",
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = expense.description,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = getCategoryName(expense.category),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        val expenseLocalDate = expense.expenseDate
                            .toLocalDateTime(TimeZone.currentSystemDefault())
                            .date
                        Text(
                            text = "${expenseLocalDate.dayOfMonth}/${expenseLocalDate.monthNumber}/${expenseLocalDate.year}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Monto
                Text(
                    text = formatCurrency(expense.amount),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.error
                )
            }

            // Botones de acción
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onEdit) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Editar gasto",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Eliminar gasto",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyTransactionsState() {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Receipt,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
            Text(
                text = "No hay transacciones registradas",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Text(
                text = "Presiona el botón + para agregar un ingreso o gasto",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            )
        }
    }
}

private fun getCategoryIcon(category: ExpenseCategory) = when (category) {
    ExpenseCategory.FOOD -> Icons.Default.Restaurant
    ExpenseCategory.TRANSPORT -> Icons.Default.DirectionsCar
    ExpenseCategory.ENTERTAINMENT -> Icons.Default.Movie
    ExpenseCategory.SERVICES -> Icons.Default.Build
    ExpenseCategory.SHOPPING -> Icons.Default.ShoppingBag
    ExpenseCategory.HEALTH -> Icons.Default.LocalHospital
    ExpenseCategory.OTHER -> Icons.Default.MoreHoriz
}

private fun getCategoryColor(category: ExpenseCategory) = when (category) {
    ExpenseCategory.FOOD -> Color(0xFFE53935)
    ExpenseCategory.TRANSPORT -> Color(0xFF1E88E5)
    ExpenseCategory.ENTERTAINMENT -> Color(0xFF8E24AA)
    ExpenseCategory.SERVICES -> Color(0xFFFB8C00)
    ExpenseCategory.SHOPPING -> Color(0xFF43A047)
    ExpenseCategory.HEALTH -> Color(0xFFD81B60)
    ExpenseCategory.OTHER -> Color(0xFF757575)
}

private fun getCategoryName(category: ExpenseCategory) = when (category) {
    ExpenseCategory.FOOD -> "Alimentación"
    ExpenseCategory.TRANSPORT -> "Transporte"
    ExpenseCategory.ENTERTAINMENT -> "Entretenimiento"
    ExpenseCategory.SERVICES -> "Servicios"
    ExpenseCategory.SHOPPING -> "Compras"
    ExpenseCategory.HEALTH -> "Salud"
    ExpenseCategory.OTHER -> "Otros"
}

private fun formatCurrency(amount: Double): String {
    val format = NumberFormat.getCurrencyInstance(Locale("es", "CO"))
    return format.format(amount)
}

/**
 * Sealed class para representar diferentes tipos de transacciones
 */
sealed class TransactionItem {
    data class ExpenseItem(val expense: Expense) : TransactionItem()
    data class IncomeItem(val income: com.finanzas.personales.domain.models.Income) : TransactionItem()
}

/**
 * Composable para mostrar un ingreso en el historial
 */
@Composable
private fun IncomeItem(
    income: com.finanzas.personales.domain.models.Income
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Icono y detalles
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Surface(
                        modifier = Modifier.size(48.dp),
                        shape = RoundedCornerShape(8.dp),
                        color = getIncomeCategoryColor(income.category)
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = getIncomeCategoryIcon(income.category),
                                contentDescription = "Categoría: ${getIncomeCategoryName(income.category)}",
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = income.description,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = getIncomeCategoryName(income.category),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        val incomeLocalDate = income.incomeDate
                            .toLocalDateTime(TimeZone.currentSystemDefault())
                            .date
                        Text(
                            text = "${incomeLocalDate.dayOfMonth}/${incomeLocalDate.monthNumber}/${incomeLocalDate.year}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Monto (en verde para ingresos)
                Text(
                    text = "+${formatCurrency(income.amount)}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF4CAF50) // Verde para ingresos
                )
            }
        }
    }
}

private fun getIncomeCategoryIcon(category: com.finanzas.personales.domain.models.IncomeCategory) = when (category) {
    com.finanzas.personales.domain.models.IncomeCategory.SALARY -> Icons.Default.Work
    com.finanzas.personales.domain.models.IncomeCategory.BONUS -> Icons.Default.Star
    com.finanzas.personales.domain.models.IncomeCategory.FREELANCE -> Icons.Default.Person
    com.finanzas.personales.domain.models.IncomeCategory.INVESTMENT -> Icons.Default.TrendingUp
    com.finanzas.personales.domain.models.IncomeCategory.GIFT -> Icons.Default.CardGiftcard
    com.finanzas.personales.domain.models.IncomeCategory.REFUND -> Icons.Default.Undo
    com.finanzas.personales.domain.models.IncomeCategory.SALE -> Icons.Default.Sell
    com.finanzas.personales.domain.models.IncomeCategory.OTHER -> Icons.Default.MoreHoriz
}

private fun getIncomeCategoryColor(category: com.finanzas.personales.domain.models.IncomeCategory) = when (category) {
    com.finanzas.personales.domain.models.IncomeCategory.SALARY -> Color(0xFF4CAF50)
    com.finanzas.personales.domain.models.IncomeCategory.BONUS -> Color(0xFFFFD700)
    com.finanzas.personales.domain.models.IncomeCategory.FREELANCE -> Color(0xFF2196F3)
    com.finanzas.personales.domain.models.IncomeCategory.INVESTMENT -> Color(0xFF9C27B0)
    com.finanzas.personales.domain.models.IncomeCategory.GIFT -> Color(0xFFE91E63)
    com.finanzas.personales.domain.models.IncomeCategory.REFUND -> Color(0xFF00BCD4)
    com.finanzas.personales.domain.models.IncomeCategory.SALE -> Color(0xFFFF9800)
    com.finanzas.personales.domain.models.IncomeCategory.OTHER -> Color(0xFF757575)
}

private fun getIncomeCategoryName(category: com.finanzas.personales.domain.models.IncomeCategory) = when (category) {
    com.finanzas.personales.domain.models.IncomeCategory.SALARY -> "Salario"
    com.finanzas.personales.domain.models.IncomeCategory.BONUS -> "Bonificación"
    com.finanzas.personales.domain.models.IncomeCategory.FREELANCE -> "Freelance"
    com.finanzas.personales.domain.models.IncomeCategory.INVESTMENT -> "Inversión"
    com.finanzas.personales.domain.models.IncomeCategory.GIFT -> "Regalo"
    com.finanzas.personales.domain.models.IncomeCategory.REFUND -> "Reembolso"
    com.finanzas.personales.domain.models.IncomeCategory.SALE -> "Venta"
    com.finanzas.personales.domain.models.IncomeCategory.OTHER -> "Otros"
}