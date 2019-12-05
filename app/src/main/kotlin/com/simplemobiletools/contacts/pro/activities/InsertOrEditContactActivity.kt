package com.simplemobiletools.contacts.pro.activities

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.provider.ContactsContract
import android.view.Menu
import com.simplemobiletools.commons.extensions.*
import com.simplemobiletools.commons.helpers.PERMISSION_READ_CONTACTS
import com.simplemobiletools.contacts.pro.R
import com.simplemobiletools.contacts.pro.adapters.ContactsAdapter
import com.simplemobiletools.contacts.pro.extensions.config
import com.simplemobiletools.contacts.pro.extensions.getContactPublicUri
import com.simplemobiletools.contacts.pro.helpers.*
import com.simplemobiletools.contacts.pro.models.Contact
import kotlinx.android.synthetic.main.activity_insert_edit_contact.*

class InsertOrEditContactActivity : SimpleActivity() {
    private val START_INSERT_ACTIVITY = 1
    private val START_EDIT_ACTIVITY = 2

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_insert_edit_contact)

        if (checkAppSideloading()) {
            return
        }

        title = getString(R.string.select_contact)
        setupViews()

        handlePermission(PERMISSION_READ_CONTACTS) {
            // we do not really care about the permission request result. Even if it was denied, load private contacts
            ContactsHelper(this).getContacts {
                gotContacts(it)
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        updateMenuItemColors(menu)
        return super.onCreateOptionsMenu(menu)
    }

    private fun setupViews() {
        updateTextColors(insert_edit_contact_holder)
        new_contact_tmb.setImageDrawable(resources.getColoredDrawableWithColor(R.drawable.ic_new_contact_vector, config.textColor))
        new_contact_holder.setOnClickListener {
            val name = intent.getStringExtra(KEY_NAME) ?: ""
            val phoneNumber = getPhoneNumberFromIntent(intent) ?: ""
            val email = getEmailFromIntent(intent) ?: ""

            Intent().apply {
                action = Intent.ACTION_INSERT
                data = ContactsContract.Contacts.CONTENT_URI

                if (phoneNumber.isNotEmpty()) {
                    putExtra(KEY_PHONE, phoneNumber)
                }

                if (name.isNotEmpty()) {
                    putExtra(KEY_NAME, name)
                }

                if (email.isNotEmpty()) {
                    putExtra(KEY_EMAIL, email)
                }

                if (resolveActivity(packageManager) != null) {
                    startActivityForResult(this, START_INSERT_ACTIVITY)
                } else {
                    toast(R.string.no_app_found)
                }
            }
        }

        select_contact_label.setTextColor(getAdjustedPrimaryColor())
    }

    private fun gotContacts(contacts: ArrayList<Contact>) {
        ContactsAdapter(this, contacts, null, LOCATION_INSERT_OR_EDIT, null, select_contact_list, select_contact_fastscroller) {
            val contact = it as Contact
            val phoneNumber = getPhoneNumberFromIntent(intent) ?: ""
            val email = getEmailFromIntent(intent) ?: ""

            Intent(applicationContext, EditContactActivity::class.java).apply {
                data = getContactPublicUri(contact)
                action = ADD_NEW_CONTACT_NUMBER

                if (phoneNumber.isNotEmpty()) {
                    putExtra(KEY_PHONE, phoneNumber)
                }

                if (email.isNotEmpty()) {
                    putExtra(KEY_EMAIL, email)
                }

                putExtra(IS_PRIVATE, contact.isPrivate())
                startActivityForResult(this, START_EDIT_ACTIVITY)
            }
        }.apply {
            select_contact_list.adapter = this
        }

        select_contact_fastscroller.setScrollToY(0)
        select_contact_fastscroller.setViews(select_contact_list) {
            val item = (select_contact_list.adapter as ContactsAdapter).contactItems.getOrNull(it)
            select_contact_fastscroller.updateBubbleText(item?.getBubbleText() ?: "")
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, resultData: Intent?) {
        super.onActivityResult(requestCode, resultCode, resultData)
        if (resultCode == Activity.RESULT_OK) {
            finish()
        }
    }
}
