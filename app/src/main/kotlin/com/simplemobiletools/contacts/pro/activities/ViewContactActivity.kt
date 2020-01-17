package com.simplemobiletools.contacts.pro.activities

import android.content.Intent
import android.os.Bundle
import android.provider.ContactsContract
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.WindowManager
import android.widget.RelativeLayout
import com.bumptech.glide.Glide
import com.simplemobiletools.commons.dialogs.ConfirmationDialog
import com.simplemobiletools.commons.extensions.*
import com.simplemobiletools.commons.helpers.PERMISSION_READ_CONTACTS
import com.simplemobiletools.commons.helpers.ensureBackgroundThread
import com.simplemobiletools.contacts.pro.R
import com.simplemobiletools.contacts.pro.dialogs.CallConfirmationDialog
import com.simplemobiletools.contacts.pro.extensions.*
import com.simplemobiletools.contacts.pro.helpers.*
import com.simplemobiletools.contacts.pro.models.*
import kotlinx.android.synthetic.main.activity_view_contact.*
import kotlinx.android.synthetic.main.item_view_address.view.*
import kotlinx.android.synthetic.main.item_view_contact_source.view.*
import kotlinx.android.synthetic.main.item_view_email.view.*
import kotlinx.android.synthetic.main.item_view_event.view.*
import kotlinx.android.synthetic.main.item_view_group.view.*
import kotlinx.android.synthetic.main.item_view_im.view.*
import kotlinx.android.synthetic.main.item_view_phone_number.view.*
import kotlinx.android.synthetic.main.item_website.view.*

class ViewContactActivity : ContactActivity() {
    private var isViewIntent = false
    private var wasEditLaunched = false
    private var duplicateContacts = ArrayList<Contact>()
    private var contactSources = ArrayList<ContactSource>()
    private var showFields = 0
    private var fullContact: Contact? = null    // contact with all fields filled from duplicates

    private val COMPARABLE_PHONE_NUMBER_LENGTH = 7

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_view_contact)

        if (checkAppSideloading()) {
            return
        }

        showFields = config.showContactFields
    }

    override fun onResume() {
        super.onResume()
        isViewIntent = intent.action == ContactsContract.QuickContact.ACTION_QUICK_CONTACT || intent.action == Intent.ACTION_VIEW
        if (isViewIntent) {
            handlePermission(PERMISSION_READ_CONTACTS) {
                if (it) {
                    ensureBackgroundThread {
                        initContact()
                    }
                } else {
                    toast(R.string.no_contacts_permission)
                    finish()
                }
            }
        } else {
            ensureBackgroundThread {
                initContact()
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_view_contact, menu)
        menu.apply {
            findItem(R.id.open_with).isVisible = contact?.isPrivate() == false
            updateMenuItemColors(this)
        }
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (contact == null) {
            return true
        }

        when (item.itemId) {
            R.id.edit -> launchEditContact(contact!!)
            R.id.share -> shareContact(fullContact!!)
            R.id.open_with -> openWith()
            R.id.delete -> deleteContactFromAllSources()
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
                val rawId = if (data.path!!.contains("lookup")) {
                    val lookupKey = getLookupKeyFromUri(data)
                    if (lookupKey != null) {
                        contact = ContactsHelper(this).getContactWithLookupKey(lookupKey)
                        fullContact = contact
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
            fullContact = contact

            if (contact == null) {
                if (!wasEditLaunched) {
                    toast(R.string.unknown_error_occurred)
                }
                finish()
            } else {
                runOnUiThread {
                    gotContact()
                }
            }
        } else {
            if (contact == null) {
                finish()
            } else {
                runOnUiThread {
                    gotContact()
                }
            }
        }
    }

    private fun gotContact() {
        if (isDestroyed || isFinishing) {
            return
        }

        contact_scrollview.beVisible()
        setupViewContact()
        contact_send_sms.beVisibleIf(contact!!.phoneNumbers.isNotEmpty())
        contact_start_call.beVisibleIf(contact!!.phoneNumbers.isNotEmpty())
        contact_send_email.beVisibleIf(contact!!.emails.isNotEmpty())

        val background = resources.getDrawable(R.drawable.contact_circular_background)
        background.applyColorFilter(config.primaryColor)
        contact_photo.background = background

        if (contact!!.photoUri.isEmpty() && contact!!.photo == null) {
            showPhotoPlaceholder(contact_photo)
        } else {
            updateContactPhoto(contact!!.photoUri, contact_photo, contact!!.photo)
            Glide.with(this).load(contact!!.photo ?: currentContactPhotoPath).into(contact_photo_big)
            contact_photo.setOnClickListener {
                contact_photo_big.alpha = 0f
                contact_photo_big.beVisible()
                contact_photo_big.animate().alpha(1f).start()
            }

            contact_photo_big.setOnClickListener {
                contact_photo_big.animate().alpha(0f).withEndAction { it.beGone() }.start()
            }
        }

        val textColor = config.textColor
        contact_send_sms.applyColorFilter(textColor)
        contact_start_call.applyColorFilter(textColor)
        contact_send_email.applyColorFilter(textColor)
        contact_name_image.applyColorFilter(textColor)
        contact_numbers_image.applyColorFilter(textColor)
        contact_emails_image.applyColorFilter(textColor)
        contact_addresses_image.applyColorFilter(textColor)
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
        setupFavorite()
        setupNames()

        ContactsHelper(this).getContactSources {
            contactSources = it
            runOnUiThread {
                setupContactDetails()
                getDuplicateContacts {
                    setupContactDetails()
                }
            }
        }
    }

    private fun setupContactDetails() {
        if (isFinishing || isDestroyed || contact == null) {
            return
        }

        setupPhoneNumbers()
        setupEmails()
        setupAddresses()
        setupIMs()
        setupEvents()
        setupWebsites()
        setupGroups()
        setupContactSources()
        setupNotes()
        setupOrganization()
        updateTextColors(contact_scrollview)
    }

    private fun launchEditContact(contact: Contact) {
        wasEditLaunched = true
        editContact(contact)
    }

    private fun openWith() {
        Intent().apply {
            action = ContactsContract.QuickContact.ACTION_QUICK_CONTACT
            data = getContactPublicUri(contact!!)
            if (resolveActivity(packageManager) != null) {
                startActivity(this)
            } else {
                toast(R.string.no_app_found)
            }
        }
    }

    private fun setupFavorite() {
        contact_toggle_favorite.apply {
            beVisible()
            setImageDrawable(getStarDrawable(contact!!.starred == 1))
            tag = contact!!.starred
            applyColorFilter(config.textColor)
            setOnClickListener {
                toast(R.string.must_be_at_edit)
            }
        }
    }

    private fun setupNames() {
        contact!!.apply {
            contact_prefix.text = prefix
            contact_prefix.beVisibleIf(prefix.isNotEmpty() && showFields and SHOW_PREFIX_FIELD != 0)
            contact_prefix.copyOnLongClick(prefix)

            contact_first_name.text = firstName
            contact_first_name.beVisibleIf(firstName.isNotEmpty() && showFields and SHOW_FIRST_NAME_FIELD != 0)
            contact_first_name.copyOnLongClick(firstName)

            contact_middle_name.text = middleName
            contact_middle_name.beVisibleIf(middleName.isNotEmpty() && showFields and SHOW_MIDDLE_NAME_FIELD != 0)
            contact_middle_name.copyOnLongClick(middleName)

            contact_surname.text = surname
            contact_surname.beVisibleIf(surname.isNotEmpty() && showFields and SHOW_SURNAME_FIELD != 0)
            contact_surname.copyOnLongClick(surname)

            contact_suffix.text = suffix
            contact_suffix.beVisibleIf(suffix.isNotEmpty() && showFields and SHOW_SUFFIX_FIELD != 0)
            contact_suffix.copyOnLongClick(suffix)

            contact_nickname.text = nickname
            contact_nickname.beVisibleIf(nickname.isNotEmpty() && showFields and SHOW_NICKNAME_FIELD != 0)
            contact_nickname.copyOnLongClick(nickname)

            if (contact_prefix.isGone() && contact_first_name.isGone() && contact_middle_name.isGone() && contact_surname.isGone() && contact_suffix.isGone()
                    && contact_nickname.isGone()) {
                contact_name_image.beInvisible()
                (contact_photo.layoutParams as RelativeLayout.LayoutParams).bottomMargin = resources.getDimension(R.dimen.medium_margin).toInt()
            }
        }
    }

    private fun setupPhoneNumbers() {
        var phoneNumbers = contact!!.phoneNumbers.toMutableSet() as LinkedHashSet<PhoneNumber>
        duplicateContacts.forEach {
            phoneNumbers.addAll(it.phoneNumbers)
        }

        phoneNumbers = phoneNumbers.distinctBy {
            if (it.normalizedNumber != null && it.normalizedNumber!!.length >= COMPARABLE_PHONE_NUMBER_LENGTH) {
                it.normalizedNumber?.substring(it.normalizedNumber!!.length - COMPARABLE_PHONE_NUMBER_LENGTH)
            } else {
                it.normalizedNumber
            }
        }.toMutableSet() as LinkedHashSet<PhoneNumber>

        phoneNumbers = phoneNumbers.sortedBy { it.type }.toMutableSet() as LinkedHashSet<PhoneNumber>
        fullContact!!.phoneNumbers = phoneNumbers.toMutableList() as ArrayList<PhoneNumber>
        contact_numbers_holder.removeAllViews()

        if (phoneNumbers.isNotEmpty() && showFields and SHOW_PHONE_NUMBERS_FIELD != 0) {
            phoneNumbers.forEach {
                layoutInflater.inflate(R.layout.item_view_phone_number, contact_numbers_holder, false).apply {
                    val phoneNumber = it
                    contact_numbers_holder.addView(this)
                    contact_number.text = phoneNumber.value
                    contact_number_type.text = getPhoneNumberTypeText(phoneNumber.type, phoneNumber.label)
                    copyOnLongClick(phoneNumber.value)

                    setOnClickListener {
                        if (config.showCallConfirmation) {
                            CallConfirmationDialog(this@ViewContactActivity, phoneNumber.value) {
                                startCallIntent(phoneNumber.value)
                            }
                        } else {
                            startCallIntent(phoneNumber.value)
                        }
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

    // a contact cannot have different emails per contact source. Such contacts are handled as separate ones, not duplicates of each other
    private fun setupEmails() {
        contact_emails_holder.removeAllViews()
        val emails = contact!!.emails
        if (emails.isNotEmpty() && showFields and SHOW_EMAILS_FIELD != 0) {
            emails.forEach {
                layoutInflater.inflate(R.layout.item_view_email, contact_emails_holder, false).apply {
                    val email = it
                    contact_emails_holder.addView(this)
                    contact_email.text = email.value
                    contact_email_type.text = getEmailTypeText(email.type, email.label)
                    copyOnLongClick(email.value)

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
        var addresses = contact!!.addresses.toMutableSet() as LinkedHashSet<Address>
        duplicateContacts.forEach {
            addresses.addAll(it.addresses)
        }

        addresses = addresses.sortedBy { it.type }.toMutableSet() as LinkedHashSet<Address>
        fullContact!!.addresses = addresses.toMutableList() as ArrayList<Address>
        contact_addresses_holder.removeAllViews()

        if (addresses.isNotEmpty() && showFields and SHOW_ADDRESSES_FIELD != 0) {
            addresses.forEach {
                layoutInflater.inflate(R.layout.item_view_address, contact_addresses_holder, false).apply {
                    val address = it
                    contact_addresses_holder.addView(this)
                    contact_address.text = address.value
                    contact_address_type.text = getAddressTypeText(address.type, address.label)
                    copyOnLongClick(address.value)

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

    private fun setupIMs() {
        var IMs = contact!!.IMs.toMutableSet() as LinkedHashSet<IM>
        duplicateContacts.forEach {
            IMs.addAll(it.IMs)
        }

        IMs = IMs.sortedBy { it.type }.toMutableSet() as LinkedHashSet<IM>
        fullContact!!.IMs = IMs.toMutableList() as ArrayList<IM>
        contact_ims_holder.removeAllViews()

        if (IMs.isNotEmpty() && showFields and SHOW_IMS_FIELD != 0) {
            IMs.forEach {
                layoutInflater.inflate(R.layout.item_view_im, contact_ims_holder, false).apply {
                    val IM = it
                    contact_ims_holder.addView(this)
                    contact_im.text = IM.value
                    contact_im_type.text = getIMTypeText(IM.type, IM.label)
                    copyOnLongClick(IM.value)
                }
            }
            contact_ims_image.beVisible()
            contact_ims_holder.beVisible()
        } else {
            contact_ims_image.beGone()
            contact_ims_holder.beGone()
        }
    }

    private fun setupEvents() {
        var events = contact!!.events.toMutableSet() as LinkedHashSet<Event>
        duplicateContacts.forEach {
            events.addAll(it.events)
        }

        events = events.sortedBy { it.type }.toMutableSet() as LinkedHashSet<Event>
        fullContact!!.events = events.toMutableList() as ArrayList<Event>
        contact_events_holder.removeAllViews()

        if (events.isNotEmpty() && showFields and SHOW_EVENTS_FIELD != 0) {
            events.forEach {
                layoutInflater.inflate(R.layout.item_view_event, contact_events_holder, false).apply {
                    contact_events_holder.addView(this)
                    it.value.getDateTimeFromDateString(contact_event)
                    contact_event_type.setText(getEventTextId(it.type))
                    copyOnLongClick(it.value)
                }
            }
            contact_events_image.beVisible()
            contact_events_holder.beVisible()
        } else {
            contact_events_image.beGone()
            contact_events_holder.beGone()
        }
    }

    private fun setupWebsites() {
        var websites = contact!!.websites.toMutableSet() as LinkedHashSet<String>
        duplicateContacts.forEach {
            websites.addAll(it.websites)
        }

        websites = websites.sorted().toMutableSet() as LinkedHashSet<String>
        fullContact!!.websites = websites.toMutableList() as ArrayList<String>
        contact_websites_holder.removeAllViews()

        if (websites.isNotEmpty() && showFields and SHOW_WEBSITES_FIELD != 0) {
            websites.forEach {
                val url = it
                layoutInflater.inflate(R.layout.item_website, contact_websites_holder, false).apply {
                    contact_websites_holder.addView(this)
                    contact_website.text = url
                    copyOnLongClick(url)

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
        var groups = contact!!.groups.toMutableSet() as LinkedHashSet<Group>
        duplicateContacts.forEach {
            groups.addAll(it.groups)
        }

        groups = groups.sortedBy { it.title }.toMutableSet() as LinkedHashSet<Group>
        fullContact!!.groups = groups.toMutableList() as ArrayList<Group>
        contact_groups_holder.removeAllViews()

        if (groups.isNotEmpty() && showFields and SHOW_GROUPS_FIELD != 0) {
            groups.forEach {
                layoutInflater.inflate(R.layout.item_view_group, contact_groups_holder, false).apply {
                    val group = it
                    contact_groups_holder.addView(this)
                    contact_group.text = group.title
                    copyOnLongClick(group.title)
                }
            }
            contact_groups_image.beVisible()
            contact_groups_holder.beVisible()
        } else {
            contact_groups_image.beGone()
            contact_groups_holder.beGone()
        }
    }

    private fun setupContactSources() {
        contact_sources_holder.removeAllViews()
        if (showFields and SHOW_CONTACT_SOURCE_FIELD != 0) {
            var sources = HashMap<Contact, String>()
            sources[contact!!] = getPublicContactSourceSync(contact!!.source, contactSources)
            duplicateContacts.forEach {
                sources[it] = getPublicContactSourceSync(it.source, contactSources)
            }

            if (sources.size > 1) {
                sources = sources.toList().sortedBy { (key, value) -> value.toLowerCase() }.toMap() as LinkedHashMap<Contact, String>
            }

            for ((key, value) in sources) {
                layoutInflater.inflate(R.layout.item_view_contact_source, contact_sources_holder, false).apply {
                    contact_source.text = value
                    contact_source.copyOnLongClick(value)
                    contact_sources_holder.addView(this)

                    contact_source.setOnClickListener {
                        launchEditContact(key)
                    }
                }
            }

            contact_source_image.beVisible()
            contact_sources_holder.beVisible()
        } else {
            contact_source_image.beGone()
            contact_sources_holder.beGone()
        }
    }

    private fun setupNotes() {
        val notes = contact!!.notes
        if (notes.isNotEmpty() && showFields and SHOW_NOTES_FIELD != 0) {
            contact_notes.text = notes
            contact_notes_image.beVisible()
            contact_notes.beVisible()
            contact_notes.copyOnLongClick(notes)
        } else {
            contact_notes_image.beGone()
            contact_notes.beGone()
        }
    }

    private fun setupOrganization() {
        val organization = contact!!.organization
        if (organization.isNotEmpty() && showFields and SHOW_ORGANIZATION_FIELD != 0) {
            contact_organization_company.text = organization.company
            contact_organization_job_position.text = organization.jobPosition
            contact_organization_image.beGoneIf(organization.isEmpty())
            contact_organization_company.beGoneIf(organization.company.isEmpty())
            contact_organization_job_position.beGoneIf(organization.jobPosition.isEmpty())
            contact_organization_company.copyOnLongClick(contact_organization_company.value)
            contact_organization_job_position.copyOnLongClick(contact_organization_job_position.value)

            if (organization.company.isEmpty() && organization.jobPosition.isNotEmpty()) {
                (contact_organization_image.layoutParams as RelativeLayout.LayoutParams).addRule(RelativeLayout.ALIGN_TOP, contact_organization_job_position.id)
            }
        } else {
            contact_organization_image.beGone()
            contact_organization_company.beGone()
            contact_organization_job_position.beGone()
        }
    }

    private fun getDuplicateContacts(callback: () -> Unit) {
        ContactsHelper(this).getDuplicatesOfContact(contact!!, false) { contacts ->
            ensureBackgroundThread {
                duplicateContacts.clear()
                val displayContactSources = getVisibleContactSources()
                contacts.filter { displayContactSources.contains(it.source) }.forEach {
                    val duplicate = ContactsHelper(this).getContactWithId(it.id, it.isPrivate())
                    if (duplicate != null) {
                        duplicateContacts.add(duplicate)
                    }
                }

                runOnUiThread {
                    callback()
                }
            }
        }
    }

    private fun deleteContactFromAllSources() {
        val addition = if (contact_sources_holder.childCount > 1) {
            "\n\n${getString(R.string.delete_from_all_sources)}"
        } else {
            ""
        }

        val message = "${getString(R.string.proceed_with_deletion)}$addition"
        ConfirmationDialog(this, message) {
            if (contact != null) {
                ContactsHelper(this).deleteContact(contact!!, true) {
                    finish()
                }
            }
        }
    }

    private fun getStarDrawable(on: Boolean) = resources.getDrawable(if (on) R.drawable.ic_star_on_vector else R.drawable.ic_star_off_vector)

    private fun View.copyOnLongClick(value: String) {
        setOnLongClickListener {
            copyToClipboard(value)
            toast(R.string.value_copied_to_clipboard)
            true
        }
    }
}
