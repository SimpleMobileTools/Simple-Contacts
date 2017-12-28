package com.simplemobiletools.contacts.fragments

import android.content.Context
import android.support.design.widget.CoordinatorLayout
import android.util.AttributeSet
import com.simplemobiletools.contacts.activities.MainActivity
import com.simplemobiletools.contacts.extensions.config
import com.simplemobiletools.contacts.helpers.Config

abstract class MyViewPagerFragment(context: Context, attributeSet: AttributeSet) : CoordinatorLayout(context, attributeSet) {
    lateinit var config: Config

    fun setupFragment(activity: MainActivity) {
        config = activity.config
        initFragment(activity)
    }

    abstract fun initFragment(activity: MainActivity)

    abstract fun textColorChanged(color: Int)

    abstract fun primaryColorChanged(color: Int)

    abstract fun startNameWithSurnameChanged(startNameWithSurname: Boolean)

    abstract fun onActivityResume()
}
