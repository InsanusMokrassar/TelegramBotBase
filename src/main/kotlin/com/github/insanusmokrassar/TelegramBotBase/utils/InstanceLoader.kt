package com.github.insanusmokrassar.TelegramBotBase.utils

import com.github.insanusmokrassar.ConfigsRemapper.ConfigWrapper
import com.github.insanusmokrassar.ConfigsRemapper.extract

class InstanceLoader : ConfigWrapper() {
    val classpath: String = ""

    fun <T> tryToLoad(): T? {
        return try {
            extract(classpath, paramsObjectInstance())
        } catch (e: IllegalArgumentException) {
            null
        }
    }
}
