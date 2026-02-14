package com.example.tester2.di

import android.content.Context
import com.example.tester2.data.recorder.AndroidAudioRecorder
import com.example.tester2.data.recorder.AudioRecorder
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AudioModule {

    @Provides
    @Singleton
    fun provideAudioRecorder(@ApplicationContext context: Context): AudioRecorder {
        return AndroidAudioRecorder(context)
    }
}
