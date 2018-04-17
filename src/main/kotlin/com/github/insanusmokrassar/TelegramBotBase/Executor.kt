package com.github.insanusmokrassar.TelegramBotBase

import com.github.insanusmokrassar.ConfigsRemapper.ReceiversManager
import com.github.insanusmokrassar.IObjectK.exceptions.ReadException
import com.github.insanusmokrassar.IObjectK.interfaces.CommonIObject
import com.github.insanusmokrassar.IObjectK.interfaces.IObject
import com.github.insanusmokrassar.IObjectK.realisations.SimpleIObject
import com.github.insanusmokrassar.IObjectK.utils.plus
import com.github.insanusmokrassar.IObjectKRealisations.readIObject
import com.github.insanusmokrassar.IObjectKRealisations.toIObject
import com.github.insanusmokrassar.TelegramBotBase.extensions.bot
import com.github.insanusmokrassar.TelegramBotBase.extensions.receiversManager
import com.github.insanusmokrassar.TelegramBotBase.models.ChatConfig
import com.github.insanusmokrassar.TelegramBotBase.models.QueryData
import com.github.insanusmokrassar.TelegramBotBase.tables.ChatsConfigs
import com.github.insanusmokrassar.TelegramBotBase.tables.ChatsLanguages
import com.github.insanusmokrassar.TelegramBotBase.tables.QueryDatas
import com.github.insanusmokrassar.TelegramBotBase.utils.BotIncomeMessagesListener
import com.pengrad.telegrambot.TelegramBot
import kotlinx.coroutines.experimental.async
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.logging.Logger

private fun initDatabase(
        config: DatabaseConfig,
        vararg additionalExposedDatabases: Table
) {
    config.apply {
        Database.connect(url, driver, username, password)
    }

    transaction {
        SchemaUtils.create(
                ChatsConfigs,
                QueryDatas,
                ChatsLanguages,
                *additionalExposedDatabases
        )
    }
}


private fun CommonIObject<String, Any>.asIObject(): IObject<Any> {
    return SimpleIObject().also {
        receiver ->
        keys().forEach {
            receiver[it] = get(it)
        }
    }
}

class Executor(
        config: LaunchConfigTemplate,
        private val defaultUserConfig: IObject<Any>,
        databaseConfig: DatabaseConfig,
        token: String,
        isDebug: Boolean = false,
        vararg additionalExposedDatabases: Table
) {
    private val receiversManager = ReceiversManager(
            *config.receiversConfigs.toTypedArray()
    )
    private val configCallbackQuery = config.onCallbackQuery
    private val bot = TelegramBot.Builder(token).apply {
        if (isDebug) {
            debug()
        }
    }.build().also {
        bot ->
        BotIncomeMessagesListener(
                bot,
                config.onMessage,
                config.onMessageEdited,
                config.onChannelPostEdited,
                config.onChannelPostEdited,
                config.onInlineQuery,
                config.onChosenInlineResult,
                {
                    updateId, query ->
                    val chatId = query.message().chat().id()
                    try {
                        handleUpdate(
                                chatId.toString(),
                                query.toIObject() + QueryData(query.data().toInt()).config.byteInputStream().readIObject()
                        )
                    } catch (e: NoSuchElementException) {
                        println("Can't find query number for: $query")
                    }
                    configCallbackQuery(updateId, query)
                },
                config.onShippingQuery,
                config.onPreCheckoutQuery
        )
    }

    init {
        initDatabase(databaseConfig, *additionalExposedDatabases)
    }

    fun handleUpdate(
            chatId: String,
            config: CommonIObject<String, Any>
    ) {
        async {
            try {
                println(config)
                val userConfig = ChatConfig(
                        chatId
                )
                val resultConfig = (userConfig.config ?. byteInputStream() ?.readIObject() ?: defaultUserConfig) + config

                userConfig.config = null

                resultConfig.bot = bot
                resultConfig.receiversManager = receiversManager
                val command: String = try {
                    resultConfig["command"]
                } catch (e: ReadException) {
                    e.printStackTrace()
                    return@async
                }

                receiversManager.handle(
                        command,
                        resultConfig.asIObject()
                )
            } catch (e: Exception) {
                Logger.getGlobal().throwing(
                        "Update listener",
                        "handle update",
                        e
                )
            }
        }
    }
}
