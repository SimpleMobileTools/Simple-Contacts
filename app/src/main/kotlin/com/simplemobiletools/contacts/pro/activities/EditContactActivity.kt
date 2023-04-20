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
import android.view.ViewParent
import android.view.WindowManager
import android.widget.*
import androidx.core.content.ContextCompat
import androidx.core.view.children
import androidx.core.widget.doAfterTextChanged
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
import kotlinx.android.synthetic.main.activity_edit_contact.contact_addresses_holder
import kotlinx.android.synthetic.main.activity_edit_contact.contact_addresses_image
import kotlinx.android.synthetic.main.activity_edit_contact.contact_appbar
import kotlinx.android.synthetic.main.activity_edit_contact.contact_emails_holder
import kotlinx.android.synthetic.main.activity_edit_contact.contact_emails_image
import kotlinx.android.synthetic.main.activity_edit_contact.contact_events_holder
import kotlinx.android.synthetic.main.activity_edit_contact.contact_events_image
import kotlinx.android.synthetic.main.activity_edit_contact.contact_groups_holder
import kotlinx.android.synthetic.main.activity_edit_contact.contact_groups_image
import kotlinx.android.synthetic.main.activity_edit_contact.contact_ims_holder
import kotlinx.android.synthetic.main.activity_edit_contact.contact_ims_image
import kotlinx.android.synthetic.main.activity_edit_contact.contact_name_image
import kotlinx.android.synthetic.main.activity_edit_contact.contact_notes_image
import kotlinx.android.synthetic.main.activity_edit_contact.contact_numbers_holder
import kotlinx.android.synthetic.main.activity_edit_contact.contact_numbers_image
import kotlinx.android.synthetic.main.activity_edit_contact.contact_organization_image
import kotlinx.android.synthetic.main.activity_edit_contact.contact_photo
import kotlinx.android.synthetic.main.activity_edit_contact.contact_photo_bottom_shadow
import kotlinx.android.synthetic.main.activity_edit_contact.contact_photo_share_icon
import kotlinx.android.synthetic.main.activity_edit_contact.contact_photo_share_info
import kotlinx.android.synthetic.main.activity_edit_contact.contact_relations_holder
import kotlinx.android.synthetic.main.activity_edit_contact.contact_relations_image
import kotlinx.android.synthetic.main.activity_edit_contact.contact_ringtone
import kotlinx.android.synthetic.main.activity_edit_contact.contact_ringtone_image
import kotlinx.android.synthetic.main.activity_edit_contact.contact_scrollview
import kotlinx.android.synthetic.main.activity_edit_contact.contact_source_image
import kotlinx.android.synthetic.main.activity_edit_contact.contact_toggle_favorite
import kotlinx.android.synthetic.main.activity_edit_contact.contact_toolbar
import kotlinx.android.synthetic.main.activity_edit_contact.contact_websites_holder
import kotlinx.android.synthetic.main.activity_edit_contact.contact_websites_image
import kotlinx.android.synthetic.main.activity_edit_contact.contact_wrapper
import kotlinx.android.synthetic.main.activity_edit_contact.view.*
import kotlinx.android.synthetic.main.activity_view_contact.*
import kotlinx.android.synthetic.main.item_edit_nickname.view.*
import kotlinx.android.synthetic.main.item_edit_phone_number.view.*
import kotlinx.android.synthetic.main.item_edit_email.view.*
import kotlinx.android.synthetic.main.item_edit_address.view.*
import kotlinx.android.synthetic.main.item_edit_im.view.*
import kotlinx.android.synthetic.main.item_edit_event.view.*
import kotlinx.android.synthetic.main.item_edit_note.view.*
import kotlinx.android.synthetic.main.item_edit_organization.view.*
import kotlinx.android.synthetic.main.item_edit_website.view.*
import kotlinx.android.synthetic.main.item_edit_relation.view.*
import kotlinx.android.synthetic.main.item_edit_group.view.*

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
    private var textChangedHandlersActive: Boolean = false

    private var configAutoFormattedNameFormat: ContactNameFormat = ContactNameFormat.NAMEFORMAT_FAMILY_GIVEN
    private var configAutoFormattedAddressFormat: String = Address.AddressFormatUSA

    enum class PrimaryNumberStatus {
        UNCHANGED, STARRED, UNSTARRED
    }

    // *****************************************************************

    override fun onCreate(savedInstanceState: Bundle?) {
        showTransparentTop = true
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_contact)

        if (checkAppSideloading()) {
            return
        }

        configAutoFormattedNameFormat = config.autoFormattedNameFormat
        configAutoFormattedAddressFormat = getString(R.string.address_format)

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
    } // EditContactActivity.onCreate()

    // *****************************************************************

    override fun onActivityResult(requestCode: Int, resultCode: Int, resultData: Intent?) {
        super.onActivityResult(requestCode, resultCode, resultData)
        if (resultCode == RESULT_OK) {
            when (requestCode) {
                INTENT_TAKE_PHOTO, INTENT_CHOOSE_PHOTO -> startCropPhotoIntent(lastPhotoIntentUri, resultData?.data)
                INTENT_CROP_PHOTO -> updateContactPhoto(lastPhotoIntentUri.toString(), contact_photo, contact_photo_bottom_shadow)
            }
        }
    } // EditContactActivity.onActivityResult()

    // *****************************************************************

    private fun initContact() {
        var contactId = intent.getIntExtra(CONTACT_ID, 0)
        val action = intent.action
        if ((contactId == 0) && (action == Intent.ACTION_EDIT || action == ADD_NEW_CONTACT_NUMBER)) {
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
                val srcContact: Contact? = ContactsHelper(this).getContactWithId(contactId, intent.getBooleanExtra(IS_PRIVATE, false))
                if (srcContact == null) {
                    toast(R.string.unknown_error_occurred)
                    state.reset()
                    hideKeyboard()
                    finish()
                } else {
                    convertKnownIMCustomTypesToIDs(srcContact.IMs)
                    convertKnownRelationCustomTypesToIDs(srcContact.relations)
                    if (srcContact != origContact) {
                        contact = srcContact
                        origContact = contact!!.deepCopy()
                    }

                    runOnUiThread {
                        gotContact()
                    }
                }
            }
        } else {
            gotContact()
        }
    } // EditContactActivity.initContact()

    // *****************************************************************

    private fun gotContact() {
        // contact_scrollview.beVisible()
        if (contact == null) {
            setupNewContact()
        }

        initActivityState()
        setupEditContact()
        originalRingtone = contact?.ringtone

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
                contact!!.name.givenName = firstName.toString()
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
            contact_name_image, contact_numbers_image, contact_emails_image,
            contact_addresses_image, contact_ims_image, contact_events_image,
            contact_notes_image, contact_ringtone_image, contact_organization_image,
            contact_websites_image, contact_relations_image, contact_groups_image,
            contact_source_image
        ).forEach {
            it.applyColorFilter(textColor)
        }

        val properPrimaryColor = getProperPrimaryColor()
        arrayOf(
            contact_nicknames_add_new, contact_numbers_add_new, contact_emails_add_new,
            contact_addresses_add_new, contact_ims_add_new, contact_events_add_new,
            contact_websites_add_new, contact_relations_add_new, contact_groups_add_new
        ).forEach {
            it.applyColorFilter(properPrimaryColor)
        }

        arrayOf(
            contact_nicknames_add_new.background,  contact_numbers_add_new.background, contact_emails_add_new.background,
            contact_addresses_add_new.background, contact_ims_add_new.background, contact_events_add_new.background,
            contact_websites_add_new.background, contact_relations_add_new.background, contact_groups_add_new.background
        ).forEach {
            it.applyColorFilter(textColor)
        }

        contact_toggle_favorite.setOnClickListener { toggleFavorite() }
        contact_photo.setOnClickListener { trySetPhoto() }
        contact_change_photo.setOnClickListener { trySetPhoto() }
        contact_nicknames_add_new.setOnClickListener { addNewNicknameField() }
        contact_numbers_add_new.setOnClickListener { addNewPhoneNumberField() }
        contact_emails_add_new.setOnClickListener { addNewEmailField() }
        contact_addresses_add_new.setOnClickListener { addNewAddressField() }
        contact_ims_add_new.setOnClickListener { addNewIMField() }
        contact_events_add_new.setOnClickListener { addNewEventField() }
        contact_websites_add_new.setOnClickListener { addNewWebsiteField() }
        contact_relations_add_new.setOnClickListener { addNewRelationField() }
        contact_groups_add_new.setOnClickListener { showSelectGroupsDialog() }
        contact_source.setOnClickListener { showSelectContactSourceDialog() }

        contact_change_photo.setOnLongClickListener { toast(R.string.change_photo); true; }

        contact_display_name.setOnFocusChangeListener { v, hasFocus -> onDisplayNameFocusChanged(v, hasFocus) }
        contact_refresh_display_name.setOnClickListener { contact_display_name.setText(getAutoDisplayName()); state.autoCalcDisplayName = true }

        contact_prefix.doAfterTextChanged { edit -> setAutoDisplayName() }
        contact_first_name.doAfterTextChanged { edit -> setAutoDisplayName() }
        contact_middle_name.doAfterTextChanged { edit -> setAutoDisplayName() }
        contact_surname.doAfterTextChanged { edit -> setAutoDisplayName() }
        contact_suffix.doAfterTextChanged { edit -> setAutoDisplayName() }

        setupFieldVisibility(state.selectShareFieldsActive)

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
            findItem(R.id.delete).isVisible = (contact?.id != 0)
            findItem(R.id.share).isVisible = (contact?.id != 0)
            findItem(R.id.open_with).isVisible = (contact?.id != 0) && (contact?.isPrivate() == false)
        }

        textChangedHandlersActive = true
        if (state.autoCalcDisplayName)
            setAutoDisplayName()

        val itemCnt = contact?.addresses?.count() ?: 0
        for (i in 0 .. itemCnt -1) {
            if (state.autoCalcFormattedAddress[i]) {
                val address: Address = contact!!.addresses[i]
                val addressHolder: ViewGroup = contact_addresses_holder.getChildAt(i) as ViewGroup
                address.formattedAddress = address.getFormattedPostalAddress(configAutoFormattedAddressFormat)
                addressHolder.contact_address.setText(address.formattedAddress.trim())
            }
        }

        contact_scrollview.beVisible()
        if (state.stateValid && (state.vScrollHeight > 0)) {
            val vScrollHeight: Int = contact_scrollview.height
            val vScrollTargetY = ((vScrollHeight * state.vScrollPosY) + state.vScrollHeight/2) / state.vScrollHeight
            contact_scrollview.scrollTo(0, vScrollTargetY)
        }
    } // EditContactActivity.gotContact()

    // *****************************************************************

    fun initActivityState() {
        state.reset()
        if (contact == null)
            return
        val editContact: Contact = contact!!

        state.stateValid = true

        val formattedName: String = editContact.name.formattedName.trim()
        state.autoCalcDisplayName = (formattedName.isEmpty()) ||
            (formattedName == editContact.name.buildDisplayName(configAutoFormattedNameFormat))

        state.autoCalcFormattedAddress.clear()
        val itemCnt: Int = editContact.addresses.count()
        if (itemCnt > 0) {
            for (i in 1..itemCnt) {
                val address: Address = editContact.addresses[i - 1]
                val formattedAddress: String = address.formattedAddress.trim()
                state.autoCalcFormattedAddress.add((formattedAddress.isEmpty()) ||
                    (formattedAddress == address.getFormattedPostalAddress(configAutoFormattedAddressFormat)))
            }
        } else {
            state.autoCalcFormattedAddress.add(true)
        }

        state.initShareInfo(editContact)
    } // EditContactActivity.initActivityState()

    // *****************************************************************

    override fun onSaveInstanceState(savedInstanceState: Bundle) {
        var itemCnt: Int

        contact = fillContactValues(false, true, true)
        state.sharePhoto = contact_photo_share_info.isChecked
        state.shareName = true

        state.shareNickname.clear()
        itemCnt = contact_nicknames_holder.childCount
        for (i in 1..itemCnt) {
            val nicknameHolder = contact_nicknames_holder.getChildAt(i-1)
            state.shareNickname.add(nicknameHolder.contact_nickname_share_info.isChecked)
        }

        state.shareNumber.clear()
        itemCnt = contact_numbers_holder.childCount
        for (i in 1..itemCnt) {
            val numberHolder = contact_numbers_holder.getChildAt(i-1)
            state.shareNumber.add(numberHolder.contact_number_share_info.isChecked)
        }

        state.shareEmail.clear()
        itemCnt = contact_emails_holder.childCount
        for (i in 1..itemCnt) {
            val emailHolder = contact_emails_holder.getChildAt(i-1)
            state.shareEmail.add(emailHolder.contact_email_share_info.isChecked)
        }

        state.shareAddress.clear()
        itemCnt = contact_addresses_holder.childCount
        for (i in 1..itemCnt) {
            val addressHolder = contact_addresses_holder.getChildAt(i-1)
            state.shareAddress.add(addressHolder.contact_address_share_info.isChecked)
        }

        state.shareIM.clear()
        itemCnt = contact_ims_holder.childCount
        for (i in 1..itemCnt) {
            val IMHolder = contact_ims_holder.getChildAt(i-1)
            state.shareIM.add(IMHolder.contact_im_share_info.isChecked)
        }

        state.shareEvent.clear()
        itemCnt = contact_events_holder.childCount
        for (i in 1..itemCnt) {
            val eventHolder = contact_events_holder.getChildAt(i-1)
            state.shareEvent.add(eventHolder.contact_event_share_info.isChecked)
        }

        state.shareNotes = contact_notes_holder.contact_note_share_info.isChecked
        // state.shareRingtone = false
        state.shareOrganization = contact_organization_holder.contact_organization_share_info.isChecked

        state.shareWebsite.clear()
        itemCnt = contact_websites_holder.childCount
        for (i in 1..itemCnt) {
            val websiteHolder = contact_websites_holder.getChildAt(i-1)
            state.shareWebsite.add(websiteHolder.contact_website_share_info.isChecked)
        }

        state.shareRelation.clear()
        itemCnt = contact_relations_holder.childCount
        for (i in 1..itemCnt) {
            val relationHolder = contact_relations_holder.getChildAt(i-1)
            state.shareRelation.add(relationHolder.contact_relation_share_info.isChecked)
        }

        state.shareGroup.clear()
        itemCnt = contact_groups_holder.childCount
        for (i in 1..itemCnt) {
            val groupHolder = contact_groups_holder.getChildAt(i-1)
            state.shareGroup.add(groupHolder.contact_group_share_info.isChecked)
        }
        // state.shareSources = false

        state.vScrollWidth  = contact_scrollview.width
        state.vScrollHeight = contact_scrollview.height
        state.vScrollPosX  = contact_scrollview.scrollX
        state.vScrollPosY  = contact_scrollview.scrollY

        super.onSaveInstanceState(savedInstanceState)
    } // EditContactActivity.onSaveInstanceState()

    // *****************************************************************

    override fun onBackPressed() {
        if (state.selectShareFieldsActive) {
            state.selectShareFieldsActive = false
            setupFieldVisibility(false)
            setupFieldShareIcons(false)
        } else if ((System.currentTimeMillis() - mLastSavePromptTS > SAVE_DISCARD_PROMPT_INTERVAL) && hasContactChanged()) {
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
        state.stateValid = false
    } // EditContactActivity.onBackPressed()

    // *****************************************************************

    private fun setupMenu() {
        (contact_appbar.layoutParams as RelativeLayout.LayoutParams).topMargin = statusBarHeight
        contact_toolbar.menu.apply {
            findItem(R.id.save).setOnMenuItemClickListener {
                if (state.selectShareFieldsActive) {
                    state.selectShareFieldsActive = false
                    setupFieldVisibility(false)
                    setupFieldShareIcons(false)
                }
                saveContact()
                state.stateValid = false
                true
            }

            findItem(R.id.share).setOnMenuItemClickListener {
                // shareContact(contact!!)
                toggleShareActive()
                true
            }

            findItem(R.id.open_with).setOnMenuItemClickListener {
                if (state.selectShareFieldsActive) {
                    state.selectShareFieldsActive = false
                    setupFieldVisibility(false)
                    setupFieldShareIcons(false)
                }
                openWith()
                true
            }

            findItem(R.id.delete).setOnMenuItemClickListener {
                if (state.selectShareFieldsActive) {
                    state.selectShareFieldsActive = false
                    setupFieldVisibility(false)
                    setupFieldShareIcons(false)
                }
                deleteContact()
                state.stateValid = false
                true
            }

            findItem(R.id.manage_visible_fields).setOnMenuItemClickListener {
                // if (prevSavedInstanceState != null)
                //     onSaveInstanceState(prevSavedInstanceState!!)
                contact = fillContactValues(false, true, true)
                ManageVisibleFieldsDialog(this@EditContactActivity) {
                    // initContact()
                    gotContact()
                }
                true
            }
        }

        contact_toolbar.setNavigationOnClickListener {
            hideKeyboard()
            if (hasContactChanged()) {
                ConfirmationAdvancedDialog(this, "", R.string.save_before_closing, R.string.save, R.string.discard) {
                    if (it) {
                        saveContact()
                    }
                    state.stateValid = false
                    finish()
                }
            }
            else
                finish()
        }
    } // EditContactActivity.setupMenu()

    // *****************************************************************

    private fun hasContactChanged(): Boolean {
        if (contact == null)
            return(false)

        val editContact = fillContactValues(false, true, false)
        if (contact != editContact)
            return(true)

        if (originalRingtone != contact?.ringtone)
            return(true)

        return(false)
    } // EditContactActivity.hasContactChanged()

    // *****************************************************************

    private fun openWith() {
        Intent().apply {
            action = Intent.ACTION_EDIT
            data = getContactPublicUri(contact!!)
            launchActivityIntent(this)
        }
    } // EditContactActivity.openWith()

    // *****************************************************************

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
    } // EditContactActivity.startCropPhotoIntent()

    // *****************************************************************

    private fun toggleShareActive() {
        val exportSelectedFieldsOnly: Boolean = config.exportSelectedFieldsOnly
        var exportNow: Boolean = false
        if (exportSelectedFieldsOnly) {
            state.selectShareFieldsActive = !state.selectShareFieldsActive
            setupFieldVisibility(state.selectShareFieldsActive)
            setupFieldShareIcons(state.selectShareFieldsActive)
            if (state.selectShareFieldsActive)
                toast(R.string.select_exportable_fields_and_click_again)
            else
                exportNow = true
        } else {
            state.selectShareFieldsActive = false
            exportNow = true
        }

        if (exportNow) {
            val exportContact: Contact = fillContactValues(exportSelectedFieldsOnly, false, false)
            convertKnownIMTypeIDsToCustomTypes(exportContact.IMs)
            shareContact(exportContact)
        }
    } // EditContactActivity.toggleShareActive()

    // *****************************************************************

    private fun setupFieldShareIcons(selectShareActive: Boolean) {
        val showRemove: Boolean = config.showRemoveButtons
        // val showContactFields: Int = config.showContactFields
        setupPhotoShareIcons(selectShareActive)
        setupNameShareIcons(selectShareActive)
        setupNicknameShareIcons(selectShareActive, showRemove)
        setupPhoneNumberShareIcons(selectShareActive, showRemove)
        setupEmailShareIcons(selectShareActive, showRemove)
        setupAddressShareIcons(selectShareActive, showRemove)
        setupIMShareIcons(selectShareActive, showRemove)
        setupEventShareIcons(selectShareActive)
        setupNoteShareIcons(selectShareActive)
        setupOrganizationShareIcons(selectShareActive)
        setupWebsiteShareIcons(selectShareActive, showRemove)
        setupRelationShareIcons(selectShareActive, showRemove)
        setupGroupShareIcons(selectShareActive, showRemove)
    } // EditContactActivity.setupFieldShareIcons()

    // *****************************************************************

    private fun setupPhotoShareIcons(selectShareActive: Boolean) {
        contact_toggle_favorite.beVisibleIf(!selectShareActive)
        contact_change_photo.beVisibleIf(!selectShareActive)
        contact_photo_share_info.beVisibleIf(selectShareActive)
        contact_photo_share_icon.beVisibleIf(selectShareActive)
    } // EditContactActivity.setupPhotoShareIcons()

    // *****************************************************************

    private fun setupNameShareIcons(selectShareActive: Boolean) {
        contact_prefix.isEnabled = !selectShareActive
        contact_first_name.isEnabled = !selectShareActive
        contact_middle_name.isEnabled = !selectShareActive
        contact_surname.isEnabled = !selectShareActive
        contact_suffix.isEnabled = !selectShareActive
        contact_phonetic_given_name.isEnabled = !selectShareActive
        contact_phonetic_middle_name.isEnabled = !selectShareActive
        contact_phonetic_surname.isEnabled = !selectShareActive
    } // EditContactActivity.setupNameShareIcons()

    // *****************************************************************

    private fun setupNicknameShareIcons(selectShareActive: Boolean, enableRemoveButton: Boolean) {
        val nicknameListHolder: ViewGroup = contact_nicknames_holder
        val nicknameHolders: Sequence<View> = nicknameListHolder.children
        for (nicknameHolder in nicknameHolders) {
            nicknameHolder.contact_nickname.isEnabled = !selectShareActive
            nicknameHolder.contact_nickname_type.isEnabled = !selectShareActive
            nicknameHolder.contact_nickname_remove.beVisibleIf(enableRemoveButton && !selectShareActive)
            nicknameHolder.contact_nickname_share_info.beVisibleIf(selectShareActive)
            nicknameHolder.contact_nickname_share_icon.beVisibleIf(selectShareActive)
        }
        contact_nicknames_add_new.beVisibleIf(((config.showContactFields and SHOW_NICKNAME_FIELD) != 0) &&
            !selectShareActive)
    } // EditContactActivity.setupNicknameShareIcons()

    // *****************************************************************

    private fun setupPhoneNumberShareIcons(selectShareActive: Boolean, enableRemoveButton: Boolean) {
        val phoneNumberListHolder: ViewGroup = contact_numbers_holder
        val phoneNumberHolders: Sequence<View> = phoneNumberListHolder.children
        for (phoneNumberHolder in phoneNumberHolders) {
            phoneNumberHolder.contact_number.isEnabled = !selectShareActive
            phoneNumberHolder.contact_number_type.isEnabled = !selectShareActive
            phoneNumberHolder.contact_number_remove.beVisibleIf(enableRemoveButton && !selectShareActive)
            phoneNumberHolder.contact_number_share_info.beVisibleIf(selectShareActive)
            phoneNumberHolder.contact_number_share_icon.beVisibleIf(selectShareActive)
        }
        contact_numbers_add_new.beVisibleIf(((config.showContactFields and SHOW_PHONE_NUMBERS_FIELD) != 0) &&
            !selectShareActive)
    } // EditContactActivity.setupPhoneNumberShareIcons()

    // *****************************************************************

    private fun setupEmailShareIcons(selectShareActive: Boolean, enableRemoveButton: Boolean) {
        val emailListHolder: ViewGroup = contact_emails_holder
        val emailHolders: Sequence<View> = emailListHolder.children
        for (emailHolder in emailHolders) {
            emailHolder.contact_email.isEnabled = !selectShareActive
            emailHolder.contact_email_type.isEnabled = !selectShareActive
            emailHolder.contact_email_remove.beVisibleIf(enableRemoveButton && !selectShareActive)
            emailHolder.contact_email_share_info.beVisibleIf(selectShareActive)
            emailHolder.contact_email_share_icon.beVisibleIf(selectShareActive)
        }
        contact_emails_add_new.beVisibleIf(((config.showContactFields and SHOW_EMAILS_FIELD) != 0) &&
            !selectShareActive)
    } // EditContactActivity.setupEmailShareIcons()

    // *****************************************************************

    private fun setupAddressShareIcons(selectShareActive: Boolean, enableRemoveButton: Boolean) {
        val addressListHolder: ViewGroup = contact_addresses_holder
        val addressHolders: Sequence<View> = addressListHolder.children
        for (addressHolder in addressHolders) {
            addressHolder.contact_address.isEnabled = !selectShareActive
            addressHolder.contact_address_street.isEnabled = !selectShareActive
            addressHolder.contact_address_neighborhood.isEnabled = !selectShareActive
            addressHolder.contact_address_postOfficeBox.isEnabled = !selectShareActive
            addressHolder.contact_address_city.isEnabled = !selectShareActive
            addressHolder.contact_address_postalCode.isEnabled = !selectShareActive
            addressHolder.contact_address_region.isEnabled = !selectShareActive
            addressHolder.contact_address_country.isEnabled = !selectShareActive

            addressHolder.contact_address_type.isEnabled = !selectShareActive
            addressHolder.contact_address_remove.beVisibleIf(enableRemoveButton && !selectShareActive)
            addressHolder.contact_address_share_info.beVisibleIf(selectShareActive)
            addressHolder.contact_address_share_icon.beVisibleIf(selectShareActive)
        }
        contact_addresses_add_new.beVisibleIf(
            ((config.showContactFields and (SHOW_ADDRESSES_FIELD or SHOW_STRUCTURED_POSTAL_ACTIVE_ADDRESS_MASK)) != 0) &&
                !selectShareActive)
    } // EditContactActivity.setupAddressShareIcons()

    // *****************************************************************

    private fun setupIMShareIcons(selectShareActive: Boolean, enableRemoveButton: Boolean) {
        val imListHolder: ViewGroup = contact_ims_holder
        val imHolders: Sequence<View> = imListHolder.children
        for (imHolder in imHolders) {
            imHolder.contact_im.isEnabled = !selectShareActive
            imHolder.contact_im_type.isEnabled = !selectShareActive
            imHolder.contact_im_protocol.isEnabled = !selectShareActive
            imHolder.contact_im_remove.beVisibleIf(enableRemoveButton && !selectShareActive)
            imHolder.contact_im_share_info.beVisibleIf(selectShareActive)
            imHolder.contact_im_share_icon.beVisibleIf(selectShareActive)
        }
        contact_ims_add_new.beVisibleIf(((config.showContactFields and SHOW_IMS_FIELD) != 0) &&
            !selectShareActive)
    } // EditContactActivity.setupIMShareIcons()

    // *****************************************************************

    private fun setupEventShareIcons(selectShareActive: Boolean) {
        val eventListHolder: ViewGroup = contact_events_holder
        val eventHolders: Sequence<View> = eventListHolder.children
        for (eventHolder in eventHolders) {
            eventHolder.contact_event.isEnabled = !selectShareActive
            eventHolder.contact_event_type.isEnabled = !selectShareActive
            // Note: Event dates can not be erased. Thus using the 'Remove' Button
            // is the only way to get rid of an unneeded event!
            eventHolder.contact_event_remove.beVisibleIf(!selectShareActive)
            eventHolder.contact_event_share_info.beVisibleIf(selectShareActive)
            eventHolder.contact_event_share_icon.beVisibleIf(selectShareActive)
        }
        contact_events_add_new.beVisibleIf(((config.showContactFields and SHOW_EVENTS_FIELD) != 0) &&
            !selectShareActive)
    } // EditContactActivity.setupEventShareIcons()

    // *****************************************************************

    private fun setupNoteShareIcons(selectShareActive: Boolean) {
        val noteHolder = contact_notes_holder as View
        noteHolder.contact_note.isEnabled = !selectShareActive
        noteHolder.contact_note_share_info.beVisibleIf(selectShareActive)
        noteHolder.contact_note_share_icon.beVisibleIf(selectShareActive)
    } // EditContactActivity.setupNoteShareIcons()

    // *****************************************************************

    private fun setupOrganizationShareIcons(selectShareActive: Boolean) {
        val organizationHolder = contact_organization_holder as View
        organizationHolder.contact_organization_company.isEnabled = !selectShareActive
        organizationHolder.contact_organization_job_title.isEnabled = !selectShareActive
        organizationHolder.contact_organization_share_info.beVisibleIf(selectShareActive)
        organizationHolder.contact_organization_share_icon.beVisibleIf(selectShareActive)
    } // EditContactActivity.setupOrganizationShareIcons()

    // *****************************************************************

    private fun setupWebsiteShareIcons(selectShareActive: Boolean, enableRemoveButton: Boolean) {
        val websiteListHolder: ViewGroup = contact_websites_holder
        val websiteHolders: Sequence<View> = websiteListHolder.children
        for (websiteHolder in websiteHolders) {
            websiteHolder.contact_website.isEnabled = !selectShareActive
            websiteHolder.contact_website_type.isEnabled = !selectShareActive
            websiteHolder.contact_website_remove.beVisibleIf(enableRemoveButton && !selectShareActive)
            websiteHolder.contact_website_share_info.beVisibleIf(selectShareActive)
            websiteHolder.contact_website_share_icon.beVisibleIf(selectShareActive)
        }
        contact_websites_add_new.beVisibleIf(((config.showContactFields and SHOW_WEBSITES_FIELD) != 0) &&
            !selectShareActive)
    } // EditContactActivity.setupWebsiteShareIcons()

    // *****************************************************************

    private fun setupRelationShareIcons(selectShareActive: Boolean, enableRemoveButton: Boolean) {
        val relationListHolder: ViewGroup = contact_relations_holder
        val relationHolders: Sequence<View> = relationListHolder.children
        for (relationHolder in relationHolders) {
            relationHolder.contact_relation.isEnabled = !selectShareActive
            relationHolder.contact_relation_type.isEnabled = !selectShareActive
            relationHolder.contact_relation_remove.beVisibleIf(enableRemoveButton && !selectShareActive)
            relationHolder.contact_relation_share_info.beVisibleIf(selectShareActive)
            relationHolder.contact_relation_share_icon.beVisibleIf(selectShareActive)
        }
        contact_relations_add_new.beVisibleIf(((config.showContactFields and SHOW_RELATIONS_FIELD) != 0) &&
            !selectShareActive)
    } // EditContactActivity.setupRelationShareIcons()

    // *****************************************************************

    private fun setupGroupShareIcons(selectShareActive: Boolean, enableRemoveButton: Boolean) {
        val groupListHolder: ViewGroup = contact_groups_holder
        val groupHolders: Sequence<View> = groupListHolder.children
        for (groupHolder in groupHolders) {
            groupHolder.contact_group.isEnabled = !selectShareActive
            groupHolder.contact_group_remove.beVisibleIf(enableRemoveButton && !selectShareActive)
            groupHolder.contact_group_share_info.beVisibleIf(selectShareActive)
            groupHolder.contact_group_share_icon.beVisibleIf(selectShareActive)
        }
        contact_groups_add_new.beVisibleIf(((config.showContactFields and SHOW_GROUPS_FIELD) != 0) &&
            !selectShareActive)
    } // EditContactActivity.setupGroupShareIcons()

    // *****************************************************************

    private fun setupFieldVisibility(selectShareActive: Boolean) {
        var showFields = config.showContactFields
        var alwaysShowFields = config.alwaysShowNonEmptyContactFields
        if (((showFields and SHOW_DISPLAYNAME_FIELD) == 0) &&
            ((alwaysShowFields and SHOW_DISPLAYNAME_FIELD) != 0) &&
            (contact!!.name.formattedName.isNotEmpty()))
            showFields = showFields or SHOW_DISPLAYNAME_FIELD
        if (((showFields and SHOW_PREFIX_FIELD) == 0) &&
            ((alwaysShowFields and SHOW_PREFIX_FIELD) != 0) &&
            (contact!!.name.prefix.isNotEmpty()))
            showFields = showFields or SHOW_PREFIX_FIELD
        if (((showFields and SHOW_FIRST_NAME_FIELD) == 0) &&
            ((alwaysShowFields and SHOW_FIRST_NAME_FIELD) != 0) &&
            (contact!!.name.givenName.isNotEmpty()))
            showFields = showFields or SHOW_FIRST_NAME_FIELD
        if (((showFields and SHOW_MIDDLE_NAME_FIELD) == 0) &&
            ((alwaysShowFields and SHOW_MIDDLE_NAME_FIELD) != 0) &&
            (contact!!.name.middleName.isNotEmpty()))
            showFields = showFields or SHOW_MIDDLE_NAME_FIELD
        if (((showFields and SHOW_SURNAME_FIELD) == 0) &&
            ((alwaysShowFields and SHOW_SURNAME_FIELD) != 0) &&
            (contact!!.name.familyName.isNotEmpty()))
            showFields = showFields or SHOW_SURNAME_FIELD
        if (((showFields and SHOW_SUFFIX_FIELD) == 0) &&
            ((alwaysShowFields and SHOW_SUFFIX_FIELD) != 0) &&
            (contact!!.name.suffix.isNotEmpty()))
            showFields = showFields or SHOW_SUFFIX_FIELD
        if (((showFields and SHOW_PREFIX_FIELD) == 0) &&
            ((alwaysShowFields and SHOW_PREFIX_FIELD) != 0) &&
            (contact!!.name.prefix.isNotEmpty()))
            showFields = showFields or SHOW_PREFIX_FIELD

        if (showFields and (SHOW_DISPLAYNAME_FIELD or SHOW_PREFIX_FIELD or SHOW_FIRST_NAME_FIELD or
                            SHOW_MIDDLE_NAME_FIELD or SHOW_SURNAME_FIELD or SHOW_SUFFIX_FIELD) == 0) {
            contact_name_image.beInvisible()
        }

        contact_display_name.beVisibleIf((showFields and SHOW_DISPLAYNAME_FIELD) != 0)
        contact_refresh_display_name.beVisibleIf((showFields and SHOW_DISPLAYNAME_FIELD) != 0)
        contact_prefix.beVisibleIf((showFields and SHOW_PREFIX_FIELD) != 0)
        contact_first_name.beVisibleIf((showFields and SHOW_FIRST_NAME_FIELD) != 0)
        contact_middle_name.beVisibleIf((showFields and SHOW_MIDDLE_NAME_FIELD) != 0)
        contact_surname.beVisibleIf((showFields and SHOW_SURNAME_FIELD) != 0)
        contact_suffix.beVisibleIf((showFields and SHOW_SUFFIX_FIELD) != 0)
        contact_phonetic_given_name.beVisibleIf((((showFields and SHOW_FIRST_NAME_FIELD) != 0) && ((showFields and SHOW_PHONETIC_NAME_FIELDS) != 0)) ||
            (((alwaysShowFields and SHOW_FIRST_NAME_FIELD) != 0) && ((alwaysShowFields and SHOW_PHONETIC_NAME_FIELDS) != 0) && (contact!!.name.phoneticGivenName.isNotEmpty())))
        contact_phonetic_middle_name.beVisibleIf((((showFields and SHOW_MIDDLE_NAME_FIELD) != 0) && ((showFields and SHOW_PHONETIC_NAME_FIELDS) != 0)) ||
            (((alwaysShowFields and SHOW_MIDDLE_NAME_FIELD) != 0) && ((alwaysShowFields and SHOW_PHONETIC_NAME_FIELDS) != 0) && (contact!!.name.phoneticMiddleName.isNotEmpty())))
        contact_phonetic_surname.beVisibleIf((((showFields and SHOW_SURNAME_FIELD) != 0) && ((showFields and SHOW_PHONETIC_NAME_FIELDS) != 0)) ||
            (((alwaysShowFields and SHOW_SURNAME_FIELD) != 0) && ((alwaysShowFields and SHOW_PHONETIC_NAME_FIELDS) != 0) && (contact!!.name.phoneticFamilyName.isNotEmpty())))

        val areNicknamesVisible: Boolean = ((showFields and SHOW_NICKNAME_FIELD) != 0)
        contact_nicknames_holder.beVisibleIf(areNicknamesVisible)
        contact_nicknames_add_new.beVisibleIf(areNicknamesVisible &&
            ((showFields and SHOW_MULTIPLE_NICKNAMES) != 0) && !selectShareActive)

        var arePhoneNumbersVisible = ((showFields and SHOW_PHONE_NUMBERS_FIELD) != 0)
        if (arePhoneNumbersVisible) {
            arePhoneNumbersVisible = !selectShareActive
            val numbersHolder = contact_numbers_holder
            numbersHolder.children.forEach { numberHolder ->
            val validEntry = numberHolder.contact_number.value.trim().isNotEmpty()
            numberHolder.beVisibleIf(validEntry || !selectShareActive)
            arePhoneNumbersVisible = arePhoneNumbersVisible || validEntry
            }
        }
        contact_numbers_image.beVisibleIf(arePhoneNumbersVisible)
        contact_numbers_holder.beVisibleIf(arePhoneNumbersVisible)
        contact_numbers_add_new.beVisibleIf(arePhoneNumbersVisible && !selectShareActive)

        var areEmailsVisible = ((showFields and SHOW_EMAILS_FIELD) != 0)
        if (areEmailsVisible) {
            areEmailsVisible = !selectShareActive
            val emailsHolder = contact_emails_holder
            emailsHolder.children.forEach { emailHolder ->
                val validEntry = emailHolder.contact_email.value.trim().isNotEmpty()
                emailHolder.beVisibleIf(validEntry || !selectShareActive)
                areEmailsVisible = areEmailsVisible || validEntry
            }
        }
        contact_emails_image.beVisibleIf(areEmailsVisible)
        contact_emails_holder.beVisibleIf(areEmailsVisible)
        contact_emails_add_new.beVisibleIf(areEmailsVisible && !selectShareActive)

        val areAddressesVisible = ((showFields and (SHOW_ADDRESSES_FIELD or SHOW_STRUCTURED_POSTAL_ACTIVE_ADDRESS_MASK)) != 0)
        contact_addresses_image.beVisibleIf(areAddressesVisible)
        contact_addresses_holder.beVisibleIf(areAddressesVisible)
        contact_addresses_add_new.beVisibleIf(areAddressesVisible && !selectShareActive)

        var areIMsVisible = ((showFields and SHOW_IMS_FIELD) != 0)
        if (areIMsVisible) {
            areIMsVisible = !selectShareActive
            val imsHolder = contact_ims_holder
            imsHolder.children.forEach { imHolder ->
                val validEntry = imHolder.contact_im.value.trim().isNotEmpty()
                imHolder.beVisibleIf(validEntry || !selectShareActive)
                areIMsVisible = areIMsVisible || validEntry
            }
        }
        contact_ims_image.beVisibleIf(areIMsVisible)
        contact_ims_holder.beVisibleIf(areIMsVisible)
        contact_ims_add_new.beVisibleIf(areIMsVisible && !selectShareActive)

        var areEventsVisible = ((showFields and SHOW_EVENTS_FIELD) != 0)
        if (areEventsVisible) {
            areEventsVisible = !selectShareActive
            val eventsHolder = contact_events_holder
            eventsHolder.children.forEach { eventHolder ->
                val validEntry = eventHolder.contact_event.value.trim().isNotEmpty()
                eventHolder.beVisibleIf(validEntry || !selectShareActive)
                areEventsVisible = areEventsVisible || validEntry
            }
        }
        contact_events_image.beVisibleIf(areEventsVisible)
        contact_events_holder.beVisibleIf(areEventsVisible)
        contact_events_add_new.beVisibleIf(areEventsVisible && !selectShareActive)

        var areNotesVisible = ((showFields and SHOW_NOTES_FIELD) != 0)
        if (areNotesVisible) {
            areNotesVisible = !selectShareActive
            val notesHolder = contact_notes_holder
            val validEntry = notesHolder.contact_note.value.trim().isNotEmpty()
            notesHolder.beVisibleIf(validEntry || !selectShareActive)
            areNotesVisible = areNotesVisible || validEntry
        }
        contact_notes_image.beVisibleIf(areNotesVisible)
        contact_notes_holder.beVisibleIf(areNotesVisible)

        var isOrganizationVisible = ((showFields and SHOW_ORGANIZATION_FIELD) != 0)
        if (isOrganizationVisible) {
            isOrganizationVisible = !selectShareActive
            val organizationHolder = contact_organization_holder
            val validEntry =
                contact_organization_holder.contact_organization_company.value.trim().isNotEmpty() ||
                contact_organization_holder.contact_organization_job_title.value.trim().isNotEmpty()
            organizationHolder.beVisibleIf(validEntry || !selectShareActive)
            isOrganizationVisible = isOrganizationVisible || validEntry
        }
        contact_organization_image.beVisibleIf(isOrganizationVisible)
        contact_organization_holder.beVisibleIf(isOrganizationVisible)

        var areWebsitesVisible = ((showFields and SHOW_WEBSITES_FIELD) != 0)
        if (areWebsitesVisible) {
            areWebsitesVisible = !selectShareActive
            val websitesHolder = contact_websites_holder
            websitesHolder.children.forEach { websiteHolder ->
                val validEntry = websiteHolder.contact_website.value.trim().isNotEmpty()
                websiteHolder.beVisibleIf(validEntry || !selectShareActive)
                areWebsitesVisible = areWebsitesVisible || validEntry
            }
        }
        contact_websites_image.beVisibleIf(areWebsitesVisible)
        contact_websites_holder.beVisibleIf(areWebsitesVisible)
        contact_websites_add_new.beVisibleIf(areWebsitesVisible && !selectShareActive)

        var areRelationsVisible: Boolean = ((showFields and SHOW_RELATIONS_FIELD) != 0)
        if (areRelationsVisible) {
            areRelationsVisible = !selectShareActive
            val relationsHolder = contact_relations_holder
            relationsHolder.children.forEach { relationHolder ->
                val validEntry = relationHolder.contact_relation.value.trim().isNotEmpty()
                relationHolder.beVisibleIf(validEntry || !selectShareActive)
                areRelationsVisible = areRelationsVisible || validEntry
            }
        }
        contact_relations_image.beVisibleIf(areRelationsVisible)
        contact_relations_holder.beVisibleIf(areRelationsVisible)
        contact_relations_add_new.beVisibleIf(areRelationsVisible && !selectShareActive)

        var areGroupsVisible = ((showFields and SHOW_GROUPS_FIELD) != 0)
        if (areGroupsVisible) {
            areGroupsVisible = !selectShareActive
            val groupsHolder = contact_groups_holder
            groupsHolder.children.forEach { groupHolder ->
                val validEntry = groupHolder.contact_group.value.trim().isNotEmpty()
                groupHolder.beVisibleIf(validEntry || !selectShareActive)
                areGroupsVisible = areGroupsVisible || validEntry
            }
        }
        contact_groups_image.beVisibleIf(areGroupsVisible)
        contact_groups_holder.beVisibleIf(areGroupsVisible)
        contact_groups_add_new.beVisibleIf(areGroupsVisible && !selectShareActive)

        val isRingtoneVisible = ((showFields and SHOW_RINGTONE_FIELD) != 0)
        contact_ringtone_image.beVisibleIf(isRingtoneVisible)
        contact_ringtone.beVisibleIf(isRingtoneVisible)

        contact_source_image.beVisibleIf((showFields and SHOW_CONTACT_SOURCE_FIELD) != 0)
        contact_source.beVisibleIf((showFields and SHOW_CONTACT_SOURCE_FIELD) != 0)
    } // EditContactActivity.setupFieldVisibility()

    // *****************************************************************

    private fun setupEditContact() {
        val showShare: Boolean = state?.selectShareFieldsActive ?: false
        val showRemove: Boolean = config.showRemoveButtons
        val showContactFields = config.showContactFields

        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN)
        setupPhoto()
        setupNames(showShare)
        setupNicknames(showRemove && !showShare && ((showContactFields and SHOW_MULTIPLE_NICKNAMES) != 0),
            showShare, ((showContactFields and SHOW_NICKNAME_TYPES) != 0))
        setupPhoneNumbers(showRemove && !showShare, showShare)
        setupEmails(showRemove && !showShare, showShare)
        setupAddresses(showRemove && !showShare, showShare)
        setupIMs(showRemove && !showShare, showShare)
        setupEvents(!showShare, showShare)
        setupNotes(showShare)
        setupOrganization(showShare)
        setupWebsites(showRemove && !showShare, showShare)
        setupRelations(showRemove && !showShare, showShare)
        setupGroups(showRemove && !showShare, showShare)
        setupContactSource()
    } // EditContactActivity.setupEditContact()

    // *****************************************************************

    private fun setupPhoto() {
        contact_photo_share_info.isChecked = state?.sharePhoto ?: false
    } // EditContactActivity.setupPhoto()

    // *****************************************************************
    // *****************************************************************

    private fun setupNames(showShare: Boolean) {
        contact!!.apply {
            contact_display_name.setText(name.formattedName)
            contact_prefix.setText(name.prefix)
            contact_first_name.setText(name.givenName)
            contact_middle_name.setText(name.middleName)
            contact_surname.setText(name.familyName)
            contact_suffix.setText(name.suffix)
            contact_phonetic_given_name.setText(name.phoneticGivenName)
            contact_phonetic_middle_name.setText(name.phoneticMiddleName)
            contact_phonetic_surname.setText(name.phoneticFamilyName)
        }
    } // EditContactActivity.setupNames()

    // *****************************************************************
    // *****************************************************************

    private fun setupNicknames(showRemove: Boolean, showShare: Boolean, showType: Boolean) {
        val nicknames: ArrayList<ContactNickname> = contact!!.nicknames
        val nicknameListHolder: ViewGroup = contact_nicknames_holder!!
        if (nicknames.isNotEmpty()) {
            nicknames.forEachIndexed { index, nickname ->
                val nicknameHolder: ViewGroup = addNicknameField(nicknameListHolder, index, showRemove, showShare, showType)
                nicknameHolder.contact_nickname.setText(nickname.name)
                setupNicknameTypePicker(showType, nicknameHolder.contact_nickname_type, nickname.type, nickname.label)
                nicknameHolder.contact_nickname_share_info.isChecked = state.shareNickname[index]
            }
        } else /* (nicknames.isEmpty()) */ {
            addNicknameField(nicknameListHolder, 0, showRemove, showShare, showType)
        }
    } // EditContactActivity.setupNicknames()

    // *****************************************************************

    private fun addNicknameField(nicknameListHolder: ViewGroup, index: Int,
                                 showRemove: Boolean, showShare: Boolean, showType: Boolean) : ViewGroup {
        var nicknameHolder: ViewGroup? = nicknameListHolder.getChildAt(index) as ViewGroup?
        if (nicknameHolder == null) {
            nicknameHolder = layoutInflater.inflate(R.layout.item_edit_nickname, nicknameHolder, false) as ViewGroup
            nicknameListHolder.addView(nicknameHolder)
        }
        updateTextColors(nicknameHolder)
        setupNicknameTypePicker(showType, nicknameHolder.contact_nickname_type, DEFAULT_NICKNAME_TYPE, "")

        nicknameHolder.contact_nickname_remove.applyColorFilter(getProperPrimaryColor())
        nicknameHolder.contact_nickname_remove.background.applyColorFilter(getProperTextColor())
        nicknameHolder.contact_nickname_remove.setOnClickListener {
            removeNicknameField(nicknameHolder)
        }

        adjustNicknameFieldWidgets(nicknameHolder, showRemove, showShare)
        nicknameHolder.contact_nickname_share_info.isChecked = true

        return(nicknameHolder)
    } // EditContactActivity.addNicknameField()

    // *****************************************************************

    private fun addNewNicknameField() {
        val showContactFields = config.showContactFields
        val nicknameListHolder: LinearLayout = contact_nicknames_holder!!
        val nicknameHolder: ViewGroup = addNicknameField(nicknameListHolder,
            9999, config.showRemoveButtons && ((showContactFields and SHOW_MULTIPLE_NICKNAMES) != 0),
            false, ((showContactFields and SHOW_NICKNAME_TYPES) != 0))

        // nicknameHolder.contact_nickname.setText(getString(R.string.unknown))
        nicknameHolder.contact_nickname.alpha = 1.0f
        nicknameHolder.contact_nickname.setTextColor(getProperTextColor())

        contact_nicknames_holder.onGlobalLayout {
            nicknameHolder.contact_nickname.requestFocus()
            showKeyboard(nicknameHolder.contact_nickname)
        }
    } // EditContactActivity.addNewNicknameField()

    // *****************************************************************

    private fun removeNicknameField(nicknameHolder: View) {
        val showRemove: Boolean = config.showRemoveButtons
        val showContactFields = config.showContactFields
        val nicknameListHolder: ViewParent? = nicknameHolder.parent
        (nicknameHolder as ViewGroup).removeAllViews()
        (nicknameListHolder as ViewGroup).removeView(nicknameHolder as View)
        if (nicknameListHolder.childCount == 0)
            addNicknameField(nicknameListHolder,
                9999, showRemove && ((showContactFields and SHOW_MULTIPLE_NICKNAMES) != 0),
                false, ((showContactFields and SHOW_NICKNAME_TYPES) != 0))
    } // EditContactActivity.removeNicknameField()

    // *****************************************************************

    private fun setupNicknameTypePicker(showType: Boolean, nicknameTypeField: TextView, type: Int, label: String) {
        nicknameTypeField.beVisibleIf(showType)
        nicknameTypeField.apply {
            text = getNicknameTypeText(type, label)
            setOnClickListener {
                showNicknameTypePicker(it as TextView)
            }
        }
    } // EditContactActivity.setupNicknameTypePicker()

    // *****************************************************************

    fun getNicknameTypeText(type: Int, label: String): String {
        return if (type == BaseTypes.TYPE_CUSTOM) {
            label
        } else {
            getString(
                when (type) {
                    Nickname.TYPE_DEFAULT -> R.string.nickname_default
                    Nickname.TYPE_OTHER_NAME -> R.string.nickname_other_name
                    Nickname.TYPE_MAIDEN_NAME -> R.string.nickname_maiden_name
                    Nickname.TYPE_SHORT_NAME -> R.string.nickname_short_name
                    Nickname.TYPE_INITIALS -> R.string.nickname_initials
                    else -> R.string.other
                }
            )
        }
    }

    // *****************************************************************

    private fun showNicknameTypePicker(nicknameTypeField: TextView) {
        val items = arrayListOf(
            RadioItem(Nickname.TYPE_DEFAULT, getString(R.string.nickname_default)),
            RadioItem(Nickname.TYPE_OTHER_NAME, getString(R.string.nickname_other_name)),
            RadioItem(Nickname.TYPE_MAIDEN_NAME, getString(R.string.nickname_maiden_name)),
            RadioItem(Nickname.TYPE_SHORT_NAME, getString(R.string.nickname_short_name)),
            RadioItem(Nickname.TYPE_INITIALS, getString(R.string.nickname_initials)),
            RadioItem(Nickname.TYPE_CUSTOM, getString(R.string.custom))
        )

        val currentNicknameTypeId = getNicknameTypeId(nicknameTypeField.value)
        RadioGroupDialog(this, items, currentNicknameTypeId) {
            if (it as Int == Nickname.TYPE_CUSTOM) {
                CustomLabelDialog(this) {
                    nicknameTypeField.text = it
                }
            } else {
                nicknameTypeField.text = getNicknameTypeText(it, "")
            }
        }
    } // EditContactActivity.showNicknameTypePicker()

    // *****************************************************************

    private fun adjustNicknameFieldWidgets(nicknameHolder: ViewGroup, showRemove: Boolean, showShare: Boolean) {
        nicknameHolder.contact_nickname_remove.beVisibleIf(showRemove)
        nicknameHolder.contact_nickname_share_info.beVisibleIf(showShare)
        nicknameHolder.contact_nickname_share_icon.beVisibleIf(showShare)
    } // EditContactActivity.adjustNicknameFieldWidgets()

    // *****************************************************************
    // *****************************************************************

    private fun setupPhoneNumbers(showRemove: Boolean, showShare: Boolean) {
        val phoneNumbers: ArrayList<PhoneNumber> = contact!!.phoneNumbers
        val phoneNumberListHolder: ViewGroup = contact_numbers_holder!!
        if (phoneNumbers.isNotEmpty()) {
            phoneNumbers.forEachIndexed { index, phoneNumber ->
                val phoneNumberHolder: ViewGroup = addPhoneNumberField(phoneNumberListHolder, index, showRemove, showShare)
                phoneNumberHolder.contact_number.setText(phoneNumber.value)
                setupPhoneNumberTypePicker(phoneNumberHolder.contact_number_type, phoneNumber.type, phoneNumber.label)
                // adjustPhoneNumberFieldWidgets(phoneNumberHolder!! as ViewGroup, showRemove, showShare)
                phoneNumberHolder.contact_number_share_info.isChecked = state.shareNumber[index]

                if (highlightLastPhoneNumber && (index == (phoneNumbers.size - 1))) {
                    numberViewToColor = phoneNumberHolder.contact_number

                    phoneNumberHolder.default_toggle_icon.tag = if (phoneNumber.isPrimary) 1 else 0
                }
            }
            initNumberHolders()
        } else /* (phoneNumbers.isEmpty()) */ {
            addPhoneNumberField(phoneNumberListHolder, 0, showRemove, showShare)
        }
    } // EditContactActivity.setupPhoneNumbers()

    // *****************************************************************

    private fun addPhoneNumberField(phoneNumberListHolder: ViewGroup, index: Int,
                                    showRemove: Boolean, showShare: Boolean) : ViewGroup {
        var phoneNumberHolder: ViewGroup? = phoneNumberListHolder.getChildAt(index) as ViewGroup?
        if (phoneNumberHolder == null) {
            phoneNumberHolder = layoutInflater.inflate(R.layout.item_edit_phone_number, phoneNumberListHolder, false) as ViewGroup
            phoneNumberListHolder.addView(phoneNumberHolder)
        }
        updateTextColors(phoneNumberHolder)
        setupPhoneNumberTypePicker(phoneNumberHolder.contact_number_type, DEFAULT_PHONE_NUMBER_TYPE, "")

        phoneNumberHolder.contact_number_remove.applyColorFilter(getProperPrimaryColor())
        phoneNumberHolder.contact_number_remove.background.applyColorFilter(getProperTextColor())
        phoneNumberHolder.contact_number_remove.setOnClickListener {
            removePhoneNumberField(phoneNumberHolder)
        }

        phoneNumberHolder.default_toggle_icon.tag = 0
        initNumberHolders()

        // setupPhoneNumberTypePicker(phoneNumberHolder.contact_phoneNumber_type, DEFAULT_phoneNumber_TYPE, "")

        adjustPhoneNumberFieldWidgets(phoneNumberHolder, showRemove, showShare)
        phoneNumberHolder.contact_number_share_info.isChecked = true

        return(phoneNumberHolder)
    } // EditContactActivity.addPhoneNumberField()

    // *****************************************************************

    private fun addNewPhoneNumberField() {
        val phoneNumberListHolder: LinearLayout = contact_numbers_holder!!
        val phoneNumberHolder: ViewGroup = addPhoneNumberField(phoneNumberListHolder,
            9999, config.showRemoveButtons, false)

        // phoneNumberHolder.contact_number.setText(getString(R.string.unknown))
        phoneNumberHolder.contact_number.alpha = 1.0f
        phoneNumberHolder.contact_number.setTextColor(getProperTextColor())

        contact_numbers_holder.onGlobalLayout {
            phoneNumberHolder.contact_number.requestFocus()
            showKeyboard(phoneNumberHolder.contact_number)
        }
    } // EditContactActivity.addNewPhoneNumberField()

    // *****************************************************************

    private fun removePhoneNumberField(phoneNumberHolder: View) {
        val phoneNumberListHolder: ViewParent = phoneNumberHolder.parent
        (phoneNumberHolder as ViewGroup).removeAllViews()
        (phoneNumberListHolder as ViewGroup).removeView(phoneNumberHolder as View)
        if (phoneNumberListHolder.childCount == 0)
            addPhoneNumberField(phoneNumberListHolder, 9999, true, false)
    } // EditContactActivity.removePhoneNumberField()

    // *****************************************************************

    private fun setupPhoneNumberTypePicker(numberTypeField: TextView, type: Int, label: String) {
        numberTypeField.apply {
            text = getPhoneNumberTypeText(type, label)
            setOnClickListener {
                showNumberTypePicker(it as TextView)
            }
        }
    } // EditContactActivity.setupPhoneNumberTypePicker()

    // *****************************************************************

    private fun adjustPhoneNumberFieldWidgets(phoneNumberHolder: ViewGroup, showRemove: Boolean, showShare: Boolean) {
        phoneNumberHolder.contact_number_remove.beVisibleIf(showRemove)
        phoneNumberHolder.contact_number_share_info.beVisibleIf(showShare)
        phoneNumberHolder.contact_number_share_icon.beVisibleIf(showShare)
    } //  EditContactActivity.adjustPhoneNumberFieldWidgets()

    // *****************************************************************

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
    } // EditContactActivity.showNumberTypePicker()

    // *****************************************************************

    private fun initNumberHolders() {
        val numbersCount = contact_numbers_holder.childCount

        if (numbersCount == 1) {
            contact_numbers_holder.getChildAt(0).default_toggle_icon.beGone()
            return
        }

        for (i in 0 until numbersCount) {
            val toggleIcon = contact_numbers_holder.getChildAt(i).default_toggle_icon
            val isPrimary = (toggleIcon.tag == 1)

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
    } // EditContactActivity.initNumberHolders()

    // *****************************************************************

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
    } // EditContactActivity.setDefaultNumber()

    // *****************************************************************
    // *****************************************************************

    private fun setupEmails(showRemove: Boolean, showShare: Boolean) {
        val emails: ArrayList<Email> = contact!!.emails
        val emailListHolder: ViewGroup = contact_emails_holder!!
        if (emails.isNotEmpty()) {
            emails.forEachIndexed { index, email ->
                val emailHolder: ViewGroup = addEmailField(emailListHolder, index, showRemove, showShare)
                emailHolder.contact_email.setText(email.address)
                setupEmailTypePicker(emailHolder.contact_email_type, email.type, email.label)
                // adjustEmailFieldWidgets(emailHolder, showRemove, showShare)
                emailHolder.contact_email_share_info.isChecked = state.shareEmail[index]
                emailHolder.contact_email.alpha = 1.0f
            }
        } else /* (email.isEmpty()) */ {
            addEmailField(emailListHolder, 0, showRemove, showShare)
        }

    } // EditContactActivity.setupEmails()

    // *****************************************************************

    private fun addEmailField(emailListHolder: ViewGroup, index: Int,
                              showRemove: Boolean, showShare: Boolean) : ViewGroup {
        var emailHolder: ViewGroup? = emailListHolder.getChildAt(index) as ViewGroup?
        if (emailHolder == null) {
            emailHolder = layoutInflater.inflate(R.layout.item_edit_email, emailListHolder, false) as ViewGroup
            emailListHolder.addView(emailHolder)
        }
        emailHolder.contact_email_remove.applyColorFilter(getProperPrimaryColor())
        emailHolder.contact_email_remove.background.applyColorFilter(getProperTextColor())
        emailHolder.contact_email_remove.setOnClickListener {
            removeEmailField(emailHolder)
        }

        // setupEmailTypePicker(emailHolder.contact_email_type, DEFAULT_EMAIL_TYPE, "")

        adjustEmailFieldWidgets(emailHolder, showRemove, showShare)

        return(emailHolder)
    } // EditContactActivity.addEmailField()

    // *****************************************************************

    private fun addNewEmailField() {
        val emailListHolder: LinearLayout = contact_emails_holder!!
        val emailHolder: ViewGroup = addEmailField(emailListHolder,
            9999, config.showRemoveButtons, false)

        // emailHolder.contact_email.setText(getString(R.string.unknown))
        emailHolder.contact_email.alpha = 1.0f
        emailHolder.contact_email.setTextColor(getProperTextColor())

        emailListHolder.onGlobalLayout {
            emailHolder.contact_email.requestFocus()
            showKeyboard(emailHolder.contact_email)
        }
    } // EditContactActivity.addNewEmailField()

    // *****************************************************************

    private fun removeEmailField(emailHolder: View) {
        val emailListHolder: ViewParent = emailHolder.parent
        (emailHolder as ViewGroup).removeAllViews()
        (emailListHolder as ViewGroup).removeView(emailHolder as View)
        if (emailListHolder.childCount == 0)
            addEmailField(emailListHolder, 9999, true, false)
    } // EditContactActivity.removeEmailField()

    // *****************************************************************

    private fun setupEmailTypePicker(emailTypeField: TextView, type: Int, label: String) {
        emailTypeField.apply {
            text = getEmailTypeText(type, label)
            setOnClickListener {
                showEmailTypePicker(it as TextView)
            }
        }
    } // EditContactActivity.setupEmailTypePicker()

    // *****************************************************************

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
    } // EditContactActivity.showEmailTypePicker()

    // *****************************************************************

    private fun adjustEmailFieldWidgets(emailHolder: ViewGroup, showRemove: Boolean, showShare: Boolean) {
        emailHolder.contact_email_remove.beVisibleIf(showRemove)
        emailHolder.contact_email_share_info.beVisibleIf(showShare)
        emailHolder.contact_email_share_icon.beVisibleIf(showShare)
    } // EditContactActivity.adjustEmailFieldWidgets()

    // *****************************************************************
    // *****************************************************************

    private fun setupAddresses(showRemove: Boolean, showShare: Boolean) {
        val addresses: ArrayList<Address> = contact!!.addresses
        val addressListHolder: LinearLayout = contact_addresses_holder!!
        if (addresses.isNotEmpty()) {
            addresses.forEachIndexed { index, address ->
                val addressHolder = addAddressField(addressListHolder, index, showRemove, showShare)

                addressHolder.contact_address.setText(address.formattedAddress.trim())
                addressHolder.contact_address_street.setText(address.street.trim())
                addressHolder.contact_address_neighborhood.setText(address.neighborhood.trim())
                addressHolder.contact_address_postOfficeBox.setText(address.postOfficeBox.trim())
                addressHolder.contact_address_city.setText(address.city.trim())
                addressHolder.contact_address_region.setText(address.region.trim())
                addressHolder.contact_address_postalCode.setText(address.postalCode.trim())
                addressHolder.contact_address_country.setText(address.country.trim())

                addressHolder.contact_address_topline.setText(getTopVisibleAddressField(addressHolder)?.value)
                setupAddressTypePicker(addressHolder.contact_address_type, address.type, address.label)
                setupRefreshDisplayAddress(addressHolder as ViewGroup)
                // adjustAddressFieldWidgets(addressHolder, addressHolder.contact_address_topline, showRemove, showShare)
                addressHolder.contact_address_share_info.isChecked = state.shareAddress[index]
            }
        }
        else
            addAddressField(addressListHolder, 0, showRemove, showShare)
    } // EditContactActivity.setupAddresses()

    // *****************************************************************

    private fun addAddressField(addressListHolder: ViewGroup, index: Int,
                                showRemove: Boolean, showShare: Boolean) : ViewGroup {
        var addressHolder: ViewGroup? = addressListHolder.getChildAt(index) as ViewGroup?
        if (addressHolder == null) {
            addressHolder = layoutInflater.inflate(R.layout.item_edit_address, addressListHolder, false) as ViewGroup
            updateTextColors(addressHolder)
            addressListHolder.addView(addressHolder)
        }
        val activityAddressCount: Int = addressListHolder.childCount
        if (state.shareAddress.size < activityAddressCount)
            state.shareAddress.add(true)
        if (state.autoCalcFormattedAddress.size < activityAddressCount)
            state.autoCalcFormattedAddress.add(true)

        setupAddressFieldVisibility(addressHolder)
        val topVisibleLine: EditText? = setupAddressFieldOrder(addressHolder)
        adjustAddressFieldWidgets(addressHolder, topVisibleLine, showRemove, showShare)
        setupAddressHolderCallbacks(addressHolder)

        setupAddressTypePicker(addressHolder.contact_address_type, DEFAULT_ADDRESS_TYPE, "")
        setupRefreshDisplayAddress(addressHolder as ViewGroup)

        addressHolder.contact_address_remove.applyColorFilter(getProperPrimaryColor())
        addressHolder.contact_address_remove.background.applyColorFilter(getProperTextColor())
        addressHolder.contact_address_remove.setOnClickListener {
            removeAddressField(addressHolder)
        }

        return(addressHolder)
    } // EditContactActivity.addAddressField()

    // *****************************************************************

    private fun setupAddressFieldVisibility(addressHolder : View) {
        val showFields = config.showContactFields
        addressHolder.contact_refresh_display_address.beVisibleIf(
            ((showFields and SHOW_ADDRESSES_FIELD) != 0) &&
                    ((showFields and SHOW_STRUCTURED_POSTAL_ACTIVE_ADDRESS_MASK) != 0) &&
                    config.showUpdateFormattedAddressButton)
        addressHolder.contact_address.beVisibleIf((showFields and SHOW_ADDRESSES_FIELD) != 0)
        addressHolder.contact_address_street.beVisibleIf((showFields and SHOW_STRUCTURED_POSTAL_ADDRESS_STREET) != 0)
        addressHolder.contact_address_neighborhood.beVisibleIf((showFields and SHOW_STRUCTURED_POSTAL_ADDRESS_EXTADDR) != 0)
        addressHolder.contact_address_postOfficeBox.beVisibleIf((showFields and SHOW_STRUCTURED_POSTAL_ADDRESS_POBOX) != 0)
        addressHolder.contact_address_city.beVisibleIf((showFields and SHOW_STRUCTURED_POSTAL_ADDRESS_CITY) != 0)
        addressHolder.contact_address_region.beVisibleIf((showFields and SHOW_STRUCTURED_POSTAL_ADDRESS_REGION) != 0)
        addressHolder.contact_address_postalCode.beVisibleIf((showFields and SHOW_STRUCTURED_POSTAL_ADDRESS_POSTCODE) != 0)
        addressHolder.contact_address_country.beVisibleIf((showFields and SHOW_STRUCTURED_POSTAL_ADDRESS_COUNTRY) != 0)
    } // EditContactActivity.setupAddressFieldVisibility()

    // *****************************************************************

    // Reorder the fields of the address in the order that is usually used in
    // the current locale.
    // https://stackoverflow.com/questions/3277196/can-i-set-androidlayout-below-at-runtime-programmatically
    // infix fun View.below(view: View) {
    //    (this.layoutParams as? RelativeLayout.LayoutParams)?.addRule(RelativeLayout.BELOW, view.id)
    // }
    private fun setupAddressFieldOrder(addressHolder : View) : EditText? {
        /* %A Streetaddress
           %D Neighborhood
           %B POBox
           %C City
           %S State/Region
           %Z PostalCode
           %N Nation/Country */
        // Note: We shall reuse the address_format string. While this does bring some
        // useless extra characters that need to be skipped while parsing, it makes sure
        // that the input and output address format are the same.
        // e.g. US: <string name="address_format">%A%n%D%n%B%n{%C,} %S %Z%n%N%n</string>
        // e.g. Austria: <string name="address_format">%A%n%D%n%B%n%Z %C{ (%S)}%n%N%n</string>

        val showFields = config.showContactFields
        val fieldOrder: String = getString(R.string.address_format)
        var prevField: EditText = addressHolder.contact_address
        var currField: EditText?
        var currVisible: Boolean
        var pos = 0
        val fieldOrderLen = fieldOrder.length
        var prevC: Char = '*'
        var C: Char

        var topVisibleField: EditText? = (
            if ((showFields and SHOW_ADDRESSES_FIELD) != 0)
                addressHolder.contact_address
            else
                null
            )

        (addressHolder.contact_address_street.layoutParams as? RelativeLayout.LayoutParams)?.removeRule(RelativeLayout.BELOW)
        (addressHolder.contact_address_neighborhood.layoutParams as? RelativeLayout.LayoutParams)?.removeRule(RelativeLayout.BELOW)
        (addressHolder.contact_address_postOfficeBox.layoutParams as? RelativeLayout.LayoutParams)?.removeRule(RelativeLayout.BELOW)
        (addressHolder.contact_address_city.layoutParams as? RelativeLayout.LayoutParams)?.removeRule(RelativeLayout.BELOW)
        (addressHolder.contact_address_postalCode.layoutParams as? RelativeLayout.LayoutParams)?.removeRule(RelativeLayout.BELOW)
        (addressHolder.contact_address_country.layoutParams as? RelativeLayout.LayoutParams)?.removeRule(RelativeLayout.BELOW)

        while (pos < fieldOrderLen) {
            C = fieldOrder[pos]
            if (prevC == '%') {
                if (C == 'A') {
                    currField = addressHolder.contact_address_street
                    currVisible = ((showFields and SHOW_STRUCTURED_POSTAL_ADDRESS_STREET) != 0)
                } else if (C == 'D') {
                    currField = addressHolder.contact_address_neighborhood
                    currVisible = ((showFields and SHOW_STRUCTURED_POSTAL_ADDRESS_EXTADDR) != 0)
                } else if (C == 'B') {
                    currField = addressHolder.contact_address_postOfficeBox
                    currVisible = ((showFields and SHOW_STRUCTURED_POSTAL_ADDRESS_POBOX) != 0)
                } else if (C == 'C') {
                    currField = addressHolder.contact_address_city
                    currVisible = ((showFields and SHOW_STRUCTURED_POSTAL_ADDRESS_CITY) != 0)
                } else if (C == 'S') {
                    currField = addressHolder.contact_address_region
                    currVisible = ((showFields and SHOW_STRUCTURED_POSTAL_ADDRESS_REGION) != 0)
                } else if (C == 'Z') {
                    currField = addressHolder.contact_address_postalCode
                    currVisible = ((showFields and SHOW_STRUCTURED_POSTAL_ADDRESS_POSTCODE) != 0)
                } else if (C == 'N') {
                    currField = addressHolder.contact_address_country
                    currVisible = ((showFields and SHOW_STRUCTURED_POSTAL_ADDRESS_COUNTRY) != 0)
                } else {
                    currField = null
                    currVisible = false
                }

                if ((topVisibleField == null) && currVisible)
                    topVisibleField = currField

                if (currField != null) {
                    if (prevField != null)
                        (currField.layoutParams as? RelativeLayout.LayoutParams)?.addRule(RelativeLayout.BELOW, prevField.id)
                    prevField = currField
                }
            }
            prevC = C
            pos++
        }
        return(topVisibleField)
    } // EditContactActivity.setupAddressFieldOrder()

    // *****************************************************************

    private fun getTopVisibleAddressField(addressHolder : View) : EditText? {
        /* %A Streetaddress
           %D Neighborhood
           %B POBox
           %C City
           %S State/Region
           %Z PostalCode
           %N Nation/Country */
        // Note: We shall reuse the address_format string. While this does bring some
        // useless extra characters that need to be skipped while parsing, it makes sure
        // that the input and output address format are the same.
        // e.g. US: <string name="address_format">%A%n%D%n%B%n{%C,} %S %Z%n%N%n</string>
        // e.g. Austria: <string name="address_format">%A%n%D%n%B%n%Z %C{ (%S)}%n%N%n</string>

        val showFields = config.showContactFields
        if ((showFields and SHOW_ADDRESSES_FIELD) != 0)
            return(addressHolder.contact_address)

        val fieldOrder: String = getString(R.string.address_format)
        val fieldOrderLen = fieldOrder.length
        var currField: EditText?
        var currVisible: Boolean
        var pos = 0
        var prevC: Char = '*'
        var C: Char

        while (pos < fieldOrderLen) {
            C = fieldOrder[pos]
            if (prevC == '%') {
                if (C == 'A') {
                    currField = addressHolder.contact_address_street
                    currVisible = ((showFields and SHOW_STRUCTURED_POSTAL_ADDRESS_STREET) != 0)
                } else if (C == 'D') {
                    currField = addressHolder.contact_address_neighborhood
                    currVisible = ((showFields and SHOW_STRUCTURED_POSTAL_ADDRESS_EXTADDR) != 0)
                } else if (C == 'B') {
                    currField = addressHolder.contact_address_postOfficeBox
                    currVisible = ((showFields and SHOW_STRUCTURED_POSTAL_ADDRESS_POBOX) != 0)
                } else if (C == 'C') {
                    currField = addressHolder.contact_address_city
                    currVisible = ((showFields and SHOW_STRUCTURED_POSTAL_ADDRESS_CITY) != 0)
                } else if (C == 'S') {
                    currField = addressHolder.contact_address_region
                    currVisible = ((showFields and SHOW_STRUCTURED_POSTAL_ADDRESS_REGION) != 0)
                } else if (C == 'Z') {
                    currField = addressHolder.contact_address_postalCode
                    currVisible = ((showFields and SHOW_STRUCTURED_POSTAL_ADDRESS_POSTCODE) != 0)
                } else if (C == 'N') {
                    currField = addressHolder.contact_address_country
                    currVisible = ((showFields and SHOW_STRUCTURED_POSTAL_ADDRESS_COUNTRY) != 0)
                } else {
                    currField = null
                    currVisible = false
                }

                if (currVisible)
                    return(currField)
            }
            prevC = C
            pos++
        }
        return(null)
    } // EditContactActivity.getTopVisibleAddressField()

    // *****************************************************************

    private fun setupAddressHolderCallbacks(addressHolder : View) {
        addressHolder.contact_address_topline.setOnFocusChangeListener { v, hasFocus -> onFormattedAddressFocusChanged(v, hasFocus) }
        addressHolder.contact_address_topline.doAfterTextChanged { edit -> onAddressToplineChanged(addressHolder) }
        addressHolder.contact_address.setOnFocusChangeListener { v, hasFocus -> onFormattedAddressFocusChanged(v, hasFocus) }
        addressHolder.contact_address_street.doAfterTextChanged { edit -> setAutoFormattedAddress(addressHolder) }
        addressHolder.contact_address_neighborhood.doAfterTextChanged { edit -> setAutoFormattedAddress(addressHolder) }
        addressHolder.contact_address_postOfficeBox.doAfterTextChanged { edit -> setAutoFormattedAddress(addressHolder) }
        addressHolder.contact_address_city.doAfterTextChanged { edit -> setAutoFormattedAddress(addressHolder) }
        addressHolder.contact_address_region.doAfterTextChanged { edit -> setAutoFormattedAddress(addressHolder) }
        addressHolder.contact_address_postalCode.doAfterTextChanged { edit -> setAutoFormattedAddress(addressHolder) }
        addressHolder.contact_address_country.doAfterTextChanged { edit -> setAutoFormattedAddress(addressHolder) }
    } // EditContactActivity.setupAddressHolderCallbacks()

    // *****************************************************************

    private fun addNewAddressField() {
        val addressListHolder: LinearLayout = contact_addresses_holder!!
        val addressHolder: ViewGroup = addAddressField(addressListHolder,
            9999, config.showRemoveButtons, false)

        addressListHolder.onGlobalLayout {
            addressHolder.contact_address_street.requestFocus()
            showKeyboard(addressHolder.contact_address_street)
        }
    } // EditContactActivity.addNewAddressField()

    // *****************************************************************

    private fun removeAddressField(addressHolder: View) {
        val addressListHolder: ViewGroup = (addressHolder.parent as ViewGroup)
        val addressIndex = addressListHolder.indexOfChild(addressHolder)
        if (addressIndex < state.shareAddress.size)
            state.shareAddress.removeAt(addressIndex)
        if (addressIndex < state.autoCalcFormattedAddress.size)
            state.autoCalcFormattedAddress.removeAt(addressIndex)

        (addressHolder as ViewGroup).removeAllViews()
        addressListHolder.removeView(addressHolder as View)
        if (addressListHolder.childCount == 0)
            addAddressField(addressListHolder, 9999, true, false)
    } // EditContactActivity.removeAddressField()

    // *****************************************************************

    private fun setupAddressTypePicker(addressTypeField: TextView, type: Int, label: String) {
        addressTypeField.apply {
            text = getAddressTypeText(type, label)
            setOnClickListener {
                showAddressTypePicker(it as TextView)
            }
        }
    } // EditContactActivity.setupAddressTypePicker()

    // *****************************************************************

    private fun setupRefreshDisplayAddress(addressHolder: ViewGroup) {
        addressHolder.contact_refresh_display_address.setOnClickListener {
            val addressIndex: Int = (addressHolder.parent as ViewGroup).indexOfChild(addressHolder)
            state.autoCalcFormattedAddress[addressIndex] = true
            setAutoFormattedAddress(addressHolder)
        }
    } // EditContactActivity.setupRefreshDisplayAddress()

    // *****************************************************************

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
    } // EditContactActivity.showAddressTypePicker()

    // *****************************************************************

    private fun adjustAddressFieldWidgets(addressHolder: ViewGroup, topLine: EditText?, showRemove: Boolean, showShare: Boolean) {
        if (topLine == null)
            return
        addressHolder.contact_address_topline.hint = topLine.hint
        addressHolder.contact_address_topline.setText(topLine.value)
        topLine.beGone()

        // - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

        addressHolder.contact_address_remove.beVisibleIf(showRemove)
        addressHolder.contact_address_share_info.beVisibleIf(showShare)
        addressHolder.contact_address_share_icon.beVisibleIf(showShare)
    } // EditContactActivity.adjustAddressFieldWidgets()

    // *****************************************************************
    // *****************************************************************

    private fun setupIMs(showRemove: Boolean, showShare: Boolean) {
        val IMs: ArrayList<IM> = contact!!.IMs
        val imListHolder: LinearLayout = contact_ims_holder!!
        if (IMs.isNotEmpty()) {
            IMs.forEachIndexed { index, IM ->
                var imHolder = addIMField(imListHolder, index, showRemove, showShare)
                // imHolder.contact_im.setText(IM.value)
                imHolder.contact_im.setText(IM.data)
                setupIMProtocolPicker(imHolder.contact_im_protocol, IM.protocol, IM.custom_protocol)
                setupIMTypePicker(imHolder.contact_im_type, IM.type, IM.label)
                // adjustIMFieldWidgets(imHolder, showRemove, showShare)
                val shareIM: Boolean = state.shareIM[index]
                imHolder.contact_im_share_info.setChecked(shareIM)
            }
        } else /* (IMs.isEmpty()) */ {
            addIMField(imListHolder, 0, showRemove, showShare)
        }
    } // EditContactActivity.setupIMs()

    // *****************************************************************

    private fun addIMField(imListHolder: ViewGroup, index: Int,
                           showRemove: Boolean, showShare: Boolean) : ViewGroup {
        var imHolder: ViewGroup?
        var childCnt = imListHolder.childCount
        var childIndex: Int = index
        if (childIndex >= childCnt) {
            imHolder = layoutInflater.inflate(R.layout.item_edit_im, imListHolder, false) as ViewGroup
            updateTextColors(imHolder)
            imListHolder.addView(imHolder)
            childIndex = imListHolder.childCount - 1
        }

        imHolder = imListHolder.getChildAt(childIndex) as ViewGroup?
        if (imHolder == null) {
            imHolder = layoutInflater.inflate(R.layout.item_edit_im, imListHolder, false) as ViewGroup
            updateTextColors(imHolder)
            imListHolder.addView(imHolder)
        }
        imHolder.contact_im_remove.applyColorFilter(getProperPrimaryColor())
        imHolder.contact_im_remove.background.applyColorFilter(getProperTextColor())
        imHolder.contact_im_remove.setOnClickListener {
            removeIMField(imHolder)
        }

        setupIMProtocolPicker(imHolder.contact_im_protocol, DEFAULT_IM_PROTOCOL, "")
        setupIMTypePicker(imHolder.contact_im_type, DEFAULT_IM_TYPE, "")

        adjustIMFieldWidgets(imHolder, showRemove, showShare)

        return(imHolder)
    } // EditContactActivity.addIMField()

    // *****************************************************************

    private fun addNewIMField() {
        val imListHolder: LinearLayout = contact_ims_holder!!
        val imHolder: ViewGroup = addIMField(imListHolder,
            9999, config.showRemoveButtons, false)

        imListHolder.onGlobalLayout {
            imHolder.contact_im.requestFocus()
            showKeyboard(imHolder.contact_im)
        }
    } // EditContactActivity.addNewIMField()

    // *****************************************************************

    private fun removeIMField(imHolder: View) {
        val imListHolder: ViewParent = imHolder.parent
        (imHolder as ViewGroup).removeAllViews()
        (imListHolder as ViewGroup).removeView(imHolder as View)
        if (imListHolder.childCount == 0)
            addIMField(imListHolder, 9999, true, false)
    } // EditContactActivity.removeIMField()

    // *****************************************************************

    private fun setupIMProtocolPicker(imProtocolField: TextView, protocol: Int, custom_protocol: String) {
        imProtocolField.text = getIMProtocolText(protocol, custom_protocol)
        imProtocolField.setOnClickListener {
            showIMProtocolPicker(it as TextView)
        }
    } // EditContactActivity.setupIMProtocolPicker()

    // *****************************************************************

    private fun showIMProtocolPicker(imProtocolField: TextView) {
        val items = arrayListOf(
            RadioItem(Im.PROTOCOL_CUSTOM,   getString(R.string.custom)),
            RadioItem(IM.PROTOCOL_IRC,      getString(R.string.instantmsg_irc)),
            RadioItem(IM.PROTOCOL_MATRIX,   getString(R.string.instantmsg_matrix)),
            RadioItem(Im.PROTOCOL_JABBER,   getString(R.string.instantmsg_xmpp_jabber)),
            RadioItem(IM.PROTOCOL_MASTODON, getString(R.string.instantmsg_mastodon)),
            RadioItem(IM.PROTOCOL_SIGNAL,   getString(R.string.instantmsg_signal)),
            RadioItem(IM.PROTOCOL_TELEGRAM, getString(R.string.telegram)),
            RadioItem(IM.PROTOCOL_DIASPORA, getString(R.string.instantmsg_diaspora)),

            // Which of the following protocols are important enough to
            // be included in the list of easily selectable instant messengers?
            // RadioItem(IM.PROTOCOL_VIBER,    getString(R.string.instantmsg_viber)),
            // RadioItem(IM.PROTOCOL_THREEMA,  getString(R.string.instantmsg_threema)),
            // RadioItem(IM.PROTOCOL_DISCORD,  getString(R.string.instantmsg_discord)),
            // RadioItem(IM.PROTOCOL_MUMBLE,   getString(R.string.instantmsg_mumble)),
            // RadioItem(IM.PROTOCOL_OLVID,    getString(R.string.instantmsg_olvid)),
            // RadioItem(IM.PROTOCOL_TEAMSPEAK,getString(R.string.instantmsg_teamspeak)),

            // The following sites are not really Instant Message Services and
            // should probably be sorted as Websites.
            // RadioItem(IM.PROTOCOL_FACEBOOK,getString(R.string.instantmsg_facebook)),
            // RadioItem(IM.PROTOCOL_INSTAGRAM,getString(R.string.instantmsg_instagram)),
            // RadioItem(IM.PROTOCOL_WHATSAPP, getString(R.string.instantmsg_whatsapp)),
            // RadioItem(IM.PROTOCOL_TWITTER,  getString(R.string.instantmsg_twitter)),
            // RadioItem(IM.PROTOCOL_WEIBO,    getString(R.string.instantmsg_weibo)),
            // RadioItem(IM.PROTOCOL_TIKTOK,   getString(R.string.instantmsg_tiktok)),
            // RadioItem(IM.PROTOCOL_TUMBLR,   getString(R.string.instantmsg_tumblr)),
            // RadioItem(IM.PROTOCOL_FLICKR,   getString(R.string.instantmsg_flickr)),
            // RadioItem(IM.PROTOCOL_LINKEDIN, getString(R.string.instantmsg_linkedin)),
            // RadioItem(IM.PROTOCOL_XING,     getString(R.string.instantmsg_xing)),

            RadioItem(Im.PROTOCOL_AIM, getString(R.string.instantmsg_aim)),
            RadioItem(Im.PROTOCOL_MSN, getString(R.string.instantmsg_windows_live)),
            RadioItem(Im.PROTOCOL_YAHOO, getString(R.string.instantmsg_yahoo)),
            RadioItem(Im.PROTOCOL_SKYPE, getString(R.string.instantmsg_skype)),
            RadioItem(Im.PROTOCOL_QQ, getString(R.string.instantmsg_qq)),
            RadioItem(Im.PROTOCOL_GOOGLE_TALK, getString(R.string.instantmsg_hangouts)),
            RadioItem(Im.PROTOCOL_ICQ, getString(R.string.instantmsg_icq)),

            // Microsoft Netmeeting has been discontinued. We shall no longer list it...
            // RadioItem(Im.PROTOCOL_NETMEETING, getString(R.string.netmeeting))
        )

        val currentIMTypeId = getIMProtocolId(imProtocolField.value)
        RadioGroupDialog(this, items, currentIMTypeId) {
            if (it as Int == Im.PROTOCOL_CUSTOM) {
                CustomLabelDialog(this) {
                    imProtocolField.text = it
                }
            } else {
                imProtocolField.text = getIMProtocolText(it, "")
            }
        }
    } // EditContactActivity.showIMProtocolPicker()

    // *****************************************************************

    private fun setupIMTypePicker(imTypeField: TextView, type: Int, label: String) {
        imTypeField.text = getIMTypeText(type, label)
        imTypeField.setOnClickListener {
            showIMTypePicker(it as TextView)
        }
    } // EditContactActivity.setupIMTypePicker()

    // *****************************************************************

    private fun showIMTypePicker(imTypeField: TextView) {
        val items = arrayListOf(
            RadioItem(Im.TYPE_HOME, getString(R.string.home)),
            RadioItem(Im.TYPE_WORK, getString(R.string.work)),
            RadioItem(Im.TYPE_OTHER, getString(R.string.other)),
            RadioItem(Im.TYPE_CUSTOM, getString(R.string.custom))
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
    } // EditContactActivity.showIMTypePicker()

    // *****************************************************************

    private fun adjustIMFieldWidgets(imHolder: ViewGroup, showRemove: Boolean, showShare: Boolean) {
        imHolder.contact_im_remove.beVisibleIf(showRemove)
        imHolder.contact_im_share_info.beVisibleIf(showShare)
        imHolder.contact_im_share_icon.beVisibleIf(showShare)
    } // EditContactActivity.adjustIMFieldWidgets()

    // *****************************************************************
    // *****************************************************************

    private fun setupEvents(showRemove: Boolean, showShare: Boolean) {
        val events: ArrayList<Event> = contact!!.events
        val eventListHolder: ViewGroup = contact_events_holder!!
        if (events.isNotEmpty()) {
            events.forEachIndexed { index, event ->
                val eventItemHolder = addEventField(eventListHolder, index, showRemove, showShare)

                (eventItemHolder.contact_event).apply {
                    if (event.startDate.isNotEmpty())
                        event.startDate.getDateTimeFromDateString(true, this)
                    else
                        ""
                    tag = event.startDate
                }

                setupEventTypePicker(eventItemHolder, event.type, event.label)
                // adjustEventFieldWidgets(eventItemHolder, showRemove, showShare)
                val shareEvent: Boolean = state.shareEvent[index]
                eventItemHolder.contact_event_share_info.setChecked(shareEvent)
            }
        } else /* (events.isEmpty()) */ {
            addEventField(eventListHolder, 0, showRemove, showShare)
        }
    } // EditContactActivity.setupEvents()

    // *****************************************************************

    private fun addEventField(eventListHolder: ViewGroup, index: Int,
                              showRemove: Boolean, showShare: Boolean) : ViewGroup {
        var eventHolder: ViewGroup? = eventListHolder.getChildAt(index) as ViewGroup?
        if (eventHolder == null) {
            eventHolder = layoutInflater.inflate(R.layout.item_edit_event, eventListHolder, false) as ViewGroup
            contact_events_holder.addView(eventHolder)
        }
        eventHolder.contact_event_remove.applyColorFilter(getProperPrimaryColor())
        eventHolder.contact_event_remove.background.applyColorFilter(getProperTextColor())
        eventHolder.contact_event_remove.setOnClickListener {
            removeEventField(eventHolder)
        }

        setupEventTypePicker(eventHolder)

        adjustEventFieldWidgets(eventHolder, showRemove, showShare)

        return(eventHolder)
    } // EditContactActivity.addEventField()

    // *****************************************************************

    private fun addNewEventField() {
        val eventListHolder: LinearLayout = contact_events_holder!!
        val eventHolder: ViewGroup = addEventField(eventListHolder,
            9999, config.showRemoveButtons, false)

        (eventHolder.contact_event).apply {
            setTextColor(getProperTextColor())
        }
    } // EditContactActivity.addNewEventField()

    // *****************************************************************

    private fun removeEventField(eventHolder: View) {
        val eventListHolder: ViewParent? = eventHolder.parent
        (eventHolder as ViewGroup).removeAllViews()
        (eventListHolder as ViewGroup).removeView(eventHolder as View)
        if (eventListHolder.childCount == 0)
            addNewEventField()
    } // EditContactActivity.removeEventField()

    // *****************************************************************

    private fun setupEventTypePicker(eventHolder: ViewGroup, type: Int = DEFAULT_EVENT_TYPE, label: String = "") {
        eventHolder.contact_event_type.apply {
            setText(getEventTypeText(type, label))
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
                }
            }
        }
    } // EditContactActivity.setupEventTypePicker()

    // *****************************************************************

    private fun showEventTypePicker(eventTypeField: TextView) {
        val items =
            if (config.permitCustomEventTypes)
                arrayListOf(
                    RadioItem(CommonDataKinds.Event.TYPE_ANNIVERSARY, getString(R.string.anniversary)),
                    RadioItem(CommonDataKinds.Event.TYPE_BIRTHDAY, getString(R.string.birthday)),
                    RadioItem(CommonDataKinds.Event.TYPE_OTHER, getString(R.string.other)),
                    RadioItem(CommonDataKinds.Event.TYPE_CUSTOM, getString(R.string.custom)))
            else
                arrayListOf(
                    RadioItem(CommonDataKinds.Event.TYPE_ANNIVERSARY, getString(R.string.anniversary)),
                    RadioItem(CommonDataKinds.Event.TYPE_BIRTHDAY, getString(R.string.birthday)))

        val currentEventTypeId = getEventTypeId(eventTypeField.value)
        // RadioGroupDialog(this, items, currentEventTypeId) {
        //     eventTypeField.setText(getEventTextId(it as Int))
        // }

        RadioGroupDialog(this, items, currentEventTypeId) {
            if (it as Int == CommonDataKinds.Event.TYPE_CUSTOM) {
                CustomLabelDialog(this) {
                    eventTypeField.text = it
                }
            } else {
                eventTypeField.text = getEventTypeText(it, "")
            }
        }

    } // EditContactActivity.showEventTypePicker()

    // *****************************************************************

    private fun adjustEventFieldWidgets(eventHolder: ViewGroup, showRemove: Boolean, showShare: Boolean) {
        eventHolder.contact_event_remove.beVisibleIf(showRemove)
        eventHolder.contact_event_share_info.beVisibleIf(showShare)
        eventHolder.contact_event_share_icon.beVisibleIf(showShare)
    } // EditContactActivity.adjustEventFieldWidgets()

    // *****************************************************************
    // *****************************************************************

    private fun setupNotes(showShare: Boolean) {   // FIXME FIXME
        val notesHolder: ViewGroup = contact_notes_holder
        notesHolder.contact_note.setText(contact!!.notes)
        notesHolder.contact_note_remove.beVisibleIf(false)
        notesHolder.contact_note_share_info.beVisibleIf(showShare)
        notesHolder.contact_note_share_icon.beVisibleIf(showShare)
        notesHolder.contact_note_share_info.isChecked = state.shareNotes
    } // EditContactActivity.setupNotes()

    // *****************************************************************
    // *****************************************************************

    private fun setupOrganization(showShare: Boolean) {
        val organizationHolder: ViewGroup = contact_organization_holder.contact_organization_holder
        organizationHolder.contact_organization_company.setText(contact!!.organization.company)
        organizationHolder.contact_organization_job_title.setText(contact!!.organization.jobTitle)
        adjustOrganisationFieldWidgets(organizationHolder, showShare)
        organizationHolder.contact_organization_share_info.isChecked = state.shareOrganization
    } // setupOrganization.setupOrganization()

    // *****************************************************************

    private fun adjustOrganisationFieldWidgets(organizationHolder: ViewGroup, showShare: Boolean) {
        organizationHolder.contact_organization_share_info.beVisibleIf(showShare)
        organizationHolder.contact_organization_share_icon.beVisibleIf(showShare)
    } // EditContactActivity.adjustOrganisationFieldWidgets()

    // *****************************************************************
    // *****************************************************************

    private fun setupWebsites(showRemove: Boolean, showShare: Boolean) {
        val websites: ArrayList<ContactWebsite> = contact!!.websites
        val websiteListHolder: LinearLayout = contact_websites_holder!!
        if (websites.isNotEmpty()) {
            websites.forEachIndexed { index, website ->
                var websiteHolder = addWebsiteField(websiteListHolder, index, showRemove, showShare)
                websiteHolder.contact_website.setText(website.URL)
                setupWebsiteTypePicker(websiteHolder.contact_website_type, website.type, website.label)
                // adjustWebsiteFieldWidgets(websiteHolder, showRemove, showShare)
                websiteHolder.contact_website_share_info.isChecked = state.shareWebsite[index]
            }
        } else /* (websites.isEmpty()) */ {
            addWebsiteField(websiteListHolder, 0, showRemove, showShare)
        }
    } // EditContactActivity.setupWebsites()

    // *****************************************************************

    private fun addWebsiteField(websiteListHolder: ViewGroup, index: Int,
                                showRemove: Boolean, showShare: Boolean) : ViewGroup {
        var websiteHolder: ViewGroup? = websiteListHolder.getChildAt(index) as ViewGroup?
        if (websiteHolder == null) {
            websiteHolder = layoutInflater.inflate(R.layout.item_edit_website, websiteListHolder, false) as ViewGroup
            updateTextColors(websiteHolder)
            websiteListHolder.addView(websiteHolder)
        }
        websiteHolder.contact_website_remove.applyColorFilter(getProperPrimaryColor())
        websiteHolder.contact_website_remove.background.applyColorFilter(getProperTextColor())
        websiteHolder.contact_website_remove.setOnClickListener {
            removeWebsiteField(websiteHolder)
        }

        setupWebsiteTypePicker(websiteHolder.contact_website_type, DEFAULT_WEBSITE_TYPE, "")

        adjustWebsiteFieldWidgets(websiteHolder, showRemove, showShare)

        return(websiteHolder)
    } // EditContactActivity.addWebsiteField()

    // *****************************************************************

    private fun addNewWebsiteField() {
        val websiteListHolder: LinearLayout = contact_websites_holder!!
        val websiteHolder: ViewGroup = addWebsiteField(websiteListHolder,
            9999, config.showRemoveButtons, false)

        websiteListHolder.onGlobalLayout {
            websiteHolder.contact_website.requestFocus()
            showKeyboard(websiteHolder.contact_website)
        }
    } // EditContactActivity.addNewWebsiteField()

    // *****************************************************************

    private fun removeWebsiteField(websiteHolder: View) {
        val websiteListHolder: ViewParent? = websiteHolder.parent
        (websiteHolder as ViewGroup).removeAllViews()
        (websiteListHolder as ViewGroup).removeView(websiteHolder as View)
        if (websiteListHolder.childCount == 0)
            addWebsiteField(websiteListHolder, 9999, true, false)
    } // EditContactActivity.removeWebsiteField()

    // *****************************************************************

    private fun setupWebsiteTypePicker(websiteTypeField: TextView, type: Int, label: String) {
        websiteTypeField.text = getWebsiteTypeText(type, label)
        websiteTypeField.setOnClickListener {
            showWebsiteTypePicker(websiteTypeField)
        }
    } // EditContactActivity.setupWebsiteTypePicker()

    // *****************************************************************

    private fun showWebsiteTypePicker(websiteTypeField: TextView) {
        val items = arrayListOf(
            RadioItem(Website.TYPE_HOME, getString(R.string.home)),
            RadioItem(Website.TYPE_WORK, getString(R.string.work)),
            RadioItem(Website.TYPE_HOMEPAGE, getString(R.string.website_homepage)),
            RadioItem(Website.TYPE_BLOG, getString(R.string.website_blog)),
            RadioItem(Website.TYPE_PROFILE, getString(R.string.website_profile)),
            RadioItem(Website.TYPE_FTP, getString(R.string.website_ftp)),
            RadioItem(Website.TYPE_OTHER, getString(R.string.other)),
            RadioItem(Website.TYPE_CUSTOM, getString(R.string.custom))
        )

        val currentWebsiteTypeId = getWebsiteTypeId(websiteTypeField.value)
        RadioGroupDialog(this, items, currentWebsiteTypeId) {
            if (it as Int == Website.TYPE_CUSTOM) {
                CustomLabelDialog(this) {
                    websiteTypeField.text = it
                }
            } else {
                websiteTypeField.text = getWebsiteTypeText(it, "")
            }
        }
    } // EditContactActivity.showWebsiteTypePicker()

    // *****************************************************************

    private fun adjustWebsiteFieldWidgets(websiteHolder: ViewGroup, showRemove: Boolean, showShare: Boolean) {
        websiteHolder.contact_website_remove.beVisibleIf(showRemove)
        websiteHolder.contact_website_share_info.beVisibleIf(showShare)
        websiteHolder.contact_website_share_icon.beVisibleIf(showShare)
    } // EditContactActivity.adjustWebsiteFieldWidgets()

    // *****************************************************************
    // *****************************************************************

    private fun setupRelations(showRemove: Boolean, showShare: Boolean) {
        val relations: ArrayList<ContactRelation> = contact!!.relations
        val relationListHolder: LinearLayout = contact_relations_holder!!
        if (relations.isNotEmpty()) {
            relations.forEachIndexed { index, relation ->
                var relationHolder = addRelationField(relationListHolder, index, showRemove, showShare)
                relationHolder.contact_relation.setText(relation.name)
                setupRelationTypePicker(relationHolder.contact_relation_type, relation.type, relation.label)
                // adjustRelationFieldWidgets(relationHolder, showRemove, showShare)
                relationHolder.contact_relation_share_info.isChecked = state.shareRelation[index]
            }
        } else /* (relations.isEmpty()) */ {
            addRelationField(relationListHolder, 0, showRemove, showShare)
        }
    } // EditContactActivity.setupRelations()

    // *****************************************************************

    private fun addRelationField(relationListHolder: ViewGroup, index: Int,
                                 showRemove: Boolean, showShare: Boolean) : ViewGroup {
        var relationHolder: ViewGroup? = relationListHolder.getChildAt(index) as ViewGroup?
        if (relationHolder == null) {
            relationHolder = layoutInflater.inflate(R.layout.item_edit_relation, relationListHolder, false) as ViewGroup
            updateTextColors(relationHolder)
            relationListHolder.addView(relationHolder)
        }
        relationHolder.contact_relation_remove.applyColorFilter(getProperPrimaryColor())
        relationHolder.contact_relation_remove.background.applyColorFilter(getProperTextColor())
        relationHolder.contact_relation_remove.setOnClickListener {
            removeRelationField(relationHolder)
        }

        setupRelationTypePicker(relationHolder.contact_relation_type, DEFAULT_RELATION_TYPE, "")

        adjustRelationFieldWidgets(relationHolder, showRemove, showShare)

        return(relationHolder)
    } // EditContactActivity.addRelationField()

    // *****************************************************************

    private fun addNewRelationField() {
        val relationListHolder: LinearLayout = contact_relations_holder!!
        val relationHolder: ViewGroup = addRelationField(relationListHolder,
            9999, config.showRemoveButtons, false)

        relationListHolder.onGlobalLayout {
            relationHolder.contact_relation.requestFocus()
            showKeyboard(relationHolder.contact_relation)
        }
    } // EditContactActivity.addNewRelationField()

    // *****************************************************************

    private fun removeRelationField(relationHolder: View) {
        val relationListHolder: ViewParent? = relationHolder.parent
        (relationHolder as ViewGroup).removeAllViews()
        (relationListHolder as ViewGroup).removeView(relationHolder as View)
        if (relationListHolder.childCount == 0)
            addRelationField(relationListHolder, 9999, true, false)
    } // EditContactActivity.removeRelationField()

    // *****************************************************************

    private fun setupRelationTypePicker(relationTypeField: TextView, type: Int, label: String) {
        relationTypeField.text = getRelationTypeText(type, label)
        relationTypeField.setOnClickListener {
            showRelationTypePicker(relationTypeField)
        }
    } // EditContactActivity.setupRelationTypePicker()

    // *****************************************************************

    private fun showRelationTypePicker(relationTypeField: TextView) {
        val items = arrayListOf (
            RadioItem(Relation.TYPE_CUSTOM, getString(R.string.custom)), // 0

            RadioItem(Relation.TYPE_FRIEND, getString(R.string.relation_friend)), // 6

            RadioItem(Relation.TYPE_SPOUSE, getString(R.string.relation_spouse)), // 14
            RadioItem(ContactRelation.TYPE_HUSBAND, getString(R.string.relation_husband)), // 103
            RadioItem(ContactRelation.TYPE_WIFE, getString(R.string.relation_wife)), // 104
            RadioItem(Relation.TYPE_DOMESTIC_PARTNER, getString(R.string.relation_domestic_partner)), // 4
            RadioItem(Relation.TYPE_PARTNER, getString(R.string.relation_partner)), // 10
            RadioItem(ContactRelation.TYPE_CO_RESIDENT, getString(R.string.relation_co_resident)), // 56
            RadioItem(ContactRelation.TYPE_NEIGHBOR, getString(R.string.relation_neighbor)), // 57
            RadioItem(Relation.TYPE_PARENT, getString(R.string.relation_parent)), // 9
            RadioItem(Relation.TYPE_FATHER, getString(R.string.relation_father)), // 5
            RadioItem(Relation.TYPE_MOTHER, getString(R.string.relation_mother)), // 8
            RadioItem(Relation.TYPE_CHILD, getString(R.string.relation_child)), // 3
            RadioItem(ContactRelation.TYPE_SON, getString(R.string.relation_son)), // 105
            RadioItem(ContactRelation.TYPE_DAUGHTER, getString(R.string.relation_daughter)), // 106
            RadioItem(ContactRelation.TYPE_SIBLING, getString(R.string.relation_sibling)), // 58
            RadioItem(Relation.TYPE_BROTHER, getString(R.string.relation_brother)), // 2
            RadioItem(Relation.TYPE_SISTER, getString(R.string.relation_sister)), // 13
            RadioItem(ContactRelation.TYPE_GRANDPARENT, getString(R.string.relation_grandparent)), // 107
            RadioItem(ContactRelation.TYPE_GRANDFATHER, getString(R.string.relation_grandfather)), // 108
            RadioItem(ContactRelation.TYPE_GRANDMOTHER, getString(R.string.relation_grandmother)), // 109
            RadioItem(ContactRelation.TYPE_GRANDCHILD, getString(R.string.relation_grandchild)), // 110
            RadioItem(ContactRelation.TYPE_GRANDSON, getString(R.string.relation_grandson)), // 111
            RadioItem(ContactRelation.TYPE_GRANDDAUGHTER, getString(R.string.relation_granddaughter)), // 112
            RadioItem(ContactRelation.TYPE_UNCLE, getString(R.string.relation_uncle)), // 113
            RadioItem(ContactRelation.TYPE_AUNT, getString(R.string.relation_aunt)), // 114
            RadioItem(ContactRelation.TYPE_NEPHEW, getString(R.string.relation_nephew)), // 115
            RadioItem(ContactRelation.TYPE_NIECE, getString(R.string.relation_niece)), // 116
            RadioItem(ContactRelation.TYPE_FATHER_IN_LAW, getString(R.string.relation_father_in_law)), // 117
            RadioItem(ContactRelation.TYPE_MOTHER_IN_LAW, getString(R.string.relation_mother_in_law)), // 118
            RadioItem(ContactRelation.TYPE_SON_IN_LAW, getString(R.string.relation_son_in_law)), // 119
            RadioItem(ContactRelation.TYPE_DAUGHTER_IN_LAW, getString(R.string.relation_daughter_in_law)), // 120
            RadioItem(ContactRelation.TYPE_BROTHER_IN_LAW, getString(R.string.relation_brother_in_law)), // 121
            RadioItem(ContactRelation.TYPE_SISTER_IN_LAW, getString(R.string.relation_sister_in_law)), // 122
            RadioItem(Relation.TYPE_RELATIVE, getString(R.string.relation_relative)), // 12
            RadioItem(ContactRelation.TYPE_KIN, getString(R.string.relation_kin)), // 59

            RadioItem(ContactRelation.TYPE_MUSE, getString(R.string.relation_muse)), // 60
            RadioItem(ContactRelation.TYPE_CRUSH, getString(R.string.relation_crush)), // 61
            RadioItem(ContactRelation.TYPE_DATE, getString(R.string.relation_date)), // 62
            RadioItem(ContactRelation.TYPE_SWEETHEART, getString(R.string.relation_sweetheart)), // 63

            RadioItem(ContactRelation.TYPE_CONTACT, getString(R.string.relation_contact)), // 51
            RadioItem(ContactRelation.TYPE_ACQUAINTANCE, getString(R.string.relation_acquaintance)), // 52
            RadioItem(ContactRelation.TYPE_MET, getString(R.string.relation_met)), // 53
            RadioItem(Relation.TYPE_REFERRED_BY, getString(R.string.relation_referred_by)), // 11
            RadioItem(ContactRelation.TYPE_AGENT, getString(R.string.relation_agent)), // 64

            RadioItem(ContactRelation.TYPE_COLLEAGUE, getString(R.string.relation_colleague)), // 55
            RadioItem(ContactRelation.TYPE_CO_WORKER, getString(R.string.relation_co_worker)), // 54
            RadioItem(ContactRelation.TYPE_SUPERIOR, getString(R.string.relation_superior)), // 101
            RadioItem(ContactRelation.TYPE_SUBORDINATE, getString(R.string.relation_subordinate)), // 102
            RadioItem(Relation.TYPE_MANAGER, getString(R.string.relation_manager)), // 7
            RadioItem(Relation.TYPE_ASSISTANT, getString(R.string.relation_assistant)), // 1

            RadioItem(ContactRelation.TYPE_ME, getString(R.string.relation_me)), // 66
            RadioItem(ContactRelation.TYPE_EMERGENCY, getString(R.string.relation_emergency)), // 65
        )

        val currentRelationTypeId = getRelationTypeFromText(relationTypeField.value)
        RadioGroupDialog(this, items, currentRelationTypeId) {
            if (it as Int == Relation.TYPE_CUSTOM) {
                CustomLabelDialog(this) {
                    relationTypeField.text = it
                }
            } else {
                relationTypeField.text = getRelationTypeText(it, "")
            }
        }
    } // EditContactActivity.showRelationTypePicker()

    // *****************************************************************

    private fun adjustRelationFieldWidgets(relationHolder: ViewGroup, showRemove: Boolean, showShare: Boolean) {
        relationHolder.contact_relation_remove.beVisibleIf(showRemove)
        relationHolder.contact_relation_share_info.beVisibleIf(showShare)
        relationHolder.contact_relation_share_icon.beVisibleIf(showShare)
    } // EditContactActivity.adjustRelationFieldWidgets()

    // *****************************************************************
    // *****************************************************************
// FIXME FIXME FIXME
    private fun setupGroups(showRemove: Boolean, showShareCheckbox: Boolean) {
        contact_groups_holder.removeAllViews()
        val groups = contact!!.groups
        groups.forEachIndexed { index, group ->
            var groupHolder = contact_groups_holder.getChildAt(index)
            if (groupHolder == null) {
                groupHolder = layoutInflater.inflate(R.layout.item_edit_group, contact_groups_holder, false)
                contact_groups_holder.addView(groupHolder)
            }

            groupHolder.contact_group.apply {
                text = group.title
                setTextColor(getProperTextColor())
                tag = group.id
                alpha = 1f
            }

            groupHolder.setOnClickListener {
                showSelectGroupsDialog()
            }

            groupHolder.contact_group_remove.apply {
                beVisible()
                applyColorFilter(getProperPrimaryColor())
                background.applyColorFilter(getProperTextColor())
                setOnClickListener {
                    removeGroup(group.id!!)
                }
            }

            groupHolder.contact_group_share_info.beVisibleIf(showShareCheckbox)
            groupHolder.contact_group_share_icon.beVisibleIf(showShareCheckbox)
            groupHolder.contact_group_share_info.isChecked = state.shareGroup[index]
        }

        if (groups.isEmpty()) {
            var groupHolder = contact_groups_holder.getChildAt(0)
            if (groupHolder == null) {
                groupHolder = layoutInflater.inflate(R.layout.item_edit_group, contact_groups_holder, false)
                contact_groups_holder.addView(groupHolder)
            }

            groupHolder.contact_group.apply {
                alpha = 0.5f
                text = getString(R.string.no_groups)
                setTextColor(getProperTextColor())
            }

            groupHolder.setOnClickListener {
                showSelectGroupsDialog()
            }

            groupHolder.contact_group_remove.beGone()
            groupHolder.contact_group_share_icon.beGone()
            groupHolder.contact_group_share_info.beGone()
        }
    } // EditContactActivity.setupGroups()

    // *****************************************************************

    private fun setupGroupsPicker(groupTitleField: TextView, group: Group? = null) {
        groupTitleField.apply {
            text = group?.title ?: getString(R.string.no_groups)
            alpha = if (group == null) 0.5f else 1f
                contact_group_share_icon.beGone()
                contact_group_share_info.beGone()
            setOnClickListener {
                showSelectGroupsDialog()
            }
                contact_groups_holder.addView(this)
        }
    } // EditContactActivity.setupGroupsPicker()

    // *****************************************************************

    private fun showSelectGroupsDialog() {
        SelectGroupsDialog(this@EditContactActivity, contact!!.groups) {
            val oldGroups: ArrayList<Group> = contact!!.groups
            val oldItemCnt: Int = oldGroups.size
            contact_groups_holder.children.forEachIndexed { index, groupHolder ->
                state.shareGroup[index] = groupHolder.contact_group_share_info.isChecked
            }

            var newShareGroup = ArrayList<Boolean>()
            val newItemCnt: Int = it.size
            for (i in 0..newItemCnt-1) {
                newShareGroup.add(true)
                for (j in 0..oldItemCnt - 1) {
                    if (oldGroups[j].id == it[i].id) {
                        newShareGroup[i] = state.shareGroup[j]
                        break
                    }
                }
            }
            contact!!.groups = it
            state.shareGroup = newShareGroup

            setupGroups(true, state.selectShareFieldsActive)
        }
    } // EditContactActivity.showSelectGroupsDialog()

    // *****************************************************************

    private fun removeGroup(id: Long) {
        contact!!.groups = contact!!.groups.filter { it.id != id } as ArrayList<Group>
        setupGroups(true, state.selectShareFieldsActive)
    } // EditContactActivity.removeGroup()

    // *****************************************************************
    // *****************************************************************

    private fun setupRingtone() {
        contact_ringtone.setOnClickListener {
            hideKeyboard()
            val ringtonePickerIntent = getRingtonePickerIntent()
            try {
                startActivityForResult(ringtonePickerIntent, INTENT_SELECT_RINGTONE)
            } catch (e: Exception) {
                val currentRingtone = contact!!.ringtone ?: getDefaultAlarmSound(RingtoneManager.TYPE_RINGTONE).uri
                SelectAlarmSoundDialog(this, currentRingtone, AudioManager.STREAM_RING, PICK_RINGTONE_INTENT_ID, RingtoneManager.TYPE_RINGTONE, true,
                    onAlarmPicked = {
                        contact!!.ringtone = it?.uri
                        val ringtoneFilename = it?.title
                        if (!isSoundOfSilence(ringtoneFilename))
                            contact_ringtone.text = ringtoneFilename
                        else
                            contact_ringtone.text = getString(R.string.no_sound)
                    }, onAlarmSoundDeleted = {}
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
    } // EditContactActivity.setupRingtone()

    // *****************************************************************
    // *****************************************************************

    private fun setupContactSource() {
        originalContactSource = contact!!.source
        getPublicContactSource(contact!!.source) {
            contact_source.text = if (it == "") getString(R.string.phone_storage) else it
        }
    } // EditContactActivity.setupContactSource()

    // *****************************************************************

    private fun showSelectContactSourceDialog() {
        showContactSourcePicker(contact!!.source) {
            contact!!.source = if (it == getString(R.string.phone_storage_hidden)) SMT_PRIVATE else it
            getPublicContactSource(it) {
                contact_source.text = if (it == "") getString(R.string.phone_storage) else it
            }
        }
    } // EditContactActivity.showSelectContactSourceDialog()

    // *****************************************************************
    // *****************************************************************

    private fun setupNewContact() {
        originalContactSource = if (hasContactPermissions()) config.lastUsedContactSource else SMT_PRIVATE
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
    } // EditContactActivity.setupNewContact()

    // *****************************************************************
    // *****************************************************************

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
            setupRefreshDisplayAddress(addressHolder as ViewGroup)
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
    } // EditContactActivity.setupTypePickers()

    // *****************************************************************

    private fun resetContactEvent(contactEvent: TextView, removeContactEventButton: ImageView) {
        contactEvent.apply {
            text = "" // getString(R.string.unknown)
            tag = ""
            // alpha = 0.5f
        }
        removeContactEventButton.beGone()
    } // EditContactActivity.resetContactEvent()

    // *****************************************************************

    private fun saveContact() {
        if (isSaving || (contact == null)) {
            return
        }

        val contactValues: Contact = fillContactValues(false, true, false)
        // convertKnownIMTypeIDsToCustomTypes(contactValues.IMs)
        // convertKnownRelationCustomTypesToIDs(contactValues.relations)

        if (// currentContactPhotoPath.isEmpty() &&
        contactValues.name.formattedName.isEmpty() &&
        contactValues.name.prefix.isEmpty() &&
        contactValues.name.givenName.isEmpty() &&
        contactValues.name.middleName.isEmpty() &&
        contactValues.name.familyName.isEmpty() &&
        contactValues.name.suffix.isEmpty() &&
        // contactValues.nicknames.isEmpty() &&
        // contactValues.phoneNumbers.isEmpty() &&
        contactValues.emails.isEmpty() &&
        // contactValues.addresses.isEmpty() &&
        // contactValues.IMs.isEmpty() &&
        // contactValues.events.isEmpty() &&
        // contactValues.notes.isEmpty() &&
        contactValues.organization.company.isEmpty() &&
        // contactValues.organization.jobTitle.isEmpty() &&
        // contactValues.websites.isEmpty() &&
        // contactValues.relations.isEmpty() &&
        true ) {
            toast(R.string.fields_empty)
            return
        }

        val oldPhotoUri = contact!!.photoUri
        val oldPrimary = contact!!.phoneNumbers.find { it.isPrimary }
        val newPrimary = contactValues.phoneNumbers.find { it.isPrimary }
        val primaryState = Pair(oldPrimary, newPrimary)

        contact = contactValues

        ensureBackgroundThread {
            config.lastUsedContactSource = contact!!.source
            when {
                contact!!.id == 0 -> insertNewContact(false)
                originalContactSource != contact!!.source -> insertNewContact(true)
                else -> {
                    val photoUpdateStatus = getPhotoUpdateStatus(oldPhotoUri, contact!!.photoUri)
                    updateContact(photoUpdateStatus, primaryState)
                }
            }
        }
    } // EditContactActivity.saveContact()

    // *****************************************************************

    private fun fillContactValues(getSelectedFieldsOnly: Boolean, getHiddenFields: Boolean, getEmptyLines: Boolean): Contact {
        val filledName = getFilledName(getHiddenFields)
        val filledNicknames = getFilledNicknames(getSelectedFieldsOnly, getHiddenFields, getEmptyLines)
        val filledPhoneNumbers = getFilledPhoneNumbers(getSelectedFieldsOnly, getHiddenFields, getEmptyLines)
        val filledEmails = getFilledEmails(getSelectedFieldsOnly, getHiddenFields, getEmptyLines)
        val filledAddresses = getFilledAddresses(getSelectedFieldsOnly, getHiddenFields, getEmptyLines)
        val filledIMs = getFilledIMs(getSelectedFieldsOnly, getHiddenFields, getEmptyLines)
        val filledEvents = getFilledEvents(getSelectedFieldsOnly, getHiddenFields, getEmptyLines)
        val filledNotes = getFilledNotes(getSelectedFieldsOnly, getHiddenFields, getEmptyLines)
        val filledOrganization = getFilledOrganization(getSelectedFieldsOnly, getHiddenFields, getEmptyLines)
        val filledWebsites = getFilledWebsites(getSelectedFieldsOnly, getHiddenFields, getEmptyLines)
        val filledRelations = getFilledRelations(getSelectedFieldsOnly, getHiddenFields, getEmptyLines)
        val selectedGroups = getSelectedGroups(getSelectedFieldsOnly, getHiddenFields, getEmptyLines)

        val newContact = contact!!.copy(
            name = filledName,
            nicknames = filledNicknames,
            phoneNumbers = filledPhoneNumbers,
            emails = filledEmails,
            addresses = filledAddresses,
            IMs = filledIMs,
            events = filledEvents,
            notes = filledNotes,
            organization = filledOrganization,
            websites = filledWebsites,
            relations = filledRelations,
            groups = selectedGroups,
            photoUri =  if (!getSelectedFieldsOnly || contact_photo_share_info.isChecked) currentContactPhotoPath else "",
            starred = if (isContactStarred()) 1 else 0
        )

        return newContact
    } // EditContactActivity.fillContactValues()

    // *****************************************************************

    fun getFilledName(getHiddenFields: Boolean): ContactName {
        // While we honor the selection of invisible fields for prefix/middle name/suffix, we
        // shall always copy the formatted, given and family names.
        // FIXME - Is this really clever? - Should we really discard part of the name
        // information, just because we are not showing it??
        // Should we automatically show a field if it contains information?
        val filledDisplayName = contact_display_name.value.trim()
        val filledPrefix = getVisibleNamePart(contact_prefix, getHiddenFields)
        // val filledGivenName = getVisibleNamePart(contact_first_name, getHiddenFields)
        val filledGivenName = contact_first_name.value.trim()
        val filledMiddleName = getVisibleNamePart(contact_middle_name, getHiddenFields)
        // val filledFamilyName = getVisibleNamePart(contact_surname, getHiddenFields)
        val filledFamilyName = contact_surname.value.trim()
        val filledSuffix = getVisibleNamePart(contact_suffix, getHiddenFields)
        val filledPhoneticGivenName = getVisibleNamePart(contact_phonetic_given_name, getHiddenFields)
        val filledPhoneticMiddleName = getVisibleNamePart(contact_phonetic_middle_name, getHiddenFields)
        val filledPhoneticFamilyName = getVisibleNamePart(contact_phonetic_surname, getHiddenFields)
        return (ContactName(filledDisplayName, filledPrefix,
            filledGivenName, filledMiddleName, filledFamilyName, filledSuffix,
            filledPhoneticGivenName, filledPhoneticMiddleName, filledPhoneticFamilyName))
    } // EditContactActivity.getFilledName()

    // *****************************************************************

    fun getVisibleNamePart(editText: EditText, getHiddenFields: Boolean): String {
        return if (getHiddenFields || editText.isVisible())
            (editText.value.trim())
        else
            ("")
    } // EditContactActivity.getVisibleNamePart()

    // *****************************************************************

    private fun getFilledNicknames(getSelectedFieldsOnly: Boolean, getHiddenFields: Boolean, getEmptyLines: Boolean): ArrayList<ContactNickname> {
        val nicknames = ArrayList<ContactNickname>()
        if (getHiddenFields || contact_nicknames_holder.isVisible()) {
            val nicknameCount = contact_nicknames_holder.childCount
            for (i in 0 until nicknameCount) {
                val nicknameHolder = contact_nicknames_holder.getChildAt(i)
                if (!getSelectedFieldsOnly || nicknameHolder.contact_nickname_share_info.isChecked) {
                    val nickname = nicknameHolder.contact_nickname.value.trim()
                    val nickType = getNicknameTypeId(nicknameHolder.contact_nickname_type.value)
                    val nickLabel = if (nickType == Nickname.TYPE_CUSTOM) nicknameHolder.contact_nickname_type.value else ""

                    if (nickname.isNotEmpty() || getEmptyLines) {
                        nicknames.add(ContactNickname(nickname, nickType, nickLabel))
                    }
                } /* if (isChecked) */
            }
        }
        return nicknames
    } // EditContactActivity::getFilledNicknames()

    // *****************************************************************

    private fun getFilledPhoneNumbers(getSelectedFieldsOnly: Boolean, getHiddenFields: Boolean, getEmptyLines: Boolean): ArrayList<PhoneNumber> {
        val phoneNumbers = ArrayList<PhoneNumber>()
        if (getHiddenFields || contact_numbers_holder.isVisible()) {
            val numbersCount = contact_numbers_holder.childCount
            for (i in 0 until numbersCount) {
                val numberHolder = contact_numbers_holder.getChildAt(i)
                if (!getSelectedFieldsOnly || numberHolder.contact_number_share_info.isChecked) {
                    val number = numberHolder.contact_number.value.trim()
                    val numberType = getPhoneNumberTypeId(numberHolder.contact_number_type.value)
                    val numberLabel = if (numberType == Phone.TYPE_CUSTOM) numberHolder.contact_number_type.value else ""

                    if (number.isNotEmpty() || getEmptyLines) {
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
                } /* if (isChecked) */
            }
        }
        return phoneNumbers
    } // EditContactActivity.getFilledPhoneNumbers()

    // *****************************************************************

    private fun getFilledEmails(getSelectedFieldsOnly: Boolean, getHiddenFields: Boolean, getEmptyLines: Boolean): ArrayList<Email> {
        val emails = ArrayList<Email>()
        if (getHiddenFields || contact_emails_holder.isVisible()) {
            val emailsCount = contact_emails_holder.childCount
            for (i in 0 until emailsCount) {
                val emailHolder = contact_emails_holder.getChildAt(i)
                if (!getSelectedFieldsOnly || emailHolder.contact_email_share_info.isChecked) {
                    val email = emailHolder.contact_email.value.trim()
                    val emailType = getEmailTypeId(emailHolder.contact_email_type.value)
                    val emailLabel = if (emailType == CommonDataKinds.Email.TYPE_CUSTOM) emailHolder.contact_email_type.value else ""

                    if (email.isNotEmpty() || getEmptyLines) {
                        emails.add(Email(email, emailType, emailLabel))
                    }
                } /* if (isChecked) */
            }
        }
        return emails
    } // EditContactActivity.getFilledEmails()

    // *****************************************************************

    private fun getFilledAddresses(getSelectedFieldsOnly: Boolean, getHiddenFields: Boolean, getEmptyLines: Boolean): ArrayList<Address> {
        val addresses = ArrayList<Address>()
        if (getHiddenFields || contact_addresses_holder.isVisible()) {
            val addressesCount = contact_addresses_holder.childCount
            for (i in 0 until addressesCount) {
                val addressHolder = contact_addresses_holder.getChildAt(i)
                if (!getSelectedFieldsOnly || addressHolder.contact_address_share_info.isChecked) {
                    val formattedAddress = addressHolder.contact_address.value.trim()
                    val street = addressHolder.contact_address_street.value.trim()
                    val postOfficeBox = addressHolder.contact_address_postOfficeBox.value.trim()
                    val neighborhood = addressHolder.contact_address_neighborhood.value.trim()
                    val city = addressHolder.contact_address_city.value.trim()
                    val region = addressHolder.contact_address_region.value.trim()
                    val postalCode = addressHolder.contact_address_postalCode.value.trim()
                    val country = addressHolder.contact_address_country.value.trim()
                    if ((formattedAddress != "") || (street != "") || (postOfficeBox != "") || (neighborhood != "") ||
                        (city != "") || (region != "") || (postalCode != "") || (country != "") || getEmptyLines
                    ) {
                        val addressType = getAddressTypeId(addressHolder.contact_address_type.value)
                        val addressLabel = if (addressType == StructuredPostal.TYPE_CUSTOM) addressHolder.contact_address_type.value else ""
                        addresses.add(
                            Address(
                                formattedAddress,
                                street,
                                postOfficeBox,
                                neighborhood,
                                city,
                                region,
                                postalCode,
                                country,
                                addressType,
                                addressLabel,
                            )
                        )
                    }
                } /* if (isChecked) */
            }
        }
        return addresses
    } // EditContactActivity.getFilledAddresses()

    // *****************************************************************

    private fun getFilledIMs(getSelectedFieldsOnly: Boolean, getHiddenFields: Boolean, getEmptyLines: Boolean): ArrayList<IM> {
        val IMs = ArrayList<IM>()
        if (getHiddenFields || contact_ims_holder.isVisible()) {
            val IMsCount = contact_ims_holder.childCount
            for (i in 0 until IMsCount) {
                val IMsHolder = contact_ims_holder.getChildAt(i)
                if (!getSelectedFieldsOnly || IMsHolder.contact_im_share_info.isChecked) {
                    val IM = IMsHolder.contact_im.value.trim()
                    // val IMType = IMsHolder.contact_im_type.value.trim()
                    // val IMTypelID = getIMTypeId(IMType)
                    // val IMCustomType = if (IMTypeID == Im.TYPE_CUSTOM) IMType else ""
                    val IMProtocol = IMsHolder.contact_im_protocol.value.trim()
                    val IMProtocolID = getIMProtocolId(IMProtocol)
                    val IMCustomProtocol = if (IMProtocolID == Im.PROTOCOL_CUSTOM) IMProtocol else ""

                    if (IM.isNotEmpty() || getEmptyLines) {
                        IMs.add(
                            IM(
                                IM, DEFAULT_IM_TYPE, "",
                                IMProtocolID, IMCustomProtocol
                            )
                        )
                    }
                } /* if (isChecked) */
            }
        }
        return IMs
    } // EditContactActivity.getFilledIMs()

    // *****************************************************************

    private fun getFilledEvents(getSelectedFieldsOnly: Boolean, getHiddenFields: Boolean, getEmptyLines: Boolean): ArrayList<Event> {
        val events = ArrayList<Event>()
        if (getHiddenFields || contact_events_holder.isVisible()) {
            val unknown = getString(R.string.unknown)
            val eventsCount = contact_events_holder.childCount
            for (i in 0 until eventsCount) {
                val eventHolder = contact_events_holder.getChildAt(i)
                if (!getSelectedFieldsOnly || eventHolder.contact_event_share_info.isChecked) {
                    val event = eventHolder.contact_event.value.trim()
                    val eventTag = eventHolder.contact_event.tag?.toString() ?: ""
                    val eventType = eventHolder.contact_event_type.value.trim()
                    val eventTypeID = getEventTypeId(eventType)
                    val eventLabel = if (eventTypeID == CommonDataKinds.Event.TYPE_CUSTOM) eventType else ""

                    if ((event.isNotEmpty() || getEmptyLines) && (event != unknown)) {
                        events.add(Event(eventTag, eventTypeID, eventLabel))
                    }
                } /* if (isChecked) */
            }
        }
        return events
    } // EditContactActivity.getFilledEvents()

    // *****************************************************************

    private fun getFilledNotes(getSelectedFieldsOnly: Boolean, getHiddenFields: Boolean, getEmptyLines: Boolean): String {
        val notesHolder = contact_notes_holder
        if (!getSelectedFieldsOnly || notesHolder.contact_note_share_info.isChecked)
            return(notesHolder.contact_note.value)
        else
            return("")
    } // EditContactActivity.getFilledNotes()

    // *****************************************************************

    private fun getFilledOrganization(getSelectedFieldsOnly: Boolean, getHiddenFields: Boolean, getEmptyLines: Boolean): Organization {
        val organizationHolder = contact_organization_holder
        var organization: Organization = contact!!.organization.deepCopy()
        if ((getHiddenFields || organizationHolder.contact_organization_company.isVisible()) &&
            (!getSelectedFieldsOnly || organizationHolder.contact_organization_share_info.isChecked)) {
            organization.company = organizationHolder.contact_organization_company.value
            organization.jobTitle = organizationHolder.contact_organization_job_title.value
        }
        return(organization)
    } // EditContactActivity.getFilledOrganization()

    // *****************************************************************

    private fun getFilledWebsites(getSelectedFieldsOnly: Boolean, getHiddenFields: Boolean, getEmptyLines: Boolean): ArrayList<ContactWebsite> {
        val websites = ArrayList<ContactWebsite>()
        if (getHiddenFields || contact_websites_holder.isVisible()) {
            val websitesCount = contact_websites_holder.childCount
            for (i in 0 until websitesCount) {
                val websiteHolder = contact_websites_holder.getChildAt(i)
                if (!getSelectedFieldsOnly || websiteHolder.contact_website_share_info.isChecked) {
                    val website = websiteHolder.contact_website.value.trim()
                    val websiteType = websiteHolder.contact_website_type.value.trim()
                    val websiteTypeID = getWebsiteTypeId(websiteType)
                    val websiteLabel = if (websiteTypeID == Website.TYPE_CUSTOM) websiteType else ""

                    if (website.isNotEmpty() || getEmptyLines) {
                        websites.add(ContactWebsite(website, websiteTypeID, websiteLabel))
                    }
                } /* if (isChecked) */
            }
        }
        return websites
    } // EditContactActivity.getFilledWebsites()

    // *****************************************************************

    private fun getFilledRelations(getSelectedFieldsOnly: Boolean, getHiddenFields: Boolean, getEmptyLines: Boolean): ArrayList<ContactRelation> {
        val relations = ArrayList<ContactRelation>()
        if (getHiddenFields || contact_relations_holder.isVisible()) {
            val relationsCount = contact_relations_holder.childCount
            for (i in 0 until relationsCount) {
                val relationHolder = contact_relations_holder.getChildAt(i)
                if (!getSelectedFieldsOnly || relationHolder.contact_relation_share_info.isChecked) {
                    val relation = relationHolder.contact_relation.value.trim()
                    val relationType = relationHolder.contact_relation_type.value.trim()
                    val relationTypeID = getRelationTypeFromText(relationType)
                    val relationLabel = if (relationTypeID == ContactRelation.TYPE_CUSTOM) relationType else ""

                    if (relation.isNotEmpty() || getEmptyLines) {
                        relations.add(ContactRelation(relation, relationTypeID, relationLabel))
                    }
                } /* if (isChecked) */
            }
        }
        return relations
    } // EditContactActivity.getFilledRelations()

    // *****************************************************************

    private fun getSelectedGroups(getSelectedFieldsOnly: Boolean, getHiddenFields: Boolean, getEmptyLines: Boolean): ArrayList<Group> {
        val selectedGroups = ArrayList<Group>()
        val NoGroupsMsg = getString(R.string.no_groups)
        if (getHiddenFields || contact_groups_holder.isVisible()) {
            val groupCount = contact_groups_holder.childCount
            for (i in 0 until groupCount) {
                val groupHolder = contact_groups_holder.getChildAt(i)
                if ((groupHolder.contact_group.text != NoGroupsMsg) &&
                    (!getSelectedFieldsOnly || groupHolder.contact_group_share_info.isChecked)) {
                    selectedGroups.add(contact!!.groups[i])
                } /* if (isChecked) */
            }
        }
        return selectedGroups
    } // EditContactActivity.getSelectedGroups()

    // *****************************************************************

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
            isSaving = false
        }
    } // EditContactActivity.insertNewContact()

    // *****************************************************************

    private fun updateContact(photoUpdateStatus: Int, primaryState: Pair<PhoneNumber?, PhoneNumber?>) {
        isSaving = true
        if (ContactsHelper(this@EditContactActivity).updateContact(contact!!, origContact, photoUpdateStatus)) {
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
            isSaving = false
        }
    } // EditContactActivity.updateContact()

    // *****************************************************************

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

                        contactsHelper.updateContact(duplicate, null, PHOTO_UNCHANGED)
                    }
                }

                runOnUiThread {
                    callback.invoke()
                }
            }
        }
    } // EditContactActivity.updateDefaultNumberForDuplicateContacts()

    // *****************************************************************

    private fun setAutoFormattedAddress(addressHolder: View?) {
        if (addressHolder == null)
            return
        val addressIndex: Int = (addressHolder.parent as ViewGroup).indexOfChild(addressHolder)

        if (textChangedHandlersActive &&
            (state.autoCalcFormattedAddress[addressIndex] || ((config.showContactFields and SHOW_ADDRESSES_FIELD) == 0))) {

            // staticEditContact.changeCnt++
            // addressHolder.contact_address.setText(staticEditContact.changeCnt.toString())
            val address: Address = Address("",
                addressHolder.contact_address_street.value.trim(),
                addressHolder.contact_address_postOfficeBox.value.trim(),
                addressHolder.contact_address_neighborhood.value.trim(),
                addressHolder.contact_address_city.value.trim(),
                addressHolder.contact_address_region.value.trim(),
                addressHolder.contact_address_postalCode.value.trim(),
                addressHolder.contact_address_country.value.trim(),
                StructuredPostal.TYPE_HOME, ""
            )
            val formattedAddress = address.getFormattedPostalAddress(configAutoFormattedAddressFormat)
            addressHolder.contact_address.setText(formattedAddress)
            if ((config.showContactFields and SHOW_ADDRESSES_FIELD) != 0)
                addressHolder.contact_address_topline.setText(formattedAddress)
        }
    } // EditContactActivity.setAutoFormattedAddress()

    // *****************************************************************

    private fun onAddressToplineChanged(addressHolder: View) {
        val topVisibleField: EditText? = getTopVisibleAddressField(addressHolder)
        if (topVisibleField == null)
            return
        val newValue: String = addressHolder.contact_address_topline.value
        if (topVisibleField.value != newValue)
            topVisibleField.setText(newValue)
    } // EditContactActivity.onAddressToplineChanged()

    // *****************************************************************

    private fun onFormattedAddressFocusChanged(v: View, hasFocus: Boolean) { // View.OnFocusChangeListener?
        val addressHolder: ViewGroup = v.parent as ViewGroup
        val addressIndex: Int = (addressHolder.parent as ViewGroup).indexOfChild(addressHolder)

        val address: Address = Address("",
            addressHolder.contact_address_street.value.trim(),
            addressHolder.contact_address_postOfficeBox.value.trim(),
            addressHolder.contact_address_neighborhood.value.trim(),
            addressHolder.contact_address_city.value.trim(),
            addressHolder.contact_address_region.value.trim(),
            addressHolder.contact_address_postalCode.value.trim(),
            addressHolder.contact_address_country.value.trim(),
            StructuredPostal.TYPE_HOME, ""
        )
        val editFormattedAddress = addressHolder.contact_address.value.trim()
        val formattedAddress = address.getFormattedPostalAddress(configAutoFormattedAddressFormat)
        state.autoCalcFormattedAddress[addressIndex] = (formattedAddress == editFormattedAddress)
        if (editFormattedAddress == "") {
            state.autoCalcFormattedAddress[addressIndex] = true
            addressHolder.contact_address.setText(formattedAddress)  // FIXME - Do we really want this?
        }
    } // EditContactActivity.onFormattedAddressFocusChanged()

    // *****************************************************************

    private fun setAutoDisplayName() {
        // staticEditContact.changeCnt++
        // contact_display_name.setText(staticEditContact.changeCnt.toString())
        if (textChangedHandlersActive &&
            (state.autoCalcDisplayName || ((config.showContactFields and SHOW_ADDRESSES_FIELD) == 0)))
            contact_display_name.setText(getAutoDisplayName())
    } // EditContactActivity.setAutoDisplayName()

    // *****************************************************************

    private fun getAutoDisplayName() : String {
        val contactName = ContactName(contact_display_name.value.trim(), contact_prefix.value.trim(), contact_first_name.value.trim(),
            contact_middle_name.value.trim(), contact_surname.value.trim(), contact_suffix.value.trim(),
            "", "", "")
        return(contactName.buildDisplayName(configAutoFormattedNameFormat))
    } // EditContactActivity.getAutoDisplayName()

    // *****************************************************************

    private fun onDisplayNameFocusChanged(v: View, hasFocus: Boolean) { // View.OnFocusChangeListener?
        if (!hasFocus) {
            state.autoCalcDisplayName = contact_display_name.value.trim().isEmpty()
            if (state.autoCalcDisplayName) {
                setAutoDisplayName()
            } else {
                state.autoCalcDisplayName = (contact_display_name.value.trim() == getAutoDisplayName())
            }
        }
    } // EditContactActivity.onDisplayNameFocusChanged()

    // *****************************************************************

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
    } // EditContactActivity.getPrimaryNumberStatus()

    // *****************************************************************

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
    } // EditContactActivity.getPhotoUpdateStatus()

    // *****************************************************************

    private fun toggleFavorite() {
        val isStarred = isContactStarred()
        contact_toggle_favorite.apply {
            setImageDrawable(getStarDrawable(!isStarred))
            tag = if (isStarred) 0 else 1

            setOnLongClickListener { toast(R.string.toggle_favorite); true; }
        }
    } // EditContactActivity.toggleFavorite()

    // *****************************************************************

    private fun isContactStarred() = contact_toggle_favorite.tag == 1

    // *****************************************************************

    private fun getStarDrawable(on: Boolean) = resources.getDrawable(if (on) R.drawable.ic_star_vector else R.drawable.ic_star_outline_vector)

    // *****************************************************************

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
    } // EditContactActivity.trySetPhoto()

    // *****************************************************************

    private fun parseIntentData(data: ArrayList<ContentValues>) {
        data.forEach {
            when (it.get(StructuredName.MIMETYPE)) {
                CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE -> parseName(it)
                CommonDataKinds.Phone.CONTENT_ITEM_TYPE -> parsePhoneNumber(it)
                CommonDataKinds.Email.CONTENT_ITEM_TYPE -> parseEmail(it)
                CommonDataKinds.StructuredPostal.CONTENT_ITEM_TYPE -> parseAddress(it)
                CommonDataKinds.Event.CONTENT_ITEM_TYPE -> parseEvent(it)
                CommonDataKinds.Note.CONTENT_ITEM_TYPE -> parseNote(it)
                CommonDataKinds.Organization.CONTENT_ITEM_TYPE -> parseOrganization(it)
                CommonDataKinds.Website.CONTENT_ITEM_TYPE -> parseWebsite(it)
                CommonDataKinds.Relation.CONTENT_ITEM_TYPE -> parseRelation(it)
            }
        }
    } // EditContactActivity.parseIntentData()

    // *****************************************************************

    private fun parseName(contentValues: ContentValues) {
        val displayName = contentValues.getAsString(CommonDataKinds.StructuredName.DISPLAY_NAME) ?: ""
        val prefix = contentValues.getAsString(CommonDataKinds.StructuredName.PREFIX) ?: ""
        val givenName = contentValues.getAsString(CommonDataKinds.StructuredName.GIVEN_NAME) ?: ""
        val middleName = contentValues.getAsString(CommonDataKinds.StructuredName.MIDDLE_NAME) ?: ""
        val familyName = contentValues.getAsString(CommonDataKinds.StructuredName.FAMILY_NAME) ?: ""
        val suffix = contentValues.getAsString(CommonDataKinds.StructuredName.SUFFIX) ?: ""
        val phoneticGivenName = contentValues.getAsString(CommonDataKinds.StructuredName.PHONETIC_GIVEN_NAME) ?: ""
        val phoneticMiddleName = contentValues.getAsString(CommonDataKinds.StructuredName.PHONETIC_MIDDLE_NAME) ?: ""
        val phoneticFamilyName = contentValues.getAsString(CommonDataKinds.StructuredName.PHONETIC_FAMILY_NAME) ?: ""
        val name = ContactName(displayName, prefix, givenName, middleName, familyName, suffix,
                                phoneticGivenName, phoneticMiddleName, phoneticFamilyName)
        contact!!.name = name
    } // EditContactActivity.parseName()

    // *****************************************************************

    private fun parsePhoneNumber(contentValues: ContentValues) {
        val phoneNumber = contentValues.getAsString(CommonDataKinds.Phone.NUMBER) ?: return
        val type = contentValues.getAsInteger(CommonDataKinds.Phone.TYPE) ?: DEFAULT_PHONE_NUMBER_TYPE
        val label = contentValues.getAsString(CommonDataKinds.Phone.LABEL) ?: ""
        val number = PhoneNumber(phoneNumber, type, label,
                        phoneNumber.normalizePhoneNumber(), false)
        contact!!.phoneNumbers.add(number)
    } // EditContactActivity.parsePhoneNumber()

    // *****************************************************************

    private fun parseEmail(contentValues: ContentValues) {
        val emailValue = contentValues.getAsString(CommonDataKinds.Email.ADDRESS) ?: return
        val type = contentValues.getAsInteger(CommonDataKinds.Email.TYPE) ?: DEFAULT_EMAIL_TYPE
        val label = contentValues.getAsString(CommonDataKinds.Email.LABEL) ?: ""
        val email = Email(emailValue, type, label)
        contact!!.emails.add(email)
    } // EditContactActivity.parseEmail()

    // *****************************************************************

    private fun parseAddress(contentValues: ContentValues) {
        val formattedAddress = contentValues.getAsString(StructuredPostal.FORMATTED_ADDRESS) ?: ""
        val type = contentValues.getAsInteger(CommonDataKinds.StructuredPostal.TYPE) ?: DEFAULT_ADDRESS_TYPE
        val label = contentValues.getAsString(CommonDataKinds.StructuredPostal.LABEL) ?: ""
        val street = contentValues.getAsString(CommonDataKinds.StructuredPostal.STREET) ?: ""
        val postOfficeBox = contentValues.getAsString(CommonDataKinds.StructuredPostal.POBOX) ?: ""
        val neighborhood = contentValues.getAsString(CommonDataKinds.StructuredPostal.NEIGHBORHOOD) ?: ""
        val city = contentValues.getAsString(CommonDataKinds.StructuredPostal.CITY) ?: ""
        val region = contentValues.getAsString(CommonDataKinds.StructuredPostal.REGION) ?: ""
        val postalCode = contentValues.getAsString(CommonDataKinds.StructuredPostal.POSTCODE) ?: ""
        val country = contentValues.getAsString(CommonDataKinds.StructuredPostal.COUNTRY) ?: ""
        val address = Address(formattedAddress, street, postOfficeBox, neighborhood,
                              city, region, postalCode, country, type, label)
        contact!!.addresses.add(address)
    } // EditContactActivity.parseAddress()

    // *****************************************************************

    private fun parseEvent(contentValues: ContentValues) {
        val eventValue = contentValues.getAsString(CommonDataKinds.Event.START_DATE) ?: return
        val type = contentValues.getAsInteger(CommonDataKinds.Event.TYPE) ?: DEFAULT_EVENT_TYPE
        val label = contentValues.getAsString(CommonDataKinds.Event.LABEL) ?: ""
        val event = Event(eventValue, type, label)
        contact!!.events.add(event)
    } // EditContactActivity.parseEvent()

    // *****************************************************************

    private fun parseNote(contentValues: ContentValues) {
        val note = contentValues.getAsString(Note.DATA1) ?: return
        contact!!.notes = note
    } // EditContactActivity.parseNote()

    // *****************************************************************

    private fun parseOrganization(contentValues: ContentValues) {
        val company = contentValues.getAsString(CommonDataKinds.Organization.COMPANY) ?: ""
        val type = contentValues.getAsInteger(CommonDataKinds.Organization.TYPE) ?: DEFAULT_ORGANIZATION_TYPE
        val label = contentValues.getAsString(CommonDataKinds.Organization.LABEL) ?: ""
        val jobPosition = contentValues.getAsString(CommonDataKinds.Organization.TITLE) ?: ""
        val department = contentValues.getAsString(CommonDataKinds.Organization.DEPARTMENT) ?: ""
        val jobDescription = contentValues.getAsString(CommonDataKinds.Organization.JOB_DESCRIPTION) ?: ""
        val symbol = contentValues.getAsString(CommonDataKinds.Organization.SYMBOL) ?: ""
        val phoneticName = contentValues.getAsString(CommonDataKinds.Organization.PHONETIC_NAME) ?: ""
        val location = contentValues.getAsString(CommonDataKinds.Organization.OFFICE_LOCATION) ?: ""
        contact!!.organization = Organization(company, jobPosition, department,
                            jobDescription, symbol, phoneticName, location, type, label)
    } // EditContactActivity.parseOrganization()

    // *****************************************************************

    private fun parseRelation(contentValues: ContentValues) {
        val name = contentValues.getAsString(CommonDataKinds.Relation.NAME) ?: return
        val type = contentValues.getAsInteger(CommonDataKinds.Relation.TYPE) ?: DEFAULT_RELATION_TYPE
        val label = contentValues.getAsString(CommonDataKinds.Relation.LABEL) ?: ""
        contact!!.relations.add(ContactRelation(name, type, label))
    } // EditContactActivity.parseRelation()

    // *****************************************************************

    private fun parseWebsite(contentValues: ContentValues) {
        val website = contentValues.getAsString(CommonDataKinds.Website.URL) ?: return
        val type = contentValues.getAsInteger(CommonDataKinds.Website.TYPE) ?: DEFAULT_RELATION_TYPE
        val label = contentValues.getAsString(CommonDataKinds.Website.LABEL) ?: ""
        contact!!.websites.add(ContactWebsite(website, type, label))
    } // EditContactActivity.parseWebsite()

    // *****************************************************************

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
    } // EditContactActivity.startTakePhotoIntent()

    // *****************************************************************

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
    } // EditContactActivity.startChoosePhotoIntent()

    // *****************************************************************

    override fun customRingtoneSelected(ringtonePath: String) {
        contact!!.ringtone = ringtonePath
        val ringtoneFilename = ringtonePath.getFilenameFromPath()
        if (!isSoundOfSilence(ringtoneFilename))
            contact_ringtone.text = ringtoneFilename
        else
            contact_ringtone.text = getString(R.string.no_sound)
    } // EditContactActivity.customRingtoneSelected()

    // *****************************************************************

    override fun systemRingtoneSelected(uri: Uri?) {
        contact!!.ringtone = uri?.toString() ?: ""
        val contactRingtone = RingtoneManager.getRingtone(this, uri)
        val ringtoneFilename = contactRingtone.getTitle(this)
        if ((uri != null) && !isSoundOfSilence(ringtoneFilename))
            contact_ringtone.text = ringtoneFilename
        else
            contact_ringtone.text = getString(R.string.no_sound)
    } // EditContactActivity.systemRingtoneSelected()

    // *****************************************************************

    private fun getNicknameTypeId(value: String) = when (value) {
        getString(R.string.nickname_default) -> Nickname.TYPE_DEFAULT
        getString(R.string.nickname_other_name) -> Nickname.TYPE_OTHER_NAME
        getString(R.string.nickname_maiden_name) -> Nickname.TYPE_MAIDEN_NAME
        getString(R.string.nickname_short_name) -> Nickname.TYPE_SHORT_NAME
        getString(R.string.nickname_initials) -> Nickname.TYPE_INITIALS
        else -> Nickname.TYPE_CUSTOM
    } // EditContactActivity.getNicknameTypeId()

    // *****************************************************************

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
    } // EditContactActivity.getPhoneNumberTypeId()

    // *****************************************************************

    private fun getEmailTypeId(value: String) = when (value) {
        getString(R.string.home) -> CommonDataKinds.Email.TYPE_HOME
        getString(R.string.work) -> CommonDataKinds.Email.TYPE_WORK
        getString(R.string.mobile) -> CommonDataKinds.Email.TYPE_MOBILE
        getString(R.string.other) -> CommonDataKinds.Email.TYPE_OTHER
        else -> CommonDataKinds.Email.TYPE_CUSTOM
    } // EditContactActivity.getEmailTypeId()

    // *****************************************************************

    private fun getAddressTypeId(value: String) = when (value) {
        getString(R.string.home) -> StructuredPostal.TYPE_HOME
        getString(R.string.work) -> StructuredPostal.TYPE_WORK
        getString(R.string.other) -> StructuredPostal.TYPE_OTHER
        else -> StructuredPostal.TYPE_CUSTOM
    } // EditContactActivity.getAddressTypeId()

    // *****************************************************************

    private fun getIMTypeId(value: String) = when (value) {
        getString(R.string.home) -> Im.TYPE_HOME
        getString(R.string.work) -> Im.TYPE_WORK
        getString(R.string.other) -> Im.TYPE_OTHER
        else -> Im.TYPE_CUSTOM
    } // EditContactActivity.getIMTypeId()

    // *****************************************************************

    private fun getIMProtocolId(value: String) = when (value) {
        getString(R.string.instantmsg_aim) -> Im.PROTOCOL_AIM
        getString(R.string.instantmsg_windows_live) -> Im.PROTOCOL_MSN
        getString(R.string.instantmsg_yahoo) -> Im.PROTOCOL_YAHOO
        getString(R.string.instantmsg_skype) -> Im.PROTOCOL_SKYPE
        getString(R.string.instantmsg_qq) -> Im.PROTOCOL_QQ
        getString(R.string.instantmsg_hangouts) -> Im.PROTOCOL_GOOGLE_TALK
        getString(R.string.instantmsg_icq) -> Im.PROTOCOL_ICQ
        getString(R.string.instantmsg_jabber) -> Im.PROTOCOL_JABBER
        getString(R.string.instantmsg_xmpp) -> Im.PROTOCOL_JABBER
        
        getString(R.string.instantmsg_sip) -> IM.PROTOCOL_SIP
        getString(R.string.instantmsg_irc) -> IM.PROTOCOL_IRC

        getString(R.string.instantmsg_matrix) -> IM.PROTOCOL_MATRIX
        getString(R.string.instantmsg_matrix_alt) -> IM.PROTOCOL_MATRIX
        getString(R.string.instantmsg_mastodon) -> IM.PROTOCOL_MASTODON
        getString(R.string.instantmsg_signal) -> IM.PROTOCOL_SIGNAL
        getString(R.string.instantmsg_telegram) -> IM.PROTOCOL_TELEGRAM
        getString(R.string.instantmsg_diaspora) -> IM.PROTOCOL_DIASPORA
        getString(R.string.instantmsg_viber) -> IM.PROTOCOL_VIBER
        getString(R.string.instantmsg_threema) -> IM.PROTOCOL_THREEMA
        getString(R.string.instantmsg_discord) -> IM.PROTOCOL_DISCORD
        getString(R.string.instantmsg_mumble) -> IM.PROTOCOL_MUMBLE
        getString(R.string.instantmsg_olvid) -> IM.PROTOCOL_OLVID
        getString(R.string.instantmsg_teamspeak) -> IM.PROTOCOL_TEAMSPEAK
        getString(R.string.instantmsg_facebook) -> IM.PROTOCOL_FACEBOOK
        getString(R.string.instantmsg_instagram) -> IM.PROTOCOL_INSTAGRAM
        getString(R.string.instantmsg_whatsapp) -> IM.PROTOCOL_WHATSAPP
        getString(R.string.instantmsg_twitter) -> IM.PROTOCOL_TWITTER
        getString(R.string.instantmsg_wechat) -> IM.PROTOCOL_WECHAT
        getString(R.string.instantmsg_weibo) -> IM.PROTOCOL_WEIBO
        getString(R.string.instantmsg_tiktok) -> IM.PROTOCOL_TIKTOK
        getString(R.string.instantmsg_tumblr) -> IM.PROTOCOL_TUMBLR
        getString(R.string.instantmsg_flickr) -> IM.PROTOCOL_FLICKR
        getString(R.string.instantmsg_linkedin) -> IM.PROTOCOL_LINKEDIN
        getString(R.string.instantmsg_xing) -> IM.PROTOCOL_XING
        getString(R.string.instantmsg_kik) -> IM.PROTOCOL_KIK
        getString(R.string.instantmsg_line) -> IM.PROTOCOL_LINE
        getString(R.string.instantmsg_kakaotalk) -> IM.PROTOCOL_KAKAOTALK
        getString(R.string.instantmsg_zoom) -> IM.PROTOCOL_ZOOM
        getString(R.string.instantmsg_github) -> IM.PROTOCOL_GITHUB
        getString(R.string.instantmsg_googleplus) -> IM.PROTOCOL_GOOGLEPLUS
        getString(R.string.instantmsg_pinterest) -> IM.PROTOCOL_PINTEREST
        // getString(R.string.instantmsg_qzone) -> IM.PROTOCOL_QZONE
        getString(R.string.instantmsg_youtube) -> IM.PROTOCOL_YOUTUBE
        getString(R.string.instantmsg_snapchat) -> IM.PROTOCOL_SNAPCHAT
        getString(R.string.instantmsg_teams) -> IM.PROTOCOL_TEAMS
        getString(R.string.instantmsg_googlemeet) -> IM.PROTOCOL_GOOGLEMEET
        getString(R.string.instantmsg_teamviewermeet) -> IM.PROTOCOL_TEAMVIEWERMEET
        getString(R.string.instantmsg_nextcloudtalk) -> IM.PROTOCOL_NEXTCLOUDTALK
        getString(R.string.instantmsg_slack) -> IM.PROTOCOL_SLACK
        getString(R.string.instantmsg_jitsi) -> IM.PROTOCOL_JITSI
        getString(R.string.instantmsg_webex) -> IM.PROTOCOL_WEBEX
        getString(R.string.instantmsg_gotomeeting) -> IM.PROTOCOL_GOTOMEETING
        getString(R.string.instantmsg_bigbluebutton) -> IM.PROTOCOL_BIGBLUEBUTTON
        else -> Im.PROTOCOL_CUSTOM
    } // EditContactActivity.getIMProtocolId()

    // *****************************************************************

    private fun getEventTypeId(value: String) = when (value) {
        getString(R.string.anniversary) -> CommonDataKinds.Event.TYPE_ANNIVERSARY
        getString(R.string.birthday) -> CommonDataKinds.Event.TYPE_BIRTHDAY
        else -> CommonDataKinds.Event.TYPE_OTHER
    } // EditContactActivity.getEventTypeId()

    // *****************************************************************

    private fun getWebsiteTypeId(value: String): Int = when (value) {
        getString(R.string.home) -> Website.TYPE_HOME
        getString(R.string.work) -> Website.TYPE_WORK
        getString(R.string.website_homepage) -> Website.TYPE_HOMEPAGE
        getString(R.string.website_blog) -> Website.TYPE_BLOG
        getString(R.string.website_profile) -> Website.TYPE_PROFILE
        getString(R.string.website_ftp) -> Website.TYPE_FTP
        getString(R.string.other) -> Website.TYPE_OTHER
        else -> Website.TYPE_CUSTOM
    } // EditContactActivity.getWebsiteTypeId()

}  // class EditContactActivity
