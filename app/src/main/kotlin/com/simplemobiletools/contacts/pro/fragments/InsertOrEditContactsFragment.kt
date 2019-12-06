package com.simplemobiletools.contacts.pro.fragments

import android.content.Context
import android.util.AttributeSet
import com.simplemobiletools.commons.extensions.getAdjustedPrimaryColor
import com.simplemobiletools.commons.extensions.getColoredDrawableWithColor
import com.simplemobiletools.contacts.pro.R
import com.simplemobiletools.contacts.pro.extensions.config
import kotlinx.android.synthetic.main.fragment_insert_or_edit_contacts.view.*

class InsertOrEditContactsFragment(context: Context, attributeSet: AttributeSet) : MyViewPagerFragment(context, attributeSet) {
    override fun fabClicked() {}

    override fun placeholderClicked() {}

    override fun viewSetup() {
        select_contact_label.setTextColor(context.getAdjustedPrimaryColor())
        new_contact_tmb.setImageDrawable(resources.getColoredDrawableWithColor(R.drawable.ic_new_contact_vector, context.config.textColor))
        new_contact_holder.setOnClickListener {

        }
    }
}
