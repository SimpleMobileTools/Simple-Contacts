package com.simplemobiletools.contacts.activities

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.ContactsContract
import android.view.Menu
import android.view.MenuItem
import com.simplemobiletools.commons.extensions.appLaunched
import com.simplemobiletools.commons.extensions.baseConfig
import com.simplemobiletools.commons.extensions.isActivityDestroyed
import com.simplemobiletools.commons.extensions.toast
import com.simplemobiletools.commons.helpers.PERMISSION_READ_CONTACTS
import com.simplemobiletools.commons.helpers.PERMISSION_WRITE_CONTACTS
import com.simplemobiletools.contacts.R
import com.simplemobiletools.contacts.adapters.SelectContactsAdapter
import com.simplemobiletools.contacts.extensions.config
import com.simplemobiletools.contacts.helpers.ContactsHelper
import com.simplemobiletools.contacts.models.Contact
import kotlinx.android.synthetic.main.layout_select_contact.*

class SelectContactActivity : SimpleActivity() {
    private var skipFavorites = false
    private var allowMultiple = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.layout_select_contact)
        appLaunched()

        handlePermission(PERMISSION_READ_CONTACTS) {
            if (it) {
                handlePermission(PERMISSION_WRITE_CONTACTS) {
                    if (it) {
                        initContacts()
                    } else {
                        toast(R.string.no_contacts_permission)
                        finish()
                    }
                }
            } else {
                toast(R.string.no_contacts_permission)
                finish()
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_select_contact, menu)
        menu.findItem(R.id.confirm).isVisible = allowMultiple
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.confirm -> confirmSelection()
            else -> return super.onOptionsItemSelected(item)
        }
        return true
    }

    private fun initContacts() {
        skipFavorites = intent.getBooleanExtra("extraSkipFavorites", false)
        allowMultiple = intent.getBooleanExtra("isPickMultiContactsWithoutProfile", false)
        if (allowMultiple) {
            title = getString(R.string.select_contacts)
            invalidateOptionsMenu()
        }

        ContactsHelper(this).getContacts {
            var contacts = it
            if (isActivityDestroyed()) {
                return@getContacts
            }

            val contactSources = config.displayContactSources
            if (!config.showAllContacts()) {
                contacts = contacts.filter { contactSources.contains(it.source) } as ArrayList<Contact>
            }

            if (skipFavorites) {
                val favorites = config.favorites
                contacts = contacts.filter { !favorites.contains(it.id.toString()) } as ArrayList<Contact>
            }

            Contact.sorting = config.sorting
            contacts.sort()

            runOnUiThread {
                select_contact_list.adapter = SelectContactsAdapter(this, contacts, HashSet())
                select_contact_fastscroller.allowBubbleDisplay = baseConfig.showInfoBubble
                select_contact_fastscroller.setViews(select_contact_list) {
                    select_contact_fastscroller.updateBubbleText(contacts[it].getBubbleText())
                }
            }
        }
    }

    private fun confirmSelection() {
        val selectedItems = (select_contact_list.adapter as SelectContactsAdapter).getSelectedItemsSet()
        val lookupKey = ContactsHelper(this).getContactLookupKey(selectedItems.first())
        val lookupUri = Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_LOOKUP_URI, lookupKey)

        Intent().apply {
            data = lookupUri
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            setResult(RESULT_OK, this)
        }
        finish()
    }
}
