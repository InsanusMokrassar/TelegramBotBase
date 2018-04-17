package com.github.insanusmokrassar.TelegramBotBase.utils.strings

import com.github.insanusmokrassar.IObjectK.interfaces.IObject
import com.github.insanusmokrassar.IObjectKRealisations.toObject

class StringsHolder<out T: Strings>(
        private val baseStringsClass: Class<T>,
        vararg from: IObject<Any>
) {
    private val languages = mutableMapOf<String?, T>()

    init {
        from.map {
            it.toObject(baseStringsClass)
        }.forEach {
            languages[it.language] = it
        }
    }

    fun languagePack(language: String? = null): T {
        return languages[language] ?: languages[null] ?: throw IllegalStateException("Can't find default strings")
    }

    fun availableLanguages(): List<String> {
        return languages.keys.filterNotNull()
    }
}