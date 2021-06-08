package hr.ferit.dominikzivko.unistat

import com.gargoylesoftware.htmlunit.html.HtmlPage
import java.time.LocalDate
import java.time.LocalDateTime

fun LocalDateTime.isToday() = toLocalDate().isEqual(LocalDate.now())
fun LocalDateTime.isYesterday() = toLocalDate().isEqual(LocalDate.now())
fun LocalDateTime.isPastWeek() = toLocalDate().isAfter(LocalDate.now().minusWeeks(1))
fun LocalDateTime.isPastMonth() = toLocalDate().isAfter(LocalDate.now().minusMonths(1))

val HtmlPage.urlString get() = url.toExternalForm()