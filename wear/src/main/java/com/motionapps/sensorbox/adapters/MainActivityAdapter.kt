package com.motionapps.sensorbox.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.motionapps.sensorbox.R
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.InternalCoroutinesApi
import java.util.*

@ExperimentalCoroutinesApi
@InternalCoroutinesApi
/**
 * user can pick one among all available activities like settings, recording, showing sensor, ...
 *
 * @param context
 */
class MainActivityAdapter(context: Context?) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    // texts to inflate
    private val texts: ArrayList<Int> = object : ArrayList<Int>() {
        init {
            add(R.string.activity_record)
            add(R.string.activity_view_sensor)
            add(R.string.activity_info_phone)
            add(R.string.activity_settings)
            add(R.string.activity_privacy_policy)
            add(R.string.activity_terms_of_use)
        }
    }
    // icons to inflate
    private val images: ArrayList<Int> = object : ArrayList<Int>() {
        init {
            add(R.drawable.ic_archive)
            add(R.drawable.ic_poll)
            add(R.drawable.ic_info)
            add(R.drawable.ic_more_horiz_24dp_wht)
            add(R.drawable.ic_incognito)
            add(R.drawable.ic_policy)
        }
    }
    // classes to use
    private val actions: ArrayList<Int> = object : ArrayList<Int>() {
        init {
            add(SENSOR_MEASUREMENT)
            add(SENSOR_SHOW)
            add(PHONE_INFO)
            add(SETTINGS)
            add(PRIVACY_POLICY)
            add(TERMS)
        }
    }

    private val mInflater: LayoutInflater = LayoutInflater.from(context)
    var clickListener: ClickListenerInterface? = null

    /**
     * inflates empty view
     *
     * @param parent
     * @param viewType
     * @return ButtonHolder with specific activity
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val view = mInflater.inflate(R.layout.button_layout_menu, parent, false)
        return ButtonHolder(view)
    }

    /**
     * adds action to the views
     *
     * @param holder ButtonHolder, where the icon and text is inflated
     * @param position of the view
     */
    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val imageView = holder.itemView.findViewById<ImageView>(R.id.menu_button_image)
        imageView.setImageResource(images[position])
        val textView = holder.itemView.findViewById<TextView>(R.id.menu_button_text)
        textView.setText(texts[position])
    }

    override fun getItemCount(): Int {
        return actions.size
    }

    /**
     * holds reference to classes after click
     *
     * @param itemView
     */
    private inner class ButtonHolder(itemView: View) :
        RecyclerView.ViewHolder(itemView), View.OnClickListener {
        override fun onClick(view: View) {
            if (clickListener != null) {
                clickListener!!.onClick(actions[bindingAdapterPosition])
            }
        }

        init {
            itemView.setOnClickListener(this)
        }
    }

    /**
     * provides callback for the activity to handle clicks from the adapter and passes specific class
     *
     */
    interface ClickListenerInterface {
        fun onClick(action: Int)
    }

    companion object{

        const val SENSOR_MEASUREMENT = 0
        const val SENSOR_SHOW = 1
        const val PHONE_INFO = 3
        const val SETTINGS  = 2
        const val PRIVACY_POLICY = 4
        const val TERMS = 5


    }

}