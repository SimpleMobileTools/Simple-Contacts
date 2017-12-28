package com.simplemobiletools.contacts.extensions

import android.support.v4.view.ViewPager

fun ViewPager.onPageChanged(pageChangedAction: (activePage: Int) -> Unit) =
        addOnPageChangeListener(object : ViewPager.OnPageChangeListener {
            override fun onPageSelected(position: Int) {
                pageChangedAction(position)
            }

            override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {
            }

            override fun onPageScrollStateChanged(state: Int) {
            }
        })
