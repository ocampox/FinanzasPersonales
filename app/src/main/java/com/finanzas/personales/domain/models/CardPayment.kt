package com.finanzas.personales.domain.models

import kotlinx.datetime.Instant

/**
 * Modelo de dominio que representa un pago registrado a una tarjeta de crédito.
 *
 * Se crea un registro por cada abono: capital, pago mínimo o "marcar como pagado".
 * Para MARK_AS_PAID el amount es 0.0 (no movió dinero real en la app).
 */
data class CardPayment(
    val id: Long = 0,
    val cardId: Long,
    val amount: Double,
    val paymentType: PaymentType,
    /** Fecha del corte al que corresponde (solo para MINIMUM_PAYMENT y MARK_AS_PAID) */
    val cutoffDate: Instant?,
    val paymentDate: Instant,
    val note: String? = null
)
