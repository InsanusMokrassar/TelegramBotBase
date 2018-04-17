package com.github.insanusmokrassar.TelegramBotBase.models

import com.github.insanusmokrassar.IObjectK.extensions.toJsonString
import com.github.insanusmokrassar.IObjectKRealisations.toIObject
import com.github.insanusmokrassar.TelegramBotBase.tables.QueryDatas
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction

class QueryData private constructor(
        val id: Int,
        val config: String
) {
    constructor(id: Int): this(
            id,
            transaction {
                QueryDatas.select {
                    QueryDatas.id.eq(id)
                }.first().let {
                    it[QueryDatas.config]
                }
            }
    )

    constructor(config: String): this(
            transaction {
                try {
                    QueryDatas.select {
                        QueryDatas.config.eq(config)
                    }.first().let {
                        it[QueryDatas.id]
                    }
                } catch (e: NoSuchElementException) {
                    QueryDatas.insert {
                        it[QueryDatas.config] = config
                    } get QueryDatas.id
                } ?: throw IllegalStateException("Can't get or create query data by config: $config")
            },
            config
    )

    constructor(config: Any): this(config.toIObject().toJsonString())
}