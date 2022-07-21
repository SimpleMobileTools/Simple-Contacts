package com.simplemobiletools.contacts.pro.dialogs

import androidx.appcompat.app.AlertDialog
import com.simplemobiletools.commons.activities.BaseSimpleActivity
import com.simplemobiletools.commons.extensions.*
import com.simplemobiletools.contacts.pro.R
import kotlinx.android.synthetic.main.dialog_custom_label.view.*

class CustomLabelDialog(val activity: BaseSimpleActivity, val callback: (label: String) -> Unit) {
    init {

        val view = activity.layoutInflater.inflate(R.layout.dialog_custom_label, null)

        activity.getAlertDialogBuilder()
            .setPositiveButton(R.string.ok, null)
            .setNegativeButton(R.string.cancel, null)
            .apply {
                activity.setupDialogStuff(view, this, R.string.label) { alertDialog ->
                    alertDialog.showKeyboard(view.custom_label_edittext)
                    alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                        val label = view.custom_label_edittext.value
                        if (label.isEmpty()) {
                            activity.toast(R.string.empty_name)
                            return@setOnClickListener
                        }

                        callback(label)
                        alertDialog.dismiss()
                    }
                }
            }
    }
}
