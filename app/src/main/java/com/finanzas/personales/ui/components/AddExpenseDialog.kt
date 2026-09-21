package com.finanzas.personales.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.finanzas.personales.data.local.entities.ExpenseCategory
import java.text.NumberFormat
import java.util.*

/**
 * Diálogo para agregar o editar un gasto a una cuenta de ahorros
 * 
 * Incluye validación de formulario, selector de categoría con iconos,
 * y validación de saldo disponible.
 * 
 * Requirements: 6.1, 6.2, 6.3, 6.5
 * 
 * @param accountName Nombre de la cuenta de ahorros
 * @param availableBalance Saldo disponible en la cuenta
 * @param expense Gasto existente para editar (null para crear nuevo)
 * @param onDismiss Callback cuando se cierra el diálogo
 * @param onConfirm Callback cuando se confirma el gasto con los datos validados
 */
@Composable
fun AddExpenseDialog(
    accountName: String,
    availableBalance: Double,
    expense: com.finanzas.personales.domain.models.Expense? = null,
    onDismiss: () -> Unit,
    onConfirm: (ExpenseData) -> Unit,
    modifier: Modifier = Modifier
) {
    val isEditMode = expense != null
    var description by remember(expense) { mutableStateOf(expense?.description ?: "") }
    var amount by remember(expense) { mutableStateOf(expense?.amount?.toString() ?: "") }
    var selectedCategory by remember(expense) { mutableStateOf<ExpenseCategory?>(expense?.category) }
    
    // Estados de validación
    var descriptionError by remember { mutableStateOf<String?>(null) }
    var amountError by remember { mutableStateOf<String?>(null) }
    var categoryError by remember { mutableStateOf<String?>(null) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = modifier
                .fillMaxWidth()
                .heightIn(max = 700.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp)
            ) {
                // Título
                Text(
                    text = if (isEditMode) "Editar Gasto" else "Nuevo Gasto",
                    style = MaterialTheme.typography.headlineSmall
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Información de la cuenta
                Text(
                    text = "Cuenta: $accountName",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Saldo disponible: ${formatCurrency(availableBalance)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Campo de descripción
                OutlinedTextField(
                    value = description,
                    onValueChange = {
                        description = it
                        descriptionError = null
                    },
                    label = { Text("Descripción") },
                    placeholder = { Text("Ej: Supermercado, Gasolina, etc.") },
                    isError = descriptionError != null,
                    supportingText = descriptionError?.let { { Text(it) } },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Campo de monto
                OutlinedTextField(
                    value = amount,
                    onValueChange = {
                        if (it.isEmpty() || it.matches(Regex("^\\d*\\.?\\d*$"))) {
                            amount = it
                            amountError = null
                        }
                    },
                    label = { Text("Monto") },
                    placeholder = { Text("0.00") },
                    leadingIcon = {
                        Text(
                            text = "$",
                            style = MaterialTheme.typography.bodyLarge
                        )
                    },
                    isError = amountError != null,
                    supportingText = amountError?.let { { Text(it) } },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Selector de categoría
                Text(
                    text = "Categoría",
                    style = MaterialTheme.typography.titleSmall
                )
                
                if (categoryError != null) {
                    Text(
                        text = categoryError!!,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                CategorySelector(
                    selectedCategory = selectedCategory,
                    onCategorySelect = {
                        selectedCategory = it
                        categoryError = null
                    }
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Botones de acción
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancelar")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            // Validar formulario
                            var hasErrors = false

                            if (description.isBlank()) {
                                descriptionError = "La descripción es requerida"
                                hasErrors = true
                            }

                            val amountValue = amount.toDoubleOrNull()
                            if (amountValue == null || amountValue <= 0) {
                                amountError = "Ingrese un monto válido mayor a cero"
                                hasErrors = true
                            } else {
                                // En modo edición, considerar el monto original al validar saldo
                                val originalAmount = expense?.amount ?: 0.0
                                val availableForThisExpense = if (isEditMode) {
                                    availableBalance + originalAmount
                                } else {
                                    availableBalance
                                }
                                if (amountValue > availableForThisExpense) {
                                    amountError = "El monto excede el saldo disponible (${formatCurrency(availableForThisExpense)})"
                                    hasErrors = true
                                }
                            }

                            if (selectedCategory == null) {
                                categoryError = "Seleccione una categoría"
                                hasErrors = true
                            }

                            // Si no hay errores, confirmar
                            if (!hasErrors) {
                                val data = ExpenseData(
                                    description = description.trim(),
                                    amount = amountValue!!,
                                    category = selectedCategory!!
                                )
                                onConfirm(data)
                            }
                        }
                    ) {
                        Text(if (isEditMode) "Actualizar" else "Guardar")
                    }
                }
            }
        }
    }
}

/**
 * Selector de categoría con iconos
 */
@Composable
private fun CategorySelector(
    selectedCategory: ExpenseCategory?,
    onCategorySelect: (ExpenseCategory) -> Unit
) {
    val categories = listOf(
        ExpenseCategory.FOOD,
        ExpenseCategory.TRANSPORT,
        ExpenseCategory.ENTERTAINMENT,
        ExpenseCategory.SERVICES,
        ExpenseCategory.SHOPPING,
        ExpenseCategory.HEALTH,
        ExpenseCategory.OTHER
    )

    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.height(240.dp)
    ) {
        items(categories) { category ->
            CategoryOption(
                category = category,
                isSelected = category == selectedCategory,
                onClick = { onCategorySelect(category) }
            )
        }
    }
}

/**
 * Opción de categoría individual
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategoryOption(
    category: ExpenseCategory,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val categoryColor = getCategoryColor(category)
    val categoryIcon = getCategoryIcon(category)
    val categoryName = getCategoryName(category)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .then(
                if (isSelected) {
                    Modifier.border(
                        width = 3.dp,
                        color = MaterialTheme.colorScheme.primary,
                        shape = RoundedCornerShape(12.dp)
                    )
                } else {
                    Modifier
                }
            ),
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                categoryColor.copy(alpha = 0.2f)
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            }
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Surface(
                modifier = Modifier.size(40.dp),
                shape = RoundedCornerShape(8.dp),
                color = categoryColor
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = categoryIcon,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = categoryName,
                style = MaterialTheme.typography.bodySmall,
                color = if (isSelected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
        }
    }
}

/**
 * Datos de un gasto validados
 */
data class ExpenseData(
    val description: String,
    val amount: Double,
    val category: ExpenseCategory
)

private fun getCategoryIcon(category: ExpenseCategory): ImageVector = when (category) {
    ExpenseCategory.FOOD -> Icons.Default.Restaurant
    ExpenseCategory.TRANSPORT -> Icons.Default.DirectionsCar
    ExpenseCategory.ENTERTAINMENT -> Icons.Default.Movie
    ExpenseCategory.SERVICES -> Icons.Default.Build
    ExpenseCategory.SHOPPING -> Icons.Default.ShoppingBag
    ExpenseCategory.HEALTH -> Icons.Default.LocalHospital
    ExpenseCategory.OTHER -> Icons.Default.MoreHoriz
}

private fun getCategoryColor(category: ExpenseCategory): Color = when (category) {
    ExpenseCategory.FOOD -> Color(0xFFE53935)
    ExpenseCategory.TRANSPORT -> Color(0xFF1E88E5)
    ExpenseCategory.ENTERTAINMENT -> Color(0xFF8E24AA)
    ExpenseCategory.SERVICES -> Color(0xFFFB8C00)
    ExpenseCategory.SHOPPING -> Color(0xFF43A047)
    ExpenseCategory.HEALTH -> Color(0xFFD81B60)
    ExpenseCategory.OTHER -> Color(0xFF757575)
}

private fun getCategoryName(category: ExpenseCategory): String = when (category) {
    ExpenseCategory.FOOD -> "Alimentación"
    ExpenseCategory.TRANSPORT -> "Transporte"
    ExpenseCategory.ENTERTAINMENT -> "Entretenimiento"
    ExpenseCategory.SERVICES -> "Servicios"
    ExpenseCategory.SHOPPING -> "Compras"
    ExpenseCategory.HEALTH -> "Salud"
    ExpenseCategory.OTHER -> "Otros"
}

/**
 * Formatea un monto como moneda
 */
private fun formatCurrency(amount: Double): String {
    return NumberFormat.getCurrencyInstance(Locale("es", "CO")).format(amount)
}
