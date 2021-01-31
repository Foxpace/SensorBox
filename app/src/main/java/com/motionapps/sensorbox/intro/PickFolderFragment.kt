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
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.navArgs
import com.balda.flipper.StorageManagerCompat
import com.github.appintro.SlideBackgroundColorHolder
import com.github.appintro.SlidePolicy
import com.motionapps.sensorbox.R
import com.motionapps.sensorservices.handlers.StorageHandler

/**
 * can pick folder to store all the data
 * this action is not required on the devices with Android below Q
 * this fragment is recycled in settings, if the user wants to change folder to save data
 */
class PickFolderFragment : Fragment(), SlideBackgroundColorHolder, SlidePolicy {

    private var backgroundColour: Int? = null
    private var isPath: Boolean = false
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
        val view: View = inflater.inflate(R.layout.fragment_pick_folder, container, false)
        view.setBackgroundColor(defaultBackgroundColor)

        // folder can be changed
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            (view.findViewById<Button>(R.id.intro_pick_folder_button)).apply {
                setOnClickListener {
                    val manager = StorageManagerCompat(requireContext())
                    val i: Intent = manager.requireExternalAccess(requireContext())!!
                    this@PickFolderFragment.startActivityForResult(i, NEW_FOLDER_REQUEST_CODE)
                }
                (view.findViewById<TextView>(R.id.description)).apply {
                    text = getString(R.string.intro_pick_folder_description).format(
                        StorageHandler.getFolderName(requireContext())
                    )
                }
            }
            updateText(view)

            // automatic creation of the folder
        } else {
            (view.findViewById<TextView>(R.id.description)).apply {
                text = getString(R.string.intro_pick_folder_description_under_Q)
            }

            (view.findViewById<TextView>(R.id.folder_pick_title)).apply{
                text = getString(R.string.intro_pick_folder_placement)
            }

            (view.findViewById<Button>(R.id.intro_pick_folder_button)).apply {
                visibility = GONE
            }

            isPath = if(StorageHandler.isFolder(requireContext())){
                StorageHandler.createMainFolder(requireContext(), null)
            }else{
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

    /**
     * Pick of the folder requires System folder activity, where user can pick path
     *
     * @param requestCode
     * @param resultCode
     * @param data
     */
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK && requestCode == NEW_FOLDER_REQUEST_CODE && data != null) {
            isPath = StorageHandler.createMainFolder(requireContext(), data) // creates folder
            updateText(requireView()) // updates text in fragment with path
        }
    }


    private fun updateText(view: View){
        (view.findViewById<TextView>(R.id.description)).apply {
            text = getString(R.string.intro_pick_folder_description).format(StorageHandler.getFolderName(requireContext()))
        }
    }

    companion object {
        const val BACKGROUND_COLOR: String = "BackgroundParameter"
        const val NEW_FOLDER_REQUEST_CODE: Int = 982

        fun newInstance(param: Int) =
            PickFolderFragment().apply {
                arguments = Bundle().apply {
                    putInt(BACKGROUND_COLOR, param)
                }
            }
    }

    override val isPolicyRespected: Boolean
        get() = isPath

    override fun onUserIllegallyRequestedNextPage() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            Toast.makeText(
                requireContext(),
                getString(R.string.intro_condition_folder),
                Toast.LENGTH_LONG
            ).show()
        }
    }
}