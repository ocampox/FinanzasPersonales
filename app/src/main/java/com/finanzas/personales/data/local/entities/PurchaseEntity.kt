package com.finanzas.personales.data.local.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "purchases",
    foreignKeys = [
        ForeignKey(
            entity = CreditCardEntity::class,
            parentColumns = ["id"],
            childColumns = ["cardId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["cardId"])]
)
data class PurchaseEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val cardId: Long,
    val description: String,
    val totalAmount: Double,
    val installments: Int, // Número total de cuotas
    val installmentAmount: Double, // Valor de cada cuota
    val paidInstallments: Int = 0, // Número de cuotas pagadas
    val purchaseDate: Long,
    val createdAt: Long
)
