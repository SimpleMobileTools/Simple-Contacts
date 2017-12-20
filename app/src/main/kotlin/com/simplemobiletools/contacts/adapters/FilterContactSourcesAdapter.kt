package com.simplemobiletools.contacts.adapters

import android.support.v7.widget.RecyclerView
import android.util.SparseArray
import android.view.View
import android.view.ViewGroup
import com.simplemobiletools.commons.extensions.getAdjustedPrimaryColor
import com.simplemobiletools.commons.interfaces.MyAdapterListener
import com.simplemobiletools.contacts.R
import com.simplemobiletools.contacts.activities.SimpleActivity
import com.simplemobiletools.contacts.extensions.config
import kotlinx.android.synthetic.main.item_filter_contact_source.view.*
import java.util.*

class FilterContactSourcesAdapter(val activity: SimpleActivity, val contactSources: List<String>, val displayContactSources: Set<String>) :
        RecyclerView.Adapter<FilterContactSourcesAdapter.ViewHolder>() {
    private val itemViews = SparseArray<View>()
    private val selectedPositions = HashSet<Int>()

    init {
        contactSources.forEachIndexed { index, value ->
            if (activity.config.showAllContacts() || displayContactSources.contains(value)) {
                selectedPositions.add(index)
            }
        }
    }

    private fun toggleItemSelection(select: Boolean, pos: Int) {
        if (select) {
            if (itemViews[pos] != null) {
                selectedPositions.add(pos)
            }
        } else {
            selectedPositions.remove(pos)
        }

        itemViews[pos]?.filter_contact_source_checkbox?.isChecked = select
    }

    private val adapterListener = object : MyAdapterListener {
        override fun toggleItemSelectionAdapter(select: Boolean, position: Int) {
            toggleItemSelection(select, position)
        }

        override fun getSelectedPositions() = selectedPositions

        override fun itemLongClicked(position: Int) {}
    }

    fun getSelectedItemsSet(): HashSet<String> {
        val selectedItemsSet = HashSet<String>(selectedPositions.size)
        selectedPositions.forEach { selectedItemsSet.add(contactSources[it]) }
        return selectedItemsSet
    }

    override fun onCreateViewHolder(parent: ViewGroup?, viewType: Int): ViewHolder {
        val view = activity.layoutInflater.inflate(R.layout.item_filter_contact_source, parent, false)
        return ViewHolder(view, adapterListener, activity)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val contactSource = contactSources[position]
        itemViews.put(position, holder.bindView(contactSource))
        toggleItemSelection(selectedPositions.contains(position), position)
    }

    override fun getItemCount() = contactSources.size

    class ViewHolder(view: View, val adapterListener: MyAdapterListener, val activity: SimpleActivity) : RecyclerView.ViewHolder(view) {
        fun bindView(contactSource: String): View {
            itemView.apply {
                filter_contact_source_checkbox.setColors(activity.config.textColor, activity.getAdjustedPrimaryColor(), activity.config.backgroundColor)
                filter_contact_source_checkbox.text = contactSource
                filter_contact_source_holder.setOnClickListener { viewClicked(!filter_contact_source_checkbox.isChecked) }
            }

            return itemView
        }

        private fun viewClicked(select: Boolean) {
            adapterListener.toggleItemSelectionAdapter(select, adapterPosition)
        }
    }
}
