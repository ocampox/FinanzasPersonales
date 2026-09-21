package com.finanzas.personales.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import java.text.NumberFormat
import java.util.*

/**
 * Diálogo para agregar o editar una compra a una tarjeta de crédito
 * 
 * Incluye validación de formulario, cálculo automático de cuotas,
 * y validación de cupo disponible.
 * 
 * Requirements: 3.1, 3.2, 3.3, 3.4
 * 
 * @param cardName Nombre de la tarjeta de crédito
 * @param availableCredit Cupo disponible en la tarjeta
 * @param purchase Compra existente para editar (null para crear nueva)
 * @param onDismiss Callback cuando se cierra el diálogo
 * @param onConfirm Callback cuando se confirma la compra con los datos validados
 */
@Composable
fun AddPurchaseDialog(
    cardName: String,
    availableCredit: Double,
    purchase: com.finanzas.personales.domain.models.Purchase? = null,
    onDismiss: () -> Unit,
    onConfirm: (PurchaseData) -> Unit,
    modifier: Modifier = Modifier
) {
    val isEditMode = purchase != null
    var description by remember(purchase) { mutableStateOf(purchase?.description ?: "") }
    var amount by remember(purchase) { mutableStateOf(purchase?.totalAmount?.toString() ?: "") }
    var installments by remember(purchase) { mutableStateOf(purchase?.installments?.toString() ?: "1") }
    
    // Estados de validación
    var descriptionError by remember { mutableStateOf<String?>(null) }
    var amountError by remember { mutableStateOf<String?>(null) }
    var installmentsError by remember { mutableStateOf<String?>(null) }

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
                    text = if (isEditMode) "Editar Compra" else "Nueva Compra",
                    style = MaterialTheme.typography.headlineSmall
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Información de la tarjeta
                Text(
                    text = "Tarjeta: $cardName",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Cupo disponible: ${formatCurrency(availableCredit)}",
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
                    placeholder = { Text("Ej: Supermercado, Ropa, etc.") },
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
                                Text(
                                    "Valor por cuota: ${formatCurrency(installmentAmount)}",
                                    color = MaterialTheme.colorScheme.primary
                                )
                            } else {
                                Text("Ingrese el monto y número de cuotas (1-36)")
                            }
                        }
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
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
                                // En modo edición, considerar el monto original al validar cupo
                                val originalAmount = purchase?.totalAmount ?: 0.0
                                val availableForThisPurchase = if (isEditMode) {
                                    availableCredit + originalAmount
                                } else {
                                    availableCredit
                                }
                                if (amountValue > availableForThisPurchase) {
                                    amountError = "El monto excede el cupo disponible (${formatCurrency(availableForThisPurchase)})"
                                    hasErrors = true
                                }
                            }

                            val installmentsValue = installments.toIntOrNull()
                            if (installmentsValue == null || installmentsValue < 1 || installmentsValue > 36) {
                                installmentsError = "Las cuotas deben estar entre 1 y 36"
                                hasErrors = true
                            }

                            // Si no hay errores, confirmar
                            if (!hasErrors) {
                                val installmentAmount = amountValue!! / installmentsValue!!
                                val data = PurchaseData(
                                    description = description.trim(),
                                    totalAmount = amountValue,
                                    installments = installmentsValue,
                                    installmentAmount = installmentAmount
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
 * Datos de una compra validados
 */
data class PurchaseData(
    val description: String,
    val totalAmount: Double,
    val installments: Int,
    val installmentAmount: Double
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
    return NumberFormat.getCurrencyInstance(Locale("es", "CO")).format(amount)
}
