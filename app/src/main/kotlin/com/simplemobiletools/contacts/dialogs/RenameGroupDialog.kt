package com.simplemobiletools.contacts.dialogs

import android.support.v7.app.AlertDialog
import com.simplemobiletools.commons.activities.BaseSimpleActivity
import com.simplemobiletools.commons.extensions.*
import com.simplemobiletools.contacts.R
import com.simplemobiletools.contacts.helpers.ContactsHelper
import com.simplemobiletools.contacts.models.Group
import kotlinx.android.synthetic.main.dialog_rename_group.view.*

class RenameGroupDialog(val activity: BaseSimpleActivity, val group: Group, val callback: () -> Unit) {
    init {

        val view = activity.layoutInflater.inflate(R.layout.dialog_rename_group, null).apply {
            rename_group_name.setText(group.title)
        }

        AlertDialog.Builder(activity)
                .setPositiveButton(R.string.ok, null)
                .setNegativeButton(R.string.cancel, null)
                .create().apply {
                    activity.setupDialogStuff(view, this, R.string.rename) {
                        showKeyboard(view.rename_group_name)
                        getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                            val newName = view.rename_group_name.value
                            if (newName.isEmpty()) {
                                activity.toast(R.string.empty_name)
                                return@setOnClickListener
                            }

                            if (!newName.isAValidFilename()) {
                                activity.toast(R.string.invalid_name)
                                return@setOnClickListener
                            }

                            ContactsHelper(activity).renameGroup(group, newName)
                            callback()
                            dismiss()
                        }
                    }
                }
    }
}
