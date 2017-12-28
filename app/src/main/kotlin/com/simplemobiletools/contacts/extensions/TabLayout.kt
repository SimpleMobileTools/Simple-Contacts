package com.simplemobiletools.contacts.extensions

import android.support.design.widget.TabLayout

fun TabLayout.onTabSelectionChanged(tabUnselectedAction: (inactiveTab: TabLayout.Tab) -> Unit, tabSelectedAction: (activeTab: TabLayout.Tab) -> Unit) =
        setOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                tabSelectedAction(tab)
            }

            override fun onTabUnselected(tab: TabLayout.Tab) {
                tabUnselectedAction(tab)
            }

            override fun onTabReselected(tab: TabLayout.Tab) {
            }
        })
