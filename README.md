# Finanzas Personales — Android App

Aplicación Android personal para gestionar tarjetas de crédito, cuentas de ahorros, compras diferidas y gastos, con funcionamiento 100% offline.

---

## Características principales

### Tarjetas de crédito
- Registro de múltiples tarjetas con cupo total, disponible, día de corte y día de pago
- Compras diferidas a N cuotas con seguimiento cuota a cuota
- Modelo de **períodos de corte independientes**: el pago mínimo del corte anterior (`lastCutoffPayment`) se conserva como valor histórico fijo; la proyección del período actual se calcula dinámicamente sin acumular deuda de ciclos anteriores
- Abonos a capital (aplican al corte anterior) y pago mínimo (avanza el ciclo)
- Historial de pagos por tarjeta
- Porcentaje de pago mínimo configurable por tarjeta para aproximar el cálculo del banco
- Filtros en el historial de compras: todas, pendientes, pagadas, diferidas, diferidas pendientes

### Cuentas de ahorros
- Registro de ingresos y gastos con categorías
- Saldo actualizado en tiempo real

### Home
- Resumen financiero reactivo (disponible total, deuda total, pago mínimo próximo, disponible real)
- Próximos pagos con urgencia codificada por color
- Se actualiza automáticamente al modificar cualquier dato

### Reportes
- KPIs de deuda: cupo total, deuda actual, cupo libre, compromisos del próximo corte
- Balance del período (ingresos vs gastos)
- Barras de deuda por tarjeta con segmento usado/libre
- Proyección de cuotas por vencer en los próximos 6 meses
- Gastos por categoría con barras proporcionales
- Filtros por período: semana, mes, mes anterior, 3 meses, año

### Movimientos
- Lista unificada de compras y gastos con búsqueda, filtros por tipo, categoría y fecha

### Ajustes / Mantenimiento
- Backup completo (v2.0) y restauración — exporta e importa el estado financiero exacto
- Reparar cuotas: corrige `paidInstallments` avanzando cuotas cuyo mes ya venció
- Recalcular pago mínimo: corrige cuotas mal marcadas y recalcula la proyección del próximo corte
- Ajustar cupo disponible: corrección manual cuando el valor del banco difiere del calculado
- Notificaciones de recordatorio de pago (WorkManager)

---

## Stack tecnológico

| Capa | Tecnología |
|---|---|
| Lenguaje | Kotlin |
| UI | Jetpack Compose + Material Design 3 |
| Arquitectura | MVVM + Clean Architecture |
| Base de datos | Room (SQLite) v6 |
| Inyección de dependencias | Hilt |
| Navegación | Navigation Compose + HorizontalPager (swipe entre pestañas) |
| Async | Kotlin Coroutines + Flow |
| Gráficos | Vico Charts |
| Fechas | kotlinx-datetime 0.5.0 |
| Notificaciones | WorkManager |
| Serialización backup | Gson |

---

## Requisitos

- **Android Studio** Hedgehog 2023.1.1 o superior
- **JDK** 17
- **Min SDK** 26 (Android 8.0)
- **Target SDK** 34 (Android 14)

---

## Build

```bash
# Debug
./gradlew assembleDebug

# Release
./gradlew assembleRelease
```

---

## Esquema de la base de datos (Room v6)

| Tabla | Descripción |
|---|---|
| `credit_cards` | Tarjetas con cupo, ciclo de corte, `lastCutoffPayment`, `minimumPaymentPercentage` |
| `purchases` | Compras diferidas con `paidInstallments` |
| `savings_accounts` | Cuentas de ahorros |
| `expenses` | Gastos categorizados |
| `incomes` | Ingresos categorizados |
| `card_payments` | Historial de abonos a tarjetas |

Migraciones: 1→2, 2→3, 3→4, 4→5, 5→6

---

## Backup

El backup (formato JSON v2.0) incluye el estado completo:
- Tarjetas: cupo, `availableLimit`, `lastCutoffDate`, `lastCutoffPayment`, `pendingMinimumPayment`, `minimumPaymentPercentage`
- Compras: incluyendo `paidInstallments`
- Cuentas, gastos, ingresos e historial de pagos

Una restauración desde backup v2.0 reconstruye el estado financiero exacto sin necesidad de recálculos manuales.

---

## Licencia

Proyecto de uso personal y privado.
