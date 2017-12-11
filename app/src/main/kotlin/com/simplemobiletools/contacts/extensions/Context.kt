package com.simplemobiletools.contacts.extensions

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.simplemobiletools.commons.R
import com.simplemobiletools.commons.extensions.toast
import com.simplemobiletools.contacts.helpers.Config

val Context.config: Config get() = Config.newInstance(applicationContext)

fun Context.sendEmail(recipient: String) {
    Intent(Intent.ACTION_SENDTO, Uri.fromParts("mailto", recipient, null)).apply {
        if (resolveActivity(packageManager) != null) {
            startActivity(this)
        } else {
            toast(R.string.no_app_found)
        }
    }
}
