package com.simplemobiletools.contacts.overloads

operator fun String.times(x: Int): String {
    val sb = StringBuilder()
    for (i in 1..x) {
        sb.append(this)
    }
    return sb.toString()
}
