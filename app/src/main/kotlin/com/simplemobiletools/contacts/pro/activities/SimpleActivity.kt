package com.simplemobiletools.contacts.pro.activities

import android.annotation.TargetApi
import android.content.ContentValues
import android.content.Intent
import android.os.Build
import android.telecom.TelecomManager
import com.simplemobiletools.commons.activities.BaseSimpleActivity
import com.simplemobiletools.contacts.pro.R
import com.simplemobiletools.contacts.pro.helpers.KEY_PHONE
import com.simplemobiletools.contacts.pro.helpers.REQUEST_CODE_SET_DEFAULT_DIALER

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

    @TargetApi(Build.VERSION_CODES.M)
    protected fun launchSetDefaultDialerIntent() {
        val intent = Intent(TelecomManager.ACTION_CHANGE_DEFAULT_DIALER).putExtra(TelecomManager.EXTRA_CHANGE_DEFAULT_DIALER_PACKAGE_NAME, packageName)
        startActivityForResult(intent, REQUEST_CODE_SET_DEFAULT_DIALER)
    }
}
