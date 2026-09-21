package com.finanzas.personales.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.compose.runtime.getValue

data class BottomNavItem(
    val screen: Screen,
    val icon: ImageVector,
    val label: String
)

val bottomNavigationItems = listOf(
    BottomNavItem(Screen.Home, Icons.Default.Home, "Inicio"),
    BottomNavItem(Screen.CreditCards, Icons.Default.CreditCard, "Tarjetas"),
    BottomNavItem(Screen.SavingsAccounts, Icons.Default.AccountBalance, "Cuentas"),
    BottomNavItem(Screen.Transactions, Icons.Default.Receipt, "Historial"),
    BottomNavItem(Screen.Reports, Icons.Default.BarChart, "Reportes")
)

/**
 * BottomBar sincronizado con el HorizontalPager.
 * Recibe el índice seleccionado y notifica al pager cuando el usuario toca un ítem.
 */
@Composable
fun SwipeableBottomNavigationBar(
    selectedIndex: Int,
    onItemSelected: (Int) -> Unit
) {
    NavigationBar {
        bottomNavigationItems.forEachIndexed { index, item ->
            NavigationBarItem(
                icon = { Icon(imageVector = item.icon, contentDescription = item.label) },
                label = { Text(item.label) },
                selected = selectedIndex == index,
                alwaysShowLabel = true,
                onClick = { onItemSelected(index) }
            )
        }
    }
}

/**
 * Versión original basada en NavController — se mantiene por compatibilidad
 * con cualquier referencia existente en el codebase.
 */
@Composable
fun BottomNavigationBar(navController: NavController) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val selectedIndex = bottomNavigationItems.indexOfFirst { it.screen.route == currentRoute }

    SwipeableBottomNavigationBar(
        selectedIndex = selectedIndex.coerceAtLeast(0),
        onItemSelected = { index ->
            val route = bottomNavigationItems[index].screen.route
            if (currentRoute != route) {
                navController.navigate(route) {
                    popUpTo(Screen.Home.route) { saveState = true }
                    launchSingleTop = true
                    restoreState = true
                }
            }
        }
    )
}
