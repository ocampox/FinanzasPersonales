package com.finanzas.personales.ui.navigation

import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.launch

/**
 * Scaffold principal de la app.
 *
 * El HorizontalPager vive dentro del composable "main" del NavGraph,
 * lo que permite tanto swipe entre pestañas como navegación a pantallas
 * de detalle con back stack correcto.
 *
 * El BottomBar se muestra cuando la ruta activa es "main"
 * (cualquier pestaña del pager) y se oculta en pantallas de detalle.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AppScaffold(initialRoute: String = Screen.Home.route) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val scope = rememberCoroutineScope()

    // El pager tiene 5 páginas; la página inicial deriva de la ruta pedida
    val mainRoutes = listOf(
        Screen.Home.route,
        Screen.CreditCards.route,
        Screen.SavingsAccounts.route,
        Screen.Transactions.route,
        Screen.Reports.route
    )
    val initialPage = mainRoutes.indexOf(initialRoute).coerceAtLeast(0)
    val pagerState = rememberPagerState(initialPage = initialPage) { 5 }

    // El BottomBar solo se muestra cuando la ruta activa es "main"
    val showBottomBar = currentRoute == "main" || currentRoute == null

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                SwipeableBottomNavigationBar(
                    selectedIndex = pagerState.targetPage,
                    onItemSelected = { index ->
                        scope.launch {
                            pagerState.animateScrollToPage(
                                page = index,
                                animationSpec = tween(durationMillis = 250)
                            )
                        }
                    }
                )
            }
        }
    ) { paddingValues ->
        NavGraph(
            navController = navController,
            pagerState = pagerState,
            modifier = Modifier.padding(paddingValues),
            startDestination = "main"
        )
    }
}
