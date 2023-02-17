package com.simplemobiletools.contacts.pro.dialogs

import com.simplemobiletools.commons.activities.BaseSimpleActivity
import com.simplemobiletools.commons.extensions.getAlertDialogBuilder
import com.simplemobiletools.commons.extensions.setupDialogStuff
import com.simplemobiletools.commons.helpers.isSPlus
import com.simplemobiletools.contacts.pro.R
import com.simplemobiletools.commons.extensions.contactsConfig
import kotlinx.android.synthetic.main.dialog_date_picker.view.*
import org.joda.time.DateTime
import java.util.*

class MyDatePickerDialog(val activity: BaseSimpleActivity, val defaultDate: String, val callback: (dateTag: String) -> Unit) {
    private var view = activity.layoutInflater.inflate(R.layout.dialog_date_picker, null)

    init {
        activity.getAlertDialogBuilder()
            .setPositiveButton(R.string.ok) { dialog, which -> dialogConfirmed() }
            .setNegativeButton(R.string.cancel, null)
            .apply {
                activity.setupDialogStuff(view, this) { alertDialog ->
                    val today = Calendar.getInstance()
                    var year = today.get(Calendar.YEAR)
                    var month = today.get(Calendar.MONTH)
                    var day = today.get(Calendar.DAY_OF_MONTH)

                    if (defaultDate.isNotEmpty()) {
                        val ignoreYear = defaultDate.startsWith("-")
                        view.hide_year.isChecked = ignoreYear

                        if (ignoreYear) {
                            month = defaultDate.substring(2, 4).toInt() - 1
                            day = defaultDate.substring(5, 7).toInt()
                        } else {
                            year = defaultDate.substring(0, 4).toInt()
                            month = defaultDate.substring(5, 7).toInt() - 1
                            day = defaultDate.substring(8, 10).toInt()
                        }
                    }

                    if (activity.contactsConfig.isUsingSystemTheme && isSPlus()) {
                        val dialogBackgroundColor = activity.getColor(R.color.you_dialog_background_color)
                        view.dialog_holder.setBackgroundColor(dialogBackgroundColor)
                        view.date_picker.setBackgroundColor(dialogBackgroundColor)
                    }

                    view.date_picker.updateDate(year, month, day)
                }
            }
    }

    private fun dialogConfirmed() {
        val year = view.date_picker.year
        val month = view.date_picker.month + 1
        val day = view.date_picker.dayOfMonth
        val date = DateTime().withDate(year, month, day).withTimeAtStartOfDay()

        val tag = if (view.hide_year.isChecked) {
            date.toString("--MM-dd")
        } else {
            date.toString("yyyy-MM-dd")
        }

        callback(tag)
    }
}
