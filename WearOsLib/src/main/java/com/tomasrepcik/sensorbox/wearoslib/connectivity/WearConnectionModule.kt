package com.tomasrepcik.sensorbox.wearoslib.connectivity

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class WearConnectionModule {
    @Binds
    @Singleton
    abstract fun bindWearConnectionRepository(repository: GooglePlayWearConnectionRepository): WearConnectionRepository
}
