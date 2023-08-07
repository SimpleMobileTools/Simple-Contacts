package com.simplemobiletools.contacts.pro.adapters

import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.simplemobiletools.commons.extensions.getProperBackgroundColor
import com.simplemobiletools.commons.extensions.getProperPrimaryColor
import com.simplemobiletools.commons.extensions.getProperTextColor
import com.simplemobiletools.commons.helpers.SMT_PRIVATE
import com.simplemobiletools.commons.models.contacts.ContactSource
import com.simplemobiletools.contacts.pro.activities.SimpleActivity
import com.simplemobiletools.contacts.pro.databinding.ItemFilterContactSourceBinding

class FilterContactSourcesAdapter(
    val activity: SimpleActivity,
    private val contactSources: List<ContactSource>,
    private val displayContactSources: List<String>
) : RecyclerView.Adapter<FilterContactSourcesAdapter.ViewHolder>() {

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
        return ViewHolder(ItemFilterContactSourceBinding.inflate(activity.layoutInflater, parent, false).root)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val contactSource = contactSources[position]
        holder.bindView(contactSource)
    }

    override fun getItemCount() = contactSources.size

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        fun bindView(contactSource: ContactSource): View {
            val isSelected = selectedKeys.contains(contactSource.hashCode())
            ItemFilterContactSourceBinding.bind(itemView).apply {
                filterContactSourceCheckbox.isChecked = isSelected
                filterContactSourceCheckbox.setColors(activity.getProperTextColor(), activity.getProperPrimaryColor(), activity.getProperBackgroundColor())
                val countText = if (contactSource.count >= 0) " (${contactSource.count})" else ""
                val displayName = "${contactSource.publicName}$countText"
                filterContactSourceCheckbox.text = displayName
                filterContactSourceHolder.setOnClickListener { viewClicked(!isSelected, contactSource) }
            }

            return itemView
        }

        private fun viewClicked(select: Boolean, contactSource: ContactSource) {
            toggleItemSelection(select, contactSource, adapterPosition)
        }
    }
}
