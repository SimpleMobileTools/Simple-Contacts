package com.simplemobiletools.contacts.pro.activities

import android.app.Activity
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import android.os.Bundle
import androidx.coordinatorlayout.widget.CoordinatorLayout
import com.simplemobiletools.commons.extensions.*
import com.simplemobiletools.commons.helpers.NavigationIcon
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

    protected val INTENT_SELECT_RINGTONE = 600

    protected var contact: Contact? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        isMaterialActivity = true
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_group_contacts)
        updateTextColors(group_contacts_coordinator)
        setupOptionsMenu()

        updateMaterialActivityViews(group_contacts_coordinator, group_contacts_list, useTransparentNavigation = true, useTopSearchMenu = false)
        setupMaterialScrollListener(group_contacts_list, group_contacts_toolbar)

        group = intent.extras?.getSerializable(GROUP) as Group
        group_contacts_toolbar.title = group.title

        group_contacts_fab.setOnClickListener {
            if (wasInit) {
                fabClicked()
            }
        }

        group_contacts_placeholder_2.setOnClickListener {
            fabClicked()
        }

        val properPrimaryColor = getProperPrimaryColor()
        group_contacts_fastscroller?.updateColors(properPrimaryColor)
        group_contacts_placeholder_2.underlineText()
        group_contacts_placeholder_2.setTextColor(properPrimaryColor)
    }

    override fun onResume() {
        super.onResume()
        refreshContacts()
        setupToolbar(group_contacts_toolbar, NavigationIcon.Arrow)
        (group_contacts_fab.layoutParams as CoordinatorLayout.LayoutParams).bottomMargin =
            navigationBarHeight + resources.getDimension(R.dimen.activity_margin).toInt()
    }

    private fun setupOptionsMenu() {
        group_contacts_toolbar.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.send_sms_to_group -> sendSMSToGroup()
                R.id.send_email_to_group -> sendEmailToGroup()
                R.id.assign_ringtone_to_group -> assignRingtoneToGroup()
                else -> return@setOnMenuItemClickListener false
            }
            return@setOnMenuItemClickListener true
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, resultData: Intent?) {
        super.onActivityResult(requestCode, resultCode, resultData)
        if (requestCode == INTENT_SELECT_RINGTONE && resultCode == Activity.RESULT_OK && resultData != null) {
            val extras = resultData.extras
            if (extras?.containsKey(RingtoneManager.EXTRA_RINGTONE_PICKED_URI) == true) {
                val uri = extras.getParcelable<Uri>(RingtoneManager.EXTRA_RINGTONE_PICKED_URI) ?: return
                try {
                    setRingtoneOnSelected(uri)
                } catch (e: Exception) {
                    showErrorToast(e)
                }
            }
        }
    }

    private fun fabClicked() {
        SelectContactsDialog(this, allContacts, true, false, groupContacts) { addedContacts, removedContacts ->
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
            group_contacts_fastscroller.beVisibleIf(groupContacts.isNotEmpty())
            updateContacts(groupContacts)
        }
    }

    private fun sendSMSToGroup() {
        if (groupContacts.isEmpty()) {
            toast(R.string.no_contacts_found)
        } else {
            sendSMSToContacts(groupContacts)
        }
    }

    private fun sendEmailToGroup() {
        if (groupContacts.isEmpty()) {
            toast(R.string.no_contacts_found)
        } else {
            sendEmailToContacts(groupContacts)
        }
    }

    private fun assignRingtoneToGroup() {
        val ringtonePickerIntent = getRingtonePickerIntent()
        try {
            startActivityForResult(ringtonePickerIntent, INTENT_SELECT_RINGTONE)
        } catch (e: Exception) {
            toast(e.toString())
        }
    }

    private fun updateContacts(contacts: ArrayList<Contact>) {
        val currAdapter = group_contacts_list.adapter
        if (currAdapter == null) {
            ContactsAdapter(this, contacts, this, LOCATION_GROUP_CONTACTS, this, group_contacts_list) {
                contactClicked(it as Contact)
            }.apply {
                group_contacts_list.adapter = this
            }

            if (areSystemAnimationsEnabled) {
                group_contacts_list.scheduleLayoutAnimation()
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

    private fun getDefaultRingtoneUri() = RingtoneManager.getActualDefaultRingtoneUri(this, RingtoneManager.TYPE_RINGTONE)

    private fun getRingtonePickerIntent(): Intent {
        val defaultRingtoneUri = getDefaultRingtoneUri()

        return Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
            putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, true)
            putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
            putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_RINGTONE)
            putExtra(RingtoneManager.EXTRA_RINGTONE_DEFAULT_URI, defaultRingtoneUri)
            putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, defaultRingtoneUri)
        }
    }

    private fun setRingtoneOnSelected(uri: Uri) {
        groupContacts.forEach {
            ContactsHelper(this).updateRingtone(it.contactId.toString(), uri.toString())
        }
    }
}
