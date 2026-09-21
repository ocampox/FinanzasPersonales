package com.finanzas.personales.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "savings_accounts")
data class SavingsAccountEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val balance: Double,
    val color: String,
    val createdAt: Long,
    val updatedAt: Long
)
