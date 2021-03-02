package com.simplemobiletools.contacts.pro.adapters

import android.graphics.drawable.BitmapDrawable
import android.util.TypedValue
import android.view.Menu
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.signature.ObjectKey
import com.simplemobiletools.commons.adapters.MyRecyclerViewAdapter
import com.simplemobiletools.commons.dialogs.ConfirmationDialog
import com.simplemobiletools.commons.dialogs.RadioGroupDialog
import com.simplemobiletools.commons.extensions.beVisibleIf
import com.simplemobiletools.commons.extensions.getTextSize
import com.simplemobiletools.commons.extensions.highlightTextFromNumbers
import com.simplemobiletools.commons.extensions.highlightTextPart
import com.simplemobiletools.commons.helpers.*
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
import java.util.*

class ContactsAdapter(activity: SimpleActivity, var contactItems: ArrayList<Contact>, private val refreshListener: RefreshContactsListener?,
                      private val location: Int, private val removeListener: RemoveFromGroupListener?, recyclerView: MyRecyclerView,
                      fastScroller: FastScroller?, highlightText: String = "", itemClick: (Any) -> Unit) :
    MyRecyclerViewAdapter(activity, recyclerView, fastScroller, itemClick) {
    private val NEW_GROUP_ID = -1

    private var config = activity.config
    private var textToHighlight = highlightText

    var startNameWithSurname = config.startNameWithSurname
    var showContactThumbnails = config.showContactThumbnails
    var showPhoneNumbers = config.showPhoneNumbers
    var fontSize = activity.getTextSize()

    private val itemLayout = if (showPhoneNumbers) R.layout.item_contact_with_number else R.layout.item_contact_without_number

    init {
        setupDragListener(true)
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

    override fun onActionModeCreated() {}

    override fun onActionModeDestroyed() {}

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = createViewHolder(itemLayout, parent)

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val contact = contactItems[position]
        val allowLongClick = location != LOCATION_INSERT_OR_EDIT
        holder.bindView(contact, true, allowLongClick) { itemView, layoutPosition ->
            setupView(itemView, contact)
        }
        bindViewHolder(holder)
    }

    override fun getItemCount() = contactItems.size

    private fun getItemWithKey(key: Int): Contact? = contactItems.firstOrNull { it.id == key }

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
        val items = if (itemsCnt == 1) {
            "\"${getSelectedItems().first().getNameToDisplay()}\""
        } else {
            resources.getQuantityString(R.plurals.delete_contacts, itemsCnt, itemsCnt)
        }

        val baseString = R.string.deletion_confirmation
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
                        refreshListener?.refreshContacts(TAB_CONTACTS or TAB_FAVORITES)
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
                refreshListener?.refreshContacts(TAB_FAVORITES)
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
        refreshListener?.refreshContacts(TAB_FAVORITES)
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
                        refreshListener?.refreshContacts(TAB_GROUPS)
                    }
                    finishActMode()
                }
            } else {
                ensureBackgroundThread {
                    activity.addContactsToGroup(selectedContacts, it.toLong())
                    refreshListener?.refreshContacts(TAB_GROUPS)
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
            Glide.with(activity).clear(holder.itemView.findViewById<ImageView>(R.id.item_contact_image))
        }
    }

    private fun setupView(view: View, contact: Contact) {
        view.apply {
            findViewById<FrameLayout>(R.id.item_contact_frame)?.isSelected = selectedKeys.contains(contact.id)
            val fullName = contact.getNameToDisplay()
            findViewById<TextView>(R.id.item_contact_name).text = if (textToHighlight.isEmpty()) fullName else {
                if (fullName.contains(textToHighlight, true)) {
                    fullName.highlightTextPart(textToHighlight, adjustedPrimaryColor)
                } else {
                    fullName.highlightTextFromNumbers(textToHighlight, adjustedPrimaryColor)
                }
            }

            findViewById<TextView>(R.id.item_contact_name).apply {
                setTextColor(textColor)
                setTextSize(TypedValue.COMPLEX_UNIT_PX, fontSize)
            }

            if (findViewById<TextView>(R.id.item_contact_number) != null) {
                val phoneNumberToUse = if (textToHighlight.isEmpty()) {
                    contact.phoneNumbers.firstOrNull()
                } else {
                    contact.phoneNumbers.firstOrNull { it.value.contains(textToHighlight) } ?: contact.phoneNumbers.firstOrNull()
                }

                val numberText = phoneNumberToUse?.value ?: ""
                findViewById<TextView>(R.id.item_contact_number).apply {
                    text = if (textToHighlight.isEmpty()) numberText else numberText.highlightTextPart(textToHighlight, adjustedPrimaryColor, false, true)
                    setTextColor(textColor)
                    setTextSize(TypedValue.COMPLEX_UNIT_PX, fontSize)
                }
            }

            findViewById<TextView>(R.id.item_contact_image).beVisibleIf(showContactThumbnails)

            if (showContactThumbnails) {
                val placeholderImage = BitmapDrawable(resources, SimpleContactsHelper(context).getContactLetterIcon(fullName))
                if (contact.photoUri.isEmpty() && contact.photo == null) {
                    findViewById<ImageView>(R.id.item_contact_image).setImageDrawable(placeholderImage)
                } else {
                    val options = RequestOptions()
                        .signature(ObjectKey(contact.getSignatureKey()))
                        .diskCacheStrategy(DiskCacheStrategy.RESOURCE)
                        .error(placeholderImage)
                        .centerCrop()

                    val itemToLoad: Any? = if (contact.photoUri.isNotEmpty()) {
                        contact.photoUri
                    } else {
                        contact.photo
                    }

                    Glide.with(activity)
                        .load(itemToLoad)
                        .apply(options)
                        .apply(RequestOptions.circleCropTransform())
                        .into(findViewById(R.id.item_contact_image))
                }
            }
        }
    }
}
