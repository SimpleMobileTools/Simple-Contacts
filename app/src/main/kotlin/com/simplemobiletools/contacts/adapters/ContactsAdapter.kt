package com.simplemobiletools.contacts.adapters

import android.graphics.drawable.Drawable
import android.view.Menu
import android.view.View
import android.view.ViewGroup
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.request.RequestOptions
import com.simplemobiletools.commons.adapters.MyRecyclerViewAdapter
import com.simplemobiletools.commons.dialogs.ConfirmationDialog
import com.simplemobiletools.commons.extensions.getColoredDrawableWithColor
import com.simplemobiletools.commons.extensions.isActivityDestroyed
import com.simplemobiletools.commons.interfaces.RefreshRecyclerViewListener
import com.simplemobiletools.commons.views.MyRecyclerView
import com.simplemobiletools.contacts.R
import com.simplemobiletools.contacts.activities.SimpleActivity
import com.simplemobiletools.contacts.extensions.config
import com.simplemobiletools.contacts.extensions.openContact
import com.simplemobiletools.contacts.models.Contact
import kotlinx.android.synthetic.main.item_contact.view.*

class ContactsAdapter(activity: SimpleActivity, var contactItems: MutableList<Contact>, val listener: RefreshRecyclerViewListener?,
                      recyclerView: MyRecyclerView, itemClick: (Any) -> Unit) : MyRecyclerViewAdapter(activity, recyclerView, itemClick) {

    lateinit private var contactDrawable: Drawable

    init {
        initDrawables()
    }

    override fun getActionMenuId() = R.menu.cab

    override fun prepareActionMode(menu: Menu) {
        menu.apply {
            findItem(R.id.cab_edit).isVisible = activity.config.callContact && isOneItemSelected()
        }
    }

    override fun prepareItemSelection(view: View) {}

    override fun markItemSelection(select: Boolean, view: View?) {
        view?.contact_frame?.isSelected = select
    }

    override fun actionItemPressed(id: Int) {
        when (id) {
            R.id.cab_edit -> editContact()
            R.id.cab_select_all -> selectAll()
            R.id.cab_delete -> askConfirmDelete()
        }
    }

    override fun getSelectableItemCount() = contactItems.size

    override fun onCreateViewHolder(parent: ViewGroup?, viewType: Int) = createViewHolder(R.layout.item_contact, parent)

    override fun onBindViewHolder(holder: MyRecyclerViewAdapter.ViewHolder, position: Int) {
        val contact = contactItems[position]
        val view = holder.bindView(contact, true) { itemView, layoutPosition ->
            setupView(itemView, contact)
        }
        bindViewHolder(holder, position, view)
    }

    override fun getItemCount() = contactItems.size

    fun initDrawables() {
        contactDrawable = activity.resources.getColoredDrawableWithColor(R.drawable.ic_person, textColor)
    }

    fun updateItems(newItems: MutableList<Contact>) {
        contactItems = newItems
        notifyDataSetChanged()
        finishActMode()
    }

    private fun editContact() {
        activity.openContact(contactItems[selectedPositions.first()])
        finishActMode()
    }

    private fun askConfirmDelete() {
        ConfirmationDialog(activity) {
            deleteContacts()
        }
    }

    private fun deleteContacts() {

    }

    override fun onViewRecycled(holder: ViewHolder?) {
        super.onViewRecycled(holder)
        if (!activity.isActivityDestroyed()) {
            Glide.with(activity).clear(holder?.itemView?.contact_tmb)
        }
    }

    private fun setupView(view: View, contact: Contact) {
        view.apply {
            contact_name.text = contact.getFullName()
            contact_name.setTextColor(textColor)
            contact_number.text = contact.number
            contact_number.setTextColor(textColor)

            if (contact.photoUri.isNotEmpty()) {
                val options = RequestOptions()
                        .diskCacheStrategy(DiskCacheStrategy.RESOURCE)
                        .error(contactDrawable)
                        .centerCrop()

                Glide.with(activity).load(contact.photoUri).transition(DrawableTransitionOptions.withCrossFade()).apply(options).into(contact_tmb)
            } else {
                contact_tmb.setImageDrawable(contactDrawable)
            }
        }
    }
}
