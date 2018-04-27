package com.github.insanusmokrassar.TelegramBotBase.extensions

import com.github.insanusmokrassar.TelegramBotBase.models.ChatAdmins
import com.pengrad.telegrambot.Callback
import com.pengrad.telegrambot.TelegramBot
import com.pengrad.telegrambot.model.ChatMember
import com.pengrad.telegrambot.request.AnswerCallbackQuery
import com.pengrad.telegrambot.request.BaseRequest
import com.pengrad.telegrambot.request.GetChatAdministrators
import com.pengrad.telegrambot.response.BaseResponse
import com.pengrad.telegrambot.response.GetChatAdministratorsResponse
import kotlinx.coroutines.experimental.async
import org.slf4j.LoggerFactory
import java.io.IOException
import java.util.*

private val logger = LoggerFactory.getLogger("TelegramAsyncExecutions")

private class DefaultCallback<T: BaseRequest<T, R>, R: BaseResponse>(
        private val onFailureCallback: ((T, IOException?) -> Unit)?,
        private val onResponseCallback: ((T, R) -> Unit)?
) : Callback<T, R> {
    override fun onFailure(request: T, e: IOException?) {
        logger.warn("Request failure: {}; Error: {}", request, e)
        onFailureCallback ?. invoke(request, e)
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

fun <T: BaseRequest<T, R>, R: BaseResponse> TelegramBot.executeAsync(
        request: T,
        onFailure: ((T, IOException?) -> Unit)? = null,
        onResponse: ((T, R) -> Unit)? = null
) {
    logger.info("Try to put request for executing: {}", request)
    execute(
            request,
            DefaultCallback(
                    onFailure,
                    onResponse
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

private val adminsCache: MutableMap<String, MutableMap<Int, ChatMember>> = HashMap()

fun TelegramBot.updateAdmins(channelChatId: Long): List<ChatMember> {
    return execute(
            GetChatAdministrators(
                    channelChatId
            )
    ).apply {
        if (isOk) {
            ChatAdmins(
                    channelChatId
            ).updateAdmins(
                    administrators().map {
                        it.user().id().toLong()
                    }
            )
        } else {
            updateAdmins(
                    channelChatId
            )
        }
    }.administrators()
}

fun TelegramBot.checkUserIsAdmin(
        userId: Int,
        channelChatId: String,
        isAdminCallback: ChatMemberCallback
) {
    (adminsCache[channelChatId] ?: let {
        WeakHashMap<Int, ChatMember>().also {
            adminsCache[channelChatId] = it
        }
    }).let {
        it[userId] ?.let {
            try {
                isAdminCallback(it)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            return
        }
    }
    async {
        updateAdmins(channelChatId.toLong()).let {
            administrators ->
            administrators.firstOrNull {
                it.user().id() == userId
            } ?.let {
                adminsCache[channelChatId] ?.set (userId, it)
                isAdminCallback(it)
            }
        }
    }
}

fun TelegramBot.chatCreator(
        channelChatId: Long,
        creatorCallback: ChatMemberCallback
) {
    async {
        updateAdmins(
                channelChatId
        ).let {
            it.firstOrNull {
                it.status() == ChatMember.Status.creator
            } ?.let(creatorCallback)
        }
    }
}
