package com.motionapps.sensorbox.di

import android.content.Context
import com.motionapps.sensorbox.fragments.displayers.GPSDisplayer
import com.motionapps.sensorservices.handlers.GPSHandler
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.FragmentComponent
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.scopes.FragmentScoped
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.InternalCoroutinesApi

@ExperimentalCoroutinesApi
@Module
@InstallIn(FragmentComponent::class)
object GpsModule {
    /**
     * Provides basic GPSHandler - can register GPS updates and specify parameters, listeners, ...
     * @return
     */
    @InternalCoroutinesApi
    @FragmentScoped
    @Provides
    fun provideGPSHandler(): GPSHandler{
        return GPSHandler()
    }

    /**
     * Uses GPSHandler to get info about location and shows it in specific tabs
     * @param context
     * @param gpsHandler - uses GPSHandler to manage access to location
     * @return
     */
    @InternalCoroutinesApi
    @FragmentScoped
    @Provides
    fun providesGPSDisplayer(@ApplicationContext context: Context, gpsHandler: GPSHandler): GPSDisplayer{
        return GPSDisplayer(context, gpsHandler)
    }

}