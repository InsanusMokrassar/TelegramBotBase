package com.github.insanusmokrassar.TelegramBotBase.tables

import org.jetbrains.exposed.sql.Table

object QueryDatas : Table() {
    val id = integer("id").primaryKey().autoIncrement()
    val config = text("config").uniqueIndex()
}