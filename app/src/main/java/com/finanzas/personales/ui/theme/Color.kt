package com.finanzas.personales.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Color palette for the Finance app
 * Following Material Design 3 color system with custom semantic colors
 */

// ============================================
// Light Theme Colors
// ============================================

val PrimaryLight = Color(0xFF006C4C)
val OnPrimaryLight = Color(0xFFFFFFFF)
val PrimaryContainerLight = Color(0xFF89F8C7)
val OnPrimaryContainerLight = Color(0xFF002114)

val SecondaryLight = Color(0xFF4D6357)
val OnSecondaryLight = Color(0xFFFFFFFF)
val SecondaryContainerLight = Color(0xFFCFE9D9)
val OnSecondaryContainerLight = Color(0xFF0A1F16)

val TertiaryLight = Color(0xFF3D6373)
val OnTertiaryLight = Color(0xFFFFFFFF)
val TertiaryContainerLight = Color(0xFFC1E8FB)
val OnTertiaryContainerLight = Color(0xFF001F29)

val ErrorLight = Color(0xFFBA1A1A)
val OnErrorLight = Color(0xFFFFFFFF)
val ErrorContainerLight = Color(0xFFFFDAD6)
val OnErrorContainerLight = Color(0xFF410002)

val BackgroundLight = Color(0xFFFBFDF9)
val OnBackgroundLight = Color(0xFF191C1A)
val SurfaceLight = Color(0xFFFBFDF9)
val OnSurfaceLight = Color(0xFF191C1A)
val SurfaceVariantLight = Color(0xFFDBE5DD)
val OnSurfaceVariantLight = Color(0xFF404943)

val OutlineLight = Color(0xFF707973)
val OutlineVariantLight = Color(0xFFBFC9C2)

// ============================================
// Dark Theme Colors
// ============================================

val PrimaryDark = Color(0xFF6FDB9F)
val OnPrimaryDark = Color(0xFF003826)
val PrimaryContainerDark = Color(0xFF005138)
val OnPrimaryContainerDark = Color(0xFF89F8C7)

val SecondaryDark = Color(0xFFB1CCBE)
val OnSecondaryDark = Color(0xFF1D352A)
val SecondaryContainerDark = Color(0xFF354B40)
val OnSecondaryContainerDark = Color(0xFFCFE9D9)

val TertiaryDark = Color(0xFFA8CEE0)
val OnTertiaryDark = Color(0xFF0E3445)
val TertiaryContainerDark = Color(0xFF254B5C)
val OnTertiaryContainerDark = Color(0xFFC1E8FB)

val ErrorDark = Color(0xFFFFB4AB)
val OnErrorDark = Color(0xFF690005)
val ErrorContainerDark = Color(0xFF93000A)
val OnErrorContainerDark = Color(0xFFFFDAD6)

val BackgroundDark = Color(0xFF191C1A)
val OnBackgroundDark = Color(0xFFE1E3DF)
val SurfaceDark = Color(0xFF191C1A)
val OnSurfaceDark = Color(0xFFE1E3DF)
val SurfaceVariantDark = Color(0xFF404943)
val OnSurfaceVariantDark = Color(0xFFBFC9C2)

val OutlineDark = Color(0xFF8A938C)
val OutlineVariantDark = Color(0xFF404943)

// ============================================
// Semantic Colors (Theme-independent)
// ============================================

/**
 * Success color for positive actions and states
 * Used for: successful transactions, positive balances, confirmations
 */
val SuccessColor = Color(0xFF4CAF50)
val OnSuccessColor = Color(0xFFFFFFFF)
val SuccessContainerLight = Color(0xFFC8E6C9)
val SuccessContainerDark = Color(0xFF1B5E20)

/**
 * Error color for negative actions and states
 * Alias for Material error color for consistency
 */
val ErrorColor = Color(0xFFE53935)
val OnErrorColor = Color(0xFFFFFFFF)

/**
 * Warning color for cautionary states
 * Used for: low balance warnings, approaching limits, pending actions
 */
val WarningColor = Color(0xFFFFA726)
val OnWarningColor = Color(0xFF000000)
val WarningContainerLight = Color(0xFFFFE0B2)
val WarningContainerDark = Color(0xFFE65100)

/**
 * Info color for informational states
 * Used for: tips, information messages, neutral notifications
 */
val InfoColor = Color(0xFF29B6F6)
val OnInfoColor = Color(0xFFFFFFFF)
val InfoContainerLight = Color(0xFFB3E5FC)
val InfoContainerDark = Color(0xFF01579B)
