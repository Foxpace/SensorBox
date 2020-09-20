package com.motionapps.sensorbox.fragments.displayers

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.NavArgs

/**
 * Interface for objects, that want to shoe sensor or other attributes of it
 */
interface Displayer {
    fun getView(context: Context, inflater: LayoutInflater, viewGroup: ViewGroup?, args: NavArgs): View?
    fun onDestroy()
}