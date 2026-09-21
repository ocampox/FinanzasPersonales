package com.finanzas.personales.data.local.dao

import androidx.room.*
import com.finanzas.personales.data.local.entities.SavingsAccountEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SavingsAccountDao {
    
    @Query("SELECT * FROM savings_accounts ORDER BY name ASC")
    fun getAllAccounts(): Flow<List<SavingsAccountEntity>>
    
    @Query("SELECT * FROM savings_accounts WHERE id = :accountId")
    suspend fun getAccountById(accountId: Long): SavingsAccountEntity?
    
    @Query("SELECT * FROM savings_accounts WHERE id = :accountId")
    fun getAccountByIdFlow(accountId: Long): Flow<SavingsAccountEntity?>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAccount(account: SavingsAccountEntity): Long
    
    @Update
    suspend fun updateAccount(account: SavingsAccountEntity)
    
    @Delete
    suspend fun deleteAccount(account: SavingsAccountEntity)
    
    @Query("UPDATE savings_accounts SET balance = :newBalance, updatedAt = :updatedAt WHERE id = :accountId")
    suspend fun updateBalance(accountId: Long, newBalance: Double, updatedAt: Long)
    
    @Query("SELECT COUNT(*) FROM savings_accounts")
    suspend fun getAccountCount(): Int
    
    @Query("SELECT SUM(balance) FROM savings_accounts")
    suspend fun getTotalBalance(): Double?
}
