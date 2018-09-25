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
import com.simplemobiletools.commons.dialogs.RadioGroupDialog
import com.simplemobiletools.commons.extensions.*
import com.simplemobiletools.commons.models.RadioItem
import com.simplemobiletools.commons.views.FastScroller
import com.simplemobiletools.commons.views.MyRecyclerView
import com.simplemobiletools.contacts.R
import com.simplemobiletools.contacts.activities.SimpleActivity
import com.simplemobiletools.contacts.dialogs.CreateNewGroupDialog
import com.simplemobiletools.contacts.extensions.*
import com.simplemobiletools.contacts.helpers.*
import com.simplemobiletools.contacts.interfaces.RefreshContactsListener
import com.simplemobiletools.contacts.interfaces.RemoveFromGroupListener
import com.simplemobiletools.contacts.models.Contact
import kotlinx.android.synthetic.main.item_contact_with_number.view.*
import java.util.*

class ContactsAdapter(activity: SimpleActivity, var contactItems: ArrayList<Contact>, private val refreshListener: RefreshContactsListener?,
                      private val location: Int, private val removeListener: RemoveFromGroupListener?, recyclerView: MyRecyclerView,
                      fastScroller: FastScroller, itemClick: (Any) -> Unit) :
        MyRecyclerViewAdapter(activity, recyclerView, fastScroller, itemClick) {

    private lateinit var contactDrawable: Drawable
    private var config = activity.config
    private var textToHighlight = ""

    var adjustedPrimaryColor = activity.getAdjustedPrimaryColor()
    var startNameWithSurname: Boolean
    var showContactThumbnails: Boolean
    var showPhoneNumbers: Boolean

    private var smallPadding = activity.resources.getDimension(R.dimen.small_margin).toInt()
    private var bigPadding = activity.resources.getDimension(R.dimen.normal_margin).toInt()

    init {
        setupDragListener(true)
        initDrawables()
        showContactThumbnails = config.showContactThumbnails
        showPhoneNumbers = config.showPhoneNumbers
        startNameWithSurname = config.startNameWithSurname
    }

    override fun getActionMenuId() = R.menu.cab

    override fun prepareActionMode(menu: Menu) {
        menu.apply {
            findItem(R.id.cab_edit).isVisible = isOneItemSelected()
            findItem(R.id.cab_remove).isVisible = location == LOCATION_FAVORITES_TAB || location == LOCATION_GROUP_CONTACTS
            findItem(R.id.cab_add_to_favorites).isVisible = location == LOCATION_CONTACTS_TAB
            findItem(R.id.cab_add_to_group).isVisible = location == LOCATION_CONTACTS_TAB || location == LOCATION_FAVORITES_TAB
            findItem(R.id.cab_send_sms_to_contacts).isVisible = location == LOCATION_CONTACTS_TAB || location == LOCATION_FAVORITES_TAB || location == LOCATION_GROUP_CONTACTS
            findItem(R.id.cab_send_email_to_contacts).isVisible = location == LOCATION_CONTACTS_TAB || location == LOCATION_FAVORITES_TAB || location == LOCATION_GROUP_CONTACTS
            findItem(R.id.cab_delete).isVisible = location == LOCATION_CONTACTS_TAB || location == LOCATION_GROUP_CONTACTS
            findItem(R.id.cab_select_all).isVisible = location != LOCATION_DIALPAD
            findItem(R.id.cab_share).isVisible = location != LOCATION_DIALPAD

            if (location == LOCATION_GROUP_CONTACTS) {
                findItem(R.id.cab_remove).title = activity.getString(R.string.remove_from_group)
            }
        }
    }

    override fun prepareItemSelection(viewHolder: ViewHolder) {}

    override fun markViewHolderSelection(select: Boolean, viewHolder: ViewHolder?) {
        viewHolder?.itemView?.contact_frame?.isSelected = select
    }

    override fun actionItemPressed(id: Int) {
        if (selectedPositions.isEmpty()) {
            return
        }

        when (id) {
            R.id.cab_edit -> editContact()
            R.id.cab_select_all -> selectAll()
            R.id.cab_add_to_favorites -> addToFavorites()
            R.id.cab_add_to_group -> addToGroup()
            R.id.cab_share -> shareContacts()
            R.id.cab_send_sms_to_contacts -> sendSMSToContacts()
            R.id.cab_send_email_to_contacts -> sendEmailToContacts()
            R.id.cab_remove -> removeContacts()
            R.id.cab_delete -> askConfirmDelete()
        }
    }

    override fun getSelectableItemCount() = contactItems.size

    override fun getIsItemSelectable(position: Int) = true

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val layout = if (showPhoneNumbers) R.layout.item_contact_with_number else R.layout.item_contact_without_number
        return createViewHolder(layout, parent)
    }

    override fun onBindViewHolder(holder: MyRecyclerViewAdapter.ViewHolder, position: Int) {
        val contact = contactItems[position]
        val view = holder.bindView(contact, true, true) { itemView, layoutPosition ->
            setupView(itemView, contact)
        }
        bindViewHolder(holder, position, view)
    }

    override fun getItemCount() = contactItems.size

    fun initDrawables() {
        contactDrawable = activity.resources.getColoredDrawableWithColor(R.drawable.ic_person, textColor)
    }

    fun updateItems(newItems: ArrayList<Contact>, highlightText: String = "") {
        if (newItems.hashCode() != contactItems.hashCode()) {
            contactItems = newItems.clone() as ArrayList<Contact>
            textToHighlight = highlightText
            notifyDataSetChanged()
            finishActMode()
        } else if (textToHighlight != highlightText) {
            textToHighlight = highlightText
            notifyDataSetChanged()
        }
        fastScroller?.measureRecyclerView()
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
            val contact = contactItems.getOrNull(it)
            if (contact != null) {
                contactsToRemove.add(contact)
            }
        }
        contactItems.removeAll(contactsToRemove)

        ContactsHelper(activity).deleteContacts(contactsToRemove)
        if (contactItems.isEmpty()) {
            refreshListener?.refreshContacts(ALL_TABS_MASK)
            finishActMode()
        } else {
            removeSelectedItems()
            refreshListener?.refreshContacts(CONTACTS_TAB_MASK or FAVORITES_TAB_MASK)
        }
    }

    private fun removeContacts() {
        val contactsToRemove = ArrayList<Contact>()
        selectedPositions.sortedDescending().forEach {
            contactsToRemove.add(contactItems[it])
        }
        contactItems.removeAll(contactsToRemove)

        if (location == LOCATION_FAVORITES_TAB) {
            ContactsHelper(activity).removeFavorites(contactsToRemove)
            if (contactItems.isEmpty()) {
                refreshListener?.refreshContacts(FAVORITES_TAB_MASK)
                finishActMode()
            } else {
                removeSelectedItems()
            }
        } else if (location == LOCATION_GROUP_CONTACTS) {
            removeListener?.removeFromGroup(contactsToRemove)
            removeSelectedItems()
        }
    }

    private fun addToFavorites() {
        ContactsHelper(activity).addFavorites(getSelectedContacts())
        refreshListener?.refreshContacts(FAVORITES_TAB_MASK)
        finishActMode()
    }

    private fun addToGroup() {
        val selectedContacts = getSelectedContacts()
        val NEW_GROUP_ID = -1
        val items = ArrayList<RadioItem>()
        ContactsHelper(activity).getStoredGroups().forEach {
            items.add(RadioItem(it.id.toInt(), it.title))
        }
        items.add(RadioItem(NEW_GROUP_ID, activity.getString(R.string.create_new_group)))

        RadioGroupDialog(activity, items, 0) {
            if (it as Int == NEW_GROUP_ID) {
                CreateNewGroupDialog(activity) {
                    Thread {
                        activity.addContactsToGroup(selectedContacts, it.id)
                        refreshListener?.refreshContacts(GROUPS_TAB_MASK)
                    }.start()
                    finishActMode()
                }
            } else {
                Thread {
                    activity.addContactsToGroup(selectedContacts, it.toLong())
                    refreshListener?.refreshContacts(GROUPS_TAB_MASK)
                }.start()
                finishActMode()
            }
        }
    }

    private fun shareContacts() {
        val contactsIDs = ArrayList<Int>()
        selectedPositions.forEach {
            contactsIDs.add(contactItems[it].id)
        }

        val filtered = contactItems.filter { contactsIDs.contains(it.id) } as ArrayList<Contact>
        activity.shareContacts(filtered)
    }

    private fun sendSMSToContacts() {
        activity.sendSMSToContacts(getSelectedContacts())
    }

    private fun sendEmailToContacts() {
        activity.sendEmailToContacts(getSelectedContacts())
    }

    private fun getSelectedContacts(): ArrayList<Contact> {
        val contacts = ArrayList<Contact>()
        selectedPositions.forEach {
            contacts.add(contactItems[it])
        }
        return contacts
    }

    override fun onViewRecycled(holder: ViewHolder) {
        super.onViewRecycled(holder)
        if (!activity.isActivityDestroyed()) {
            Glide.with(activity).clear(holder.itemView?.contact_tmb!!)
        }
    }

    private fun setupView(view: View, contact: Contact) {
        view.apply {
            val fullName = contact.getFullName()
            contact_name.text = if (textToHighlight.isEmpty()) fullName else fullName.highlightTextPart(textToHighlight, adjustedPrimaryColor)
            contact_name.setTextColor(textColor)
            contact_name.setPadding(if (showContactThumbnails) smallPadding else bigPadding, smallPadding, smallPadding, 0)

            if (contact_number != null) {
                val numberText = contact.phoneNumbers.firstOrNull()?.value ?: ""
                contact_number.text = if (textToHighlight.isEmpty()) numberText else numberText.highlightTextPart(textToHighlight, adjustedPrimaryColor)
                contact_number.setTextColor(textColor)
                contact_number.setPadding(if (showContactThumbnails) smallPadding else bigPadding, 0, smallPadding, 0)
            }

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
