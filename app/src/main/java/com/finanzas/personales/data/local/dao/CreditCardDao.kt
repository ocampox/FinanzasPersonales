package com.finanzas.personales.data.local.dao

import androidx.room.*
import com.finanzas.personales.data.local.entities.CreditCardEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CreditCardDao {
    
    @Query("SELECT * FROM credit_cards ORDER BY name ASC")
    fun getAllCards(): Flow<List<CreditCardEntity>>
    
    @Query("SELECT * FROM credit_cards WHERE id = :cardId")
    suspend fun getCardById(cardId: Long): CreditCardEntity?
    
    @Query("SELECT * FROM credit_cards WHERE id = :cardId")
    fun getCardByIdFlow(cardId: Long): Flow<CreditCardEntity?>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCard(card: CreditCardEntity): Long
    
    @Update
    suspend fun updateCard(card: CreditCardEntity)
    
    @Delete
    suspend fun deleteCard(card: CreditCardEntity)
    
    @Query("UPDATE credit_cards SET availableLimit = :newLimit, updatedAt = :updatedAt WHERE id = :cardId")
    suspend fun updateAvailableLimit(cardId: Long, newLimit: Double, updatedAt: Long)
    
    @Query("UPDATE credit_cards SET pendingMinimumPayment = :pendingPayment, updatedAt = :updatedAt WHERE id = :cardId")
    suspend fun updatePendingMinimumPayment(cardId: Long, pendingPayment: Double, updatedAt: Long)

    @Query("UPDATE credit_cards SET lastCutoffPayment = :lastCutoffPayment, updatedAt = :updatedAt WHERE id = :cardId")
    suspend fun updateLastCutoffPayment(cardId: Long, lastCutoffPayment: Double, updatedAt: Long)
    
    @Query("UPDATE credit_cards SET lastCutoffDate = :cutoffDate, updatedAt = :updatedAt WHERE id = :cardId")
    suspend fun updateLastCutoffDate(cardId: Long, cutoffDate: Long, updatedAt: Long)

    @Query("SELECT COUNT(*) FROM credit_cards")
    suspend fun getCardCount(): Int

    @Query("SELECT SUM(totalLimit - availableLimit) FROM credit_cards")
    suspend fun getTotalDebt(): Double?
    
    /**
     * Transacción para actualizar múltiples campos de la tarjeta de crédito
     */
    @Transaction
    suspend fun updateCardPaymentInfo(
        cardId: Long,
        newAvailableLimit: Double,
        newPendingMinimumPayment: Double,
        newLastCutoffDate: Long?,
        newLastCutoffPayment: Double? = null,
        updatedAt: Long
    ) {
        updateAvailableLimit(cardId, newAvailableLimit, updatedAt)
        updatePendingMinimumPayment(cardId, newPendingMinimumPayment, updatedAt)
        if (newLastCutoffDate != null) {
            updateLastCutoffDate(cardId, newLastCutoffDate, updatedAt)
        }
        if (newLastCutoffPayment != null) {
            updateLastCutoffPayment(cardId, newLastCutoffPayment, updatedAt)
        }
    }
}
