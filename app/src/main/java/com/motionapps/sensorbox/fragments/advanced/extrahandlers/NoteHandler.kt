package com.motionapps.sensorbox.fragments.advanced.extrahandlers

import android.view.LayoutInflater
import android.view.View
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import com.motionapps.sensorbox.R
import dagger.hilt.android.scopes.ViewModelScoped
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.InternalCoroutinesApi
import javax.inject.Inject

@InternalCoroutinesApi
@ExperimentalCoroutinesApi
@ViewModelScoped

/**
 * Simple to use class for storage of custom Strings - notes for the measurement
 * all the strings are stored in all the JSON files
 */
class NoteHandler @Inject constructor() {

    val notes: ArrayList<String> = ArrayList()

    /**
     * adding to linearLayout notes
     *
     * @param linearLayout
     * @param layoutInflater
     */
    fun refreshLayout(
        linearLayout: LinearLayout,
        layoutInflater: LayoutInflater
    ) {

        if (notes.isNotEmpty()) {
            for (s: String in notes) {
                addNote(s, linearLayout, layoutInflater, false)
            }
        }
    }

    /**
     * Adds note to linearlayout and can add it to storage
     *
     * @param s - note to store/show
     * @param linearLayout - linearLayout to add view
     * @param layoutInflater - inflater to inflate view
     * @param addToNotes - true, note is added to storage and shown, false - it is only shown
     */
    internal fun addNote(
        s: String,
        linearLayout: LinearLayout,
        layoutInflater: LayoutInflater,
        addToNotes: Boolean
    ){

        val view: View = layoutInflater.inflate(R.layout.item_layout_annotation, null)!!
        (view.findViewById<TextView>(R.id.annot_row_text)).also{
            it.text = s
        }
        (view.findViewById<ImageButton>(R.id.annot_row_button)).also {
            it.setOnClickListener{
                removeNote(view, s, linearLayout)
            }
        }

        linearLayout.addView(view)
        if(addToNotes){
            notes.add(s)
        }
    }

    /**
     * removes view from linealLayout and storage
     *
     * @param view - view to remove from layout
     * @param s - string value of note
     * @param linearLayout - view is removed from this layout
     */
    private fun removeNote(view: View, s: String, linearLayout: LinearLayout){
        linearLayout.removeView(view)
        notes.remove(s)
    }

}