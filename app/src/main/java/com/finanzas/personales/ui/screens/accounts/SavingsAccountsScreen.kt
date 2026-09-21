package com.finanzas.personales.ui.screens.accounts

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.finanzas.personales.ui.components.AnimatedListItem
import com.finanzas.personales.ui.components.SavingsAccountItem

/**
 * Pantalla de lista de cuentas de ahorros
 * 
 * Muestra todas las cuentas registradas con su información básica:
 * - Nombre de la cuenta
 * - Saldo actual
 * 
 * Incluye un FAB para agregar nuevas cuentas y navegación al detalle
 * al hacer click en una cuenta.
 * 
 * Requirements: 2.1, 2.2
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SavingsAccountsScreen(
    viewModel: SavingsAccountsViewModel = hiltViewModel(),
    showSuccessMessage: Boolean = false,
    onNavigateToAddAccount: () -> Unit = {},
    onNavigateToAccountDetail: (Long) -> Unit = {},
    onClearSuccessMessage: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    // Mostrar mensaje de éxito solo una vez cuando showSuccessMessage es true
    LaunchedEffect(showSuccessMessage) {
        if (showSuccessMessage) {
            snackbarHostState.showSnackbar(
                message = "Cuenta creada exitosamente",
                duration = SnackbarDuration.Short
            )
            // Limpiar el parámetro después de mostrar el mensaje
            onClearSuccessMessage()
            // Esperar 1 segundo antes de cancelar el snackbar si aún está visible
            kotlinx.coroutines.delay(1000)
            snackbarHostState.currentSnackbarData?.dismiss()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Cuentas de Ahorros") },
                actions = {
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Ajustes"
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateToAddAccount,
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Agregar cuenta"
                )
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                uiState.isLoading -> {
                    LoadingContent()
                }
                uiState.error != null -> {
                    ErrorContent(
                        error = uiState.error!!,
                        onRetry = { viewModel.refresh() },
                        onDismiss = { viewModel.clearError() }
                    )
                }
                uiState.savingsAccounts.isEmpty() -> {
                    EmptyContent(onAddAccount = onNavigateToAddAccount)
                }
                else -> {
                    SavingsAccountsContent(
                        uiState = uiState,
                        onAccountClick = onNavigateToAccountDetail
                    )
                }
            }
        }
    }
}

/**
 * Contenido principal con la lista de cuentas
 */
@Composable
private fun SavingsAccountsContent(
    uiState: SavingsAccountsUiState,
    onAccountClick: (Long) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = "Mis Cuentas (${uiState.savingsAccounts.size})",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 4.dp)
            )
        }

        items(
            items = uiState.savingsAccounts,
            key = { it.id }
        ) { account ->
            AnimatedListItem(item = account) { animatedAccount ->
                SavingsAccountItem(
                    account = animatedAccount,
                    onClick = { onAccountClick(animatedAccount.id) }
                )
            }
        }
    }
}

/**
 * Contenido mostrado mientras se cargan las cuentas
 */
@Composable
private fun LoadingContent() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            CircularProgressIndicator()
            Text(
                text = "Cargando cuentas...",
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}

/**
 * Contenido mostrado cuando hay un error
 */
@Composable
private fun ErrorContent(
    error: String,
    onRetry: () -> Unit,
    onDismiss: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.errorContainer
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Error",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
                Text(
                    text = error,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    textAlign = TextAlign.Center
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(onClick = onDismiss) {
                        Text("Cerrar")
                    }
                    Button(onClick = onRetry) {
                        Text("Reintentar")
                    }
                }
            }
        }
    }
}

/**
 * Contenido mostrado cuando no hay cuentas registradas
 */
@Composable
private fun EmptyContent(onAddAccount: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(32.dp)
        ) {
            Icon(
                imageVector = Icons.Default.AccountBalance,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "No hay cuentas registradas",
                style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Center
            )
            Text(
                text = "Agrega tu primera cuenta de ahorros para comenzar a gestionar tus finanzas",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            Button(onClick = onAddAccount) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Agregar Cuenta")
            }
        }
    }
}
