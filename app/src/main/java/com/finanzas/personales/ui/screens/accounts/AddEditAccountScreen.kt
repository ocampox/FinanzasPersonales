package com.finanzas.personales.ui.screens.accounts

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.launch

/**
 * Pantalla para agregar o editar una cuenta de ahorros
 * 
 * Incluye formulario con validación para:
 * - Nombre de la cuenta
 * - Saldo inicial/actual
 * - Selector de color
 * 
 * Requirements: 2.1, 2.3
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditAccountScreen(
    viewModel: AddEditAccountViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit = {},
    onNavigateToAccounts: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    // Navegar cuando se guarda exitosamente
    LaunchedEffect(key1 = uiState.saveSuccess) {
        if (uiState.saveSuccess) {
            // Si es creación, navegar inmediatamente a la pantalla de cuentas; si es edición, volver atrás
            if (uiState.isEditMode) {
                // En edición, mostrar mensaje y volver atrás
                try {
                    snackbarHostState.showSnackbar(
                        message = "Cuenta actualizada exitosamente",
                        duration = SnackbarDuration.Short
                    )
                } catch (e: Exception) {
                    // Ignorar errores del snackbar
                }
                kotlinx.coroutines.delay(500)
                onNavigateBack()
            } else {
                // En creación, navegar inmediatamente sin delay
                onNavigateToAccounts()
            }
        }
    }

    // Mostrar errores
    LaunchedEffect(uiState.error) {
        uiState.error?.let { error ->
            snackbarHostState.showSnackbar(
                message = error,
                duration = SnackbarDuration.Short
            )
            viewModel.clearError()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { 
                    Text(if (uiState.isEditMode) "Editar Cuenta" else "Nueva Cuenta") 
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Volver"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (uiState.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
                AddEditAccountContent(
                    uiState = uiState,
                    onNameChange = viewModel::onNameChange,
                    onBalanceChange = viewModel::onBalanceChange,
                    onColorSelect = viewModel::onColorSelect,
                    onSave = viewModel::saveAccount
                )
            }
        }
    }
}

/**
 * Contenido del formulario
 */
@Composable
private fun AddEditAccountContent(
    uiState: AddEditAccountUiState,
    onNameChange: (String) -> Unit,
    onBalanceChange: (String) -> Unit,
    onColorSelect: (String) -> Unit,
    onSave: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Campo de nombre
        OutlinedTextField(
            value = uiState.name,
            onValueChange = onNameChange,
            label = { Text("Nombre de la cuenta") },
            placeholder = { Text("Ej: Cuenta Corriente, Ahorros Principal") },
            isError = uiState.nameError != null,
            supportingText = uiState.nameError?.let { { Text(it) } },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        // Campo de saldo
        OutlinedTextField(
            value = uiState.balance,
            onValueChange = onBalanceChange,
            label = { Text(if (uiState.isEditMode) "Saldo actual" else "Saldo inicial") },
            placeholder = { Text("Ej: 1000000") },
            isError = uiState.balanceError != null,
            supportingText = uiState.balanceError?.let { { Text(it) } },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            singleLine = true,
            prefix = { Text("$") }
        )

        // Selector de color
        ColorSelector(
            selectedColor = uiState.selectedColor,
            onColorSelect = onColorSelect
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Botón de guardar
        Button(
            onClick = onSave,
            modifier = Modifier.fillMaxWidth(),
            enabled = !uiState.isSaving
        ) {
            if (uiState.isSaving) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = MaterialTheme.colorScheme.onPrimary
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(if (uiState.isEditMode) "Guardar Cambios" else "Crear Cuenta")
        }
    }
}

/**
 * Selector de color para la cuenta
 */
@Composable
private fun ColorSelector(
    selectedColor: String,
    onColorSelect: (String) -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "Color de la cuenta",
            style = MaterialTheme.typography.titleSmall
        )

        val colors = listOf(
            "#006C4C", // Primary green
            "#E53935", // Red
            "#1E88E5", // Blue
            "#FB8C00", // Orange
            "#8E24AA", // Purple
            "#43A047", // Green
            "#00ACC1", // Cyan
            "#F4511E", // Deep orange
            "#5E35B1", // Deep purple
            "#C0CA33", // Lime
            "#00897B", // Teal
            "#D81B60"  // Pink
        )

        LazyVerticalGrid(
            columns = GridCells.Fixed(6),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.height(120.dp)
        ) {
            items(colors) { colorHex ->
                ColorOption(
                    color = colorHex,
                    isSelected = colorHex == selectedColor,
                    onClick = { onColorSelect(colorHex) }
                )
            }
        }
    }
}

/**
 * Opción de color individual
 */
@Composable
private fun ColorOption(
    color: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val parsedColor = try {
        Color(android.graphics.Color.parseColor(color))
    } catch (e: Exception) {
        MaterialTheme.colorScheme.primary
    }

    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(parsedColor)
            .then(
                if (isSelected) {
                    Modifier.border(
                        width = 3.dp,
                        color = MaterialTheme.colorScheme.primary,
                        shape = CircleShape
                    )
                } else {
                    Modifier
                }
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (isSelected) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "Seleccionado",
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}
