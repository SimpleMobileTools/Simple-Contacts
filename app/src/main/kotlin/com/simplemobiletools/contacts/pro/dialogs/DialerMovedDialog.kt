package com.simplemobiletools.contacts.pro.dialogs

import android.text.Html
import android.text.method.LinkMovementMethod
import androidx.appcompat.app.AlertDialog
import com.simplemobiletools.commons.activities.BaseSimpleActivity
import com.simplemobiletools.commons.extensions.launchViewIntent
import com.simplemobiletools.commons.extensions.setupDialogStuff
import com.simplemobiletools.contacts.pro.R
import kotlinx.android.synthetic.main.dialog_dialer_moved.view.*

class DialerMovedDialog(val activity: BaseSimpleActivity) {
    init {
        val view = activity.layoutInflater.inflate(R.layout.dialog_dialer_moved, null).apply {
            dialer_moved.text = Html.fromHtml(activity.getString(R.string.dialer_moved))
            dialer_moved.movementMethod = LinkMovementMethod.getInstance()

            dialer_moved_icon.setOnClickListener {
                activity.launchViewIntent("https://play.google.com/store/apps/details?id=com.simplemobiletools.dialer")
            }
        }

        AlertDialog.Builder(activity)
            .setPositiveButton(R.string.download) { dialog, which -> }
            .setNegativeButton(R.string.cancel, null)
            .create().apply {
                activity.setupDialogStuff(view, this)
            }
    }
}
