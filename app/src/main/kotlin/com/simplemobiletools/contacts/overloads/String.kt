package com.simplemobiletools.contacts.overloads

operator fun String.times(x: Int): String {
    val stringBuffer = StringBuffer()
    for (i in 1..x) {
        stringBuffer.append(this)
    }
    return stringBuffer.toString()
}
