package com.github.insanusmokrassar.TelegramBotBase.models

import com.github.insanusmokrassar.TelegramBotBase.tables.ChatsLanguages
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import java.util.*

private val chatsLanguagesCache = WeakHashMap<Long, ChatLanguage>()

fun chatLanguage(chatId: Long): ChatLanguage {
    return chatsLanguagesCache[chatId] ?: ChatLanguage(chatId).apply {
        chatsLanguagesCache[chatId] = this
    }
}

class ChatLanguage(
        val userId: Long
) {
    var language: String
        get() {
            return transaction {
                ChatsLanguages.select {
                    ChatsLanguages.chatId.eq(userId)
                }.firstOrNull() ?.let {
                    it[ChatsLanguages.language]
                } ?: ChatsLanguages.insert {
                    it[ChatsLanguages.chatId] = userId
                } get ChatsLanguages.language ?: throw IllegalStateException("Can't get language")
            }
        }
        set(value) = transaction {
            ChatsLanguages.update({ ChatsLanguages.chatId.eq(userId) }) {
                it[ChatsLanguages.language] = value
            }
        }
}
