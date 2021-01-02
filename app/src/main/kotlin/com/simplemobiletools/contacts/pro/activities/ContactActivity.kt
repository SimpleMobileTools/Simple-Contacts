package com.simplemobiletools.contacts.pro.activities

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.provider.ContactsContract.CommonDataKinds.*
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
import com.simplemobiletools.commons.helpers.letterBackgroundColors
import com.simplemobiletools.commons.models.RadioItem
import com.simplemobiletools.contacts.pro.R
import com.simplemobiletools.contacts.pro.extensions.sendEmailIntent
import com.simplemobiletools.contacts.pro.extensions.shareContacts
import com.simplemobiletools.contacts.pro.helpers.ContactsHelper
import com.simplemobiletools.contacts.pro.models.Contact
import java.util.*

abstract class ContactActivity : SimpleActivity() {
    protected var contact: Contact? = null
    protected var currentContactPhotoPath = ""

    fun showPhotoPlaceholder(photoView: ImageView) {
        val placeholder = BitmapDrawable(resources, getBigLetterPlaceholder(contact?.getNameToDisplay() ?: "A"))
        photoView.setImageDrawable(placeholder)
        currentContactPhotoPath = ""
        contact?.photo = null
    }

    fun updateContactPhoto(path: String, photoView: ImageView, bottomShadow: ImageView, bitmap: Bitmap? = null) {
        currentContactPhotoPath = path

        if (isDestroyed || isFinishing) {
            return
        }

        val options = RequestOptions()
            .diskCacheStrategy(DiskCacheStrategy.RESOURCE)
            .centerCrop()

        val wantedWidth = realScreenSize.x
        val wantedHeight = resources.getDimension(R.dimen.top_contact_image_height).toInt()

        Glide.with(this)
            .load(bitmap ?: path)
            .transition(DrawableTransitionOptions.withCrossFade())
            .apply(options)
            .override(wantedWidth, wantedHeight)
            .listener(object : RequestListener<Drawable> {
                override fun onResourceReady(resource: Drawable?, model: Any?, target: Target<Drawable>?, dataSource: DataSource?, isFirstResource: Boolean): Boolean {
                    photoView.background = ColorDrawable(0)
                    bottomShadow.beVisible()
                    return false
                }

                override fun onLoadFailed(e: GlideException?, model: Any?, target: Target<Drawable>?, isFirstResource: Boolean): Boolean {
                    showPhotoPlaceholder(photoView)
                    bottomShadow.beGone()
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
            launchSendSMSIntent(numbers.first().value)
        } else if (numbers.size > 1) {
            val items = ArrayList<RadioItem>()
            numbers.forEachIndexed { index, phoneNumber ->
                items.add(RadioItem(index, phoneNumber.value, phoneNumber.value))
            }

            RadioGroupDialog(this, items) {
                launchSendSMSIntent(it as String)
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

    private fun getBigLetterPlaceholder(name: String): Bitmap {
        val letter = name.getNameLetter()
        val height = resources.getDimension(R.dimen.top_contact_image_height).toInt()
        val bitmap = Bitmap.createBitmap(realScreenSize.x, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val view = TextView(this)
        view.layout(0, 0, bitmap.width, bitmap.height)

        val circlePaint = Paint().apply {
            color = letterBackgroundColors[Math.abs(name.hashCode()) % letterBackgroundColors.size].toInt()
            isAntiAlias = true
            style = Paint.Style.FILL
        }

        val wantedTextSize = bitmap.height / 2f
        val textPaint = Paint().apply {
            color = circlePaint.color.getContrastColor()
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
            textSize = wantedTextSize
            style = Paint.Style.FILL
        }

        canvas.drawPaint(circlePaint)

        val xPos = canvas.width / 2f
        val yPos = canvas.height / 2 - (textPaint.descent() + textPaint.ascent()) / 2
        canvas.drawText(letter, xPos, yPos, textPaint)
        view.draw(canvas)
        return bitmap
    }
}
