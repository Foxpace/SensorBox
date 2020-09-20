package com.motionapps.sensorbox.intro

import android.Manifest
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.preference.PreferenceManager
import com.github.appintro.AppIntro
import com.github.appintro.AppIntroFragment
import com.github.appintro.AppIntroPageTransformerType
import com.motionapps.sensorbox.activities.MainActivity
import com.motionapps.sensorbox.R
import com.motionapps.sensorbox.fragments.settings.SettingsFragment
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.InternalCoroutinesApi

@ExperimentalCoroutinesApi
@InternalCoroutinesApi
/**
 * For intro is used https://github.com/AppIntro/AppIntro
 * Starts only, when the user opens the app for the first time
 */
class IntroActivity: AppIntro() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        isWizardMode = true
        isSystemBackButtonLocked = true
        isIndicatorEnabled = true
        isColorTransitionsEnabled = true



        setTransformer(
            AppIntroPageTransformerType.Parallax(
            titleParallaxFactor = 1.0,
            imageParallaxFactor = -1.0,
            descriptionParallaxFactor = 2.0
        ))

        setIndicatorColor(
            selectedIndicatorColor = ContextCompat.getColor(this,
                R.color.colorWhite
            ),
            unselectedIndicatorColor = Color.LTGRAY
        )

        // welcome slide
        addSlide(
            AppIntroFragment.newInstance(
            title = getString(R.string.intro_welcome),
            description = getString(R.string.intro_welcome_description),
            imageDrawable = R.drawable.ic_launcher_white_square,
            titleColor = Color.WHITE,
            descriptionColor = Color.WHITE,
            backgroundColor = Color.DKGRAY,
            titleTypefaceFontRes = R.font.roboto_regular,
            descriptionTypefaceFontRes = R.font.roboto_regular
        ))

        // incognito slide
        addSlide(
            AppIntroFragment.newInstance(
                title = getString(R.string.intro_incognito),
                description = getString(R.string.intro_incognito_description),
                imageDrawable = R.drawable.ic_incognito,
                titleColor = Color.WHITE,
                descriptionColor = Color.WHITE,
                backgroundColor = ContextCompat.getColor(this,
                    R.color.colorBlack
                ),
                titleTypefaceFontRes = R.font.roboto_regular,
                descriptionTypefaceFontRes = R.font.roboto_regular
            )
        )

        // permissions
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            addSlide(
                AppIntroFragment.newInstance(
                    title = getString(R.string.intro_permission),
                    description = getString(R.string.intro_permission_description),
                    imageDrawable = R.drawable.ic_ok_big,
                    titleColor = Color.WHITE,
                    descriptionColor = Color.WHITE,
                    backgroundColor = ContextCompat.getColor(this,
                        R.color.colorDarkGreen
                    ),
                    titleTypefaceFontRes = R.font.roboto_regular,
                    descriptionTypefaceFontRes = R.font.roboto_regular
                )
            )

            // android Q requires Background GPS access and activity recognition
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                askForPermissions(
                    permissions = arrayOf(
                        Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        Manifest.permission.ACCESS_BACKGROUND_LOCATION,
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACTIVITY_RECOGNITION
                    ),
                    slideNumber = 3,
                    required = true
                )
            } else {
                askForPermissions(
                    permissions = arrayOf(
                        Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        Manifest.permission.ACCESS_FINE_LOCATION
                    ),
                    slideNumber = 3,
                    required = true
                )
            }

            // warning about battery drainage
            addSlide(PowerSaverExplanationFragment())

            // custom slide to add app to the whitelist - battery optimizations
            addSlide(PowerSaverFragment())
        }

        addSlide(PickFolderFragment.newInstance(ContextCompat.getColor(this, R.color.colorBlueGray)))

    }

    override fun onUserDeniedPermission(permissionName: String) {
        // User pressed "Deny" on the permission dialog
        Toast.makeText(this, getString(R.string.intro_deny), Toast.LENGTH_LONG).show()
    }
    override fun onUserDisabledPermission(permissionName: String) {
        // User pressed "Deny" + "Don't ask again" on the permission dialog
        Toast.makeText(this, getString(R.string.intro_deny_dont_ask), Toast.LENGTH_LONG).show()
    }

    /**
     * User finished all the slides successfully
     *
     * @param currentFragment
     */
    override fun onDonePressed(currentFragment: Fragment?) {
        super.onDonePressed(currentFragment)
        PreferenceManager.getDefaultSharedPreferences(this).edit().apply{
            putBoolean(SettingsFragment.APP_FIRST_TIME, true)
            apply()
        }
        finish()
        startActivity(Intent(this, MainActivity::class.java))

    }
}