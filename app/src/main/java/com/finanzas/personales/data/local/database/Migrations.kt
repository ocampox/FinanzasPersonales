package com.finanzas.personales.data.local.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object Migrations {
    /**
     * Migración de versión 1 a 2:
     * - Agrega pendingMinimumPayment a credit_cards
     * - Agrega lastCutoffDate a credit_cards
     * - Agrega paidInstallments a purchases
     */
    val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(database: SupportSQLiteDatabase) {
            // Agregar columnas a credit_cards
            database.execSQL("ALTER TABLE credit_cards ADD COLUMN pendingMinimumPayment REAL NOT NULL DEFAULT 0.0")
            database.execSQL("ALTER TABLE credit_cards ADD COLUMN lastCutoffDate INTEGER")
            
            // Agregar columna a purchases
            database.execSQL("ALTER TABLE purchases ADD COLUMN paidInstallments INTEGER NOT NULL DEFAULT 0")
        }
    }

    /**
     * Migración de versión 2 a 3:
     * - Agrega tabla incomes para ingresos a cuentas de ahorros
     */
    val MIGRATION_2_3 = object : Migration(2, 3) {
        override fun migrate(database: SupportSQLiteDatabase) {
            // Crear tabla incomes
            database.execSQL("""
                CREATE TABLE IF NOT EXISTS incomes (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    accountId INTEGER NOT NULL,
                    description TEXT NOT NULL,
                    amount REAL NOT NULL,
                    category TEXT NOT NULL,
                    incomeDate INTEGER NOT NULL,
                    createdAt INTEGER NOT NULL,
                    FOREIGN KEY(accountId) REFERENCES savings_accounts(id) ON DELETE CASCADE
                )
            """)
            
            // Crear índice para accountId
            database.execSQL("CREATE INDEX IF NOT EXISTS index_incomes_accountId ON incomes(accountId)")
        }
    }

    /**
     * Migración de versión 3 a 4:
     * - Agrega minimumPaymentPercentage a credit_cards para permitir configurar
     *   el porcentaje del banco usado en el cálculo del pago mínimo.
     */
    val MIGRATION_3_4 = object : Migration(3, 4) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL(
                "ALTER TABLE credit_cards ADD COLUMN minimumPaymentPercentage REAL NOT NULL DEFAULT 0.0"
            )
        }
    }

    /**
     * Migración de versión 4 a 5:
     * - Agrega tabla card_payments para registrar el historial de abonos a tarjetas.
     */
    val MIGRATION_4_5 = object : Migration(4, 5) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL("""
                CREATE TABLE IF NOT EXISTS card_payments (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    cardId INTEGER NOT NULL,
                    amount REAL NOT NULL,
                    paymentType TEXT NOT NULL,
                    cutoffDate INTEGER,
                    paymentDate INTEGER NOT NULL,
                    note TEXT,
                    FOREIGN KEY(cardId) REFERENCES credit_cards(id) ON DELETE CASCADE
                )
            """)
            database.execSQL(
                "CREATE INDEX IF NOT EXISTS index_card_payments_cardId ON card_payments(cardId)"
            )
        }
    }

    /**
     * Migración de versión 5 a 6:
     * - Agrega lastCutoffPayment a credit_cards: el valor histórico del pago mínimo
     *   del corte anterior, solo informativo, no se acumula entre cortes.
     */
    val MIGRATION_5_6 = object : Migration(5, 6) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL(
                "ALTER TABLE credit_cards ADD COLUMN lastCutoffPayment REAL NOT NULL DEFAULT 0.0"
            )
        }
    }
}

