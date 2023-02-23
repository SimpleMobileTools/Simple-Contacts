package com.simplemobiletools.contacts.pro.activities

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ContentValues
import android.content.Intent
import android.graphics.Bitmap
import android.media.AudioManager
import android.media.RingtoneManager
import android.net.Uri
import android.os.Bundle
import android.provider.ContactsContract.CommonDataKinds
import android.provider.ContactsContract.CommonDataKinds.*
import android.provider.MediaStore
import android.telephony.PhoneNumberUtils
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.EditText
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.simplemobiletools.commons.dialogs.ConfirmationAdvancedDialog
import com.simplemobiletools.commons.dialogs.RadioGroupDialog
import com.simplemobiletools.commons.dialogs.SelectAlarmSoundDialog
import com.simplemobiletools.commons.extensions.*
import com.simplemobiletools.commons.helpers.*
import com.simplemobiletools.commons.models.PhoneNumber
import com.simplemobiletools.commons.models.RadioItem
import com.simplemobiletools.commons.models.contacts.*
import com.simplemobiletools.commons.models.contacts.Email
import com.simplemobiletools.commons.models.contacts.Event
import com.simplemobiletools.commons.models.contacts.Organization
import com.simplemobiletools.contacts.pro.R
import com.simplemobiletools.contacts.pro.dialogs.CustomLabelDialog
import com.simplemobiletools.contacts.pro.dialogs.ManageVisibleFieldsDialog
import com.simplemobiletools.contacts.pro.dialogs.MyDatePickerDialog
import com.simplemobiletools.contacts.pro.dialogs.SelectGroupsDialog
import com.simplemobiletools.contacts.pro.extensions.*
import com.simplemobiletools.contacts.pro.helpers.*
import kotlinx.android.synthetic.main.activity_edit_contact.*
import kotlinx.android.synthetic.main.item_edit_address.view.*
import kotlinx.android.synthetic.main.item_edit_email.view.*
import kotlinx.android.synthetic.main.item_edit_group.view.*
import kotlinx.android.synthetic.main.item_edit_im.view.*
import kotlinx.android.synthetic.main.item_edit_phone_number.view.*
import kotlinx.android.synthetic.main.item_edit_website.view.*
import kotlinx.android.synthetic.main.item_event.view.*

class EditContactActivity : ContactActivity() {
    private val INTENT_TAKE_PHOTO = 1
    private val INTENT_CHOOSE_PHOTO = 2
    private val INTENT_CROP_PHOTO = 3

    private val TAKE_PHOTO = 1
    private val CHOOSE_PHOTO = 2
    private val REMOVE_PHOTO = 3

    private var mLastSavePromptTS = 0L
    private var wasActivityInitialized = false
    private var lastPhotoIntentUri: Uri? = null
    private var isSaving = false
    private var isThirdPartyIntent = false
    private var highlightLastPhoneNumber = false
    private var highlightLastEmail = false
    private var numberViewToColor: EditText? = null
    private var emailViewToColor: EditText? = null
    private var originalContactSource = ""

    enum class PrimaryNumberStatus {
        UNCHANGED, STARRED, UNSTARRED
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        showTransparentTop = true
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_contact)

        if (checkAppSideloading()) {
            return
        }

        contact_wrapper.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
        setupMenu()

        val action = intent.action
        isThirdPartyIntent = action == Intent.ACTION_EDIT || action == Intent.ACTION_INSERT || action == ADD_NEW_CONTACT_NUMBER
        val isFromSimpleContacts = intent.getBooleanExtra(IS_FROM_SIMPLE_CONTACTS, false)
        if (isThirdPartyIntent && !isFromSimpleContacts) {
            handlePermission(PERMISSION_READ_CONTACTS) {
                if (it) {
                    handlePermission(PERMISSION_WRITE_CONTACTS) {
                        if (it) {
                            initContact()
                        } else {
                            toast(R.string.no_contacts_permission)
                            hideKeyboard()
                            finish()
                        }
                    }
                } else {
                    toast(R.string.no_contacts_permission)
                    hideKeyboard()
                    finish()
                }
            }
        } else {
            initContact()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, resultData: Intent?) {
        super.onActivityResult(requestCode, resultCode, resultData)
        if (resultCode == RESULT_OK) {
            when (requestCode) {
                INTENT_TAKE_PHOTO, INTENT_CHOOSE_PHOTO -> startCropPhotoIntent(lastPhotoIntentUri, resultData?.data)
                INTENT_CROP_PHOTO -> updateContactPhoto(lastPhotoIntentUri.toString(), contact_photo, contact_photo_bottom_shadow)
            }
        }
    }

    private fun initContact() {
        var contactId = intent.getIntExtra(CONTACT_ID, 0)
        val action = intent.action
        if (contactId == 0 && (action == Intent.ACTION_EDIT || action == ADD_NEW_CONTACT_NUMBER)) {
            val data = intent.data
            if (data != null && data.path != null) {
                val rawId = if (data.path!!.contains("lookup")) {
                    if (data.pathSegments.last().startsWith("local_")) {
                        data.path!!.substringAfter("local_").toInt()
                    } else {
                        getLookupUriRawId(data)
                    }
                } else {
                    getContactUriRawId(data)
                }

                if (rawId != -1) {
                    contactId = rawId
                }
            }
        }

        if (contactId != 0) {
            ensureBackgroundThread {
                contact = ContactsHelper(this).getContactWithId(contactId, intent.getBooleanExtra(IS_PRIVATE, false))
                if (contact == null) {
                    toast(R.string.unknown_error_occurred)
                    hideKeyboard()
                    finish()
                } else {
                    runOnUiThread {
                        gotContact()
                    }
                }
            }
        } else {
            gotContact()
        }
    }

    private fun gotContact() {
        contact_scrollview.beVisible()
        if (contact == null) {
            setupNewContact()
        } else {
            setupEditContact()
            originalRingtone = contact?.ringtone
        }

        val action = intent.action
        if (((contact!!.id == 0 && action == Intent.ACTION_INSERT) || action == ADD_NEW_CONTACT_NUMBER) && intent.extras != null) {
            val phoneNumber = getPhoneNumberFromIntent(intent)
            if (phoneNumber != null) {
                contact!!.phoneNumbers.add(PhoneNumber(phoneNumber, DEFAULT_PHONE_NUMBER_TYPE, "", phoneNumber.normalizePhoneNumber()))
                if (phoneNumber.isNotEmpty() && action == ADD_NEW_CONTACT_NUMBER) {
                    highlightLastPhoneNumber = true
                }
            }

            val email = intent.getStringExtra(KEY_EMAIL)
            if (email != null) {
                val newEmail = Email(email, DEFAULT_EMAIL_TYPE, "")
                contact!!.emails.add(newEmail)
                highlightLastEmail = true
            }

            val firstName = intent.extras!!.get(KEY_NAME)
            if (firstName != null) {
                contact!!.firstName = firstName.toString()
            }

            val data = intent.extras!!.getParcelableArrayList<ContentValues>("data")
            if (data != null) {
                parseIntentData(data)
            }
            setupEditContact()
        }

        setupTypePickers()
        setupRingtone()

        if (contact!!.photoUri.isEmpty() && contact!!.photo == null) {
            showPhotoPlaceholder(contact_photo)
            contact_photo_bottom_shadow.beGone()
        } else {
            updateContactPhoto(contact!!.photoUri, contact_photo, contact_photo_bottom_shadow, contact!!.photo)
        }

        val textColor = getProperTextColor()
        arrayOf(
            contact_name_image, contact_numbers_image, contact_emails_image, contact_addresses_image, contact_ims_image, contact_events_image,
            contact_notes_image, contact_ringtone_image, contact_organization_image, contact_websites_image, contact_groups_image, contact_source_image
        ).forEach {
            it.applyColorFilter(textColor)
        }

        val properPrimaryColor = getProperPrimaryColor()
        arrayOf(
            contact_numbers_add_new,
            contact_emails_add_new,
            contact_addresses_add_new,
            contact_ims_add_new,
            contact_events_add_new,
            contact_websites_add_new,
            contact_groups_add_new
        ).forEach {
            it.applyColorFilter(properPrimaryColor)
        }

        arrayOf(
            contact_numbers_add_new.background,
            contact_emails_add_new.background,
            contact_addresses_add_new.background,
            contact_ims_add_new.background,
            contact_events_add_new.background,
            contact_websites_add_new.background,
            contact_groups_add_new.background
        ).forEach {
            it.applyColorFilter(textColor)
        }

        contact_toggle_favorite.setOnClickListener { toggleFavorite() }
        contact_photo.setOnClickListener { trySetPhoto() }
        contact_change_photo.setOnClickListener { trySetPhoto() }
        contact_numbers_add_new.setOnClickListener { addNewPhoneNumberField() }
        contact_emails_add_new.setOnClickListener { addNewEmailField() }
        contact_addresses_add_new.setOnClickListener { addNewAddressField() }
        contact_ims_add_new.setOnClickListener { addNewIMField() }
        contact_events_add_new.setOnClickListener { addNewEventField() }
        contact_websites_add_new.setOnClickListener { addNewWebsiteField() }
        contact_groups_add_new.setOnClickListener { showSelectGroupsDialog() }
        contact_source.setOnClickListener { showSelectContactSourceDialog() }

        contact_change_photo.setOnLongClickListener { toast(R.string.change_photo); true; }

        setupFieldVisibility()

        contact_toggle_favorite.apply {
            setImageDrawable(getStarDrawable(contact!!.starred == 1))
            tag = contact!!.starred
            setOnLongClickListener { toast(R.string.toggle_favorite); true; }
        }

        updateTextColors(contact_scrollview)
        numberViewToColor?.setTextColor(properPrimaryColor)
        emailViewToColor?.setTextColor(properPrimaryColor)
        wasActivityInitialized = true

        contact_toolbar.menu.apply {
            findItem(R.id.delete).isVisible = contact?.id != 0
            findItem(R.id.share).isVisible = contact?.id != 0
            findItem(R.id.open_with).isVisible = contact?.id != 0 && contact?.isPrivate() == false
        }
    }

    override fun onBackPressed() {
        if (System.currentTimeMillis() - mLastSavePromptTS > SAVE_DISCARD_PROMPT_INTERVAL && hasContactChanged()) {
            mLastSavePromptTS = System.currentTimeMillis()
            ConfirmationAdvancedDialog(this, "", R.string.save_before_closing, R.string.save, R.string.discard) {
                if (it) {
                    saveContact()
                } else {
                    super.onBackPressed()
                }
            }
        } else {
            super.onBackPressed()
        }
    }

    private fun setupMenu() {
        (contact_appbar.layoutParams as RelativeLayout.LayoutParams).topMargin = statusBarHeight
        contact_toolbar.menu.apply {
            findItem(R.id.save).setOnMenuItemClickListener {
                saveContact()
                true
            }

            findItem(R.id.share).setOnMenuItemClickListener {
                shareContact(contact!!)
                true
            }

            findItem(R.id.open_with).setOnMenuItemClickListener {
                openWith()
                true
            }

            findItem(R.id.delete).setOnMenuItemClickListener {
                deleteContact()
                true
            }

            findItem(R.id.manage_visible_fields).setOnMenuItemClickListener {
                ManageVisibleFieldsDialog(this@EditContactActivity) {
                    initContact()
                }
                true
            }
        }

        contact_toolbar.setNavigationOnClickListener {
            hideKeyboard()
            finish()
        }
    }

    private fun hasContactChanged() = contact != null && contact != fillContactValues() || originalRingtone != contact?.ringtone

    private fun openWith() {
        Intent().apply {
            action = Intent.ACTION_EDIT
            data = getContactPublicUri(contact!!)
            launchActivityIntent(this)
        }
    }

    private fun startCropPhotoIntent(primaryUri: Uri?, backupUri: Uri?) {
        if (primaryUri == null) {
            toast(R.string.unknown_error_occurred)
            return
        }

        var imageUri = primaryUri
        var bitmap = MediaStore.Images.Media.getBitmap(contentResolver, primaryUri)
        if (bitmap == null) {
            imageUri = backupUri
            try {
                bitmap = MediaStore.Images.Media.getBitmap(contentResolver, backupUri) ?: return
            } catch (e: Exception) {
                showErrorToast(e)
                return
            }

            // we might have received an URI which we have no permission to send further, so just copy the received image in a new uri (for example from Google Photos)
            val newFile = getCachePhoto()
            val fos = newFile.outputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos)
            imageUri = getCachePhotoUri(newFile)
        }

        hideKeyboard()
        lastPhotoIntentUri = getCachePhotoUri()
        Intent("com.android.camera.action.CROP").apply {
            setDataAndType(imageUri, "image/*")
            putExtra(MediaStore.EXTRA_OUTPUT, lastPhotoIntentUri)
            putExtra("outputX", 512)
            putExtra("outputY", 512)
            putExtra("aspectX", 1)
            putExtra("aspectY", 1)
            putExtra("crop", "true")
            putExtra("scale", "true")
            putExtra("scaleUpIfNeeded", "true")
            clipData = ClipData("Attachment", arrayOf("text/primaryUri-list"), ClipData.Item(lastPhotoIntentUri))
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)

            try {
                startActivityForResult(this, INTENT_CROP_PHOTO)
            } catch (e: ActivityNotFoundException) {
                toast(R.string.no_app_found)
            } catch (e: Exception) {
                showErrorToast(e)
            }
        }
    }

    private fun setupFieldVisibility() {
        val showFields = baseConfig.showContactFields
        if (showFields and (SHOW_PREFIX_FIELD or SHOW_FIRST_NAME_FIELD or SHOW_MIDDLE_NAME_FIELD or SHOW_SURNAME_FIELD or SHOW_SUFFIX_FIELD) == 0) {
            contact_name_image.beInvisible()
        }

        contact_prefix.beVisibleIf(showFields and SHOW_PREFIX_FIELD != 0)
        contact_first_name.beVisibleIf(showFields and SHOW_FIRST_NAME_FIELD != 0)
        contact_middle_name.beVisibleIf(showFields and SHOW_MIDDLE_NAME_FIELD != 0)
        contact_surname.beVisibleIf(showFields and SHOW_SURNAME_FIELD != 0)
        contact_suffix.beVisibleIf(showFields and SHOW_SUFFIX_FIELD != 0)
        contact_nickname.beVisibleIf(showFields and SHOW_NICKNAME_FIELD != 0)

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

        val areIMsVisible = showFields and SHOW_IMS_FIELD != 0
        contact_ims_image.beVisibleIf(areIMsVisible)
        contact_ims_holder.beVisibleIf(areIMsVisible)
        contact_ims_add_new.beVisibleIf(areIMsVisible)

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

        val isRingtoneVisible = showFields and SHOW_RINGTONE_FIELD != 0
        contact_ringtone.beVisibleIf(isRingtoneVisible)
        contact_ringtone_image.beVisibleIf(isRingtoneVisible)
    }

    private fun setupEditContact() {
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN)
        setupNames()
        setupPhoneNumbers()
        setupEmails()
        setupAddresses()
        setupIMs()
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
            contact_nickname.setText(nickname)
        }
    }

    private fun setupPhoneNumbers() {
        val phoneNumbers = contact!!.phoneNumbers

        phoneNumbers.forEachIndexed { index, number ->
            var numberHolder = contact_numbers_holder.getChildAt(index)
            if (numberHolder == null) {
                numberHolder = layoutInflater.inflate(R.layout.item_edit_phone_number, contact_numbers_holder, false)
                contact_numbers_holder.addView(numberHolder)
            }

            numberHolder!!.apply {
                contact_number.setText(number.value)
                contact_number.tag = number.normalizedNumber
                setupPhoneNumberTypePicker(contact_number_type, number.type, number.label)
                if (highlightLastPhoneNumber && index == phoneNumbers.size - 1) {
                    numberViewToColor = contact_number
                }

                default_toggle_icon.tag = if (number.isPrimary) 1 else 0
            }
        }

        initNumberHolders()
    }

    private fun setDefaultNumber(selected: ImageView) {
        val numbersCount = contact_numbers_holder.childCount
        for (i in 0 until numbersCount) {
            val toggleIcon = contact_numbers_holder.getChildAt(i).default_toggle_icon
            if (toggleIcon != selected) {
                toggleIcon.tag = 0
            }
        }

        selected.tag = if (selected.tag == 1) 0 else 1

        initNumberHolders()
    }

    private fun initNumberHolders() {
        val numbersCount = contact_numbers_holder.childCount

        if (numbersCount == 1) {
            contact_numbers_holder.getChildAt(0).default_toggle_icon.beGone()
            return
        }

        for (i in 0 until numbersCount) {
            val toggleIcon = contact_numbers_holder.getChildAt(i).default_toggle_icon
            val isPrimary = toggleIcon.tag == 1

            val drawableId = if (isPrimary) {
                R.drawable.ic_star_vector
            } else {
                R.drawable.ic_star_outline_vector
            }

            val drawable = ContextCompat.getDrawable(this@EditContactActivity, drawableId)
            drawable?.apply {
                mutate()
                setTint(getProperTextColor())
            }

            toggleIcon.setImageDrawable(drawable)
            toggleIcon.beVisible()
            toggleIcon.setOnClickListener {
                setDefaultNumber(toggleIcon)
            }
        }
    }

    private fun setupEmails() {
        contact!!.emails.forEachIndexed { index, email ->
            var emailHolder = contact_emails_holder.getChildAt(index)
            if (emailHolder == null) {
                emailHolder = layoutInflater.inflate(R.layout.item_edit_email, contact_emails_holder, false)
                contact_emails_holder.addView(emailHolder)
            }

            emailHolder!!.apply {
                contact_email.setText(email.value)
                setupEmailTypePicker(contact_email_type, email.type, email.label)
                if (highlightLastEmail && index == contact!!.emails.size - 1) {
                    emailViewToColor = contact_email
                }
            }
        }
    }

    private fun setupAddresses() {
        contact!!.addresses.forEachIndexed { index, address ->
            var addressHolder = contact_addresses_holder.getChildAt(index)
            if (addressHolder == null) {
                addressHolder = layoutInflater.inflate(R.layout.item_edit_address, contact_addresses_holder, false)
                contact_addresses_holder.addView(addressHolder)
            }

            addressHolder!!.apply {
                contact_address.setText(address.value)
                setupAddressTypePicker(contact_address_type, address.type, address.label)
            }
        }
    }

    private fun setupIMs() {
        contact!!.IMs.forEachIndexed { index, IM ->
            var imHolder = contact_ims_holder.getChildAt(index)
            if (imHolder == null) {
                imHolder = layoutInflater.inflate(R.layout.item_edit_im, contact_ims_holder, false)
                contact_ims_holder.addView(imHolder)
            }

            imHolder!!.apply {
                contact_im.setText(IM.value)
                setupIMTypePicker(contact_im_type, IM.type, IM.label)
            }
        }
    }

    private fun setupNotes() {
        contact_notes.setText(contact!!.notes)
    }

    private fun setupRingtone() {
        contact_ringtone.setOnClickListener {
            hideKeyboard()
            val ringtonePickerIntent = getRingtonePickerIntent()
            try {
                startActivityForResult(ringtonePickerIntent, INTENT_SELECT_RINGTONE)
            } catch (e: Exception) {
                val currentRingtone = contact!!.ringtone ?: getDefaultAlarmSound(RingtoneManager.TYPE_RINGTONE).uri
                SelectAlarmSoundDialog(
                    this,
                    currentRingtone,
                    AudioManager.STREAM_RING,
                    PICK_RINGTONE_INTENT_ID,
                    RingtoneManager.TYPE_RINGTONE,
                    true,
                    onAlarmPicked = {
                        contact!!.ringtone = it?.uri
                        contact_ringtone.text = it?.title
                    },
                    onAlarmSoundDeleted = {}
                )
            }
        }

        val ringtone = contact!!.ringtone
        if (ringtone?.isEmpty() == true) {
            contact_ringtone.text = getString(R.string.no_sound)
        } else if (ringtone?.isNotEmpty() == true) {
            if (ringtone == SILENT) {
                contact_ringtone.text = getString(R.string.no_sound)
            } else {
                systemRingtoneSelected(Uri.parse(ringtone))
            }
        } else {
            val default = getDefaultAlarmSound(RingtoneManager.TYPE_RINGTONE)
            contact_ringtone.text = default.title
        }
    }

    private fun setupOrganization() {
        contact_organization_company.setText(contact!!.organization.company)
        contact_organization_job_position.setText(contact!!.organization.jobPosition)
    }

    private fun setupWebsites() {
        contact!!.websites.forEachIndexed { index, website ->
            var websitesHolder = contact_websites_holder.getChildAt(index)
            if (websitesHolder == null) {
                websitesHolder = layoutInflater.inflate(R.layout.item_edit_website, contact_websites_holder, false)
                contact_websites_holder.addView(websitesHolder)
            }

            websitesHolder!!.contact_website.setText(website)
        }
    }

    private fun setupEvents() {
        contact!!.events.forEachIndexed { index, event ->
            var eventHolder = contact_events_holder.getChildAt(index)
            if (eventHolder == null) {
                eventHolder = layoutInflater.inflate(R.layout.item_event, contact_events_holder, false)
                contact_events_holder.addView(eventHolder)
            }

            (eventHolder as ViewGroup).apply {
                val contactEvent = contact_event.apply {
                    event.value.getDateTimeFromDateString(true, this)
                    tag = event.value
                    alpha = 1f
                }

                setupEventTypePicker(this, event.type)

                contact_event_remove.apply {
                    beVisible()
                    applyColorFilter(getProperPrimaryColor())
                    background.applyColorFilter(getProperTextColor())
                    setOnClickListener {
                        resetContactEvent(contactEvent, this)
                    }
                }
            }
        }
    }

    private fun setupGroups() {
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
                    setTextColor(getProperTextColor())
                    tag = group.id
                    alpha = 1f
                }

                setOnClickListener {
                    showSelectGroupsDialog()
                }

                contact_group_remove.apply {
                    beVisible()
                    applyColorFilter(getProperPrimaryColor())
                    background.applyColorFilter(getProperTextColor())
                    setOnClickListener {
                        removeGroup(group.id!!)
                    }
                }
            }
        }

        if (groups.isEmpty()) {
            layoutInflater.inflate(R.layout.item_edit_group, contact_groups_holder, false).apply {
                contact_group.apply {
                    alpha = 0.5f
                    text = getString(R.string.no_groups)
                    setTextColor(getProperTextColor())
                }

                contact_groups_holder.addView(this)
                contact_group_remove.beGone()
                setOnClickListener {
                    showSelectGroupsDialog()
                }
            }
        }
    }

    private fun setupContactSource() {
        originalContactSource = contact!!.source
        getPublicContactSource(contact!!.source) {
            contact_source.text = if (it == "") getString(R.string.phone_storage) else it
        }
    }

    private fun setupNewContact() {
        originalContactSource = if (hasContactPermissions()) baseConfig.lastUsedContactSource else SMT_PRIVATE
        contact = getEmptyContact()
        getPublicContactSource(contact!!.source) {
            contact_source.text = if (it == "") getString(R.string.phone_storage) else it
        }

        // if the last used contact source is not available anymore, use the first available one. Could happen at ejecting SIM card
        ContactsHelper(this).getSaveableContactSources { sources ->
            val sourceNames = sources.map { it.name }
            if (!sourceNames.contains(originalContactSource)) {
                originalContactSource = sourceNames.first()
                contact?.source = originalContactSource
                getPublicContactSource(contact!!.source) {
                    contact_source.text = if (it == "") getString(R.string.phone_storage) else it
                }
            }
        }
    }

    private fun setupTypePickers() {
        if (contact!!.phoneNumbers.isEmpty()) {
            val numberHolder = contact_numbers_holder.getChildAt(0)
            (numberHolder as? ViewGroup)?.contact_number_type?.apply {
                setupPhoneNumberTypePicker(this, DEFAULT_PHONE_NUMBER_TYPE, "")
            }
        }

        if (contact!!.emails.isEmpty()) {
            val emailHolder = contact_emails_holder.getChildAt(0)
            (emailHolder as? ViewGroup)?.contact_email_type?.apply {
                setupEmailTypePicker(this, DEFAULT_EMAIL_TYPE, "")
            }
        }

        if (contact!!.addresses.isEmpty()) {
            val addressHolder = contact_addresses_holder.getChildAt(0)
            (addressHolder as? ViewGroup)?.contact_address_type?.apply {
                setupAddressTypePicker(this, DEFAULT_ADDRESS_TYPE, "")
            }
        }

        if (contact!!.IMs.isEmpty()) {
            val IMHolder = contact_ims_holder.getChildAt(0)
            (IMHolder as? ViewGroup)?.contact_im_type?.apply {
                setupIMTypePicker(this, DEFAULT_IM_TYPE, "")
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

    private fun setupPhoneNumberTypePicker(numberTypeField: TextView, type: Int, label: String) {
        numberTypeField.apply {
            text = getPhoneNumberTypeText(type, label)
            setOnClickListener {
                showNumberTypePicker(it as TextView)
            }
        }
    }

    private fun setupEmailTypePicker(emailTypeField: TextView, type: Int, label: String) {
        emailTypeField.apply {
            text = getEmailTypeText(type, label)
            setOnClickListener {
                showEmailTypePicker(it as TextView)
            }
        }
    }

    private fun setupAddressTypePicker(addressTypeField: TextView, type: Int, label: String) {
        addressTypeField.apply {
            text = getAddressTypeText(type, label)
            setOnClickListener {
                showAddressTypePicker(it as TextView)
            }
        }
    }

    private fun setupIMTypePicker(imTypeField: TextView, type: Int, label: String) {
        imTypeField.apply {
            text = getIMTypeText(type, label)
            setOnClickListener {
                showIMTypePicker(it as TextView)
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
            MyDatePickerDialog(this, eventField.tag?.toString() ?: "") { dateTag ->
                eventField.apply {
                    dateTag.getDateTimeFromDateString(true, this)
                    tag = dateTag
                    alpha = 1f
                }
            }
        }

        eventHolder.contact_event_remove.apply {
            applyColorFilter(getProperPrimaryColor())
            background.applyColorFilter(getProperTextColor())
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
            RadioItem(Phone.TYPE_MOBILE, getString(R.string.mobile)),
            RadioItem(Phone.TYPE_HOME, getString(R.string.home)),
            RadioItem(Phone.TYPE_WORK, getString(R.string.work)),
            RadioItem(Phone.TYPE_MAIN, getString(R.string.main_number)),
            RadioItem(Phone.TYPE_FAX_WORK, getString(R.string.work_fax)),
            RadioItem(Phone.TYPE_FAX_HOME, getString(R.string.home_fax)),
            RadioItem(Phone.TYPE_PAGER, getString(R.string.pager)),
            RadioItem(Phone.TYPE_OTHER, getString(R.string.other)),
            RadioItem(Phone.TYPE_CUSTOM, getString(R.string.custom))
        )

        val currentNumberTypeId = getPhoneNumberTypeId(numberTypeField.value)
        RadioGroupDialog(this, items, currentNumberTypeId) {
            if (it as Int == Phone.TYPE_CUSTOM) {
                CustomLabelDialog(this) {
                    numberTypeField.text = it
                }
            } else {
                numberTypeField.text = getPhoneNumberTypeText(it, "")
            }
        }
    }

    private fun showEmailTypePicker(emailTypeField: TextView) {
        val items = arrayListOf(
            RadioItem(CommonDataKinds.Email.TYPE_HOME, getString(R.string.home)),
            RadioItem(CommonDataKinds.Email.TYPE_WORK, getString(R.string.work)),
            RadioItem(CommonDataKinds.Email.TYPE_MOBILE, getString(R.string.mobile)),
            RadioItem(CommonDataKinds.Email.TYPE_OTHER, getString(R.string.other)),
            RadioItem(CommonDataKinds.Email.TYPE_CUSTOM, getString(R.string.custom))
        )

        val currentEmailTypeId = getEmailTypeId(emailTypeField.value)
        RadioGroupDialog(this, items, currentEmailTypeId) {
            if (it as Int == CommonDataKinds.Email.TYPE_CUSTOM) {
                CustomLabelDialog(this) {
                    emailTypeField.text = it
                }
            } else {
                emailTypeField.text = getEmailTypeText(it, "")
            }
        }
    }

    private fun showAddressTypePicker(addressTypeField: TextView) {
        val items = arrayListOf(
            RadioItem(StructuredPostal.TYPE_HOME, getString(R.string.home)),
            RadioItem(StructuredPostal.TYPE_WORK, getString(R.string.work)),
            RadioItem(StructuredPostal.TYPE_OTHER, getString(R.string.other)),
            RadioItem(StructuredPostal.TYPE_CUSTOM, getString(R.string.custom))
        )

        val currentAddressTypeId = getAddressTypeId(addressTypeField.value)
        RadioGroupDialog(this, items, currentAddressTypeId) {
            if (it as Int == StructuredPostal.TYPE_CUSTOM) {
                CustomLabelDialog(this) {
                    addressTypeField.text = it
                }
            } else {
                addressTypeField.text = getAddressTypeText(it, "")
            }
        }
    }

    private fun showIMTypePicker(imTypeField: TextView) {
        val items = arrayListOf(
            RadioItem(Im.PROTOCOL_AIM, getString(R.string.aim)),
            RadioItem(Im.PROTOCOL_MSN, getString(R.string.windows_live)),
            RadioItem(Im.PROTOCOL_YAHOO, getString(R.string.yahoo)),
            RadioItem(Im.PROTOCOL_SKYPE, getString(R.string.skype)),
            RadioItem(Im.PROTOCOL_QQ, getString(R.string.qq)),
            RadioItem(Im.PROTOCOL_GOOGLE_TALK, getString(R.string.hangouts)),
            RadioItem(Im.PROTOCOL_ICQ, getString(R.string.icq)),
            RadioItem(Im.PROTOCOL_JABBER, getString(R.string.jabber)),
            RadioItem(Im.PROTOCOL_CUSTOM, getString(R.string.custom))
        )

        val currentIMTypeId = getIMTypeId(imTypeField.value)
        RadioGroupDialog(this, items, currentIMTypeId) {
            if (it as Int == Im.PROTOCOL_CUSTOM) {
                CustomLabelDialog(this) {
                    imTypeField.text = it
                }
            } else {
                imTypeField.text = getIMTypeText(it, "")
            }
        }
    }

    private fun showEventTypePicker(eventTypeField: TextView) {
        val items = arrayListOf(
            RadioItem(CommonDataKinds.Event.TYPE_ANNIVERSARY, getString(R.string.anniversary)),
            RadioItem(CommonDataKinds.Event.TYPE_BIRTHDAY, getString(R.string.birthday)),
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

    private fun showSelectContactSourceDialog() {
        showContactSourcePicker(contact!!.source) {
            contact!!.source = if (it == getString(R.string.phone_storage_hidden)) SMT_PRIVATE else it
            getPublicContactSource(it) {
                contact_source.text = if (it == "") getString(R.string.phone_storage) else it
            }
        }
    }

    private fun saveContact() {
        if (isSaving || contact == null) {
            return
        }

        val contactFields = arrayListOf(
            contact_prefix, contact_first_name, contact_middle_name, contact_surname, contact_suffix, contact_nickname,
            contact_notes, contact_organization_company, contact_organization_job_position
        )

        if (contactFields.all { it.value.isEmpty() }) {
            if (currentContactPhotoPath.isEmpty() &&
                getFilledPhoneNumbers().isEmpty() &&
                getFilledEmails().isEmpty() &&
                getFilledAddresses().isEmpty() &&
                getFilledIMs().isEmpty() &&
                getFilledEvents().isEmpty() &&
                getFilledWebsites().isEmpty()
            ) {
                toast(R.string.fields_empty)
                return
            }
        }

        val contactValues = fillContactValues()

        val oldPhotoUri = contact!!.photoUri
        val oldPrimary = contact!!.phoneNumbers.find { it.isPrimary }
        val newPrimary = contactValues.phoneNumbers.find { it.isPrimary }
        val primaryState = Pair(oldPrimary, newPrimary)

        contact = contactValues

        ensureBackgroundThread {
            baseConfig.lastUsedContactSource = contact!!.source
            when {
                contact!!.id == 0 -> insertNewContact(false)
                originalContactSource != contact!!.source -> insertNewContact(true)
                else -> {
                    val photoUpdateStatus = getPhotoUpdateStatus(oldPhotoUri, contact!!.photoUri)
                    updateContact(photoUpdateStatus, primaryState)
                }
            }
        }
    }

    private fun fillContactValues(): Contact {
        val filledPhoneNumbers = getFilledPhoneNumbers()
        val filledEmails = getFilledEmails()
        val filledAddresses = getFilledAddresses()
        val filledIMs = getFilledIMs()
        val filledEvents = getFilledEvents()
        val filledWebsites = getFilledWebsites()

        val newContact = contact!!.copy(
            prefix = contact_prefix.value,
            firstName = contact_first_name.value,
            middleName = contact_middle_name.value,
            surname = contact_surname.value,
            suffix = contact_suffix.value,
            nickname = contact_nickname.value,
            photoUri = currentContactPhotoPath,
            phoneNumbers = filledPhoneNumbers,
            emails = filledEmails,
            addresses = filledAddresses,
            IMs = filledIMs,
            events = filledEvents,
            starred = if (isContactStarred()) 1 else 0,
            notes = contact_notes.value,
            websites = filledWebsites
        )

        val company = contact_organization_company.value
        val jobPosition = contact_organization_job_position.value
        newContact.organization = Organization(company, jobPosition)
        return newContact
    }

    private fun getFilledPhoneNumbers(): ArrayList<PhoneNumber> {
        val phoneNumbers = ArrayList<PhoneNumber>()
        val numbersCount = contact_numbers_holder.childCount
        for (i in 0 until numbersCount) {
            val numberHolder = contact_numbers_holder.getChildAt(i)
            val number = numberHolder.contact_number.value
            val numberType = getPhoneNumberTypeId(numberHolder.contact_number_type.value)
            val numberLabel = if (numberType == Phone.TYPE_CUSTOM) numberHolder.contact_number_type.value else ""

            if (number.isNotEmpty()) {
                var normalizedNumber = number.normalizePhoneNumber()

                // fix a glitch when onBackPressed the app thinks that a number changed because we fetched
                // normalized number +421903123456, then at getting it from the input field we get 0903123456, can happen at WhatsApp contacts
                val fetchedNormalizedNumber = numberHolder.contact_number.tag?.toString() ?: ""
                if (PhoneNumberUtils.compare(number.normalizePhoneNumber(), fetchedNormalizedNumber)) {
                    normalizedNumber = fetchedNormalizedNumber
                }

                val isPrimary = numberHolder.default_toggle_icon.tag == 1
                phoneNumbers.add(PhoneNumber(number, numberType, numberLabel, normalizedNumber, isPrimary))
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
            val emailLabel = if (emailType == CommonDataKinds.Email.TYPE_CUSTOM) emailHolder.contact_email_type.value else ""

            if (email.isNotEmpty()) {
                emails.add(Email(email, emailType, emailLabel))
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
            val addressLabel = if (addressType == StructuredPostal.TYPE_CUSTOM) addressHolder.contact_address_type.value else ""

            if (address.isNotEmpty()) {
                addresses.add(Address(address, addressType, addressLabel))
            }
        }
        return addresses
    }

    private fun getFilledIMs(): ArrayList<IM> {
        val IMs = ArrayList<IM>()
        val IMsCount = contact_ims_holder.childCount
        for (i in 0 until IMsCount) {
            val IMsHolder = contact_ims_holder.getChildAt(i)
            val IM = IMsHolder.contact_im.value
            val IMType = getIMTypeId(IMsHolder.contact_im_type.value)
            val IMLabel = if (IMType == Im.PROTOCOL_CUSTOM) IMsHolder.contact_im_type.value else ""

            if (IM.isNotEmpty()) {
                IMs.add(IM(IM, IMType, IMLabel))
            }
        }
        return IMs
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

    private fun insertNewContact(deleteCurrentContact: Boolean) {
        isSaving = true
        if (!deleteCurrentContact) {
            toast(R.string.inserting)
        }

        if (ContactsHelper(this@EditContactActivity).insertContact(contact!!)) {
            if (deleteCurrentContact) {
                contact!!.source = originalContactSource
                ContactsHelper(this).deleteContact(contact!!, false) {
                    setResult(Activity.RESULT_OK)
                    hideKeyboard()
                    finish()
                }
            } else {
                setResult(Activity.RESULT_OK)
                hideKeyboard()
                finish()
            }
        } else {
            toast(R.string.unknown_error_occurred)
        }
    }

    private fun updateContact(photoUpdateStatus: Int, primaryState: Pair<PhoneNumber?, PhoneNumber?>) {
        isSaving = true
        if (ContactsHelper(this@EditContactActivity).updateContact(contact!!, photoUpdateStatus)) {
            val status = getPrimaryNumberStatus(primaryState.first, primaryState.second)
            if (status != PrimaryNumberStatus.UNCHANGED) {
                updateDefaultNumberForDuplicateContacts(primaryState, status) {
                    setResult(Activity.RESULT_OK)
                    hideKeyboard()
                    finish()
                }
            } else {
                setResult(Activity.RESULT_OK)
                hideKeyboard()
                finish()
            }
        } else {
            toast(R.string.unknown_error_occurred)
        }
    }

    private fun updateDefaultNumberForDuplicateContacts(
        toggleState: Pair<PhoneNumber?, PhoneNumber?>,
        primaryStatus: PrimaryNumberStatus,
        callback: () -> Unit
    ) {
        val contactsHelper = ContactsHelper(this)

        contactsHelper.getDuplicatesOfContact(contact!!, false) { contacts ->
            ensureBackgroundThread {
                val displayContactSources = getVisibleContactSources()
                contacts.filter { displayContactSources.contains(it.source) }.forEach { contact ->
                    val duplicate = contactsHelper.getContactWithId(contact.id, contact.isPrivate())
                    if (duplicate != null) {
                        if (primaryStatus == PrimaryNumberStatus.UNSTARRED) {
                            val number = duplicate.phoneNumbers.find { it.normalizedNumber == toggleState.first!!.normalizedNumber }
                            number?.isPrimary = false
                        } else if (primaryStatus == PrimaryNumberStatus.STARRED) {
                            val number = duplicate.phoneNumbers.find { it.normalizedNumber == toggleState.second!!.normalizedNumber }
                            if (number != null) {
                                duplicate.phoneNumbers.forEach {
                                    it.isPrimary = false
                                }
                                number.isPrimary = true
                            }
                        }

                        contactsHelper.updateContact(duplicate, PHOTO_UNCHANGED)
                    }
                }

                runOnUiThread {
                    callback.invoke()
                }
            }
        }
    }

    private fun getPrimaryNumberStatus(oldPrimary: PhoneNumber?, newPrimary: PhoneNumber?): PrimaryNumberStatus {
        return if (oldPrimary != null && newPrimary != null && oldPrimary != newPrimary) {
            PrimaryNumberStatus.STARRED
        } else if (oldPrimary == null && newPrimary != null) {
            PrimaryNumberStatus.STARRED
        } else if (oldPrimary != null && newPrimary == null) {
            PrimaryNumberStatus.UNSTARRED
        } else {
            PrimaryNumberStatus.UNCHANGED
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
        setupPhoneNumberTypePicker(numberHolder.contact_number_type, DEFAULT_PHONE_NUMBER_TYPE, "")
        contact_numbers_holder.addView(numberHolder)
        contact_numbers_holder.onGlobalLayout {
            numberHolder.contact_number.requestFocus()
            showKeyboard(numberHolder.contact_number)
        }
        numberHolder.default_toggle_icon.tag = 0
        initNumberHolders()
    }

    private fun addNewEmailField() {
        val emailHolder = layoutInflater.inflate(R.layout.item_edit_email, contact_emails_holder, false) as ViewGroup
        updateTextColors(emailHolder)
        setupEmailTypePicker(emailHolder.contact_email_type, DEFAULT_EMAIL_TYPE, "")
        contact_emails_holder.addView(emailHolder)
        contact_emails_holder.onGlobalLayout {
            emailHolder.contact_email.requestFocus()
            showKeyboard(emailHolder.contact_email)
        }
    }

    private fun addNewAddressField() {
        val addressHolder = layoutInflater.inflate(R.layout.item_edit_address, contact_addresses_holder, false) as ViewGroup
        updateTextColors(addressHolder)
        setupAddressTypePicker(addressHolder.contact_address_type, DEFAULT_ADDRESS_TYPE, "")
        contact_addresses_holder.addView(addressHolder)
        contact_addresses_holder.onGlobalLayout {
            addressHolder.contact_address.requestFocus()
            showKeyboard(addressHolder.contact_address)
        }
    }

    private fun addNewIMField() {
        val IMHolder = layoutInflater.inflate(R.layout.item_edit_im, contact_ims_holder, false) as ViewGroup
        updateTextColors(IMHolder)
        setupIMTypePicker(IMHolder.contact_im_type, DEFAULT_IM_TYPE, "")
        contact_ims_holder.addView(IMHolder)
        contact_ims_holder.onGlobalLayout {
            IMHolder.contact_im.requestFocus()
            showKeyboard(IMHolder.contact_im)
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

            setOnLongClickListener { toast(R.string.toggle_favorite); true; }
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

    private fun getStarDrawable(on: Boolean) = resources.getDrawable(if (on) R.drawable.ic_star_vector else R.drawable.ic_star_outline_vector)

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
                else -> {
                    showPhotoPlaceholder(contact_photo)
                    contact_photo_bottom_shadow.beGone()
                }
            }
        }
    }

    private fun parseIntentData(data: ArrayList<ContentValues>) {
        data.forEach {
            when (it.get(StructuredName.MIMETYPE)) {
                CommonDataKinds.Email.CONTENT_ITEM_TYPE -> parseEmail(it)
                StructuredPostal.CONTENT_ITEM_TYPE -> parseAddress(it)
                CommonDataKinds.Organization.CONTENT_ITEM_TYPE -> parseOrganization(it)
                CommonDataKinds.Event.CONTENT_ITEM_TYPE -> parseEvent(it)
                Website.CONTENT_ITEM_TYPE -> parseWebsite(it)
                Note.CONTENT_ITEM_TYPE -> parseNote(it)
            }
        }
    }

    private fun parseEmail(contentValues: ContentValues) {
        val type = contentValues.getAsInteger(CommonDataKinds.Email.DATA2) ?: DEFAULT_EMAIL_TYPE
        val emailValue = contentValues.getAsString(CommonDataKinds.Email.DATA1) ?: return
        val email = Email(emailValue, type, "")
        contact!!.emails.add(email)
    }

    private fun parseAddress(contentValues: ContentValues) {
        val type = contentValues.getAsInteger(StructuredPostal.DATA2) ?: DEFAULT_ADDRESS_TYPE
        val addressValue = contentValues.getAsString(StructuredPostal.DATA4)
            ?: contentValues.getAsString(StructuredPostal.DATA1) ?: return
        val address = Address(addressValue, type, "")
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
        val website = contentValues.getAsString(Website.DATA1) ?: return
        contact!!.websites.add(website)
    }

    private fun parseNote(contentValues: ContentValues) {
        val note = contentValues.getAsString(Note.DATA1) ?: return
        contact!!.notes = note
    }

    private fun startTakePhotoIntent() {
        hideKeyboard()
        val uri = getCachePhotoUri()
        lastPhotoIntentUri = uri
        Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
            putExtra(MediaStore.EXTRA_OUTPUT, uri)

            try {
                startActivityForResult(this, INTENT_TAKE_PHOTO)
            } catch (e: ActivityNotFoundException) {
                toast(R.string.no_app_found)
            } catch (e: Exception) {
                showErrorToast(e)
            }
        }
    }

    private fun startChoosePhotoIntent() {
        hideKeyboard()
        val uri = getCachePhotoUri()
        lastPhotoIntentUri = uri
        Intent(Intent.ACTION_PICK).apply {
            type = "image/*"
            clipData = ClipData("Attachment", arrayOf("text/uri-list"), ClipData.Item(uri))
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
            putExtra(MediaStore.EXTRA_OUTPUT, uri)

            try {
                startActivityForResult(this, INTENT_CHOOSE_PHOTO)
            } catch (e: ActivityNotFoundException) {
                toast(R.string.no_app_found)
            } catch (e: Exception) {
                showErrorToast(e)
            }
        }
    }

    override fun customRingtoneSelected(ringtonePath: String) {
        contact!!.ringtone = ringtonePath
        contact_ringtone.text = ringtonePath.getFilenameFromPath()
    }

    override fun systemRingtoneSelected(uri: Uri?) {
        contact!!.ringtone = uri?.toString() ?: ""
        val contactRingtone = RingtoneManager.getRingtone(this, uri)
        contact_ringtone.text = contactRingtone.getTitle(this)
    }

    private fun getPhoneNumberTypeId(value: String) = when (value) {
        getString(R.string.mobile) -> Phone.TYPE_MOBILE
        getString(R.string.home) -> Phone.TYPE_HOME
        getString(R.string.work) -> Phone.TYPE_WORK
        getString(R.string.main_number) -> Phone.TYPE_MAIN
        getString(R.string.work_fax) -> Phone.TYPE_FAX_WORK
        getString(R.string.home_fax) -> Phone.TYPE_FAX_HOME
        getString(R.string.pager) -> Phone.TYPE_PAGER
        getString(R.string.other) -> Phone.TYPE_OTHER
        else -> Phone.TYPE_CUSTOM
    }

    private fun getEmailTypeId(value: String) = when (value) {
        getString(R.string.home) -> CommonDataKinds.Email.TYPE_HOME
        getString(R.string.work) -> CommonDataKinds.Email.TYPE_WORK
        getString(R.string.mobile) -> CommonDataKinds.Email.TYPE_MOBILE
        getString(R.string.other) -> CommonDataKinds.Email.TYPE_OTHER
        else -> CommonDataKinds.Email.TYPE_CUSTOM
    }

    private fun getEventTypeId(value: String) = when (value) {
        getString(R.string.anniversary) -> CommonDataKinds.Event.TYPE_ANNIVERSARY
        getString(R.string.birthday) -> CommonDataKinds.Event.TYPE_BIRTHDAY
        else -> CommonDataKinds.Event.TYPE_OTHER
    }

    private fun getAddressTypeId(value: String) = when (value) {
        getString(R.string.home) -> StructuredPostal.TYPE_HOME
        getString(R.string.work) -> StructuredPostal.TYPE_WORK
        getString(R.string.other) -> StructuredPostal.TYPE_OTHER
        else -> StructuredPostal.TYPE_CUSTOM
    }

    private fun getIMTypeId(value: String) = when (value) {
        getString(R.string.aim) -> Im.PROTOCOL_AIM
        getString(R.string.windows_live) -> Im.PROTOCOL_MSN
        getString(R.string.yahoo) -> Im.PROTOCOL_YAHOO
        getString(R.string.skype) -> Im.PROTOCOL_SKYPE
        getString(R.string.qq) -> Im.PROTOCOL_QQ
        getString(R.string.hangouts) -> Im.PROTOCOL_GOOGLE_TALK
        getString(R.string.icq) -> Im.PROTOCOL_ICQ
        getString(R.string.jabber) -> Im.PROTOCOL_JABBER
        else -> Im.PROTOCOL_CUSTOM
    }
}
