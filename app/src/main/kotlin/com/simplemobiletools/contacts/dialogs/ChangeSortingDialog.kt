package com.simplemobiletools.contacts.dialogs

import android.support.v7.app.AlertDialog
import com.simplemobiletools.commons.activities.BaseSimpleActivity
import com.simplemobiletools.commons.extensions.setupDialogStuff
import com.simplemobiletools.commons.helpers.SORT_BY_FIRST_NAME
import com.simplemobiletools.commons.helpers.SORT_BY_MIDDLE_NAME
import com.simplemobiletools.commons.helpers.SORT_BY_SURNAME
import com.simplemobiletools.commons.helpers.SORT_DESCENDING
import com.simplemobiletools.contacts.R
import com.simplemobiletools.contacts.extensions.config
import kotlinx.android.synthetic.main.dialog_change_sorting.view.*

class ChangeSortingDialog(val activity: BaseSimpleActivity, private val callback: () -> Unit) {
    private var currSorting = 0
    private var config = activity.config
    private var view = activity.layoutInflater.inflate(R.layout.dialog_change_sorting, null)

    init {
        AlertDialog.Builder(activity)
                .setPositiveButton(R.string.ok, { dialog, which -> dialogConfirmed() })
                .setNegativeButton(R.string.cancel, null)
                .create().apply {
            activity.setupDialogStuff(view, this, R.string.sort_by)
        }

        currSorting = config.sorting
        setupSortRadio()
        setupOrderRadio()
    }

    private fun setupSortRadio() {
        val sortingRadio = view.sorting_dialog_radio_sorting
        val sortBtn = when {
            currSorting and SORT_BY_FIRST_NAME != 0 -> sortingRadio.sorting_dialog_radio_first_name
            currSorting and SORT_BY_MIDDLE_NAME != 0 -> sortingRadio.sorting_dialog_radio_middle_name
            else -> sortingRadio.sorting_dialog_radio_surname
        }
        sortBtn.isChecked = true
    }

    private fun setupOrderRadio() {
        val orderRadio = view.sorting_dialog_radio_order
        var orderBtn = orderRadio.sorting_dialog_radio_ascending

        if (currSorting and SORT_DESCENDING != 0) {
            orderBtn = orderRadio.sorting_dialog_radio_descending
        }
        orderBtn.isChecked = true
    }

    private fun dialogConfirmed() {
        val sortingRadio = view.sorting_dialog_radio_sorting
        var sorting = when (sortingRadio.checkedRadioButtonId) {
            R.id.sorting_dialog_radio_first_name -> SORT_BY_FIRST_NAME
            R.id.sorting_dialog_radio_middle_name -> SORT_BY_MIDDLE_NAME
            else -> SORT_BY_SURNAME
        }

        if (view.sorting_dialog_radio_order.checkedRadioButtonId == R.id.sorting_dialog_radio_descending) {
            sorting = sorting or SORT_DESCENDING
        }

        config.sorting = sorting
        callback()
    }
}
