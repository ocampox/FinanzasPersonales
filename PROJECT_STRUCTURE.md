# Estructura del Proyecto — Finanzas Personales

## Raíz

```
Finanzas/
├── app/                        # Módulo principal
├── gradle/wrapper/             # Gradle wrapper
├── .kiro/                      # Specs y configuración Kiro
├── README.md
├── PROJECT_STRUCTURE.md
├── build.gradle.kts            # Build raíz
└── settings.gradle.kts
```

---

## Árbol de código fuente

```
app/src/main/java/com/finanzas/personales/
│
├── data/
│   ├── local/
│   │   ├── dao/
│   │   │   ├── CreditCardDao.kt          CRUD tarjetas + updateCardPaymentInfo
│   │   │   ├── PurchaseDao.kt            CRUD compras + updateMultiplePaidInstallments
│   │   │   ├── SavingsAccountDao.kt      CRUD cuentas + getAccountByIdFlow
│   │   │   ├── ExpenseDao.kt
│   │   │   ├── IncomeDao.kt
│   │   │   └── CardPaymentDao.kt         Historial de abonos a tarjetas
│   │   │
│   │   ├── entities/
│   │   │   ├── CreditCardEntity.kt       availableLimit, lastCutoffDate, lastCutoffPayment,
│   │   │   │                             pendingMinimumPayment, minimumPaymentPercentage
│   │   │   ├── PurchaseEntity.kt         paidInstallments
│   │   │   ├── SavingsAccountEntity.kt
│   │   │   ├── ExpenseEntity.kt
│   │   │   ├── ExpenseCategory.kt
│   │   │   ├── IncomeEntity.kt
│   │   │   ├── IncomeCategory.kt
│   │   │   └── CardPaymentEntity.kt      amount, paymentType, cutoffDate, paymentDate
│   │   │
│   │   └── database/
│   │       ├── FinanceDatabase.kt        Room v6, 6 entidades
│   │       ├── Migrations.kt             MIGRATION_1_2 … MIGRATION_5_6
│   │       ├── Converters.kt
│   │       └── DatabaseCallback.kt
│   │
│   ├── models/
│   │   └── BackupData.kt                 v2.0: CreditCardBackup, PurchaseBackup,
│   │                                     SavingsAccountBackup, ExpenseBackup,
│   │                                     IncomeBackup, CardPaymentBackup
│   │
│   └── repository/
│       ├── FinanceRepository.kt          Interfaz completa
│       ├── FinanceRepositoryImpl.kt      Implementación + addPayment (3 ramas)
│       │                                 + recalculateNextCutoffPayment
│       │                                 + repairInstallments
│       │                                 + insertCardPaymentRecord
│       └── BackupService.kt             Exportación/importación JSON v2.0
│
├── domain/
│   ├── models/
│   │   ├── CreditCard.kt                lastCutoffPayment, minimumPaymentPercentage
│   │   ├── Purchase.kt                  getInstallmentsDueInCutoff, getRemainingInstallments
│   │   ├── SavingsAccount.kt
│   │   ├── Expense.kt
│   │   ├── Income.kt
│   │   ├── CardPayment.kt               paymentType: CAPITAL | MINIMUM_PAYMENT | MARK_AS_PAID
│   │   ├── PaymentType.kt
│   │   ├── PaymentDue.kt
│   │   ├── FinancialSummary.kt
│   │   ├── FinanceError.kt
│   │   ├── InstallmentDetail.kt
│   │   ├── CutoffPaymentBreakdown.kt
│   │   ├── Mappers.kt                   toDomain / toEntity para todas las entidades
│   │   └── Result.kt
│   │
│   └── usecases/
│       ├── AddPaymentUseCase.kt
│       ├── AddPurchaseUseCase.kt
│       ├── CalculateCardPaymentUseCase.kt
│       ├── CalculateFinancialSummaryUseCase.kt
│       ├── CalculateMinimumPaymentUseCase.kt
│       └── GetUpcomingPaymentsUseCase.kt   usa lastCutoffPayment como monto histórico
│
├── ui/
│   ├── screens/
│   │   ├── home/
│   │   │   ├── HomeScreen.kt            4 KPIs reactivos + próximos pagos
│   │   │   └── HomeViewModel.kt         Flow reactivo sobre 3 colecciones Room
│   │   │
│   │   ├── cards/
│   │   │   ├── CreditCardsScreen.kt     Badge cuotas activas + pago mínimo estimado
│   │   │   ├── CreditCardsViewModel.kt  combine(cards, purchases) reactivo
│   │   │   ├── CardDetailScreen.kt      Historial pagos + compras, filtros colapsables
│   │   │   ├── CardDetailViewModel.kt   observeAccountData, observePurchases, observePayments
│   │   │   ├── AddEditCardScreen.kt     minimumPaymentPercentage configurable
│   │   │   └── AddEditCardViewModel.kt
│   │   │
│   │   ├── accounts/
│   │   │   ├── SavingsAccountsScreen.kt
│   │   │   ├── SavingsAccountsViewModel.kt
│   │   │   ├── AccountDetailScreen.kt   Flow reactivo (cuenta + gastos + ingresos)
│   │   │   ├── AccountDetailViewModel.kt observeAccountData con combine de 3 flows
│   │   │   ├── AddEditAccountScreen.kt
│   │   │   └── AddEditAccountViewModel.kt
│   │   │
│   │   ├── transactions/
│   │   │   ├── TransactionsScreen.kt    Búsqueda + filtros (tipo, categoría, fecha)
│   │   │   ├── TransactionsViewModel.kt combine de 8 flows
│   │   │   └── TransactionUiModel.kt
│   │   │
│   │   ├── reports/
│   │   │   ├── ReportsScreen.kt         6 secciones: KPIs, balance, deuda por tarjeta,
│   │   │   │                            proyección 6 meses, categorías, próximos pagos
│   │   │   └── ReportsViewModel.kt      combine de 5 flows, sin Vico
│   │   │
│   │   └── settings/
│   │       ├── SettingsScreen.kt        Backup, reparación, ajuste de cupo
│   │       └── SettingsViewModel.kt     repairInstallments, recalculateAllMinimumPayments,
│   │                                    adjustAvailableLimit
│   │
│   ├── components/
│   │   ├── AddPaymentDialog.kt          Abono capital / pago mínimo / Ya pagué el mínimo
│   │   ├── AddPurchaseDialog.kt
│   │   ├── AddExpenseDialog.kt
│   │   ├── AddIncomeDialog.kt (si existe)
│   │   ├── CreditCardItem.kt            Badge compras activas + lastCutoffPayment
│   │   ├── FinancialSummaryCard.kt
│   │   ├── AnimatedListItem.kt
│   │   └── ChartCard.kt
│   │
│   ├── navigation/
│   │   ├── AppScaffold.kt               HorizontalPager (swipe entre 5 pestañas) + NavGraph
│   │   ├── NavGraph.kt                  Ruta "main" contiene el pager; detalles en stack
│   │   ├── BottomNavigationBar.kt       SwipeableBottomNavigationBar con targetPage
│   │   └── Screen.kt                   Rutas selladas
│   │
│   └── theme/
│       ├── Color.kt
│       ├── Theme.kt
│       └── Type.kt
│
├── di/
│   ├── DatabaseModule.kt               Provee DAOs + migración 5→6 registrada
│   ├── RepositoryModule.kt
│   └── WorkManagerModule.kt
│
├── work/
│   └── PaymentReminderWorker.kt        Worker diario de notificaciones
│
├── utils/
│   └── NotificationHelper.kt
│
├── FinanceApplication.kt
└── MainActivity.kt
```

---

## Arquitectura

```
UI (Compose + ViewModel)
        ↓ StateFlow / collectAsState
Domain (UseCases + Models)
        ↓ suspend / Flow
Data (Repository → DAOs → Room)
```

- **Reactividad**: Los ViewModels observan Flows de Room. Cualquier escritura en la BD actualiza automáticamente todas las pantallas suscritas.
- **Inyección**: Hilt provee DAOs, repositorio, use cases y WorkManager.
- **Navegación**: 5 pestañas principales via `HorizontalPager` (swipe habilitado). Pantallas de detalle apilan sobre el pager via `NavHost`.

---

## Modelo de ciclo de corte

```
Período anterior
    → Fecha de corte
    → Se guarda lastCutoffPayment (histórico, no cambia con compras nuevas)
    → lastCutoffDate = fecha del corte
    → pendingMinimumPayment = 0
    → Comienza nuevo período
        → Nuevas compras acumulan para el siguiente corte
        → Proyección calculada dinámicamente (no persistida)
        → Abonos a capital aplican al corte anterior (lastCutoffDate)
    → Pago mínimo registrado:
        → paidInstallments avanza según cuotas del corte pagado
        → lastCutoffPayment queda como referencia histórica
```

---

## Base de datos — esquema (v6)

### `credit_cards`
| Campo | Tipo | Descripción |
|---|---|---|
| `availableLimit` | REAL | Cupo libre actual |
| `pendingMinimumPayment` | REAL | Uso temporal para proyecciones |
| `lastCutoffDate` | INTEGER? | Timestamp del último corte pagado |
| `lastCutoffPayment` | REAL | Importe histórico del último corte |
| `minimumPaymentPercentage` | REAL | % banco (0.0–1.0) |

### `purchases`
| Campo | Tipo | Descripción |
|---|---|---|
| `paidInstallments` | INTEGER | Cuotas ya cobradas |

### `card_payments`
Historial de abonos: `amount`, `paymentType` (CAPITAL / MINIMUM_PAYMENT / MARK_AS_PAID), `cutoffDate`, `paymentDate`, `note`

---

## Backup (formato JSON v2.0)

Exporta el estado completo. Una restauración reconstruye exactamente el estado financiero sin recálculos manuales.

| Campo de backup | Campo en BD |
|---|---|
| `CreditCardBackup.lastCutoffPayment` | `credit_cards.lastCutoffPayment` |
| `CreditCardBackup.pendingMinimumPayment` | `credit_cards.pendingMinimumPayment` |
| `CreditCardBackup.lastCutoffDate` | `credit_cards.lastCutoffDate` |
| `CreditCardBackup.minimumPaymentPercentage` | `credit_cards.minimumPaymentPercentage` |
| `PurchaseBackup.paidInstallments` | `purchases.paidInstallments` |
| `IncomeBackup` | tabla `incomes` |
| `CardPaymentBackup` | tabla `card_payments` |

Compatibilidad: backups v1.0 se importan con `incomes` y `cardPayments` vacíos.
