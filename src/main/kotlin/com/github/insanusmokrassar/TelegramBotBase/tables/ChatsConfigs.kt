package com.github.insanusmokrassar.TelegramBotBase.tables

import org.jetbrains.exposed.sql.Table

object ChatsConfigs: Table() {
    val id = integer("id").primaryKey().autoIncrement()
    val chatId = text("chatId").uniqueIndex()
    val config = text("config").nullable()
}