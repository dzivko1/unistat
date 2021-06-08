package hr.ferit.dominikzivko.unistat.data

import com.gargoylesoftware.htmlunit.html.*
import domyutil.*
import domyutil.jfx.*
import hr.ferit.dominikzivko.unistat.UnexpectedResponseException
import hr.ferit.dominikzivko.unistat.ui.FLOAT_FORMAT
import hr.ferit.dominikzivko.unistat.ui.SERVER_DATE_TIME_FORMATTER
import hr.ferit.dominikzivko.unistat.ui.component.ProgressMonitor
import hr.ferit.dominikzivko.unistat.urlString
import hr.ferit.dominikzivko.unistat.web.AuthWebConnection
import javafx.beans.binding.Bindings
import javafx.beans.property.SimpleObjectProperty
import org.apache.logging.log4j.LogManager
import java.time.LocalDateTime
import java.util.*
import java.util.concurrent.Callable

interface DataSource {
    val userID: UUID
    fun fetchGeneralData(): User
    fun fetchBills(existingBills: List<Bill>, progressMonitor: ProgressMonitor): List<Bill>
}

class WebDataSource(val conn: AuthWebConnection) : DataSource {
    private val log by lazy { LogManager.getLogger(javaClass) }

    private lateinit var billsUrlIdentifier: String

    private val _userID = SimpleObjectProperty<UUID>().apply {
        bind(
            Bindings.createObjectBinding(
                { nameUUIDFromString(conn.currentUser!!.username) },
                conn.currentUserProperty
            )
        )
    }
    override val userID: UUID get() = _userID.value

    override fun fetchGeneralData(): User {
        log.debug("Fetching general data...")
        conn.fetchAuthorized(Pref.url_student).run {
            //conn.fetchAuthorized("file:\\\\\\C:\\Users\\domyz\\desktop\\Student_podaci_ISSP_Srce.htm").run {
            if (!urlString.startsWith(Pref.url_student))
                throw UnexpectedResponseException("Unexpected server response to data fetch.\n\tReceived: $urlString\n\tExpected: ${Pref.url_student}")

            val username = conn.currentUser!!.username
            val fullName = extract("section:nth-of-type(1) .card-title")
            val institution = extract("section:nth-of-type(1) .font-italic").substringAfter(": ")
            val level = extract("section:nth-of-type(1) .row:nth-of-type(1) .col:nth-of-type(1) p:nth-of-type(2)")
            val balance = extract("section:nth-of-type(1) .row:nth-of-type(1) .col:nth-of-type(2) p:nth-of-type(2)")
                .substringBefore(' ').toFloat(FLOAT_FORMAT)

            extractBillsUrlIdentifier(this)

            return User(username, fullName, institution, level, balance, userID)
        }
    }

    //var a: String? = "file:\\\\\\C:\\Users\\domyz\\desktop\\Računi_studenta_ISSP_Srce1.htm"
    override fun fetchBills(existingBills: List<Bill>, progressMonitor: ProgressMonitor): List<Bill> {
        log.debug("Fetching bills...")
        runFx {
            progressMonitor.message = strings["fetching_bills"] + "..."
            progressMonitor.progress = -1
        }
        //c++
        if (!this::billsUrlIdentifier.isInitialized)
            extractBillsUrlIdentifier()

        conn.fetchAuthorized(Pref.url_billsBase + billsUrlIdentifier).run {
            //conn.fetchAuthorized(a ?: "file:\\\\\\C:\\Users\\domyz\\desktop\\Računi_studenta_ISSP_Srce.htm").run {
            //a = null
            if (!urlString.startsWith(Pref.url_billsBase))
                throw UnexpectedResponseException("Unexpected server response to data fetch.\n\tReceived: $urlString\n\tExpected: ${Pref.url_billsBase}")

            fun extractBillOutline(row: HtmlTableRow): Pair<String, LocalDateTime> = Pair(
                row.extract(0),
                row.run {
                    val date = extract(1)
                    val time = extract(2)
                    LocalDateTime.parse("$date. $time", SERVER_DATE_TIME_FORMATTER)
                }
            )

            fun billExists(row: HtmlTableRow) = existingBills.any { existingBill ->

                fun areEntriesEqual(a: List<BillEntry>, b: List<BillEntry>): Boolean {
                    return if (a.size != b.size) false
                    else a.toSet().all { entryA ->
                        b.toSet().any { entryB ->
                            entryA.areDetailsEqual(entryB)
                        }
                    }

                }

                val (source, dateTime) = extractBillOutline(row)
                return@any if (dateTime != existingBill.dateTime || source != existingBill.source) false
                else {
                    log.debug("Checking if new bills ended.")
                    areEntriesEqual(fetchEntries(row), existingBill.entries)
                }
            }

            val billRows = querySelector<HtmlTable>("table").rows.reversed().dropLast(1)
            val newBillCount = billRows.takeWhile { billExists(it).not() }.count()

            return List(newBillCount) { index ->
                log.debug("Fetching bill ${index + 1}/$newBillCount")
                billRows[index].run {
                    val (source, dateTime) = extractBillOutline(this)
                    val entries = fetchEntries(this)

                    runFx {
                        progressMonitor.message = "${strings["downloaded_bills"]}: ${index + 1}/$newBillCount"
                        progressMonitor.progress = (index + 1.0) / newBillCount
                    }

                    return@List Bill(dateTime, source, userID, entries)
                }
            }
        }
    }

    //var b = "file:\\\\\\C:\\Users\\domyz\\desktop\\Detalji_računa_ISSP_Srce1.htm"
    //var c = 0
    private fun fetchEntries(billTableRow: HtmlTableRow): List<BillEntry> {
        val detailsLocation = billTableRow.getCell(6).querySelector<HtmlAnchor>("a").hrefAttribute
        val detailsPage = conn.fetchAuthorized(detailsLocation)
        //val detailsPage = conn.fetchAuthorized(if (c == 1) "file:\\\\\\C:\\Users\\domyz\\desktop\\Detalji_računa_ISSP_Srce.htm" else b)
        val entriesTable = detailsPage.querySelector<HtmlTable>("table")
        val entryCount = entriesTable.rowCount - 2

        return List(entryCount) { index ->
            entriesTable.getRow(index + 1).run {
                val name = extract(0)
                val amount = extract(1).toInt()
                val price = extract(2).toFloat(FLOAT_FORMAT)
                val subsidy = extract(4).toFloat(FLOAT_FORMAT)
                return@List BillEntry(Article(name, price), amount, subsidy)
            }
        }
    }

    private fun extractBillsUrlIdentifier(studentPage: HtmlPage? = null) {
        (studentPage ?: log.info("Finding bill page location...").let { conn.fetchAuthorized(Pref.url_student) }).run {
            billsUrlIdentifier = "?" + querySelector<HtmlAnchor>("section:nth-of-type(1) a.btn")
                .hrefAttribute.substringAfter('?')
        }
    }

    private fun HtmlPage.extract(selector: String) = querySelector<DomNode>(selector).asNormalizedText()
    private fun HtmlTableRow.extract(index: Int) = getCell(index).asNormalizedText()
}