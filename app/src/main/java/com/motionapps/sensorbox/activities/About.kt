package com.motionapps.sensorbox.activities


import android.app.Dialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.Fragment
import com.motionapps.sensorbox.BuildConfig
import com.motionapps.sensorbox.R
import de.psdev.licensesdialog.LicensesDialog
import mehdi.sakout.aboutpage.AboutPage
import mehdi.sakout.aboutpage.Element

/**
 * Shows about page created by Android About Page: https://github.com/medyo/android-about-page
 * To show all the licenses used in project: https://github.com/PSDev/LicensesDialog
 */

class About : Fragment() {

    private var dialog: Dialog? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        // android version tab
        val versionElement = Element()
        versionElement.iconDrawable =
            R.drawable.ic_android
        versionElement.iconTint = R.color.colorWhite
        versionElement.title = getString(R.string.app_version).format(BuildConfig.VERSION_NAME)

        // license dialog show tab
        val licences = Element()
        licences.title = getString(R.string.licenses)
        licences.iconDrawable = R.drawable.ic_license
        licences.iconTint = R.color.colorWhite
        licences.setOnClickListener{
            dialog = LicensesDialog.Builder(requireContext()).setNotices(R.raw.notices).build().show()
        }

        // creation of Aboutpage with all the other elements
        val aboutPage = AboutPage(requireContext(), true)
            .isRTL(false)
            .setImage(R.drawable.ic_launcher_white_square)
            .setDescription(getString(R.string.about_app_description))
            .addItem(versionElement)
            .addItem(googlePlay())
            // .addItem(websiteElement())
            .addItem(emailElement())
            .addItem(githubElement())
            .addItem(licences)
            .addItem(getPolicyElement())
            .addItem(getTermsElement())

        aboutPage.setCustomFont(
            ResourcesCompat.getFont(
                requireContext(),
                R.font.roboto_family
            )
        )

        return aboutPage.create()
    }

    override fun onDestroy() {
        super.onDestroy()
        dialog?.let {
            if(it.isShowing){
                it.dismiss()
            }
        }
        dialog = null
    }

    /**
     * Tab element for website
     * @return About page Element
     */
//    private fun websiteElement(): Element{
//        var url = getString(R.string.creative_motion_web)
//        if (!url.startsWith("http://") && !url.startsWith("https://")) {
//            url = "http://$url"
//        }
//        val websiteElement = Element()
//        websiteElement.title = getString(R.string.about_site_title)
//        websiteElement.iconDrawable = R.drawable.ic_link
//        websiteElement.iconTint = R.color.colorWhite
//        websiteElement.value = url
//        val uri = Uri.parse(url)
//        val browserIntent = Intent(Intent.ACTION_VIEW, uri)
//        websiteElement.intent = browserIntent
//        return websiteElement
//    }

    /**
     * Tab element for Google play store
     * @return About page Element
     */
    private fun googlePlay(): Element {
        val playStoreElement = Element()
        playStoreElement.title = getString(R.string.about_play_store)
        playStoreElement.iconDrawable = mehdi.sakout.aboutpage.R.drawable.about_icon_google_play
        playStoreElement.iconTint = R.color.colorWhite
        playStoreElement.value = BuildConfig.APPLICATION_ID

        val uri =
            Uri.parse(getString(R.string.google_play_link).format(BuildConfig.APPLICATION_ID))
        val goToMarket = Intent(Intent.ACTION_VIEW, uri)
        playStoreElement.intent = goToMarket

        return playStoreElement
    }

    /**
     * Tab element for mail
     * @return About page Element
     */
    private fun emailElement(): Element{
        val email = getString(R.string.creative_motion_mail)
        val emailElement = Element()
        emailElement.title = getString(R.string.about_title_email)
        emailElement.iconDrawable = R.drawable.ic_mail
        emailElement.iconTint = R.color.colorWhite
        val intent = Intent(Intent.ACTION_SENDTO)
        intent.data = Uri.parse("mailto:")
        intent.putExtra(Intent.EXTRA_EMAIL, arrayOf(email))
        emailElement.intent = intent
        return emailElement
    }

    /**
     * Tab element for github repository
     * @return About page Element
     */
    private fun githubElement(): Element{
        val gitHubElement = Element()
        val id = getString(R.string.github_page)
        gitHubElement.title = getString(mehdi.sakout.aboutpage.R.string.about_github)
        gitHubElement.iconDrawable =
            R.drawable.ic_github
        gitHubElement.iconTint = R.color.colorWhite
        gitHubElement.value = id

        val intent = Intent()
        intent.action = Intent.ACTION_VIEW
        intent.addCategory(Intent.CATEGORY_BROWSABLE)
        intent.data = Uri.parse(id)

        gitHubElement.intent = intent
        return gitHubElement
    }

    /**
     * Tab element for Privacy policy to read
     * @return Privacy policy Element
     */
    private fun getPolicyElement(): Element{

        val websiteElement = Element()
        websiteElement.title = getString(R.string.intro_policy_button)
        websiteElement.iconDrawable = R.drawable.ic_incognito
        websiteElement.iconTint = R.color.colorWhite
        websiteElement.value = getString(R.string.link_privacy_policy)
        val uri = Uri.parse(getString(R.string.link_privacy_policy))
        val browserIntent = Intent(Intent.ACTION_VIEW, uri)
        websiteElement.intent = browserIntent
        return websiteElement
    }

    /**
     * Tab element for Terms of use
     * @return Terms of use page Element
     */
    private fun getTermsElement(): Element{

        val websiteElement = Element()
        websiteElement.title = getString(R.string.intro_terms_button)
        websiteElement.iconDrawable = R.drawable.ic_policy
        websiteElement.iconTint = R.color.colorWhite
        websiteElement.value = getString(R.string.link_terms)
        val uri = Uri.parse(getString(R.string.link_terms))
        val browserIntent = Intent(Intent.ACTION_VIEW, uri)
        websiteElement.intent = browserIntent
        return websiteElement
    }

}