package com.finanzas.personales.data.local.dao

import androidx.room.*
import com.finanzas.personales.data.local.entities.IncomeEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface IncomeDao {
    
    @Query("SELECT * FROM incomes WHERE accountId = :accountId ORDER BY incomeDate DESC")
    fun getIncomesByAccount(accountId: Long): Flow<List<IncomeEntity>>
    
    @Query("SELECT * FROM incomes ORDER BY incomeDate DESC")
    fun getAllIncomes(): Flow<List<IncomeEntity>>
    
    @Query("SELECT * FROM incomes WHERE id = :incomeId")
    suspend fun getIncomeById(incomeId: Long): IncomeEntity?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertIncome(income: IncomeEntity): Long
    
    @Update
    suspend fun updateIncome(income: IncomeEntity)
    
    @Delete
    suspend fun deleteIncome(income: IncomeEntity)
    
    @Query("""
        SELECT * FROM incomes 
        WHERE accountId = :accountId 
        AND incomeDate BETWEEN :startDate AND :endDate
        ORDER BY incomeDate DESC
    """)
    fun getIncomesByAccountAndDateRange(
        accountId: Long, 
        startDate: Long, 
        endDate: Long
    ): Flow<List<IncomeEntity>>
    
    @Query("SELECT COUNT(*) FROM incomes WHERE accountId = :accountId")
    suspend fun getIncomeCountByAccount(accountId: Long): Int
    
    @Query("SELECT SUM(amount) FROM incomes WHERE accountId = :accountId")
    suspend fun getTotalIncomeByAccount(accountId: Long): Double?
}