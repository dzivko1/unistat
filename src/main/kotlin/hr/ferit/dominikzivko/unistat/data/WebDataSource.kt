package hr.ferit.dominikzivko.unistat.data

import com.gargoylesoftware.htmlunit.html.*
import domyutil.*
import domyutil.jfx.*
import hr.ferit.dominikzivko.unistat.UnexpectedResponseException
import hr.ferit.dominikzivko.unistat.checkCancelled
import hr.ferit.dominikzivko.unistat.gui.FLOAT_FORMAT
import hr.ferit.dominikzivko.unistat.gui.SERVER_DATE_TIME_FORMATTER
import hr.ferit.dominikzivko.unistat.gui.component.ProgressMonitor
import hr.ferit.dominikzivko.unistat.safeText
import hr.ferit.dominikzivko.unistat.urlString
import hr.ferit.dominikzivko.unistat.web.AuthWebGateway
import org.apache.logging.log4j.LogManager
import java.time.LocalDateTime
import java.util.*

/**
 * An implementation of [DataSource] which gathers data from the webserver. Loading web pages is done through an
 * [AuthWebGateway].
 *
 * This implementation requires user authentication with the webserver and will ask for it at the time it is requested
 * by said webserver.
 */
class WebDataSource(val web: AuthWebGateway) : DataSource {
    private val log by lazy { LogManager.getLogger(javaClass) }

    /** The identifying part of a URL leading to the user's bills page. */
    private lateinit var billsUrlIdentifier: String

    override val userID: UUID? get() = web.currentUser?.id

    override fun start() {
        web.start()
    }

    override fun stop() {
        web.stop()
    }

    /**
     * Fetches the user's general data while requiring authentication as outlined in [AuthWebGateway].
     */
    override fun fetchGeneralData(progressMonitor: ProgressMonitor?): User {
        log.debug("Fetching general data...")

        progressMonitor?.applyFx {
            message = strings["fetchingGeneralData"] + "..."
            progress = -1
        }

        web.fetchAuthorized(Pref.url_student, progressMonitor, canPromptLogin = true).run {
            checkExpected(Pref.url_student)

            val infoDiv = querySelector<DomNode>("#mainDivContent .col-7")

            val username = web.currentUser!!.username
            val fullName = infoDiv.extract("./h2[1]")
            val institution = infoDiv.extract("./p[1]").substringAfter(": ")
            val level = infoDiv.extract("./div[1]/div[1]/p[2]")
            val balance = infoDiv.extract("./div[1]/div[2]/p[2]")
                .substringBefore(' ').toFloat(FLOAT_FORMAT)

            extractBillsUrlIdentifier(this)

            return User(username, fullName, institution, level, balance, userID!!)
        }
    }

    /**
     * Fetches the user's bill data while requiring authentication as outlined in [AuthWebGateway].
     */
    override fun fetchBills(existingBills: List<Bill>, progressMonitor: ProgressMonitor?): List<Bill> {
        log.debug("Fetching bills...")
        progressMonitor?.applyFx {
            message = strings["fetchingBills"] + "..."
            progress = -1
        }

        if (!this::billsUrlIdentifier.isInitialized)
            extractBillsUrlIdentifier()

        val billsUrl = Pref.url_billsBase + billsUrlIdentifier
        web.fetchAuthorized(billsUrl).run {
            checkExpected(billsUrl)

            fun billExists(row: HtmlTableRow) = existingBills.any { existingBill ->
                val (source, dateTime) = row.extractBillOutline()
                return@any if (dateTime != existingBill.dateTime || source != existingBill.source) false
                else {
                    checkCancelled()
                    log.debug("Checking if new bills ended.")
                    val newEntries = fetchEntries(row)
                    newEntries == existingBill.entries
                }
            }

            // Sometimes the fetched bills are not ordered correctly, so we are sorting the rows to the expected order
            val billRows = querySelector<HtmlTable>("table")
                .rows.drop(1).sortedByDescending { it.extractBillDateTime() }

            val newBillCount = billRows.takeWhile { billExists(it).not() }.count()

            return List(newBillCount) { index ->
                checkCancelled()
                log.debug("Fetching bill ${index + 1}/$newBillCount")
                billRows[index].run {
                    val (source, dateTime) = extractBillOutline()
                    val entries = fetchEntries(this)

                    progressMonitor?.applyFx {
                        message = "${strings["downloadedBills"]}: ${index + 1}/$newBillCount"
                        progress = (index + 1.0) / newBillCount
                    }

                    return@List Bill(dateTime, source, userID!!, entries)
                }
            }.reversed()
        }
    }

    private fun fetchEntries(billTableRow: HtmlTableRow): List<BillEntry> {
        val detailsLocation = billTableRow.querySelector<HtmlAnchor>("a").hrefAttribute
        val detailsPage = web.fetchAuthorized(detailsLocation)
        val entriesTable = detailsPage.querySelector<HtmlTable>("table")
        val entryCount = entriesTable.rowCount - 2

        return List(entryCount) { index ->
            entriesTable.getRow(index + 1).run {
                val name = extract(0)
                val amount = extract(1).toInt()
                val price = extractDecimal(2).toBigDecimal().setScale(2)
                val subsidy = extractDecimal(4).toBigDecimal().setScale(2)
                return@List BillEntry(Article(name, price), amount, subsidy)
            }
        }
    }

    /**
     * Revokes the current user's authorization by logging them out of the webserver.
     * @see AuthWebGateway.logout
     */
    override fun revokeAuthorization() {
        web.logout()
    }

    private fun extractBillsUrlIdentifier(studentPage: HtmlPage? = null) {
        (studentPage ?: {
            log.info("Finding bill page location...")
            web.fetchAuthorized(Pref.url_student)
        } as HtmlPage).run {
            billsUrlIdentifier = "?" + getFirstByXPath<HtmlAnchor>("//div[@id='mainDivContent']/div[2]/div/div/div/div/div[1]/div[2]/div/div/div/a[1]")
                .hrefAttribute.substringAfter('?')
        }
    }

    private fun DomNode.extract(selector: String) = getFirstByXPath<DomNode>(selector).safeText
    private fun HtmlTableRow.extract(index: Int) = getCell(index).safeText
    private fun HtmlTableRow.extractDecimal(index: Int) = extract(index).replace(',', '.')
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
