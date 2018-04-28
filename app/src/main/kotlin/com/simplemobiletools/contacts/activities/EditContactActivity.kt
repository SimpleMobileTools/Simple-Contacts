package com.simplemobiletools.contacts.activities

import android.app.DatePickerDialog
import android.content.ClipData
import android.content.ContentValues
import android.content.Intent
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Bundle
import android.provider.ContactsContract.CommonDataKinds
import android.provider.MediaStore
import android.view.Menu
import android.view.MenuItem
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.ImageView
import android.widget.TextView
import com.simplemobiletools.commons.dialogs.RadioGroupDialog
import com.simplemobiletools.commons.extensions.*
import com.simplemobiletools.commons.helpers.PERMISSION_READ_CONTACTS
import com.simplemobiletools.commons.helpers.PERMISSION_WRITE_CONTACTS
import com.simplemobiletools.commons.models.RadioItem
import com.simplemobiletools.contacts.R
import com.simplemobiletools.contacts.dialogs.SelectGroupsDialog
import com.simplemobiletools.contacts.extensions.*
import com.simplemobiletools.contacts.helpers.*
import com.simplemobiletools.contacts.models.*
import kotlinx.android.synthetic.main.activity_edit_contact.*
import kotlinx.android.synthetic.main.item_edit_address.view.*
import kotlinx.android.synthetic.main.item_edit_email.view.*
import kotlinx.android.synthetic.main.item_edit_group.view.*
import kotlinx.android.synthetic.main.item_edit_phone_number.view.*
import kotlinx.android.synthetic.main.item_edit_website.view.*
import kotlinx.android.synthetic.main.item_event.view.*
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat
import java.util.*

class EditContactActivity : ContactActivity() {
    private val INTENT_TAKE_PHOTO = 1
    private val INTENT_CHOOSE_PHOTO = 2
    private val INTENT_CROP_PHOTO = 3

    private val TAKE_PHOTO = 1
    private val CHOOSE_PHOTO = 2
    private val REMOVE_PHOTO = 3

    private val KEY_PHONE = "phone"
    private val KEY_NAME = "name"

    private var wasActivityInitialized = false
    private var lastPhotoIntentUri: Uri? = null
    private var isSaving = false
    private var isThirdPartyIntent = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_contact)
        supportActionBar?.setHomeAsUpIndicator(R.drawable.ic_cross)
        showFields = config.showContactFields

        val action = intent.action
        isThirdPartyIntent = action == Intent.ACTION_EDIT || action == Intent.ACTION_INSERT_OR_EDIT || action == Intent.ACTION_INSERT
        if (isThirdPartyIntent) {
            handlePermission(PERMISSION_READ_CONTACTS) {
                if (it) {
                    handlePermission(PERMISSION_WRITE_CONTACTS) {
                        if (it) {
                            initContact()
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
        } else {
            initContact()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_edit_contact, menu)
        if (wasActivityInitialized) {
            menu.findItem(R.id.delete).isVisible = contact?.id != 0
            menu.findItem(R.id.share).isVisible = contact?.id != 0
        }
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.save -> saveContact()
            R.id.delete -> deleteContact()
            R.id.share -> shareContact()
            else -> return super.onOptionsItemSelected(item)
        }
        return true
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, resultData: Intent?) {
        super.onActivityResult(requestCode, resultCode, resultData)
        if (resultCode == RESULT_OK) {
            when (requestCode) {
                INTENT_TAKE_PHOTO, INTENT_CHOOSE_PHOTO -> startCropPhotoIntent(lastPhotoIntentUri)
                INTENT_CROP_PHOTO -> updateContactPhoto(lastPhotoIntentUri.toString(), contact_photo)
            }
        }
    }

    private fun initContact() {
        var contactId = intent.getIntExtra(CONTACT_ID, 0)
        val action = intent.action
        if (contactId == 0 && action == Intent.ACTION_EDIT) {
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
            setupNewContact()
        } else {
            setupEditContact()
        }

        if (contact!!.id == 0 && intent.extras?.containsKey(KEY_PHONE) == true && (action == Intent.ACTION_INSERT_OR_EDIT || action == Intent.ACTION_INSERT)) {
            val phoneNumber = intent.extras.get(KEY_PHONE)?.toString() ?: ""
            contact!!.phoneNumbers.add(PhoneNumber(phoneNumber, DEFAULT_PHONE_NUMBER_TYPE))

            contact!!.firstName = intent.extras.get(KEY_NAME)?.toString() ?: ""

            val data = intent.extras.getParcelableArrayList<ContentValues>("data")
            if (data != null) {
                parseIntentData(data)
            }
            setupEditContact()
        }

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
        contact_numbers_image.applyColorFilter(textColor)
        contact_emails_image.applyColorFilter(textColor)
        contact_addresses_image.applyColorFilter(textColor)
        contact_events_image.applyColorFilter(textColor)
        contact_notes_image.applyColorFilter(textColor)
        contact_organization_image.applyColorFilter(textColor)
        contact_websites_image.applyColorFilter(textColor)
        contact_groups_image.applyColorFilter(textColor)
        contact_source_image.applyColorFilter(textColor)

        val adjustedPrimaryColor = getAdjustedPrimaryColor()
        contact_numbers_add_new.applyColorFilter(adjustedPrimaryColor)
        contact_numbers_add_new.background.applyColorFilter(textColor)
        contact_emails_add_new.applyColorFilter(adjustedPrimaryColor)
        contact_emails_add_new.background.applyColorFilter(textColor)
        contact_addresses_add_new.applyColorFilter(adjustedPrimaryColor)
        contact_addresses_add_new.background.applyColorFilter(textColor)
        contact_events_add_new.applyColorFilter(adjustedPrimaryColor)
        contact_events_add_new.background.applyColorFilter(textColor)
        contact_websites_add_new.applyColorFilter(adjustedPrimaryColor)
        contact_websites_add_new.background.applyColorFilter(textColor)
        contact_groups_add_new.applyColorFilter(adjustedPrimaryColor)
        contact_groups_add_new.background.applyColorFilter(textColor)

        contact_toggle_favorite.setOnClickListener { toggleFavorite() }
        contact_photo.setOnClickListener { trySetPhoto() }
        contact_send_sms.setOnClickListener { trySendSMS() }
        contact_start_call.setOnClickListener { tryStartCall(contact!!) }
        contact_send_email.setOnClickListener { trySendEmail() }
        contact_numbers_add_new.setOnClickListener { addNewPhoneNumberField() }
        contact_emails_add_new.setOnClickListener { addNewEmailField() }
        contact_addresses_add_new.setOnClickListener { addNewAddressField() }
        contact_events_add_new.setOnClickListener { addNewEventField() }
        contact_websites_add_new.setOnClickListener { addNewWebsiteField() }
        contact_groups_add_new.setOnClickListener { showSelectGroupsDialog() }

        setupFieldVisibility()

        contact_toggle_favorite.apply {
            setImageDrawable(getStarDrawable(contact!!.starred == 1))
            tag = contact!!.starred
            applyColorFilter(textColor)
        }

        updateTextColors(contact_scrollview)
        wasActivityInitialized = true
        invalidateOptionsMenu()
    }

    private fun startCropPhotoIntent(uri: Uri?) {
        if (uri == null) {
            toast(R.string.unknown_error_occurred)
            return
        }

        lastPhotoIntentUri = getCachePhotoUri()
        Intent("com.android.camera.action.CROP").apply {
            setDataAndType(uri, "image/*")
            putExtra(MediaStore.EXTRA_OUTPUT, lastPhotoIntentUri)
            putExtra("outputX", 720)
            putExtra("outputY", 720)
            putExtra("aspectX", 1)
            putExtra("aspectY", 1)
            putExtra("crop", "true")
            putExtra("scale", "true")
            putExtra("scaleUpIfNeeded", "true")
            clipData = ClipData("Attachment", arrayOf("text/uri-list"), ClipData.Item(lastPhotoIntentUri))
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
            if (resolveActivity(packageManager) != null) {
                startActivityForResult(this, INTENT_CROP_PHOTO)
            } else {
                toast(R.string.no_app_found)
            }
        }
    }

    private fun setupFieldVisibility() {
        if (showFields and (SHOW_PREFIX_FIELD or SHOW_FIRST_NAME_FIELD or SHOW_MIDDLE_NAME_FIELD or SHOW_SURNAME_FIELD or SHOW_SUFFIX_FIELD) == 0) {
            contact_name_image.beInvisible()
        }

        contact_prefix.beVisibleIf(showFields and SHOW_PREFIX_FIELD != 0)
        contact_first_name.beVisibleIf(showFields and SHOW_FIRST_NAME_FIELD != 0)
        contact_middle_name.beVisibleIf(showFields and SHOW_MIDDLE_NAME_FIELD != 0)
        contact_surname.beVisibleIf(showFields and SHOW_SURNAME_FIELD != 0)
        contact_suffix.beVisibleIf(showFields and SHOW_SUFFIX_FIELD != 0)

        contact_source.beVisibleIf(showFields and SHOW_CONTACT_SOURCE_FIELD != 0)
        contact_source_image.beVisibleIf(showFields and SHOW_CONTACT_SOURCE_FIELD != 0)

        val arePhoneNumbersVisible = showFields and SHOW_PHONE_NUMBERS_FIELD != 0
        contact_numbers_image.beVisibleIf(arePhoneNumbersVisible)
        contact_numbers_holder.beVisibleIf(arePhoneNumbersVisible)
        contact_numbers_add_new.beVisibleIf(arePhoneNumbersVisible)

        val areEmailsVisible = showFields and SHOW_EMAILS_FIELD != 0
        contact_emails_image.beVisibleIf(areEmailsVisible)
        contact_emails_holder.beVisibleIf(areEmailsVisible)
        contact_emails_add_new.beVisibleIf(areEmailsVisible)

        val areAddressesVisible = showFields and SHOW_ADDRESSES_FIELD != 0
        contact_addresses_image.beVisibleIf(areAddressesVisible)
        contact_addresses_holder.beVisibleIf(areAddressesVisible)
        contact_addresses_add_new.beVisibleIf(areAddressesVisible)

        val isOrganizationVisible = showFields and SHOW_ORGANIZATION_FIELD != 0
        contact_organization_company.beVisibleIf(isOrganizationVisible)
        contact_organization_job_position.beVisibleIf(isOrganizationVisible)
        contact_organization_image.beVisibleIf(isOrganizationVisible)

        val areEventsVisible = showFields and SHOW_EVENTS_FIELD != 0
        contact_events_image.beVisibleIf(areEventsVisible)
        contact_events_holder.beVisibleIf(areEventsVisible)
        contact_events_add_new.beVisibleIf(areEventsVisible)

        val areWebsitesVisible = showFields and SHOW_WEBSITES_FIELD != 0
        contact_websites_image.beVisibleIf(areWebsitesVisible)
        contact_websites_holder.beVisibleIf(areWebsitesVisible)
        contact_websites_add_new.beVisibleIf(areWebsitesVisible)

        val areGroupsVisible = showFields and SHOW_GROUPS_FIELD != 0
        contact_groups_image.beVisibleIf(areGroupsVisible)
        contact_groups_holder.beVisibleIf(areGroupsVisible)
        contact_groups_add_new.beVisibleIf(areGroupsVisible)

        val areNotesVisible = showFields and SHOW_NOTES_FIELD != 0
        contact_notes.beVisibleIf(areNotesVisible)
        contact_notes_image.beVisibleIf(areNotesVisible)
    }

    private fun setupEditContact() {
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN)
        supportActionBar?.title = resources.getString(R.string.edit_contact)

        setupNames()
        setupPhoneNumbers()
        setupEmails()
        setupAddresses()
        setupNotes()
        setupOrganization()
        setupWebsites()
        setupEvents()
        setupGroups()
        setupContactSource()
    }

    private fun setupNames() {
        contact!!.apply {
            contact_prefix.setText(prefix)
            contact_first_name.setText(firstName)
            contact_middle_name.setText(middleName)
            contact_surname.setText(surname)
            contact_suffix.setText(suffix)
        }
    }

    private fun setupPhoneNumbers() {
        if (showFields and SHOW_PHONE_NUMBERS_FIELD != 0) {
            contact!!.phoneNumbers.forEachIndexed { index, number ->
                var numberHolder = contact_numbers_holder.getChildAt(index)
                if (numberHolder == null) {
                    numberHolder = layoutInflater.inflate(R.layout.item_edit_phone_number, contact_numbers_holder, false)
                    contact_numbers_holder.addView(numberHolder)
                }

                numberHolder!!.apply {
                    contact_number.setText(number.value)
                    setupPhoneNumberTypePicker(contact_number_type, number.type)
                }
            }
        }
    }

    private fun setupEmails() {
        if (showFields and SHOW_EMAILS_FIELD != 0) {
            contact!!.emails.forEachIndexed { index, email ->
                var emailHolder = contact_emails_holder.getChildAt(index)
                if (emailHolder == null) {
                    emailHolder = layoutInflater.inflate(R.layout.item_edit_email, contact_emails_holder, false)
                    contact_emails_holder.addView(emailHolder)
                }

                emailHolder!!.apply {
                    contact_email.setText(email.value)
                    setupEmailTypePicker(contact_email_type, email.type)
                }
            }
        }
    }

    private fun setupAddresses() {
        if (showFields and SHOW_ADDRESSES_FIELD != 0) {
            contact!!.addresses.forEachIndexed { index, address ->
                var addressHolder = contact_addresses_holder.getChildAt(index)
                if (addressHolder == null) {
                    addressHolder = layoutInflater.inflate(R.layout.item_edit_address, contact_addresses_holder, false)
                    contact_addresses_holder.addView(addressHolder)
                }

                addressHolder!!.apply {
                    contact_address.setText(address.value)
                    setupAddressTypePicker(contact_address_type, address.type)
                }
            }
        }
    }

    private fun setupNotes() {
        if (showFields and SHOW_NOTES_FIELD != 0) {
            contact_notes.setText(contact!!.notes)
        }
    }

    private fun setupOrganization() {
        if (showFields and SHOW_ORGANIZATION_FIELD != 0) {
            contact_organization_company.setText(contact!!.organization.company)
            contact_organization_job_position.setText(contact!!.organization.jobPosition)
        }
    }

    private fun setupWebsites() {
        if (showFields and SHOW_WEBSITES_FIELD != 0) {
            contact!!.websites.forEachIndexed { index, website ->
                var websitesHolder = contact_websites_holder.getChildAt(index)
                if (websitesHolder == null) {
                    websitesHolder = layoutInflater.inflate(R.layout.item_edit_website, contact_websites_holder, false)
                    contact_websites_holder.addView(websitesHolder)
                }

                websitesHolder!!.contact_website.setText(website)
            }
        }
    }

    private fun setupEvents() {
        if (showFields and SHOW_EVENTS_FIELD != 0) {
            contact!!.events.forEachIndexed { index, event ->
                var eventHolder = contact_events_holder.getChildAt(index)
                if (eventHolder == null) {
                    eventHolder = layoutInflater.inflate(R.layout.item_event, contact_events_holder, false)
                    contact_events_holder.addView(eventHolder)
                }

                (eventHolder as ViewGroup).apply {
                    val contactEvent = contact_event.apply {
                        getDateTime(event.value, this)
                        tag = event.value
                        alpha = 1f
                    }

                    setupEventTypePicker(this, event.type)

                    contact_event_remove.apply {
                        beVisible()
                        applyColorFilter(getAdjustedPrimaryColor())
                        background.applyColorFilter(config.textColor)
                        setOnClickListener {
                            resetContactEvent(contactEvent, this)
                        }
                    }
                }
            }
        }
    }

    private fun setupGroups() {
        if (showFields and SHOW_GROUPS_FIELD != 0) {
            contact_groups_holder.removeAllViews()
            val groups = contact!!.groups
            groups.forEachIndexed { index, group ->
                var groupHolder = contact_groups_holder.getChildAt(index)
                if (groupHolder == null) {
                    groupHolder = layoutInflater.inflate(R.layout.item_edit_group, contact_groups_holder, false)
                    contact_groups_holder.addView(groupHolder)
                }

                (groupHolder as ViewGroup).apply {
                    contact_group.apply {
                        text = group.title
                        setTextColor(config.textColor)
                        tag = group.id
                        alpha = 1f
                    }

                    setOnClickListener {
                        showSelectGroupsDialog()
                    }

                    contact_group_remove.apply {
                        beVisible()
                        applyColorFilter(getAdjustedPrimaryColor())
                        background.applyColorFilter(config.textColor)
                        setOnClickListener {
                            removeGroup(group.id)
                        }
                    }
                }
            }

            if (groups.isEmpty()) {
                layoutInflater.inflate(R.layout.item_edit_group, contact_groups_holder, false).apply {
                    contact_group.apply {
                        alpha = 0.5f
                        text = getString(R.string.no_groups)
                        setTextColor(config.textColor)
                    }

                    contact_groups_holder.addView(this)
                    contact_group_remove.beGone()
                    setOnClickListener {
                        showSelectGroupsDialog()
                    }
                }
            }
        }
    }

    private fun setupContactSource() {
        contact_source.text = getPublicContactSource(contact!!.source)
    }

    private fun setupNewContact() {
        supportActionBar?.title = resources.getString(R.string.new_contact)
        val contactSource = if (hasContactPermissions()) config.lastUsedContactSource else SMT_PRIVATE
        val organization = Organization("", "")
        contact = Contact(0, "", "", "", "", "", "", ArrayList(), ArrayList(), ArrayList(), ArrayList(), contactSource, 0, 0, "", null, "",
                ArrayList(), organization, ArrayList())
        contact_source.text = getPublicContactSource(contact!!.source)
        contact_source.setOnClickListener {
            showContactSourcePicker(contact!!.source) {
                contact!!.source = if (it == getString(R.string.phone_storage_hidden)) SMT_PRIVATE else it
                contact_source.text = getPublicContactSource(it)
            }
        }
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

        if (contact!!.addresses.isEmpty()) {
            val addressHolder = contact_addresses_holder.getChildAt(0)
            (addressHolder as? ViewGroup)?.contact_address_type?.apply {
                setupAddressTypePicker(this)
            }
        }

        if (contact!!.events.isEmpty()) {
            val eventHolder = contact_events_holder.getChildAt(0)
            (eventHolder as? ViewGroup)?.apply {
                setupEventTypePicker(this)
            }
        }

        if (contact!!.groups.isEmpty()) {
            val groupsHolder = contact_groups_holder.getChildAt(0)
            (groupsHolder as? ViewGroup)?.contact_group?.apply {
                setupGroupsPicker(this)
            }
        }
    }

    private fun setupPhoneNumberTypePicker(numberTypeField: TextView, type: Int = DEFAULT_PHONE_NUMBER_TYPE) {
        numberTypeField.apply {
            setText(getPhoneNumberTextId(type))
            setOnClickListener {
                showNumberTypePicker(it as TextView)
            }
        }
    }

    private fun setupEmailTypePicker(emailTypeField: TextView, type: Int = DEFAULT_EMAIL_TYPE) {
        emailTypeField.apply {
            setText(getEmailTextId(type))
            setOnClickListener {
                showEmailTypePicker(it as TextView)
            }
        }
    }

    private fun setupAddressTypePicker(addressTypeField: TextView, type: Int = DEFAULT_ADDRESS_TYPE) {
        addressTypeField.apply {
            setText(getAddressTextId(type))
            setOnClickListener {
                showAddressTypePicker(it as TextView)
            }
        }
    }

    private fun setupEventTypePicker(eventHolder: ViewGroup, type: Int = DEFAULT_EVENT_TYPE) {
        eventHolder.contact_event_type.apply {
            setText(getEventTextId(type))
            setOnClickListener {
                showEventTypePicker(it as TextView)
            }
        }

        val eventField = eventHolder.contact_event
        eventField.setOnClickListener {
            val setDateListener = DatePickerDialog.OnDateSetListener { view, year, monthOfYear, dayOfMonth ->
                eventHolder.contact_event_remove.beVisible()
                val date = DateTime().withDate(year, monthOfYear + 1, dayOfMonth).withTimeAtStartOfDay()
                val formatted = date.toString(DateTimeFormat.mediumDate())
                eventField.apply {
                    text = formatted
                    tag = date.toString("yyyy-MM-dd")
                    alpha = 1f
                }
            }

            val date = getDateTime(eventField.tag?.toString() ?: "")
            DatePickerDialog(this, getDialogTheme(), setDateListener, date.year, date.monthOfYear - 1, date.dayOfMonth).show()
        }

        eventHolder.contact_event_remove.apply {
            applyColorFilter(getAdjustedPrimaryColor())
            background.applyColorFilter(config.textColor)
            setOnClickListener {
                resetContactEvent(eventField, this@apply)
            }
        }
    }

    private fun setupGroupsPicker(groupTitleField: TextView, group: Group? = null) {
        groupTitleField.apply {
            text = group?.title ?: getString(R.string.no_groups)
            alpha = if (group == null) 0.5f else 1f
            setOnClickListener {
                showSelectGroupsDialog()
            }
        }
    }

    private fun resetContactEvent(contactEvent: TextView, removeContactEventButton: ImageView) {
        contactEvent.apply {
            text = getString(R.string.unknown)
            tag = ""
            alpha = 0.5f
        }
        removeContactEventButton.beGone()
    }

    private fun removeGroup(id: Long) {
        contact!!.groups = contact!!.groups.filter { it.id != id } as ArrayList<Group>
        setupGroups()
    }

    private fun showNumberTypePicker(numberTypeField: TextView) {
        val items = arrayListOf(
                RadioItem(CommonDataKinds.Phone.TYPE_MOBILE, getString(R.string.mobile)),
                RadioItem(CommonDataKinds.Phone.TYPE_HOME, getString(R.string.home)),
                RadioItem(CommonDataKinds.Phone.TYPE_WORK, getString(R.string.work)),
                RadioItem(CommonDataKinds.Phone.TYPE_MAIN, getString(R.string.main_number)),
                RadioItem(CommonDataKinds.Phone.TYPE_FAX_WORK, getString(R.string.work_fax)),
                RadioItem(CommonDataKinds.Phone.TYPE_FAX_HOME, getString(R.string.home_fax)),
                RadioItem(CommonDataKinds.Phone.TYPE_PAGER, getString(R.string.pager)),
                RadioItem(CommonDataKinds.Phone.TYPE_OTHER, getString(R.string.other))
        )

        val currentNumberTypeId = getPhoneNumberTypeId(numberTypeField.value)
        RadioGroupDialog(this, items, currentNumberTypeId) {
            numberTypeField.setText(getPhoneNumberTextId(it as Int))
        }
    }

    private fun showEmailTypePicker(emailTypeField: TextView) {
        val items = arrayListOf(
                RadioItem(CommonDataKinds.Email.TYPE_HOME, getString(R.string.home)),
                RadioItem(CommonDataKinds.Email.TYPE_WORK, getString(R.string.work)),
                RadioItem(CommonDataKinds.Email.TYPE_MOBILE, getString(R.string.mobile)),
                RadioItem(CommonDataKinds.Email.TYPE_OTHER, getString(R.string.other))
        )

        val currentEmailTypeId = getEmailTypeId(emailTypeField.value)
        RadioGroupDialog(this, items, currentEmailTypeId) {
            emailTypeField.setText(getEmailTextId(it as Int))
        }
    }

    private fun showAddressTypePicker(addressTypeField: TextView) {
        val items = arrayListOf(
                RadioItem(CommonDataKinds.StructuredPostal.TYPE_HOME, getString(R.string.home)),
                RadioItem(CommonDataKinds.StructuredPostal.TYPE_WORK, getString(R.string.work)),
                RadioItem(CommonDataKinds.StructuredPostal.TYPE_OTHER, getString(R.string.other))
        )

        val currentAddressTypeId = getAddressTypeId(addressTypeField.value)
        RadioGroupDialog(this, items, currentAddressTypeId) {
            addressTypeField.setText(getAddressTextId(it as Int))
        }
    }

    private fun showEventTypePicker(eventTypeField: TextView) {
        val items = arrayListOf(
                RadioItem(CommonDataKinds.Event.TYPE_BIRTHDAY, getString(R.string.birthday)),
                RadioItem(CommonDataKinds.Event.TYPE_ANNIVERSARY, getString(R.string.anniversary)),
                RadioItem(CommonDataKinds.Event.TYPE_OTHER, getString(R.string.other))
        )

        val currentEventTypeId = getEventTypeId(eventTypeField.value)
        RadioGroupDialog(this, items, currentEventTypeId) {
            eventTypeField.setText(getEventTextId(it as Int))
        }
    }

    private fun showSelectGroupsDialog() {
        SelectGroupsDialog(this@EditContactActivity, contact!!.groups) {
            contact!!.groups = it
            setupGroups()
        }
    }

    private fun saveContact() {
        if (isSaving || contact == null) {
            return
        }

        contact!!.apply {
            val oldPhotoUri = photoUri

            prefix = contact_prefix.value
            firstName = contact_first_name.value
            middleName = contact_middle_name.value
            surname = contact_surname.value
            suffix = contact_suffix.value
            photoUri = currentContactPhotoPath
            phoneNumbers = getFilledPhoneNumbers()
            emails = getFilledEmails()
            addresses = getFilledAddresses()
            events = getFilledEvents()
            source = contact!!.source
            starred = if (isContactStarred()) 1 else 0
            notes = contact_notes.value
            websites = getFilledWebsites()

            val company = contact_organization_company.value
            val jobPosition = contact_organization_job_position.value
            organization = Organization(company, jobPosition)

            Thread {
                config.lastUsedContactSource = source
                if (id == 0) {
                    insertNewContact()
                } else {
                    val photoUpdateStatus = getPhotoUpdateStatus(oldPhotoUri, photoUri)
                    updateContact(photoUpdateStatus)
                }
            }.start()
        }
    }

    private fun getFilledPhoneNumbers(): ArrayList<PhoneNumber> {
        val phoneNumbers = ArrayList<PhoneNumber>()
        val numbersCount = contact_numbers_holder.childCount
        for (i in 0 until numbersCount) {
            val numberHolder = contact_numbers_holder.getChildAt(i)
            val number = numberHolder.contact_number.value
            val numberType = getPhoneNumberTypeId(numberHolder.contact_number_type.value)

            if (number.isNotEmpty()) {
                phoneNumbers.add(PhoneNumber(number, numberType))
            }
        }
        return phoneNumbers
    }

    private fun getFilledEmails(): ArrayList<Email> {
        val emails = ArrayList<Email>()
        val emailsCount = contact_emails_holder.childCount
        for (i in 0 until emailsCount) {
            val emailHolder = contact_emails_holder.getChildAt(i)
            val email = emailHolder.contact_email.value
            val emailType = getEmailTypeId(emailHolder.contact_email_type.value)

            if (email.isNotEmpty()) {
                emails.add(Email(email, emailType))
            }
        }
        return emails
    }

    private fun getFilledAddresses(): ArrayList<Address> {
        val addresses = ArrayList<Address>()
        val addressesCount = contact_addresses_holder.childCount
        for (i in 0 until addressesCount) {
            val addressHolder = contact_addresses_holder.getChildAt(i)
            val address = addressHolder.contact_address.value
            val addressType = getAddressTypeId(addressHolder.contact_address_type.value)

            if (address.isNotEmpty()) {
                addresses.add(Address(address, addressType))
            }
        }
        return addresses
    }

    private fun getFilledEvents(): ArrayList<Event> {
        val unknown = getString(R.string.unknown)
        val events = ArrayList<Event>()
        val eventsCount = contact_events_holder.childCount
        for (i in 0 until eventsCount) {
            val eventHolder = contact_events_holder.getChildAt(i)
            val event = eventHolder.contact_event.value
            val eventType = getEventTypeId(eventHolder.contact_event_type.value)

            if (event.isNotEmpty() && event != unknown) {
                events.add(Event(eventHolder.contact_event.tag.toString(), eventType))
            }
        }
        return events
    }

    private fun getFilledWebsites(): ArrayList<String> {
        val websites = ArrayList<String>()
        val websitesCount = contact_websites_holder.childCount
        for (i in 0 until websitesCount) {
            val websiteHolder = contact_websites_holder.getChildAt(i)
            val website = websiteHolder.contact_website.value
            if (website.isNotEmpty()) {
                websites.add(website)
            }
        }
        return websites
    }

    private fun insertNewContact() {
        isSaving = true
        toast(R.string.inserting)
        if (ContactsHelper(this@EditContactActivity).insertContact(contact!!)) {
            finish()
        } else {
            toast(R.string.unknown_error_occurred)
        }
    }

    private fun updateContact(photoUpdateStatus: Int) {
        isSaving = true
        if (ContactsHelper(this@EditContactActivity).updateContact(contact!!, photoUpdateStatus)) {
            finish()
        } else {
            toast(R.string.unknown_error_occurred)
        }
    }

    private fun getPhotoUpdateStatus(oldUri: String, newUri: String): Int {
        return if (oldUri.isEmpty() && newUri.isNotEmpty()) {
            PHOTO_ADDED
        } else if (oldUri.isNotEmpty() && newUri.isEmpty()) {
            PHOTO_REMOVED
        } else if (oldUri != newUri) {
            PHOTO_CHANGED
        } else {
            PHOTO_UNCHANGED
        }
    }

    private fun addNewPhoneNumberField() {
        val numberHolder = layoutInflater.inflate(R.layout.item_edit_phone_number, contact_numbers_holder, false) as ViewGroup
        updateTextColors(numberHolder)
        setupPhoneNumberTypePicker(numberHolder.contact_number_type)
        contact_numbers_holder.addView(numberHolder)
        contact_numbers_holder.onGlobalLayout {
            numberHolder.contact_number.requestFocus()
            showKeyboard(numberHolder.contact_number)
        }
    }

    private fun addNewEmailField() {
        val emailHolder = layoutInflater.inflate(R.layout.item_edit_email, contact_emails_holder, false) as ViewGroup
        updateTextColors(emailHolder)
        setupEmailTypePicker(emailHolder.contact_email_type)
        contact_emails_holder.addView(emailHolder)
        contact_emails_holder.onGlobalLayout {
            emailHolder.contact_email.requestFocus()
            showKeyboard(emailHolder.contact_email)
        }
    }

    private fun addNewAddressField() {
        val addressHolder = layoutInflater.inflate(R.layout.item_edit_address, contact_addresses_holder, false) as ViewGroup
        updateTextColors(addressHolder)
        setupAddressTypePicker(addressHolder.contact_address_type)
        contact_addresses_holder.addView(addressHolder)
        contact_addresses_holder.onGlobalLayout {
            addressHolder.contact_address.requestFocus()
            showKeyboard(addressHolder.contact_address)
        }
    }

    private fun addNewEventField() {
        val eventHolder = layoutInflater.inflate(R.layout.item_event, contact_events_holder, false) as ViewGroup
        updateTextColors(eventHolder)
        setupEventTypePicker(eventHolder)
        contact_events_holder.addView(eventHolder)
    }

    private fun toggleFavorite() {
        val isStarred = isContactStarred()
        contact_toggle_favorite.apply {
            setImageDrawable(getStarDrawable(!isStarred))
            tag = if (isStarred) 0 else 1
            applyColorFilter(config.textColor)
        }
    }

    private fun addNewWebsiteField() {
        val websitesHolder = layoutInflater.inflate(R.layout.item_edit_website, contact_websites_holder, false) as ViewGroup
        updateTextColors(websitesHolder)
        contact_websites_holder.addView(websitesHolder)
        contact_websites_holder.onGlobalLayout {
            websitesHolder.contact_website.requestFocus()
            showKeyboard(websitesHolder.contact_website)
        }
    }

    private fun isContactStarred() = contact_toggle_favorite.tag == 1

    private fun getStarDrawable(on: Boolean) = resources.getDrawable(if (on) R.drawable.ic_star_on_big else R.drawable.ic_star_off_big)

    private fun trySetPhoto() {
        val items = arrayListOf(
                RadioItem(TAKE_PHOTO, getString(R.string.take_photo)),
                RadioItem(CHOOSE_PHOTO, getString(R.string.choose_photo))
        )

        if (currentContactPhotoPath.isNotEmpty() || contact!!.photo != null) {
            items.add(RadioItem(REMOVE_PHOTO, getString(R.string.remove_photo)))
        }

        RadioGroupDialog(this, items) {
            when (it as Int) {
                TAKE_PHOTO -> startTakePhotoIntent()
                CHOOSE_PHOTO -> startChoosePhotoIntent()
                else -> showPhotoPlaceholder(contact_photo)
            }
        }
    }

    private fun parseIntentData(data: ArrayList<ContentValues>) {
        data.forEach {
            when (it.get(CommonDataKinds.StructuredName.MIMETYPE)) {
                CommonDataKinds.Email.CONTENT_ITEM_TYPE -> parseEmail(it)
                CommonDataKinds.StructuredPostal.CONTENT_ITEM_TYPE -> parseAddress(it)
                CommonDataKinds.Organization.CONTENT_ITEM_TYPE -> parseOrganization(it)
                CommonDataKinds.Event.CONTENT_ITEM_TYPE -> parseEvent(it)
                CommonDataKinds.Website.CONTENT_ITEM_TYPE -> parseWebsite(it)
                CommonDataKinds.Note.CONTENT_ITEM_TYPE -> parseNote(it)
            }
        }
    }

    private fun parseEmail(contentValues: ContentValues) {
        val type = contentValues.getAsInteger(CommonDataKinds.Email.DATA2) ?: DEFAULT_EMAIL_TYPE
        val emailValue = contentValues.getAsString(CommonDataKinds.Email.DATA1) ?: return
        val email = Email(emailValue, type)
        contact!!.emails.add(email)
    }

    private fun parseAddress(contentValues: ContentValues) {
        val type = contentValues.getAsInteger(CommonDataKinds.StructuredPostal.DATA2) ?: DEFAULT_ADDRESS_TYPE
        val addressValue = contentValues.getAsString(CommonDataKinds.StructuredPostal.DATA4)
                ?: contentValues.getAsString(CommonDataKinds.StructuredPostal.DATA1) ?: return
        val address = Address(addressValue, type)
        contact!!.addresses.add(address)
    }

    private fun parseOrganization(contentValues: ContentValues) {
        val company = contentValues.getAsString(CommonDataKinds.Organization.DATA1) ?: ""
        val jobPosition = contentValues.getAsString(CommonDataKinds.Organization.DATA4) ?: ""
        contact!!.organization = Organization(company, jobPosition)
    }

    private fun parseEvent(contentValues: ContentValues) {
        val type = contentValues.getAsInteger(CommonDataKinds.Event.DATA2) ?: DEFAULT_EVENT_TYPE
        val eventValue = contentValues.getAsString(CommonDataKinds.Event.DATA1) ?: return
        val event = Event(eventValue, type)
        contact!!.events.add(event)
    }

    private fun parseWebsite(contentValues: ContentValues) {
        val website = contentValues.getAsString(CommonDataKinds.Website.DATA1) ?: return
        contact!!.websites.add(website)
    }

    private fun parseNote(contentValues: ContentValues) {
        val note = contentValues.getAsString(CommonDataKinds.Note.DATA1) ?: return
        contact!!.notes = note
    }

    private fun startTakePhotoIntent() {
        val uri = getCachePhotoUri()
        lastPhotoIntentUri = uri
        Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
            putExtra(MediaStore.EXTRA_OUTPUT, uri)
            if (resolveActivity(packageManager) != null) {
                startActivityForResult(this, INTENT_TAKE_PHOTO)
            } else {
                toast(R.string.no_app_found)
            }
        }
    }

    private fun startChoosePhotoIntent() {
        val uri = getCachePhotoUri()
        lastPhotoIntentUri = uri
        Intent(Intent.ACTION_PICK).apply {
            type = "image/*"
            clipData = ClipData("Attachment", arrayOf("text/uri-list"), ClipData.Item(uri))
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
            putExtra(MediaStore.EXTRA_OUTPUT, uri)
            if (resolveActivity(packageManager) != null) {
                startActivityForResult(this, INTENT_CHOOSE_PHOTO)
            } else {
                toast(R.string.no_app_found)
            }
        }
    }

    private fun getPhoneNumberTypeId(value: String) = when (value) {
        getString(R.string.mobile) -> CommonDataKinds.Phone.TYPE_MOBILE
        getString(R.string.home) -> CommonDataKinds.Phone.TYPE_HOME
        getString(R.string.work) -> CommonDataKinds.Phone.TYPE_WORK
        getString(R.string.main_number) -> CommonDataKinds.Phone.TYPE_MAIN
        getString(R.string.work_fax) -> CommonDataKinds.Phone.TYPE_FAX_WORK
        getString(R.string.home_fax) -> CommonDataKinds.Phone.TYPE_FAX_HOME
        getString(R.string.pager) -> CommonDataKinds.Phone.TYPE_PAGER
        else -> CommonDataKinds.Phone.TYPE_OTHER
    }

    private fun getEmailTypeId(value: String) = when (value) {
        getString(R.string.home) -> CommonDataKinds.Email.TYPE_HOME
        getString(R.string.work) -> CommonDataKinds.Email.TYPE_WORK
        getString(R.string.mobile) -> CommonDataKinds.Email.TYPE_MOBILE
        else -> CommonDataKinds.Email.TYPE_OTHER
    }

    private fun getEventTypeId(value: String) = when (value) {
        getString(R.string.birthday) -> CommonDataKinds.Event.TYPE_BIRTHDAY
        getString(R.string.anniversary) -> CommonDataKinds.Event.TYPE_ANNIVERSARY
        else -> CommonDataKinds.Event.TYPE_OTHER
    }

    private fun getAddressTypeId(value: String) = when (value) {
        getString(R.string.home) -> CommonDataKinds.StructuredPostal.TYPE_HOME
        getString(R.string.work) -> CommonDataKinds.StructuredPostal.TYPE_WORK
        else -> CommonDataKinds.StructuredPostal.TYPE_OTHER
    }
}
