package com.finanzas.personales.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.finanzas.personales.data.local.dao.CardPaymentDao
import com.finanzas.personales.data.local.dao.CreditCardDao
import com.finanzas.personales.data.local.dao.ExpenseDao
import com.finanzas.personales.data.local.dao.IncomeDao
import com.finanzas.personales.data.local.dao.PurchaseDao
import com.finanzas.personales.data.local.dao.SavingsAccountDao
import com.finanzas.personales.data.local.entities.CardPaymentEntity
import com.finanzas.personales.data.local.entities.CreditCardEntity
import com.finanzas.personales.data.local.entities.ExpenseEntity
import com.finanzas.personales.data.local.entities.IncomeEntity
import com.finanzas.personales.data.local.entities.PurchaseEntity
import com.finanzas.personales.data.local.entities.SavingsAccountEntity

@Database(
    entities = [
        CreditCardEntity::class,
        SavingsAccountEntity::class,
        PurchaseEntity::class,
        ExpenseEntity::class,
        IncomeEntity::class,
        CardPaymentEntity::class
    ],
    version = 6,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class FinanceDatabase : RoomDatabase() {
    
    abstract fun creditCardDao(): CreditCardDao
    abstract fun savingsAccountDao(): SavingsAccountDao
    abstract fun purchaseDao(): PurchaseDao
    abstract fun expenseDao(): ExpenseDao
    abstract fun incomeDao(): IncomeDao
    abstract fun cardPaymentDao(): CardPaymentDao
    
    companion object {
        const val DATABASE_NAME = "finance_database"
    }
}
