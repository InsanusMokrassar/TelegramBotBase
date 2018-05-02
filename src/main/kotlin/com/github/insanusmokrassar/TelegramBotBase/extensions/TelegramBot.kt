package com.github.insanusmokrassar.TelegramBotBase.extensions

import com.github.insanusmokrassar.TelegramBotBase.models.ChatAdmins
import com.pengrad.telegrambot.Callback
import com.pengrad.telegrambot.TelegramBot
import com.pengrad.telegrambot.model.ChatMember
import com.pengrad.telegrambot.model.request.InlineKeyboardMarkup
import com.pengrad.telegrambot.model.request.ParseMode
import com.pengrad.telegrambot.request.*
import com.pengrad.telegrambot.response.BaseResponse
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.delay
import org.slf4j.LoggerFactory
import java.io.IOException
import java.lang.ref.WeakReference

private val logger = LoggerFactory.getLogger("TelegramAsyncExecutions")

private class DefaultCallback<T: BaseRequest<T, R>, R: BaseResponse>(
        private val onFailureCallback: ((T, IOException?) -> Unit)?,
        private val onResponseCallback: ((T, R) -> Unit)?,
        bot: TelegramBot,
        private var retries: Int? = 0,
        private val retriesDelay: Long = 1000L
) : Callback<T, R> {
    private val bot = WeakReference(bot)
    override fun onFailure(request: T, e: IOException?) {
        logger.warn("Request failure: {}; Error: {}", request, e)
        onFailureCallback ?. invoke(request, e)
        (retries ?.let {
            it > 0
        } ?: true).let {
            if (it) {
                async {
                    delay(retriesDelay)
                    bot.get() ?. executeAsync(
                            request,
                            onFailureCallback,
                            onResponseCallback,
                            retries ?. minus(1),
                            retriesDelay
                    )
                }
            }
        }
    }

    override fun onResponse(request: T, response: R) {
        logger.info("Request success: {}\nResponse: {}", request, response)
        if (response.isOk) {
            onResponseCallback ?. invoke(request, response)
        } else {
            onFailure(request, IOException(response.description()))
        }
    }
}

val REPEATS_INFINITY: Int? = null

fun <T: BaseRequest<T, R>, R: BaseResponse> TelegramBot.executeAsync(
        request: T,
        onFailure: ((T, IOException?) -> Unit)? = null,
        onResponse: ((T, R) -> Unit)? = null,
        retries: Int? = 0,
        retriesDelay: Long = 1000L
) {
    logger.info("Try to put request for executing: {}", request)
    execute(
            request,
            DefaultCallback(
                    onFailure,
                    onResponse,
                    this,
                    retries,
                    retriesDelay
            )
    )
}

fun TelegramBot.queryAnswer(
        id: String,
        answerText: String,
        asAlert: Boolean = false
) {
    executeAsync(
            AnswerCallbackQuery(
                    id
            )
                    .text(answerText)
                    .showAlert(asAlert)
    )
}

typealias ChatMemberCallback = (ChatMember) -> Unit

fun TelegramBot.updateAdmins(channelChatId: Long): List<ChatMember> {
    return execute(
            GetChatAdministrators(
                    channelChatId
            )
    ).apply {
        ChatAdmins(
                channelChatId
        ).updateAdmins(
                administrators().map {
                    it.user().id().toLong()
                }
        )
    }.administrators()
}

fun TelegramBot.checkUserIsAdmin(
        userId: Int,
        channelId: Long,
        isAdminCallback: ChatMemberCallback
) {
    async {
        updateAdmins(channelId).let {
            administrators ->
            administrators.firstOrNull {
                it.user().id() == userId
            } ?.let {
                isAdminCallback(it)
            }
        }
    }
}

fun TelegramBot.chatCreator(
        channelId: Long,
        creatorCallback: ChatMemberCallback
) {
    async {
        updateAdmins(
                channelId
        ).let {
            it.firstOrNull {
                it.status() == ChatMember.Status.creator
            } ?.let(creatorCallback)
        }
    }
}

fun TelegramBot.sendEditExistOrNew(
        chatId: Long,
        messageId: Int? = null,
        text: String? = null,
        markup: InlineKeyboardMarkup = InlineKeyboardMarkup(),
        success: (() -> Unit)? = null
) {
    messageId ?.let {
        text ?.let {
            executeAsync(
                    EditMessageText(
                            chatId, messageId, text
                    ).replyMarkup(markup).parseMode(ParseMode.Markdown),
                    onFailure = {
                        _, e ->
                        if (e ?. message ?. contains("message is not modified") == true) {
                            sendEditExistOrNew(
                                    chatId, messageId, null, markup, success
                            )
                        } else {
                            sendEditExistOrNew(
                                    chatId,
                                    null,
                                    text,
                                    markup,
                                    success
                            )
                        }
                    },
                    onResponse = {
                        req, res ->
                        success ?. invoke()
                    }
            )
        } ?:let {
            executeAsync(
                    EditMessageReplyMarkup(
                            chatId, messageId
                    ).replyMarkup(markup),
                    onFailure = {
                        _, e ->
                        if (e ?. message ?. contains("message is not modified") == true) {
                            success ?. invoke()
                        } else {
                            sendEditExistOrNew(
                                    chatId,
                                    null,
                                    text,
                                    markup,
                                    success
                            )
                        }
                    },
                    onResponse = {
                        req, res ->
                        success ?. invoke()
                    }
            )
        }
    } ?:let {
        text ?.let {
            executeAsync(
                    SendMessage(
                            chatId, text
                    ).replyMarkup(markup).parseMode(ParseMode.Markdown),
                    onResponse = {
                        req, res ->
                        success ?. invoke()
                    }
            )
        }
    }
}

