package com.simplemobiletools.contacts.pro.adapters

import android.util.SparseArray
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
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
    private val textColor = config.textColor
    private val adjustedPrimaryColor = activity.getAdjustedPrimaryColor()
    private val fontSize = activity.getTextSize()

    private val contactDrawable = activity.resources.getColoredDrawableWithColor(R.drawable.ic_person_vector, textColor)
    private val showContactThumbnails = config.showContactThumbnails
    private val showPhoneNumbers = config.showPhoneNumbers
    private val itemLayout = if (showPhoneNumbers) R.layout.item_add_favorite_with_number else R.layout.item_add_favorite_without_number
    private var textToHighlight = ""

    private var smallPadding = activity.resources.getDimension(R.dimen.small_margin).toInt()
    private var mediumPadding = activity.resources.getDimension(R.dimen.medium_margin).toInt()
    private var bigPadding = activity.resources.getDimension(R.dimen.normal_margin).toInt()

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
                if (!showContactThumbnails && !showPhoneNumbers) {
                    contact_name.setPadding(bigPadding, bigPadding, bigPadding, bigPadding)
                } else {
                    contact_name.setPadding(if (showContactThumbnails) smallPadding else bigPadding, smallPadding, smallPadding, 0)
                }

                if (contact_number != null) {
                    val phoneNumberToUse = if (textToHighlight.isEmpty()) {
                        contact.phoneNumbers.firstOrNull()
                    } else {
                        contact.phoneNumbers.firstOrNull { it.value.contains(textToHighlight) } ?: contact.phoneNumbers.firstOrNull()
                    }

                    val numberText = phoneNumberToUse?.value ?: ""
                    contact_number.text = if (textToHighlight.isEmpty()) numberText else numberText.highlightTextPart(textToHighlight, adjustedPrimaryColor, false, true)
                    contact_number.setTextColor(textColor)
                    contact_number.setPadding(if (showContactThumbnails) smallPadding else bigPadding, 0, smallPadding, 0)
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
                    if (contact.photoUri.isNotEmpty()) {
                        val options = RequestOptions()
                                .signature(ObjectKey(contact.photoUri))
                                .diskCacheStrategy(DiskCacheStrategy.RESOURCE)
                                .error(contactDrawable)
                                .centerCrop()

                        if (!activity.isDestroyed && !activity.isFinishing) {
                            Glide.with(activity)
                                    .load(contact.photoUri)
                                    .transition(DrawableTransitionOptions.withCrossFade())
                                    .apply(options)
                                    .apply(RequestOptions.circleCropTransform())
                                    .into(contact_tmb)
                            contact_tmb.setPadding(smallPadding, smallPadding, smallPadding, smallPadding)
                        }
                    } else if (contact.photo != null) {
                        val options = RequestOptions()
                                .signature(ObjectKey(contact.hashCode()))
                                .diskCacheStrategy(DiskCacheStrategy.RESOURCE)
                                .error(contactDrawable)
                                .centerCrop()

                        Glide.with(activity)
                                .load(contact.photo)
                                .transition(DrawableTransitionOptions.withCrossFade())
                                .apply(options)
                                .apply(RequestOptions.circleCropTransform())
                                .into(contact_tmb)
                        contact_tmb.setPadding(smallPadding, smallPadding, smallPadding, smallPadding)
                    } else {
                        contact_tmb.setPadding(mediumPadding, mediumPadding, mediumPadding, mediumPadding)
                        contact_tmb.setImageDrawable(contactDrawable)
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
