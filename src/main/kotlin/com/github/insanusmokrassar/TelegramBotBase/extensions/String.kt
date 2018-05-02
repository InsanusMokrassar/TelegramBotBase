package com.github.insanusmokrassar.TelegramBotBase.extensions

const val maxMessageCharacters = 4096

fun String.breakByLength(): Iterable<String> {
    var leftToBreak = this
    val breakedParts = ArrayList<String>()
    while (leftToBreak.isNotEmpty()) {
        val toIndex = if (maxMessageCharacters > leftToBreak.length) {
            leftToBreak.length
        } else {
            maxMessageCharacters
        }
        val part = leftToBreak.substring(0, toIndex)
        leftToBreak = leftToBreak.substring(toIndex)
        breakedParts.add(part)
    }
    return breakedParts
}
