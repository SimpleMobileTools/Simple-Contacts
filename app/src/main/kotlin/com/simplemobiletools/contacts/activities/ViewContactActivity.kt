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
import com.simplemobiletools.contacts.helpers.*
import kotlinx.android.synthetic.main.activity_view_contact.*
import kotlinx.android.synthetic.main.item_event.view.*
import kotlinx.android.synthetic.main.item_view_address.view.*
import kotlinx.android.synthetic.main.item_view_email.view.*
import kotlinx.android.synthetic.main.item_view_group.view.*
import kotlinx.android.synthetic.main.item_view_phone_number.view.*
import kotlinx.android.synthetic.main.item_website.view.*

class ViewContactActivity : ContactActivity() {
    private var isViewIntent = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_view_contact)
        showFields = config.showContactFields
    }

    override fun onResume() {
        super.onResume()
        isViewIntent = intent.action == ContactsContract.QuickContact.ACTION_QUICK_CONTACT || intent.action == Intent.ACTION_VIEW
        if (isViewIntent) {
            handlePermission(PERMISSION_READ_CONTACTS) {
                if (it) {
                    initContact()
                } else {
                    toast(R.string.no_contacts_permission)
                    finish()
                }
            }
        } else {
            initContact()
        }
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

    private fun initContact() {
        var wasLookupKeyUsed = false
        var contactId = intent.getIntExtra(CONTACT_ID, 0)
        if (contactId == 0 && isViewIntent) {
            val data = intent.data
            if (data != null) {
                val rawId = if (data.path.contains("lookup")) {
                    val lookupKey = getLookupKeyFromUri(data)
                    if (lookupKey != null) {
                        contact = ContactsHelper(this).getContactWithLookupKey(lookupKey)
                        wasLookupKeyUsed = true
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

        if (contactId != 0 && !wasLookupKeyUsed) {
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
        contact_numbers_image.applyColorFilter(textColor)
        contact_emails_image.applyColorFilter(textColor)
        contact_events_image.applyColorFilter(textColor)
        contact_source_image.applyColorFilter(textColor)
        contact_notes_image.applyColorFilter(textColor)
        contact_organization_image.applyColorFilter(textColor)
        contact_websites_image.applyColorFilter(textColor)
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
            contact_prefix.text = prefix
            contact_prefix.beVisibleIf(prefix.isNotEmpty() && showFields and SHOW_PREFIX_FIELD != 0)

            contact_first_name.text = firstName
            contact_first_name.beVisibleIf(firstName.isNotEmpty() && showFields and SHOW_FIRST_NAME_FIELD != 0)

            contact_middle_name.text = middleName
            contact_middle_name.beVisibleIf(middleName.isNotEmpty() && showFields and SHOW_MIDDLE_NAME_FIELD != 0)

            contact_surname.text = surname
            contact_surname.beVisibleIf(surname.isNotEmpty() && showFields and SHOW_SURNAME_FIELD != 0)

            contact_suffix.text = suffix
            contact_suffix.beVisibleIf(suffix.isNotEmpty() && showFields and SHOW_SUFFIX_FIELD != 0)

            if (contact_prefix.isGone() && contact_first_name.isGone() && contact_middle_name.isGone() && contact_surname.isGone() && contact_suffix.isGone()) {
                contact_name_image.beInvisible()
                (contact_photo.layoutParams as RelativeLayout.LayoutParams).bottomMargin = resources.getDimension(R.dimen.medium_margin).toInt()
            }

            if (showFields and SHOW_CONTACT_SOURCE_FIELD != 0) {
                contact_source.text = getPublicContactSource(source)
                contact_source_image.beVisible()
                contact_source.beVisible()
            } else {
                contact_source_image.beGone()
                contact_source.beGone()
            }
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
        setupOrganization()
        setupWebsites()
        setupGroups()
    }

    private fun setupPhoneNumbers() {
        contact_numbers_holder.removeAllViews()
        val phoneNumbers = contact!!.phoneNumbers
        if (phoneNumbers.isNotEmpty() && showFields and SHOW_PHONE_NUMBERS_FIELD != 0) {
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
            contact_numbers_image.beVisible()
            contact_numbers_holder.beVisible()
        } else {
            contact_numbers_image.beGone()
            contact_numbers_holder.beGone()
        }
    }

    private fun setupEmails() {
        contact_emails_holder.removeAllViews()
        val emails = contact!!.emails
        if (emails.isNotEmpty() && showFields and SHOW_EMAILS_FIELD != 0) {
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
            contact_emails_image.beVisible()
            contact_emails_holder.beVisible()
        } else {
            contact_emails_image.beGone()
            contact_emails_holder.beGone()
        }
    }

    private fun setupAddresses() {
        contact_addresses_holder.removeAllViews()
        val addresses = contact!!.addresses
        if (addresses.isNotEmpty() && showFields and SHOW_ADDRESSES_FIELD != 0) {
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
            contact_addresses_image.beVisible()
            contact_addresses_holder.beVisible()
        } else {
            contact_addresses_image.beGone()
            contact_addresses_holder.beGone()
        }
    }

    private fun setupEvents() {
        contact_events_holder.removeAllViews()
        val events = contact!!.events
        if (events.isNotEmpty() && showFields and SHOW_EVENTS_FIELD != 0) {
            events.forEach {
                layoutInflater.inflate(R.layout.item_event, contact_events_holder, false).apply {
                    contact_events_holder.addView(this)
                    contact_event.alpha = 1f
                    getDateTime(it.value, contact_event)
                    contact_event_type.setText(getEventTextId(it.type))
                    contact_event_remove.beGone()
                }
            }
            contact_events_image.beVisible()
            contact_events_holder.beVisible()
        } else {
            contact_events_image.beGone()
            contact_events_holder.beGone()
        }
    }

    private fun setupNotes() {
        val notes = contact!!.notes
        if (notes.isNotEmpty() && showFields and SHOW_NOTES_FIELD != 0) {
            contact_notes.text = notes
            contact_notes_image.beVisible()
            contact_notes.beVisible()
        } else {
            contact_notes_image.beGone()
            contact_notes.beGone()
        }
    }

    private fun setupOrganization() {
        val organization = contact!!.organization
        if (!organization.isEmpty() && showFields and SHOW_ORGANIZATION_FIELD != 0) {
            contact_organization_company.text = organization.company
            contact_organization_job_position.text = organization.jobPosition
            contact_organization_image.beGoneIf(organization.isEmpty())
            contact_organization_company.beGoneIf(organization.company.isEmpty())
            contact_organization_job_position.beGoneIf(organization.jobPosition.isEmpty())
        } else {
            contact_organization_image.beGone()
            contact_organization_company.beGone()
            contact_organization_job_position.beGone()
        }
    }

    private fun setupWebsites() {
        contact_websites_holder.removeAllViews()
        val websites = contact!!.websites
        if (websites.isNotEmpty() && showFields and SHOW_WEBSITES_FIELD != 0) {
            websites.forEach {
                val url = it
                layoutInflater.inflate(R.layout.item_website, contact_websites_holder, false).apply {
                    contact_websites_holder.addView(this)
                    contact_website.text = url

                    setOnClickListener {
                        openWebsiteIntent(url)
                    }
                }
            }
            contact_websites_image.beVisible()
            contact_websites_holder.beVisible()
        } else {
            contact_websites_image.beGone()
            contact_websites_holder.beGone()
        }
    }

    private fun setupGroups() {
        contact_groups_holder.removeAllViews()
        val groups = contact!!.groups
        if (groups.isNotEmpty() && showFields and SHOW_GROUPS_FIELD != 0) {
            groups.forEach {
                layoutInflater.inflate(R.layout.item_view_group, contact_groups_holder, false).apply {
                    val group = it
                    contact_groups_holder.addView(this)
                    contact_group.text = group.title
                }
            }
            contact_groups_image.beVisible()
            contact_groups_holder.beVisible()
        } else {
            contact_groups_image.beGone()
            contact_groups_holder.beGone()
        }
    }

    private fun getStarDrawable(on: Boolean) = resources.getDrawable(if (on) R.drawable.ic_star_on_big else R.drawable.ic_star_off_big)
}
