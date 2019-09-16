package com.simplemobiletools.contacts.pro.activities

import android.graphics.Bitmap
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.provider.ContactsContract
import android.widget.ImageView
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
import com.simplemobiletools.commons.extensions.applyColorFilter
import com.simplemobiletools.commons.extensions.getColoredDrawableWithColor
import com.simplemobiletools.commons.extensions.getContrastColor
import com.simplemobiletools.commons.models.RadioItem
import com.simplemobiletools.contacts.pro.R
import com.simplemobiletools.contacts.pro.extensions.config
import com.simplemobiletools.contacts.pro.extensions.sendEmailIntent
import com.simplemobiletools.contacts.pro.extensions.sendSMSIntent
import com.simplemobiletools.contacts.pro.extensions.shareContacts
import com.simplemobiletools.contacts.pro.helpers.ContactsHelper
import com.simplemobiletools.contacts.pro.models.Contact
import java.util.*

abstract class ContactActivity : SimpleActivity() {
    protected var contact: Contact? = null
    protected var currentContactPhotoPath = ""

    fun showPhotoPlaceholder(photoView: ImageView) {
        val background = resources.getDrawable(R.drawable.contact_circular_background)
        background.applyColorFilter(config.primaryColor)
        photoView.background = background

        val placeholder = resources.getColoredDrawableWithColor(R.drawable.ic_person_vector, config.primaryColor.getContrastColor())
        val padding = resources.getDimension(R.dimen.activity_margin).toInt()
        photoView.setPadding(padding, padding, padding, padding)
        photoView.setImageDrawable(placeholder)
        currentContactPhotoPath = ""
        contact?.photo = null
    }

    fun updateContactPhoto(path: String, photoView: ImageView, bitmap: Bitmap? = null) {
        currentContactPhotoPath = path
        val options = RequestOptions()
                .diskCacheStrategy(DiskCacheStrategy.RESOURCE)
                .centerCrop()

        if (isDestroyed || isFinishing) {
            return
        }

        Glide.with(this)
                .load(bitmap ?: path)
                .transition(DrawableTransitionOptions.withCrossFade())
                .apply(options)
                .apply(RequestOptions.circleCropTransform())
                .listener(object : RequestListener<Drawable> {
                    override fun onResourceReady(resource: Drawable?, model: Any?, target: Target<Drawable>?, dataSource: DataSource?, isFirstResource: Boolean): Boolean {
                        photoView.setPadding(0, 0, 0, 0)
                        photoView.background = ColorDrawable(0)
                        return false
                    }

                    override fun onLoadFailed(e: GlideException?, model: Any?, target: Target<Drawable>?, isFirstResource: Boolean): Boolean {
                        showPhotoPlaceholder(photoView)
                        return true
                    }
                }).into(photoView)
    }

    fun deleteContact() {
        ConfirmationDialog(this) {
            if (contact != null) {
                ContactsHelper(this).deleteContact(contact!!, false) {
                    finish()
                }
            }
        }
    }

    fun shareContact(contact: Contact) {
        shareContacts(arrayListOf(contact))
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

    fun getPhoneNumberTypeText(type: Int, label: String): String {
        return if (type == ContactsContract.CommonDataKinds.BaseTypes.TYPE_CUSTOM) {
            label
        } else {
            getString(when (type) {
                ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE -> R.string.mobile
                ContactsContract.CommonDataKinds.Phone.TYPE_HOME -> R.string.home
                ContactsContract.CommonDataKinds.Phone.TYPE_WORK -> R.string.work
                ContactsContract.CommonDataKinds.Phone.TYPE_MAIN -> R.string.main_number
                ContactsContract.CommonDataKinds.Phone.TYPE_FAX_WORK -> R.string.work_fax
                ContactsContract.CommonDataKinds.Phone.TYPE_FAX_HOME -> R.string.home_fax
                ContactsContract.CommonDataKinds.Phone.TYPE_PAGER -> R.string.pager
                else -> R.string.other
            })
        }
    }

    fun getEmailTypeText(type: Int, label: String): String {
        return if (type == ContactsContract.CommonDataKinds.BaseTypes.TYPE_CUSTOM) {
            label
        } else {
            getString(when (type) {
                ContactsContract.CommonDataKinds.Email.TYPE_HOME -> R.string.home
                ContactsContract.CommonDataKinds.Email.TYPE_WORK -> R.string.work
                ContactsContract.CommonDataKinds.Email.TYPE_MOBILE -> R.string.mobile
                else -> R.string.other
            })
        }
    }

    fun getAddressTypeText(type: Int, label: String): String {
        return if (type == ContactsContract.CommonDataKinds.BaseTypes.TYPE_CUSTOM) {
            label
        } else {
            getString(when (type) {
                ContactsContract.CommonDataKinds.StructuredPostal.TYPE_HOME -> R.string.home
                ContactsContract.CommonDataKinds.StructuredPostal.TYPE_WORK -> R.string.work
                else -> R.string.other
            })
        }
    }

    fun getIMTypeText(type: Int, label: String): String {
        return if (type == ContactsContract.CommonDataKinds.Im.PROTOCOL_CUSTOM) {
            label
        } else {
            getString(when (type) {
                ContactsContract.CommonDataKinds.Im.PROTOCOL_AIM -> R.string.aim
                ContactsContract.CommonDataKinds.Im.PROTOCOL_MSN -> R.string.windows_live
                ContactsContract.CommonDataKinds.Im.PROTOCOL_YAHOO -> R.string.yahoo
                ContactsContract.CommonDataKinds.Im.PROTOCOL_SKYPE -> R.string.skype
                ContactsContract.CommonDataKinds.Im.PROTOCOL_QQ -> R.string.qq
                ContactsContract.CommonDataKinds.Im.PROTOCOL_GOOGLE_TALK -> R.string.hangouts
                ContactsContract.CommonDataKinds.Im.PROTOCOL_ICQ -> R.string.icq
                else -> R.string.jabber
            })
        }
    }

    fun getEventTextId(type: Int) = when (type) {
        ContactsContract.CommonDataKinds.Event.TYPE_ANNIVERSARY -> R.string.anniversary
        ContactsContract.CommonDataKinds.Event.TYPE_BIRTHDAY -> R.string.birthday
        else -> R.string.other
    }
}
