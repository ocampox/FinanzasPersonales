package com.finanzas.personales.di

import android.content.Context
import androidx.room.Room
import com.finanzas.personales.data.local.dao.CardPaymentDao
import com.finanzas.personales.data.local.dao.CreditCardDao
import com.finanzas.personales.data.local.dao.ExpenseDao
import com.finanzas.personales.data.local.dao.IncomeDao
import com.finanzas.personales.data.local.dao.PurchaseDao
import com.finanzas.personales.data.local.dao.SavingsAccountDao
import com.finanzas.personales.data.local.database.DatabaseCallback
import com.finanzas.personales.data.local.database.FinanceDatabase
import com.finanzas.personales.data.local.database.Migrations
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Provider
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideFinanceDatabase(
        @ApplicationContext context: Context,
        callback: DatabaseCallback
    ): FinanceDatabase {
        // Asegurar que la base de datos se guarde en el directorio de datos de la app (persistente)
        // y no en caché, para que persista cuando se limpia el caché
        val dbPath = context.getDatabasePath(FinanceDatabase.DATABASE_NAME)
        dbPath.parentFile?.mkdirs() // Crear el directorio si no existe
        
        return Room.databaseBuilder(
            context,
            FinanceDatabase::class.java,
            FinanceDatabase.DATABASE_NAME
        )
            .addCallback(callback)
            .addMigrations(Migrations.MIGRATION_1_2, Migrations.MIGRATION_2_3, Migrations.MIGRATION_3_4, Migrations.MIGRATION_4_5, Migrations.MIGRATION_5_6)
            .fallbackToDestructiveMigration() // Para desarrollo inicial
            .build()
    }

    @Provides
    @Singleton
    fun provideDatabaseCallback(
        database: Provider<FinanceDatabase>
    ): DatabaseCallback {
        return DatabaseCallback(database)
    }

    @Provides
    @Singleton
    fun provideCreditCardDao(database: FinanceDatabase): CreditCardDao {
        return database.creditCardDao()
    }

    @Provides
    @Singleton
    fun provideSavingsAccountDao(database: FinanceDatabase): SavingsAccountDao {
        return database.savingsAccountDao()
    }

    @Provides
    @Singleton
    fun providePurchaseDao(database: FinanceDatabase): PurchaseDao {
        return database.purchaseDao()
    }

    @Provides
    @Singleton
    fun provideExpenseDao(database: FinanceDatabase): ExpenseDao {
        return database.expenseDao()
    }

    @Provides
    @Singleton
    fun provideIncomeDao(database: FinanceDatabase): IncomeDao {
        return database.incomeDao()
    }

    @Provides
    @Singleton
    fun provideCardPaymentDao(database: FinanceDatabase): CardPaymentDao {
        return database.cardPaymentDao()
    }
}
