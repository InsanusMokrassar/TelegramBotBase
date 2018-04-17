package com.github.insanusmokrassar.TelegramBotBase.utils

import com.github.insanusmokrassar.IObjectK.interfaces.IObject
import com.github.insanusmokrassar.IObjectKRealisations.readIObject
import java.io.FileInputStream

fun load(filename: String) : IObject<Any> {
    return (ClassLoader.getSystemResourceAsStream(filename) ?: FileInputStream(filename)).readIObject()
}
