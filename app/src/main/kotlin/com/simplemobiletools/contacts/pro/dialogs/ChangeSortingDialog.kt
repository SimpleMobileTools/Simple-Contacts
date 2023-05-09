package com.simplemobiletools.contacts.pro.dialogs

import com.simplemobiletools.commons.activities.BaseSimpleActivity
import com.simplemobiletools.commons.extensions.beGoneIf
import com.simplemobiletools.commons.extensions.getAlertDialogBuilder
import com.simplemobiletools.commons.extensions.setupDialogStuff
import com.simplemobiletools.commons.helpers.*
import com.simplemobiletools.contacts.pro.R
import com.simplemobiletools.contacts.pro.extensions.config
import kotlinx.android.synthetic.main.dialog_change_sorting.view.*

class ChangeSortingDialog(val activity: BaseSimpleActivity, private val showCustomSorting: Boolean = false, private val callback: () -> Unit) {
    private var currSorting = 0
    private var config = activity.config
    private var view = activity.layoutInflater.inflate(R.layout.dialog_change_sorting, null)

    init {
        activity.getAlertDialogBuilder()
            .setPositiveButton(R.string.ok) { dialog, which -> dialogConfirmed() }
            .setNegativeButton(R.string.cancel, null)
            .apply {
                activity.setupDialogStuff(view, this, R.string.sort_by)
            }

        currSorting = if (showCustomSorting && config.isCustomOrderSelected) {
            SORT_BY_CUSTOM
        } else {
            config.sorting
        }

        setupSortRadio()
        setupOrderRadio()
    }

    private fun setupSortRadio() {
        val sortingRadio = view.sorting_dialog_radio_sorting

        sortingRadio.setOnCheckedChangeListener { group, checkedId ->
            val isCustomSorting = checkedId == sortingRadio.sorting_dialog_radio_custom.id
            view.sorting_dialog_radio_order.beGoneIf(isCustomSorting)
            view.divider.beGoneIf(isCustomSorting)
        }

        val sortBtn = when {
            ((currSorting and SORT_BY_FIRST_NAME) != 0) -> sortingRadio.sorting_dialog_radio_first_name
            ((currSorting and SORT_BY_MIDDLE_NAME) != 0) -> sortingRadio.sorting_dialog_radio_middle_name
            ((currSorting and SORT_BY_SURNAME) != 0) -> sortingRadio.sorting_dialog_radio_surname
            ((currSorting and SORT_BY_FULL_NAME) != 0) -> sortingRadio.sorting_dialog_radio_full_name
            ((currSorting and SORT_BY_CUSTOM) != 0) -> sortingRadio.sorting_dialog_radio_custom
            else -> sortingRadio.sorting_dialog_radio_date_created
        }
        sortBtn.isChecked = true

        if (showCustomSorting) {
            sortingRadio.sorting_dialog_radio_custom.isChecked = config.isCustomOrderSelected
        }
        view.sorting_dialog_radio_custom.beGoneIf(!showCustomSorting)
    }

    private fun setupOrderRadio() {
        val orderRadio = view.sorting_dialog_radio_order
        var orderBtn = orderRadio.sorting_dialog_radio_ascending

        if ((currSorting and SORT_DESCENDING) != 0) {
            orderBtn = orderRadio.sorting_dialog_radio_descending
        }
        orderBtn.isChecked = true
    }

    private fun dialogConfirmed() {
        val sortingRadio = view.sorting_dialog_radio_sorting
        var sorting = when (sortingRadio.checkedRadioButtonId) {
            R.id.sorting_dialog_radio_first_name -> SORT_BY_FIRST_NAME
            R.id.sorting_dialog_radio_middle_name -> SORT_BY_MIDDLE_NAME
            R.id.sorting_dialog_radio_surname -> SORT_BY_SURNAME
            R.id.sorting_dialog_radio_full_name -> SORT_BY_FULL_NAME
            R.id.sorting_dialog_radio_custom -> SORT_BY_CUSTOM
            else -> SORT_BY_DATE_CREATED
        }

        if ((sorting != SORT_BY_CUSTOM) &&
            (view.sorting_dialog_radio_order.checkedRadioButtonId == R.id.sorting_dialog_radio_descending)) {
            sorting = sorting or SORT_DESCENDING
        }

        if (showCustomSorting) {
            if (sorting == SORT_BY_CUSTOM) {
                config.isCustomOrderSelected = true
            } else {
                config.isCustomOrderSelected = false
                config.sorting = sorting
            }
        } else {
            config.sorting = sorting
        }

        callback()
    }
}
