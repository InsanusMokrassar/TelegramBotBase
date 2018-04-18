package com.github.insanusmokrassar.TelegramBotBase.extensions

import com.github.insanusmokrassar.ConfigsRemapper.ReceiversManager
import com.github.insanusmokrassar.IObjectK.exceptions.ReadException
import com.github.insanusmokrassar.IObjectK.interfaces.CommonIObject
import com.github.insanusmokrassar.TelegramBotBase.Executor
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

var CommonIObject<String, Any>.executor: Executor?
    get() = try {
        get("executor")
    } catch (e: ReadException) {
        null
    }
    set(value) = value ?.let {
        set("executor", it)
    } ?:let {
        remove("executor")
    }