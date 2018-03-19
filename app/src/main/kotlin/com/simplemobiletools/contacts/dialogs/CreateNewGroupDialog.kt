package com.simplemobiletools.contacts.dialogs

import android.support.v7.app.AlertDialog
import android.view.View
import com.simplemobiletools.commons.activities.BaseSimpleActivity
import com.simplemobiletools.commons.extensions.setupDialogStuff
import com.simplemobiletools.commons.extensions.showKeyboard
import com.simplemobiletools.commons.extensions.toast
import com.simplemobiletools.commons.extensions.value
import com.simplemobiletools.contacts.R
import com.simplemobiletools.contacts.helpers.ContactsHelper
import com.simplemobiletools.contacts.models.Group
import kotlinx.android.synthetic.main.dialog_create_new_group.view.*

class CreateNewGroupDialog(val activity: BaseSimpleActivity, val callback: (newGroup: Group) -> Unit) {
    init {
        val view = activity.layoutInflater.inflate(R.layout.dialog_create_new_group, null)

        AlertDialog.Builder(activity)
                .setPositiveButton(R.string.ok, null)
                .setNegativeButton(R.string.cancel, null)
                .create().apply {
                    activity.setupDialogStuff(view, this, R.string.create_new_group) {
                        showKeyboard(view.group_name)
                        getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(View.OnClickListener {
                            val name = view.group_name.value
                            if (name.isEmpty()) {
                                activity.toast(R.string.empty_name)
                                return@OnClickListener
                            }

                            val newGroup = ContactsHelper(activity).createNewGroup(name)
                            if (newGroup != null) {
                                callback(newGroup)
                            }
                            dismiss()
                        })
                    }
                }
    }
}
