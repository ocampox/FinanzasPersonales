package com.finanzas.personales.data.local.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Entidad Room que registra cada abono realizado a una tarjeta de crédito.
 *
 * Se crea un registro cada vez que el usuario registra un pago, ya sea
 * abono a capital, pago mínimo o marcado de pago ya realizado.
 * Esto permite tener un historial auditable de todos los movimientos.
 */
@Entity(
    tableName = "card_payments",
    foreignKeys = [
        ForeignKey(
            entity = CreditCardEntity::class,
            parentColumns = ["id"],
            childColumns = ["cardId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("cardId")]
)
data class CardPaymentEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val cardId: Long,
    /** Monto del abono. 0.0 para MARK_AS_PAID (no movió dinero). */
    val amount: Double,
    /** Tipo: "CAPITAL", "MINIMUM_PAYMENT" o "MARK_AS_PAID" */
    val paymentType: String,
    /** Fecha del corte al que corresponde este pago (timestamp ms). Null para abonos a capital. */
    val cutoffDate: Long?,
    /** Timestamp del momento en que se registró el pago */
    val paymentDate: Long,
    /** Nota opcional (ej: "Pago ya realizado - normalización") */
    val note: String? = null
)
