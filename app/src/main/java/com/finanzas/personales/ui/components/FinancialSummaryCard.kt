package com.finanzas.personales.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import java.text.NumberFormat
import java.util.*

/**
 * Componente reutilizable para mostrar métricas financieras
 * 
 * Muestra un card con un título, monto, icono y color personalizable.
 * Incluye animación de entrada para el monto.
 * 
 * @param title Título de la métrica (ej: "Disponible Total")
 * @param amount Monto a mostrar
 * @param icon Icono representativo de la métrica
 * @param containerColor Color de fondo del card
 * @param contentColor Color del contenido (texto e icono)
 * @param modifier Modificador opcional para personalización
 */
@Composable
fun FinancialSummaryCard(
    title: String,
    amount: Double,
    icon: ImageVector,
    containerColor: Color = MaterialTheme.colorScheme.primaryContainer,
    contentColor: Color = MaterialTheme.colorScheme.onPrimaryContainer,
    modifier: Modifier = Modifier
) {
    var animationPlayed by remember { mutableStateOf(false) }
    val animatedAmount by animateFloatAsState(
        targetValue = if (animationPlayed) amount.toFloat() else 0f,
        animationSpec = tween(durationMillis = 800),
        label = "amount_animation"
    )

    LaunchedEffect(Unit) {
        animationPlayed = true
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(120.dp),
        colors = CardDefaults.cardColors(
            containerColor = containerColor,
            contentColor = contentColor
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    modifier = Modifier.size(24.dp)
                )
            }

            Text(
                text = formatCurrency(animatedAmount.toDouble()),
                style = MaterialTheme.typography.headlineMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * Formatea un monto como moneda en formato colombiano (COP)
 */
private fun formatCurrency(amount: Double): String {
    val format = NumberFormat.getCurrencyInstance(Locale("es", "CO"))
    return format.format(amount)
}
