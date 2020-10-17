package com.simplemobiletools.contacts.pro.activities

import android.app.Dialog
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.provider.ContactsContract.CommonDataKinds.*
import android.view.Window
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.Target
import com.google.zxing.BarcodeFormat
import com.journeyapps.barcodescanner.BarcodeEncoder
import com.simplemobiletools.commons.dialogs.ConfirmationDialog
import com.simplemobiletools.commons.dialogs.RadioGroupDialog
import com.simplemobiletools.commons.helpers.SimpleContactsHelper
import com.simplemobiletools.commons.models.RadioItem
import com.simplemobiletools.contacts.pro.R
import com.simplemobiletools.contacts.pro.extensions.sendEmailIntent
import com.simplemobiletools.contacts.pro.extensions.sendSMSIntent
import com.simplemobiletools.contacts.pro.extensions.shareContacts
import com.simplemobiletools.contacts.pro.extensions.shareQRContact
import com.simplemobiletools.contacts.pro.helpers.ContactsHelper
import com.simplemobiletools.contacts.pro.models.Contact
import java.util.*

abstract class ContactActivity : SimpleActivity() {
    protected var contact: Contact? = null
    protected var currentContactPhotoPath = ""

    fun showPhotoPlaceholder(photoView: ImageView) {
        val placeholder = BitmapDrawable(resources, SimpleContactsHelper(this).getContactLetterIcon(contact?.getNameToDisplay() ?: "A"))
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

    fun shareContactQR(contact: Contact) {
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setCancelable(true)
        dialog.setContentView(R.layout.dialog_qr_view)

        val QRImage = dialog.findViewById<ImageView>(R.id.qr_imageviev)

        val bitmap = BarcodeEncoder().encodeBitmap(shareQRContact(contact), BarcodeFormat.QR_CODE, 512, 512)

        QRImage.setImageBitmap(bitmap)

        dialog.show()
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

    fun getEmailTypeText(type: Int, label: String): String {
        return if (type == BaseTypes.TYPE_CUSTOM) {
            label
        } else {
            getString(when (type) {
                Email.TYPE_HOME -> R.string.home
                Email.TYPE_WORK -> R.string.work
                Email.TYPE_MOBILE -> R.string.mobile
                else -> R.string.other
            })
        }
    }

    fun getAddressTypeText(type: Int, label: String): String {
        return if (type == BaseTypes.TYPE_CUSTOM) {
            label
        } else {
            getString(when (type) {
                StructuredPostal.TYPE_HOME -> R.string.home
                StructuredPostal.TYPE_WORK -> R.string.work
                else -> R.string.other
            })
        }
    }

    fun getIMTypeText(type: Int, label: String): String {
        return if (type == Im.PROTOCOL_CUSTOM) {
            label
        } else {
            getString(when (type) {
                Im.PROTOCOL_AIM -> R.string.aim
                Im.PROTOCOL_MSN -> R.string.windows_live
                Im.PROTOCOL_YAHOO -> R.string.yahoo
                Im.PROTOCOL_SKYPE -> R.string.skype
                Im.PROTOCOL_QQ -> R.string.qq
                Im.PROTOCOL_GOOGLE_TALK -> R.string.hangouts
                Im.PROTOCOL_ICQ -> R.string.icq
                else -> R.string.jabber
            })
        }
    }

    fun getEventTextId(type: Int) = when (type) {
        Event.TYPE_ANNIVERSARY -> R.string.anniversary
        Event.TYPE_BIRTHDAY -> R.string.birthday
        else -> R.string.other
    }
}
