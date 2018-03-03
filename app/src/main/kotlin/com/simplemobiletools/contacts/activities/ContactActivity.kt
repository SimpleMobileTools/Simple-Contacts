package com.simplemobiletools.contacts.activities

import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.provider.ContactsContract
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
import com.simplemobiletools.commons.extensions.getColoredBitmap
import com.simplemobiletools.commons.extensions.getContrastColor
import com.simplemobiletools.commons.helpers.getDateFormats
import com.simplemobiletools.commons.models.RadioItem
import com.simplemobiletools.contacts.R
import com.simplemobiletools.contacts.extensions.config
import com.simplemobiletools.contacts.extensions.sendEmailIntent
import com.simplemobiletools.contacts.extensions.sendSMSIntent
import com.simplemobiletools.contacts.extensions.shareContacts
import com.simplemobiletools.contacts.helpers.ContactsHelper
import com.simplemobiletools.contacts.models.Contact
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*

abstract class ContactActivity : SimpleActivity() {
    var contact: Contact? = null
    var currentContactPhotoPath = ""

    fun showPhotoPlaceholder(photoView: ImageView) {
        val placeholder = resources.getColoredBitmap(R.drawable.ic_person, config.primaryColor.getContrastColor())
        val padding = resources.getDimension(R.dimen.activity_margin).toInt()
        photoView.setPadding(padding, padding, padding, padding)
        photoView.setImageBitmap(placeholder)
        currentContactPhotoPath = ""
        contact?.photo = null
    }

    fun updateContactPhoto(path: String, photoView: ImageView, bitmap: Bitmap? = null) {
        currentContactPhotoPath = path
        val options = RequestOptions()
                .diskCacheStrategy(DiskCacheStrategy.RESOURCE)
                .centerCrop()

        Glide.with(this)
                .load(bitmap ?: path)
                .transition(DrawableTransitionOptions.withCrossFade())
                .apply(options)
                .listener(object : RequestListener<Drawable> {
                    override fun onResourceReady(resource: Drawable?, model: Any?, target: Target<Drawable>?, dataSource: DataSource?, isFirstResource: Boolean): Boolean {
                        photoView.setPadding(0, 0, 0, 0)
                        return false
                    }

                    override fun onLoadFailed(e: GlideException?, model: Any?, target: Target<Drawable>?, isFirstResource: Boolean): Boolean {
                        showPhotoPlaceholder(photoView)
                        return true
                    }
                }).into(photoView)
    }

    fun getDateTime(dateString: String, viewToUpdate: TextView? = null): DateTime {
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

    fun deleteContact() {
        ConfirmationDialog(this) {
            if (contact != null) {
                ContactsHelper(this).deleteContact(contact!!)
                finish()
            }
        }
    }

    fun shareContact() {
        if (contact != null) {
            shareContacts(arrayListOf(contact!!))
        }
    }

    fun trySendSMS() {
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

    fun trySendEmail() {
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

    fun getPhoneNumberTextId(type: Int) = when (type) {
        ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE -> R.string.mobile
        ContactsContract.CommonDataKinds.Phone.TYPE_HOME -> R.string.home
        ContactsContract.CommonDataKinds.Phone.TYPE_WORK -> R.string.work
        ContactsContract.CommonDataKinds.Phone.TYPE_MAIN -> R.string.main_number
        ContactsContract.CommonDataKinds.Phone.TYPE_FAX_WORK -> R.string.work_fax
        ContactsContract.CommonDataKinds.Phone.TYPE_FAX_HOME -> R.string.home_fax
        ContactsContract.CommonDataKinds.Phone.TYPE_PAGER -> R.string.pager
        else -> R.string.other
    }

    fun getEmailTextId(type: Int) = when (type) {
        ContactsContract.CommonDataKinds.Email.TYPE_HOME -> R.string.home
        ContactsContract.CommonDataKinds.Email.TYPE_WORK -> R.string.work
        ContactsContract.CommonDataKinds.Email.TYPE_MOBILE -> R.string.mobile
        else -> R.string.other
    }

    fun getAddressTextId(type: Int) = when (type) {
        ContactsContract.CommonDataKinds.StructuredPostal.TYPE_HOME -> R.string.home
        ContactsContract.CommonDataKinds.StructuredPostal.TYPE_WORK -> R.string.work
        else -> R.string.other
    }

    fun getEventTextId(type: Int) = when (type) {
        ContactsContract.CommonDataKinds.Event.TYPE_BIRTHDAY -> R.string.birthday
        ContactsContract.CommonDataKinds.Event.TYPE_ANNIVERSARY -> R.string.anniversary
        else -> R.string.other
    }
}
