package com.finanzas.personales.data.local.database

import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Provider

class DatabaseCallback(
    private val database: Provider<FinanceDatabase>
) : RoomDatabase.Callback() {

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate(db: SupportSQLiteDatabase) {
        super.onCreate(db)
        // Aquí se pueden agregar datos iniciales si es necesario
        applicationScope.launch {
            // Ejemplo: populateDatabase()
        }
    }

    override fun onOpen(db: SupportSQLiteDatabase) {
        super.onOpen(db)
        // Operaciones que se ejecutan cada vez que se abre la base de datos
    }
}
