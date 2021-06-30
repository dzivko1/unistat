package hr.ferit.dominikzivko.unistat

import com.gargoylesoftware.htmlunit.html.HtmlPage
import java.time.LocalDate

fun LocalDate.isToday() = this == LocalDate.now()
fun LocalDate.isYesterday() = this == LocalDate.now().minusDays(1)
fun LocalDate.isPastWeek() = this > LocalDate.now().minusWeeks(1) && this <= LocalDate.now()
fun LocalDate.isPastMonth() = this > LocalDate.now().minusMonths(1) && this <= LocalDate.now()

val HtmlPage.urlString: String get() = url.toExternalForm()