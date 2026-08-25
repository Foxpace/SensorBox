package com.tomasrepcik.sensorbox.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.tomasrepcik.sensorbox.core.preferences.AppPreferencesDataStoreFactory
import com.tomasrepcik.sensorbox.core.preferences.AppPreferencesRepository
import com.tomasrepcik.sensorbox.core.preferences.DataStoreAppPreferencesRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object PreferencesModule {
    @Provides
    @Singleton
    fun providePreferencesDataStore(@ApplicationContext context: Context): DataStore<Preferences> =
        AppPreferencesDataStoreFactory.create(context)

    @Provides
    @Singleton
    fun providePreferencesRepository(dataStore: DataStore<Preferences>): AppPreferencesRepository =
        DataStoreAppPreferencesRepository(dataStore)
}
