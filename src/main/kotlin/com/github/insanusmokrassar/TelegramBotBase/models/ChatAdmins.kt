package com.github.insanusmokrassar.TelegramBotBase.models

import com.github.insanusmokrassar.TelegramBotBase.tables.ChatsAdmins
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction

class ChatAdmins(
        val chatId: Long
) {
    val admins: List<Long>
        get() = transaction {
            ChatsAdmins.select {
                ChatsAdmins.chatId.eq(chatId)
            }.map {
                it[ChatsAdmins.userId]
            }
        }

    operator fun contains(userId: Long): Boolean {
        return transaction {
            !ChatsAdmins.select {
                ChatsAdmins.chatId.eq(chatId).and(ChatsAdmins.userId.eq(userId))
            }.empty()
        }
    }

    operator fun plus(userId: Long) {
        if (userId in this) {
            return
        }
        transaction {
            ChatsAdmins.insert {
                it[ChatsAdmins.chatId] = this@ChatAdmins.chatId
                it[ChatsAdmins.userId] = userId
            }
        }
    }

    operator fun minus(userId: Long) {
        transaction {
            ChatsAdmins.deleteWhere {
                ChatsAdmins.chatId.eq(chatId).and(ChatsAdmins.userId.eq(userId))
            }
        }
    }

    fun updateAdmins(newList: List<Long>) {
        val admins = admins
        val toAdd = newList.filter {
            !admins.contains(it)
        }
        val toDelete = admins.filter {
            !newList.contains(it)
        }
        toDelete.forEach {
            this - it
        }
        toAdd.forEach {
            this + it
        }
    }
}