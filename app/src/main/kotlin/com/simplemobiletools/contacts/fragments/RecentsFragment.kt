package com.simplemobiletools.contacts.fragments

import android.content.Context
import android.util.AttributeSet

class RecentsFragment(context: Context, attributeSet: AttributeSet) : MyViewPagerFragment(context, attributeSet) {
    override fun fabClicked() {
        finishActMode()
    }

    override fun placeholderClicked() {

    }
}
