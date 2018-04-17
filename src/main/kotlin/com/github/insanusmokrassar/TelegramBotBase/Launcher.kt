package com.github.insanusmokrassar.TelegramBotBase

import com.github.insanusmokrassar.IObjectK.interfaces.IObject
import com.github.insanusmokrassar.IObjectKRealisations.toObject
import com.github.insanusmokrassar.TelegramBotBase.utils.InstanceLoader
import com.github.insanusmokrassar.TelegramBotBase.utils.load
import com.pengrad.telegrambot.model.*
import com.xenomachina.argparser.ArgParser
import com.xenomachina.argparser.ShowHelpException
import com.xenomachina.argparser.default

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
    val databaseConfigFile by parser.storing(
            "File name to database config"
    ).default(databaseConfigFilename)
}

abstract class LaunchConfigTemplate {
    open val receiversConfigs: List<IObject<Any>> = emptyList()

    open val onMessage: (updateId: Int, message: Message) -> Unit = { _, _ -> }
    open val onMessageEdited: (updateId: Int, message: Message) -> Unit = { _, _ -> }
    open val onChannelPost: (updateId: Int, message: Message) -> Unit = { _, _ -> }
    open val onChannelPostEdited: (updateId: Int, message: Message) -> Unit = { _, _ -> }
    open val onInlineQuery: (updateId: Int, query: InlineQuery) -> Unit = { _, _ -> }
    open val onChosenInlineResult: (updateId: Int, result: ChosenInlineResult) -> Unit = { _, _ -> }
    open val onCallbackQuery: (updateId: Int, query: CallbackQuery) -> Unit = { _, _ -> }
    open val onShippingQuery: (updateId: Int, query: ShippingQuery) -> Unit = { _, _ -> }
    open val onPreCheckoutQuery: (updateId: Int, query: PreCheckoutQuery) -> Unit = { _, _ -> }
}

private class Config : LaunchConfigTemplate() {
    override val receiversConfigs: List<IObject<Any>> = emptyList()
    
    val onMessageConfig = InstanceLoader()
    override val onMessage: (updateId: Int, message: Message) -> Unit
        get() = onMessageConfig.tryToLoad() ?: { _, _ -> }
    
    val onMessageEditedConfig = InstanceLoader()
    override val onMessageEdited: (updateId: Int, message: Message) -> Unit
        get() = onMessageEditedConfig.tryToLoad() ?: { _, _ -> }

    val onChannelPostConfig = InstanceLoader()
    override val onChannelPost: (updateId: Int, message: Message) -> Unit
        get() = onChannelPostConfig.tryToLoad() ?: { _, _ -> }

    val onChannelPostEditedConfig = InstanceLoader()
    override val onChannelPostEdited: (updateId: Int, message: Message) -> Unit
        get() = onChannelPostEditedConfig.tryToLoad() ?: { _, _ -> }
    
    val onInlineQueryConfig = InstanceLoader()
    override val onInlineQuery: (updateId: Int, query: InlineQuery) -> Unit
        get() = onInlineQueryConfig.tryToLoad() ?: { _, _ -> }
    
    val onChosenInlineResultConfig = InstanceLoader()
    override val onChosenInlineResult: (updateId: Int, result: ChosenInlineResult) -> Unit
        get() = onChosenInlineResultConfig.tryToLoad() ?: { _, _ -> }
    
    val onCallbackQueryConfig = InstanceLoader()
    override val onCallbackQuery: (updateId: Int, query: CallbackQuery) -> Unit
        get() = onCallbackQueryConfig.tryToLoad() ?: { _, _ -> }
    
    val onShippingQueryConfig = InstanceLoader()
    override val onShippingQuery: (updateId: Int, query: ShippingQuery) -> Unit
        get() = onShippingQueryConfig.tryToLoad() ?: { _, _ -> }
    
    val onPreCheckoutQueryConfig = InstanceLoader()
    override val onPreCheckoutQuery: (updateId: Int, query: PreCheckoutQuery) -> Unit
        get() = onPreCheckoutQueryConfig.tryToLoad() ?: { _, _ -> } 
}

fun main(args: Array<String>) {
    val parser = try {
        ArgParser(args).parseInto(::LauncherArgumentsParser)
    } catch (e: ShowHelpException) {
        e.printAndExit()
    }
    val executor = Executor(
            load(parser.configFile).run { toObject(Config::class.java) },
            load(parser.defaultUserConfigFilename),
            load(parser.databaseConfigFile).toObject(DatabaseConfig::class.java),
            parser.token,
            parser.debug
    )
}
