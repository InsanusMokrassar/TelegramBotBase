package com.github.insanusmokrassar.TelegramBotBase.utils

import com.github.insanusmokrassar.IObjectK.interfaces.IObject
import com.github.insanusmokrassar.IObjectKRealisations.toIObject
import com.pengrad.telegrambot.TelegramBot
import com.pengrad.telegrambot.UpdatesListener
import com.pengrad.telegrambot.model.*
import kotlinx.coroutines.experimental.async
import java.util.logging.Logger

typealias UpdateCallback = (updateId: Int, message: IObject<Any>) -> Unit
typealias MediaGroupCallback = (mediaGroupId: String, message: List<IObject<Any>>) -> Unit

class BotIncomeMessagesListener(
        bot: TelegramBot,
        private val onMessage: UpdateCallback = { _, _ -> },
        private val onMessageEdited: UpdateCallback = { _, _ -> },
        private val onChannelPost: UpdateCallback = { _, _ -> },
        private val onChannelPostEdited: UpdateCallback = { _, _ -> },
        private val onInlineQuery: UpdateCallback = { _, _ -> },
        private val onChosenInlineResult: UpdateCallback = { _, _ -> },
        private val onCallbackQuery: UpdateCallback = { _, _ -> },
        private val onShippingQuery: UpdateCallback = { _, _ -> },
        private val onPreCheckoutQuery: UpdateCallback = { _, _ -> },
        private val onMediaGroup: MediaGroupCallback? = null
) {
    init {
        onMediaGroup ?.let {
            onMediaGroup ->
            bot.setUpdatesListener {
                var read = 0
                val groups = HashMap<String, MutableList<IObject<Any>>>()
                it.forEach {
                    update ->
                    try {
                        println("Update: ${update.toIObject()}")
                        val updateIObject = update.toIObject()
                        update.message() ?.let {
                            message ->
                            message.mediaGroupId() ?.let {
                                groupId ->
                                (groups[groupId] ?:let {
                                    ArrayList<IObject<Any>>().apply {
                                        groups[groupId] = this
                                    }
                                }).add(updateIObject)
                            } ?:let {
                                onMessage(update.updateId(), updateIObject)
                            }
                        } ?: update.editedMessage() ?.let {
                            onMessageEdited(update.updateId(), updateIObject)
                        } ?: update.channelPost() ?.let {
                            onChannelPost(update.updateId(), updateIObject)
                        } ?: update.editedChannelPost() ?.let {
                            onChannelPostEdited(update.updateId(), updateIObject)
                        } ?: update.inlineQuery() ?.let {
                            onInlineQuery(update.updateId(), updateIObject)
                        } ?: update.chosenInlineResult() ?.let {
                            onChosenInlineResult(update.updateId(), updateIObject)
                        } ?: update.callbackQuery() ?.let {
                            onCallbackQuery(update.updateId(), updateIObject)
                        } ?: update.shippingQuery() ?.let {
                            onShippingQuery(update.updateId(), updateIObject)
                        } ?: update.preCheckoutQuery() ?.let {
                            onPreCheckoutQuery(update.updateId(), updateIObject)
                        } ?:let {
                            Logger.getGlobal().warning("${this::class.java.simpleName} can't handle update: ${update.toIObject()}")
                        }
                    } catch (e: Throwable) {
                        return@setUpdatesListener read
                    }
                    read++
                }
                while (groups.isNotEmpty()) {
                    val current = groups.entries.first()
                    try {
                        onMediaGroup(current.key, current.value)
                    } catch (e: Exception) {
                        continue
                    }
                    groups.remove(current.key)
                }
                UpdatesListener.CONFIRMED_UPDATES_ALL
            }
        } ?:let {
            bot.setUpdatesListener {
                var read = 0
                it.forEach {
                    update ->
                    try {
                        println("Update: ${update.toIObject()}")
                        val updateIObject = update.toIObject()
                        update.message() ?.let {
                            onMessage(update.updateId(), updateIObject)
                        } ?: update.editedMessage() ?.let {
                            onMessageEdited(update.updateId(), updateIObject)
                        } ?: update.channelPost() ?.let {
                            onChannelPost(update.updateId(), updateIObject)
                        } ?: update.editedChannelPost() ?.let {
                            onChannelPostEdited(update.updateId(), updateIObject)
                        } ?: update.inlineQuery() ?.let {
                            onInlineQuery(update.updateId(), updateIObject)
                        } ?: update.chosenInlineResult() ?.let {
                            onChosenInlineResult(update.updateId(), updateIObject)
                        } ?: update.callbackQuery() ?.let {
                            onCallbackQuery(update.updateId(), updateIObject)
                        } ?: update.shippingQuery() ?.let {
                            onShippingQuery(update.updateId(), updateIObject)
                        } ?: update.preCheckoutQuery() ?.let {
                            onPreCheckoutQuery(update.updateId(), updateIObject)
                        } ?:let {
                            Logger.getGlobal().warning("${this::class.java.simpleName} can't handle update: ${update.toIObject()}")
                        }
                    } catch (e: Throwable) {
                        return@setUpdatesListener read
                    }
                    read++
                }
                UpdatesListener.CONFIRMED_UPDATES_ALL
            }
        }
    }
}