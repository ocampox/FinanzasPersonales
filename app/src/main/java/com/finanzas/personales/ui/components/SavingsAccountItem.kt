package com.finanzas.personales.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.finanzas.personales.domain.models.SavingsAccount
import com.finanzas.personales.ui.theme.SuccessColor
import java.text.NumberFormat
import java.util.*

/**
 * Componente para mostrar una cuenta de ahorros en lista
 * 
 * Muestra información de la cuenta incluyendo nombre y saldo actual.
 * Incluye animación de entrada para el saldo.
 * 
 * @param account Datos de la cuenta de ahorros
 * @param onClick Callback cuando se hace click en la cuenta
 * @param modifier Modificador opcional
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SavingsAccountItem(
    account: SavingsAccount,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var animationPlayed by remember { mutableStateOf(false) }
    val animatedBalance by animateFloatAsState(
        targetValue = if (animationPlayed) account.balance.toFloat() else 0f,
        animationSpec = tween(durationMillis = 800),
        label = "balance_animation"
    )

    LaunchedEffect(Unit) {
        animationPlayed = true
    }

    val accountColor = try {
        Color(android.graphics.Color.parseColor(account.color))
    } catch (e: Exception) {
        MaterialTheme.colorScheme.secondaryContainer
    }

    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icono y nombre
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Surface(
                    modifier = Modifier.size(48.dp),
                    shape = MaterialTheme.shapes.small,
                    color = accountColor
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccountBalance,
                            contentDescription = "Icono de cuenta de ahorros",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = account.name,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Saldo disponible",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Saldo
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = formatCurrency(animatedBalance.toDouble()),
                    style = MaterialTheme.typography.titleLarge,
                    color = if (account.hasPositiveBalance) SuccessColor else MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

/**
 * Formatea un monto como moneda
 */
private fun formatCurrency(amount: Double): String {
    val format = NumberFormat.getCurrencyInstance(Locale("es", "CO"))
    return format.format(amount)
}
