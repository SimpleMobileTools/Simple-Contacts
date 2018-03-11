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
import com.simplemobiletools.contacts.dialogs.ChangeSortingDialog
import com.simplemobiletools.contacts.dialogs.FilterContactSourcesDialog
import com.simplemobiletools.contacts.extensions.config
import com.simplemobiletools.contacts.helpers.ContactsHelper
import com.simplemobiletools.contacts.helpers.SMT_PRIVATE
import com.simplemobiletools.contacts.models.Contact
import kotlinx.android.synthetic.main.layout_select_contact.*

class SelectContactActivity : SimpleActivity() {
    private var isGetEmailIntent = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.layout_select_contact)
        appLaunched()

        handlePermission(PERMISSION_READ_CONTACTS) {
            if (it) {
                handlePermission(PERMISSION_WRITE_CONTACTS) {
                    if (it) {
                        isGetEmailIntent = intent.data == ContactsContract.CommonDataKinds.Email.CONTENT_URI
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
        menuInflater.inflate(R.menu.menu_select_activity, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.sort -> showSortingDialog()
            R.id.filter -> showFilterDialog()
            else -> return super.onOptionsItemSelected(item)
        }
        return true
    }

    private fun showSortingDialog() {
        ChangeSortingDialog(this) {
            initContacts()
        }
    }

    private fun showFilterDialog() {
        FilterContactSourcesDialog(this) {
            initContacts()
        }
    }

    private fun initContacts() {
        ContactsHelper(this).getContacts {
            if (isActivityDestroyed()) {
                return@getContacts
            }

            var contacts = it.filter {
                if (isGetEmailIntent) {
                    (it.source != SMT_PRIVATE && it.emails.isNotEmpty())
                } else {
                    true
                }
            } as ArrayList<Contact>

            val contactSources = config.displayContactSources
            if (!config.showAllContacts()) {
                contacts = contacts.filter { contactSources.contains(it.source) } as ArrayList<Contact>
            }

            Contact.sorting = config.sorting
            contacts.sort()

            runOnUiThread {
                select_contact_list.adapter = SelectContactsAdapter(this, contacts, ArrayList(), false) {
                    confirmSelection(it)
                }

                select_contact_fastscroller.allowBubbleDisplay = baseConfig.showInfoBubble
                select_contact_fastscroller.setViews(select_contact_list) {
                    select_contact_fastscroller.updateBubbleText(contacts[it].getBubbleText())
                }
            }
        }
    }

    private fun confirmSelection(contact: Contact) {
        Intent().apply {
            data = getResultUri(contact)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            setResult(RESULT_OK, this)
        }
        finish()
    }

    private fun getResultUri(contact: Contact): Uri {
        return if (isGetEmailIntent) {
            val emailID = ContactsHelper(this).getContactDataId(contact.id.toString())
            Uri.withAppendedPath(ContactsContract.Data.CONTENT_URI, emailID)
        } else {
            val lookupKey = ContactsHelper(this).getContactLookupKey(contact.id.toString())
            Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_LOOKUP_URI, lookupKey)
        }
    }
}
