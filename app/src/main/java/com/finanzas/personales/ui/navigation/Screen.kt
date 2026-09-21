package com.finanzas.personales.ui.navigation

/**
 * Sealed class representing all navigation destinations in the app
 */
sealed class Screen(val route: String) {
    // Bottom navigation screens
    data object Home : Screen("home")
    data object CreditCards : Screen("credit_cards?created={created}") {
        fun createRoute(created: Boolean = false) = if (created) {
            "credit_cards?created=true"
        } else {
            "credit_cards"
        }
    }
    data object SavingsAccounts : Screen("savings_accounts?created={created}") {
        fun createRoute(created: Boolean = false) = if (created) {
            "savings_accounts?created=true"
        } else {
            "savings_accounts"
        }
    }
    data object Transactions : Screen("transactions")
    data object Reports : Screen("reports")
    
    // Detail screens with arguments
    data object CardDetail : Screen("card_detail/{cardId}") {
        fun createRoute(cardId: Long) = "card_detail/$cardId"
    }
    
    data object AddEditCard : Screen("add_edit_card?cardId={cardId}") {
        fun createRoute(cardId: Long? = null) = if (cardId != null) {
            "add_edit_card?cardId=$cardId"
        } else {
            "add_edit_card"
        }
    }
    
    data object AccountDetail : Screen("account_detail/{accountId}") {
        fun createRoute(accountId: Long) = "account_detail/$accountId"
    }
    
    data object AddEditAccount : Screen("add_edit_account?accountId={accountId}") {
        fun createRoute(accountId: Long? = null) = if (accountId != null) {
            "add_edit_account?accountId=$accountId"
        } else {
            "add_edit_account"
        }
    }
    
    data object Settings : Screen("settings")
}

/**
 * Bottom navigation items
 */
val bottomNavItems = listOf(
    Screen.Home,
    Screen.CreditCards,
    Screen.SavingsAccounts,
    Screen.Transactions,
    Screen.Reports
)
