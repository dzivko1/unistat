package hr.ferit.dominikzivko.unistat

import com.gargoylesoftware.htmlunit.html.HtmlPage
import java.time.LocalDate
import java.time.LocalDateTime

fun LocalDateTime.isToday() = toLocalDate().isEqual(LocalDate.now())

fun LocalDateTime.isYesterday() = toLocalDate().isEqual(LocalDate.now().minusDays(1))

fun LocalDateTime.isPastWeek() = toLocalDate().run {
    isAfter(LocalDate.now().minusWeeks(1)) && isBefore(LocalDate.now().plusDays(1))
}

fun LocalDateTime.isPastMonth() = toLocalDate().run {
    isAfter(LocalDate.now().minusMonths(1)) && isBefore(LocalDate.now().plusDays(1))
}

val HtmlPage.urlString: String get() = url.toExternalForm()