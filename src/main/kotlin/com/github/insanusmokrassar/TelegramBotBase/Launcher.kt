package com.github.insanusmokrassar.TelegramBotBase

import com.github.insanusmokrassar.ConfigsRemapper.ReceiversManager
import com.github.insanusmokrassar.IObjectK.exceptions.ReadException
import com.github.insanusmokrassar.IObjectK.interfaces.CommonIObject
import com.github.insanusmokrassar.IObjectK.interfaces.IObject
import com.github.insanusmokrassar.IObjectK.realisations.SimpleIObject
import com.github.insanusmokrassar.IObjectK.utils.plus
import com.github.insanusmokrassar.IObjectKRealisations.readIObject
import com.github.insanusmokrassar.IObjectKRealisations.toIObject
import com.github.insanusmokrassar.IObjectKRealisations.toObject
import com.github.insanusmokrassar.TelegramBotBase.extensions.bot
import com.github.insanusmokrassar.TelegramBotBase.extensions.receiversManager
import com.github.insanusmokrassar.TelegramBotBase.models.QueryData
import com.github.insanusmokrassar.TelegramBotBase.models.ChatConfig
import com.github.insanusmokrassar.TelegramBotBase.tables.ChatsConfigs
import com.github.insanusmokrassar.TelegramBotBase.tables.ChatsLanguages
import com.github.insanusmokrassar.TelegramBotBase.tables.QueryDatas
import com.github.insanusmokrassar.TelegramBotBase.utils.BotIncomeMessagesListener
import com.github.insanusmokrassar.TelegramBotBase.utils.InstanceLoader
import com.github.insanusmokrassar.TelegramBotBase.utils.load
import com.pengrad.telegrambot.TelegramBot
import com.pengrad.telegrambot.model.*
import com.xenomachina.argparser.ArgParser
import com.xenomachina.argparser.ShowHelpException
import com.xenomachina.argparser.default
import kotlinx.coroutines.experimental.async
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils.create
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.logging.Logger

const val databaseConfigFilename: String = "database_config.json"
const val defaultConfigFilename: String = "default_config.json"
const val configFilename: String = "config.json"

private class LauncherArgumentsParser(parser: ArgParser) {
    val token by parser.positional("bot api token such as 123456789:ABCDEFGHIJKLMNOPQRSTUVWXYZABCDEFGHI")
    val debug by parser.flagging("enable/disable debug mode of bot")
    val configFile by parser.storing("File name of config").default(configFilename)
    val defaultUserConfigFilename by parser.storing(
            "File name of config for users which will be used bt default if user will have no his own current commands config"
    ).default(defaultConfigFilename)
}

private data class DatabaseConfig(
        val url: String,
        val driver: String,
        val username: String,
        val password: String
)

private class Config {
    val configs: List<IObject<Any>> = emptyList()
    
    val onMessageConfig = InstanceLoader()
    val onMessage: (updateId: Int, message: Message) -> Unit
        get() = onMessageConfig.tryToLoad() ?: { _, _ -> }
    
    val onMessageEditedConfig = InstanceLoader()
    val onMessageEdited: (updateId: Int, message: Message) -> Unit
        get() = onMessageConfig.tryToLoad() ?: { _, _ -> }
    
    val onChannelPostEditedConfig = InstanceLoader()
    val onChannelPostEdited: (updateId: Int, message: Message) -> Unit
        get() = onChannelPostEditedConfig.tryToLoad() ?: { _, _ -> }
    
    val onInlineQueryConfig = InstanceLoader()
    val onInlineQuery: (updateId: Int, query: InlineQuery) -> Unit
        get() = onInlineQueryConfig.tryToLoad() ?: { _, _ -> }
    
    val onChosenInlineResultConfig = InstanceLoader()
    val onChosenInlineResult: (updateId: Int, result: ChosenInlineResult) -> Unit
        get() = onChosenInlineResultConfig.tryToLoad() ?: { _, _ -> }
    
    val onCallbackQueryConfig = InstanceLoader()
    val onCallbackQuery: (updateId: Int, query: CallbackQuery) -> Unit
        get() = onCallbackQueryConfig.tryToLoad() ?: { _, _ -> }
    
    val onShippingQueryConfig = InstanceLoader()
    val onShippingQuery: (updateId: Int, query: ShippingQuery) -> Unit
        get() = onShippingQueryConfig.tryToLoad() ?: { _, _ -> }
    
    val onPreCheckoutQueryConfig = InstanceLoader()
    val onPreCheckoutQuery: (updateId: Int, query: PreCheckoutQuery) -> Unit
        get() = onPreCheckoutQueryConfig.tryToLoad() ?: { _, _ -> } 
}

private fun initDatabase() {
    ClassLoader.getSystemResourceAsStream(databaseConfigFilename).readIObject().toObject(DatabaseConfig::class.java).apply {
        Database.connect(url, driver, username, password)
    }

    transaction {
        create(
                ChatsConfigs,
                QueryDatas,
                ChatsLanguages
        )
    }
}

fun main(args: Array<String>) {
    val parser = try {
        ArgParser(args).parseInto(::LauncherArgumentsParser)
    } catch (e: ShowHelpException) {
        e.printAndExit()
    }
    initDatabase()
    val config = load(parser.configFile).run {
        toObject(Config::class.java)
    }
    val defaultConfig = load(parser.defaultUserConfigFilename)
    val receiversManager = ReceiversManager(
            *config.configs.toTypedArray()
    )
    val configCallbackQuery = config.onCallbackQuery
    TelegramBot.Builder(parser.token).apply {
        if (parser.debug) {
            debug()
        }
    }.build().let {
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
                                query.toIObject() + QueryData(query.data().toInt()).config.byteInputStream().readIObject(),
                                defaultConfig,
                                bot,
                                receiversManager
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
}

fun handleUpdate(
        chatId: String,
        config: CommonIObject<String, Any>,
        defaultConfig: IObject<Any>,
        bot: TelegramBot,
        receiversManager: ReceiversManager
) {
    async {
        try {
            println(config)
            val userConfig = ChatConfig(
                    chatId
            )
            val resultConfig = (userConfig.config ?. byteInputStream() ?.readIObject() ?: defaultConfig) + config

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

private fun CommonIObject<String, Any>.asIObject(): IObject<Any> {
    return SimpleIObject().also {
        receiver ->
        keys().forEach {
            receiver[it] = get(it)
        }
    }
}
