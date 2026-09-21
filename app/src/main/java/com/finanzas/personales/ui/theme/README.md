# Design System - Finanzas Personales

This directory contains the complete design system for the Finance app, implementing Material Design 3 guidelines with custom specifications.

## Files Overview

### Color.kt
Defines the complete color palette for the app:
- **Light Theme Colors**: Primary, Secondary, Tertiary, Error, Background, Surface colors
- **Dark Theme Colors**: Corresponding dark theme variants
- **Semantic Colors**: Success, Error, Warning, Info colors for consistent UI feedback

#### Semantic Color Usage
- **Success** (`SuccessColor`): Positive actions, successful transactions, positive balances
- **Error** (`ErrorColor`): Negative actions, errors, failed operations
- **Warning** (`WarningColor`): Cautionary states, low balance warnings, approaching limits
- **Info** (`InfoColor`): Informational messages, tips, neutral notifications

### Theme.kt
Main theme composable that applies the design system:
- Supports light and dark themes
- Supports dynamic colors (Android 12+)
- Configures status bar appearance
- Applies color scheme, typography, and shapes

#### Usage
```kotlin
@Composable
fun MyScreen() {
    FinanceTheme {
        // Your composable content
    }
}
```

#### Parameters
- `darkTheme`: Boolean - Use dark theme (defaults to system setting)
- `dynamicColor`: Boolean - Use dynamic colors on Android 12+ (defaults to true)

### Type.kt
Typography system following Material Design 3 type scale:

| Style | Size | Weight | Usage |
|-------|------|--------|-------|
| Display Large | 57sp | Normal | Hero sections |
| Display Medium | 45sp | Normal | Large headers |
| Display Small | 36sp | Normal | Section headers |
| Headline Large | 32sp | Bold | Major headings |
| Headline Medium | 28sp | Bold | Section headings |
| Headline Small | 24sp | SemiBold | Subsection headings |
| Title Large | 22sp | SemiBold | Card titles |
| Title Medium | 16sp | Medium | List item titles |
| Title Small | 14sp | Medium | Small titles |
| Body Large | 16sp | Normal | Primary content |
| Body Medium | 14sp | Normal | Secondary content |
| Body Small | 12sp | Normal | Tertiary content |
| Label Large | 14sp | Medium | Button text |
| Label Medium | 12sp | Medium | Form labels |
| Label Small | 11sp | Medium | Captions |

### Shape.kt
Shape system for consistent corner radius:

| Shape | Radius | Usage |
|-------|--------|-------|
| Extra Small | 4dp | Chips, small buttons |
| Small | 8dp | Buttons, text fields |
| Medium | 12dp | Cards, dialogs |
| Large | 16dp | Bottom sheets, large cards |
| Extra Large | 28dp | Full screen dialogs |

## Design Specifications

### Spacing System
Use multiples of 8dp for consistent spacing:
- **Extra Small**: 4dp
- **Small**: 8dp
- **Medium**: 16dp
- **Large**: 24dp
- **Extra Large**: 32dp

### Elevation
- **Normal Cards**: 2.dp
- **Elevated Cards**: 4.dp
- **Dialogs**: 6.dp
- **Bottom Sheets**: 8.dp

### Icon Sizes
- **Standard Icons**: 24.dp
- **Large Icons**: 48.dp
- **Small Icons**: 16.dp

### Touch Targets
Minimum touch target size: **48.dp** (for accessibility)

## Accessibility

The design system follows WCAG AA guidelines:
- **Contrast Ratios**: Minimum 4.5:1 for normal text
- **Touch Targets**: Minimum 48.dp for all interactive elements
- **Text Scaling**: Supports system text scaling
- **Content Descriptions**: All interactive elements should have contentDescription

## Examples

### Using Semantic Colors
```kotlin
// Success state
Card(
    colors = CardDefaults.cardColors(
        containerColor = SuccessContainerLight
    )
) {
    Text(
        text = "Transaction successful",
        color = SuccessColor
    )
}

// Warning state
Card(
    colors = CardDefaults.cardColors(
        containerColor = WarningContainerLight
    )
) {
    Text(
        text = "Low balance warning",
        color = WarningColor
    )
}
```

### Using Typography
```kotlin
Column {
    Text(
        text = "Total Available",
        style = MaterialTheme.typography.titleLarge
    )
    Text(
        text = "$1,234.56",
        style = MaterialTheme.typography.displayMedium
    )
    Text(
        text = "Last updated: Today",
        style = MaterialTheme.typography.bodySmall
    )
}
```

### Using Shapes
```kotlin
Card(
    shape = MaterialTheme.shapes.large,
    modifier = Modifier.padding(16.dp)
) {
    // Card content
}

Button(
    shape = MaterialTheme.shapes.medium,
    onClick = { }
) {
    Text("Add Transaction")
}
```

## Theme Customization

To customize the theme for specific screens or components:

```kotlin
// Override specific colors
CompositionLocalProvider(
    LocalContentColor provides MaterialTheme.colorScheme.primary
) {
    // Content with custom color
}

// Force dark theme for a section
FinanceTheme(darkTheme = true) {
    // Content in dark theme
}
```

## Best Practices

1. **Always use theme colors** instead of hardcoded colors
2. **Use semantic colors** for consistent feedback (Success, Error, Warning, Info)
3. **Follow the spacing system** (multiples of 8dp)
4. **Use appropriate typography styles** for hierarchy
5. **Ensure minimum touch targets** of 48.dp
6. **Test in both light and dark themes**
7. **Verify color contrast ratios** for accessibility
8. **Use shapes from the theme** for consistency
