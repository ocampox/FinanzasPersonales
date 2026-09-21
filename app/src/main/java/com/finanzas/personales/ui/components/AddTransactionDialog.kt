package com.finanzas.personales.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.finanzas.personales.data.local.entities.ExpenseCategory

/**
 * Diálogo para agregar una nueva transacción (compra o gasto)
 * 
 * Incluye validación de formulario y manejo de errores.
 * 
 * @param type Tipo de transacción (compra o gasto)
 * @param onDismiss Callback cuando se cierra el diálogo
 * @param onConfirm Callback cuando se confirma la transacción con los datos validados
 * @param availableAccounts Lista de cuentas/tarjetas disponibles para seleccionar
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTransactionDialog(
    type: TransactionType,
    onDismiss: () -> Unit,
    onConfirm: (TransactionData) -> Unit,
    availableAccounts: List<AccountOption>,
    modifier: Modifier = Modifier
) {
    var description by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var installments by remember { mutableStateOf("1") }
    var selectedAccount by remember { mutableStateOf<AccountOption?>(availableAccounts.firstOrNull()) }
    var selectedCategory by remember { mutableStateOf(ExpenseCategory.OTHER) }
    var showCategoryMenu by remember { mutableStateOf(false) }
    var showAccountMenu by remember { mutableStateOf(false) }
    
    // Estados de validación
    var descriptionError by remember { mutableStateOf<String?>(null) }
    var amountError by remember { mutableStateOf<String?>(null) }
    var installmentsError by remember { mutableStateOf<String?>(null) }

    val title = when (type) {
        TransactionType.PURCHASE -> "Nueva Compra"
        TransactionType.EXPENSE -> "Nuevo Gasto"
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = modifier
                .fillMaxWidth()
                .heightIn(max = 600.dp),
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
                    text = title,
                    style = MaterialTheme.typography.headlineSmall
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
                    placeholder = { Text("Ej: Supermercado") },
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

                // Selector de cuenta/tarjeta
                ExposedDropdownMenuBox(
                    expanded = showAccountMenu,
                    onExpandedChange = { showAccountMenu = it }
                ) {
                    OutlinedTextField(
                        value = selectedAccount?.name ?: "Seleccionar",
                        onValueChange = {},
                        readOnly = true,
                        label = {
                            Text(
                                when (type) {
                                    TransactionType.PURCHASE -> "Tarjeta"
                                    TransactionType.EXPENSE -> "Cuenta"
                                }
                            )
                        },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showAccountMenu) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = showAccountMenu,
                        onDismissRequest = { showAccountMenu = false }
                    ) {
                        availableAccounts.forEach { account ->
                            DropdownMenuItem(
                                text = { Text(account.name) },
                                onClick = {
                                    selectedAccount = account
                                    showAccountMenu = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Campo específico según tipo
                when (type) {
                    TransactionType.PURCHASE -> {
                        // Campo de cuotas
                        OutlinedTextField(
                            value = installments,
                            onValueChange = {
                                if (it.isEmpty() || it.matches(Regex("^\\d+$"))) {
                                    installments = it
                                    installmentsError = null
                                }
                            },
                            label = { Text("Número de cuotas") },
                            placeholder = { Text("1") },
                            isError = installmentsError != null,
                            supportingText = {
                                if (installmentsError != null) {
                                    Text(installmentsError!!)
                                } else {
                                    val installmentAmount = calculateInstallmentAmount(amount, installments)
                                    if (installmentAmount > 0) {
                                        Text("Valor por cuota: ${formatCurrency(installmentAmount)}")
                                    }
                                }
                            },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    TransactionType.EXPENSE -> {
                        // Selector de categoría
                        ExposedDropdownMenuBox(
                            expanded = showCategoryMenu,
                            onExpandedChange = { showCategoryMenu = it }
                        ) {
                            OutlinedTextField(
                                value = getCategoryName(selectedCategory),
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Categoría") },
                                leadingIcon = {
                                    Icon(
                                        imageVector = getCategoryIcon(selectedCategory),
                                        contentDescription = null
                                    )
                                },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showCategoryMenu) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .menuAnchor()
                            )
                            ExposedDropdownMenu(
                                expanded = showCategoryMenu,
                                onDismissRequest = { showCategoryMenu = false }
                            ) {
                                ExpenseCategory.values().forEach { category ->
                                    DropdownMenuItem(
                                        text = { Text(getCategoryName(category)) },
                                        leadingIcon = {
                                            Icon(
                                                imageVector = getCategoryIcon(category),
                                                contentDescription = null
                                            )
                                        },
                                        onClick = {
                                            selectedCategory = category
                                            showCategoryMenu = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

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
                                amountError = "Ingrese un monto válido"
                                hasErrors = true
                            }

                            if (type == TransactionType.PURCHASE) {
                                val installmentsValue = installments.toIntOrNull()
                                if (installmentsValue == null || installmentsValue < 1 || installmentsValue > 36) {
                                    installmentsError = "Las cuotas deben estar entre 1 y 36"
                                    hasErrors = true
                                }
                            }

                            if (selectedAccount == null) {
                                hasErrors = true
                            }

                            // Si no hay errores, confirmar
                            if (!hasErrors) {
                                val data = TransactionData(
                                    description = description.trim(),
                                    amount = amountValue!!,
                                    accountId = selectedAccount!!.id,
                                    installments = if (type == TransactionType.PURCHASE) installments.toInt() else 1,
                                    category = if (type == TransactionType.EXPENSE) selectedCategory else null
                                )
                                onConfirm(data)
                            }
                        }
                    ) {
                        Text("Guardar")
                    }
                }
            }
        }
    }
}

/**
 * Datos de una transacción validados
 */
data class TransactionData(
    val description: String,
    val amount: Double,
    val accountId: Long,
    val installments: Int = 1,
    val category: ExpenseCategory? = null
)

/**
 * Opción de cuenta/tarjeta para el selector
 */
data class AccountOption(
    val id: Long,
    val name: String
)

/**
 * Calcula el valor de cada cuota
 */
private fun calculateInstallmentAmount(amount: String, installments: String): Double {
    val amountValue = amount.toDoubleOrNull() ?: return 0.0
    val installmentsValue = installments.toIntOrNull() ?: return 0.0
    if (installmentsValue <= 0) return 0.0
    return amountValue / installmentsValue
}

/**
 * Formatea un monto como moneda
 */
private fun formatCurrency(amount: Double): String {
    return java.text.NumberFormat.getCurrencyInstance(java.util.Locale("es", "CO")).format(amount)
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
 * Obtiene el icono de una categoría
 */
@Composable
private fun getCategoryIcon(category: ExpenseCategory): androidx.compose.ui.graphics.vector.ImageVector {
    return when (category) {
        ExpenseCategory.FOOD -> Icons.Default.Restaurant
        ExpenseCategory.TRANSPORT -> Icons.Default.DirectionsCar
        ExpenseCategory.ENTERTAINMENT -> Icons.Default.Movie
        ExpenseCategory.SERVICES -> Icons.Default.Build
        ExpenseCategory.SHOPPING -> Icons.Default.ShoppingCart
        ExpenseCategory.HEALTH -> Icons.Default.LocalHospital
        ExpenseCategory.OTHER -> Icons.Default.MoreHoriz
    }
}
