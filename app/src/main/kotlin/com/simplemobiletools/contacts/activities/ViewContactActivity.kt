package com.simplemobiletools.contacts.activities

import android.content.Intent
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.provider.ContactsContract
import android.view.Menu
import android.view.MenuItem
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.RelativeLayout
import android.widget.TextView
import com.simplemobiletools.commons.extensions.*
import com.simplemobiletools.commons.helpers.PERMISSION_READ_CONTACTS
import com.simplemobiletools.contacts.R
import com.simplemobiletools.contacts.extensions.*
import com.simplemobiletools.contacts.helpers.*
import kotlinx.android.synthetic.main.activity_view_contact.*
import kotlinx.android.synthetic.main.item_event.view.*
import kotlinx.android.synthetic.main.item_view_email.view.*
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
                    getLookupUriRawId(data)
                } else {
                    getContactUriRawId(data)
                }

                if (rawId != -1) {
                    contactId = rawId
                }
            }
        }

        if (contactId != 0) {
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

        setupEditContact()

        setupTypePickers()
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

        contact_send_sms.setOnClickListener { trySendSMS() }
        contact_start_call.setOnClickListener { tryStartCall(contact!!) }
        contact_send_email.setOnClickListener { trySendEmail() }

        updateTextColors(contact_scrollview)
        invalidateOptionsMenu()
    }

    private fun setupEditContact() {
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
        setupEvents()
    }

    private fun setupPhoneNumbers() {
        contact_numbers_holder.removeAllViews()
        val phoneNumbers = contact!!.phoneNumbers
        phoneNumbers.forEach {
            layoutInflater.inflate(R.layout.item_view_phone_number, contact_numbers_holder, false).apply {
                contact_numbers_holder.addView(this)
                contact_number.text = it.value
                setupPhoneNumberTypePicker(contact_number_type, it.type)
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
                contact_emails_holder.addView(this)
                contact_email.text = it.value
                setupEmailTypePicker(contact_email_type, it.type)
            }
        }

        contact_email_image.beVisibleIf(emails.isNotEmpty())
        contact_emails_holder.beVisibleIf(emails.isNotEmpty())
    }

    private fun setupEvents() {
        contact_events_holder.removeAllViews()
        val events = contact!!.events
        events.forEach {
            layoutInflater.inflate(R.layout.item_event, contact_events_holder, false).apply {
                contact_events_holder.addView(this)
                contact_event.alpha = 1f
                getDateTime(it.value, contact_event)
                setupEventTypePicker(this as ViewGroup, it.type)
                contact_event_remove.beGone()
            }
        }

        contact_event_image.beVisibleIf(events.isNotEmpty())
        contact_events_holder.beVisibleIf(events.isNotEmpty())
    }

    private fun setupTypePickers() {
        if (contact!!.phoneNumbers.isEmpty()) {
            val numberHolder = contact_numbers_holder.getChildAt(0)
            (numberHolder as? ViewGroup)?.contact_number_type?.apply {
                setupPhoneNumberTypePicker(this)
            }
        }

        if (contact!!.emails.isEmpty()) {
            val emailHolder = contact_emails_holder.getChildAt(0)
            (emailHolder as? ViewGroup)?.contact_email_type?.apply {
                setupEmailTypePicker(this)
            }
        }

        if (contact!!.events.isEmpty()) {
            val eventHolder = contact_events_holder.getChildAt(0)
            (eventHolder as? ViewGroup)?.apply {
                setupEventTypePicker(this)
            }
        }
    }

    private fun setupPhoneNumberTypePicker(numberTypeField: TextView, type: Int = DEFAULT_PHONE_NUMBER_TYPE) {
        numberTypeField.setText(getPhoneNumberTextId(type))
    }

    private fun setupEmailTypePicker(emailTypeField: TextView, type: Int = DEFAULT_EMAIL_TYPE) {
        emailTypeField.setText(getEmailTextId(type))
    }

    private fun setupEventTypePicker(eventHolder: ViewGroup, type: Int = DEFAULT_EVENT_TYPE) {
        eventHolder.contact_event_type.setText(getEventTextId(type))
    }

    private fun getStarDrawable(on: Boolean) = resources.getDrawable(if (on) R.drawable.ic_star_on_big else R.drawable.ic_star_off_big)
}
