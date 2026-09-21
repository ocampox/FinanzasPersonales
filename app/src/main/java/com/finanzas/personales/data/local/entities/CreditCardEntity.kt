package com.finanzas.personales.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "credit_cards")
data class CreditCardEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val totalLimit: Double,
    val availableLimit: Double,
    val cutoffDay: Int,
    val paymentDay: Int,
    val color: String,
    /**
     * Pago mínimo del corte anterior que aún no se ha pagado.
     * Se setea cuando el usuario registra un pago mínimo o marca como pagado,
     * y solo se usa para mostrar como referencia en la UI.
     * No se acumula de corte en corte.
     */
    val pendingMinimumPayment: Double = 0.0,
    /** Fecha del último corte procesado (timestamp ms). */
    val lastCutoffDate: Long? = null,
    /**
     * Valor del pago mínimo que generó el corte que acaba de cerrar.
     * Es solo histórico/informativo — el importe exacto que se debe pagar
     * antes de la fecha de pago del corte anterior.
     * No se recalcula con las compras nuevas del nuevo período.
     */
    val lastCutoffPayment: Double = 0.0,
    /** Porcentaje del saldo usado aplicado como pago mínimo (0.0–1.0). */
    val minimumPaymentPercentage: Double = 0.0,
    val createdAt: Long,
    val updatedAt: Long
)
