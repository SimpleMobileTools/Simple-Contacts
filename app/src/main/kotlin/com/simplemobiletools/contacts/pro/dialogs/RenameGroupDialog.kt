package com.simplemobiletools.contacts.pro.dialogs

import androidx.appcompat.app.AlertDialog
import com.simplemobiletools.commons.activities.BaseSimpleActivity
import com.simplemobiletools.commons.extensions.*
import com.simplemobiletools.commons.helpers.ContactsHelper
import com.simplemobiletools.commons.helpers.ensureBackgroundThread
import com.simplemobiletools.commons.models.contacts.Group
import com.simplemobiletools.contacts.pro.databinding.DialogRenameGroupBinding

class RenameGroupDialog(val activity: BaseSimpleActivity, val group: Group, val callback: () -> Unit) {
    init {
        val binding = DialogRenameGroupBinding.inflate(activity.layoutInflater).apply {
            renameGroupTitle.setText(group.title)
        }

        activity.getAlertDialogBuilder()
            .setPositiveButton(com.simplemobiletools.commons.R.string.ok, null)
            .setNegativeButton(com.simplemobiletools.commons.R.string.cancel, null)
            .apply {
                activity.setupDialogStuff(binding.root, this, com.simplemobiletools.commons.R.string.rename) { alertDialog ->
                    alertDialog.showKeyboard(binding.renameGroupTitle)
                    alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                        val newTitle = binding.renameGroupTitle.value
                        if (newTitle.isEmpty()) {
                            activity.toast(com.simplemobiletools.commons.R.string.empty_name)
                            return@setOnClickListener
                        }

                        if (!newTitle.isAValidFilename()) {
                            activity.toast(com.simplemobiletools.commons.R.string.invalid_name)
                            return@setOnClickListener
                        }

                        group.title = newTitle
                        group.contactsCount = 0
                        ensureBackgroundThread {
                            if (group.isPrivateSecretGroup()) {
                                activity.groupsDB.insertOrUpdate(group)
                            } else {
                                ContactsHelper(activity).renameGroup(group)
                            }
                            activity.runOnUiThread {
                                callback()
                                alertDialog.dismiss()
                            }
                        }
                    }
                }
            }
    }
}
