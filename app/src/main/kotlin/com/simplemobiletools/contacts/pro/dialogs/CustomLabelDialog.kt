package com.simplemobiletools.contacts.pro.dialogs

import androidx.appcompat.app.AlertDialog
import com.simplemobiletools.commons.activities.BaseSimpleActivity
import com.simplemobiletools.commons.extensions.*
import com.simplemobiletools.contacts.pro.databinding.DialogCustomLabelBinding

class CustomLabelDialog(val activity: BaseSimpleActivity, val callback: (label: String) -> Unit) {
    init {
        val binding = DialogCustomLabelBinding.inflate(activity.layoutInflater)

        activity.getAlertDialogBuilder()
            .setPositiveButton(com.simplemobiletools.commons.R.string.ok, null)
            .setNegativeButton(com.simplemobiletools.commons.R.string.cancel, null)
            .apply {
                activity.setupDialogStuff(binding.root, this, com.simplemobiletools.commons.R.string.label) { alertDialog ->
                    alertDialog.showKeyboard(binding.customLabelEdittext)
                    alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                        val label = binding.customLabelEdittext.value
                        if (label.isEmpty()) {
                            activity.toast(com.simplemobiletools.commons.R.string.empty_name)
                            return@setOnClickListener
                        }

                        callback(label)
                        alertDialog.dismiss()
                    }
                }
            }
    }
}
