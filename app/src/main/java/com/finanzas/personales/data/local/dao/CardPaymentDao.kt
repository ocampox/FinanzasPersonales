package com.finanzas.personales.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.finanzas.personales.data.local.entities.CardPaymentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CardPaymentDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPayment(payment: CardPaymentEntity): Long

    @Query("SELECT * FROM card_payments WHERE cardId = :cardId ORDER BY paymentDate DESC")
    fun getPaymentsByCard(cardId: Long): Flow<List<CardPaymentEntity>>

    @Query("SELECT * FROM card_payments ORDER BY paymentDate DESC")
    fun getAllPayments(): Flow<List<CardPaymentEntity>>

    @Query("DELETE FROM card_payments WHERE cardId = :cardId")
    suspend fun deletePaymentsByCard(cardId: Long)
}
