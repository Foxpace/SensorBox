package com.motionapps.sensorbox.uiHandlers

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.afollestad.materialdialogs.MaterialDialog
import com.motionapps.sensorbox.R


class PermissionHandler {

    companion object{
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
                    if(rational){
                        fragment.requestPermissions(permission, requestCode)
                        dialog.dismiss()
                    }else{
                        dialog.dismiss()
                        Toast.makeText(fragment.requireContext(), text, Toast.LENGTH_LONG).show()
                        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                        val uri: Uri = Uri.fromParts("package", fragment.requireActivity().packageName, null)
                        intent.data = uri
                        fragment.startActivity(intent)
                    }
                }

                negativeButton(R.string.cancel) {
                    dialog.dismiss()
                }
            }
            return dialog
        }
    }


}