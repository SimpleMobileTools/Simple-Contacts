package com.simplemobiletools.contacts.activities

import android.content.Intent
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.provider.ContactsContract
import android.view.Menu
import android.view.MenuItem
import android.view.WindowManager
import android.widget.RelativeLayout
import com.simplemobiletools.commons.extensions.*
import com.simplemobiletools.commons.helpers.PERMISSION_READ_CONTACTS
import com.simplemobiletools.contacts.R
import com.simplemobiletools.contacts.extensions.*
import com.simplemobiletools.contacts.helpers.CONTACT_ID
import com.simplemobiletools.contacts.helpers.ContactsHelper
import com.simplemobiletools.contacts.helpers.IS_PRIVATE
import kotlinx.android.synthetic.main.activity_view_contact.*
import kotlinx.android.synthetic.main.item_event.view.*
import kotlinx.android.synthetic.main.item_view_address.view.*
import kotlinx.android.synthetic.main.item_view_email.view.*
import kotlinx.android.synthetic.main.item_view_group.view.*
import kotlinx.android.synthetic.main.item_view_phone_number.view.*

class ViewContactActivity : ContactActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_view_contact)
    }

    override fun onResume() {
        super.onResume()
        tryInitContact()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_view_contact, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.edit -> editContact(contact!!)
            R.id.share -> shareContact()
            R.id.delete -> deleteContact()
            else -> return super.onOptionsItemSelected(item)
        }
        return true
    }

    private fun tryInitContact() {
        handlePermission(PERMISSION_READ_CONTACTS) {
            if (it) {
                initContact()
            } else {
                toast(R.string.no_contacts_permission)
                finish()
            }
        }
    }

    private fun initContact() {
        var contactId = intent.getIntExtra(CONTACT_ID, 0)
        val action = intent.action
        if (contactId == 0 && (action == ContactsContract.QuickContact.ACTION_QUICK_CONTACT || action == Intent.ACTION_VIEW)) {
            val data = intent.data
            if (data != null) {
                val rawId = if (data.path.contains("lookup")) {
                    val lookupKey = getLookupKeyFromUri(data)
                    if (lookupKey != null) {
                        contact = ContactsHelper(this).getContactWithLookupKey(lookupKey)
                    }

                    getLookupUriRawId(data)
                } else {
                    getContactUriRawId(data)
                }

                if (rawId != -1) {
                    contactId = rawId
                }
            }
        }

        if (contactId != 0 && contact == null) {
            contact = ContactsHelper(this).getContactWithId(contactId, intent.getBooleanExtra(IS_PRIVATE, false))
            if (contact == null) {
                toast(R.string.unknown_error_occurred)
                finish()
                return
            }
        }

        if (contact == null) {
            finish()
            return
        }

        setupViewContact()

        contact_send_sms.beVisibleIf(contact!!.phoneNumbers.isNotEmpty())
        contact_start_call.beVisibleIf(contact!!.phoneNumbers.isNotEmpty())
        contact_send_email.beVisibleIf(contact!!.emails.isNotEmpty())

        contact_photo.background = ColorDrawable(config.primaryColor)

        if (contact!!.photoUri.isEmpty() && contact!!.photo == null) {
            showPhotoPlaceholder(contact_photo)
        } else {
            updateContactPhoto(contact!!.photoUri, contact_photo, contact!!.photo)
        }

        val textColor = config.textColor
        contact_send_sms.applyColorFilter(textColor)
        contact_start_call.applyColorFilter(textColor)
        contact_send_email.applyColorFilter(textColor)
        contact_name_image.applyColorFilter(textColor)
        contact_number_image.applyColorFilter(textColor)
        contact_email_image.applyColorFilter(textColor)
        contact_event_image.applyColorFilter(textColor)
        contact_source_image.applyColorFilter(textColor)
        contact_notes_image.applyColorFilter(textColor)
        contact_groups_image.applyColorFilter(textColor)

        contact_send_sms.setOnClickListener { trySendSMS() }
        contact_start_call.setOnClickListener { tryStartCall(contact!!) }
        contact_send_email.setOnClickListener { trySendEmail() }

        updateTextColors(contact_scrollview)
        invalidateOptionsMenu()
    }

    private fun setupViewContact() {
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN)
        contact!!.apply {
            contact_first_name.text = firstName
            contact_first_name.beVisibleIf(firstName.isNotEmpty())

            contact_middle_name.text = middleName
            contact_middle_name.beVisibleIf(middleName.isNotEmpty())

            contact_surname.text = surname
            contact_surname.beVisibleIf(surname.isNotEmpty())

            if (firstName.isEmpty() && middleName.isEmpty() && surname.isEmpty()) {
                contact_name_image.beInvisible()
                (contact_photo.layoutParams as RelativeLayout.LayoutParams).bottomMargin = resources.getDimension(R.dimen.medium_margin).toInt()
            }
            contact_source.text = getPublicContactSource(source)
        }

        contact_toggle_favorite.apply {
            beVisible()
            setImageDrawable(getStarDrawable(contact!!.starred == 1))
            tag = contact!!.starred
            applyColorFilter(config.textColor)
        }

        setupPhoneNumbers()
        setupEmails()
        setupAddresses()
        setupEvents()
        setupNotes()
        setupGroups()
    }

    private fun setupPhoneNumbers() {
        contact_numbers_holder.removeAllViews()
        val phoneNumbers = contact!!.phoneNumbers
        phoneNumbers.forEach {
            layoutInflater.inflate(R.layout.item_view_phone_number, contact_numbers_holder, false).apply {
                val phoneNumber = it
                contact_numbers_holder.addView(this)
                contact_number.text = phoneNumber.value
                contact_number_type.setText(getPhoneNumberTextId(phoneNumber.type))

                setOnClickListener {
                    startCallIntent(phoneNumber.value)
                }
            }
        }

        contact_number_image.beVisibleIf(phoneNumbers.isNotEmpty())
        contact_numbers_holder.beVisibleIf(phoneNumbers.isNotEmpty())
    }

    private fun setupEmails() {
        contact_emails_holder.removeAllViews()
        val emails = contact!!.emails
        emails.forEach {
            layoutInflater.inflate(R.layout.item_view_email, contact_emails_holder, false).apply {
                val email = it
                contact_emails_holder.addView(this)
                contact_email.text = email.value
                contact_email_type.setText(getEmailTextId(email.type))

                setOnClickListener {
                    sendEmailIntent(email.value)
                }
            }
        }

        contact_email_image.beVisibleIf(emails.isNotEmpty())
        contact_emails_holder.beVisibleIf(emails.isNotEmpty())
    }

    private fun setupAddresses() {
        contact_addresses_holder.removeAllViews()
        val addresses = contact!!.addresses
        addresses.forEach {
            layoutInflater.inflate(R.layout.item_view_address, contact_addresses_holder, false).apply {
                val address = it
                contact_addresses_holder.addView(this)
                contact_address.text = address.value
                contact_address_type.setText(getAddressTextId(address.type))

                setOnClickListener {
                    sendAddressIntent(address.value)
                }
            }
        }

        contact_address_image.beVisibleIf(addresses.isNotEmpty())
        contact_addresses_holder.beVisibleIf(addresses.isNotEmpty())
    }

    private fun setupEvents() {
        contact_events_holder.removeAllViews()
        val events = contact!!.events
        events.forEach {
            layoutInflater.inflate(R.layout.item_event, contact_events_holder, false).apply {
                contact_events_holder.addView(this)
                contact_event.alpha = 1f
                getDateTime(it.value, contact_event)
                contact_event_type.setText(getEventTextId(it.type))
                contact_event_remove.beGone()
            }
        }

        contact_event_image.beVisibleIf(events.isNotEmpty())
        contact_events_holder.beVisibleIf(events.isNotEmpty())
    }

    private fun setupNotes() {
        val notes = contact!!.notes
        contact_notes.text = notes
        contact_notes_image.beVisibleIf(notes.isNotEmpty())
        contact_notes.beVisibleIf(notes.isNotEmpty())
    }

    private fun setupGroups() {
        contact_groups_holder.removeAllViews()
        val groups = contact!!.groups
        groups.forEach {
            layoutInflater.inflate(R.layout.item_view_group, contact_groups_holder, false).apply {
                val group = it
                contact_groups_holder.addView(this)
                contact_group.text = group.title
            }
        }

        contact_groups_image.beVisibleIf(groups.isNotEmpty())
        contact_groups_holder.beVisibleIf(groups.isNotEmpty())
    }

    private fun getStarDrawable(on: Boolean) = resources.getDrawable(if (on) R.drawable.ic_star_on_big else R.drawable.ic_star_off_big)
}
