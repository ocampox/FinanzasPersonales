package com.finanzas.personales.domain.models

/**
 * Sealed class para representar el resultado de operaciones que pueden fallar
 */
sealed class Result<out T> {
    data class Success<T>(val data: T) : Result<T>()
    data class Error(val error: FinanceError) : Result<Nothing>()
}

/**
 * Función de extensión para ejecutar código solo si el resultado es exitoso
 */
inline fun <T> Result<T>.onSuccess(action: (T) -> Unit): Result<T> {
    if (this is Result.Success) {
        action(data)
    }
    return this
}

/**
 * Función de extensión para ejecutar código solo si el resultado es error
 */
inline fun <T> Result<T>.onError(action: (FinanceError) -> Unit): Result<T> {
    if (this is Result.Error) {
        action(error)
    }
    return this
}

/**
 * Función de extensión para obtener el dato o null
 */
fun <T> Result<T>.getOrNull(): T? {
    return when (this) {
        is Result.Success -> data
        is Result.Error -> null
    }
}
