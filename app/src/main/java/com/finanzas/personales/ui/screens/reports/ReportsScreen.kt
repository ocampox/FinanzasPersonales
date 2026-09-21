package com.finanzas.personales.ui.screens.reports

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.finanzas.personales.data.local.entities.ExpenseCategory
import com.finanzas.personales.domain.models.PaymentDue
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import java.text.NumberFormat
import java.util.*

/**
 * Pantalla de reportes financieros.
 *
 * Secciones:
 *  1. KPIs de deuda — cupo total, deuda actual, cupo libre, % uso global
 *  2. Balance del período — gastos vs ingresos de cuentas de ahorro
 *  3. Deuda por tarjeta — barras horizontales propias con nombre, monto y % visible
 *  4. Proyección de cuotas — compromisos mensuales de los próximos 6 meses (barras propias)
 *  5. Gastos por categoría — barras de progreso proporcionales con monto y %
 *  6. Próximos pagos — lista con urgencia codificada por color
 *
 * Requirements: 7.1, 7.2, 7.3, 7.4, 7.5
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen(
    viewModel: ReportsViewModel = hiltViewModel(),
    onNavigateToSettings: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val dateFilter by viewModel.dateFilter.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Reportes") },
                actions = {
                    IconButton(onClick = { viewModel.loadData() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Actualizar")
                    }
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Ajustes")
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when {
                uiState.isLoading -> ReportsLoading()
                uiState.error != null -> ReportsError(
                    error = uiState.error!!,
                    onRetry = { viewModel.loadData() },
                    onDismiss = { viewModel.clearError() }
                )
                else -> ReportsContent(
                    uiState = uiState,
                    dateFilter = dateFilter,
                    onDateFilterChange = { viewModel.updateDateFilter(it) }
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Contenido principal
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun ReportsContent(
    uiState: ReportsUiState,
    dateFilter: DateFilter,
    onDateFilterChange: (DateFilter) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // ── 1. KPIs de deuda ─────────────────────────────────────────────────
        item {
            DebtKpiSection(uiState = uiState)
        }

        // ── Filtro de período ─────────────────────────────────────────────────
        item {
            PeriodFilterSection(selected = dateFilter, onSelect = onDateFilterChange)
        }

        // ── 2. Balance del período ────────────────────────────────────────────
        item {
            PeriodBalanceSection(uiState = uiState, period = dateFilter.displayName)
        }

        // ── 3. Deuda por tarjeta ──────────────────────────────────────────────
        if (uiState.cardUsageData.isNotEmpty()) {
            item {
                CardDebtSection(cardUsageData = uiState.cardUsageData)
            }
        }

        // ── 4. Proyección mensual de cuotas ───────────────────────────────────
        if (uiState.monthlyProjection.isNotEmpty() && uiState.monthlyProjection.any { it.total > 0 }) {
            item {
                MonthlyProjectionSection(projection = uiState.monthlyProjection)
            }
        }

        // ── 5. Gastos por categoría ───────────────────────────────────────────
        if (uiState.expensesByCategory.isNotEmpty()) {
            item {
                ExpensesByCategorySection(
                    expenses = uiState.expensesByCategory,
                    total = uiState.periodTotalExpenses
                )
            }
        }

        // ── 6. Próximos pagos ─────────────────────────────────────────────────
        if (uiState.upcomingPayments.isNotEmpty()) {
            item {
                UpcomingPaymentsSection(payments = uiState.upcomingPayments)
            }
        }

        // ── Estado vacío ──────────────────────────────────────────────────────
        if (uiState.cardUsageData.isEmpty() &&
            uiState.expensesByCategory.isEmpty() &&
            uiState.upcomingPayments.isEmpty()
        ) {
            item { EmptyReports() }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Sección 1 — KPIs de deuda
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun DebtKpiSection(uiState: ReportsUiState) {
    SectionCard(title = "Resumen de Deuda", icon = Icons.Default.CreditCard) {
        // Barra de uso global
        if (uiState.totalLimit > 0) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Uso global del cupo",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${uiState.overallUsagePercentage.toInt()}%",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = usageColor(uiState.overallUsagePercentage)
                    )
                }
                AnimatedLinearBar(
                    progress = (uiState.overallUsagePercentage / 100f).coerceIn(0f, 1f),
                    color = usageColor(uiState.overallUsagePercentage),
                    height = 10.dp
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // 4 métricas en grid 2×2
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            KpiBox(
                label = "Cupo total",
                value = formatCurrency(uiState.totalLimit),
                color = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.weight(1f)
            )
            KpiBox(
                label = "Deuda actual",
                value = formatCurrency(uiState.totalDebt),
                color = if (uiState.totalDebt > 0) MaterialTheme.colorScheme.errorContainer
                else MaterialTheme.colorScheme.secondaryContainer,
                contentColor = if (uiState.totalDebt > 0) MaterialTheme.colorScheme.onErrorContainer
                else MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.weight(1f)
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            KpiBox(
                label = "Cupo libre",
                value = formatCurrency(uiState.totalAvailableCredit),
                color = MaterialTheme.colorScheme.tertiaryContainer,
                contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                modifier = Modifier.weight(1f)
            )
            KpiBox(
                label = "Próximo corte\n(total cuotas)",
                value = formatCurrency(uiState.totalNextCutoffPayment),
                color = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Sección 2 — Balance del período
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun PeriodBalanceSection(uiState: ReportsUiState, period: String) {
    val balance = uiState.periodTotalIncomes - uiState.periodTotalExpenses
    val isPositive = balance >= 0

    SectionCard(
        title = "Balance del Período",
        subtitle = period,
        icon = Icons.Default.AccountBalance
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Ingresos
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    Icons.Default.TrendingUp,
                    contentDescription = null,
                    tint = Color(0xFF2E7D32),
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = formatCurrency(uiState.periodTotalIncomes),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF2E7D32),
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "Ingresos",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Separador vertical
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(64.dp)
                    .background(MaterialTheme.colorScheme.outlineVariant)
                    .align(Alignment.CenterVertically)
            )

            // Gastos
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    Icons.Default.TrendingDown,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = formatCurrency(uiState.periodTotalExpenses),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "Gastos (${uiState.periodTransactionCount})",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Balance neto
        if (uiState.periodTotalIncomes > 0 || uiState.periodTotalExpenses > 0) {
            Spacer(modifier = Modifier.height(16.dp))
            Divider()
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Balance neto",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "${if (isPositive) "+" else ""}${formatCurrency(balance)}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isPositive) Color(0xFF2E7D32) else MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Sección 3 — Deuda por tarjeta
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun CardDebtSection(cardUsageData: List<CardUsage>) {
    SectionCard(title = "Deuda por Tarjeta", icon = Icons.Default.CreditCard) {
        Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
            cardUsageData.forEach { usage ->
                CardDebtRow(usage = usage)
            }
        }
    }
}

@Composable
private fun CardDebtRow(usage: CardUsage) {
    val cardColor = try {
        Color(android.graphics.Color.parseColor(usage.card.color))
    } catch (e: Exception) {
        MaterialTheme.colorScheme.primary
    }

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        // Nombre + porcentaje
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(cardColor)
                )
                Text(
                    text = usage.card.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                text = "${usage.usagePercentage.toInt()}%",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = usageColor(usage.usagePercentage)
            )
        }

        // Barra horizontal: segmento usado (color tarjeta) + segmento libre (gris)
        AnimatedSegmentedBar(
            usedFraction = (usage.usagePercentage / 100f).coerceIn(0f, 1f),
            usedColor = cardColor,
            freeColor = MaterialTheme.colorScheme.surfaceVariant,
            height = 12.dp
        )

        // Montos detallados
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            LabeledAmount(
                label = "Usado",
                amount = usage.usedAmount,
                color = usageColor(usage.usagePercentage)
            )
            LabeledAmount(
                label = "Libre",
                amount = usage.availableAmount,
                color = Color(0xFF2E7D32)
            )
            LabeledAmount(
                label = "Próx. corte",
                amount = usage.nextCutoffPayment,
                color = MaterialTheme.colorScheme.tertiary
            )
        }

        // Chips de contexto
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (usage.activePurchasesCount > 0) {
                InfoChip(
                    text = "${usage.activePurchasesCount} compra${if (usage.activePurchasesCount != 1) "s" else ""} activa${if (usage.activePurchasesCount != 1) "s" else ""}",
                    color = MaterialTheme.colorScheme.tertiaryContainer,
                    textColor = MaterialTheme.colorScheme.onTertiaryContainer
                )
            }
            if (usage.remainingDebt > 0) {
                InfoChip(
                    text = "${formatCurrency(usage.remainingDebt)} pendiente total",
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    textColor = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Sección 4 — Proyección mensual de cuotas
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun MonthlyProjectionSection(projection: List<MonthlyInstallmentTotal>) {
    val maxTotal = projection.maxOfOrNull { it.total }?.takeIf { it > 0 } ?: return

    SectionCard(
        title = "Cuotas por Vencer",
        subtitle = "Compromisos de pago en los próximos 6 meses",
        icon = Icons.Default.CalendarMonth
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            projection.forEach { month ->
                MonthlyProjectionRow(
                    month = month,
                    maxTotal = maxTotal
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "* Incluye todas las cuotas diferidas activas de todas las tarjetas",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun MonthlyProjectionRow(
    month: MonthlyInstallmentTotal,
    maxTotal: Double
) {
    val fraction = (month.total / maxTotal).toFloat().coerceIn(0f, 1f)
    val monthNames = listOf(
        "Ene", "Feb", "Mar", "Abr", "May", "Jun",
        "Jul", "Ago", "Sep", "Oct", "Nov", "Dic"
    )
    val label = "${monthNames[month.month - 1]} ${month.year}"
    val barColor = if (month.isCurrentMonth) MaterialTheme.colorScheme.primary
    else MaterialTheme.colorScheme.secondary

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Etiqueta del mes
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = if (month.isCurrentMonth) FontWeight.Bold else FontWeight.Normal,
            color = if (month.isCurrentMonth) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.width(56.dp)
        )

        // Barra proporcional
        Box(
            modifier = Modifier
                .weight(1f)
                .height(22.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            if (fraction > 0f) {
                AnimatedLinearBar(
                    progress = fraction,
                    color = barColor,
                    height = 22.dp
                )
            }
        }

        // Monto a la derecha
        Text(
            text = if (month.total > 0) formatCurrencyCompact(month.total) else "—",
            style = MaterialTheme.typography.bodySmall,
            fontWeight = if (month.isCurrentMonth) FontWeight.Bold else FontWeight.Normal,
            color = if (month.isCurrentMonth) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(72.dp),
            textAlign = TextAlign.End
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Sección 5 — Gastos por categoría
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun ExpensesByCategorySection(
    expenses: List<CategoryExpense>,
    total: Double
) {
    SectionCard(
        title = "Gastos por Categoría",
        subtitle = "Cuentas de ahorro",
        icon = Icons.Default.PieChart
    ) {
        if (total <= 0) {
            EmptySectionMessage("No hay gastos registrados en el período")
            return@SectionCard
        }
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            expenses.forEach { expense ->
                CategoryRow(
                    expense = expense,
                    total = total
                )
            }
        }
    }
}

@Composable
private fun CategoryRow(expense: CategoryExpense, total: Double) {
    val fraction = if (total > 0) (expense.amount / total).toFloat().coerceIn(0f, 1f) else 0f
    val percentage = (fraction * 100).toInt()

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(expense.category.toColor())
                )
                Text(
                    text = expense.category.toDisplayName(),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1
                )
                Text(
                    text = "${expense.count} tx",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = formatCurrency(expense.amount),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "$percentage%",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        AnimatedLinearBar(
            progress = fraction,
            color = expense.category.toColor(),
            height = 6.dp
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Sección 6 — Próximos pagos
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun UpcomingPaymentsSection(payments: List<PaymentDue>) {
    SectionCard(title = "Próximos Pagos", icon = Icons.Default.Alarm) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            payments.forEach { payment ->
                UpcomingPaymentRow(payment = payment)
            }
        }
    }
}

@Composable
private fun UpcomingPaymentRow(payment: PaymentDue) {
    val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
    val days = payment.daysUntilDue(today)
    val isOverdue = payment.isOverdue(today)
    val isUrgent = !isOverdue && days <= 3

    val containerColor = when {
        isOverdue -> MaterialTheme.colorScheme.errorContainer
        isUrgent -> MaterialTheme.colorScheme.tertiaryContainer
        else -> MaterialTheme.colorScheme.surfaceVariant
    }
    val labelColor = when {
        isOverdue -> MaterialTheme.colorScheme.error
        isUrgent -> MaterialTheme.colorScheme.tertiary
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Surface(
        shape = MaterialTheme.shapes.medium,
        color = containerColor,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = payment.card.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = formatDate(payment.dueDate),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = when {
                        isOverdue -> "⚠ Vencido hace ${-days} día${if (-days != 1) "s" else ""}"
                        days == 0 -> "⚡ Vence hoy"
                        days == 1 -> "⚡ Vence mañana"
                        else -> "Vence en $days días"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = if (isOverdue || isUrgent) FontWeight.Bold else FontWeight.Normal,
                    color = labelColor
                )
            }
            Text(
                text = formatCurrency(payment.amount),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = labelColor
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Componentes de UI reutilizables
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun SectionCard(
    title: String,
    subtitle: String? = null,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    if (subtitle != null) {
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            content()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PeriodFilterSection(selected: DateFilter, onSelect: (DateFilter) -> Unit) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(DateFilter.entries) { filter ->
            FilterChip(
                selected = filter == selected,
                onClick = { onSelect(filter) },
                label = { Text(filter.displayName) }
            )
        }
    }
}

/**
 * Barra de progreso animada sin depender de Vico.
 */
@Composable
private fun AnimatedLinearBar(
    progress: Float,
    color: Color,
    height: androidx.compose.ui.unit.Dp,
    modifier: Modifier = Modifier
) {
    var animated by remember { mutableStateOf(false) }
    val animatedProgress by animateFloatAsState(
        targetValue = if (animated) progress else 0f,
        animationSpec = tween(durationMillis = 700),
        label = "bar_progress"
    )
    LaunchedEffect(progress) { animated = true }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(50))
            .background(MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(animatedProgress)
                .fillMaxHeight()
                .clip(RoundedCornerShape(50))
                .background(color)
        )
    }
}

/**
 * Barra segmentada (usado | libre) para las tarjetas.
 */
@Composable
private fun AnimatedSegmentedBar(
    usedFraction: Float,
    usedColor: Color,
    freeColor: Color,
    height: androidx.compose.ui.unit.Dp
) {
    var animated by remember { mutableStateOf(false) }
    val animatedFraction by animateFloatAsState(
        targetValue = if (animated) usedFraction else 0f,
        animationSpec = tween(durationMillis = 700),
        label = "segment_progress"
    )
    LaunchedEffect(usedFraction) { animated = true }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(50))
    ) {
        // Segmento usado
        if (animatedFraction > 0f) {
            Box(
                modifier = Modifier
                    .weight(animatedFraction)
                    .fillMaxHeight()
                    .background(usedColor)
            )
        }
        // Segmento libre
        val freeFraction = (1f - animatedFraction).coerceAtLeast(0f)
        if (freeFraction > 0f) {
            Box(
                modifier = Modifier
                    .weight(freeFraction)
                    .fillMaxHeight()
                    .background(freeColor)
            )
        }
    }
}

@Composable
private fun KpiBox(
    label: String,
    value: String,
    color: Color,
    contentColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = color,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = contentColor.copy(alpha = 0.8f)
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = contentColor,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun LabeledAmount(
    label: String,
    amount: Double,
    color: Color
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = formatCurrencyCompact(amount),
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun InfoChip(text: String, color: Color, textColor: Color) {
    Surface(
        shape = RoundedCornerShape(50),
        color = color
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = textColor,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        )
    }
}

@Composable
private fun EmptySectionMessage(text: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun EmptyReports() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Assessment,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
            Text(
                text = "Sin datos para mostrar",
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center
            )
            Text(
                text = "Agrega tarjetas, compras y gastos para ver tus reportes",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Estados de carga y error
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun ReportsLoading() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            CircularProgressIndicator()
            Text("Calculando reportes...", style = MaterialTheme.typography.bodyLarge)
        }
    }
}

@Composable
private fun ReportsError(error: String, onRetry: () -> Unit, onDismiss: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Error", style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onErrorContainer)
                Text(error, style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onErrorContainer, textAlign = TextAlign.Center)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = onDismiss) { Text("Cerrar") }
                    Button(onClick = onRetry) { Text("Reintentar") }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Helpers
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun usageColor(percentage: Float): Color = when {
    percentage >= 90f -> MaterialTheme.colorScheme.error
    percentage >= 70f -> Color(0xFFF57C00) // naranja advertencia
    else -> MaterialTheme.colorScheme.primary
}

private fun ExpenseCategory.toDisplayName(): String = when (this) {
    ExpenseCategory.FOOD -> "Alimentación"
    ExpenseCategory.TRANSPORT -> "Transporte"
    ExpenseCategory.ENTERTAINMENT -> "Entretenimiento"
    ExpenseCategory.SERVICES -> "Servicios"
    ExpenseCategory.SHOPPING -> "Compras"
    ExpenseCategory.HEALTH -> "Salud"
    ExpenseCategory.OTHER -> "Otros"
}

private fun ExpenseCategory.toColor(): Color = when (this) {
    ExpenseCategory.FOOD -> Color(0xFFEF5350)
    ExpenseCategory.TRANSPORT -> Color(0xFF26C6DA)
    ExpenseCategory.ENTERTAINMENT -> Color(0xFFFFCA28)
    ExpenseCategory.SERVICES -> Color(0xFF66BB6A)
    ExpenseCategory.SHOPPING -> Color(0xFFAB47BC)
    ExpenseCategory.HEALTH -> Color(0xFF42A5F5)
    ExpenseCategory.OTHER -> Color(0xFF78909C)
}

private fun formatDate(date: kotlinx.datetime.LocalDate): String {
    val months = listOf(
        "Ene", "Feb", "Mar", "Abr", "May", "Jun",
        "Jul", "Ago", "Sep", "Oct", "Nov", "Dic"
    )
    return "${date.dayOfMonth} ${months[date.monthNumber - 1]} ${date.year}"
}

private fun formatCurrency(amount: Double): String =
    NumberFormat.getCurrencyInstance(Locale("es", "CO")).format(amount)

/**
 * Formato compacto para valores grandes en espacios reducidos.
 * < 1M → $999.999 | ≥ 1M → $1,2M | ≥ 1B → $1,2B
 */
private fun formatCurrencyCompact(amount: Double): String {
    val symbol = "$"
    return when {
        amount >= 1_000_000_000 -> "$symbol${String.format("%.1f", amount / 1_000_000_000)}B"
        amount >= 1_000_000 -> "$symbol${String.format("%.1f", amount / 1_000_000)}M"
        else -> NumberFormat.getCurrencyInstance(Locale("es", "CO")).format(amount)
    }
}
