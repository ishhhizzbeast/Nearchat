package com.example.nearchat.di

import android.content.Context
import com.example.nearchat.data.chat.AndroidBluetoothController
import com.example.nearchat.domain.chat.BluetoothController
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object BluetoothModule {

    @Singleton
    @Provides
    fun provideBlutoothController(@ApplicationContext context: Context) : BluetoothController{
        return AndroidBluetoothController(context)
    }
}