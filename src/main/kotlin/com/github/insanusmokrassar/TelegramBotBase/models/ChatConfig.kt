package com.github.insanusmokrassar.TelegramBotBase.models

import com.github.insanusmokrassar.TelegramBotBase.tables.ChatsConfigs
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import java.util.*

private val cache = WeakHashMap<Long, ChatConfig>()

fun getUserConfig(userId: Long): ChatConfig {
    return cache[userId] ?: ChatConfig(userId).apply {
        cache[userId] = this
    }
}

class ChatConfig private constructor(
        config: String?,
        val chatId: Long
) {
    var config: String? = config
        set(value) {
            transaction {
                ChatsConfigs.update({ ChatsConfigs.chatId.eq(chatId) }) {
                    it[ChatsConfigs.config] = value
                }
            }
            field = value
        }

    internal constructor(
            userId: Long
    ) : this(
            transaction {
                try {
                    ChatsConfigs.select {
                        ChatsConfigs.chatId.eq(userId)
                    }.first().let {
                        it[ChatsConfigs.config]
                    }
                } catch (e: NoSuchElementException) {
                    ChatsConfigs.insert {
                        it[ChatsConfigs.chatId] = userId
                    }
                    null
                }
            },
            userId
    )
}
