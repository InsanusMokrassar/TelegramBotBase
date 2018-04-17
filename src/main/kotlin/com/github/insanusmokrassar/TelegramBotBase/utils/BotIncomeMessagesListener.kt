package com.github.insanusmokrassar.TelegramBotBase.utils

import com.github.insanusmokrassar.IObjectKRealisations.toIObject
import com.pengrad.telegrambot.TelegramBot
import com.pengrad.telegrambot.UpdatesListener
import com.pengrad.telegrambot.model.*
import java.util.logging.Logger

class BotIncomeMessagesListener(
        bot: TelegramBot,
        private val onMessage: (updateId: Int, message: Message) -> Unit = { _, _ -> },
        private val onMessageEdited: (updateId: Int, message: Message) -> Unit = { _, _ -> },
        private val onChannelPost: (updateId: Int, message: Message) -> Unit = { _, _ -> },
        private val onChannelPostEdited: (updateId: Int, message: Message) -> Unit = { _, _ -> },
        private val onInlineQuery: (updateId: Int, query: InlineQuery) -> Unit = { _, _ -> },
        private val onChosenInlineResult: (updateId: Int, result: ChosenInlineResult) -> Unit = { _, _ -> },
        private val onCallbackQuery: (updateId: Int, query: CallbackQuery) -> Unit = { _, _ -> },
        private val onShippingQuery: (updateId: Int, query: ShippingQuery) -> Unit = { _, _ -> },
        private val onPreCheckoutQuery: (updateId: Int, query: PreCheckoutQuery) -> Unit = { _, _ -> }
) {
    init {
        bot.setUpdatesListener {
            var read = 0
            it.forEach {
                update ->
                try {
                    println("Update: ${update.toIObject()}")
                    update.message() ?.let {
                        onMessage(update.updateId(), it)
                    } ?: update.editedMessage() ?.let {
                        onMessageEdited(update.updateId(), it)
                    } ?: update.channelPost() ?.let {
                        onChannelPost(update.updateId(), it)
                    } ?: update.editedChannelPost() ?.let {
                        onChannelPostEdited(update.updateId(), it)
                    } ?: update.inlineQuery() ?.let {
                        onInlineQuery(update.updateId(), it)
                    } ?: update.chosenInlineResult() ?.let {
                        onChosenInlineResult(update.updateId(), it)
                    } ?: update.callbackQuery() ?.let {
                        onCallbackQuery(update.updateId(), it)
                    } ?: update.shippingQuery() ?.let {
                        onShippingQuery(update.updateId(), it)
                    } ?: update.preCheckoutQuery() ?.let {
                        onPreCheckoutQuery(update.updateId(), it)
                    } ?:let {
                        Logger.getGlobal().warning("${this::class.java.simpleName} can't handle update: ${update.toIObject()}")
                    }
                } catch (e: Exception) {
                    return@setUpdatesListener read
                }
                read++
            }
            UpdatesListener.CONFIRMED_UPDATES_ALL
        }
    }
}