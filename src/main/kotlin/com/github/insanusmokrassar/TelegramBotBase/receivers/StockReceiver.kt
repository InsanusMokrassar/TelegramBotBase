package com.github.insanusmokrassar.TelegramBotBase.receivers

import com.github.insanusmokrassar.ConfigsRemapper.Receiver
import com.github.insanusmokrassar.IObjectK.interfaces.IObject
import com.github.insanusmokrassar.IObjectKRealisations.toObject

open class StockReceiverConfig {
    val text: String? = null
}

abstract class StockReceiver : Receiver {
    protected abstract val command: String
    protected abstract val callback: (IObject<Any>) -> Unit

    override fun receive(data: IObject<Any>) {
        data.toObject(StockReceiverConfig::class.java).text ?.let {
            text ->
            val command = text.substring(
                    0 until text.indexOf(' ').let {
                        if (it < 0) {
                            text.length
                        } else {
                            it
                        }
                    }
            )
            if (this.command == command) {
                callback(data)
            }
        }
    }
}
