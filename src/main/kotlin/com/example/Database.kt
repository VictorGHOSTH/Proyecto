package com.example

import com.typesafe.config.ConfigFactory
import io.ktor.server.config.*
import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction

object DatabaseFactory {
    fun init(config: HoconApplicationConfig) {
        val url = config.property("ktor.database.url").getString()
        val driver = config.property("ktor.database.driver").getString()
        val user = config.property("ktor.database.user").getString()
        val password = config.property("ktor.database.password").getString()

        Database.connect(
            url = url,
            driver = driver,
            user = user,
            password = password
        )

        println("✅ Base de datos conectada: $url")
    }

    suspend fun <T> dbQuery(block: suspend () -> T): T =
        newSuspendedTransaction(Dispatchers.IO) { block() }
}