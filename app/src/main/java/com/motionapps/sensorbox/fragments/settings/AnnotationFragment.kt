package com.motionapps.sensorbox.fragments.settings

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.preference.PreferenceManager
import com.motionapps.sensorbox.R


class AnnotationFragment : Fragment() {

    private var linearLayout: LinearLayout ?= null
    private var editText: EditText ?= null
    private val annots: ArrayList<String> = ArrayList()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_annotation, container, false)
        linearLayout = view.findViewById(R.id.annots_container)
        editText = view.findViewById(R.id.annots_adding_edittext)
        inflateAnnots(view)
        return view
    }

    /**
     * Loads annots from sharedPreferences and inflate views to show
     *
     * @param view - for button registration
     */
    private fun inflateAnnots(view: View) {
        val sharedPreferences: SharedPreferences =
            PreferenceManager.getDefaultSharedPreferences(requireContext())
        val stringSet: Set<String>? = sharedPreferences.getStringSet(ANNOTS, null)
        if (stringSet != null) {
            for (s: String in stringSet) {
                addAnnot(s)
                annots.add(s)
            }
        }

        (view.findViewById<Button>(R.id.annots_adding_button)).also {
            it?.setOnClickListener {
                val s: String? = editText?.text?.toString() // adding annotation to linearLayout
                if (s!= null && s != "") {
                    addAnnot(s)
                    annots.add(s)
                    editText?.text?.clear()
                }
            }
        }
    }

    /**
     * method to add all the annotations from preferences
     * also, the button for removing annotation is registered
     * @param s - value of the annotation
     */
    private fun addAnnot(s: String) {
        val view: View = layoutInflater.inflate(R.layout.item_layout_annotation, linearLayout, false)
        val text: TextView = view.findViewById(R.id.annot_row_text)
        text.text = s

        val button: ImageButton = view.findViewById(R.id.annot_row_button)
        button.setOnClickListener {
            removeAnnot(view, s)
        }

        linearLayout?.addView(view)

    }

    /**
     * removal of the annotation
     *
     * @param view - view to remove
     * @param s - value to remove
     */
    private fun removeAnnot(view: View, s: String) {
        linearLayout?.removeView(view)
        annots.remove(s)
    }

    /**
     * Called at the end of the fragment - all the left annotations are saved
     *
     */
    private fun saveItems() {
        val sharedPreferences: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(requireContext())
        val editor: SharedPreferences.Editor = sharedPreferences.edit()
        if(annots.isNotEmpty()){
            editor.putStringSet(ANNOTS, annots.toSet())
        }else{
            editor.putStringSet(ANNOTS, null)
        }

        editor.apply()
    }

    override fun onDestroy() {
        super.onDestroy()
        saveItems()
        hideKeyboard(requireActivity())
    }

    /**
     * Hiding keys on leaving the fragment
     *
     * @param activity
     */
    private fun hideKeyboard(activity: Activity) {
        val inputMethodManager =
            activity.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager

        // Check if no view has focus
        val currentFocusedView = activity.currentFocus
        currentFocusedView?.let {
            inputMethodManager.hideSoftInputFromWindow(
                currentFocusedView.windowToken, InputMethodManager.HIDE_NOT_ALWAYS)
        }
    }

    companion object{
        const val ANNOTS = "annotation_set"
    }
}