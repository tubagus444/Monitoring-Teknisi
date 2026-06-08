package com.skynet.monitoring.di

import com.skynet.monitoring.data.repository.AuthRepository
import com.skynet.monitoring.data.repository.AuthRepositoryImpl
import com.skynet.monitoring.data.repository.LocationRepository
import com.skynet.monitoring.data.repository.LocationRepositoryImpl
import com.skynet.monitoring.data.repository.NotificationRepository
import com.skynet.monitoring.data.repository.NotificationRepositoryImpl
import com.skynet.monitoring.data.repository.TaskRepository
import com.skynet.monitoring.data.repository.TaskRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindAuthRepository(impl: AuthRepositoryImpl): AuthRepository

    @Binds
    @Singleton
    abstract fun bindTaskRepository(impl: TaskRepositoryImpl): TaskRepository

    @Binds
    @Singleton
    abstract fun bindLocationRepository(impl: LocationRepositoryImpl): LocationRepository

    @Binds
    @Singleton
    abstract fun bindNotificationRepository(impl: NotificationRepositoryImpl): NotificationRepository
}
