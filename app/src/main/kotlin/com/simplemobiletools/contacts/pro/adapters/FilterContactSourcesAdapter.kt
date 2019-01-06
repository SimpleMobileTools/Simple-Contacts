package com.simplemobiletools.contacts.pro.adapters

import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.simplemobiletools.commons.extensions.getAdjustedPrimaryColor
import com.simplemobiletools.contacts.pro.R
import com.simplemobiletools.contacts.pro.activities.SimpleActivity
import com.simplemobiletools.contacts.pro.extensions.config
import com.simplemobiletools.contacts.pro.helpers.SMT_PRIVATE
import com.simplemobiletools.contacts.pro.models.ContactSource
import kotlinx.android.synthetic.main.item_filter_contact_source.view.*
import java.util.*

class FilterContactSourcesAdapter(val activity: SimpleActivity, private val contactSources: List<ContactSource>, private val displayContactSources: ArrayList<String>) :
        RecyclerView.Adapter<FilterContactSourcesAdapter.ViewHolder>() {
    private val selectedKeys = HashSet<Int>()

    init {
        contactSources.forEachIndexed { index, contactSource ->
            if (displayContactSources.contains(contactSource.name)) {
                selectedKeys.add(contactSource.hashCode())
            }

            if (contactSource.type == SMT_PRIVATE && displayContactSources.contains(SMT_PRIVATE)) {
                selectedKeys.add(contactSource.hashCode())
            }
        }
    }

    private fun toggleItemSelection(select: Boolean, contactSource: ContactSource, position: Int) {
        if (select) {
            selectedKeys.add(contactSource.hashCode())
        } else {
            selectedKeys.remove(contactSource.hashCode())
        }

        notifyItemChanged(position)
    }

    fun getSelectedContactSources() = contactSources.filter { selectedKeys.contains(it.hashCode()) }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = activity.layoutInflater.inflate(R.layout.item_filter_contact_source, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val contactSource = contactSources[position]
        holder.bindView(contactSource)
    }

    override fun getItemCount() = contactSources.size

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        fun bindView(contactSource: ContactSource): View {
            val isSelected = selectedKeys.contains(contactSource.hashCode())
            itemView.apply {
                filter_contact_source_checkbox.isChecked = isSelected
                filter_contact_source_checkbox.setColors(activity.config.textColor, activity.getAdjustedPrimaryColor(), activity.config.backgroundColor)
                filter_contact_source_checkbox.text = contactSource.publicName
                filter_contact_source_holder.setOnClickListener { viewClicked(!isSelected, contactSource) }
            }

            return itemView
        }

        private fun viewClicked(select: Boolean, contactSource: ContactSource) {
            toggleItemSelection(select, contactSource, adapterPosition)
        }
    }
}
