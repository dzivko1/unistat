package hr.ferit.dominikzivko.unistat.data

import com.gargoylesoftware.htmlunit.html.*
import domyutil.*
import domyutil.jfx.*
import hr.ferit.dominikzivko.unistat.UnexpectedResponseException
import hr.ferit.dominikzivko.unistat.checkCancelled
import hr.ferit.dominikzivko.unistat.gui.FLOAT_FORMAT
import hr.ferit.dominikzivko.unistat.gui.SERVER_DATE_TIME_FORMATTER
import hr.ferit.dominikzivko.unistat.gui.component.ProgressMonitor
import hr.ferit.dominikzivko.unistat.urlString
import hr.ferit.dominikzivko.unistat.web.AuthWebGateway
import org.apache.logging.log4j.LogManager
import java.time.LocalDateTime
import java.util.*

class WebDataSource(val web: AuthWebGateway) : DataSource {
    private val log by lazy { LogManager.getLogger(javaClass) }

    private lateinit var billsUrlIdentifier: String

    override val userID: UUID? get() = web.currentUser?.id

    override fun start() {
        web.start()
    }

    override fun stop() {
        web.stop()
    }

    override fun fetchGeneralData(progressMonitor: ProgressMonitor): User {
        log.debug("Fetching general data...")

        progressMonitor.applyFx {
            message = strings["fetchingGeneralData"] + "..."
            progress = -1
        }

        web.fetchAuthorized(Pref.url_student, progressMonitor, canPromptLogin = true).run {
            checkExpected(Pref.url_student)

            val username = web.currentUser!!.username
            val fullName = extract("section:nth-of-type(1) .card-title")
            val institution = extract("section:nth-of-type(1) .font-italic").substringAfter(": ")
            val level = extract("section:nth-of-type(1) .row:nth-of-type(1) .col:nth-of-type(1) p:nth-of-type(2)")
            val balance = extract("section:nth-of-type(1) .row:nth-of-type(1) .col:nth-of-type(2) p:nth-of-type(2)")
                .substringBefore(' ').toFloat(FLOAT_FORMAT)

            extractBillsUrlIdentifier(this)

            return User(username, fullName, institution, level, balance, userID!!)
        }
    }

    override fun fetchBills(existingBills: List<Bill>, progressMonitor: ProgressMonitor): List<Bill> {
        log.debug("Fetching bills...")
        progressMonitor.applyFx {
            message = strings["fetchingBills"] + "..."
            progress = -1
        }

        if (!this::billsUrlIdentifier.isInitialized)
            extractBillsUrlIdentifier()

        val billsUrl = Pref.url_billsBase + billsUrlIdentifier
        web.fetchAuthorized(billsUrl).run {
            checkExpected(billsUrl)

            fun billExists(row: HtmlTableRow) = existingBills.any { existingBill ->
                fun areEntriesEqual(a: List<BillEntry>, b: List<BillEntry>): Boolean {
                    return if (a.size != b.size) false
                    else a.toSet().all { entryA ->
                        b.toSet().any { entryB ->
                            entryA.areDetailsEqual(entryB)
                        }
                    }
                }
                val (source, dateTime) = row.extractBillOutline()
                return@any if (dateTime != existingBill.dateTime || source != existingBill.source) false
                else {
                    checkCancelled()
                    log.debug("Checking if new bills ended.")
                    val newEntries = fetchEntries(row)
                    areEntriesEqual(newEntries, existingBill.entries)
                }
            }

            // Sometimes the fetched bills are not ordered correctly, so we are sorting the rows to the expected order
            val billRows = querySelector<HtmlTable>("table")
                .rows.drop(1).sortedBy { it.extractBillDateTime() }

            val newBillCount = billRows.takeWhile { billExists(it).not() }.count()

            return List(newBillCount) { index ->
                checkCancelled()
                log.debug("Fetching bill ${index + 1}/$newBillCount")
                billRows[index].run {
                    val (source, dateTime) = extractBillOutline()
                    val entries = fetchEntries(this)

                    progressMonitor.applyFx {
                        message = "${strings["downloadedBills"]}: ${index + 1}/$newBillCount"
                        progress = (index + 1.0) / newBillCount
                    }

                    return@List Bill(dateTime, source, userID!!, entries)
                }
            }
        }
    }

    private fun fetchEntries(billTableRow: HtmlTableRow): List<BillEntry> {
        val detailsLocation = billTableRow.getCell(6).querySelector<HtmlAnchor>("a").hrefAttribute
        val detailsPage = web.fetchAuthorized(detailsLocation)
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

    override fun revokeAuthorization() {
        web.logout()
    }

    private fun extractBillsUrlIdentifier(studentPage: HtmlPage? = null) {
        (studentPage ?: log.info("Finding bill page location...").let { web.fetchAuthorized(Pref.url_student) }).run {
            billsUrlIdentifier = "?" + querySelector<HtmlAnchor>("section:nth-of-type(1) a.btn")
                .hrefAttribute.substringAfter('?')
        }
    }

    private fun HtmlPage.extract(selector: String) = querySelector<DomNode>(selector).asNormalizedText()
    private fun HtmlTableRow.extract(index: Int) = getCell(index).asNormalizedText()
    private fun HtmlTableRow.extractBillDateTime() = run {
        val date = extract(1)
        val time = extract(2)
        LocalDateTime.parse("$date. $time", SERVER_DATE_TIME_FORMATTER)
    }
    private fun HtmlTableRow.extractBillOutline() = Pair(
        extract(0),
        extractBillDateTime()
    )

    private fun HtmlPage.checkExpected(expectedUrl: String) {
        if (!urlString.startsWith(expectedUrl))
            throw UnexpectedResponseException("Unexpected server response to data fetch.\n\tReceived: $urlString\n\tExpected: $expectedUrl")
    }
}
