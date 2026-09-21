package com.finanzas.personales.ui.navigation

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.finanzas.personales.ui.screens.accounts.AccountDetailScreen
import com.finanzas.personales.ui.screens.accounts.AddEditAccountScreen
import com.finanzas.personales.ui.screens.accounts.SavingsAccountsScreen
import com.finanzas.personales.ui.screens.cards.AddEditCardScreen
import com.finanzas.personales.ui.screens.cards.CardDetailScreen
import com.finanzas.personales.ui.screens.cards.CreditCardsScreen
import com.finanzas.personales.ui.screens.home.HomeScreen
import com.finanzas.personales.ui.screens.reports.ReportsScreen
import com.finanzas.personales.ui.screens.settings.SettingsScreen
import com.finanzas.personales.ui.screens.transactions.TransactionsScreen
import kotlinx.coroutines.launch

/**
 * Grafo de navegación de la app.
 *
 * La ruta raíz "main" contiene el HorizontalPager con las 5 pestañas.
 * Todas las demás rutas son pantallas de detalle que se apilan sobre el pager.
 *
 * @param pagerState Estado del pager compartido con AppScaffold para sincronizar el BottomBar.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun NavGraph(
    navController: NavHostController,
    pagerState: PagerState,
    modifier: Modifier = Modifier,
    startDestination: String = "main"
) {
    val scope = rememberCoroutineScope()

    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier,
        // Las transiciones entre detalle y pager
        enterTransition = {
            slideInHorizontally(initialOffsetX = { it }, animationSpec = tween(300)) +
                fadeIn(animationSpec = tween(300))
        },
        exitTransition = {
            slideOutHorizontally(targetOffsetX = { -it }, animationSpec = tween(300)) +
                fadeOut(animationSpec = tween(300))
        },
        popEnterTransition = {
            slideInHorizontally(initialOffsetX = { -it }, animationSpec = tween(300)) +
                fadeIn(animationSpec = tween(300))
        },
        popExitTransition = {
            slideOutHorizontally(targetOffsetX = { it }, animationSpec = tween(300)) +
                fadeOut(animationSpec = tween(300))
        }
    ) {

        // ── Pantallas principales (pager) ─────────────────────────────
        composable(route = "main") {
            HorizontalPager(
                state = pagerState,
                userScrollEnabled = true,
                key = { it }
            ) { page ->
                when (page) {
                    0 -> HomeScreen(
                        onNavigateToCardDetail = { cardId ->
                            navController.navigate(Screen.CardDetail.createRoute(cardId))
                        },
                        onNavigateToCards = {
                            scope.launch { pagerState.animateScrollToPage(1) }
                        },
                        onNavigateToAccounts = {
                            scope.launch { pagerState.animateScrollToPage(2) }
                        },
                        onNavigateToSettings = {
                            navController.navigate(Screen.Settings.route)
                        }
                    )
                    1 -> CreditCardsScreen(
                        onNavigateToCardDetail = { cardId ->
                            navController.navigate(Screen.CardDetail.createRoute(cardId))
                        },
                        onNavigateToAddCard = {
                            navController.navigate(Screen.AddEditCard.createRoute())
                        },
                        onClearSuccessMessage = {},
                        onNavigateToSettings = {
                            navController.navigate(Screen.Settings.route)
                        }
                    )
                    2 -> SavingsAccountsScreen(
                        onNavigateToAccountDetail = { accountId ->
                            navController.navigate(Screen.AccountDetail.createRoute(accountId))
                        },
                        onNavigateToAddAccount = {
                            navController.navigate(Screen.AddEditAccount.createRoute())
                        },
                        onClearSuccessMessage = {},
                        onNavigateToSettings = {
                            navController.navigate(Screen.Settings.route)
                        }
                    )
                    3 -> TransactionsScreen(
                        onNavigateToCardDetail = { cardId ->
                            navController.navigate(Screen.CardDetail.createRoute(cardId))
                        },
                        onNavigateToAccountDetail = { accountId ->
                            navController.navigate(Screen.AccountDetail.createRoute(accountId))
                        },
                        onNavigateToSettings = {
                            navController.navigate(Screen.Settings.route)
                        }
                    )
                    4 -> ReportsScreen(
                        onNavigateToSettings = {
                            navController.navigate(Screen.Settings.route)
                        }
                    )
                }
            }
        }

        // ── Card Detail ───────────────────────────────────────────────
        composable(
            route = Screen.CardDetail.route,
            arguments = listOf(navArgument("cardId") { type = NavType.LongType })
        ) {
            CardDetailScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToEdit = { id -> navController.navigate(Screen.AddEditCard.createRoute(id)) }
            )
        }

        // ── Add/Edit Card ─────────────────────────────────────────────
        composable(
            route = Screen.AddEditCard.route,
            arguments = listOf(navArgument("cardId") { type = NavType.LongType; defaultValue = -1L })
        ) {
            AddEditCardScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToCards = {
                    // Volver al pager en la pestaña Tarjetas
                    scope.launch { pagerState.animateScrollToPage(1) }
                    navController.popBackStack("main", inclusive = false)
                }
            )
        }

        // ── Account Detail ────────────────────────────────────────────
        composable(
            route = Screen.AccountDetail.route,
            arguments = listOf(navArgument("accountId") { type = NavType.LongType })
        ) {
            AccountDetailScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToEdit = { id -> navController.navigate(Screen.AddEditAccount.createRoute(id)) }
            )
        }

        // ── Add/Edit Account ──────────────────────────────────────────
        composable(
            route = Screen.AddEditAccount.route,
            arguments = listOf(navArgument("accountId") { type = NavType.LongType; defaultValue = -1L })
        ) {
            AddEditAccountScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToAccounts = {
                    scope.launch { pagerState.animateScrollToPage(2) }
                    navController.popBackStack("main", inclusive = false)
                }
            )
        }

        // ── Settings ──────────────────────────────────────────────────
        composable(route = Screen.Settings.route) {
            SettingsScreen()
        }
    }
}
