package com.finanzas.personales.data.local.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.finanzas.personales.domain.models.IncomeCategory

@Entity(
    tableName = "incomes",
    foreignKeys = [
        ForeignKey(
            entity = SavingsAccountEntity::class,
            parentColumns = ["id"],
            childColumns = ["accountId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["accountId"])]
)
data class IncomeEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val accountId: Long,
    val description: String,
    val amount: Double,
    val category: IncomeCategory,
    val incomeDate: Long,
    val createdAt: Long
)