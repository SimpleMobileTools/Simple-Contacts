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
import com.simplemobiletools.contacts.helpers.SMT_PRIVATE
import com.simplemobiletools.contacts.models.ContactSource
import kotlinx.android.synthetic.main.item_filter_contact_source.view.*
import java.util.*

class FilterContactSourcesAdapter(val activity: SimpleActivity, private val contactSources: List<ContactSource>, private val displayContactSources: Set<String>) :
        RecyclerView.Adapter<FilterContactSourcesAdapter.ViewHolder>() {
    private val itemViews = SparseArray<View>()
    private val selectedPositions = HashSet<Int>()

    init {
        contactSources.forEachIndexed { index, contactSource ->
            if (activity.config.showAllContacts() || displayContactSources.contains(contactSource.name)) {
                selectedPositions.add(index)
            }

            if (contactSource.name == activity.config.localAccountName && contactSource.type == activity.config.localAccountType) {
                contactSource.name = activity.getString(R.string.phone_storage)
            }

            if (contactSource.type == SMT_PRIVATE && displayContactSources.contains(SMT_PRIVATE)) {
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

    fun getSelectedItemsSet() = selectedPositions

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = activity.layoutInflater.inflate(R.layout.item_filter_contact_source, parent, false)
        return ViewHolder(view, adapterListener, activity)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val contactSource = contactSources[position]
        itemViews.put(position, holder.bindView(contactSource.name))
        toggleItemSelection(selectedPositions.contains(position), position)
    }

    override fun getItemCount() = contactSources.size

    class ViewHolder(view: View, private val adapterListener: MyAdapterListener, val activity: SimpleActivity) : RecyclerView.ViewHolder(view) {
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
