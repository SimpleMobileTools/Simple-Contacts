package com.simplemobiletools.contacts.pro.activities

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import com.simplemobiletools.commons.extensions.beVisibleIf
import com.simplemobiletools.commons.extensions.getAdjustedPrimaryColor
import com.simplemobiletools.commons.extensions.underlineText
import com.simplemobiletools.commons.extensions.updateTextColors
import com.simplemobiletools.commons.helpers.ensureBackgroundThread
import com.simplemobiletools.contacts.pro.R
import com.simplemobiletools.contacts.pro.adapters.ContactsAdapter
import com.simplemobiletools.contacts.pro.dialogs.SelectContactsDialog
import com.simplemobiletools.contacts.pro.extensions.*
import com.simplemobiletools.contacts.pro.helpers.ContactsHelper
import com.simplemobiletools.contacts.pro.helpers.GROUP
import com.simplemobiletools.contacts.pro.helpers.LOCATION_GROUP_CONTACTS
import com.simplemobiletools.contacts.pro.interfaces.RefreshContactsListener
import com.simplemobiletools.contacts.pro.interfaces.RemoveFromGroupListener
import com.simplemobiletools.contacts.pro.models.Contact
import com.simplemobiletools.contacts.pro.models.Group
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

        group = intent.extras?.getSerializable(GROUP) as Group
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

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_group, menu)
        updateMenuItemColors(menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.send_sms_to_group -> sendSMSToGroup()
            R.id.send_email_to_group -> sendEmailToGroup()
            else -> return super.onOptionsItemSelected(item)
        }
        return true
    }

    private fun fabClicked() {
        SelectContactsDialog(this, allContacts, groupContacts) { addedContacts, removedContacts ->
            ensureBackgroundThread {
                addContactsToGroup(addedContacts, group.id!!)
                removeContactsFromGroup(removedContacts, group.id!!)
                refreshContacts()
            }
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
            updateContacts(groupContacts)
        }
    }

    private fun sendSMSToGroup() {
        sendSMSToContacts(groupContacts)
    }

    private fun sendEmailToGroup() {
        sendEmailToContacts(groupContacts)
    }

    private fun updateContacts(contacts: ArrayList<Contact>) {
        val currAdapter = group_contacts_list.adapter
        if (currAdapter == null) {
            ContactsAdapter(this, contacts, this, LOCATION_GROUP_CONTACTS, this, group_contacts_list, group_contacts_fastscroller) {
                contactClicked(it as Contact)
            }.apply {
                group_contacts_list.adapter = this
            }

            group_contacts_fastscroller.setScrollToY(0)
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

    override fun contactClicked(contact: Contact) {
        handleGenericContactClick(contact)
    }

    override fun removeFromGroup(contacts: ArrayList<Contact>) {
        ensureBackgroundThread {
            removeContactsFromGroup(contacts, group.id!!)
            if (groupContacts.size == contacts.size) {
                refreshContacts()
            }
        }
    }
}
