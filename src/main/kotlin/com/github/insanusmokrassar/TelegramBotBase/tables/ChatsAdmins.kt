package com.github.insanusmokrassar.TelegramBotBase.tables

import org.jetbrains.exposed.sql.Table

object ChatsAdmins : Table() {
    val chatId = long("chatId").primaryKey()
    val userId = long("userId").primaryKey()
}