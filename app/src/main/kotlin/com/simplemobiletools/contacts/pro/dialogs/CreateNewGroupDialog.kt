package com.simplemobiletools.contacts.pro.dialogs

import android.view.View
import androidx.appcompat.app.AlertDialog
import com.simplemobiletools.commons.activities.BaseSimpleActivity
import com.simplemobiletools.commons.dialogs.RadioGroupDialog
import com.simplemobiletools.commons.extensions.setupDialogStuff
import com.simplemobiletools.commons.extensions.showKeyboard
import com.simplemobiletools.commons.extensions.toast
import com.simplemobiletools.commons.extensions.value
import com.simplemobiletools.commons.helpers.ensureBackgroundThread
import com.simplemobiletools.commons.models.RadioItem
import com.simplemobiletools.contacts.pro.R
import com.simplemobiletools.contacts.pro.extensions.getPrivateContactSource
import com.simplemobiletools.contacts.pro.helpers.ContactsHelper
import com.simplemobiletools.contacts.pro.models.ContactSource
import com.simplemobiletools.contacts.pro.models.Group
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

                            val contactSources = ArrayList<ContactSource>()
                            ContactsHelper(activity).getContactSources {
                                it.filter { it.type.contains("google", true) }.mapTo(contactSources) { ContactSource(it.name, it.type, it.name) }
                                contactSources.add(activity.getPrivateContactSource())

                                val items = ArrayList<RadioItem>()
                                contactSources.forEachIndexed { index, contactSource ->
                                    items.add(RadioItem(index, contactSource.name))
                                }

                                activity.runOnUiThread {
                                    if (items.size == 1) {
                                        createGroupUnder(name, contactSources.first(), this)
                                    } else {
                                        RadioGroupDialog(activity, items, titleId = R.string.create_group_under_account) {
                                            val contactSource = contactSources[it as Int]
                                            createGroupUnder(name, contactSource, this)
                                        }
                                    }
                                }
                            }
                        })
                    }
                }
    }

    private fun createGroupUnder(name: String, contactSource: ContactSource, dialog: AlertDialog) {
        ensureBackgroundThread {
            val newGroup = ContactsHelper(activity).createNewGroup(name, contactSource.name, contactSource.type)
            activity.runOnUiThread {
                if (newGroup != null) {
                    callback(newGroup)
                }
                dialog.dismiss()
            }
        }
    }
}
