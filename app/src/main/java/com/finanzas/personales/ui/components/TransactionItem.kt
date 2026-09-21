package com.finanzas.personales.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.finanzas.personales.data.local.entities.ExpenseCategory
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import java.text.NumberFormat
import java.util.*

/**
 * Tipo de transacción para diferenciación visual
 */
enum class TransactionType {
    PURCHASE,  // Compra con tarjeta de crédito
    EXPENSE    // Gasto desde cuenta de ahorros
}

/**
 * Componente para mostrar una transacción en lista
 * 
 * Diferencia visualmente entre compras (tarjeta de crédito) y gastos (cuenta de ahorros).
 * Muestra descripción, monto, fecha y categoría/información adicional.
 * 
 * @param type Tipo de transacción (compra o gasto)
 * @param description Descripción de la transacción
 * @param amount Monto de la transacción
 * @param date Fecha de la transacción
 * @param category Categoría del gasto (solo para gastos)
 * @param installmentInfo Información de cuotas (solo para compras)
 * @param accountOrCardName Nombre de la cuenta o tarjeta asociada
 * @param onClick Callback cuando se hace click en la transacción
 * @param modifier Modificador opcional
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionItem(
    type: TransactionType,
    description: String,
    amount: Double,
    date: Instant,
    category: ExpenseCategory? = null,
    installmentInfo: String? = null,
    accountOrCardName: String,
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val icon = when (type) {
        TransactionType.PURCHASE -> Icons.Default.CreditCard
        TransactionType.EXPENSE -> getCategoryIcon(category)
    }

    val iconColor = when (type) {
        TransactionType.PURCHASE -> MaterialTheme.colorScheme.primary
        TransactionType.EXPENSE -> MaterialTheme.colorScheme.tertiary
    }

    val iconBackgroundColor = when (type) {
        TransactionType.PURCHASE -> MaterialTheme.colorScheme.primaryContainer
        TransactionType.EXPENSE -> MaterialTheme.colorScheme.tertiaryContainer
    }

    Card(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .semantics {
                contentDescription = "$description, ${formatCurrency(amount)}, ${formatDate(date)}"
            },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icono
            Surface(
                modifier = Modifier.size(48.dp),
                shape = MaterialTheme.shapes.small,
                color = iconBackgroundColor
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = when (type) {
                            TransactionType.PURCHASE -> "Compra con tarjeta de crédito"
                            TransactionType.EXPENSE -> "Gasto de cuenta de ahorros"
                        },
                        tint = iconColor,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Información de la transacción
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = description,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = accountOrCardName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = formatDate(date),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (installmentInfo != null) {
                        Text(
                            text = "•",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = installmentInfo,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    if (category != null) {
                        Text(
                            text = "•",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = getCategoryName(category),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.tertiary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Monto
            Text(
                text = formatCurrency(amount),
                style = MaterialTheme.typography.titleMedium,
                color = when (type) {
                    TransactionType.PURCHASE -> MaterialTheme.colorScheme.primary
                    TransactionType.EXPENSE -> MaterialTheme.colorScheme.error
                }
            )
        }
    }
}

/**
 * Obtiene el icono correspondiente a una categoría de gasto
 */
@Composable
private fun getCategoryIcon(category: ExpenseCategory?): ImageVector {
    return when (category) {
        ExpenseCategory.FOOD -> Icons.Default.Restaurant
        ExpenseCategory.TRANSPORT -> Icons.Default.DirectionsCar
        ExpenseCategory.ENTERTAINMENT -> Icons.Default.Movie
        ExpenseCategory.SERVICES -> Icons.Default.Build
        ExpenseCategory.SHOPPING -> Icons.Default.ShoppingCart
        ExpenseCategory.HEALTH -> Icons.Default.LocalHospital
        ExpenseCategory.OTHER -> Icons.Default.MoreHoriz
        null -> Icons.Default.AccountBalance
    }
}

/**
 * Obtiene el nombre legible de una categoría
 */
private fun getCategoryName(category: ExpenseCategory): String {
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
 * Formatea una fecha en formato legible
 */
private fun formatDate(instant: Instant): String {
    val localDateTime = instant.toLocalDateTime(TimeZone.currentSystemDefault())
    val day = localDateTime.dayOfMonth.toString().padStart(2, '0')
    val month = localDateTime.monthNumber.toString().padStart(2, '0')
    val year = localDateTime.year
    return "$day/$month/$year"
}

/**
 * Formatea un monto como moneda
 */
private fun formatCurrency(amount: Double): String {
    val format = NumberFormat.getCurrencyInstance(Locale("es", "CO"))
    return format.format(amount)
}
