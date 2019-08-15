package com.simplemobiletools.contacts.pro.dialogs

import androidx.appcompat.app.AlertDialog
import com.simplemobiletools.commons.activities.BaseSimpleActivity
import com.simplemobiletools.commons.extensions.*
import com.simplemobiletools.commons.helpers.ensureBackgroundThread
import com.simplemobiletools.contacts.pro.R
import com.simplemobiletools.contacts.pro.extensions.groupsDB
import com.simplemobiletools.contacts.pro.helpers.ContactsHelper
import com.simplemobiletools.contacts.pro.models.Group
import kotlinx.android.synthetic.main.dialog_rename_group.view.*

class RenameGroupDialog(val activity: BaseSimpleActivity, val group: Group, val callback: () -> Unit) {
    init {

        val view = activity.layoutInflater.inflate(R.layout.dialog_rename_group, null).apply {
            rename_group_title.setText(group.title)
        }

        AlertDialog.Builder(activity)
                .setPositiveButton(R.string.ok, null)
                .setNegativeButton(R.string.cancel, null)
                .create().apply {
                    activity.setupDialogStuff(view, this, R.string.rename) {
                        showKeyboard(view.rename_group_title)
                        getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                            val newTitle = view.rename_group_title.value
                            if (newTitle.isEmpty()) {
                                activity.toast(R.string.empty_name)
                                return@setOnClickListener
                            }

                            if (!newTitle.isAValidFilename()) {
                                activity.toast(R.string.invalid_name)
                                return@setOnClickListener
                            }

                            group.title = newTitle
                            ensureBackgroundThread {
                                if (group.isPrivateSecretGroup()) {
                                    activity.groupsDB.insertOrUpdate(group)
                                } else {
                                    ContactsHelper(activity).renameGroup(group)
                                }
                                activity.runOnUiThread {
                                    callback()
                                    dismiss()
                                }
                            }
                        }
                    }
                }
    }
}
