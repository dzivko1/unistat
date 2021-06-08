package hr.ferit.dominikzivko.unistat.data

import org.apache.logging.log4j.LogManager
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.StdOutSqlLogger
import org.jetbrains.exposed.sql.addLogger
import org.jetbrains.exposed.sql.transactions.TransactionManager
import org.jetbrains.exposed.sql.transactions.transaction
import java.sql.Connection

object AppDatabase {
    private val log by lazy { LogManager.getLogger(javaClass) }

    fun initialize() {
        log.info("Initializing database.")
        Database.connect("jdbc:sqlite:file:sqlite.db", "org.sqlite.JDBC")
        TransactionManager.manager.defaultIsolationLevel = Connection.TRANSACTION_SERIALIZABLE

        transaction {
            addLogger(StdOutSqlLogger)
            SchemaUtils.create(Users, Bills, BillEntries, Articles)
        }
    }
}