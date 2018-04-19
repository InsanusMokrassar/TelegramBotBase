package com.github.insanusmokrassar.TelegramBotBase

import com.github.insanusmokrassar.IObjectK.interfaces.IObject
import com.github.insanusmokrassar.IObjectKRealisations.toObject
import com.github.insanusmokrassar.TelegramBotBase.utils.load
import com.xenomachina.argparser.ArgParser
import com.xenomachina.argparser.ShowHelpException
import com.xenomachina.argparser.default

const val databaseConfigFilename: String = "database_config.json"
const val defaultConfigFilename: String = "default_config.json"
const val defaultUserIdRemapFilename: String = "user_id_remap_rules.json"
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
    val userIdRemapFile by parser.storing(
            "File name to user id remap rules"
    ).default(defaultUserIdRemapFilename)
}

class Config {
    val receiversConfigs: List<IObject<Any>> = emptyList()
}

fun main(args: Array<String>) {
    val parser = try {
        ArgParser(args).parseInto(::LauncherArgumentsParser)
    } catch (e: ShowHelpException) {
        e.printAndExit()
    }
    val executor = Executor(
            load(parser.configFile).run { toObject(Config::class.java) },
            parser.token,
            load(parser.defaultUserConfigFilename),
            load(parser.databaseConfigFile).toObject(DatabaseConfig::class.java),
            load(parser.userIdRemapFile),
            parser.debug
    )
}
