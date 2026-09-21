# Navigation System

This package contains the complete navigation system for the Finance app.

## Components

### Screen.kt
Defines all navigation routes as a sealed class hierarchy:
- **Bottom Navigation Screens**: Home, CreditCards, SavingsAccounts, Transactions, Reports
- **Detail Screens**: CardDetail, AccountDetail (with ID parameters)
- **Add/Edit Screens**: AddEditCard, AddEditAccount (with optional ID parameters)

Each screen with parameters provides a `createRoute()` function for type-safe navigation.

### BottomNavigationBar.kt
Material 3 bottom navigation bar component with:
- 5 navigation items (Home, Tarjetas, Cuentas, Transacciones, Reportes)
- Icons and labels for each item
- Automatic state management (selected item highlighting)
- Proper back stack handling (saves and restores state)

### NavGraph.kt
Main navigation graph that defines:
- All composable destinations
- Navigation arguments for detail screens
- Navigation callbacks for each screen
- Placeholder for Reports screen (to be implemented in task 13)

### AppScaffold.kt
Main app scaffold that combines:
- NavHost with all navigation routes
- Bottom navigation bar (shown only on main screens)
- Proper padding handling for content

## Navigation Flow

### Main Screens (with bottom bar)
```
Home ←→ CreditCards ←→ SavingsAccounts ←→ Transactions ←→ Reports
```

### Detail Navigation
```
Home → CardDetail → AddEditCard
CreditCards → CardDetail → AddEditCard
CreditCards → AddEditCard (new card)

Home → AccountDetail → AddEditAccount
SavingsAccounts → AccountDetail → AddEditAccount
SavingsAccounts → AddEditAccount (new account)
```

## Usage

### MainActivity
```kotlin
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            FinanceTheme {
                AppScaffold()
            }
        }
    }
}
```

### Navigating from a Screen
```kotlin
// Navigate to detail with ID
navController.navigate(Screen.CardDetail.createRoute(cardId))

// Navigate to add/edit screen
navController.navigate(Screen.AddEditCard.createRoute()) // New
navController.navigate(Screen.AddEditCard.createRoute(cardId)) // Edit

// Navigate back
navController.popBackStack()
```

## Back Stack Management

The bottom navigation bar implements proper back stack management:
- `popUpTo(Screen.Home.route)` - Prevents building up a large stack
- `saveState = true` - Saves state when navigating away
- `restoreState = true` - Restores state when returning
- `launchSingleTop = true` - Avoids multiple copies of same destination

## Requirements Fulfilled

This implementation satisfies requirement 8.4:
- ✅ NavHost configured with Jetpack Navigation Compose
- ✅ Routes defined for all screens
- ✅ BottomNavigationBar with 5 main sections
- ✅ Navigation between screens with arguments
- ✅ Proper back stack management
