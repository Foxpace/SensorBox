package com.motionapps.sensorbox.uiHandlers

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment
import androidx.preference.PreferenceManager
import com.afollestad.materialdialogs.MaterialDialog
import com.motionapps.sensorbox.R


class PermissionHandler {

    companion object{

        @RequiresApi(Build.VERSION_CODES.M)
        fun showPermissionSettings(
            fragment: Fragment,
            text: Int,
            toastText: Int,
            preferenceSetting: String?, // is null, if the permission was asked for in introduction of the app
            permission: Array<String>,
            requestCode: Int,
            icon: Int,
            rational: Boolean
        ): MaterialDialog{

            val dialog = MaterialDialog(fragment.requireActivity())
            dialog.show {
                title(R.string.activity_permissions_required)
                message(text)
                icon(icon)
                cancelable(true)
                cancelOnTouchOutside(false)
                cornerRadius(16f)

                positiveButton(R.string.show_permission) { dialog ->
                    dialog.dismiss()
                    if(rational){ // ask for permission if permission was not asked for / was denied
                        preferenceSetting?.let { // for the first time, if the gps was not asked for - foreground and background are split into 2 options
                            val sharedPreferences: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(fragment.requireContext())
                            val editor: SharedPreferences.Editor = sharedPreferences.edit()
                            editor.putBoolean(preferenceSetting, false)
                            editor.apply()
                        }
                        fragment.requestPermissions(permission, requestCode) //TODO replace for new module
                    }else{ // show settings, if do not asked was clicked
                        Toast.makeText(fragment.requireContext(), toastText, Toast.LENGTH_LONG).show()
                        showSettings(fragment.requireContext())
                    }
                }
                // show privacy policy in dialog
                negativeButton(R.string.about_privacy_policy) {
                    it.dismiss()
                    val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(fragment.getString(R.string.link_privacy_policy)))
                    fragment.startActivity(browserIntent)
                }
            }
            return dialog
        }

        fun showSettings(context: Context){
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
            val uri: Uri = Uri.fromParts("package", context.packageName, null)
            intent.data = uri
            context.startActivity(intent)
        }

    }
}