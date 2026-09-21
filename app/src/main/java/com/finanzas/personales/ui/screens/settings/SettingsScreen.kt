package com.finanzas.personales.ui.screens.settings

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import java.io.File
import java.io.FileOutputStream

/**
 * Pantalla de configuración
 * 
 * Permite al usuario activar/desactivar notificaciones y configurar
 * otras opciones de la aplicación.
 * 
 * Requirements: 10.5
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var showImportDialog by remember { mutableStateOf(false) }
    var selectedFileUri by remember { mutableStateOf<Uri?>(null) }
    var showRepairConfirmDialog by remember { mutableStateOf(false) }

    // Cargar tarjetas para el ajuste de cupo
    LaunchedEffect(Unit) { viewModel.loadCards() }
    
    // Launcher para seleccionar archivo
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            selectedFileUri = it
            showImportDialog = true
        }
    }
    
    // Snackbar host
    val snackbarHostState = remember { SnackbarHostState() }
    
    // Mostrar mensajes de éxito/error
    LaunchedEffect(uiState.exportMessage) {
        uiState.exportMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearExportMessage()
        }
    }
    
    LaunchedEffect(uiState.exportError) {
        uiState.exportError?.let {
            snackbarHostState.showSnackbar("Error: $it")
            viewModel.clearExportMessage()
        }
    }
    
    LaunchedEffect(uiState.importMessage) {
        uiState.importMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearImportMessage()
        }
    }
    
    LaunchedEffect(uiState.importError) {
        uiState.importError?.let {
            snackbarHostState.showSnackbar("Error: $it")
            viewModel.clearImportMessage()
        }
    }

    LaunchedEffect(uiState.repairMessage) {
        uiState.repairMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearRepairMessage()
        }
    }

    LaunchedEffect(uiState.repairError) {
        uiState.repairError?.let {
            snackbarHostState.showSnackbar("Error: $it")
            viewModel.clearRepairMessage()
        }
    }

    LaunchedEffect(uiState.recalculateMessage) {
        uiState.recalculateMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearRecalculateMessage()
        }
    }

    LaunchedEffect(uiState.recalculateError) {
        uiState.recalculateError?.let {
            snackbarHostState.showSnackbar("Error: $it")
            viewModel.clearRecalculateMessage()
        }
    }

    LaunchedEffect(uiState.adjustMessage) {
        uiState.adjustMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearAdjustMessage()
        }
    }

    LaunchedEffect(uiState.adjustError) {
        uiState.adjustError?.let {
            snackbarHostState.showSnackbar("Error: $it")
            viewModel.clearAdjustMessage()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Configuración") }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Sección de notificaciones
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Notifications,
                                contentDescription = "Notificaciones",
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Column {
                                Text(
                                    text = "Notificaciones de recordatorio",
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Text(
                                    text = "Recibe notificaciones sobre pagos próximos",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        
                        Switch(
                            checked = uiState.notificationsEnabled,
                            onCheckedChange = { viewModel.toggleNotifications(it) }
                        )
                    }
                    
                    if (uiState.notificationsEnabled) {
                        Text(
                            text = "Las notificaciones se mostrarán 3 días antes de cada pago",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(start = 40.dp)
                        )
                    }
                }
            }
            
            // Sección de backup y exportación
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Backup,
                            contentDescription = "Backup",
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Backup y Exportación",
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                    
                    Text(
                        text = "Exporta tus datos a un archivo JSON o restaura desde un backup anterior",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { viewModel.exportData() },
                            enabled = !uiState.isExporting,
                            modifier = Modifier.weight(1f)
                        ) {
                            if (uiState.isExporting) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.FileDownload,
                                    contentDescription = "Exportar",
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Exportar")
                        }
                        
                        Button(
                            onClick = { filePickerLauncher.launch("application/json") },
                            enabled = !uiState.isImporting,
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        ) {
                            if (uiState.isImporting) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.FileUpload,
                                    contentDescription = "Importar",
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Importar")
                        }
                    }
                }
            }
            
            // Información adicional
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Acerca de las notificaciones",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Las notificaciones te ayudan a recordar los pagos pendientes de tus tarjetas de crédito. Se enviarán automáticamente 3 días antes de cada fecha de pago.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Sección de mantenimiento
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Build,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Mantenimiento",
                            style = MaterialTheme.typography.titleMedium
                        )
                    }

                    Text(
                        text = "Repara compras que aparecen como activas pero cuya última cuota ya venció. Útil si el conteo de compras activas muestra un número incorrecto.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Button(
                        onClick = { showRepairConfirmDialog = true },
                        enabled = !uiState.isRepairing,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondary
                        )
                    ) {
                        if (uiState.isRepairing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = MaterialTheme.colorScheme.onSecondary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Reparando...")
                        } else {
                            Icon(
                                imageVector = Icons.Default.Build,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Reparar cuotas")
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                        onClick = { viewModel.recalculateAllMinimumPayments() },
                        enabled = !uiState.isRecalculating,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.tertiary
                        )
                    ) {
                        if (uiState.isRecalculating) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = MaterialTheme.colorScheme.onTertiary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Recalculando...")
                        } else {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Recalcular pago mínimo")
                        }
                    }
                    Text(
                        text = "Recalcula el pago mínimo proyectado del próximo corte con las cuotas actuales. No modifica cuotas ni cupos.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Sección de ajuste de cupo disponible
            if (uiState.cards.isNotEmpty()) {
                AdjustAvailableLimitCard(
                    cards = uiState.cards,
                    isAdjusting = uiState.isAdjusting,
                    onConfirm = { cardId, newLimit ->
                        viewModel.adjustAvailableLimit(cardId, newLimit)
                    }
                )
            }
        }
    }
    
    // Diálogo de confirmación de reparación
    if (showRepairConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showRepairConfirmDialog = false },
            icon = { Icon(Icons.Default.Build, contentDescription = null) },
            title = { Text("Reparar cuotas") },
            text = {
                Text(
                    "Se sincronizarán las cuotas con vencimiento hasta el último corte " +
                    "completado de cada tarjeta y se limpiarán los pagos pendientes de " +
                    "las tarjetas corregidas. Esta acción solo avanzará cuotas pendientes " +
                    "y no modificará el cupo.\n\n" +
                    "Esta operación no se puede deshacer. ¿Deseas continuar?"
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showRepairConfirmDialog = false
                        viewModel.repairInstallments()
                    }
                ) {
                    Text("Reparar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRepairConfirmDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    // Diálogo para elegir entre reemplazar o fusionar
    if (showImportDialog && selectedFileUri != null) {
        AlertDialog(
            onDismissRequest = { 
                showImportDialog = false
                selectedFileUri = null
            },
            title = { Text("Importar datos") },
            text = { Text("¿Cómo deseas importar los datos?\n\n• Reemplazar: Elimina todos los datos actuales e importa los del archivo\n• Fusionar: Agrega los datos del archivo a los existentes") },
            confirmButton = {
                Button(
                    onClick = {
                        selectedFileUri?.let { uri ->
                            val filePath = copyUriToFile(context, uri)
                            filePath?.let {
                                viewModel.importData(it, merge = false)
                            }
                        }
                        showImportDialog = false
                        selectedFileUri = null
                    }
                ) {
                    Text("Reemplazar")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        selectedFileUri?.let { uri ->
                            val filePath = copyUriToFile(context, uri)
                            filePath?.let {
                                viewModel.importData(it, merge = true)
                            }
                        }
                        showImportDialog = false
                        selectedFileUri = null
                    }
                ) {
                    Text("Fusionar")
                }
            }
        )
    }
}

/**
 * Card para ajustar manualmente el cupo disponible de una tarjeta.
 * Útil cuando el valor real del banco difiere del calculado por la app.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AdjustAvailableLimitCard(
    cards: List<com.finanzas.personales.domain.models.CreditCard>,
    isAdjusting: Boolean,
    onConfirm: (Long, Double) -> Unit
) {
    var selectedCard by remember { mutableStateOf(cards.first()) }
    var newLimit by remember { mutableStateOf("") }
    var limitError by remember { mutableStateOf<String?>(null) }
    var expanded by remember { mutableStateOf(false) }

    val currencyFormat = java.text.NumberFormat.getCurrencyInstance(java.util.Locale("es", "CO"))

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.EditNote,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Ajustar cupo disponible",
                    style = MaterialTheme.typography.titleMedium
                )
            }

            Text(
                text = "Corrige el cupo disponible de una tarjeta cuando el valor del banco difiere del calculado por la app. No afecta el historial ni las cuotas.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Selector de tarjeta
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = it }
            ) {
                OutlinedTextField(
                    value = "${selectedCard.name} — disponible: ${currencyFormat.format(selectedCard.availableLimit)}",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Tarjeta") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor()
                )
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    cards.forEach { card ->
                        DropdownMenuItem(
                            text = {
                                Column {
                                    Text(card.name, style = MaterialTheme.typography.bodyMedium)
                                    Text(
                                        "Disponible: ${currencyFormat.format(card.availableLimit)} / Total: ${currencyFormat.format(card.totalLimit)}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            },
                            onClick = {
                                selectedCard = card
                                newLimit = ""
                                limitError = null
                                expanded = false
                            }
                        )
                    }
                }
            }

            // Campo de nuevo cupo
            OutlinedTextField(
                value = newLimit,
                onValueChange = {
                    if (it.isEmpty() || it.matches(Regex("^\\d*\\.?\\d*$"))) {
                        newLimit = it
                        limitError = null
                    }
                },
                label = { Text("Nuevo cupo disponible") },
                placeholder = { Text(currencyFormat.format(selectedCard.availableLimit)) },
                supportingText = {
                    if (limitError != null) Text(limitError!!)
                    else Text("Cupo total: ${currencyFormat.format(selectedCard.totalLimit)}")
                },
                isError = limitError != null,
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                    keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal
                ),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                prefix = { Text("$") }
            )

            Button(
                onClick = {
                    val value = newLimit.toDoubleOrNull()
                    when {
                        value == null || value < 0 ->
                            limitError = "Ingresa un valor válido"
                        value > selectedCard.totalLimit ->
                            limitError = "No puede superar el cupo total (${currencyFormat.format(selectedCard.totalLimit)})"
                        else -> onConfirm(selectedCard.id, value)
                    }
                },
                enabled = !isAdjusting && newLimit.isNotBlank(),
                modifier = Modifier.fillMaxWidth()
            ) {
                if (isAdjusting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Guardando...")
                } else {
                    Icon(
                        imageVector = Icons.Default.Save,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Aplicar ajuste")
                }
            }
        }
    }
}

/**
 * Copia un URI a un archivo temporal para poder leerlo
 */
private fun copyUriToFile(context: android.content.Context, uri: Uri): String? {
    return try {
        val inputStream = context.contentResolver.openInputStream(uri)
        val tempFile = File(context.cacheDir, "import_backup.json")
        FileOutputStream(tempFile).use { output ->
            inputStream?.use { input ->
                input.copyTo(output)
            }
        }
        tempFile.absolutePath
    } catch (e: Exception) {
        null
    }
}

