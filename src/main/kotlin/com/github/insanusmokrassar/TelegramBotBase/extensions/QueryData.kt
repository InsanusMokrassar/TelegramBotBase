package com.github.insanusmokrassar.TelegramBotBase.extensions

import com.github.insanusmokrassar.TelegramBotBase.models.QueryData
import com.pengrad.telegrambot.model.request.InlineKeyboardButton

fun QueryData.toInlineKeyboardButton(text: String): InlineKeyboardButton {
    return InlineKeyboardButton(
            text
    ).callbackData(
            id.toString()
    )
}
