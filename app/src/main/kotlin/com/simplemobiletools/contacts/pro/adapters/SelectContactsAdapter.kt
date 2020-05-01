package com.simplemobiletools.contacts.pro.adapters

import android.graphics.drawable.BitmapDrawable
import android.util.SparseArray
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.signature.ObjectKey
import com.simplemobiletools.commons.extensions.*
import com.simplemobiletools.commons.views.FastScroller
import com.simplemobiletools.commons.views.MyRecyclerView
import com.simplemobiletools.contacts.pro.R
import com.simplemobiletools.contacts.pro.activities.SimpleActivity
import com.simplemobiletools.contacts.pro.extensions.config
import com.simplemobiletools.contacts.pro.helpers.highlightTextFromNumbers
import com.simplemobiletools.contacts.pro.models.Contact
import kotlinx.android.synthetic.main.item_add_favorite_with_number.view.*
import java.util.*

class SelectContactsAdapter(val activity: SimpleActivity, var contacts: ArrayList<Contact>, private val selectedContacts: ArrayList<Contact>, private val allowPickMultiple: Boolean,
                            recyclerView: MyRecyclerView, val fastScroller: FastScroller, private val itemClick: ((Contact) -> Unit)? = null) :
        RecyclerView.Adapter<SelectContactsAdapter.ViewHolder>() {
    private val itemViews = SparseArray<View>()
    private val selectedPositions = HashSet<Int>()
    private val config = activity.config
    private val adjustedPrimaryColor = activity.getAdjustedPrimaryColor()
    private val fontSize = activity.getTextSize()

    private val showContactThumbnails = config.showContactThumbnails
    private val showPhoneNumbers = config.showPhoneNumbers
    private val itemLayout = if (showPhoneNumbers) R.layout.item_add_favorite_with_number else R.layout.item_add_favorite_without_number
    private var textToHighlight = ""

    init {
        contacts.forEachIndexed { index, contact ->
            if (selectedContacts.asSequence().map { it.id }.contains(contact.id)) {
                selectedPositions.add(index)
            }
        }

        if (recyclerView.itemDecorationCount > 0) {
            recyclerView.removeItemDecorationAt(0)
        }
    }

    private fun toggleItemSelection(select: Boolean, pos: Int) {
        if (select) {
            if (itemViews[pos] != null) {
                selectedPositions.add(pos)
            }
        } else {
            selectedPositions.remove(pos)
        }

        itemViews[pos]?.contact_checkbox?.isChecked = select
    }

    fun getSelectedItemsSet(): HashSet<Contact> {
        val selectedItemsSet = HashSet<Contact>(selectedPositions.size)
        selectedPositions.forEach { selectedItemsSet.add(contacts[it]) }
        return selectedItemsSet
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = activity.layoutInflater.inflate(itemLayout, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val contact = contacts[position]
        itemViews.put(position, holder.bindView(contact))
        toggleItemSelection(selectedPositions.contains(position), position)
    }

    override fun getItemCount() = contacts.size

    fun updateItems(newItems: ArrayList<Contact>, highlightText: String = "") {
        if (newItems.hashCode() != contacts.hashCode()) {
            contacts = newItems.clone() as ArrayList<Contact>
            textToHighlight = highlightText
            notifyDataSetChanged()
        } else if (textToHighlight != highlightText) {
            textToHighlight = highlightText
            notifyDataSetChanged()
        }
        fastScroller.measureRecyclerView()
    }

    override fun onViewRecycled(holder: ViewHolder) {
        super.onViewRecycled(holder)
        if (!activity.isDestroyed && !activity.isFinishing) {
            Glide.with(activity).clear(holder.itemView.contact_tmb)
        }
    }

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        fun bindView(contact: Contact): View {
            itemView.apply {
                contact_checkbox.beVisibleIf(allowPickMultiple)
                contact_checkbox.setColors(config.textColor, context.getAdjustedPrimaryColor(), config.backgroundColor)
                val textColor = config.textColor

                val fullName = contact.getNameToDisplay()
                contact_name.text = if (textToHighlight.isEmpty()) fullName else {
                    if (fullName.contains(textToHighlight, true)) {
                        fullName.highlightTextPart(textToHighlight, adjustedPrimaryColor)
                    } else {
                        highlightTextFromNumbers(fullName, textToHighlight, adjustedPrimaryColor)
                    }
                }

                contact_name.setTextColor(textColor)
                contact_name.setTextSize(TypedValue.COMPLEX_UNIT_PX, fontSize)

                if (contact_number != null) {
                    val phoneNumberToUse = if (textToHighlight.isEmpty()) {
                        contact.phoneNumbers.firstOrNull()
                    } else {
                        contact.phoneNumbers.firstOrNull { it.value.contains(textToHighlight) } ?: contact.phoneNumbers.firstOrNull()
                    }

                    val numberText = phoneNumberToUse?.value ?: ""
                    contact_number.text = if (textToHighlight.isEmpty()) numberText else numberText.highlightTextPart(textToHighlight, adjustedPrimaryColor, false, true)
                    contact_number.setTextColor(textColor)
                    contact_number.setTextSize(TypedValue.COMPLEX_UNIT_PX, fontSize)
                }

                contact_frame.setOnClickListener {
                    if (itemClick != null) {
                        itemClick.invoke(contact)
                    } else {
                        viewClicked(!contact_checkbox.isChecked)
                    }
                }

                contact_tmb.beVisibleIf(showContactThumbnails)

                if (showContactThumbnails) {
                    val avatarName = when {
                        contact.isABusinessContact() -> contact.getFullCompany()
                        config.startNameWithSurname -> contact.surname
                        else -> contact.firstName
                    }

                    val placeholderImage = BitmapDrawable(resources, context.getContactLetterIcon(avatarName))

                    if (contact.photoUri.isEmpty() && contact.photo == null) {
                        contact_tmb.setImageDrawable(placeholderImage)
                    } else {
                        val options = RequestOptions()
                            .signature(ObjectKey(contact.getSignatureKey()))
                            .diskCacheStrategy(DiskCacheStrategy.RESOURCE)
                            .error(placeholderImage)
                            .centerCrop()

                        val itemToLoad: Any? = if (contact.photoUri.isNotEmpty()) {
                            contact.photoUri
                        } else {
                            contact.photo
                        }

                        Glide.with(activity)
                            .load(itemToLoad)
                            .apply(options)
                            .apply(RequestOptions.circleCropTransform())
                            .into(contact_tmb)
                    }
                }
            }

            return itemView
        }

        private fun viewClicked(select: Boolean) {
            toggleItemSelection(select, adapterPosition)
        }
    }
}
