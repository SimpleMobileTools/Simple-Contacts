package com.simplemobiletools.contacts.pro.dialogs

import androidx.appcompat.app.AlertDialog
import com.simplemobiletools.commons.activities.BaseSimpleActivity
import com.simplemobiletools.commons.extensions.setupDialogStuff
import com.simplemobiletools.commons.extensions.showKeyboard
import com.simplemobiletools.commons.extensions.toast
import com.simplemobiletools.commons.extensions.value
import com.simplemobiletools.contacts.pro.R
import kotlinx.android.synthetic.main.dialog_custom_label.view.*

class CustomLabelDialog(val activity: BaseSimpleActivity, val callback: (label: String) -> Unit) {
    init {

        val view = activity.layoutInflater.inflate(R.layout.dialog_custom_label, null)

        AlertDialog.Builder(activity)
                .setPositiveButton(R.string.ok, null)
                .setNegativeButton(R.string.cancel, null)
                .create().apply {
                    activity.setupDialogStuff(view, this, R.string.label) {
                        showKeyboard(view.custom_label_edittext)
                        getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                            val label = view.custom_label_edittext.value
                            if (label.isEmpty()) {
                                activity.toast(R.string.empty_name)
                                return@setOnClickListener
                            }

                            callback(label)
                            dismiss()
                        }
                    }
                }
    }
}
