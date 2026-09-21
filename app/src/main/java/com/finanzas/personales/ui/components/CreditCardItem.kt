package com.finanzas.personales.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.finanzas.personales.domain.models.CreditCard
import java.text.NumberFormat
import java.util.*

/**
 * Componente para mostrar una tarjeta de crédito en lista
 *
 * Muestra información de la tarjeta incluyendo nombre, cupo usado/total,
 * barra de progreso visual del uso del cupo, pago mínimo estimado del próximo
 * corte y un badge con el número de compras con cuotas activas.
 *
 * @param creditCard              Datos de la tarjeta de crédito
 * @param nextPayment             Pago mínimo estimado del próximo corte (opcional)
 * @param activePurchasesCount    Número de compras con cuotas pendientes (badge, 0 = oculto)
 * @param onClick                 Callback cuando se hace click en la tarjeta
 * @param modifier                Modificador opcional
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreditCardItem(
    creditCard: CreditCard,
    nextPayment: Double? = null,
    activePurchasesCount: Int = 0,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var animationPlayed by remember { mutableStateOf(false) }
    val animatedProgress by animateFloatAsState(
        targetValue = if (animationPlayed) creditCard.usagePercentage / 100f else 0f,
        animationSpec = tween(durationMillis = 1000),
        label = "progress_animation"
    )

    LaunchedEffect(Unit) {
        animationPlayed = true
    }

    val cardColor = try {
        Color(android.graphics.Color.parseColor(creditCard.color))
    } catch (e: Exception) {
        MaterialTheme.colorScheme.primaryContainer
    }

    Card(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .semantics {
                contentDescription = "Tarjeta de crédito ${creditCard.name}, cupo usado ${String.format("%.0f", creditCard.usedLimit)} de ${String.format("%.0f", creditCard.totalLimit)}"
            },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header con icono, nombre y badge de cuotas activas
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Surface(
                        modifier = Modifier.size(40.dp),
                        shape = MaterialTheme.shapes.small,
                        color = cardColor
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CreditCard,
                                contentDescription = "Icono de tarjeta de crédito",
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = creditCard.name,
                        style = MaterialTheme.typography.titleLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Badge: número de compras con cuotas activas
                if (activePurchasesCount > 0) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = activePurchasesCount.toString(),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onTertiary
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Información de cupo
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Cupo usado",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = formatCurrency(creditCard.usedLimit),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Cupo total",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = formatCurrency(creditCard.totalLimit),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Barra de progreso
            Column {
                LinearProgressIndicator(
                    progress = animatedProgress,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp),
                    color = getProgressColor(creditCard.usagePercentage),
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${creditCard.usagePercentage.toInt()}% usado",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Pago mínimo estimado del próximo corte
            nextPayment?.let { payment ->
                if (payment > 0) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Divider()
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Próximo pago estimado",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "próximo corte",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            text = formatCurrency(payment),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}

/**
 * Determina el color de la barra de progreso según el porcentaje de uso
 */
@Composable
private fun getProgressColor(usagePercentage: Float): Color {
    return when {
        usagePercentage >= 90f -> MaterialTheme.colorScheme.error
        usagePercentage >= 70f -> Color(0xFFFFA726) // Warning color
        else -> MaterialTheme.colorScheme.primary
    }
}

/**
 * Formatea un monto como moneda
 */
private fun formatCurrency(amount: Double): String {
    val format = NumberFormat.getCurrencyInstance(Locale("es", "CO"))
    return format.format(amount)
}
