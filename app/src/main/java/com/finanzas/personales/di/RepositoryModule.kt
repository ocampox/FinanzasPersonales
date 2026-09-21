package com.finanzas.personales.di

import com.finanzas.personales.data.repository.FinanceRepository
import com.finanzas.personales.data.repository.FinanceRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module for providing repository dependencies
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    
    /**
     * Proporciona la implementación del FinanceRepository
     */
    @Binds
    @Singleton
    abstract fun bindFinanceRepository(
        financeRepositoryImpl: FinanceRepositoryImpl
    ): FinanceRepository
}
