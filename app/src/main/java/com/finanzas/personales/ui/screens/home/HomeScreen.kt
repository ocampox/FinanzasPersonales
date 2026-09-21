package com.finanzas.personales.ui.screens.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Wallet
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.finanzas.personales.domain.models.PaymentDue
import com.finanzas.personales.ui.components.FinancialSummaryCard
import com.finanzas.personales.ui.theme.ErrorColor
import com.finanzas.personales.ui.theme.SuccessColor
import com.finanzas.personales.ui.theme.WarningColor
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import java.text.NumberFormat
import java.util.*

/**
 * Pantalla principal de la aplicación
 * 
 * Muestra el resumen financiero del usuario con:
 * - Disponible Total (suma de saldos en cuentas)
 * - Deuda Total (suma de cupo usado en tarjetas)
 * - Disponible Real (disponible - deuda)
 * - Lista de próximos pagos de tarjetas
 * 
 * Los datos se actualizan en tiempo real usando StateFlow.
 * 
 * Requirements: 5.4, 5.5, 7.1, 7.4
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel = hiltViewModel(),
    onNavigateToCardDetail: (Long) -> Unit = {},
    onNavigateToCards: () -> Unit = {},
    onNavigateToAccounts: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Finanzas Personales") },
                actions = {
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Ajustes"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                uiState.isLoading -> {
                    LoadingContent()
                }
                uiState.error != null -> {
                    ErrorContent(
                        error = uiState.error!!,
                        onRetry = { viewModel.loadData() },
                        onDismiss = { viewModel.clearError() }
                    )
                }
                uiState.financialSummary != null -> {
                    HomeContent(
                        uiState = uiState,
                        onCardClick = onNavigateToCardDetail
                    )
                }
                else -> {
                    EmptyContent()
                }
            }
        }
    }
}

/**
 * Contenido principal de la pantalla Home
 */
@Composable
private fun HomeContent(
    uiState: HomeUiState,
    onCardClick: (Long) -> Unit
) {
    val summary = uiState.financialSummary!!

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Sección de resumen financiero
        item {
            Text(
                text = "Resumen Financiero",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }

        // Card de Disponible Total
        item {
            FinancialSummaryCard(
                title = "Disponible Total",
                amount = summary.totalAvailable,
                icon = Icons.Default.AccountBalance,
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }

        // Card de Deuda Total
        item {
            FinancialSummaryCard(
                title = "Deuda Total",
                amount = summary.totalDebt,
                icon = Icons.Default.CreditCard,
                containerColor = if (summary.totalDebt > 0) {
                    MaterialTheme.colorScheme.errorContainer
                } else {
                    MaterialTheme.colorScheme.secondaryContainer
                },
                contentColor = if (summary.totalDebt > 0) {
                    MaterialTheme.colorScheme.onErrorContainer
                } else {
                    MaterialTheme.colorScheme.onSecondaryContainer
                }
            )
        }

        // Card de Pago Mínimo Total
        item {
            FinancialSummaryCard(
                title = "Pago Mínimo Total",
                amount = summary.totalMinimumPayments,
                icon = Icons.Default.CreditCard,
                containerColor = if (summary.totalMinimumPayments > 0) {
                    WarningColor.copy(alpha = 0.2f)
                } else {
                    MaterialTheme.colorScheme.secondaryContainer
                },
                contentColor = if (summary.totalMinimumPayments > 0) {
                    WarningColor
                } else {
                    MaterialTheme.colorScheme.onSecondaryContainer
                }
            )
        }

        // Card de Disponible Real
        item {
            FinancialSummaryCard(
                title = "Disponible Real",
                amount = summary.realAvailable,
                icon = Icons.Default.Wallet,
                containerColor = when {
                    summary.realAvailable > 0 -> MaterialTheme.colorScheme.tertiaryContainer
                    summary.realAvailable < 0 -> MaterialTheme.colorScheme.errorContainer
                    else -> MaterialTheme.colorScheme.surfaceVariant
                },
                contentColor = when {
                    summary.realAvailable > 0 -> MaterialTheme.colorScheme.onTertiaryContainer
                    summary.realAvailable < 0 -> MaterialTheme.colorScheme.onErrorContainer
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
        }

        // Sección de próximos pagos
        if (uiState.upcomingPayments.isNotEmpty()) {
            item {
                Text(
                    text = "Próximos Pagos",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(top = 8.dp, bottom = 8.dp)
                )
            }

            items(
                items = uiState.upcomingPayments,
                key = { it.card.id }
            ) { payment ->
                PaymentDueCard(
                    payment = payment,
                    onClick = { onCardClick(payment.card.id) }
                )
            }
        } else {
            item {
                Text(
                    text = "No hay pagos próximos",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp)
                )
            }
        }
    }
}

/**
 * Card que muestra información de un pago próximo
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PaymentDueCard(
    payment: PaymentDue,
    onClick: () -> Unit
) {
    val currentDate = Clock.System.now()
        .toLocalDateTime(TimeZone.currentSystemDefault())
        .date
    
    val daysUntilDue = payment.daysUntilDue(currentDate)
    val isOverdue = payment.isOverdue(currentDate)
    val isUpcoming = payment.isUpcoming(currentDate)

    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = when {
                isOverdue -> MaterialTheme.colorScheme.errorContainer
                isUpcoming -> MaterialTheme.colorScheme.tertiaryContainer
                else -> MaterialTheme.colorScheme.surfaceVariant
            }
        ),
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
                        text = payment.card.name,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = formatDate(payment.dueDate),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                Text(
                    text = formatCurrency(payment.amount),
                    style = MaterialTheme.typography.titleLarge,
                    color = when {
                        isOverdue -> ErrorColor
                        isUpcoming -> WarningColor
                        else -> SuccessColor
                    }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Indicador de días hasta el vencimiento
            Text(
                text = when {
                    isOverdue -> "Vencido hace ${-daysUntilDue} días"
                    daysUntilDue == 0 -> "Vence hoy"
                    daysUntilDue == 1 -> "Vence mañana"
                    else -> "Vence en $daysUntilDue días"
                },
                style = MaterialTheme.typography.bodySmall,
                color = when {
                    isOverdue -> ErrorColor
                    isUpcoming -> WarningColor
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
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
                text = "Cargando datos...",
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
    onRetry: () -> Unit,
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
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(onClick = onDismiss) {
                        Text("Cerrar")
                    }
                    Button(onClick = onRetry) {
                        Text("Reintentar")
                    }
                }
            }
        }
    }
}

/**
 * Contenido mostrado cuando no hay datos
 */
@Composable
private fun EmptyContent() {
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
                imageVector = Icons.Default.AccountBalance,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "No hay datos disponibles",
                style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Center
            )
            Text(
                text = "Agrega tarjetas de crédito y cuentas de ahorros para comenzar",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Formatea una fecha en formato legible
 */
private fun formatDate(date: kotlinx.datetime.LocalDate): String {
    val months = listOf(
        "Ene", "Feb", "Mar", "Abr", "May", "Jun",
        "Jul", "Ago", "Sep", "Oct", "Nov", "Dic"
    )
    return "${date.dayOfMonth} ${months[date.monthNumber - 1]} ${date.year}"
}

/**
 * Formatea un monto como moneda en formato colombiano (COP)
 */
private fun formatCurrency(amount: Double): String {
    val format = NumberFormat.getCurrencyInstance(Locale("es", "CO"))
    return format.format(amount)
}