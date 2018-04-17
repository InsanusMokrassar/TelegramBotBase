package com.github.insanusmokrassar.TelegramBotBase.extensions

import com.github.insanusmokrassar.ConfigsRemapper.ReceiversManager
import com.github.insanusmokrassar.IObjectK.exceptions.ReadException
import com.github.insanusmokrassar.IObjectK.interfaces.CommonIObject
import com.pengrad.telegrambot.TelegramBot

var CommonIObject<String, Any>.bot: TelegramBot?
    get() = try {
        get("bot")
    } catch (e: ReadException) {
        null
    }
    set(value) = value ?.let {
        set("bot", it)
    } ?:let {
        remove("bot")
    }

var CommonIObject<String, Any>.receiversManager: ReceiversManager?
    get() = try {
        get("receiversManager")
    } catch (e: ReadException) {
        null
    }
    set(value) = value ?.let {
        set("receiversManager", it)
    } ?:let {
        remove("receiversManager")
    }