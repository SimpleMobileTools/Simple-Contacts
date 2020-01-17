package com.simplemobiletools.contacts.pro.activities

import android.annotation.TargetApi
import android.content.ContentValues
import android.content.Intent
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.telecom.TelecomManager
import com.simplemobiletools.commons.activities.BaseSimpleActivity
import com.simplemobiletools.commons.extensions.getColoredDrawableWithColor
import com.simplemobiletools.commons.extensions.toast
import com.simplemobiletools.contacts.pro.R
import com.simplemobiletools.contacts.pro.extensions.config
import com.simplemobiletools.contacts.pro.helpers.*

open class SimpleActivity : BaseSimpleActivity() {
    override fun getAppIconIDs() = arrayListOf(
            R.mipmap.ic_launcher_red,
            R.mipmap.ic_launcher_pink,
            R.mipmap.ic_launcher_purple,
            R.mipmap.ic_launcher_deep_purple,
            R.mipmap.ic_launcher_indigo,
            R.mipmap.ic_launcher_blue,
            R.mipmap.ic_launcher_light_blue,
            R.mipmap.ic_launcher_cyan,
            R.mipmap.ic_launcher_teal,
            R.mipmap.ic_launcher_green,
            R.mipmap.ic_launcher_light_green,
            R.mipmap.ic_launcher_lime,
            R.mipmap.ic_launcher_yellow,
            R.mipmap.ic_launcher_amber,
            R.mipmap.ic_launcher,
            R.mipmap.ic_launcher_deep_orange,
            R.mipmap.ic_launcher_brown,
            R.mipmap.ic_launcher_blue_grey,
            R.mipmap.ic_launcher_grey_black
    )

    override fun getAppLauncherName() = getString(R.string.app_launcher_name)

    protected fun getPhoneNumberFromIntent(intent: Intent): String? {
        if (intent.extras?.containsKey(KEY_PHONE) == true) {
            return intent.getStringExtra(KEY_PHONE)
        } else if (intent.extras?.containsKey("data") == true) {
            // sample contact number from Google Contacts:
            // data: [data1=+123 456 789 mimetype=vnd.android.cursor.item/phone_v2 _id=-1 data2=0]
            val data = intent.extras!!.get("data")
            if (data != null) {
                val contentValues = (data as? ArrayList<Any>)?.firstOrNull() as? ContentValues
                if (contentValues != null && contentValues.containsKey("data1")) {
                    return contentValues.getAsString("data1")
                }
            }
        }
        return null
    }

    protected fun getEmailFromIntent(intent: Intent): String? {
        return if (intent.dataString?.startsWith("$KEY_MAILTO:") == true) {
            Uri.decode(intent.dataString!!.substringAfter("$KEY_MAILTO:").trim())
        } else {
            null
        }
    }

    @TargetApi(Build.VERSION_CODES.M)
    protected fun launchSetDefaultDialerIntent() {
        Intent(TelecomManager.ACTION_CHANGE_DEFAULT_DIALER).putExtra(TelecomManager.EXTRA_CHANGE_DEFAULT_DIALER_PACKAGE_NAME, packageName).apply {
            if (resolveActivity(packageManager) != null) {
                startActivityForResult(this, REQUEST_CODE_SET_DEFAULT_DIALER)
            } else {
                toast(R.string.no_app_found)
            }
        }
    }

    protected fun getTabIcon(position: Int): Drawable {
        val drawableId = when (position) {
            LOCATION_CONTACTS_TAB -> R.drawable.ic_person_vector
            LOCATION_FAVORITES_TAB -> R.drawable.ic_star_on_vector
            else -> R.drawable.ic_group_vector
        }

        return resources.getColoredDrawableWithColor(drawableId, config.textColor)
    }
}
