package com.simplemobiletools.contacts.activities

import android.os.Bundle
import com.simplemobiletools.commons.extensions.toast
import com.simplemobiletools.commons.extensions.updateTextColors
import com.simplemobiletools.contacts.R
import com.simplemobiletools.contacts.adapters.ContactsAdapter
import com.simplemobiletools.contacts.dialogs.SelectContactsDialog
import com.simplemobiletools.contacts.extensions.config
import com.simplemobiletools.contacts.extensions.editContact
import com.simplemobiletools.contacts.extensions.tryStartCall
import com.simplemobiletools.contacts.extensions.viewContact
import com.simplemobiletools.contacts.helpers.*
import com.simplemobiletools.contacts.models.Contact
import com.simplemobiletools.contacts.models.Group
import kotlinx.android.synthetic.main.activity_group_contacts.*

class GroupContactsActivity : SimpleActivity() {
    private var allContacts = ArrayList<Contact>()
    private var groupContacts = ArrayList<Contact>()
    lateinit var group: Group

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_group_contacts)
        updateTextColors(group_contacts_coordinator)

        group = intent.extras.getSerializable(GROUP) as Group
        supportActionBar?.title = group.title

        refreshContacts()
        group_contacts_fab.setOnClickListener {
            SelectContactsDialog(this, allContacts, groupContacts) { addedContacts, removedContacts ->
                refreshContacts()
            }
        }
    }

    private fun refreshContacts() {
        ContactsHelper(this).getContacts {
            allContacts = it
            groupContacts = it.filter { it.groups.map { it.id }.contains(group.id) } as ArrayList<Contact>

            Contact.sorting = config.sorting
            groupContacts.sort()

            updateContacts(groupContacts)
        }
    }

    private fun updateContacts(contacts: ArrayList<Contact>) {
        ContactsAdapter(this, contacts, null, LOCATION_GROUP_CONTACTS, group_contacts_list, group_contacts_fastscroller) {
            when (config.onContactClick) {
                ON_CLICK_CALL_CONTACT -> {
                    val contact = it as Contact
                    if (contact.phoneNumbers.isNotEmpty()) {
                        tryStartCall(it)
                    } else {
                        toast(R.string.no_phone_number_found)
                    }
                }
                ON_CLICK_VIEW_CONTACT -> viewContact(it as Contact)
                ON_CLICK_EDIT_CONTACT -> editContact(it as Contact)
            }
        }.apply {
            setupDragListener(true)
            addVerticalDividers(true)
            group_contacts_list.adapter = this
        }

        group_contacts_fastscroller.setScrollTo(0)
        group_contacts_fastscroller.setViews(group_contacts_list) {
            val item = (group_contacts_list.adapter as ContactsAdapter).contactItems.getOrNull(it)
            group_contacts_fastscroller.updateBubbleText(item?.getBubbleText() ?: "")
        }
    }
}
