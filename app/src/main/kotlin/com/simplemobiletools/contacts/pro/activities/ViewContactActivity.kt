package com.simplemobiletools.contacts.pro.activities

import android.content.ActivityNotFoundException
import android.content.ContentUris
import android.content.Intent
import android.media.AudioManager
import android.media.RingtoneManager
import android.net.Uri
import android.os.Bundle
import android.provider.ContactsContract
import android.view.View
import android.view.WindowInsetsController
import android.view.WindowManager
import android.widget.RelativeLayout
import androidx.core.view.isVisible
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.FitCenter
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.request.RequestOptions
import com.simplemobiletools.commons.dialogs.CallConfirmationDialog
import com.simplemobiletools.commons.dialogs.ConfirmationDialog
import com.simplemobiletools.commons.dialogs.SelectAlarmSoundDialog
import com.simplemobiletools.commons.extensions.*
import com.simplemobiletools.commons.helpers.*
import com.simplemobiletools.commons.models.PhoneNumber
import com.simplemobiletools.contacts.pro.R
import com.simplemobiletools.contacts.pro.dialogs.ChooseSocialDialog
import com.simplemobiletools.contacts.pro.dialogs.ManageVisibleFieldsDialog
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
    private var duplicateInitialized = false
    private val mergeDuplicate: Boolean by lazy {
        config.mergeDuplicateContacts
    }

    private val COMPARABLE_PHONE_NUMBER_LENGTH = 9

    override fun onCreate(savedInstanceState: Bundle?) {
        showTransparentTop = true
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_view_contact)

        if (checkAppSideloading()) {
            return
        }

        showFields = config.showContactFields
        contact_wrapper.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
        setupMenu()

        if (isRPlus()) {
            window.insetsController?.setSystemBarsAppearance(0, WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS)
        }
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

    override fun onBackPressed() {
        if (contact_photo_big.alpha == 1f) {
            hideBigContactPhoto()
        } else {
            super.onBackPressed()
        }
    }

    private fun setupMenu() {
        (contact_appbar.layoutParams as RelativeLayout.LayoutParams).topMargin = statusBarHeight
        contact_toolbar.menu.apply {
            findItem(R.id.share).setOnMenuItemClickListener {
                if (fullContact != null) {
                    shareContact(fullContact!!)
                }
                true
            }

            findItem(R.id.edit).setOnMenuItemClickListener {
                launchEditContact(contact!!)
                true
            }

            findItem(R.id.open_with).setOnMenuItemClickListener {
                openWith()
                true
            }

            findItem(R.id.delete).setOnMenuItemClickListener {
                deleteContactFromAllSources()
                true
            }

            findItem(R.id.manage_visible_fields).setOnMenuItemClickListener {
                ManageVisibleFieldsDialog(this@ViewContactActivity) {
                    showFields = config.showContactFields
                    ensureBackgroundThread {
                        initContact()
                    }
                }
                true
            }
        }

        contact_toolbar.setNavigationOnClickListener {
            finish()
        }
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

        if (contact!!.photoUri.isEmpty() && contact!!.photo == null) {
            showPhotoPlaceholder(contact_photo)
            contact_photo_bottom_shadow.beGone()
        } else {
            updateContactPhoto(contact!!.photoUri, contact_photo, contact_photo_bottom_shadow, contact!!.photo)
            val options = RequestOptions()
                .transform(FitCenter(), RoundedCorners(resources.getDimension(R.dimen.normal_margin).toInt()))

            Glide.with(this)
                .load(contact!!.photo ?: currentContactPhotoPath)
                .apply(options)
                .into(contact_photo_big)

            contact_photo.setOnClickListener {
                contact_photo_big.alpha = 0f
                contact_photo_big.beVisible()
                contact_photo_big.animate().alpha(1f).start()
            }

            contact_photo_big.setOnClickListener {
                hideBigContactPhoto()
            }
        }

        val textColor = getProperTextColor()
        arrayOf(
            contact_name_image, contact_numbers_image, contact_emails_image, contact_addresses_image, contact_events_image, contact_source_image,
            contact_notes_image, contact_ringtone_image, contact_organization_image, contact_websites_image, contact_groups_image
        ).forEach {
            it.applyColorFilter(textColor)
        }

        contact_send_sms.setOnClickListener { trySendSMS() }
        contact_start_call.setOnClickListener { tryStartCall(contact!!) }
        contact_send_email.setOnClickListener { trySendEmail() }

        contact_send_sms.setOnLongClickListener { toast(R.string.send_sms); true; }
        contact_start_call.setOnLongClickListener { toast(R.string.call_contact); true; }
        contact_send_email.setOnLongClickListener { toast(R.string.send_email); true; }

        updateTextColors(contact_scrollview)
        contact_toolbar.menu.findItem(R.id.open_with).isVisible = contact?.isPrivate() == false
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
                    duplicateInitialized = true
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
        setupRingtone()
        setupOrganization()
        updateTextColors(contact_scrollview)
    }

    private fun launchEditContact(contact: Contact) {
        wasEditLaunched = true
        duplicateInitialized = false
        editContact(contact)
    }

    private fun openWith() {
        val uri = getContactPublicUri(contact!!)
        launchViewContactIntent(uri)
    }

    private fun setupFavorite() {
        contact_toggle_favorite.apply {
            beVisible()
            tag = contact!!.starred
            setImageDrawable(getStarDrawable(tag == 1))

            setOnClickListener {
                val newIsStarred = if (tag == 1) 0 else 1
                ensureBackgroundThread {
                    val contacts = arrayListOf(contact!!)
                    if (newIsStarred == 1) {
                        ContactsHelper(context).addFavorites(contacts)
                    } else {
                        ContactsHelper(context).removeFavorites(contacts)
                    }
                }
                contact!!.starred = newIsStarred
                tag = contact!!.starred
                setImageDrawable(getStarDrawable(tag == 1))
            }

            setOnLongClickListener { toast(R.string.toggle_favorite); true; }
        }
    }

    private fun setupNames() {
        var displayName = contact!!.getNameToDisplay()
        if (contact!!.nickname.isNotEmpty()) {
            displayName += " (${contact!!.nickname})"
        }

        val showNameFields = showFields and SHOW_PREFIX_FIELD != 0 || showFields and SHOW_FIRST_NAME_FIELD != 0 || showFields and SHOW_MIDDLE_NAME_FIELD != 0 ||
            showFields and SHOW_SURNAME_FIELD != 0 || showFields and SHOW_SUFFIX_FIELD != 0

        contact_name.text = displayName
        contact_name.copyOnLongClick(displayName)
        contact_name.beVisibleIf(displayName.isNotEmpty() && !contact!!.isABusinessContact() && showNameFields)
        contact_name_image.beInvisibleIf(contact_name.isGone())
    }

    private fun setupPhoneNumbers() {
        var phoneNumbers = contact!!.phoneNumbers.toMutableSet() as LinkedHashSet<PhoneNumber>

        if (mergeDuplicate) {
            duplicateContacts.forEach {
                phoneNumbers.addAll(it.phoneNumbers)
            }
        }

        if (duplicateInitialized) {
            val contactDefaultsNumbers = contact!!.phoneNumbers.filter { it.isPrimary }
            val duplicateContactsDefaultNumbers = duplicateContacts.flatMap { it.phoneNumbers }.filter { it.isPrimary }
            val defaultNumbers = (contactDefaultsNumbers + duplicateContactsDefaultNumbers).toSet()

            if (defaultNumbers.size > 1) {
                phoneNumbers.forEach { it.isPrimary = false }
            } else if (defaultNumbers.size == 1) {
                if (mergeDuplicate) {
                    val defaultNumber = defaultNumbers.first()
                    val candidate = phoneNumbers.find { it.normalizedNumber == defaultNumber.normalizedNumber && !it.isPrimary }
                    candidate?.isPrimary = true
                } else {
                    duplicateContactsDefaultNumbers.forEach { defaultNumber ->
                        val candidate = phoneNumbers.find { it.normalizedNumber == defaultNumber.normalizedNumber && !it.isPrimary }
                        candidate?.isPrimary = true
                    }
                }
            }
        }

        phoneNumbers = phoneNumbers.distinctBy {
            if (it.normalizedNumber.length >= COMPARABLE_PHONE_NUMBER_LENGTH) {
                it.normalizedNumber.substring(it.normalizedNumber.length - COMPARABLE_PHONE_NUMBER_LENGTH)
            } else {
                it.normalizedNumber
            }
        }.toMutableSet() as LinkedHashSet<PhoneNumber>

        phoneNumbers = phoneNumbers.sortedBy { it.type }.toMutableSet() as LinkedHashSet<PhoneNumber>
        fullContact!!.phoneNumbers = phoneNumbers.toMutableList() as ArrayList<PhoneNumber>
        contact_numbers_holder.removeAllViews()

        if (phoneNumbers.isNotEmpty() && showFields and SHOW_PHONE_NUMBERS_FIELD != 0) {
            phoneNumbers.forEach { phoneNumber ->
                layoutInflater.inflate(R.layout.item_view_phone_number, contact_numbers_holder, false).apply {
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

                    contact_number_holder.default_toggle_icon.isVisible = duplicateInitialized && phoneNumber.isPrimary
                }
            }
            contact_numbers_image.beVisible()
            contact_numbers_holder.beVisible()
        } else {
            contact_numbers_image.beGone()
            contact_numbers_holder.beGone()
        }

        // make sure the Call and SMS buttons are visible if any phone number is shown
        if (phoneNumbers.isNotEmpty()) {
            contact_send_sms.beVisible()
            contact_start_call.beVisible()
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

        if (mergeDuplicate) {
            duplicateContacts.forEach {
                addresses.addAll(it.addresses)
            }
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

        if (mergeDuplicate) {
            duplicateContacts.forEach {
                IMs.addAll(it.IMs)
            }
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

        if (mergeDuplicate) {
            duplicateContacts.forEach {
                events.addAll(it.events)
            }
        }

        events = events.sortedBy { it.type }.toMutableSet() as LinkedHashSet<Event>
        fullContact!!.events = events.toMutableList() as ArrayList<Event>
        contact_events_holder.removeAllViews()

        if (events.isNotEmpty() && showFields and SHOW_EVENTS_FIELD != 0) {
            events.forEach {
                layoutInflater.inflate(R.layout.item_view_event, contact_events_holder, false).apply {
                    contact_events_holder.addView(this)
                    it.value.getDateTimeFromDateString(true, contact_event)
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

        if (mergeDuplicate) {
            duplicateContacts.forEach {
                websites.addAll(it.websites)
            }
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

        if (mergeDuplicate) {
            duplicateContacts.forEach {
                groups.addAll(it.groups)
            }
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

            if (mergeDuplicate) {
                duplicateContacts.forEach {
                    sources[it] = getPublicContactSourceSync(it.source, contactSources)
                }
            }

            if (sources.size > 1) {
                sources = sources.toList().sortedBy { (key, value) -> value.toLowerCase() }.toMap() as LinkedHashMap<Contact, String>
            }

            for ((key, value) in sources) {
                layoutInflater.inflate(R.layout.item_view_contact_source, contact_sources_holder, false).apply {
                    contact_source.text = if (value == "") getString(R.string.phone_storage) else value
                    contact_source.copyOnLongClick(value)
                    contact_sources_holder.addView(this)

                    contact_source.setOnClickListener {
                        launchEditContact(key)
                    }

                    if (value.toLowerCase() == WHATSAPP) {
                        contact_source_image.setImageDrawable(getPackageDrawable(WHATSAPP_PACKAGE))
                        contact_source_image.beVisible()
                        contact_source_image.setOnClickListener {
                            showSocialActions(key.id)
                        }
                    }

                    if (value.toLowerCase() == SIGNAL) {
                        contact_source_image.setImageDrawable(getPackageDrawable(SIGNAL_PACKAGE))
                        contact_source_image.beVisible()
                        contact_source_image.setOnClickListener {
                            showSocialActions(key.id)
                        }
                    }

                    if (value.toLowerCase() == VIBER) {
                        contact_source_image.setImageDrawable(getPackageDrawable(VIBER_PACKAGE))
                        contact_source_image.beVisible()
                        contact_source_image.setOnClickListener {
                            showSocialActions(key.id)
                        }
                    }

                    if (value.toLowerCase() == TELEGRAM) {
                        contact_source_image.setImageDrawable(getPackageDrawable(TELEGRAM_PACKAGE))
                        contact_source_image.beVisible()
                        contact_source_image.setOnClickListener {
                            showSocialActions(key.id)
                        }
                    }

                    if (value.toLowerCase() == THREEMA) {
                        contact_source_image.setImageDrawable(getPackageDrawable(THREEMA_PACKAGE))
                        contact_source_image.beVisible()
                        contact_source_image.setOnClickListener {
                            showSocialActions(key.id)
                        }
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

    private fun setupRingtone() {
        if (showFields and SHOW_RINGTONE_FIELD != 0) {
            contact_ringtone_image.beVisible()
            contact_ringtone.beVisible()

            val ringtone = contact!!.ringtone
            if (ringtone?.isEmpty() == true) {
                contact_ringtone.text = getString(R.string.no_sound)
            } else if (ringtone?.isNotEmpty() == true && ringtone != getDefaultRingtoneUri().toString()) {
                if (ringtone == SILENT) {
                    contact_ringtone.text = getString(R.string.no_sound)
                } else {
                    systemRingtoneSelected(Uri.parse(ringtone))
                }
            } else {
                contact_ringtone_image.beGone()
                contact_ringtone.beGone()
                return
            }

            contact_ringtone.copyOnLongClick(contact_ringtone.text.toString())

            contact_ringtone.setOnClickListener {
                val ringtonePickerIntent = getRingtonePickerIntent()
                try {
                    startActivityForResult(ringtonePickerIntent, INTENT_SELECT_RINGTONE)
                } catch (e: Exception) {
                    val currentRingtone = contact!!.ringtone ?: getDefaultAlarmSound(RingtoneManager.TYPE_RINGTONE).uri
                    SelectAlarmSoundDialog(this@ViewContactActivity,
                        currentRingtone,
                        AudioManager.STREAM_RING,
                        PICK_RINGTONE_INTENT_ID,
                        RingtoneManager.TYPE_RINGTONE,
                        true,
                        onAlarmPicked = {
                            contact_ringtone.text = it?.title
                            ringtoneUpdated(it?.uri)
                        },
                        onAlarmSoundDeleted = {}
                    )
                }
            }
        } else {
            contact_ringtone_image.beGone()
            contact_ringtone.beGone()
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

    private fun showSocialActions(contactId: Int) {
        ensureBackgroundThread {
            val actions = getSocialActions(contactId)
            runOnUiThread {
                if (!isDestroyed && !isFinishing) {
                    ChooseSocialDialog(this@ViewContactActivity, actions) { action ->
                        Intent(Intent.ACTION_VIEW).apply {
                            val uri = ContentUris.withAppendedId(ContactsContract.Data.CONTENT_URI, action.dataId)
                            setDataAndType(uri, action.mimetype)
                            flags = Intent.FLAG_ACTIVITY_CLEAR_TASK
                            try {
                                startActivity(this)
                            } catch (e: SecurityException) {
                                handlePermission(PERMISSION_CALL_PHONE) { success ->
                                    if (success) {
                                        startActivity(this)
                                    } else {
                                        toast(R.string.no_phone_call_permission)
                                    }
                                }
                            } catch (e: ActivityNotFoundException) {
                                toast(R.string.no_app_found)
                            } catch (e: Exception) {
                                showErrorToast(e)
                            }
                        }
                    }
                }
            }
        }
    }

    override fun customRingtoneSelected(ringtonePath: String) {
        contact_ringtone.text = ringtonePath.getFilenameFromPath()
        ringtoneUpdated(ringtonePath)
    }

    override fun systemRingtoneSelected(uri: Uri?) {
        val contactRingtone = RingtoneManager.getRingtone(this, uri)
        contact_ringtone.text = contactRingtone.getTitle(this)
        ringtoneUpdated(uri?.toString() ?: "")
    }

    private fun ringtoneUpdated(path: String?) {
        contact!!.ringtone = path

        ensureBackgroundThread {
            if (contact!!.isPrivate()) {
                LocalContactsHelper(this).updateRingtone(contact!!.contactId, path ?: "")
            } else {
                ContactsHelper(this).updateRingtone(contact!!.contactId.toString(), path ?: "")
            }
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

    private fun getStarDrawable(on: Boolean) = resources.getDrawable(if (on) R.drawable.ic_star_vector else R.drawable.ic_star_outline_vector)

    private fun hideBigContactPhoto() {
        contact_photo_big.animate().alpha(0f).withEndAction { contact_photo_big.beGone() }.start()
    }

    private fun View.copyOnLongClick(value: String) {
        setOnLongClickListener {
            copyToClipboard(value)
            toast(R.string.value_copied_to_clipboard)
            true
        }
    }
}
