package com.motionapps.sensorbox.di

import android.content.Context
import android.media.MediaPlayer
import com.motionapps.countdowndialog.CountDownMain
import com.motionapps.sensorbox.R
import com.motionapps.sensorbox.fragments.advanced.extrahandlers.AlarmHandler
import com.motionapps.sensorbox.fragments.advanced.extrahandlers.NoteHandler
import com.motionapps.sensorbox.uiHandlers.SensorViewHandler
import com.motionapps.sensorbox.viewmodels.MainRepository
import com.motionapps.wearoslib.WearOsHandler
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityRetainedComponent
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.scopes.ActivityRetainedScoped
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.InternalCoroutinesApi


@ExperimentalCoroutinesApi
@InternalCoroutinesApi
@Module
@InstallIn(ActivityRetainedComponent::class)
object MainRepositoryModule {
    /**
     * Instance of CountDown manager - counts in dialog
     * Uses custom sound
     * @param context
     * @return
     */
    @ActivityRetainedScoped
    @Provides
    fun getCountDownDialog(@ApplicationContext context: Context): CountDownMain {
        val countDown = CountDownMain()
        countDown.mediaPlayerBeep = MediaPlayer.create(context, R.raw.beep)
        countDown.mediaPlayerStart = MediaPlayer.create(context, R.raw.start)
        return countDown
    }

    /**
     * Instance of WearOsHandler - communication with Wear Os
     * @return
     */
    @ActivityRetainedScoped
    @Provides
    fun getWearOsHandler(): WearOsHandler {
        return WearOsHandler()
    }

    /**
     * Manages MainActivity -> MainViewModel -> MainRepository
     * This repository provide data about sensors, notes, alarms, Wear Os - everything
     * Does not interfere with SensorService - separate library
     * @param context
     * @param countDownMain - capability to countDown
     * @param wearOsHandler - capability to communicate with Wear Os
     * @return MainRepository
     */
    @ActivityRetainedScoped
    @Provides
    fun getMainRepository(@ApplicationContext context: Context, countDownMain: CountDownMain, wearOsHandler: WearOsHandler): MainRepository {
        return MainRepository(SensorViewHandler(context), countDownMain, NoteHandler(), AlarmHandler(), wearOsHandler)
    }

}