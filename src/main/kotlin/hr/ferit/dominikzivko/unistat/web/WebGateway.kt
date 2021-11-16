package hr.ferit.dominikzivko.unistat.web

import com.gargoylesoftware.htmlunit.BrowserVersion
import com.gargoylesoftware.htmlunit.SilentCssErrorHandler
import com.gargoylesoftware.htmlunit.WebClient
import com.gargoylesoftware.htmlunit.html.HtmlPage
import com.gargoylesoftware.htmlunit.javascript.SilentJavaScriptErrorListener
import com.gargoylesoftware.htmlunit.util.Cookie
import domyutil.*
import hr.ferit.dominikzivko.unistat.AppComponent
import hr.ferit.dominikzivko.unistat.data.Cookies
import hr.ferit.dominikzivko.unistat.data.Pref
import hr.ferit.dominikzivko.unistat.urlString
import org.apache.logging.log4j.LogManager
import org.jetbrains.exposed.sql.batchInsert
import org.jetbrains.exposed.sql.deleteAll
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.statements.api.ExposedBlob
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.logging.Level
import java.util.logging.Logger

/**
 * An [AppComponent] handling all communication with the webserver.
 *
 * The cookies acquired by processing requests are saved and loaded if they are needed for auto-login purposes. They can
 * also be cleared on demand.
 */
class WebGateway : AppComponent {
    private val log by lazy { LogManager.getLogger(javaClass) }

    private val webClient = WebClient(BrowserVersion.INTERNET_EXPLORER).apply {
        options.apply {
            isThrowExceptionOnScriptError = false
            isThrowExceptionOnFailingStatusCode = false
            isPrintContentOnFailingStatusCode = false
            isRedirectEnabled = true
            isJavaScriptEnabled = true

            // When the app image is exported, an error claiming that the handshake failed pops up. This fixes it.
            // The idea came from an (outdated) SO answer: https://stackoverflow.com/a/47670855/6640693
            setSSLClientProtocols("TLSv1.2")
        }
        javaScriptErrorListener = SilentJavaScriptErrorListener()
        cssErrorHandler = SilentCssErrorHandler()
        Logger.getLogger("com.gargoylesoftware.htmlunit").level = Level.OFF
    }

    var lastPage: HtmlPage? = null
        private set

    override fun start() {
        if (Pref.autoLogin) loadCookies()
    }

    override fun stop() {
        if (Pref.autoLogin) saveCookies()
    }

    /**
     * Fetches a web page with the specified URL and returns the resulting [HtmlPage].
     */
    fun get(url: String): HtmlPage {
        val fullUrl = if (url.startsWith("http")) url else Pref.url_base + url
        log.debug("Connecting to: $fullUrl ...")
        return webClient.getPage<HtmlPage>(fullUrl).also {
            val stillExecuting = webClient.waitForBackgroundJavaScript(10000)
            if (stillExecuting > 0)
                log.warn("Background javascript still executing after timeout!")
            lastPage = it
            log.debug("Connection response: ${it.urlString}")
        }
    }

    fun clearCookies(): Unit = transaction {
        log.info("Clearing cookies.")
        webClient.cookieManager.clearCookies()
        Cookies.deleteAll()
    }

    private fun saveCookies(): Unit = transaction {
        log.info("Saving cookies.")
        runCatching {
            Cookies.deleteAll()
            Cookies.batchInsert(webClient.cookieManager.cookies) { cookie ->
                this[Cookies.content] = ExposedBlob(serialize(cookie))
            }
        }.onFailure { log.error("Could not save cookies", it) }
    }

    private fun loadCookies(): Unit = transaction {
        log.info("Loading cookies.")
        runCatching {
            Cookies.selectAll().forEach {
                webClient.cookieManager.addCookie(deserialize(it[Cookies.content].bytes) as Cookie)
            }
        }.onFailure { log.error("Could not load cookies.", it) }
    }
}