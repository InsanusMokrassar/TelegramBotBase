package com.github.insanusmokrassar.TelegramBotBase.tables

import org.jetbrains.exposed.sql.Table

object ChatsConfigs: Table() {
    val chatId = long("chatId").primaryKey()
    val config = text("config").nullable()
}