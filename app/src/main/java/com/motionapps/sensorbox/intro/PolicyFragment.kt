package com.motionapps.sensorbox.intro

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.navArgs
import com.github.appintro.SlideBackgroundColorHolder
import com.motionapps.sensorbox.R


/**
 * can pick folder to store all the data
 * this action is not required on the devices with Android below Q
 * this fragment is recycled in settings, if the user wants to change folder to save data
 */
class PolicyFragment : Fragment(), SlideBackgroundColorHolder {

    private var backgroundColour: Int? = null
    private val args: PickFolderFragmentArgs by navArgs()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            backgroundColour = it.getInt(BACKGROUND_COLOR, -1)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        val view: View = inflater.inflate(R.layout.fragment_policy, container, false)
        view.setBackgroundColor(defaultBackgroundColor)

        view.findViewById<Button>(R.id.intro_policy).setOnClickListener {
            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.link_privacy_policy)))
            startActivity(browserIntent)
        }

        view.findViewById<Button>(R.id.intro_terms).setOnClickListener {
            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.link_terms)))
            startActivity(browserIntent)
        }

        return view
    }

    // default colour of the background
    override val defaultBackgroundColor: Int
        get() {
            return when (backgroundColour) {
                null -> {
                    ContextCompat.getColor(requireContext(), R.color.colorBlack)
                }
                -1 -> {
                    args.color
                }
                else -> {
                    backgroundColour as Int
                }
            }
        }

    override fun setBackgroundColor(backgroundColor: Int) {}

    companion object {
        const val BACKGROUND_COLOR: String = "BackgroundParameter"

        fun newInstance(param: Int) =
            PolicyFragment().apply {
                arguments = Bundle().apply {
                    putInt(BACKGROUND_COLOR, param)
                }
            }
    }
}