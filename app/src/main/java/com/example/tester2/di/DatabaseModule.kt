package com.example.tester2.di

import android.content.Context
import androidx.room.Room
import com.example.tester2.data.local.HiveDatabase
import com.example.tester2.data.local.PendingVoiceDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): HiveDatabase =
        Room.databaseBuilder(context, HiveDatabase::class.java, "hive_db")
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    fun providePendingVoiceDao(db: HiveDatabase): PendingVoiceDao = db.pendingVoiceDao()
}
