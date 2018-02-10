package com.simplemobiletools.contacts.activities

import android.content.Intent
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.provider.ContactsContract
import android.provider.ContactsContract.CommonDataKinds
import android.view.Menu
import android.view.MenuItem
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.RelativeLayout
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
import com.simplemobiletools.commons.helpers.getDateFormats
import com.simplemobiletools.commons.models.RadioItem
import com.simplemobiletools.contacts.R
import com.simplemobiletools.contacts.extensions.*
import com.simplemobiletools.contacts.helpers.*
import com.simplemobiletools.contacts.models.Contact
import kotlinx.android.synthetic.main.activity_view_contact.*
import kotlinx.android.synthetic.main.item_event.view.*
import kotlinx.android.synthetic.main.item_view_email.view.*
import kotlinx.android.synthetic.main.item_view_phone_number.view.*
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*

class ViewContactActivity : SimpleActivity() {
    private var currentContactPhotoPath = ""
    private var contact: Contact? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_view_contact)

        handlePermission(PERMISSION_READ_CONTACTS) {
            if (it) {
                initContact()
            } else {
                toast(R.string.no_contacts_permission)
                finish()
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_view_contact, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.edit -> editContact()
            R.id.share -> shareContact()
            R.id.delete -> deleteContact()
            else -> return super.onOptionsItemSelected(item)
        }
        return true
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
            contact = ContactsHelper(this).getContactWithId(contactId)
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
        val phoneNumbers = contact!!.phoneNumbers
        phoneNumbers.forEachIndexed { index, number ->
            var numberHolder = contact_numbers_holder.getChildAt(index)
            if (numberHolder == null) {
                numberHolder = layoutInflater.inflate(R.layout.item_view_phone_number, contact_numbers_holder, false)
                contact_numbers_holder.addView(numberHolder)
            }

            numberHolder?.apply {
                contact_number.text = number.value
                setupPhoneNumberTypePicker(contact_number_type, number.type)
            }
        }

        contact_number_image.beVisibleIf(phoneNumbers.isNotEmpty())
        contact_numbers_holder.beVisibleIf(phoneNumbers.isNotEmpty())
    }

    private fun setupEmails() {
        val emails = contact!!.emails
        emails.forEachIndexed { index, email ->
            var emailHolder = contact_emails_holder.getChildAt(index)
            if (emailHolder == null) {
                emailHolder = layoutInflater.inflate(R.layout.item_view_email, contact_emails_holder, false)
                contact_emails_holder.addView(emailHolder)
            }

            emailHolder?.apply {
                contact_email.text = email.value
                setupEmailTypePicker(contact_email_type, email.type)
            }
        }

        contact_email_image.beVisibleIf(emails.isNotEmpty())
        contact_emails_holder.beVisibleIf(emails.isNotEmpty())
    }

    private fun setupEvents() {
        val events = contact!!.events
        events.forEachIndexed { index, event ->
            var eventHolder = contact_events_holder.getChildAt(index)
            if (eventHolder == null) {
                eventHolder = layoutInflater.inflate(R.layout.item_event, contact_events_holder, false)
                contact_events_holder.addView(eventHolder)
            }

            (eventHolder as? ViewGroup)?.apply {
                contact_event.apply {
                    getDateTime(event.value, this)
                    tag = event.value
                    alpha = 1f
                }

                setupEventTypePicker(this, event.type)
                contact_event_remove.beGone()
            }
        }

        contact_event_image.beVisibleIf(events.isNotEmpty())
        contact_events_holder.beVisibleIf(events.isNotEmpty())
    }

    private fun editContact() {
        Intent(applicationContext, EditContactActivity::class.java).apply {
            putExtra(CONTACT_ID, contact!!.id)
            startActivity(this)
        }
    }

    private fun shareContact() {
        shareContacts(arrayListOf(contact!!))
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
        numberTypeField.setText(getPhoneNumberTextId(type))
    }

    private fun setupEmailTypePicker(emailTypeField: TextView, type: Int = DEFAULT_EMAIL_TYPE) {
        emailTypeField.setText(getEmailTextId(type))
    }

    private fun setupEventTypePicker(eventHolder: ViewGroup, type: Int = DEFAULT_EVENT_TYPE) {
        eventHolder.contact_event_type.setText(getEventTextId(type))
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

    private fun deleteContact() {
        ConfirmationDialog(this) {
            ContactsHelper(this).deleteContact(contact!!)
            finish()
        }
    }

    private fun getStarDrawable(on: Boolean) = resources.getDrawable(if (on) R.drawable.ic_star_on_big else R.drawable.ic_star_off_big)

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

    private fun getPhoneNumberTextId(type: Int) = when (type) {
        CommonDataKinds.Phone.TYPE_MOBILE -> R.string.mobile
        CommonDataKinds.Phone.TYPE_HOME -> R.string.home
        CommonDataKinds.Phone.TYPE_WORK -> R.string.work
        CommonDataKinds.Phone.TYPE_MAIN -> R.string.main_number
        CommonDataKinds.Phone.TYPE_FAX_WORK -> R.string.work_fax
        CommonDataKinds.Phone.TYPE_FAX_HOME -> R.string.home_fax
        CommonDataKinds.Phone.TYPE_PAGER -> R.string.pager
        else -> R.string.other
    }

    private fun getEmailTextId(type: Int) = when (type) {
        CommonDataKinds.Email.TYPE_HOME -> R.string.home
        CommonDataKinds.Email.TYPE_WORK -> R.string.work
        CommonDataKinds.Email.TYPE_MOBILE -> R.string.mobile
        else -> R.string.other
    }

    private fun getEventTextId(type: Int) = when (type) {
        CommonDataKinds.Event.TYPE_BIRTHDAY -> R.string.birthday
        CommonDataKinds.Event.TYPE_ANNIVERSARY -> R.string.anniversary
        else -> R.string.other
    }
}
