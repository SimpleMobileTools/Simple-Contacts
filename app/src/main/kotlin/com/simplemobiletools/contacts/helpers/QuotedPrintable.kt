package com.simplemobiletools.contacts.helpers

import java.io.ByteArrayOutputStream

// https://alvinalexander.com/java/jwarehouse/android/core/java/com/google/android/mms/pdu/QuotedPrintable.java.shtml
object QuotedPrintable {
    private const val ESCAPE_CHAR: Byte = '='.toByte()
    fun decode(value: String?): String {
        val bytes = value?.toByteArray()
        if (bytes == null || bytes.isEmpty()) {
            return ""
        }

        val buffer = ByteArrayOutputStream()
        var i = 0
        while (i < bytes.size) {
            val b = bytes[i].toInt()
            if (b == ESCAPE_CHAR.toInt()) {
                try {
                    if ('\r' == bytes[i + 1].toChar() && '\n' == bytes[i + 2].toChar()) {
                        i += 2
                        continue
                    }

                    val u = Character.digit(bytes[++i].toChar(), 16)
                    val l = Character.digit(bytes[++i].toChar(), 16)
                    if (u == -1 || l == -1) {
                        return ""
                    }

                    buffer.write(((u shl 4) + l).toChar().toInt())
                } catch (e: ArrayIndexOutOfBoundsException) {
                    return ""
                }

            } else {
                buffer.write(b)
            }
            i++
        }
        return String(buffer.toByteArray())
    }
}
