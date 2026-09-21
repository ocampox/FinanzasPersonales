package com.finanzas.personales.domain.models

/**
 * Tipo de pago que se puede realizar a una tarjeta de crédito
 */
enum class PaymentType {
    /** Abono registrado por el usuario. */
    CAPITAL,

    /**
     * Tipo histórico conservado para poder mostrar backups anteriores.
     */
    MINIMUM_PAYMENT,

    /**
     * Tipo histórico conservado para poder mostrar backups anteriores.
     */
    MARK_AS_PAID
}

