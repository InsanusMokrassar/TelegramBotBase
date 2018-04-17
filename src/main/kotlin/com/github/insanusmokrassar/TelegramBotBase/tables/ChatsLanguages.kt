package com.github.insanusmokrassar.TelegramBotBase.tables

import org.jetbrains.exposed.sql.Table

object ChatsLanguages : Table() {
    val id = integer("id").primaryKey().autoIncrement()
    val chatId = long("chatId").uniqueIndex()
    val language = text("language").default("English")
}
