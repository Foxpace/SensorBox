package com.motionapps.sensorbox.uiHandlers

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment
import com.afollestad.materialdialogs.MaterialDialog
import com.motionapps.sensorbox.R
import kotlinx.coroutines.internal.artificialFrame


class PermissionHandler {

    companion object{

        @RequiresApi(Build.VERSION_CODES.M)
        fun showPermissionSettings(
            fragment: Fragment,
            text: Int,
            permission: Array<String>,
            requestCode: Int,
            rational: Boolean
        ): MaterialDialog{
            val dialog = MaterialDialog(fragment.requireActivity())
            dialog.show {
                title(R.string.activity_permissions_required)
                message(text)
                cancelable(false)
                cancelOnTouchOutside(false)
                cornerRadius(16f)

                positiveButton(R.string.show_permission) { dialog ->
                    dialog.dismiss()
                    if(rational){
                        fragment.requestPermissions(permission, requestCode)
                    }else{
                        Toast.makeText(fragment.requireContext(), text, Toast.LENGTH_LONG).show()
                        showSettings(fragment.requireContext())
                    }
                }

                negativeButton(R.string.cancel) {
                    dialog.dismiss()
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