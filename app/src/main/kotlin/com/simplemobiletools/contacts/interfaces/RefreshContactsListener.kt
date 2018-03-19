package com.simplemobiletools.contacts.interfaces

interface RefreshContactsListener {
    fun refreshContacts(refreshContactsTab: Boolean, refreshFavoritesTab: Boolean)

    fun refreshFavorites()
}
