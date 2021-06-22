package hr.ferit.dominikzivko.unistat.data

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.apache.logging.log4j.LogManager
import java.io.File

class Exporter {
    private val log by lazy { LogManager.getLogger(javaClass) }

    fun exportBills(bills: List<Bill>, location: File) {
        log.info("Exporting ${bills.size} bills to $location.")
        val json = Json.encodeToString(bills)
        location.writeText(json)
    }
}