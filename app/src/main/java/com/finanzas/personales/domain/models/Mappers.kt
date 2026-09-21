package com.finanzas.personales.domain.models

import com.finanzas.personales.data.local.entities.CreditCardEntity
import com.finanzas.personales.data.local.entities.ExpenseEntity
import com.finanzas.personales.data.local.entities.IncomeEntity
import com.finanzas.personales.data.local.entities.PurchaseEntity
import com.finanzas.personales.data.local.entities.SavingsAccountEntity
import kotlinx.datetime.Instant

/**
 * Extensión para convertir CreditCardEntity a CreditCard (modelo de dominio)
 */
fun CreditCardEntity.toDomain(): CreditCard {
    return CreditCard(
        id = id,
        name = name,
        totalLimit = totalLimit,
        availableLimit = availableLimit,
        cutoffDay = cutoffDay,
        paymentDay = paymentDay,
        color = color,
        pendingMinimumPayment = pendingMinimumPayment,
        lastCutoffDate = lastCutoffDate?.let { Instant.fromEpochMilliseconds(it) },
        lastCutoffPayment = lastCutoffPayment,
        minimumPaymentPercentage = minimumPaymentPercentage,
        createdAt = Instant.fromEpochMilliseconds(createdAt),
        updatedAt = Instant.fromEpochMilliseconds(updatedAt)
    )
}

/**
 * Extensión para convertir CreditCard a CreditCardEntity
 */
fun CreditCard.toEntity(): CreditCardEntity {
    return CreditCardEntity(
        id = id,
        name = name,
        totalLimit = totalLimit,
        availableLimit = availableLimit,
        cutoffDay = cutoffDay,
        paymentDay = paymentDay,
        color = color,
        pendingMinimumPayment = pendingMinimumPayment,
        lastCutoffDate = lastCutoffDate?.toEpochMilliseconds(),
        lastCutoffPayment = lastCutoffPayment,
        minimumPaymentPercentage = minimumPaymentPercentage,
        createdAt = createdAt.toEpochMilliseconds(),
        updatedAt = updatedAt.toEpochMilliseconds()
    )
}

/**
 * Extensión para convertir SavingsAccountEntity a SavingsAccount (modelo de dominio)
 */
fun SavingsAccountEntity.toDomain(): SavingsAccount {
    return SavingsAccount(
        id = id,
        name = name,
        balance = balance,
        color = color,
        createdAt = Instant.fromEpochMilliseconds(createdAt),
        updatedAt = Instant.fromEpochMilliseconds(updatedAt)
    )
}

/**
 * Extensión para convertir SavingsAccount a SavingsAccountEntity
 */
fun SavingsAccount.toEntity(): SavingsAccountEntity {
    return SavingsAccountEntity(
        id = id,
        name = name,
        balance = balance,
        color = color,
        createdAt = createdAt.toEpochMilliseconds(),
        updatedAt = updatedAt.toEpochMilliseconds()
    )
}

/**
 * Extensión para convertir PurchaseEntity a Purchase (modelo de dominio)
 */
fun PurchaseEntity.toDomain(): Purchase {
    return Purchase(
        id = id,
        cardId = cardId,
        description = description,
        totalAmount = totalAmount,
        installments = installments,
        installmentAmount = installmentAmount,
        paidInstallments = paidInstallments,
        purchaseDate = Instant.fromEpochMilliseconds(purchaseDate)
    )
}

/**
 * Extensión para convertir Purchase a PurchaseEntity
 */
fun Purchase.toEntity(createdAt: Long = System.currentTimeMillis()): PurchaseEntity {
    return PurchaseEntity(
        id = id,
        cardId = cardId,
        description = description,
        totalAmount = totalAmount,
        installments = installments,
        installmentAmount = installmentAmount,
        paidInstallments = paidInstallments,
        purchaseDate = purchaseDate.toEpochMilliseconds(),
        createdAt = createdAt
    )
}

/**
 * Extensión para convertir ExpenseEntity a Expense (modelo de dominio)
 */
fun ExpenseEntity.toDomain(): Expense {
    return Expense(
        id = id,
        accountId = accountId,
        description = description,
        amount = amount,
        category = category,
        expenseDate = Instant.fromEpochMilliseconds(expenseDate)
    )
}

/**
 * Extensión para convertir Expense a ExpenseEntity
 */
fun Expense.toEntity(createdAt: Long = System.currentTimeMillis()): ExpenseEntity {
    return ExpenseEntity(
        id = id,
        accountId = accountId,
        description = description,
        amount = amount,
        category = category,
        expenseDate = expenseDate.toEpochMilliseconds(),
        createdAt = createdAt
    )
}

/**
 * Extensión para convertir IncomeEntity a Income (modelo de dominio)
 */
fun IncomeEntity.toDomain(): Income {
    return Income(
        id = id,
        accountId = accountId,
        description = description,
        amount = amount,
        category = category,
        incomeDate = Instant.fromEpochMilliseconds(incomeDate)
    )
}

/**
 * Extensión para convertir Income a IncomeEntity
 */
fun Income.toEntity(createdAt: Long = System.currentTimeMillis()): IncomeEntity {
    return IncomeEntity(
        id = id,
        accountId = accountId,
        description = description,
        amount = amount,
        category = category,
        incomeDate = incomeDate.toEpochMilliseconds(),
        createdAt = createdAt
    )
}

/**
 * Extensión para convertir una lista de CreditCardEntity a lista de CreditCard
 */
fun List<CreditCardEntity>.toDomain(): List<CreditCard> {
    return map { it.toDomain() }
}

/**
 * Extensión para convertir una lista de SavingsAccountEntity a lista de SavingsAccount
 */
fun List<SavingsAccountEntity>.toSavingsAccountDomain(): List<SavingsAccount> {
    return map { it.toDomain() }
}

/**
 * Extensión para convertir una lista de PurchaseEntity a lista de Purchase
 */
fun List<PurchaseEntity>.toPurchaseDomain(): List<Purchase> {
    return map { it.toDomain() }
}

/**
 * Extensión para convertir una lista de ExpenseEntity a lista de Expense
 */
fun List<ExpenseEntity>.toExpenseDomain(): List<Expense> {
    return map { it.toDomain() }
}

/**
 * Extensión para convertir una lista de IncomeEntity a lista de Income
 */
fun List<IncomeEntity>.toIncomeDomain(): List<Income> {
    return map { it.toDomain() }
}

/**
 * Extensión para convertir CardPaymentEntity a CardPayment (modelo de dominio)
 */
fun com.finanzas.personales.data.local.entities.CardPaymentEntity.toDomain(): CardPayment {
    return CardPayment(
        id = id,
        cardId = cardId,
        amount = amount,
        paymentType = when (paymentType) {
            "CAPITAL" -> PaymentType.CAPITAL
            "MINIMUM_PAYMENT" -> PaymentType.MINIMUM_PAYMENT
            else -> PaymentType.MARK_AS_PAID
        },
        cutoffDate = cutoffDate?.let { Instant.fromEpochMilliseconds(it) },
        paymentDate = Instant.fromEpochMilliseconds(paymentDate),
        note = note
    )
}

/**
 * Extensión para convertir CardPayment a CardPaymentEntity
 */
fun CardPayment.toEntity(): com.finanzas.personales.data.local.entities.CardPaymentEntity {
    return com.finanzas.personales.data.local.entities.CardPaymentEntity(
        id = id,
        cardId = cardId,
        amount = amount,
        paymentType = paymentType.name,
        cutoffDate = cutoffDate?.toEpochMilliseconds(),
        paymentDate = paymentDate.toEpochMilliseconds(),
        note = note
    )
}

/**
 * Extensión para convertir una lista de CardPaymentEntity a lista de CardPayment
 */
fun List<com.finanzas.personales.data.local.entities.CardPaymentEntity>.toCardPaymentDomain(): List<CardPayment> {
    return map { it.toDomain() }
}
