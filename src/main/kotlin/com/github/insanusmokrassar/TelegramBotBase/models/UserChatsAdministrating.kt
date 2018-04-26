package com.github.insanusmokrassar.TelegramBotBase.models

import com.github.insanusmokrassar.TelegramBotBase.tables.ChatsAdmins
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction

class UserChatsAdministrating(
        val userId: Long
) {
    val chats: List<Long>
        get() = transaction {
            ChatsAdmins.select {
                ChatsAdmins.userId.eq(userId)
            }.map {
                it[ChatsAdmins.chatId]
            }
        }

    operator fun contains(chatId: Long): Boolean {
        return transaction {
            !ChatsAdmins.select {
                ChatsAdmins.chatId.eq(chatId).and(ChatsAdmins.userId.eq(userId))
            }.empty()
        }
    }

    operator fun plus(chatId: Long) {
        if (userId in this) {
            return
        }
        transaction {
            ChatsAdmins.insert {
                it[ChatsAdmins.chatId] = chatId
                it[ChatsAdmins.userId] = this@UserChatsAdministrating.userId
            }
        }
    }

    operator fun minus(chatId: Long) {
        transaction {
            ChatsAdmins.deleteWhere {
                ChatsAdmins.chatId.eq(chatId).and(ChatsAdmins.userId.eq(userId))
            }
        }
    }

    fun updateChats(newList: List<Long>) {
        val chats = chats
        val toAdd = newList.filter {
            !chats.contains(it)
        }
        val toDelete = chats.filter {
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