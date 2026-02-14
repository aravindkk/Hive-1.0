package com.example.tester2.di

import com.example.tester2.data.repository.VoiceRepository
import com.example.tester2.data.repository.VoiceRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class VoiceModule {

    @Binds
    @Singleton
    abstract fun bindVoiceRepository(
        voiceRepositoryImpl: VoiceRepositoryImpl
    ): VoiceRepository
}
