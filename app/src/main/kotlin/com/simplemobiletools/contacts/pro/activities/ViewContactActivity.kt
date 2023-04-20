/* *********************************************************************
 *                                                                     *
 *                        ViewContactActivity.kt                       *
 *                                                                     *
 ***********************************************************************
 * 321098765432109876543210987654321+123456789012345678901234567890123 *
 * Every Android application needs to be prepared that processing is
 * interrupted at any time (e.g. because the user switches to another
 * app, turns off the devices, switches from landscape to portrait mode
 * or for any other reason). However if the user resumes working with
 * our app, he should seamlessly resume where he left of. Thus when
 * the the app is paused it needs to store it current state, so that
 * it can restore it when processing resumes.
 * EditContactState contains fields to store the state for all fields
 * of the EditContactActivity.
 * Note: While the EditContactActivity is active, the elements of
 * EditContactState are NOT updated to reflect the current state of
 * all user interface elements. This only occurs on onSaveInstanceState()!
 * The state information will be serialized and stored in the
 * savedInstanceState parameter that Android provides for the onCreate
 * and onSaveInstanceState callbacks. Serialization is accomplished by the
 * kolinx.serialization library, which requires the appropriate
 * configuration of the Gradle build file (see instructions below)
 * and a resync of project to the changed gradle file (using the
 * Android Studio menu item "File/Sync Project with Gradle File".
 * https://kotlinlang.org/docs/serialization.html
 * https://developer.android.com/guide/components/activities/activity-lifecycle
 * https://developer.android.com/topic/libraries/architecture/saving-states
 */

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
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.RelativeLayout
import androidx.core.view.isVisible
import androidx.core.view.children
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.FitCenter
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.request.RequestOptions
import com.simplemobiletools.commons.dialogs.CallConfirmationDialog
import com.simplemobiletools.commons.dialogs.ConfirmationDialog
import com.simplemobiletools.commons.dialogs.SelectAlarmSoundDialog
import com.simplemobiletools.commons.extensions.*
import com.simplemobiletools.commons.helpers.*
import com.simplemobiletools.commons.models.contacts.*
import com.simplemobiletools.commons.models.PhoneNumber
import com.simplemobiletools.contacts.pro.R
import com.simplemobiletools.contacts.pro.dialogs.ChooseSocialDialog
import com.simplemobiletools.contacts.pro.dialogs.ManageVisibleFieldsDialog
import com.simplemobiletools.contacts.pro.extensions.*
import com.simplemobiletools.contacts.pro.helpers.*
import kotlinx.android.synthetic.main.activity_edit_contact.*
import kotlinx.android.synthetic.main.activity_view_contact.*
import kotlinx.android.synthetic.main.activity_view_contact.contact_addresses_holder
import kotlinx.android.synthetic.main.activity_view_contact.contact_addresses_image
import kotlinx.android.synthetic.main.activity_view_contact.contact_appbar
import kotlinx.android.synthetic.main.activity_view_contact.contact_emails_holder
import kotlinx.android.synthetic.main.activity_view_contact.contact_emails_image
import kotlinx.android.synthetic.main.activity_view_contact.contact_events_holder
import kotlinx.android.synthetic.main.activity_view_contact.contact_events_image
import kotlinx.android.synthetic.main.activity_view_contact.contact_groups_holder
import kotlinx.android.synthetic.main.activity_view_contact.contact_groups_image
import kotlinx.android.synthetic.main.activity_view_contact.contact_ims_holder
import kotlinx.android.synthetic.main.activity_view_contact.contact_ims_image
import kotlinx.android.synthetic.main.activity_view_contact.contact_name_image
import kotlinx.android.synthetic.main.activity_view_contact.contact_notes
import kotlinx.android.synthetic.main.activity_view_contact.contact_notes_image
import kotlinx.android.synthetic.main.activity_view_contact.contact_notes_share_icon
import kotlinx.android.synthetic.main.activity_view_contact.contact_notes_share_info
import kotlinx.android.synthetic.main.activity_view_contact.contact_numbers_holder
import kotlinx.android.synthetic.main.activity_view_contact.contact_numbers_image
import kotlinx.android.synthetic.main.activity_view_contact.contact_organization_company
import kotlinx.android.synthetic.main.activity_view_contact.contact_organization_image
import kotlinx.android.synthetic.main.activity_view_contact.contact_organization_share_icon
import kotlinx.android.synthetic.main.activity_view_contact.contact_organization_share_info
import kotlinx.android.synthetic.main.activity_view_contact.contact_photo
import kotlinx.android.synthetic.main.activity_view_contact.contact_photo_bottom_shadow
import kotlinx.android.synthetic.main.activity_view_contact.contact_photo_share_icon
import kotlinx.android.synthetic.main.activity_view_contact.contact_photo_share_info
import kotlinx.android.synthetic.main.activity_view_contact.contact_relations_holder
import kotlinx.android.synthetic.main.activity_view_contact.contact_relations_image
import kotlinx.android.synthetic.main.activity_view_contact.contact_ringtone
import kotlinx.android.synthetic.main.activity_view_contact.contact_ringtone_image
import kotlinx.android.synthetic.main.activity_view_contact.contact_scrollview
import kotlinx.android.synthetic.main.activity_view_contact.contact_source_image
import kotlinx.android.synthetic.main.activity_view_contact.contact_toggle_favorite
import kotlinx.android.synthetic.main.activity_view_contact.contact_toolbar
import kotlinx.android.synthetic.main.activity_view_contact.contact_websites_holder
import kotlinx.android.synthetic.main.activity_view_contact.contact_websites_image
import kotlinx.android.synthetic.main.activity_view_contact.contact_wrapper
import kotlinx.android.synthetic.main.item_view_phone_number.view.*
import kotlinx.android.synthetic.main.item_view_email.view.*
import kotlinx.android.synthetic.main.item_view_address.view.*
import kotlinx.android.synthetic.main.item_view_im.view.*
import kotlinx.android.synthetic.main.item_view_event.view.*
import kotlinx.android.synthetic.main.item_view_group.view.*
import kotlinx.android.synthetic.main.item_view_website.view.*
import kotlinx.android.synthetic.main.item_view_relation.view.*
import kotlinx.android.synthetic.main.item_view_contact_source.view.*

class ViewContactActivity : ContactActivity() {
    private var isViewIntent = false
    private var wasEditLaunched = false
    private var duplicateContacts = ArrayList<Contact>()
    private var contactSources = ArrayList<ContactSource>()
    private var showFields = 0
    private var fullContact: Contact? = null        // contact with all fields filled from duplicates
    private var origFullContact: Contact? = null    // contact with all fields filled from duplicates
    private var duplicateInitialized = false
    private val mergeDuplicate: Boolean get() = config.mergeDuplicateContacts
    private var configAutoFormattedAddressFormat: String = Address.AddressFormatUSA

    private val COMPARABLE_PHONE_NUMBER_LENGTH = 9

    override fun onCreate(savedInstanceState: Bundle?) {
        showTransparentTop = true

        super.onCreate(savedInstanceState)
        fullContact = restoreContact(savedInstanceState, "ActiveFullContact")
        origFullContact = restoreContact(savedInstanceState, "OrigFullContact")

        setContentView(R.layout.activity_view_contact)

        if (checkAppSideloading()) {
            return
        }

        showFields = config.showContactFields
        configAutoFormattedAddressFormat = getString(R.string.address_format)

        contact_wrapper.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
        setupMenu()
    } // ViewContactActivity.onCreate()

    override fun onResume() {
        super.onResume()
        showFields = config.showContactFields
        isViewIntent = (intent.action == ContactsContract.QuickContact.ACTION_QUICK_CONTACT) ||
                       (intent.action == Intent.ACTION_VIEW)
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
    } // ViewContactActivity.onResume()

    override fun onSaveInstanceState(savedInstanceState: Bundle) {
        var itemCnt: Int
        saveContact(savedInstanceState, "ActiveFullContact", fullContact)
        saveContact(savedInstanceState, "OrigFullContact", origFullContact)

        state.sharePhoto = contact_photo_share_info.isChecked
        state.shareName = true
        state.shareNickname.clear()

        /*
        itemCnt = contact_numbers_holder.childCount
        for (i in 1..itemCnt) {
            val nicknameHolder = contact_nicknames_holder.getChildAt(i-1)
            state.shareNickname.add(nicknameHolder.contact_nickname_share_info.isChecked)
        }
        */

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

        state.shareNotes = contact_notes_share_info.isChecked
        // state.shareRingtone = false
        state.shareOrganization = contact_organization_share_info.isChecked

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
        state.vScrollHeight  = contact_scrollview.height
        state.vScrollPosX  = contact_scrollview.scrollX
        state.vScrollPosY  = contact_scrollview.scrollY

        super.onSaveInstanceState(savedInstanceState)
    } // ViewContactActivity.onSaveInstanceState()

    override fun onBackPressed() {
        if (state.selectShareFieldsActive) {
            state.selectShareFieldsActive = false
            setupFieldShareIcons(false)
        } else if (contact_photo_big.alpha == 1f) {
            hideBigContactPhoto()
        } else {
            super.onBackPressed()
        }
    } // ViewContactActivity.onBackPressed()

    private fun setupMenu() {
        (contact_appbar.layoutParams as RelativeLayout.LayoutParams).topMargin = statusBarHeight
        contact_toolbar.menu.apply {
            findItem(R.id.share).setOnMenuItemClickListener {
                if (fullContact != null) {
                    // shareContact(fullContact!!)
                    toggleShareActive()
                }
                true
            }

            findItem(R.id.edit).setOnMenuItemClickListener {
                if (contact != null) {
                    if (state.selectShareFieldsActive) {
                        state.selectShareFieldsActive = false
                        setupFieldShareIcons(false)
                    }
                    if (contact_photo_big.isVisible()) {
                        contact_photo_big.beGone()
                        setupFieldShareIcons(state.selectShareFieldsActive)
                    }
                    launchEditContact(contact!!)
                }
                true
            }

            findItem(R.id.open_with).setOnMenuItemClickListener {
                if (state.selectShareFieldsActive) {
                    state.selectShareFieldsActive = false
                    setupFieldShareIcons(false)
                }
                if (contact_photo_big.isVisible()) {
                    contact_photo_big.beGone()
                    setupFieldShareIcons(state.selectShareFieldsActive)
                }
                openWith()
                true
            }

            findItem(R.id.delete).setOnMenuItemClickListener {
                if (state.selectShareFieldsActive) {
                    state.selectShareFieldsActive = false
                    setupFieldShareIcons(false)
                }
                if (contact_photo_big.isVisible()) {
                    contact_photo_big.beGone()
                    setupFieldShareIcons(state.selectShareFieldsActive)
                }
                deleteContactFromAllSources()
                true
            }

            findItem(R.id.manage_visible_fields).setOnMenuItemClickListener {
                // if (prevSavedInstanceState != null)
                //     onSaveInstanceState(prevSavedInstanceState!!)
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
    } // ViewContactActivity.setupMenu()

    private fun initContact() {
        var wasLookupKeyUsed = false
        var contactId: Int
        try {
            contactId = intent.getIntExtra(CONTACT_ID, 0)
        } catch (e: Exception) {
            return
        }

        if ((contactId == 0) && isViewIntent) {
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

        if ((contactId != 0) && !wasLookupKeyUsed) {
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
    } // ViewContactActivity.initContact()

    // *****************************************************************
    // *                ViewContactActivity.gotContact()               *
    // *****************************************************************
    /**
     * Contacts are fetched in a 3-step
     * .) Fetch the 'main' Contact
     * .) Fetch the list of available contact sources
     * .) Fetch Duplicates of the 'main' Contact (optional)
     */
    private fun gotContact() {
        if (isDestroyed || isFinishing) {
            return
        }
        showFields = config.showContactFields

        if (contact == null)
            return
        convertKnownIMCustomTypesToIDs(contact!!.IMs)
        convertKnownRelationCustomTypesToIDs(contact!!.relations)

        if ((origContact == null) || (contact != origContact)) {
            origContact = contact!!.deepCopy()
            if (config.mergeDuplicateContacts) {
                fullContact = null
                origFullContact = null
                duplicateInitialized = false
            } else {
                fullContact = contact!!.deepCopy()
                origFullContact = contact!!.deepCopy()
                duplicateInitialized = true
            }
            state.initShareInfo(contact!!)
        }

        // buildFullContact(mergeDuplicate, true)
        // state.initShareInfo(fullContact!!)
        setupViewContact()
        setupFieldShareIcons(state.selectShareFieldsActive)

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
            contact_name_image, contact_numbers_image, contact_emails_image,
            contact_addresses_image, contact_events_image, contact_source_image,
            contact_notes_image, contact_ringtone_image, contact_organization_image,
            contact_websites_image, contact_relations_image, contact_groups_image
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

        contact_toolbar.menu.findItem(R.id.open_with).isVisible = (contact?.isPrivate() == false)
    } // ViewContactActivity.gotContact()

    // *****************************************************************

    private fun setupViewContact() {
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN)
        setupFavorite()
        setupNames()

        ContactsHelper(this).getContactSources {
            contactSources = it
            runOnUiThread {
                setupContactDetails()
            }
        }

        if (config.mergeDuplicateContacts) {
            getDuplicateContacts {
                duplicateInitialized = true
                buildFullContact(true, true)
                convertKnownIMCustomTypesToIDs(fullContact!!.IMs)
                convertKnownRelationCustomTypesToIDs(fullContact!!.relations)
                if ((origFullContact == null) || (fullContact != origFullContact)) {
                    origFullContact = fullContact!!.deepCopy()
                    state.initShareInfo(fullContact!!)
                }
                setupContactDetails()
            }
        } else {
            setupContactDetails()
        }
    } // ViewContactActivity.setupViewContact()

    // *****************************************************************

    private fun buildFullContact(mergeDuplicate: Boolean, sortDataByType: Boolean) {
        if (contact == null) {
            fullContact = null
            return
        }

        fullContact = contact!!.deepCopy()
        val mergeDuplicate_ext: Boolean = mergeDuplicate && duplicateContacts.isNotEmpty()
        val sortDataByType_ext: Boolean = sortDataByType && mergeDuplicate_ext
        if (!mergeDuplicate_ext && !sortDataByType_ext)
            return

        // - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
        // Just a single photo can be displayed for all merged contacts.
        // Thus we shall just look for the first contact that has a photo
        // and use that for the merged contact. All other available photos
        // are ignored.
        if (mergeDuplicate_ext && (fullContact!!.photo == null) && fullContact!!.photoUri.isEmpty()) {
            val mergeCnt: Int = duplicateContacts.count()
            for (i in 0..mergeCnt -1) {
                if ((duplicateContacts[i].photo != null) ||
                    (duplicateContacts[i].photoUri.isNotEmpty())) {
                    fullContact!!.photo = duplicateContacts[i].photo
                    fullContact!!.photoUri = duplicateContacts[i].photoUri
                    break
                }
            }
        }

        // - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

        // FIXME Merge Name
        // The names of all merged contacts should always be the same.
        // Thus we can just use the name of the first contact and do
        // not need to provide any activity here...

        // - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

        if (mergeDuplicate_ext) {
            var nicknames: ArrayList<ContactNickname> = fullContact!!.nicknames
            duplicateContacts.forEach {
                nicknames.addAll(it.nicknames)
            }
            if (nicknames.count() > 1)
                nicknames = nicknames.distinctBy { it.name } as ArrayList<ContactNickname>
            fullContact!!.nicknames = nicknames
        }
        // Nickname types are a weird concept to begin with, and sorting
        // by nickname types is even weirder. Let's not do this!
        // if (sortDataByType_ext)
        //     fullContact!!.nicknames.sortBy{ it.type }

        // - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

        if (mergeDuplicate_ext) {
            val phoneNumbers: ArrayList<PhoneNumber> = fullContact!!.phoneNumbers
            duplicateContacts.forEach {
                phoneNumbers.addAll(it.phoneNumbers)
            }
            fullContact!!.phoneNumbers = phoneNumbers
        }
        if (sortDataByType_ext)
            fullContact!!.phoneNumbers.sortBy{ it.type }

        // Special treatment for phone numbers: Duplicates of phone numbers
        // (from multiple sources) are trimmed to leave a unique entry for
        // each number. If a number is a primary number for any source, it
        // will be considered a primary number for the collection too.
        if (mergeDuplicate_ext) {
            var phoneNumbers: ArrayList<PhoneNumber> = fullContact!!.phoneNumbers
            val defaultNumbers = phoneNumbers.filter { it.isPrimary }
            defaultNumbers.forEach { defaultNumber ->
                var candidate = phoneNumbers.find { (it.normalizedNumber == defaultNumber.normalizedNumber) && (!it.isPrimary) }
                while (candidate != null) {
                    candidate.isPrimary = true
                    candidate = phoneNumbers.find { (it.normalizedNumber == defaultNumber.normalizedNumber) && (!it.isPrimary) }
                }
            }
            // Now that we have spread the 'primary' attribute to all copies
            // of a phone number, we can simply discard all copies (but one)
            // of each number.
            // Note: We will look at the (at most) COMPARABLE_PHONE_NUMBER_LENGTH
            // FINAL (not first!!) digits of the phone number.
            if (phoneNumbers.count() > 1) {
                phoneNumbers = phoneNumbers.distinctBy {
                    if (it.normalizedNumber.length >= COMPARABLE_PHONE_NUMBER_LENGTH) {
                        it.normalizedNumber.substring(it.normalizedNumber.length - COMPARABLE_PHONE_NUMBER_LENGTH)
                    } else {
                        it.normalizedNumber
                    }
                } as ArrayList<PhoneNumber>
            }
            fullContact!!.phoneNumbers = phoneNumbers
        } // if (mergeDuplicate_ext)

        // - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

        // FIXME - The following documentation was part of the original
        // source, however it does not seem to be correct. Contacts are
        // considered duplicates it the names match, even if the emails don't!
        //
        // A contact can NOT have different emails per contact source. Such
        // contacts are handled as separate ones, not duplicates of each other.
        // Thus there is no need to merge the emails here!
        if (mergeDuplicate_ext) {
            var emails: ArrayList<Email> = fullContact!!.emails
            duplicateContacts.forEach {
                emails.addAll(it.emails)
            }
            if (emails.count() > 1)
                emails = emails.distinctBy { it.address } as ArrayList<Email>
            fullContact!!.emails = emails
        }
        if (sortDataByType_ext)
            fullContact!!.emails.sortBy{ it.type }

        // - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

        if (mergeDuplicate_ext) {
            var addresses: ArrayList<Address> = fullContact!!.addresses
            duplicateContacts.forEach {
                addresses.addAll(it.addresses)
            }
            if (addresses.count() > 1)
                addresses = addresses.distinctBy { it.formattedAddress } as ArrayList<Address>
            fullContact!!.addresses = addresses
        }
        if (sortDataByType_ext)
            fullContact!!.addresses.sortBy{ it.type }

        // - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

        if (mergeDuplicate_ext) {
            var IMs: ArrayList<IM> = fullContact!!.IMs
            duplicateContacts.forEach {
                IMs.addAll(it.IMs)
            }
            if (IMs.count() > 1)
                IMs = IMs.distinct() as ArrayList<IM>
            fullContact!!.IMs = IMs
        }
        if (sortDataByType_ext)
            fullContact!!.IMs.sortBy{ it.protocol }

        // - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

        if (mergeDuplicate_ext) {
            var events: ArrayList<Event> = fullContact!!.events
            duplicateContacts.forEach {
                events.addAll(it.events)
            }
            if (events.count() > 1)
                events = events.distinct() as ArrayList<Event>
            fullContact!!.events = events
        }
        if (sortDataByType_ext)
            fullContact!!.events.sortBy{ it.type }

        // - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

        // Just a single String is available for all merged notes. Thus
        // if we have merge contacts, we shall collect all notes, eliminate
        // duplicates and paste the remaining notes into a single string...
        if (mergeDuplicate_ext) {
            var notes: ArrayList<String> = ArrayList()
            var addNotes: String = fullContact!!.notes.trim()
            if (addNotes.isNotEmpty())
                notes.add(addNotes)

            duplicateContacts.forEach {
                addNotes = it.notes.trim()
                if (addNotes.isNotEmpty())
                    notes.add(addNotes)
            }
            if (notes.count() > 1)
                notes = notes.distinct() as ArrayList<String>

            if (notes.count() == 0)
                fullContact!!.notes = ""
            else if (notes.count() == 1)
                fullContact!!.notes = notes[0]
            else {
                var sumNotes: String = notes[0]
                for (i in 1 .. notes.count() -1)
                    sumNotes = sumNotes + "\n\n" + notes[i]
                fullContact!!.notes = sumNotes
            }
        }

        // - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

        // Just a single ringtone can be displayed for all merged contacts.
        // Pasting multiple ringtones together will not create a useful
        // result, thus we shall just look for the first contact that has
        // any, non-null ringtone information and use that for the merged
        // contact.
        if (mergeDuplicate_ext && (fullContact!!.ringtone?.trim()?.isEmpty() == true)) {
            val mergeCnt: Int = duplicateContacts.count()
            for (i in 0..mergeCnt -1) {
                if (duplicateContacts[i].ringtone?.trim()?.isNotEmpty() == true) {
                    fullContact!!.ringtone = duplicateContacts[i].ringtone?.trim()
                    break
                }
            }
        }

        // - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

        if (mergeDuplicate_ext && fullContact!!.organization.company.trim().isEmpty() &&
            fullContact!!.organization.jobTitle.trim().isEmpty()) {
            val mergeCnt: Int = duplicateContacts.count()
            for (i in 0..mergeCnt -1) {
                if ((duplicateContacts[i].organization.company.trim().isNotEmpty()) ||
                    (duplicateContacts[i].organization.jobTitle.trim().isNotEmpty())) {
                    fullContact!!.organization.company = duplicateContacts[i].organization.company.trim()
                    fullContact!!.organization.jobTitle = duplicateContacts[i].organization.jobTitle.trim()
                    break
                }
            }
        }

        // - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

        if (mergeDuplicate_ext) {
            val websites = fullContact!!.websites
            duplicateContacts.forEach {
                websites.addAll(it.websites)
            }
            fullContact!!.websites = websites
        }
        if (sortDataByType_ext)
            fullContact!!.websites.sortBy{ it.type }

        // - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

        if (mergeDuplicate_ext) {
            val relations = fullContact!!.relations
            duplicateContacts.forEach {
                relations.addAll(it.relations)
            }
            fullContact!!.relations = relations
        }
        if (sortDataByType_ext)
            fullContact!!.relations.sortBy{ it.type }

        // - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

        if (mergeDuplicate_ext) {
            val groups = fullContact!!.groups
            duplicateContacts.forEach {
                groups.addAll(it.groups)
            }
            fullContact!!.groups = groups
        }
        if (sortDataByType_ext)
            fullContact!!.groups.sortBy{ it.title }

        // - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

        // Merge Sources - See: setupContactSources()

        // - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
    } // ViewContactActivity.buildFullContact()

    // *****************************************************************

    private fun toggleShareActive() {
        if (!duplicateInitialized)
            return

        val exportSelectedFieldsOnly: Boolean = config.exportSelectedFieldsOnly
        var exportNow: Boolean = false
        if (exportSelectedFieldsOnly) {
            state.selectShareFieldsActive = !state.selectShareFieldsActive
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
            val exportContact: Contact = fillContactValues(exportSelectedFieldsOnly, false)
            convertKnownIMTypeIDsToCustomTypes(exportContact.IMs)
            shareContact(exportContact)
        }
    } // ViewContactActivity.toggleShareActive()

    // *****************************************************************

    private fun setupFieldShareIcons(selectShareActive: Boolean) {
        setupPhotoShareIcons(selectShareActive)
        setupNameShareIcons(selectShareActive)
        setupPhoneNumberShareIcons(selectShareActive)
        setupEmailShareIcons(selectShareActive)
        setupAddressShareIcons(selectShareActive)
        setupIMShareIcons(selectShareActive)
        setupEventShareIcons(selectShareActive)
        setupNoteShareIcons(selectShareActive)
        setupOrganizationShareIcons(selectShareActive)
        setupWebsiteShareIcons(selectShareActive)
        setupRelationShareIcons(selectShareActive)
        setupGroupShareIcons(selectShareActive)
        setupOrganizationShareIcons(selectShareActive)
    } // ViewContactActivity.setupFieldShareIcons()

    // *****************************************************************

    private fun setupFieldVisibility(selectShareActive: Boolean) {
        val showFields = config.showContactFields
        val isNameVisible: Boolean =  (showFields and (SHOW_PREFIX_FIELD or SHOW_FIRST_NAME_FIELD or
            SHOW_MIDDLE_NAME_FIELD or SHOW_SURNAME_FIELD or SHOW_SUFFIX_FIELD) != 0)
        val areNicknamesVisible: Boolean = ((showFields and SHOW_NICKNAME_FIELD) != 0)

        contact_name_image.beVisibleIf(isNameVisible or areNicknamesVisible)
        contact_name.beVisibleIf(isNameVisible or areNicknamesVisible)

        val arePhoneNumbersVisible: Boolean = ((showFields and SHOW_PHONE_NUMBERS_FIELD) != 0)
        contact_numbers_image.beVisibleIf(arePhoneNumbersVisible)
        contact_numbers_holder.beVisibleIf(arePhoneNumbersVisible)

        val areEmailsVisible: Boolean = ((showFields and SHOW_EMAILS_FIELD) != 0)
        contact_emails_image.beVisibleIf(areEmailsVisible)
        contact_emails_holder.beVisibleIf(areEmailsVisible)

        val areAddressesVisible: Boolean = ((showFields and (SHOW_ADDRESSES_FIELD or SHOW_STRUCTURED_POSTAL_ACTIVE_ADDRESS_MASK)) != 0)
        contact_addresses_image.beVisibleIf(areAddressesVisible)
        contact_addresses_holder.beVisibleIf(areAddressesVisible)

        val areIMsVisible: Boolean = ((showFields and SHOW_IMS_FIELD) != 0)
        contact_ims_image.beVisibleIf(areIMsVisible)
        contact_ims_holder.beVisibleIf(areIMsVisible)

        val isOrganizationVisible: Boolean = ((showFields and SHOW_ORGANIZATION_FIELD) != 0)
        contact_organization_image.beVisibleIf(isOrganizationVisible)
        contact_organization_company.beVisibleIf(isOrganizationVisible)
        contact_organization_job_position.beVisibleIf(isOrganizationVisible)

        val areEventsVisible: Boolean = ((showFields and SHOW_EVENTS_FIELD) != 0)
        contact_events_image.beVisibleIf(areEventsVisible)
        contact_events_holder.beVisibleIf(areEventsVisible)

        val areWebsitesVisible: Boolean = ((showFields and SHOW_WEBSITES_FIELD) != 0)
        contact_websites_image.beVisibleIf(areWebsitesVisible)
        contact_websites_holder.beVisibleIf(areWebsitesVisible)

        val areRelationsVisible: Boolean = ((showFields and SHOW_RELATIONS_FIELD) != 0)
        contact_relations_image.beVisibleIf(areRelationsVisible)
        contact_relations_holder.beVisibleIf(areRelationsVisible)

        val areGroupsVisible: Boolean = ((showFields and SHOW_GROUPS_FIELD) != 0)
        contact_groups_image.beVisibleIf(areGroupsVisible)
        contact_groups_holder.beVisibleIf(areGroupsVisible)

        val areNotesVisible: Boolean = ((showFields and SHOW_NOTES_FIELD) != 0)
        contact_notes_image.beVisibleIf(areNotesVisible)
        contact_notes.beVisibleIf(areNotesVisible)

        val isRingtoneVisible: Boolean = ((showFields and SHOW_RINGTONE_FIELD) != 0)
        contact_ringtone.beVisibleIf(isRingtoneVisible)
        contact_ringtone_image.beVisibleIf(isRingtoneVisible)

        val areSourcesVisible: Boolean = ((showFields and SHOW_CONTACT_SOURCE_FIELD) != 0)
        contact_source_image.beVisibleIf(areSourcesVisible)
        contact_sources_holder.beVisibleIf(areSourcesVisible)
    } // ViewContactActivity.setupFieldVisibility()

    // *****************************************************************

    private fun setupContactDetails() {
        if (isFinishing || isDestroyed || contact == null) {
            return
        }

        val viewContact: Contact? = if (duplicateInitialized) fullContact else contact
        val selectShareActive: Boolean = state.selectShareFieldsActive
        if (viewContact != null) {
            setupPhoneNumbers(viewContact, selectShareActive)
            setupEmails(viewContact, selectShareActive)
            setupAddresses(viewContact, selectShareActive)
            setupIMs(viewContact, selectShareActive)
            setupEvents(viewContact, selectShareActive)
            setupNotes(viewContact, selectShareActive)
            setupWebsites(viewContact, selectShareActive)
            setupRelations(viewContact, selectShareActive)
            setupGroups(viewContact, selectShareActive)
            setupContactSources(viewContact)
            setupRingtone(viewContact)
            setupOrganization(viewContact, selectShareActive)
            if (duplicateInitialized)
                setupFieldShareIcons(state.selectShareFieldsActive)
        }
        updateTextColors(contact_scrollview)
        if (!selectShareActive) {
            contact_toggle_favorite.beVisible()
            contact_send_sms.beVisibleIf(viewContact!!.phoneNumbers.isNotEmpty())
            contact_start_call.beVisibleIf(viewContact!!.phoneNumbers.isNotEmpty())
            contact_send_email.beVisibleIf(viewContact!!.emails.isNotEmpty())
        } else {
            contact_toggle_favorite.beGone()
            contact_send_sms.beGone()
            contact_start_call.beGone()
            contact_send_email.beGone()
        }

        contact_scrollview.beVisible()
        if (state.stateValid && (state.vScrollHeight > 0)) {
            val vScrollHeight: Int = contact_scrollview.height
            val vScrollTargetY = ((vScrollHeight * state.vScrollPosY) + state.vScrollHeight/2) / state.vScrollHeight
            contact_scrollview.scrollTo(0, vScrollTargetY)
        }
    } // ViewContactActivity.setupContactDetails()

    // *****************************************************************

    private fun launchEditContact(contact: Contact) {
        wasEditLaunched = true
        duplicateInitialized = false
        editContact(contact)
    } // ViewContactActivity.launchEditContact()

    // *****************************************************************

    private fun openWith() {
        if (contact != null) {
            val uri = getContactPublicUri(contact!!)
            launchViewContactIntent(uri)
        }
    } // ViewContactActivity.openWith()

    // *****************************************************************

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
    } // ViewContactActivity.setupFavorite()

    // *****************************************************************

    private fun setupPhotoShareIcons(selectShareActive: Boolean) {
        val viewContact: Contact? = if (duplicateInitialized) fullContact else contact
        if (selectShareActive) {
            contact_toggle_favorite.beGone()
            contact_send_email.beGone()
            contact_start_call.beGone()
            contact_send_sms.beGone()
        } else {
            contact_toggle_favorite.beVisible()
            if (viewContact != null) {
                contact_send_email.beVisibleIf(viewContact.emails.isNotEmpty())
                contact_start_call.beVisibleIf(viewContact.phoneNumbers.isNotEmpty())
                contact_send_sms.beVisibleIf(viewContact.phoneNumbers.isNotEmpty())
            } else {
                contact_send_email.beGone()
                contact_start_call.beGone()
                contact_send_sms.beGone()
            }
        }
        var ext_selectShareActive: Boolean = selectShareActive
        if ((viewContact!!.photo == null) && (viewContact!!.photoUri == ""))
            ext_selectShareActive = false
        contact_photo_share_info.beVisibleIf(ext_selectShareActive)
        contact_photo_share_icon.beVisibleIf(ext_selectShareActive)
    } // ViewContactActivity.setupPhotoShareIcons()

    // *****************************************************************

    private fun setupNames() {
        var displayName = contact!!.getNameToDisplay(config.contactNameShowFormattedName, config.contactNameFormat)
        if (contact!!.nicknames.isNotEmpty()) {
            var nickCount = 0
            contact!!.nicknames.forEach { if (it.name.isNotEmpty()) nickCount++ }
            if (nickCount > 0) {
                nickCount = 0
                contact!!.nicknames.forEach {
                    if (it.name.isNotEmpty()) {
                        if (nickCount == 0)
                            displayName += " (${it.name}"
                        else
                            displayName += ", ${it.name}"
                        nickCount++
                    }
                }
                displayName += ")"
            }
        }

        val phoneticName = contact!!.name.buildPhoneticName(config.contactNameFormat)
        if (phoneticName.isNotEmpty() && ((showFields and SHOW_PHONETIC_NAME_FIELDS) != 0))
            contact_name.text = "$displayName\n[$phoneticName]"
        else
            contact_name.text = displayName
        contact_name.copyOnLongClick(displayName)

        val showNameFields = ((showFields and (SHOW_PREFIX_FIELD or SHOW_FIRST_NAME_FIELD or SHOW_MIDDLE_NAME_FIELD or SHOW_SURNAME_FIELD or SHOW_SUFFIX_FIELD)) != 0)
        contact_name.beVisibleIf(displayName.isNotEmpty() && !contact!!.isABusinessContact() && showNameFields)
        contact_name_image.beInvisibleIf(contact_name.isGone())
    } // ViewContactActivity.setupNames()

    // *****************************************************************

    private fun setupNameShareIcons(selectShareActive: Boolean) {
        // FIXME
    } // ViewContactActivity.setupNameShareIcons()

    // *****************************************************************

    private fun setupPhoneNumbers(viewContact: Contact, showShareCheckbox: Boolean) {
        val phoneNumbers = viewContact.phoneNumbers
        contact_numbers_holder.removeAllViews()
        if (phoneNumbers.isNotEmpty() && ((showFields and SHOW_PHONE_NUMBERS_FIELD) != 0)) {
            phoneNumbers.forEachIndexed { index, phoneNumber ->
                layoutInflater.inflate(R.layout.item_view_phone_number, contact_numbers_holder, false).apply {
                    contact_numbers_holder.addView(this)
                    contact_number.text = phoneNumber.value
                    contact_number_type.text = getPhoneNumberTypeText(phoneNumber.type, phoneNumber.label)
                    copyOnLongClick(phoneNumber.value)

                    contact_number_share_info.isChecked = if (index < state.shareNumber.size) state.shareNumber[index] else true
                    contact_number_share_info.beVisibleIf(showShareCheckbox)
                    contact_number_share_icon.beVisibleIf(showShareCheckbox)

                    setOnClickListener {
                        if (config.showCallConfirmation) {
                            CallConfirmationDialog(this@ViewContactActivity, phoneNumber.value) {
                                startCallIntent(phoneNumber.value)
                            }
                        } else {
                            startCallIntent(phoneNumber.value)
                        }
                    }

                    contact_view_number_holder.default_toggle_icon.isVisible = phoneNumber.isPrimary
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
        } else {
            contact_send_sms.beGone()
            contact_start_call.beGone()
        }
    } // ViewContactActivity.setupPhoneNumbers()

    // *****************************************************************

    private fun setupPhoneNumberShareIcons(selectShareActive: Boolean) {
        val phoneNumberListHolder: ViewGroup = contact_numbers_holder
        val phoneNumberHolders: Sequence<View> = phoneNumberListHolder.children
        for (phoneNumberHolder in phoneNumberHolders) {
            phoneNumberHolder.contact_number_share_info.beVisibleIf(selectShareActive)
            phoneNumberHolder.contact_number_share_icon.beVisibleIf(selectShareActive)
        }
    } // ViewContactActivity.setupPhoneNumberShareIcons()

    // *****************************************************************

    // A contact cannot have different emails per contact source. Such contacts are handled as separate ones, not duplicates of each other
    private fun setupEmails(viewContact: Contact, showShareCheckbox: Boolean) {
        contact_emails_holder.removeAllViews()
        val emails = viewContact.emails
        if (emails.isNotEmpty() && ((showFields and SHOW_EMAILS_FIELD) != 0)) {
            emails.forEachIndexed { index, email ->
                layoutInflater.inflate(R.layout.item_view_email, contact_emails_holder, false).apply {
                    contact_emails_holder.addView(this)
                    contact_email.text = email.address
                    contact_email_type.text = getEmailTypeText(email.type, email.label)
                    copyOnLongClick(email.address)

                    contact_email_share_info.isChecked = if (index < state.shareEmail.size) state.shareEmail[index] else true
                    contact_email_share_info.beVisibleIf(showShareCheckbox)
                    contact_email_share_icon.beVisibleIf(showShareCheckbox)

                    setOnClickListener {
                        sendEmailIntent(email.address)
                    }
                }
            }
            contact_emails_image.beVisible()
            contact_emails_holder.beVisible()
        } else {
            contact_emails_image.beGone()
            contact_emails_holder.beGone()
        }
    } // ViewContactActivity.setupEmails()

    // *****************************************************************

    private fun setupEmailShareIcons(selectShareActive: Boolean) {
        val emailListHolder: ViewGroup = contact_emails_holder
        val emailHolders: Sequence<View> = emailListHolder.children
        for (emailHolder in emailHolders) {
            emailHolder.contact_email_share_info.beVisibleIf(selectShareActive)
            emailHolder.contact_email_share_icon.beVisibleIf(selectShareActive)
        }
    } // ViewContactActivity.setupEmailShareIcons()

    // *****************************************************************

    private fun setupAddresses(viewContact: Contact, showShareCheckbox: Boolean) {
        val addresses = viewContact.addresses

        contact_addresses_holder.removeAllViews()
        if (addresses.isNotEmpty() &&
            ((showFields and (SHOW_ADDRESSES_FIELD or SHOW_STRUCTURED_POSTAL_ACTIVE_ADDRESS_MASK)) != 0)) {
            addresses.forEachIndexed { index, address ->
                layoutInflater.inflate(R.layout.item_view_address, contact_addresses_holder, false).apply {
                    val displayAddress: String
                    contact_addresses_holder.addView(this)
                    if (((showFields and SHOW_ADDRESSES_FIELD) != 0) && (address.formattedAddress != ""))
                        displayAddress = address.formattedAddress
                    else {
                        val formattedAddress = address.getFormattedPostalAddress(configAutoFormattedAddressFormat)
                        if (((showFields and SHOW_STRUCTURED_POSTAL_ACTIVE_ADDRESS_MASK) != 0) && (formattedAddress != ""))
                            displayAddress = formattedAddress
                        else if (address.formattedAddress != "")
                            displayAddress = address.formattedAddress
                        else
                            displayAddress = formattedAddress
                    }
                    contact_address.text = displayAddress
                    contact_address_type.text = getAddressTypeText(address.type, address.label)
                    copyOnLongClick(displayAddress)

                    contact_address_share_info.isChecked = if (index < state.shareAddress.size) state.shareAddress[index] else true
                    contact_address_share_info.beVisibleIf(showShareCheckbox)
                    contact_address_share_icon.beVisibleIf(showShareCheckbox)

                    setOnClickListener {
                        sendAddressIntent(displayAddress)
                    }
                }
            }
            contact_addresses_image.beVisible()
            contact_addresses_holder.beVisible()
        } else {
            contact_addresses_image.beGone()
            contact_addresses_holder.beGone()
        }
    } // ViewContactActivity.setupAddresses()

    // *****************************************************************

    private fun setupAddressShareIcons(selectShareActive: Boolean) {
        val addressListHolder: ViewGroup = contact_addresses_holder
        val addressHolders: Sequence<View> = addressListHolder.children
        for (addressHolder in addressHolders) {
            addressHolder.contact_address_share_info.beVisibleIf(selectShareActive)
            addressHolder.contact_address_share_icon.beVisibleIf(selectShareActive)
        }
    } // ViewContactActivity.setupAddressShareIcons()

    // *****************************************************************

    private fun setupIMs(viewContact: Contact, showShareCheckbox: Boolean) {
        val IMs: ArrayList<IM> = viewContact.IMs

        contact_ims_holder.removeAllViews()
        if (IMs.isNotEmpty() && ((showFields and SHOW_IMS_FIELD) != 0)) {
            IMs.forEachIndexed { index, IM ->
                layoutInflater.inflate(R.layout.item_view_im, contact_ims_holder, false).apply {
                    contact_ims_holder.addView(this)
                    contact_im.text = IM.data
                    contact_im_type.text = getIMTypeText(IM.type, IM.label)
                    contact_im_protocol.text = getIMProtocolText(IM.protocol, IM.custom_protocol)
                    copyOnLongClick(IM.data)

                    contact_im_share_info.isChecked = if (index < state.shareIM.size) state.shareIM[index] else true
                    contact_im_share_info.beVisibleIf(showShareCheckbox)
                    contact_im_share_icon.beVisibleIf(showShareCheckbox)
                }
            }
            contact_ims_image.beVisible()
            contact_ims_holder.beVisible()
        } else {
            contact_ims_image.beGone()
            contact_ims_holder.beGone()
        }
    } // ViewContactActivity.setupIMs()

    // *****************************************************************

    private fun setupIMShareIcons(selectShareActive: Boolean) {
        val imListHolder: ViewGroup = contact_ims_holder
        val imHolders: Sequence<View> = imListHolder.children
        for (imHolder in imHolders) {
            imHolder.contact_im_share_info.beVisibleIf(selectShareActive)
            imHolder.contact_im_share_icon.beVisibleIf(selectShareActive)
        }
    } // ViewContactActivity.setupIMShareIcons()

    // *****************************************************************

    private fun setupEvents(viewContact: Contact, showShareCheckbox: Boolean) {
        val events: ArrayList<Event> = viewContact.events

        contact_events_holder.removeAllViews()
        if (events.isNotEmpty() && ((showFields and SHOW_EVENTS_FIELD) != 0)) {
            events.forEachIndexed { index, event ->
                layoutInflater.inflate(R.layout.item_view_event, contact_events_holder, false).apply {
                    contact_events_holder.addView(this)
                    event.startDate.getDateTimeFromDateString(true, contact_event)
                    contact_event_type.setText(getEventTypeText(event.type, event.label))
                    copyOnLongClick(event.startDate)

                    contact_event_share_info.isChecked = if (index < state.shareEvent.size) state.shareEvent[index] else true
                    contact_event_share_info.beVisibleIf(showShareCheckbox)
                    contact_event_share_icon.beVisibleIf(showShareCheckbox)
                }
            }
            contact_events_image.beVisible()
            contact_events_holder.beVisible()
        } else {
            contact_events_image.beGone()
            contact_events_holder.beGone()
        }
    } // ViewContactActivity.setupEvents()

    // *****************************************************************

    private fun setupEventShareIcons(selectShareActive: Boolean) {
        val eventListHolder: ViewGroup = contact_events_holder
        val eventHolders: Sequence<View> = eventListHolder.children
        for (eventHolder in eventHolders) {
            eventHolder.contact_event_share_info.beVisibleIf(selectShareActive)
            eventHolder.contact_event_share_icon.beVisibleIf(selectShareActive)
        }
    } // ViewContactActivity.setupEventShareIcons()

    // *****************************************************************

    private fun setupWebsites(viewContact: Contact, showShareCheckbox: Boolean) {
        val websites: ArrayList<ContactWebsite> = viewContact.websites

        contact_websites_holder.removeAllViews()
        if (websites.isNotEmpty() && ((showFields and SHOW_WEBSITES_FIELD) != 0)) {
            websites.forEachIndexed { index, url ->
                layoutInflater.inflate(R.layout.item_view_website, contact_websites_holder, false).apply {
                    contact_websites_holder.addView(this)
                    contact_website.text = url.URL
                    contact_website_type.setText(getWebsiteTypeText(url.type, url.label))
                    contact_website_type.beVisible()
                    copyOnLongClick(url.URL)

                    contact_website_share_info.isChecked = if (index < state.shareWebsite.size) state.shareWebsite[index] else true
                    contact_website_share_info.beVisibleIf(showShareCheckbox)
                    contact_website_share_icon.beVisibleIf(showShareCheckbox)

                    setOnClickListener {
                        openWebsiteIntent(url.URL)
                    }
                }
            }
            contact_websites_image.beVisible()
            contact_websites_holder.beVisible()
        } else {
            contact_websites_image.beGone()
            contact_websites_holder.beGone()
        }
    } // ViewContactActivity.setupWebsites()

    // *****************************************************************

    private fun setupWebsiteShareIcons(selectShareActive: Boolean) {
        val websiteListHolder: ViewGroup = contact_websites_holder
        val websiteHolders: Sequence<View> = websiteListHolder.children
        for (websiteHolder in websiteHolders) {
            websiteHolder.contact_website_share_info.beVisibleIf(selectShareActive)
            websiteHolder.contact_website_share_icon.beVisibleIf(selectShareActive)
        }
    } // ViewContactActivity.setupWebsiteShareIcons()

    // *****************************************************************

    private fun setupRelations(viewContact: Contact, showShareCheckbox: Boolean) {
        val relations: ArrayList<ContactRelation> = viewContact.relations

        contact_relations_holder.removeAllViews()
        if (relations.isNotEmpty() && ((showFields and SHOW_RELATIONS_FIELD) != 0)) {
            relations.forEachIndexed { index, relation ->
                layoutInflater.inflate(R.layout.item_view_relation, contact_relations_holder, false).apply {
                    contact_relations_holder.addView(this)
                    contact_relation.text = relation.name
                    contact_relation_type.setText(getRelationTypeText(relation.type, relation.label))
                    contact_relation_type.beVisible()
                    copyOnLongClick(relation.name)

                    contact_relation_share_info.isChecked = if (index < state.shareRelation.size) state.shareRelation[index] else true
                    contact_relation_share_info.beVisibleIf(showShareCheckbox)
                    contact_relation_share_icon.beVisibleIf(showShareCheckbox)

                    // setOnClickListener {
                    //     openRelationIntent(relation.name)
                    // }
                }
            }
            contact_relations_image.beVisible()
            contact_relations_holder.beVisible()
        } else {
            contact_relations_image.beGone()
            contact_relations_holder.beGone()
        }
    } // ViewContactActivity.setupRelations()

    // *****************************************************************

    private fun setupRelationShareIcons(selectShareActive: Boolean) {
        val relationListHolder: ViewGroup = contact_relations_holder
        val relationHolders: Sequence<View> = relationListHolder.children
        for (relationHolder in relationHolders) {
            relationHolder.contact_relation_share_info.beVisibleIf(selectShareActive)
            relationHolder.contact_relation_share_icon.beVisibleIf(selectShareActive)
        }
    } // ViewContactActivity.setupRelationShareIcons()

    // *****************************************************************

    private fun setupGroups(viewContact: Contact, showShareCheckbox: Boolean) {
        val groups = viewContact.groups

        contact_groups_holder.removeAllViews()
        if (groups.isNotEmpty() && showFields and SHOW_GROUPS_FIELD != 0) {
            groups.forEachIndexed { index, group ->
                layoutInflater.inflate(R.layout.item_view_group, contact_groups_holder, false).apply {
                    contact_groups_holder.addView(this)
                    contact_group.text = group.title
                    copyOnLongClick(group.title)

                    contact_group_share_info.isChecked = if (index < state.shareGroup.size) state.shareGroup[index] else true
                    contact_group_share_info.beVisibleIf(showShareCheckbox)
                    contact_group_share_icon.beVisibleIf(showShareCheckbox)
                }
            }
            contact_groups_image.beVisible()
            contact_groups_holder.beVisible()
        } else {
            contact_groups_image.beGone()
            contact_groups_holder.beGone()
        }
    } // ViewContactActivity.setupGroups()

    // *****************************************************************

    private fun setupGroupShareIcons(selectShareActive: Boolean) {
        val groupListHolder: ViewGroup = contact_groups_holder
        val groupHolders: Sequence<View> = groupListHolder.children
        for (groupHolder in groupHolders) {
            groupHolder.contact_group_share_info.beVisibleIf(selectShareActive)
            groupHolder.contact_group_share_icon.beVisibleIf(selectShareActive)
        }
    } // ViewContactActivity.setupGroupShareIcons()

    // *****************************************************************

    private fun setupContactSources(viewContact: Contact) {
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
    } // ViewContactActivity.setupContactSources()

    // *****************************************************************

    private fun setupNotes(viewContact: Contact, showShareCheckbox: Boolean) {
        val notes = viewContact.notes
        if (notes.isNotEmpty() && ((showFields and SHOW_NOTES_FIELD) != 0)) {
            contact_notes.text = notes
            contact_notes_image.beVisible()
            contact_notes.beVisible()
            contact_notes.copyOnLongClick(notes)

            contact_notes_share_info.isChecked = state.shareNotes
            contact_notes_share_info.beVisibleIf(showShareCheckbox)
            contact_notes_share_icon.beVisibleIf(showShareCheckbox)
        } else {
            contact_notes_image.beGone()
            contact_notes.beGone()
            contact_notes_share_info.isChecked = true
            contact_notes_share_info.beGone()
            contact_notes_share_icon.beGone()
        }
    } // ViewContactActivity.setupNotes()

    // *****************************************************************

    private fun setupNoteShareIcons(selectShareActive: Boolean) {
        if (contact_notes.isVisible()) {
            contact_notes_share_info.beVisibleIf(selectShareActive)
            contact_notes_share_icon.beVisibleIf(selectShareActive)
        } else {
            contact_notes_share_info.beGone()
            contact_notes_share_icon.beGone()
        }
    } // ViewContactActivity.setupNoteShareIcons()

    // *****************************************************************

    private fun setupRingtone(viewContact: Contact) {
        if ((showFields and SHOW_RINGTONE_FIELD) != 0) {
            contact_ringtone_image.beVisible()
            contact_ringtone.beVisible()

            val ringtone = viewContact.ringtone
            if (ringtone?.isEmpty() == true) {
                contact_ringtone.text = getString(R.string.no_sound)
            } else if ((ringtone?.isNotEmpty() == true) && (ringtone != getDefaultRingtoneUri().toString())) {
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
                    val currentRingtone = viewContact.ringtone ?: getDefaultAlarmSound(RingtoneManager.TYPE_RINGTONE).uri
                    SelectAlarmSoundDialog(this@ViewContactActivity,
                        currentRingtone,
                        AudioManager.STREAM_RING,
                        PICK_RINGTONE_INTENT_ID,
                        RingtoneManager.TYPE_RINGTONE,
                        true,
                        onAlarmPicked = {
                            val ringtoneFilename = it?.title
                            if (!isSoundOfSilence(ringtoneFilename))
                                contact_ringtone.text = ringtoneFilename
                            else
                                contact_ringtone.text = getString(R.string.no_sound)
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
    } // ViewContactActivity.setupRingtone()

    // *****************************************************************

    private fun setupOrganization(viewContact: Contact, showShareCheckbox: Boolean) {
        val organization = viewContact.organization
        if ((organization.company.isNotEmpty() || organization.jobTitle.isNotEmpty()) &&
            ((showFields and SHOW_ORGANIZATION_FIELD) != 0)) {
            if (false) {
                contact_organization_company.text = organization.company
                contact_organization_job_position.text = organization.jobTitle
                contact_organization_image.beGoneIf(organization.isEmpty())
                contact_organization_company.beGoneIf(organization.company.isEmpty())
                contact_organization_job_position.beGoneIf(organization.jobTitle.isEmpty())
                contact_organization_company.copyOnLongClick(contact_organization_company.value)
                contact_organization_job_position.copyOnLongClick(contact_organization_job_position.value)
            }
            else {
                val company_position: String = (
                    if (organization.company.isNotEmpty()) {
                        if (organization.jobTitle.isNotEmpty())
                            organization.company + "\n" + organization.jobTitle
                        else
                            organization.company
                    }
                    else
                        organization.jobTitle
                    )
                contact_organization_company.text = company_position
                contact_organization_job_position.text = ""
                contact_organization_image.beGoneIf(company_position.isEmpty())
                contact_organization_company.beGoneIf(company_position.isEmpty())
                contact_organization_job_position.beGone()
                contact_organization_company.copyOnLongClick(company_position)
            }

            contact_organization_share_info.isChecked = state.shareOrganization
            contact_organization_share_info.beVisibleIf(showShareCheckbox)
            contact_organization_share_icon.beVisibleIf(showShareCheckbox)

            // if (organization.company.isEmpty() && organization.jobTitle.isNotEmpty()) {
            //     (contact_organization_image.layoutParams as RelativeLayout.LayoutParams).addRule(RelativeLayout.ALIGN_TOP, contact_organization_job_position.id)
            // }
        } else {
            contact_organization_image.beGone()
            contact_organization_company.beGone()
            contact_organization_job_position.beGone()
            contact_organization_share_info.isChecked = true
            contact_organization_share_info.beGone()
            contact_organization_share_icon.beGone()
        }
    } // ViewContactActivity.setupOrganization()

    // *****************************************************************

    private fun setupOrganizationShareIcons(selectShareActive: Boolean) {
        if (contact_organization_company.isVisible()) {
            contact_organization_share_info.beVisibleIf(selectShareActive)
            contact_organization_share_icon.beVisibleIf(selectShareActive)
        } else {
            contact_organization_share_info.beGone()
            contact_organization_share_icon.beGone()
        }
    } // ViewContactActivity.setupOrganizationShareIcons()

    // *****************************************************************

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
    } // ViewContactActivity.showSocialActions()

    // *****************************************************************

    override fun customRingtoneSelected(ringtonePath: String) {
        val ringtoneFilename = ringtonePath.getFilenameFromPath()
        if (!isSoundOfSilence(ringtoneFilename))
            contact_ringtone.text = ringtoneFilename
        else
            contact_ringtone.text = getString(R.string.no_sound)
        ringtoneUpdated(ringtonePath)
    } // ViewContactActivity.customRingtoneSelected()

    // *****************************************************************

    override fun systemRingtoneSelected(uri: Uri?) {
        val contactRingtone = RingtoneManager.getRingtone(this, uri)
        val ringtoneFilename = contactRingtone.getTitle(this)
        if ((uri != null) && !isSoundOfSilence(ringtoneFilename))
            contact_ringtone.text = ringtoneFilename
        else
            contact_ringtone.text = getString(R.string.no_sound)
        ringtoneUpdated(uri?.toString() ?: "")
    } // ViewContactActivity.systemRingtoneSelected()

    // *****************************************************************

    private fun ringtoneUpdated(path: String?) {
        contact!!.ringtone = path

        ensureBackgroundThread {
            if (contact!!.isPrivate()) {
                LocalContactsHelper(this).updateRingtone(contact!!.contactId, path ?: "")
            } else {
                ContactsHelper(this).updateRingtone(contact!!.contactId.toString(), path ?: "")
            }
        }
    } // ViewContactActivity.ringtoneUpdated()

    // *****************************************************************

    private fun fillContactValues(getSelectedFieldsOnly: Boolean, getHiddenFields: Boolean): Contact {
        val filledPhoneNumbers = getSelectedPhoneNumbers(getSelectedFieldsOnly, getHiddenFields)
        val filledEmails = getSelectedEmails(getSelectedFieldsOnly, getHiddenFields)
        val filledAddresses = getSelectedAddresses(getSelectedFieldsOnly, getHiddenFields)
        val filledIMs = getSelectedIMs(getSelectedFieldsOnly, getHiddenFields)
        val filledEvents = getSelectedEvents(getSelectedFieldsOnly, getHiddenFields)
        val filledWebsites = getSelectedWebsites(getSelectedFieldsOnly, getHiddenFields)
        val filledRelations = getSelectedRelations(getSelectedFieldsOnly, getHiddenFields)

        val selectedGroups = getSelectedGroups(getSelectedFieldsOnly, getHiddenFields)

        val filledOrganization: Organization
        if ((getHiddenFields || contact_organization_company.isVisible) &&
            (!getSelectedFieldsOnly || contact_organization_share_info.isChecked)) {
            filledOrganization = fullContact!!.organization.deepCopy()
        } else {
            filledOrganization = Organization.getEmptyOrganization()
        }

        val newContact = fullContact!!.copy(
            // name = newName,                  // Always share the full name/nicknames
            // nicknames = filledNicknames,
            photoUri =  if (!getSelectedFieldsOnly || contact_photo_share_info.isChecked) currentContactPhotoPath else "",
            phoneNumbers = filledPhoneNumbers,
            emails = filledEmails,
            addresses = filledAddresses,
            IMs = filledIMs,
            events = filledEvents,
            starred = contact!!.starred,
            notes = if (!getSelectedFieldsOnly || contact_notes_share_info.isChecked) fullContact!!.notes else "",
            organization = filledOrganization,
            websites = filledWebsites,
            relations = filledRelations,
            groups = selectedGroups
        )
        return newContact
    } // ViewContactActivity.fillContactValues()

    // *****************************************************************

    private fun getSelectedPhoneNumbers(getSelectedFieldsOnly: Boolean, getHiddenFields: Boolean): ArrayList<PhoneNumber> {
        val phoneNumbers = ArrayList<PhoneNumber>()
        if (getHiddenFields || contact_numbers_holder.isVisible) {
            val numbersCount = contact_numbers_holder.childCount
            for (i in 0 until numbersCount) {
                val numberHolder = contact_numbers_holder.getChildAt(i)
                if (!getSelectedFieldsOnly || numberHolder.contact_number_share_info.isChecked) {
                    phoneNumbers.add(fullContact!!.phoneNumbers[i])
                } /* if (isChecked) */
            }
        }
        return phoneNumbers
    } // ViewContactActivity.getSelectedPhoneNumbers()

    // *****************************************************************

    private fun getSelectedEmails(getSelectedFieldsOnly: Boolean, getHiddenFields: Boolean): ArrayList<Email> {
        val emails = ArrayList<Email>()
        if (getHiddenFields || contact_emails_holder.isVisible) {
            val emailsCount = contact_emails_holder.childCount
            for (i in 0 until emailsCount) {
                val emailHolder = contact_emails_holder.getChildAt(i)
                if (!getSelectedFieldsOnly || emailHolder.contact_email_share_info.isChecked) {
                    emails.add(fullContact!!.emails[i])
                } /* if (isChecked) */
            }
        }
        return emails
    } // ViewContactActivity.getSelectedEmails()

    // *****************************************************************

    private fun getSelectedAddresses(getSelectedFieldsOnly: Boolean, getHiddenFields: Boolean): ArrayList<Address> {
        val addresses = ArrayList<Address>()
        if (getHiddenFields || contact_addresses_holder.isVisible) {
            val addressesCount = contact_addresses_holder.childCount
            for (i in 0 until addressesCount) {
                val addressHolder = contact_addresses_holder.getChildAt(i)
                if (!getSelectedFieldsOnly || addressHolder.contact_address_share_info.isChecked) {
                    addresses.add(fullContact!!.addresses[i])
                } /* if (isChecked) */
            }
        }
        return addresses
    } // ViewContactActivity.getSelectedAddresses()

    // *****************************************************************

    private fun getSelectedIMs(getSelectedFieldsOnly: Boolean, getHiddenFields: Boolean): ArrayList<IM> {
        val IMs = ArrayList<IM>()
        if (getHiddenFields || contact_ims_holder.isVisible) {
            val IMsCount = contact_ims_holder.childCount
            for (i in 0 until IMsCount) {
                val IMsHolder = contact_ims_holder.getChildAt(i)
                if (!getSelectedFieldsOnly || IMsHolder.contact_im_share_info.isChecked) {
                    IMs.add(fullContact!!.IMs[i])
                } /* if (isChecked) */
            }
        }
        return IMs
    } // ViewContactActivity.getSelectedIMs()

    // *****************************************************************

    private fun getSelectedEvents(getSelectedFieldsOnly: Boolean, getHiddenFields: Boolean): ArrayList<Event> {
        val events = ArrayList<Event>()
        if (getHiddenFields || contact_events_holder.isVisible) {
            val unknown = getString(R.string.unknown)
            val eventsCount = contact_events_holder.childCount
            for (i in 0 until eventsCount) {
                val eventHolder = contact_events_holder.getChildAt(i)
                if (!getSelectedFieldsOnly || eventHolder.contact_event_share_info.isChecked) {
                    events.add(fullContact!!.events[i])
                } /* if (isChecked) */
            }
        }
        return events
    } // ViewContactActivity.getSelectedEvents()

    // *****************************************************************

    private fun getSelectedWebsites(getSelectedFieldsOnly: Boolean, getHiddenFields: Boolean): ArrayList<ContactWebsite> {
        val websites = ArrayList<ContactWebsite>()
        if (getHiddenFields || contact_websites_holder.isVisible) {
            val websitesCount = contact_websites_holder.childCount
            for (i in 0 until websitesCount) {
                val websiteHolder = contact_websites_holder.getChildAt(i)
                if (!getSelectedFieldsOnly || websiteHolder.contact_website_share_info.isChecked) {
                    websites.add(fullContact!!.websites[i])
                } /* if (isChecked) */
            }
        }
        return websites
    } // ViewContactActivity.getSelectedWebsites()

    // *****************************************************************

    private fun getSelectedRelations(getSelectedFieldsOnly: Boolean, getHiddenFields: Boolean): ArrayList<ContactRelation> {
        val relations = ArrayList<ContactRelation>()
        if (getHiddenFields || contact_relations_holder.isVisible) {
            val relationsCount = contact_relations_holder.childCount
            for (i in 0 until relationsCount) {
                val relationHolder = contact_relations_holder.getChildAt(i)
                if (!getSelectedFieldsOnly || relationHolder.contact_relation_share_info.isChecked) {
                    relations.add(fullContact!!.relations[i])
                } /* if (isChecked) */
            }
        }
        return relations
    } // ViewContactActivity.getSelectedRelations()

    // *****************************************************************

    private fun getSelectedGroups(getSelectedFieldsOnly: Boolean, getHiddenFields: Boolean): ArrayList<Group> {
        val selectedGroups = ArrayList<Group>()
        val NoGroupsMsg = getString(R.string.no_groups)
        if (getHiddenFields || contact_groups_holder.isVisible) {
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
    } // ViewContactActivity.getSelectedGroups()

    // *****************************************************************

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
    } // ViewContactActivity.getDuplicateContacts()

    // *****************************************************************

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
    } // ViewContactActivity.deleteContactFromAllSources()

    // *****************************************************************

    private fun getStarDrawable(on: Boolean) = resources.getDrawable(if (on) R.drawable.ic_star_vector else R.drawable.ic_star_outline_vector)

    private fun hideBigContactPhoto() {
        contact_photo_big.animate().alpha(0f).withEndAction { contact_photo_big.beGone() }.start()
    } // ViewContactActivity.hideBigContactPhoto()

    // *****************************************************************

    private fun View.copyOnLongClick(value: String) {
        setOnLongClickListener {
            copyToClipboard(value)
            true
        }
    } // ViewContactActivity.copyOnLongClick()
} // class ViewContactActivity

/* *********************************************************************
 *                        ViewContactActivity.kt                       *
 ***********************************************************************/
