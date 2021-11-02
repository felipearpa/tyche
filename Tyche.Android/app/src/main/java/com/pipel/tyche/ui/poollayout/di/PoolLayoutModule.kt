package com.pipel.tyche.ui.poollayout.di

import com.pipel.tyche.usecase.DefaultFindPoolsLayoutsUseCase
import com.pipel.tyche.usecase.FindPoolsLayoutsUseCase
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
interface PoolLayoutModule {

    @Binds
    @Singleton
    fun provideFindPoolsLayoutsUseCase(impl: DefaultFindPoolsLayoutsUseCase): FindPoolsLayoutsUseCase

}