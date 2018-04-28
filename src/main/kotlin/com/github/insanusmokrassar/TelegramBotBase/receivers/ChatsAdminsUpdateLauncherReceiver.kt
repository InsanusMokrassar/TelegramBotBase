package com.github.insanusmokrassar.TelegramBotBase.receivers

import com.github.insanusmokrassar.ConfigsRemapper.Receiver
import com.github.insanusmokrassar.IObjectK.interfaces.IObject
import com.github.insanusmokrassar.TelegramBotBase.extensions.bot
import com.github.insanusmokrassar.TelegramBotBase.extensions.updateAdmins
import com.github.insanusmokrassar.TelegramBotBase.models.ChatAdmins
import com.pengrad.telegrambot.TelegramBot
import kotlinx.coroutines.experimental.Deferred
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.delay
import java.lang.ref.WeakReference
import java.util.logging.Logger

class ChatsAdminsUpdateLauncherReceiverInitConfig {
    val delayTime: Long = 600 * 1000L//default - once in 10 minutes
}

abstract class ChatsAdminsUpdateLauncherReceiver(
        config: ChatsAdminsUpdateLauncherReceiverInitConfig
) : Receiver {
    private val logger = Logger.getLogger(Receiver::class.java.simpleName)
    private var deferred: Deferred<Unit>? = null
    private var botWR: WeakReference<TelegramBot>? = null
    private val delayTime = config.delayTime
    abstract fun getChannelsToUpdate(): Iterable<Long>
    override fun receive(data: IObject<Any>) {
        data.bot ?.let { botWR = WeakReference(it) } ?: return
        deferred ?:let {
            deferred = async {
                while (true) {
                    botWR ?. get() ?.let {
                        bot ->
                        getChannelsToUpdate().forEach {
                            try {
                                bot.updateAdmins(it)
                            } catch (e: Exception) {
                                logger.throwing(
                                        ChatsAdminsUpdateLauncherReceiver::class.java.simpleName,
                                        "Updating of admins in $it",
                                        e
                                )
                                try {
                                    ChatAdmins(it).updateAdmins(emptyList())
                                } catch (e: Exception) {
                                    logger.throwing(
                                            ChatsAdminsUpdateLauncherReceiver::class.java.simpleName,
                                            "Clear list of admins in $it after error in update",
                                            e
                                    )
                                }
                            }
                        }
                    }
                    delay(delayTime)
                }
            }
        }
    }
}
