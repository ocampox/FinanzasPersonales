package com.finanzas.personales.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import java.text.NumberFormat
import java.util.Locale

/** Registra un pago real realizado a una tarjeta de crédito. */
@Composable
fun AddPaymentDialog(
    cardName: String,
    currentUsedLimit: Double,
    totalLimit: Double,
    onDismiss: () -> Unit,
    onConfirm: (Double) -> Unit,
    isLoading: Boolean = false,
    modifier: Modifier = Modifier
) {
    var amount by remember { mutableStateOf("") }
    var amountError by remember { mutableStateOf<String?>(null) }
    val availableCredit = totalLimit - currentUsedLimit

    Dialog(onDismissRequest = onDismiss) {
        Card(modifier = modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()).padding(24.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, modifier = Modifier.size(28.dp))
                    Spacer(Modifier.width(12.dp))
                    Text("Registrar abono", style = MaterialTheme.typography.headlineSmall)
                }
                Spacer(Modifier.height(12.dp))
                Text(
                    "Registra el pago realizado en $cardName. Liberará cupo y aplicará el importe a las cuotas pendientes más antiguas.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(20.dp))
                OutlinedTextField(
                    value = amount,
                    onValueChange = {
                        if (it.isEmpty() || it.matches(Regex("^\\d*\\.?\\d*$"))) {
                            amount = it
                            amountError = null
                        }
                    },
                    label = { Text("Monto pagado") },
                    leadingIcon = { Icon(Icons.Default.AttachMoney, contentDescription = null) },
                    isError = amountError != null,
                    supportingText = {
                        val value = amount.toDoubleOrNull()
                        when {
                            amountError != null -> Text(amountError!!)
                            value != null && value > 0 -> Text(
                                "Cupo disponible después: ${formatCurrency((availableCredit + value).coerceAtMost(totalLimit))}"
                            )
                            else -> Text("Máximo: ${formatCurrency(currentUsedLimit)}")
                        }
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(24.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss, enabled = !isLoading) { Text("Cancelar") }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val value = amount.toDoubleOrNull()
                            amountError = when {
                                value == null || value <= 0 -> "Ingrese un monto válido mayor a cero"
                                value > currentUsedLimit -> "El abono no puede superar la deuda registrada (${formatCurrency(currentUsedLimit)})"
                                else -> null
                            }
                            if (amountError == null) onConfirm(value!!)
                        },
                        enabled = !isLoading
                    ) {
                        if (isLoading) CircularProgressIndicator(Modifier.size(16.dp)) else Text("Guardar")
                    }
                }
            }
        }
    }
}

private fun formatCurrency(amount: Double): String =
    NumberFormat.getCurrencyInstance(Locale("es", "CO")).format(amount)
