package com.simplemobiletools.contacts.pro.dialogs

import androidx.appcompat.app.AlertDialog
import com.simplemobiletools.commons.activities.BaseSimpleActivity
import com.simplemobiletools.commons.extensions.setupDialogStuff
import com.simplemobiletools.commons.extensions.showKeyboard
import com.simplemobiletools.commons.extensions.value
import com.simplemobiletools.contacts.pro.R
import com.simplemobiletools.contacts.pro.extensions.addBlockedNumber
import kotlinx.android.synthetic.main.dialog_add_blocked_number.view.*

class AddBlockedNumberDialog(val activity: BaseSimpleActivity, val callback: () -> Unit) {
    init {

        val view = activity.layoutInflater.inflate(R.layout.dialog_add_blocked_number, null)

        AlertDialog.Builder(activity)
                .setPositiveButton(R.string.ok, null)
                .setNegativeButton(R.string.cancel, null)
                .create().apply {
                    activity.setupDialogStuff(view, this) {
                        showKeyboard(view.add_blocked_number_edittext)
                        getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                            val phoneNumber = view.add_blocked_number_edittext.value
                            if (phoneNumber.isNotEmpty()) {
                                activity.addBlockedNumber(phoneNumber)
                            }

                            callback()
                            dismiss()
                        }
                    }
                }
    }
}
