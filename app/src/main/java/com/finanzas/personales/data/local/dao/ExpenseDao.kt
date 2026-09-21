package com.finanzas.personales.data.local.dao

import androidx.room.*
import com.finanzas.personales.data.local.entities.ExpenseCategory
import com.finanzas.personales.data.local.entities.ExpenseEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ExpenseDao {
    
    @Query("SELECT * FROM expenses WHERE accountId = :accountId ORDER BY expenseDate DESC")
    fun getExpensesByAccount(accountId: Long): Flow<List<ExpenseEntity>>
    
    @Query("SELECT * FROM expenses ORDER BY expenseDate DESC")
    fun getAllExpenses(): Flow<List<ExpenseEntity>>
    
    @Query("SELECT * FROM expenses WHERE id = :expenseId")
    suspend fun getExpenseById(expenseId: Long): ExpenseEntity?
    
    @Query("SELECT * FROM expenses WHERE category = :category ORDER BY expenseDate DESC")
    fun getExpensesByCategory(category: ExpenseCategory): Flow<List<ExpenseEntity>>
    
    @Query("""
        SELECT * FROM expenses 
        WHERE accountId = :accountId 
        AND category = :category 
        ORDER BY expenseDate DESC
    """)
    fun getExpensesByAccountAndCategory(
        accountId: Long, 
        category: ExpenseCategory
    ): Flow<List<ExpenseEntity>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpense(expense: ExpenseEntity): Long
    
    @Update
    suspend fun updateExpense(expense: ExpenseEntity)
    
    @Delete
    suspend fun deleteExpense(expense: ExpenseEntity)
    
    @Query("""
        SELECT * FROM expenses 
        WHERE accountId = :accountId 
        AND expenseDate BETWEEN :startDate AND :endDate
        ORDER BY expenseDate DESC
    """)
    fun getExpensesByAccountAndDateRange(
        accountId: Long, 
        startDate: Long, 
        endDate: Long
    ): Flow<List<ExpenseEntity>>
    
    @Query("""
        SELECT * FROM expenses 
        WHERE expenseDate BETWEEN :startDate AND :endDate
        ORDER BY expenseDate DESC
    """)
    fun getExpensesByDateRange(startDate: Long, endDate: Long): Flow<List<ExpenseEntity>>
    
    @Query("SELECT COUNT(*) FROM expenses WHERE accountId = :accountId")
    suspend fun getExpenseCountByAccount(accountId: Long): Int
    
    @Query("SELECT SUM(amount) FROM expenses WHERE accountId = :accountId")
    suspend fun getTotalAmountByAccount(accountId: Long): Double?
    
    @Query("SELECT SUM(amount) FROM expenses WHERE category = :category")
    suspend fun getTotalAmountByCategory(category: ExpenseCategory): Double?
}
