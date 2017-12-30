package com.simplemobiletools.contacts.adapters

import android.graphics.drawable.Drawable
import android.support.v7.widget.RecyclerView
import android.util.SparseArray
import android.view.View
import android.view.ViewGroup
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.signature.ObjectKey
import com.simplemobiletools.commons.extensions.getColoredDrawableWithColor
import com.simplemobiletools.commons.interfaces.MyAdapterListener
import com.simplemobiletools.contacts.R
import com.simplemobiletools.contacts.activities.SimpleActivity
import com.simplemobiletools.contacts.extensions.config
import com.simplemobiletools.contacts.helpers.Config
import com.simplemobiletools.contacts.models.Contact
import kotlinx.android.synthetic.main.item_add_favorite_with_number.view.*
import java.util.*

class AddFavoritesAdapter(val activity: SimpleActivity, val contacts: List<Contact>) : RecyclerView.Adapter<AddFavoritesAdapter.ViewHolder>() {
    private val itemViews = SparseArray<View>()
    private val selectedPositions = HashSet<Int>()
    private val config = activity.config
    private val textColor = config.textColor
    private val contactDrawable = activity.resources.getColoredDrawableWithColor(R.drawable.ic_person, textColor)
    private val startNameWithSurname = config.startNameWithSurname
    private val itemLayout = if (config.showPhoneNumbers) R.layout.item_add_favorite_with_number else R.layout.item_add_favorite_without_number

    private fun toggleItemSelection(select: Boolean, pos: Int) {
        if (select) {
            if (itemViews[pos] != null) {
                selectedPositions.add(pos)
            }
        } else {
            selectedPositions.remove(pos)
        }

        itemViews[pos]?.contact_checkbox?.isChecked = select
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
        selectedPositions.forEach { selectedItemsSet.add(contacts[it].id.toString()) }
        return selectedItemsSet
    }

    override fun onCreateViewHolder(parent: ViewGroup?, viewType: Int): ViewHolder {
        val view = activity.layoutInflater.inflate(itemLayout, parent, false)
        return ViewHolder(view, adapterListener, activity)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val eventType = contacts[position]
        itemViews.put(position, holder.bindView(eventType, startNameWithSurname, contactDrawable, config))
        toggleItemSelection(selectedPositions.contains(position), position)
    }

    override fun getItemCount() = contacts.size

    class ViewHolder(view: View, val adapterListener: MyAdapterListener, val activity: SimpleActivity) : RecyclerView.ViewHolder(view) {
        fun bindView(contact: Contact, startNameWithSurname: Boolean, contactDrawable: Drawable, config: Config): View {
            itemView.apply {
                contact_checkbox.setColors(config.textColor, config.primaryColor, config.backgroundColor)
                val textColor = config.textColor

                contact_name.text = contact.getFullName(startNameWithSurname)
                contact_name.setTextColor(textColor)
                contact_number?.text = contact.phoneNumbers.firstOrNull()?.value ?: ""
                contact_number?.setTextColor(textColor)
                contact_frame.setOnClickListener { viewClicked(!contact_checkbox.isChecked) }

                if (contact.photoUri.isNotEmpty()) {
                    val options = RequestOptions()
                            .signature(ObjectKey(contact.photoUri))
                            .diskCacheStrategy(DiskCacheStrategy.RESOURCE)
                            .error(contactDrawable)
                            .centerCrop()

                    Glide.with(activity).load(contact.photoUri).transition(DrawableTransitionOptions.withCrossFade()).apply(options).into(contact_tmb)
                } else {
                    contact_tmb.setImageDrawable(contactDrawable)
                }
            }

            return itemView
        }

        private fun viewClicked(select: Boolean) {
            adapterListener.toggleItemSelectionAdapter(select, adapterPosition)
        }
    }

    override fun onViewRecycled(holder: ViewHolder?) {
        super.onViewRecycled(holder)
        Glide.with(activity).clear(holder?.itemView?.contact_tmb)
    }
}
