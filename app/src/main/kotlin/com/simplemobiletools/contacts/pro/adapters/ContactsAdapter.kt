package com.simplemobiletools.contacts.pro.adapters

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
import com.simplemobiletools.commons.extensions.beVisibleIf
import com.simplemobiletools.commons.extensions.getAdjustedPrimaryColor
import com.simplemobiletools.commons.extensions.getColoredDrawableWithColor
import com.simplemobiletools.commons.extensions.highlightTextPart
import com.simplemobiletools.commons.helpers.ensureBackgroundThread
import com.simplemobiletools.commons.models.RadioItem
import com.simplemobiletools.commons.views.FastScroller
import com.simplemobiletools.commons.views.MyRecyclerView
import com.simplemobiletools.contacts.pro.R
import com.simplemobiletools.contacts.pro.activities.SimpleActivity
import com.simplemobiletools.contacts.pro.dialogs.CreateNewGroupDialog
import com.simplemobiletools.contacts.pro.extensions.*
import com.simplemobiletools.contacts.pro.helpers.*
import com.simplemobiletools.contacts.pro.interfaces.RefreshContactsListener
import com.simplemobiletools.contacts.pro.interfaces.RemoveFromGroupListener
import com.simplemobiletools.contacts.pro.models.Contact
import kotlinx.android.synthetic.main.item_contact_with_number.view.*
import java.util.*

class ContactsAdapter(activity: SimpleActivity, var contactItems: ArrayList<Contact>, private val refreshListener: RefreshContactsListener?,
                      private val location: Int, private val removeListener: RemoveFromGroupListener?, recyclerView: MyRecyclerView,
                      fastScroller: FastScroller, highlightText: String = "", itemClick: (Any) -> Unit) :
        MyRecyclerViewAdapter(activity, recyclerView, fastScroller, itemClick) {
    private val NEW_GROUP_ID = -1

    private lateinit var contactDrawable: Drawable
    private lateinit var businessContactDrawable: Drawable
    private var config = activity.config
    private var textToHighlight = highlightText

    var adjustedPrimaryColor = activity.getAdjustedPrimaryColor()
    var startNameWithSurname: Boolean
    var showContactThumbnails: Boolean
    var showPhoneNumbers: Boolean

    private var smallPadding = activity.resources.getDimension(R.dimen.small_margin).toInt()
    private var mediumPadding = activity.resources.getDimension(R.dimen.medium_margin).toInt()
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

    override fun actionItemPressed(id: Int) {
        if (selectedKeys.isEmpty()) {
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

    override fun getItemSelectionKey(position: Int) = contactItems.getOrNull(position)?.id

    override fun getItemKeyPosition(key: Int) = contactItems.indexOfFirst { it.id == key }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val layout = if (showPhoneNumbers) R.layout.item_contact_with_number else R.layout.item_contact_without_number
        return createViewHolder(layout, parent)
    }

    override fun onBindViewHolder(holder: MyRecyclerViewAdapter.ViewHolder, position: Int) {
        val contact = contactItems[position]
        val allowLongClick = location != LOCATION_INSERT_OR_EDIT
        holder.bindView(contact, true, allowLongClick) { itemView, layoutPosition ->
            setupView(itemView, contact)
        }
        bindViewHolder(holder)
    }

    override fun getItemCount() = contactItems.size

    private fun getItemWithKey(key: Int): Contact? = contactItems.firstOrNull { it.id == key }

    fun initDrawables() {
        contactDrawable = activity.resources.getColoredDrawableWithColor(R.drawable.ic_person_vector, textColor)
        businessContactDrawable = activity.resources.getColoredDrawableWithColor(R.drawable.ic_business_vector, textColor)
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
        val contact = getItemWithKey(selectedKeys.first()) ?: return
        activity.editContact(contact)
    }

    private fun askConfirmDelete() {
        val itemsCnt = selectedKeys.size
        val firstItem = getSelectedItems().first()
        val items = if (itemsCnt == 1) {
            "\"${firstItem.getNameToDisplay()}\""
        } else {
            resources.getQuantityString(R.plurals.delete_contacts, itemsCnt, itemsCnt)
        }

        val baseString = R.string.delete_contacts_confirmation
        val question = String.format(resources.getString(baseString), items)

        ConfirmationDialog(activity, question) {
            deleteContacts()
        }
    }

    private fun deleteContacts() {
        if (selectedKeys.isEmpty()) {
            return
        }

        val contactsToRemove = getSelectedItems()
        val positions = getSelectedItemPositions()
        contactItems.removeAll(contactsToRemove)

        ContactsHelper(activity).getContacts(true) { allContacts ->
            ensureBackgroundThread {
                contactsToRemove.forEach {
                    val contactToRemove = it
                    val duplicates = allContacts.filter { it.id != contactToRemove.id && it.getHashToCompare() == contactToRemove.getHashToCompare() }.toMutableList() as ArrayList<Contact>
                    duplicates.add(contactToRemove)
                    ContactsHelper(activity).deleteContacts(duplicates)
                }

                activity.runOnUiThread {
                    if (contactItems.isEmpty()) {
                        refreshListener?.refreshContacts(ALL_TABS_MASK)
                        finishActMode()
                    } else {
                        removeSelectedItems(positions)
                        refreshListener?.refreshContacts(CONTACTS_TAB_MASK or FAVORITES_TAB_MASK)
                    }
                }
            }
        }
    }

    // used for removing contacts from groups or favorites, not deleting actual contacts
    private fun removeContacts() {
        val contactsToRemove = getSelectedItems()
        val positions = getSelectedItemPositions()
        contactItems.removeAll(contactsToRemove)

        if (location == LOCATION_FAVORITES_TAB) {
            ContactsHelper(activity).removeFavorites(contactsToRemove)
            if (contactItems.isEmpty()) {
                refreshListener?.refreshContacts(FAVORITES_TAB_MASK)
                finishActMode()
            } else {
                removeSelectedItems(positions)
            }
        } else if (location == LOCATION_GROUP_CONTACTS) {
            removeListener?.removeFromGroup(contactsToRemove)
            removeSelectedItems(positions)
        }
    }

    private fun addToFavorites() {
        ContactsHelper(activity).addFavorites(getSelectedItems())
        refreshListener?.refreshContacts(FAVORITES_TAB_MASK)
        finishActMode()
    }

    private fun addToGroup() {
        val items = ArrayList<RadioItem>()
        ContactsHelper(activity).getStoredGroups {
            it.forEach {
                items.add(RadioItem(it.id!!.toInt(), it.title))
            }
            items.add(RadioItem(NEW_GROUP_ID, activity.getString(R.string.create_new_group)))
            showGroupsPicker(items)
        }
    }

    private fun showGroupsPicker(radioItems: ArrayList<RadioItem>) {
        val selectedContacts = getSelectedItems()
        RadioGroupDialog(activity, radioItems, 0) {
            if (it as Int == NEW_GROUP_ID) {
                CreateNewGroupDialog(activity) {
                    ensureBackgroundThread {
                        activity.addContactsToGroup(selectedContacts, it.id!!.toLong())
                        refreshListener?.refreshContacts(GROUPS_TAB_MASK)
                    }
                    finishActMode()
                }
            } else {
                ensureBackgroundThread {
                    activity.addContactsToGroup(selectedContacts, it.toLong())
                    refreshListener?.refreshContacts(GROUPS_TAB_MASK)
                }
                finishActMode()
            }
        }
    }

    private fun shareContacts() {
        activity.shareContacts(getSelectedItems())
    }

    private fun sendSMSToContacts() {
        activity.sendSMSToContacts(getSelectedItems())
    }

    private fun sendEmailToContacts() {
        activity.sendEmailToContacts(getSelectedItems())
    }

    private fun getSelectedItems() = contactItems.filter { selectedKeys.contains(it.id) } as ArrayList<Contact>

    override fun onViewRecycled(holder: ViewHolder) {
        super.onViewRecycled(holder)
        if (!activity.isDestroyed && !activity.isFinishing) {
            Glide.with(activity).clear(holder.itemView.contact_tmb)
        }
    }

    private fun setupView(view: View, contact: Contact) {
        view.apply {
            contact_frame?.isSelected = selectedKeys.contains(contact.id)
            val fullName = contact.getNameToDisplay()
            contact_name.text = if (textToHighlight.isEmpty()) fullName else {
                if (fullName.contains(textToHighlight, true)) {
                    fullName.highlightTextPart(textToHighlight, adjustedPrimaryColor)
                } else {
                    highlightTextFromNumbers(fullName, textToHighlight, adjustedPrimaryColor)
                }
            }

            contact_name.setTextColor(textColor)
            contact_name.setPadding(if (showContactThumbnails) smallPadding else bigPadding, smallPadding, smallPadding, 0)

            if (contact_number != null) {
                val phoneNumberToUse = if (textToHighlight.isEmpty()) {
                    contact.phoneNumbers.firstOrNull()
                } else {
                    contact.phoneNumbers.firstOrNull { it.value.contains(textToHighlight) } ?: contact.phoneNumbers.firstOrNull()
                }

                val numberText = phoneNumberToUse?.value ?: ""
                contact_number.text = if (textToHighlight.isEmpty()) numberText else numberText.highlightTextPart(textToHighlight, adjustedPrimaryColor, false, true)
                contact_number.setTextColor(textColor)
                contact_number.setPadding(if (showContactThumbnails) smallPadding else bigPadding, 0, smallPadding, 0)
            }

            contact_tmb.beVisibleIf(showContactThumbnails)

            if (showContactThumbnails) {
                val placeholderImage = if (contact.isABusinessContact()) businessContactDrawable else contactDrawable
                when {
                    contact.photoUri.isNotEmpty() -> {
                        val options = RequestOptions()
                                .signature(ObjectKey(contact.photoUri))
                                .diskCacheStrategy(DiskCacheStrategy.RESOURCE)
                                .error(placeholderImage)
                                .centerCrop()

                        Glide.with(activity)
                                .load(contact.photoUri)
                                .transition(DrawableTransitionOptions.withCrossFade())
                                .apply(options)
                                .apply(RequestOptions.circleCropTransform())
                                .into(contact_tmb)
                        contact_tmb.setPadding(smallPadding, smallPadding, smallPadding, smallPadding)
                    }
                    contact.photo != null -> {
                        val options = RequestOptions()
                                .signature(ObjectKey(contact.photo!!))
                                .diskCacheStrategy(DiskCacheStrategy.RESOURCE)
                                .error(placeholderImage)
                                .centerCrop()

                        Glide.with(activity)
                                .load(contact.photo)
                                .transition(DrawableTransitionOptions.withCrossFade())
                                .apply(options)
                                .apply(RequestOptions.circleCropTransform())
                                .into(contact_tmb)
                        contact_tmb.setPadding(smallPadding, smallPadding, smallPadding, smallPadding)
                    }
                    else -> {
                        contact_tmb.setPadding(mediumPadding, mediumPadding, mediumPadding, mediumPadding)
                        contact_tmb.setImageDrawable(placeholderImage)
                    }
                }
            }
        }
    }
}
