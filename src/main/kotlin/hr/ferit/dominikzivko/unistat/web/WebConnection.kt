package hr.ferit.dominikzivko.unistat.web

import com.gargoylesoftware.htmlunit.BrowserVersion
import com.gargoylesoftware.htmlunit.WebClient
import com.gargoylesoftware.htmlunit.html.HtmlPage
import hr.ferit.dominikzivko.unistat.data.Pref
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import org.apache.logging.log4j.LogManager
import java.io.IOException

class WebConnection {
    private val log by lazy { LogManager.getLogger(javaClass) }

    private val webClient = WebClient(BrowserVersion.INTERNET_EXPLORER)

    init {
        webClient.options.isThrowExceptionOnScriptError = false
    }

    fun get(url: String) : HtmlPage {
        val fullUrl = if (url.startsWith("http")) url else Pref.url_base + url
        log.debug("Connecting to: $fullUrl")
        return webClient.getPage<HtmlPage>(fullUrl).also {
            val stillExecuting = webClient.waitForBackgroundJavaScript(10000)
            if (stillExecuting > 0)
                log.warn("Background javascript still executing after timeout!")
        }
    }
}