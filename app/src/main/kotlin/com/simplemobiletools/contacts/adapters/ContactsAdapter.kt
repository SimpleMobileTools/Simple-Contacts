package com.simplemobiletools.contacts.adapters

import android.graphics.drawable.Drawable
import android.view.Menu
import android.view.View
import android.view.ViewGroup
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.signature.ObjectKey
import com.simplemobiletools.commons.adapters.MyRecyclerViewAdapter
import com.simplemobiletools.commons.dialogs.ConfirmationDialog
import com.simplemobiletools.commons.extensions.beVisibleIf
import com.simplemobiletools.commons.extensions.getColoredDrawableWithColor
import com.simplemobiletools.commons.extensions.isActivityDestroyed
import com.simplemobiletools.commons.views.FastScroller
import com.simplemobiletools.commons.views.MyRecyclerView
import com.simplemobiletools.contacts.R
import com.simplemobiletools.contacts.activities.SimpleActivity
import com.simplemobiletools.contacts.extensions.config
import com.simplemobiletools.contacts.extensions.editContact
import com.simplemobiletools.contacts.extensions.shareContacts
import com.simplemobiletools.contacts.helpers.ContactsHelper
import com.simplemobiletools.contacts.interfaces.RefreshContactsListener
import com.simplemobiletools.contacts.models.Contact
import kotlinx.android.synthetic.main.item_contact_with_number.view.*
import java.util.*

class ContactsAdapter(activity: SimpleActivity, var contactItems: ArrayList<Contact>, private val listener: RefreshContactsListener?,
                      private val isFavoritesFragment: Boolean, recyclerView: MyRecyclerView, fastScroller: FastScroller, itemClick: (Any) -> Unit) :
        MyRecyclerViewAdapter(activity, recyclerView, fastScroller, itemClick) {

    private lateinit var contactDrawable: Drawable
    var config = activity.config
    var startNameWithSurname: Boolean
    var showContactThumbnails: Boolean
    var showPhoneNumbers: Boolean

    private var smallPadding = activity.resources.getDimension(R.dimen.small_margin).toInt()
    private var bigPadding = activity.resources.getDimension(R.dimen.normal_margin).toInt()

    init {
        initDrawables()
        showContactThumbnails = config.showContactThumbnails
        showPhoneNumbers = config.showPhoneNumbers
        startNameWithSurname = config.startNameWithSurname
    }

    override fun getActionMenuId() = R.menu.cab

    override fun prepareActionMode(menu: Menu) {
        menu.apply {
            findItem(R.id.cab_edit).isVisible = isOneItemSelected()
            findItem(R.id.cab_remove).isVisible = isFavoritesFragment
            findItem(R.id.cab_select_all).isVisible = isFavoritesFragment
            findItem(R.id.cab_add_to_favorites).isVisible = !isFavoritesFragment
            findItem(R.id.cab_delete).isVisible = !isFavoritesFragment
        }
    }

    override fun prepareItemSelection(view: View) {}

    override fun markItemSelection(select: Boolean, view: View?) {
        view?.contact_frame?.isSelected = select
    }

    override fun actionItemPressed(id: Int) {
        if (selectedPositions.isEmpty()) {
            return
        }

        when (id) {
            R.id.cab_edit -> editContact()
            R.id.cab_select_all -> selectAll()
            R.id.cab_add_to_favorites -> addToFavorites()
            R.id.cab_share -> shareContacts()
            R.id.cab_remove -> removeFavorites()
            R.id.cab_delete -> askConfirmDelete()
        }
    }

    override fun getSelectableItemCount() = contactItems.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val layout = if (showPhoneNumbers) R.layout.item_contact_with_number else R.layout.item_contact_without_number
        return createViewHolder(layout, parent)
    }

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

    fun updateItems(newItems: ArrayList<Contact>) {
        contactItems = newItems
        notifyDataSetChanged()
        finishActMode()
    }

    private fun editContact() {
        activity.editContact(contactItems[selectedPositions.first()])
    }

    private fun askConfirmDelete() {
        ConfirmationDialog(activity) {
            deleteContacts()
        }
    }

    private fun deleteContacts() {
        if (selectedPositions.isEmpty()) {
            return
        }

        val contactsToRemove = ArrayList<Contact>()
        selectedPositions.sortedDescending().forEach {
            contactsToRemove.add(contactItems[it])
        }
        contactItems.removeAll(contactsToRemove)

        ContactsHelper(activity).deleteContacts(contactsToRemove)
        if (contactItems.isEmpty()) {
            listener?.refreshContacts()
            finishActMode()
        } else {
            removeSelectedItems()
            listener?.refreshFavorites()
        }
    }

    private fun removeFavorites() {
        val favoritesToRemove = ArrayList<Contact>()
        selectedPositions.sortedDescending().forEach {
            favoritesToRemove.add(contactItems[it])
        }
        contactItems.removeAll(favoritesToRemove)

        ContactsHelper(activity).removeFavorites(favoritesToRemove)
        if (contactItems.isEmpty()) {
            listener?.refreshFavorites()
            finishActMode()
        } else {
            removeSelectedItems()
        }
    }

    private fun addToFavorites() {
        val newFavorites = ArrayList<Contact>()
        selectedPositions.forEach { newFavorites.add(contactItems[it]) }
        ContactsHelper(activity).addFavorites(newFavorites)
        listener?.refreshFavorites()
        finishActMode()
    }

    private fun shareContacts() {
        val contactsIDs = ArrayList<Int>()
        selectedPositions.forEach {
            contactsIDs.add(contactItems[it].id)
        }

        ContactsHelper(activity).getContacts {
            val filtered = it.filter { contactsIDs.contains(it.id) } as ArrayList<Contact>
            activity.shareContacts(filtered)
        }
    }

    override fun onViewRecycled(holder: ViewHolder) {
        super.onViewRecycled(holder)
        if (!activity.isActivityDestroyed()) {
            Glide.with(activity).clear(holder.itemView?.contact_tmb!!)
        }
    }

    private fun setupView(view: View, contact: Contact) {
        view.apply {
            contact_name.text = contact.getFullName(startNameWithSurname)
            contact_name.setTextColor(textColor)
            contact_name.setPadding(if (showContactThumbnails) smallPadding else bigPadding, smallPadding, smallPadding, 0)

            contact_number?.text = contact.phoneNumbers.firstOrNull()?.value ?: ""
            contact_number?.setTextColor(textColor)
            contact_number?.setPadding(if (showContactThumbnails) smallPadding else bigPadding, 0, smallPadding, 0)

            contact_tmb.beVisibleIf(showContactThumbnails)

            if (showContactThumbnails) {
                when {
                    contact.photoUri.isNotEmpty() -> {
                        val options = RequestOptions()
                                .signature(ObjectKey(contact.photoUri))
                                .diskCacheStrategy(DiskCacheStrategy.RESOURCE)
                                .error(contactDrawable)
                                .centerCrop()

                        Glide.with(activity).load(contact.photoUri).transition(DrawableTransitionOptions.withCrossFade()).apply(options).into(contact_tmb)
                    }
                    contact.photo != null -> {
                        val options = RequestOptions()
                                .signature(ObjectKey(contact.photo!!))
                                .diskCacheStrategy(DiskCacheStrategy.RESOURCE)
                                .error(contactDrawable)
                                .centerCrop()

                        Glide.with(activity).load(contact.photo).transition(DrawableTransitionOptions.withCrossFade()).apply(options).into(contact_tmb)
                    }
                    else -> contact_tmb.setImageDrawable(contactDrawable)
                }
            }
        }
    }
}
