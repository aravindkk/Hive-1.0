package com.example.tester2.di

import com.example.tester2.data.repository.StorageRepository
import com.example.tester2.data.repository.StorageRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class StorageModule {

    @Binds
    @Singleton
    abstract fun bindStorageRepository(
        storageRepositoryImpl: StorageRepositoryImpl
    ): StorageRepository
}
