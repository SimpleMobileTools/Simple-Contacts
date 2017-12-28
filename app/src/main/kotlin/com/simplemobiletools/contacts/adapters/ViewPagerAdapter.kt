package com.simplemobiletools.contacts.adapters

import android.support.v4.view.PagerAdapter
import android.view.View
import android.view.ViewGroup
import com.simplemobiletools.contacts.R
import com.simplemobiletools.contacts.activities.MainActivity
import com.simplemobiletools.contacts.fragments.MyViewPagerFragment

class ViewPagerAdapter(val activity: MainActivity) : PagerAdapter() {

    override fun instantiateItem(container: ViewGroup, position: Int): Any {
        val layout = if (position == 0) R.layout.fragment_contacts else R.layout.fragment_favorites
        val view = activity.layoutInflater.inflate(layout, container, false)
        container.addView(view)
        (view as MyViewPagerFragment).setupFragment(activity)
        return view
    }

    override fun destroyItem(container: ViewGroup, position: Int, item: Any) {
        container.removeView(item as View)
    }

    override fun getCount() = 2
    override fun isViewFromObject(view: View, item: Any) = view == item
}
