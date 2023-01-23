package com.motionapps.sensorbox.permissions

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.result.ActivityResultLauncher
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment
import androidx.preference.PreferenceManager
import com.afollestad.materialdialogs.MaterialDialog
import com.motionapps.sensorbox.R


class PermissionSettingsDialog {

    companion object {

        fun showPermissionRational(
            permissionCallback: ActivityResultLauncher<String>,
            fragment: Fragment,
            messageText: Int,
            permission: String,
            icon: Int,
        ): MaterialDialog {
            return MaterialDialog(fragment.requireActivity()).show {
                title(R.string.activity_permissions_required)
                message(messageText)
                icon(icon)
                cancelable(true)
                cancelOnTouchOutside(true)
                cornerRadius(16f)

                positiveButton(R.string.show_permission) { dialog ->

                    dialog.dismiss()
                    val rational = fragment.shouldShowRequestPermissionRationale(permission)
                    val alreadyAsked = storeAskedPermission(fragment.requireContext(), permission)
                    if (rational || !alreadyAsked) {
                        permissionCallback.launch(permission)
                    } else {
                        showSettings(fragment.requireContext())
                    }

                }
                // show privacy policy in dialog
                negativeButton(R.string.about_privacy_policy) {
                    it.dismiss()
                    val browserIntent = Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse(fragment.getString(R.string.link_privacy_policy))
                    )
                    fragment.startActivity(browserIntent)
                }
            }
        }

        private fun storeAskedPermission(context: Context, permission: String): Boolean {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                return false
            }

            val preferenceManager = PreferenceManager.getDefaultSharedPreferences(context)
            val returnValue = preferenceManager.getBoolean(permission, false)
            preferenceManager.edit().apply {
                this.putBoolean(permission, true)
            }.apply()
            return returnValue
        }

        @RequiresApi(Build.VERSION_CODES.S)
        fun showPermissionRational(
            permissionCallback: ActivityResultLauncher<Array<String>>,
            fragment: Fragment,
            messageText: Int,
            permissions: Array<String>,
            icon: Int,
        ): MaterialDialog {
            return MaterialDialog(fragment.requireActivity()).show {
                title(R.string.activity_permissions_required)
                message(messageText)
                icon(icon)
                cancelable(true)
                cancelOnTouchOutside(true)
                cornerRadius(16f)

                positiveButton(R.string.show_permission) { dialog ->

                    dialog.dismiss()
                    val rational = permissions.any { fragment.shouldShowRequestPermissionRationale(it) }
                    val alreadyAsked = permissions.any { storeAskedPermission(fragment.requireContext(), it)}
                    if (rational || !alreadyAsked) {
                        permissionCallback.launch(permissions)
                    } else {
                        showSettings(fragment.requireContext())
                    }

                }
                // show privacy policy in dialog
                negativeButton(R.string.about_privacy_policy) {
                    it.dismiss()
                    val browserIntent = Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse(fragment.getString(R.string.link_privacy_policy))
                    )
                    fragment.startActivity(browserIntent)
                }
            }
        }

        fun showSettings(context: Context) {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
            val uri: Uri = Uri.fromParts("package", context.packageName, null)
            intent.data = uri
            context.startActivity(intent)
        }

    }
}