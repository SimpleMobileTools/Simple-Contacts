package com.simplemobiletools.contacts.extensions

import android.content.Intent
import android.net.Uri
import com.simplemobiletools.commons.R
import com.simplemobiletools.commons.extensions.toast
import com.simplemobiletools.commons.helpers.PERMISSION_CALL_PHONE
import com.simplemobiletools.contacts.activities.SimpleActivity

fun SimpleActivity.startCallIntent(recipient: String) {
    handlePermission(PERMISSION_CALL_PHONE) {
        if (it) {
            Intent(Intent.ACTION_CALL).apply {
                data = Uri.fromParts("tel", recipient, null)
                if (resolveActivity(packageManager) != null) {
                    startActivity(this)
                } else {
                    toast(R.string.no_app_found)
                }
            }
        }
    }
}
