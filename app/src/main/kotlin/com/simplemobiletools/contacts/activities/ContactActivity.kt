package com.simplemobiletools.contacts.activities

import android.app.DatePickerDialog
import android.content.ClipData
import android.content.Intent
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.provider.ContactsContract
import android.provider.MediaStore
import android.support.v4.content.FileProvider
import android.view.Menu
import android.view.MenuItem
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.ImageView
import android.widget.TextView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.Target
import com.simplemobiletools.commons.dialogs.ConfirmationDialog
import com.simplemobiletools.commons.dialogs.RadioGroupDialog
import com.simplemobiletools.commons.extensions.*
import com.simplemobiletools.commons.helpers.PERMISSION_READ_CONTACTS
import com.simplemobiletools.commons.helpers.PERMISSION_WRITE_CONTACTS
import com.simplemobiletools.commons.helpers.getDateFormats
import com.simplemobiletools.commons.models.RadioItem
import com.simplemobiletools.contacts.BuildConfig
import com.simplemobiletools.contacts.R
import com.simplemobiletools.contacts.extensions.*
import com.simplemobiletools.contacts.helpers.*
import com.simplemobiletools.contacts.models.Contact
import com.simplemobiletools.contacts.models.Email
import com.simplemobiletools.contacts.models.Event
import com.simplemobiletools.contacts.models.PhoneNumber
import kotlinx.android.synthetic.main.activity_contact.*
import kotlinx.android.synthetic.main.item_email.view.*
import kotlinx.android.synthetic.main.item_event.view.*
import kotlinx.android.synthetic.main.item_phone_number.view.*
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat
import java.io.File
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*

class ContactActivity : SimpleActivity() {
    private val DEFAULT_EMAIL_TYPE = ContactsContract.CommonDataKinds.Email.TYPE_HOME
    private val DEFAULT_PHONE_NUMBER_TYPE = ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE
    private val DEFAULT_EVENT_TYPE = ContactsContract.CommonDataKinds.Event.TYPE_BIRTHDAY

    private val INTENT_TAKE_PHOTO = 1
    private val INTENT_CHOOSE_PHOTO = 2
    private val INTENT_CROP_PHOTO = 3

    private val TAKE_PHOTO = 1
    private val CHOOSE_PHOTO = 2
    private val REMOVE_PHOTO = 3

    private val KEY_PHONE = "phone"

    private var wasActivityInitialized = false
    private var currentContactPhotoPath = ""
    private var lastPhotoIntentUri: Uri? = null
    private var contact: Contact? = null
    private var isSaving = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_contact)
        supportActionBar?.setHomeAsUpIndicator(R.drawable.ic_cross)

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
    }

    private fun initContact() {
        var contactId = intent.getIntExtra(CONTACT_ID, 0)
        val action = intent.action
        if (contactId == 0 && (action == ContactsContract.QuickContact.ACTION_QUICK_CONTACT ||
                action == Intent.ACTION_VIEW ||
                action == Intent.ACTION_EDIT)) {
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
            contact = ContactsHelper(this).getContactWithId(contactId)
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

        if (contact!!.id == 0 && intent.action == Intent.ACTION_INSERT_OR_EDIT && intent.extras?.containsKey(KEY_PHONE) == true) {
            val phoneNumber = intent.getStringExtra(KEY_PHONE)
            contact!!.phoneNumbers.add(PhoneNumber(phoneNumber, DEFAULT_PHONE_NUMBER_TYPE))
            setupPhoneNumbers()
        }

        setupTypePickers()
        contact_send_sms.beVisibleIf(contact!!.phoneNumbers.isNotEmpty())
        contact_start_call.beVisibleIf(contact!!.phoneNumbers.isNotEmpty())
        contact_send_email.beVisibleIf(contact!!.emails.isNotEmpty())

        contact_photo.background = ColorDrawable(config.primaryColor)

        if (contact!!.photoUri.isEmpty()) {
            showPhotoPlaceholder()
        } else {
            updateContactPhoto(contact!!.photoUri)
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

        val adjustedPrimaryColor = getAdjustedPrimaryColor()
        contact_number_add_new.applyColorFilter(adjustedPrimaryColor)
        contact_number_add_new.background.applyColorFilter(textColor)
        contact_email_add_new.applyColorFilter(adjustedPrimaryColor)
        contact_email_add_new.background.applyColorFilter(textColor)
        contact_event_add_new.applyColorFilter(adjustedPrimaryColor)
        contact_event_add_new.background.applyColorFilter(textColor)

        contact_photo.setOnClickListener { trySetPhoto() }
        contact_send_sms.setOnClickListener { trySendSMS() }
        contact_start_call.setOnClickListener { tryStartCall(contact!!) }
        contact_send_email.setOnClickListener { trySendEmail() }
        contact_number_add_new.setOnClickListener { addNewPhoneNumberField() }
        contact_email_add_new.setOnClickListener { addNewEmailField() }
        contact_event_add_new.setOnClickListener { addNewEventField() }

        updateTextColors(contact_scrollview)
        wasActivityInitialized = true
        invalidateOptionsMenu()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_contact, menu)
        if (wasActivityInitialized) {
            menu.findItem(R.id.delete).isVisible = contact?.id != 0
        }
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.save -> saveContact()
            R.id.delete -> deleteContact()
            else -> return super.onOptionsItemSelected(item)
        }
        return true
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, resultData: Intent?) {
        super.onActivityResult(requestCode, resultCode, resultData)
        if (resultCode == RESULT_OK) {
            when (requestCode) {
                INTENT_TAKE_PHOTO, INTENT_CHOOSE_PHOTO -> startCropPhotoIntent(lastPhotoIntentUri!!)
                INTENT_CROP_PHOTO -> updateContactPhoto(lastPhotoIntentUri.toString())
            }
        }
    }

    private fun startCropPhotoIntent(uri: Uri) {
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
            startActivityForResult(this, INTENT_CROP_PHOTO)
        }
    }

    private fun setupEditContact() {
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN)
        supportActionBar?.title = resources.getString(R.string.edit_contact)
        contact_first_name.setText(contact!!.firstName)
        contact_middle_name.setText(contact!!.middleName)
        contact_surname.setText(contact!!.surname)
        contact_source.text = contact!!.source

        setupPhoneNumbers()
        setupEmails()
        setupEvents()
    }

    private fun setupPhoneNumbers() {
        contact!!.phoneNumbers.forEachIndexed { index, number ->
            var numberHolder = contact_numbers_holder.getChildAt(index)
            if (numberHolder == null) {
                numberHolder = layoutInflater.inflate(R.layout.item_phone_number, contact_numbers_holder, false)
                contact_numbers_holder.addView(numberHolder)
            }

            (numberHolder as? ViewGroup)?.apply {
                contact_number.setText(number.value)
                setupPhoneNumberTypePicker(contact_number_type, number.type)
            }
        }
    }

    private fun setupEmails() {
        contact!!.emails.forEachIndexed { index, email ->
            var emailHolder = contact_emails_holder.getChildAt(index)
            if (emailHolder == null) {
                emailHolder = layoutInflater.inflate(R.layout.item_email, contact_emails_holder, false)
                contact_emails_holder.addView(emailHolder)
            }

            (emailHolder as? ViewGroup)?.apply {
                contact_email.setText(email.value)
                setupEmailTypePicker(contact_email_type, email.type)
            }
        }
    }

    private fun setupEvents() {
        contact!!.events.forEachIndexed { index, event ->
            var eventHolder = contact_events_holder.getChildAt(index)
            if (eventHolder == null) {
                eventHolder = layoutInflater.inflate(R.layout.item_event, contact_events_holder, false)
                contact_events_holder.addView(eventHolder)
            }

            (eventHolder as? ViewGroup)?.apply {
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

    private fun setupNewContact() {
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE)
        supportActionBar?.title = resources.getString(R.string.new_contact)
        contact = Contact(0, "", "", "", "", ArrayList(), ArrayList(), ArrayList(), "")
        contact_source.text = config.lastUsedContactSource
        contact_source.setOnClickListener { showAccountSourcePicker() }
    }

    private fun showPhotoPlaceholder() {
        val placeholder = resources.getColoredBitmap(R.drawable.ic_person, config.primaryColor.getContrastColor())
        val padding = resources.getDimension(R.dimen.activity_margin).toInt()
        contact_photo.setPadding(padding, padding, padding, padding)
        contact_photo.setImageBitmap(placeholder)
        currentContactPhotoPath = ""
    }

    private fun updateContactPhoto(path: String) {
        currentContactPhotoPath = path
        val options = RequestOptions()
                .diskCacheStrategy(DiskCacheStrategy.RESOURCE)
                .centerCrop()

        Glide.with(this)
                .load(path)
                .transition(DrawableTransitionOptions.withCrossFade())
                .apply(options)
                .listener(object : RequestListener<Drawable> {
                    override fun onResourceReady(resource: Drawable?, model: Any?, target: Target<Drawable>?, dataSource: DataSource?, isFirstResource: Boolean): Boolean {
                        contact_photo.setPadding(0, 0, 0, 0)
                        return false
                    }

                    override fun onLoadFailed(e: GlideException?, model: Any?, target: Target<Drawable>?, isFirstResource: Boolean): Boolean {
                        showPhotoPlaceholder()
                        return true
                    }
                }).into(contact_photo)
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

    private fun resetContactEvent(contactEvent: TextView, removeContactEventButton: ImageView) {
        contactEvent.apply {
            text = getString(R.string.unknown)
            tag = ""
            alpha = 0.5f
        }
        removeContactEventButton.beGone()
    }

    private fun getDateTime(dateString: String, viewToUpdate: TextView? = null): DateTime {
        val dateFormats = getDateFormats()
        var date = DateTime()
        for (format in dateFormats) {
            try {
                date = DateTime.parse(dateString, DateTimeFormat.forPattern(format))

                val formatter = DateFormat.getDateInstance(DateFormat.MEDIUM, Locale.getDefault())
                var localPattern = (formatter as SimpleDateFormat).toLocalizedPattern()

                val hasYear = format.contains("y")
                if (!hasYear) {
                    localPattern = localPattern.replace("y", "").trim()
                    date = date.withYear(DateTime().year)
                }

                val formattedString = date.toString(localPattern)
                viewToUpdate?.text = formattedString
                break
            } catch (ignored: Exception) {
            }
        }
        return date
    }

    private fun showNumberTypePicker(numberTypeField: TextView) {
        val items = arrayListOf(
                RadioItem(ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE, getString(R.string.mobile)),
                RadioItem(ContactsContract.CommonDataKinds.Phone.TYPE_HOME, getString(R.string.home)),
                RadioItem(ContactsContract.CommonDataKinds.Phone.TYPE_WORK, getString(R.string.work)),
                RadioItem(ContactsContract.CommonDataKinds.Phone.TYPE_MAIN, getString(R.string.main_number)),
                RadioItem(ContactsContract.CommonDataKinds.Phone.TYPE_FAX_WORK, getString(R.string.work_fax)),
                RadioItem(ContactsContract.CommonDataKinds.Phone.TYPE_FAX_HOME, getString(R.string.home_fax)),
                RadioItem(ContactsContract.CommonDataKinds.Phone.TYPE_PAGER, getString(R.string.pager)),
                RadioItem(ContactsContract.CommonDataKinds.Phone.TYPE_OTHER, getString(R.string.other))
        )

        val currentNumberTypeId = getPhoneNumberTypeId(numberTypeField.value)
        RadioGroupDialog(this, items, currentNumberTypeId) {
            numberTypeField.setText(getPhoneNumberTextId(it as Int))
        }
    }

    private fun showEmailTypePicker(emailTypeField: TextView) {
        val items = arrayListOf(
                RadioItem(ContactsContract.CommonDataKinds.Email.TYPE_HOME, getString(R.string.home)),
                RadioItem(ContactsContract.CommonDataKinds.Email.TYPE_WORK, getString(R.string.work)),
                RadioItem(ContactsContract.CommonDataKinds.Email.TYPE_MOBILE, getString(R.string.mobile)),
                RadioItem(ContactsContract.CommonDataKinds.Email.TYPE_OTHER, getString(R.string.other))
        )

        val currentEmailTypeId = getEmailTypeId(emailTypeField.value)
        RadioGroupDialog(this, items, currentEmailTypeId) {
            emailTypeField.setText(getEmailTextId(it as Int))
        }
    }

    private fun showEventTypePicker(eventTypeField: TextView) {
        val items = arrayListOf(
                RadioItem(ContactsContract.CommonDataKinds.Event.TYPE_BIRTHDAY, getString(R.string.birthday)),
                RadioItem(ContactsContract.CommonDataKinds.Event.TYPE_ANNIVERSARY, getString(R.string.anniversary)),
                RadioItem(ContactsContract.CommonDataKinds.Event.TYPE_OTHER, getString(R.string.other))
        )

        val currentEventTypeId = getEventTypeId(eventTypeField.value)
        RadioGroupDialog(this, items, currentEventTypeId) {
            eventTypeField.setText(getEventTextId(it as Int))
        }
    }

    private fun saveContact() {
        if (isSaving) {
            return
        }

        contact!!.apply {
            val oldPhotoUri = photoUri

            firstName = contact_first_name.value
            middleName = contact_middle_name.value
            surname = contact_surname.value
            photoUri = currentContactPhotoPath
            phoneNumbers = getFilledPhoneNumbers()
            emails = getFilledEmails()
            events = getFilledEvents()
            source = contact_source.value

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

    private fun insertNewContact() {
        isSaving = true
        if (ContactsHelper(this@ContactActivity).insertContact(contact!!)) {
            finish()
        } else {
            toast(R.string.unknown_error_occurred)
        }
    }

    private fun updateContact(photoUpdateStatus: Int) {
        isSaving = true
        if (ContactsHelper(this@ContactActivity).updateContact(contact!!, photoUpdateStatus)) {
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
        layoutInflater.inflate(R.layout.item_phone_number, contact_numbers_holder, false).apply {
            updateTextColors(this as ViewGroup)
            setupPhoneNumberTypePicker(contact_number_type)
            contact_numbers_holder.addView(this)
            contact_numbers_holder.onGlobalLayout {
                contact_number.requestFocus()
                showKeyboard(contact_number)
            }
        }
    }

    private fun addNewEmailField() {
        layoutInflater.inflate(R.layout.item_email, contact_emails_holder, false).apply {
            updateTextColors(this as ViewGroup)
            setupEmailTypePicker(contact_email_type)
            contact_emails_holder.addView(this)
            contact_emails_holder.onGlobalLayout {
                contact_email.requestFocus()
                showKeyboard(contact_email)
            }
        }
    }

    private fun addNewEventField() {
        layoutInflater.inflate(R.layout.item_event, contact_events_holder, false).apply {
            updateTextColors(this as ViewGroup)
            setupEventTypePicker(this)
            contact_events_holder.addView(this)
        }
    }

    private fun deleteContact() {
        ConfirmationDialog(this) {
            ContactsHelper(this).deleteContact(contact!!)
            finish()
        }
    }

    private fun showAccountSourcePicker() {
        ContactsHelper(this).getContactSources {
            val items = ArrayList<RadioItem>()
            val sources = it
            val currentSource = contact_source.value
            var currentSourceIndex = -1
            sources.forEachIndexed { index, account ->
                items.add(RadioItem(index, account))
                if (account == currentSource) {
                    currentSourceIndex = index
                }
            }

            runOnUiThread {
                RadioGroupDialog(this, items, currentSourceIndex) {
                    contact_source.text = sources[it as Int]
                }
            }
        }
    }

    private fun trySetPhoto() {
        val items = arrayListOf(
                RadioItem(TAKE_PHOTO, getString(R.string.take_photo)),
                RadioItem(CHOOSE_PHOTO, getString(R.string.choose_photo))
        )

        if (currentContactPhotoPath.isNotEmpty()) {
            items.add(RadioItem(REMOVE_PHOTO, getString(R.string.remove_photo)))
        }

        RadioGroupDialog(this, items) {
            when (it as Int) {
                TAKE_PHOTO -> startTakePhotoIntent()
                CHOOSE_PHOTO -> startChoosePhotoIntent()
                else -> showPhotoPlaceholder()
            }
        }
    }

    private fun trySendSMS() {
        val numbers = contact!!.phoneNumbers
        if (numbers.size == 1) {
            sendSMSIntent(numbers.first().value)
        } else if (numbers.size > 1) {
            val items = ArrayList<RadioItem>()
            numbers.forEachIndexed { index, phoneNumber ->
                items.add(RadioItem(index, phoneNumber.value, phoneNumber.value))
            }

            RadioGroupDialog(this, items) {
                sendSMSIntent(it as String)
            }
        }
    }

    private fun trySendEmail() {
        val emails = contact!!.emails
        if (emails.size == 1) {
            sendEmailIntent(emails.first().value)
        } else if (emails.size > 1) {
            val items = ArrayList<RadioItem>()
            emails.forEachIndexed { index, email ->
                items.add(RadioItem(index, email.value, email.value))
            }

            RadioGroupDialog(this, items) {
                sendEmailIntent(it as String)
            }
        }
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

    private fun getCachePhotoUri(): Uri {
        val imagesFolder = File(cacheDir, "my_cache")
        if (!imagesFolder.exists()) {
            imagesFolder.mkdirs()
        }

        val file = File(imagesFolder, "Photo_${System.currentTimeMillis()}.jpg")
        file.createNewFile()
        return FileProvider.getUriForFile(this, "${BuildConfig.APPLICATION_ID}.provider", file)
    }

    private fun getPhoneNumberTextId(type: Int) = when (type) {
        ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE -> R.string.mobile
        ContactsContract.CommonDataKinds.Phone.TYPE_HOME -> R.string.home
        ContactsContract.CommonDataKinds.Phone.TYPE_WORK -> R.string.work
        ContactsContract.CommonDataKinds.Phone.TYPE_MAIN -> R.string.main_number
        ContactsContract.CommonDataKinds.Phone.TYPE_FAX_WORK -> R.string.work_fax
        ContactsContract.CommonDataKinds.Phone.TYPE_FAX_HOME -> R.string.home_fax
        ContactsContract.CommonDataKinds.Phone.TYPE_PAGER -> R.string.pager
        else -> R.string.other
    }

    private fun getPhoneNumberTypeId(value: String) = when (value) {
        getString(R.string.mobile) -> ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE
        getString(R.string.home) -> ContactsContract.CommonDataKinds.Phone.TYPE_HOME
        getString(R.string.work) -> ContactsContract.CommonDataKinds.Phone.TYPE_WORK
        getString(R.string.main_number) -> ContactsContract.CommonDataKinds.Phone.TYPE_MAIN
        getString(R.string.work_fax) -> ContactsContract.CommonDataKinds.Phone.TYPE_FAX_WORK
        getString(R.string.home_fax) -> ContactsContract.CommonDataKinds.Phone.TYPE_FAX_HOME
        getString(R.string.pager) -> ContactsContract.CommonDataKinds.Phone.TYPE_PAGER
        else -> ContactsContract.CommonDataKinds.Phone.TYPE_OTHER
    }

    private fun getEmailTextId(type: Int) = when (type) {
        ContactsContract.CommonDataKinds.Email.TYPE_HOME -> R.string.home
        ContactsContract.CommonDataKinds.Email.TYPE_WORK -> R.string.work
        ContactsContract.CommonDataKinds.Email.TYPE_MOBILE -> R.string.mobile
        else -> R.string.other
    }

    private fun getEmailTypeId(value: String) = when (value) {
        getString(R.string.home) -> ContactsContract.CommonDataKinds.Email.TYPE_HOME
        getString(R.string.work) -> ContactsContract.CommonDataKinds.Email.TYPE_WORK
        getString(R.string.mobile) -> ContactsContract.CommonDataKinds.Email.TYPE_MOBILE
        else -> ContactsContract.CommonDataKinds.Email.TYPE_OTHER
    }

    private fun getEventTextId(type: Int) = when (type) {
        ContactsContract.CommonDataKinds.Event.TYPE_BIRTHDAY -> R.string.birthday
        ContactsContract.CommonDataKinds.Event.TYPE_ANNIVERSARY -> R.string.anniversary
        else -> R.string.other
    }

    private fun getEventTypeId(value: String) = when (value) {
        getString(R.string.birthday) -> ContactsContract.CommonDataKinds.Event.TYPE_BIRTHDAY
        getString(R.string.anniversary) -> ContactsContract.CommonDataKinds.Event.TYPE_ANNIVERSARY
        else -> ContactsContract.CommonDataKinds.Event.TYPE_OTHER
    }
}
