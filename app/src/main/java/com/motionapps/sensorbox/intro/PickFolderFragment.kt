package com.motionapps.sensorbox.intro

import android.app.Activity
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.navArgs
import com.balda.flipper.StorageManagerCompat
import com.github.appintro.SlideBackgroundColorHolder
import com.github.appintro.SlidePolicy
import com.motionapps.sensorbox.R
import com.motionapps.sensorservices.handlers.StorageHandler
import es.dmoral.toasty.Toasty

/**
 * can pick folder to store all the data
 * this action is not required on the devices with Android below Q
 * this fragment is recycled in settings, if the user wants to change folder to save data
 */
class PickFolderFragment : Fragment(), SlideBackgroundColorHolder, SlidePolicy {

    private var backgroundColour: Int? = null
    private var isPath: Boolean = false
    private val args: PickFolderFragmentArgs by navArgs()
    private val resultLauncher = this@PickFolderFragment.registerForActivityResult(ActivityResultContracts.StartActivityForResult()){
            result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            isPath = StorageHandler.createMainFolder(requireContext(), result.data) // creates folder
            updateText(requireView()) // updates text in fragment with path
        }
    }

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
        val view: View = inflater.inflate(R.layout.fragment_pick_folder, container, false)
        view.setBackgroundColor(defaultBackgroundColor)
        // folder can be changed
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            (view.findViewById<Button>(R.id.intro_pick_folder_button)).apply {
                setOnClickListener {
                    val manager = StorageManagerCompat(requireContext())
                    val i: Intent = manager.requireExternalAccess(requireContext())!!
                    resultLauncher.launch(i)
                }
                (view.findViewById<TextView>(R.id.description)).apply {
                    text =
                        requireActivity().getString(R.string.intro_pick_folder_description).format(
                            StorageHandler.getFolderName(requireContext())
                        )
                }
            }
            updateText(view)

            // automatic creation of the folder
        } else {
            (view.findViewById<TextView>(R.id.description)).apply {
                text = requireActivity().getString(R.string.intro_pick_folder_description_under_Q)
            }

            (view.findViewById<TextView>(R.id.folder_pick_title)).apply {
                text = requireActivity().getString(R.string.intro_pick_folder_placement)
            }

            (view.findViewById<Button>(R.id.intro_pick_folder_button)).apply {
                visibility = GONE
            }

            isPath = if (StorageHandler.isFolder(requireContext())) {
                StorageHandler.createMainFolder(requireContext(), null)
            } else {
                true
            }

        }

        return view
    }

    // default colour of the background
    override val defaultBackgroundColor: Int
        get() {
            return when (backgroundColour) {
                null -> {
                    ContextCompat.getColor(requireContext(), R.color.colorBlueGray)
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

    private fun updateText(view: View) {
        (view.findViewById<TextView>(R.id.description)).apply {
            text = requireActivity().getString(R.string.intro_pick_folder_description)
                .format(StorageHandler.getFolderName(requireContext()))
        }
    }

    companion object {
        const val BACKGROUND_COLOR: String = "BackgroundParameter"

        @JvmStatic
        fun newInstance(param: Int): PickFolderFragment {
            val fragment = PickFolderFragment()
            val arguments = Bundle().apply {
                putInt(BACKGROUND_COLOR, param)
            }
            fragment.arguments = arguments
            return fragment

        }
    }

    override val isPolicyRespected: Boolean
        get() = isPath

    override fun onUserIllegallyRequestedNextPage() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            Toasty.warning(
                this@PickFolderFragment.requireActivity(),
                this@PickFolderFragment.requireActivity()
                    .getString(R.string.intro_condition_folder),
                Toasty.LENGTH_LONG
            ).show()
        }
    }
}