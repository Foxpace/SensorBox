package com.motionapps.sensorbox.di

import com.motionapps.countdowndialog.CountDownMain
import com.motionapps.sensorbox.uiHandlers.GraphUpdater
import com.motionapps.sensorbox.viewmodels.MeasurementRepository
import com.motionapps.wearoslib.WearOsHandler
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityRetainedComponent
import dagger.hilt.android.scopes.ActivityRetainedScoped


@Module
@InstallIn(ActivityRetainedComponent::class)
object MeasurementRepositoryModule {

    /**
     * Repository for MeasurementActivity
     * Contains countdown lib, graphUpdater - updates chart with registered sensor, and WearOs handler
     *
     * @param countDownMain - created in MainRepository - reusable instances
     * @param graphUpdater - created in MainRepository - reusable instances
     * @param wearOsHandler - created in MainRepository - reusable instances
     * @return
     */
    @ActivityRetainedScoped
    @Provides
    fun getMeasurementRepository(countDownMain: CountDownMain, graphUpdater: GraphUpdater, wearOsHandler: WearOsHandler): MeasurementRepository {
        return MeasurementRepository(countDownMain, graphUpdater, wearOsHandler)

    }
}