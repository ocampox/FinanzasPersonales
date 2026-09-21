package com.finanzas.personales.data.local.dao

import androidx.room.*
import com.finanzas.personales.data.local.entities.PurchaseEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PurchaseDao {
    
    @Query("SELECT * FROM purchases WHERE cardId = :cardId ORDER BY purchaseDate DESC")
    fun getPurchasesByCard(cardId: Long): Flow<List<PurchaseEntity>>
    
    @Query("SELECT * FROM purchases ORDER BY purchaseDate DESC")
    fun getAllPurchases(): Flow<List<PurchaseEntity>>
    
    @Query("SELECT * FROM purchases WHERE id = :purchaseId")
    suspend fun getPurchaseById(purchaseId: Long): PurchaseEntity?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPurchase(purchase: PurchaseEntity): Long
    
    @Update
    suspend fun updatePurchase(purchase: PurchaseEntity)
    
    @Delete
    suspend fun deletePurchase(purchase: PurchaseEntity)
    
    @Query("""
        SELECT * FROM purchases 
        WHERE cardId = :cardId 
        AND purchaseDate <= :cutoffDate
        ORDER BY purchaseDate DESC
    """)
    suspend fun getPurchasesForCutoff(cardId: Long, cutoffDate: Long): List<PurchaseEntity>
    
    @Query("""
        SELECT * FROM purchases 
        WHERE cardId = :cardId 
        AND purchaseDate BETWEEN :startDate AND :endDate
        ORDER BY purchaseDate DESC
    """)
    fun getPurchasesByCardAndDateRange(
        cardId: Long, 
        startDate: Long, 
        endDate: Long
    ): Flow<List<PurchaseEntity>>
    
    @Query("SELECT COUNT(*) FROM purchases WHERE cardId = :cardId")
    suspend fun getPurchaseCountByCard(cardId: Long): Int
    
    @Query("SELECT SUM(totalAmount) FROM purchases WHERE cardId = :cardId")
    suspend fun getTotalAmountByCard(cardId: Long): Double?
    
    @Query("UPDATE purchases SET paidInstallments = :paidInstallments WHERE id = :purchaseId")
    suspend fun updatePaidInstallments(purchaseId: Long, paidInstallments: Int)
    
    /**
     * Transacción para actualizar múltiples compras en una sola operación
     */
    @Transaction
    suspend fun updateMultiplePaidInstallments(updates: Map<Long, Int>) {
        updates.forEach { (purchaseId, paidInstallments) ->
            updatePaidInstallments(purchaseId, paidInstallments)
        }
    }
}
