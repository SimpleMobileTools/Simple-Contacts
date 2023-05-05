package com.simplemobiletools.contacts.pro.adapters

import android.graphics.drawable.BitmapDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Filter
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.request.RequestOptions
import com.simplemobiletools.commons.extensions.beGone
import com.simplemobiletools.commons.extensions.getProperBackgroundColor
import com.simplemobiletools.commons.extensions.getProperTextColor
import com.simplemobiletools.commons.extensions.normalizeString
import com.simplemobiletools.commons.helpers.SimpleContactsHelper
import com.simplemobiletools.commons.models.contacts.Contact
import com.simplemobiletools.contacts.pro.R
import com.simplemobiletools.contacts.pro.activities.SimpleActivity
import kotlinx.android.synthetic.main.item_autocomplete_name_number.view.item_autocomplete_image
import kotlinx.android.synthetic.main.item_autocomplete_name_number.view.item_autocomplete_name
import kotlinx.android.synthetic.main.item_autocomplete_name_number.view.item_autocomplete_number

class AutoCompleteTextViewAdapter(
    val activity: SimpleActivity,
    val contacts: ArrayList<Contact>,
    var enableAutoFill: Boolean = false
) : ArrayAdapter<Contact>(activity, 0, contacts) {
    var resultList = ArrayList<Contact>()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val contact = resultList[position]
        var listItem = convertView
        val nameToUse = contact.getNameToDisplay()
        if (listItem == null || listItem.tag != nameToUse.isNotEmpty()) {
            listItem = LayoutInflater.from(activity).inflate(R.layout.item_autocomplete_name_number, parent, false)
        }

        val placeholder = BitmapDrawable(activity.resources, SimpleContactsHelper(context).getContactLetterIcon(nameToUse))
        listItem!!.apply {
            setBackgroundColor(context.getProperBackgroundColor())
            item_autocomplete_name.setTextColor(context.getProperTextColor())
            item_autocomplete_number.setTextColor(context.getProperTextColor())

            tag = nameToUse.isNotEmpty()
            item_autocomplete_name.text = nameToUse
            contact.phoneNumbers.apply {
                val phoneNumber = firstOrNull { it.isPrimary }?.normalizedNumber ?: firstOrNull()?.normalizedNumber
                if (phoneNumber.isNullOrEmpty()) {
                    item_autocomplete_number.beGone()
                } else {
                    item_autocomplete_number.text = phoneNumber
                }
            }

            val options = RequestOptions()
                .diskCacheStrategy(DiskCacheStrategy.RESOURCE)
                .error(placeholder)
                .centerCrop()

            Glide.with(context)
                .load(contact.photoUri)
                .transition(DrawableTransitionOptions.withCrossFade())
                .placeholder(placeholder)
                .apply(options)
                .apply(RequestOptions.circleCropTransform())
                .into(item_autocomplete_image)
        }

        return listItem
    }

    override fun getFilter() = object : Filter() {
        override fun performFiltering(constraint: CharSequence?): FilterResults {
            val filterResults = FilterResults()
            if (constraint != null) {
                resultList.clear()
                if (enableAutoFill) {
                    val searchString = constraint.toString().normalizeString()
                    contacts.forEach {
                        if (it.getNameToDisplay().contains(searchString, true)) {
                            resultList.add(it)
                        }
                    }

                    resultList.sortWith(compareBy<Contact>
                    { it.name.startsWith(searchString, true) }.thenBy
                    { it.name.contains(searchString, true) })
                    resultList.reverse()

                    filterResults.values = resultList
                    filterResults.count = resultList.size
                }
            }
            return filterResults
        }

        override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
            if ((results?.count ?: -1) > 0) {
                notifyDataSetChanged()
            } else {
                notifyDataSetInvalidated()
            }
        }

        override fun convertResultToString(resultValue: Any?) = (resultValue as? Contact)?.name
    }

    override fun getItem(index: Int) = resultList[index]

    override fun getCount() = resultList.size
}
