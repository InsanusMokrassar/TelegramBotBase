package com.github.insanusmokrassar.TelegramBotBase

import com.github.insanusmokrassar.ConfigsRemapper.ReceiversManager
import com.github.insanusmokrassar.IObjectK.exceptions.ReadException
import com.github.insanusmokrassar.IObjectK.extensions.remap
import com.github.insanusmokrassar.IObjectK.interfaces.CommonIObject
import com.github.insanusmokrassar.IObjectK.interfaces.IObject
import com.github.insanusmokrassar.IObjectK.realisations.SimpleIObject
import com.github.insanusmokrassar.IObjectK.utils.plus
import com.github.insanusmokrassar.IObjectKRealisations.readIObject
import com.github.insanusmokrassar.IObjectKRealisations.toIObject
import com.github.insanusmokrassar.IObjectKRealisations.toObject
import com.github.insanusmokrassar.TelegramBotBase.extensions.bot
import com.github.insanusmokrassar.TelegramBotBase.extensions.executor
import com.github.insanusmokrassar.TelegramBotBase.models.ChatConfig
import com.github.insanusmokrassar.TelegramBotBase.models.QueryData
import com.github.insanusmokrassar.TelegramBotBase.tables.ChatsConfigs
import com.github.insanusmokrassar.TelegramBotBase.tables.ChatsLanguages
import com.github.insanusmokrassar.TelegramBotBase.tables.QueryDatas
import com.github.insanusmokrassar.TelegramBotBase.utils.BotIncomeMessagesListener
import com.github.insanusmokrassar.TelegramBotBase.utils.UpdateCallback
import com.github.insanusmokrassar.TelegramBotBase.utils.load
import com.pengrad.telegrambot.TelegramBot
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

private class DefaultOnUpdateListener(
        private val executor: Executor,
        command: String
) : UpdateCallback {
    private val commandConfig = CommandConfig(command).toIObject()
    override fun invoke(updateId: Int, message: IObject<Any>) {
        executor.handleUpdate(
                message + commandConfig
        )
    }
}

class Executor(
        config: Config,
        token: String,
        private val defaultUserConfig: IObject<Any> = SimpleIObject(),
        databaseConfig: DatabaseConfig = load(databaseConfigFilename).toObject(DatabaseConfig::class.java),
        private val userIdRemapRules: IObject<Any> = load(defaultUserIdRemapFilename),
        isDebug: Boolean = false,
        vararg additionalExposedDatabases: Table
) {
    private val receiversManager = ReceiversManager(
            *config.receiversConfigs.toTypedArray()
    )
    private val bot = TelegramBot.Builder(token).apply {
        if (isDebug) {
            debug()
        }
    }.build().also {
        bot ->
        BotIncomeMessagesListener(
                bot,
                DefaultOnUpdateListener(this, "onMessage"),
                DefaultOnUpdateListener(this, "onMessageEdited"),
                DefaultOnUpdateListener(this, "onChannelPost"),
                DefaultOnUpdateListener(this, "onChannelPostEdited"),
                DefaultOnUpdateListener(this, "onInlineQuery"),
                DefaultOnUpdateListener(this, "onChosenInlineResult"),
                DefaultOnUpdateListener(this, "onCallbackQuery"),
                DefaultOnUpdateListener(this, "onShippingQuery"),
                DefaultOnUpdateListener(this, "onPreCheckoutQuery")
        )
    }

    init {
        initDatabase(databaseConfig, *additionalExposedDatabases)
    }

    fun handleUpdate(
            config: CommonIObject<String, Any>
    ) {
        try {
            println(config)
            val userConfig = (config.toObject(ChatIdContainer::class.java).configChatId ?:let {
                userIdRemapRules.remap(
                        config,
                        config
                )
                config.toObject(ChatIdContainer::class.java).configChatId
            }) ?.let {
                ChatConfig(
                        it.toString()
                ).run {
                    val currentConfig = this.config ?. byteInputStream() ?.readIObject()
                    this.config = null
                    currentConfig
                }
            } ?: defaultUserConfig
            val resultConfig = tryToAddQueryCallback(config + userConfig)

            resultConfig.bot = bot
            resultConfig.executor = this
            val command: String = try {
                resultConfig["command"]
            } catch (e: ReadException) {
                e.printStackTrace()
                return
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

    private fun tryToAddQueryCallback(message: CommonIObject<String, Any>): CommonIObject<String, Any> {
        return try {
            message + QueryData(
                    message.get<IObject<Any>>("callback_query").get<String>("data").toInt()
            ).config.byteInputStream().readIObject()
        } catch (e: Exception) {
            message
        }
    }
}