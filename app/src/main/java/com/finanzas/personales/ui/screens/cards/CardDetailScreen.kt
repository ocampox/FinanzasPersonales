package com.finanzas.personales.ui.screens.cards

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import com.finanzas.personales.domain.models.CreditCard
import com.finanzas.personales.domain.models.Purchase
import com.finanzas.personales.ui.components.AddPurchaseDialog
import com.finanzas.personales.ui.components.AddPaymentDialog
import com.finanzas.personales.ui.components.ChartCard
import com.patrykandpatrick.vico.compose.axis.horizontal.rememberBottomAxis
import com.patrykandpatrick.vico.compose.axis.vertical.rememberStartAxis
import com.patrykandpatrick.vico.compose.chart.Chart
import com.patrykandpatrick.vico.compose.chart.column.columnChart
import com.patrykandpatrick.vico.compose.m3.style.m3ChartStyle
import com.patrykandpatrick.vico.compose.style.ProvideChartStyle
import com.patrykandpatrick.vico.core.entry.entryModelOf
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import java.text.NumberFormat
import java.util.*

/**
 * Pantalla de detalle de tarjeta de crédito
 * 
 * Muestra información completa de la tarjeta:
 * - Información básica (nombre, cupo, fechas)
 * - Gráfico de uso del cupo
 * - Historial de compras con desglose de cuotas
 * - Opciones para agregar compra, editar y eliminar
 * 
 * Requirements: 1.4, 3.5, 7.2
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CardDetailScreen(
    viewModel: CardDetailViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit = {},
    onNavigateToEdit: (Long) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    // Estado UI-only: si el panel de filtros está expandido
    var showFilterPanel by remember { mutableStateOf(false) }

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
            Column {
                TopAppBar(
                    title = { Text(uiState.creditCard?.name ?: "Detalle de Tarjeta") },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
                        }
                    },
                    actions = {
                        // Filtros — ícono relleno cuando hay filtro activo, con botón de limpiar
                        IconButton(onClick = { showFilterPanel = !showFilterPanel }) {
                            Icon(
                                imageVector = if (showFilterPanel)
                                    Icons.Default.FilterListOff
                                else
                                    Icons.Default.FilterList,
                                contentDescription = if (showFilterPanel) "Ocultar filtros" else "Mostrar filtros",
                                tint = if (uiState.purchaseFilter != PurchaseFilter.ALL)
                                    MaterialTheme.colorScheme.primary
                                else
                                    LocalContentColor.current
                            )
                        }
                        // Botón de limpiar filtro — solo visible cuando hay filtro activo
                        if (uiState.purchaseFilter != PurchaseFilter.ALL) {
                            IconButton(onClick = { viewModel.clearPurchaseFilter() }) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "Limpiar filtro"
                                )
                            }
                        }
                        IconButton(
                            onClick = { uiState.creditCard?.let { onNavigateToEdit(it.id) } }
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = "Editar")
                        }
                        IconButton(onClick = { viewModel.showDeleteConfirmation() }) {
                            Icon(Icons.Default.Delete, contentDescription = "Eliminar")
                        }
                    }
                )

                // Panel de filtros expandible con animación
                AnimatedVisibility(
                    visible = showFilterPanel,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    PurchaseFiltersPanel(
                        selectedFilter = uiState.purchaseFilter,
                        allCount = uiState.purchases.size,
                        pendingCount = uiState.purchases.count { !it.isFullyPaid() },
                        paidCount = uiState.purchases.count { it.isFullyPaid() },
                        installmentsCount = uiState.purchases.count { it.installments > 1 },
                        installmentsPendingCount = uiState.purchases.count { it.installments > 1 && !it.isFullyPaid() },
                        onFilterSelected = { viewModel.updatePurchaseFilter(it) }
                    )
                }
            }
        },
        floatingActionButton = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                // Botón de abono
                ExtendedFloatingActionButton(
                    onClick = { viewModel.showAddPaymentDialog() },
                    icon = {
                        Icon(Icons.Default.AccountBalanceWallet, contentDescription = null)
                    },
                    text = { Text("Abono") },
                    containerColor = MaterialTheme.colorScheme.secondary
                )
                
                // Botón de compra
                FloatingActionButton(
                    onClick = { viewModel.showAddPurchaseDialog() }
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Agregar compra")
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
            uiState.creditCard?.let { card ->
                CardDetailContent(
                    card = card,
                    purchases = uiState.filteredPurchases,
                    allPurchasesCount = uiState.purchases.size,
                    purchaseFilter = uiState.purchaseFilter,
                    payments = uiState.payments,
                    minimumPayment = uiState.minimumPayment,
                    nextCutoffDate = uiState.nextCutoffDate,
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
            title = { Text("Eliminar tarjeta") },
            text = { Text("¿Estás seguro de que deseas eliminar esta tarjeta? Esta acción no se puede deshacer.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.hideDeleteConfirmation()
                        viewModel.deleteCard()
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

    // Diálogo de agregar compra
    uiState.creditCard?.let { card ->
        if (uiState.showAddPurchaseDialog) {
            AddPurchaseDialog(
                cardName = card.name,
                availableCredit = card.availableLimit,
                onDismiss = { viewModel.hideAddPurchaseDialog() },
                onConfirm = { purchaseData ->
                    viewModel.addPurchase(
                        description = purchaseData.description,
                        totalAmount = purchaseData.totalAmount,
                        installments = purchaseData.installments,
                        installmentAmount = purchaseData.installmentAmount
                    )
                }
            )
        }

        // Diálogo de editar compra
        if (uiState.showEditPurchaseDialog && uiState.purchaseToEdit != null) {
            AddPurchaseDialog(
                cardName = card.name,
                availableCredit = card.availableLimit,
                purchase = uiState.purchaseToEdit,
                onDismiss = { viewModel.hideEditPurchaseDialog() },
                onConfirm = { purchaseData ->
                    viewModel.updatePurchase(
                        purchaseId = uiState.purchaseToEdit!!.id,
                        description = purchaseData.description,
                        totalAmount = purchaseData.totalAmount,
                        installments = purchaseData.installments,
                        installmentAmount = purchaseData.installmentAmount
                    )
                }
            )
        }

        // Diálogo de confirmación de eliminación de compra
        if (uiState.showDeletePurchaseConfirmation) {
            AlertDialog(
                onDismissRequest = { viewModel.hideDeletePurchaseConfirmation() },
                title = { Text("Eliminar compra") },
                text = { Text("¿Estás seguro de que deseas eliminar esta compra? Esta acción no se puede deshacer.") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            viewModel.deletePurchase()
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
                        onClick = { viewModel.hideDeletePurchaseConfirmation() },
                        enabled = !uiState.isDeleting
                    ) {
                        Text("Cancelar")
                    }
                }
            )
        }
        
        // Diálogo de agregar abono
        if (uiState.showAddPaymentDialog) {
            AddPaymentDialog(
                cardName = card.name,
                currentUsedLimit = card.totalLimit - card.availableLimit,
                totalLimit = card.totalLimit,
                onDismiss = { viewModel.hideAddPaymentDialog() },
                onConfirm = { amount -> viewModel.addPayment(amount) },
                isLoading = uiState.isAddingPayment
            )
        }
    }
}

@Composable
private fun CardDetailContent(
    card: CreditCard,
    purchases: List<Purchase>,
    allPurchasesCount: Int,
    purchaseFilter: PurchaseFilter,
    payments: List<com.finanzas.personales.domain.models.CardPayment>,
    minimumPayment: Double?,
    nextCutoffDate: LocalDate?,
    viewModel: CardDetailViewModel,
    modifier: Modifier = Modifier
) {
    var paymentsExpanded by remember { mutableStateOf(true) }
    var purchasesExpanded by remember { mutableStateOf(true) }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // ── Info tarjeta ──────────────────────────────────────────────
        item(key = "card_info") {
            CardInfoSection(
                card = card,
                minimumPayment = minimumPayment,
                nextCutoffDate = nextCutoffDate
            )
        }

        // ── Gráfico ───────────────────────────────────────────────────
        item(key = "chart") { CreditUsageChart(card = card) }

        // ── Historial de Pagos ────────────────────────────────────────
        item(key = "payments_header") {
            SectionHeader(
                title = "Historial de Pagos",
                count = payments.size,
                expanded = paymentsExpanded,
                onToggle = { paymentsExpanded = !paymentsExpanded }
            )
        }

        if (paymentsExpanded) {
            if (payments.isEmpty()) {
                item(key = "payments_empty") {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Aún no hay pagos registrados.\nA partir de ahora cada abono quedará guardado aquí.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            } else {
                items(payments, key = { "payment_${it.id}" }) { payment ->
                    CardPaymentItem(payment = payment)
                }
            }
        }

        // ── Historial de Compras ──────────────────────────────────────
        item(key = "purchases_header") {
            SectionHeader(
                title = "Historial de Compras",
                count = allPurchasesCount,
                filteredCount = if (purchaseFilter != PurchaseFilter.ALL) purchases.size else null,
                expanded = purchasesExpanded,
                onToggle = { purchasesExpanded = !purchasesExpanded }
            )
        }

        if (purchasesExpanded) {
            if (purchases.isEmpty()) {
                item(key = "purchases_empty") {
                    EmptyPurchasesState(
                        message = when (purchaseFilter) {
                            PurchaseFilter.PENDING -> "No hay compras con cuotas pendientes"
                            PurchaseFilter.PAID -> "No hay compras completamente pagadas"
                            PurchaseFilter.INSTALLMENTS -> "No hay compras diferidas a más de 1 cuota"
                            PurchaseFilter.INSTALLMENTS_PENDING -> "No hay compras diferidas y con cuotas pendientes"
                            PurchaseFilter.ALL -> "No hay compras registradas"
                        }
                    )
                }
            } else {
                items(purchases, key = { it.id }) { purchase ->
                    PurchaseItem(
                        purchase = purchase,
                        onEdit = { viewModel.showEditPurchaseDialog(purchase) },
                        onDelete = { viewModel.showDeletePurchaseConfirmation(purchase) }
                    )
                }
            }
        }
    }
}

@Composable
private fun CardInfoSection(
    card: CreditCard,
    minimumPayment: Double?,
    nextCutoffDate: LocalDate?
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color(android.graphics.Color.parseColor(card.color))
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = card.name,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Cupo disponible
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Cupo Disponible",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.9f)
                )
                Text(
                    text = formatCurrency(card.availableLimit),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            // Cupo total
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Cupo Total",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.9f)
                )
                Text(
                    text = formatCurrency(card.totalLimit),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Barra de progreso
            LinearProgressIndicator(
                progress = card.usagePercentage / 100f,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp),
                color = Color.White,
                trackColor = Color.White.copy(alpha = 0.3f)
            )

            // Próximo pago estimado — proyección dinámica del próximo corte
            val projectedPayment = minimumPayment ?: 0.0
            if (projectedPayment > 0) {
                Spacer(modifier = Modifier.height(12.dp))
                Divider(color = Color.White.copy(alpha = 0.3f), thickness = 1.dp)
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Próximo pago estimado",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.9f)
                        )
                        nextCutoffDate?.let {
                            Text(
                                text = "Corte: ${formatDate(it)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(alpha = 0.7f)
                            )
                        }
                    }
                    Text(
                        text = formatCurrency(projectedPayment),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }

            Text(
                text = "${String.format("%.1f", card.usagePercentage)}% usado",
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.9f)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Fechas
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Fecha de Corte",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.9f)
                    )
                    Text(
                        text = "Día ${card.cutoffDay}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Fecha de Pago",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.9f)
                    )
                    Text(
                        text = "Día ${card.paymentDay}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    }
}

@Composable
private fun CreditUsageChart(card: CreditCard) {
    ChartCard(
        title = "Uso del Cupo",
        modifier = Modifier.fillMaxWidth()
    ) {
        val usedAmount = card.usedLimit
        val availableAmount = card.availableLimit

        // Crear modelo de datos para el gráfico
        val chartEntryModel = entryModelOf(usedAmount.toFloat(), availableAmount.toFloat())

        ProvideChartStyle(m3ChartStyle()) {
            Chart(
                chart = columnChart(),
                model = chartEntryModel,
                startAxis = rememberStartAxis(),
                bottomAxis = rememberBottomAxis(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Leyenda
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            LegendItem(
                color = MaterialTheme.colorScheme.primary,
                label = "Usado",
                value = formatCurrency(usedAmount)
            )
            LegendItem(
                color = MaterialTheme.colorScheme.secondary,
                label = "Disponible",
                value = formatCurrency(availableAmount)
            )
        }
    }
}

@Composable
private fun LegendItem(
    color: Color,
    label: String,
    value: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .background(color, RoundedCornerShape(2.dp))
        )
        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun PurchaseItem(
    purchase: Purchase,
    onEdit: () -> Unit = {},
    onDelete: () -> Unit = {}
) {
    val remainingInstallments = purchase.getRemainingInstallments()
    val remainingAmount = purchase.getRemainingAmount()
    val isFullyPaid = purchase.isFullyPaid()

    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Descripción y monto total
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = purchase.description,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = formatCurrency(purchase.totalAmount),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            // Fecha de compra
            val purchaseLocalDate = purchase.purchaseDate
                .toLocalDateTime(TimeZone.currentSystemDefault())
                .date
            Text(
                text = "Fecha: ${purchaseLocalDate.dayOfMonth}/${purchaseLocalDate.monthNumber}/${purchaseLocalDate.year}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Divider()

            // Desglose de cuotas - Diseño vertical
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Cuotas
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Cuotas",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = if (isFullyPaid) {
                            "Pagado (${purchase.installments}/${purchase.installments})"
                        } else {
                            "${purchase.paidInstallments} pagadas, $remainingInstallments pendientes (${purchase.installments} total)"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (isFullyPaid) {
                            MaterialTheme.colorScheme.tertiary
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        }
                    )
                }

                // Valor cuota
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Valor cuota",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = formatCurrency(purchase.installmentAmount),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Saldo pendiente
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Saldo pendiente",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = formatCurrency(remainingAmount),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (isFullyPaid) {
                            MaterialTheme.colorScheme.tertiary
                        } else {
                            MaterialTheme.colorScheme.error
                        }
                    )
                }
            }

            // Indicador de estado
            if (isFullyPaid) {
                Surface(
                    color = MaterialTheme.colorScheme.tertiaryContainer,
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = "✓ Completamente pagado",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
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
                        contentDescription = "Editar compra",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Eliminar compra",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyPurchasesState(
    message: String = "No hay compras registradas"
) {
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
                imageVector = Icons.Default.ShoppingCart,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
            Text(
                text = message,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            if (message == "No hay compras registradas") {
                Text(
                    text = "Presiona el botón + para agregar una compra",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

private fun formatCurrency(amount: Double): String {
    val format = NumberFormat.getCurrencyInstance(Locale("es", "CO"))
    return format.format(amount)
}

// ─────────────────────────────────────────────────────────────────────────────
// Encabezado de sección colapsable
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Encabezado con título, contador y botón de colapsar/expandir.
 * Usado tanto para el historial de pagos como para el de compras.
 */
@Composable
private fun SectionHeader(
    title: String,
    count: Int,
    filteredCount: Int? = null,
    expanded: Boolean,
    onToggle: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            // Contador
            if (count > 0) {
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.secondaryContainer
                ) {
                    Text(
                        text = if (filteredCount != null) "$filteredCount/$count" else "$count",
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
        }
        Icon(
            imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
            contentDescription = if (expanded) "Colapsar" else "Expandir",
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Item de historial de pagos
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun CardPaymentItem(
    payment: com.finanzas.personales.domain.models.CardPayment
) {
    val isMarkAsPaid = payment.paymentType == com.finanzas.personales.domain.models.PaymentType.MARK_AS_PAID

    val typeLabel = when (payment.paymentType) {
        com.finanzas.personales.domain.models.PaymentType.CAPITAL -> "Abono"
        com.finanzas.personales.domain.models.PaymentType.MINIMUM_PAYMENT -> "Pago de corte"
        com.finanzas.personales.domain.models.PaymentType.MARK_AS_PAID -> "Pago registrado manualmente"
    }

    val containerColor = when (payment.paymentType) {
        com.finanzas.personales.domain.models.PaymentType.CAPITAL -> MaterialTheme.colorScheme.primaryContainer
        com.finanzas.personales.domain.models.PaymentType.MINIMUM_PAYMENT -> MaterialTheme.colorScheme.secondaryContainer
        com.finanzas.personales.domain.models.PaymentType.MARK_AS_PAID -> MaterialTheme.colorScheme.tertiaryContainer
    }

    val paymentLocalDate = payment.paymentDate
        .toLocalDateTime(TimeZone.currentSystemDefault())
        .date

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = containerColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = typeLabel,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "${paymentLocalDate.dayOfMonth}/${paymentLocalDate.monthNumber}/${paymentLocalDate.year}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (!payment.note.isNullOrBlank()) {
                    Text(
                        text = payment.note,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                payment.cutoffDate?.let { cutoff ->
                    val cutoffDate = cutoff.toLocalDateTime(TimeZone.currentSystemDefault()).date
                    Text(
                        text = "Corte: ${cutoffDate.dayOfMonth}/${cutoffDate.monthNumber}/${cutoffDate.year}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Text(
                text = if (isMarkAsPaid) "Ya pagado" else formatCurrency(payment.amount),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

private fun formatDate(date: LocalDate): String {
    val months = listOf(
        "Ene", "Feb", "Mar", "Abr", "May", "Jun",
        "Jul", "Ago", "Sep", "Oct", "Nov", "Dic"
    )
    return "${date.dayOfMonth} ${months[date.monthNumber - 1]} ${date.year}"
}

// ─────────────────────────────────────────────────────────────────────────────
// Panel de filtros de compras
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Panel expandible con los filtros de estado de cuotas.
 * Mismo patrón visual que FiltersPanel en TransactionsScreen.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PurchaseFiltersPanel(
    selectedFilter: PurchaseFilter,
    allCount: Int,
    pendingCount: Int,
    paidCount: Int,
    installmentsCount: Int,
    installmentsPendingCount: Int,
    onFilterSelected: (PurchaseFilter) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "Estado de cuotas",
            style = MaterialTheme.typography.labelLarge
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = selectedFilter == PurchaseFilter.ALL,
                onClick = { onFilterSelected(PurchaseFilter.ALL) },
                label = { Text("Todas ($allCount)") }
            )
            FilterChip(
                selected = selectedFilter == PurchaseFilter.PENDING,
                onClick = { onFilterSelected(PurchaseFilter.PENDING) },
                label = { Text("Pendientes ($pendingCount)") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Schedule,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                }
            )
            FilterChip(
                selected = selectedFilter == PurchaseFilter.PAID,
                onClick = { onFilterSelected(PurchaseFilter.PAID) },
                label = { Text("Pagadas ($paidCount)") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                }
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = selectedFilter == PurchaseFilter.INSTALLMENTS,
                onClick = { onFilterSelected(PurchaseFilter.INSTALLMENTS) },
                label = { Text("Diferidas ($installmentsCount)") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.ViewWeek,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                }
            )
            FilterChip(
                selected = selectedFilter == PurchaseFilter.INSTALLMENTS_PENDING,
                onClick = { onFilterSelected(PurchaseFilter.INSTALLMENTS_PENDING) },
                label = { Text("Diferidas pendientes ($installmentsPendingCount)") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.PendingActions,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                }
            )
        }
    }
}
