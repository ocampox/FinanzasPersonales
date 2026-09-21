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
import com.finanzas.personales.domain.models.IncomeCategory
import java.text.NumberFormat
import java.util.*

/**
 * Diálogo para agregar o editar un ingreso a una cuenta de ahorros
 * 
 * Incluye validación de formulario y selector de categoría con iconos.
 * 
 * @param accountName Nombre de la cuenta de ahorros
 * @param income Ingreso existente para editar (null para crear nuevo)
 * @param onDismiss Callback cuando se cierra el diálogo
 * @param onConfirm Callback cuando se confirma el ingreso con los datos validados
 */
@Composable
fun AddIncomeDialog(
    accountName: String,
    income: com.finanzas.personales.domain.models.Income? = null,
    onDismiss: () -> Unit,
    onConfirm: (IncomeData) -> Unit,
    modifier: Modifier = Modifier
) {
    val isEditMode = income != null
    var description by remember(income) { mutableStateOf(income?.description ?: "") }
    var amount by remember(income) { mutableStateOf(income?.amount?.toString() ?: "") }
    var selectedCategory by remember(income) { mutableStateOf<IncomeCategory?>(income?.category) }
    
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
                    text = if (isEditMode) "Editar Ingreso" else "Nuevo Ingreso",
                    style = MaterialTheme.typography.headlineSmall
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Información de la cuenta
                Text(
                    text = "Cuenta: $accountName",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
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
                    placeholder = { Text("Ej: Salario, Bonificación, Venta, etc.") },
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

                IncomeCategorySelector(
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
                            }

                            if (selectedCategory == null) {
                                categoryError = "Seleccione una categoría"
                                hasErrors = true
                            }

                            // Si no hay errores, confirmar
                            if (!hasErrors) {
                                val data = IncomeData(
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
 * Selector de categoría de ingresos con iconos
 */
@Composable
private fun IncomeCategorySelector(
    selectedCategory: IncomeCategory?,
    onCategorySelect: (IncomeCategory) -> Unit
) {
    val categories = listOf(
        IncomeCategory.SALARY,
        IncomeCategory.BONUS,
        IncomeCategory.FREELANCE,
        IncomeCategory.INVESTMENT,
        IncomeCategory.GIFT,
        IncomeCategory.REFUND,
        IncomeCategory.SALE,
        IncomeCategory.OTHER
    )

    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.height(280.dp)
    ) {
        items(categories) { category ->
            IncomeCategoryOption(
                category = category,
                isSelected = category == selectedCategory,
                onClick = { onCategorySelect(category) }
            )
        }
    }
}

/**
 * Opción de categoría de ingreso individual
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun IncomeCategoryOption(
    category: IncomeCategory,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val categoryColor = getIncomeCategoryColor(category)
    val categoryIcon = getIncomeCategoryIcon(category)
    val categoryName = getIncomeCategoryName(category)

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
 * Datos de un ingreso validados
 */
data class IncomeData(
    val description: String,
    val amount: Double,
    val category: IncomeCategory
)

private fun getIncomeCategoryIcon(category: IncomeCategory): ImageVector = when (category) {
    IncomeCategory.SALARY -> Icons.Default.Work
    IncomeCategory.BONUS -> Icons.Default.Star
    IncomeCategory.FREELANCE -> Icons.Default.Person
    IncomeCategory.INVESTMENT -> Icons.Default.TrendingUp
    IncomeCategory.GIFT -> Icons.Default.CardGiftcard
    IncomeCategory.REFUND -> Icons.Default.Undo
    IncomeCategory.SALE -> Icons.Default.Sell
    IncomeCategory.OTHER -> Icons.Default.MoreHoriz
}

private fun getIncomeCategoryColor(category: IncomeCategory): Color = when (category) {
    IncomeCategory.SALARY -> Color(0xFF4CAF50)
    IncomeCategory.BONUS -> Color(0xFFFFD700)
    IncomeCategory.FREELANCE -> Color(0xFF2196F3)
    IncomeCategory.INVESTMENT -> Color(0xFF9C27B0)
    IncomeCategory.GIFT -> Color(0xFFE91E63)
    IncomeCategory.REFUND -> Color(0xFF00BCD4)
    IncomeCategory.SALE -> Color(0xFFFF9800)
    IncomeCategory.OTHER -> Color(0xFF757575)
}

private fun getIncomeCategoryName(category: IncomeCategory): String = when (category) {
    IncomeCategory.SALARY -> "Salario"
    IncomeCategory.BONUS -> "Bonificación"
    IncomeCategory.FREELANCE -> "Freelance"
    IncomeCategory.INVESTMENT -> "Inversión"
    IncomeCategory.GIFT -> "Regalo"
    IncomeCategory.REFUND -> "Reembolso"
    IncomeCategory.SALE -> "Venta"
    IncomeCategory.OTHER -> "Otros"
}