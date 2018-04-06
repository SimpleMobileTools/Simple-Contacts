package com.simplemobiletools.contacts.activities

import android.os.Bundle
import com.simplemobiletools.commons.extensions.*
import com.simplemobiletools.contacts.R
import com.simplemobiletools.contacts.adapters.ContactsAdapter
import com.simplemobiletools.contacts.dialogs.SelectContactsDialog
import com.simplemobiletools.contacts.extensions.*
import com.simplemobiletools.contacts.helpers.*
import com.simplemobiletools.contacts.interfaces.RefreshContactsListener
import com.simplemobiletools.contacts.interfaces.RemoveFromGroupListener
import com.simplemobiletools.contacts.models.Contact
import com.simplemobiletools.contacts.models.Group
import kotlinx.android.synthetic.main.activity_group_contacts.*

class GroupContactsActivity : SimpleActivity(), RemoveFromGroupListener, RefreshContactsListener {
    private var allContacts = ArrayList<Contact>()
    private var groupContacts = ArrayList<Contact>()
    private var wasInit = false
    lateinit var group: Group

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_group_contacts)
        updateTextColors(group_contacts_coordinator)

        group = intent.extras.getSerializable(GROUP) as Group
        supportActionBar?.title = group.title

        group_contacts_fab.setOnClickListener {
            if (wasInit) {
                fabClicked()
            }
        }

        group_contacts_placeholder_2.setOnClickListener {
            fabClicked()
        }

        group_contacts_placeholder_2.underlineText()
        group_contacts_placeholder_2.setTextColor(getAdjustedPrimaryColor())
    }

    override fun onResume() {
        super.onResume()
        refreshContacts()
    }

    private fun fabClicked() {
        SelectContactsDialog(this, allContacts, groupContacts) { addedContacts, removedContacts ->
            Thread {
                addContactsToGroup(addedContacts, group.id)
                removeContactsFromGroup(removedContacts, group.id)
                refreshContacts()
            }.start()
        }
    }

    private fun refreshContacts() {
        ContactsHelper(this).getContacts {
            wasInit = true
            allContacts = it

            groupContacts = it.filter { it.groups.map { it.id }.contains(group.id) } as ArrayList<Contact>
            group_contacts_placeholder_2.beVisibleIf(groupContacts.isEmpty())
            group_contacts_placeholder.beVisibleIf(groupContacts.isEmpty())
            group_contacts_list.beVisibleIf(groupContacts.isNotEmpty())

            Contact.sorting = config.sorting
            groupContacts.sort()

            updateContacts(groupContacts)
        }
    }

    private fun updateContacts(contacts: ArrayList<Contact>) {
        val currAdapter = group_contacts_list.adapter
        if (currAdapter == null) {
            ContactsAdapter(this, contacts, this, LOCATION_GROUP_CONTACTS, this, group_contacts_list, group_contacts_fastscroller) {
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
                addVerticalDividers(true)
                group_contacts_list.adapter = this
            }

            group_contacts_fastscroller.setScrollTo(0)
            group_contacts_fastscroller.setViews(group_contacts_list) {
                val item = (group_contacts_list.adapter as ContactsAdapter).contactItems.getOrNull(it)
                group_contacts_fastscroller.updateBubbleText(item?.getBubbleText() ?: "")
            }
        } else {
            (currAdapter as ContactsAdapter).updateItems(contacts)
        }
    }

    override fun refreshContacts(refreshTabsMask: Int) {
        refreshContacts()
    }

    override fun removeFromGroup(contacts: ArrayList<Contact>) {
        removeContactsFromGroup(contacts, group.id)
        if (groupContacts.size == 0) {
            refreshContacts()
        }
    }
}
