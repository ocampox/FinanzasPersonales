package com.finanzas.personales.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

/**
 * Wrapper para gráficos con título y opciones
 * 
 * Proporciona un contenedor consistente para todos los gráficos de la aplicación
 * con título, subtítulo opcional y menú de opciones.
 * 
 * @param title Título del gráfico
 * @param subtitle Subtítulo opcional (ej: período de tiempo)
 * @param onOptionsClick Callback para el botón de opciones (opcional)
 * @param modifier Modificador opcional
 * @param content Contenido del gráfico (composable)
 */
@Composable
fun ChartCard(
    title: String,
    subtitle: String? = null,
    onOptionsClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header con título y opciones
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    subtitle?.let {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                onOptionsClick?.let { onClick ->
                    IconButton(onClick = onClick) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Opciones",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Contenido del gráfico
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 200.dp),
                contentAlignment = Alignment.Center
            ) {
                content()
            }
        }
    }
}

/**
 * Variante de ChartCard con altura personalizable
 * 
 * @param title Título del gráfico
 * @param subtitle Subtítulo opcional
 * @param height Altura del contenedor del gráfico
 * @param onOptionsClick Callback para el botón de opciones
 * @param modifier Modificador opcional
 * @param content Contenido del gráfico
 */
@Composable
fun ChartCard(
    title: String,
    subtitle: String? = null,
    height: androidx.compose.ui.unit.Dp,
    onOptionsClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    subtitle?.let {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                onOptionsClick?.let { onClick ->
                    IconButton(onClick = onClick) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Opciones",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Contenido con altura fija
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(height),
                contentAlignment = Alignment.Center
            ) {
                content()
            }
        }
    }
}
