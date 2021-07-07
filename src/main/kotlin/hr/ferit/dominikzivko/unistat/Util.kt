package hr.ferit.dominikzivko.unistat

import com.gargoylesoftware.htmlunit.html.HtmlPage
import hr.ferit.dominikzivko.unistat.ui.floatToString
import javafx.scene.chart.StackedBarChart
import javafx.scene.control.Tooltip
import javafx.util.Duration
import java.time.LocalDate

fun LocalDate.isToday() = this == LocalDate.now()
fun LocalDate.isYesterday() = this == LocalDate.now().minusDays(1)
fun LocalDate.isPastWeek() = this > LocalDate.now().minusWeeks(1) && this <= LocalDate.now()
fun LocalDate.isPastMonth() = this > LocalDate.now().minusMonths(1) && this <= LocalDate.now()

val HtmlPage.urlString: String get() = url.toExternalForm()

fun <X, Y : Number> StackedBarChart<X, Y>.installBarTooltips() {
    data.forEach { series ->
        series.data.forEach { item ->
            val yString = when (item.yValue) {
                is Float, is Double -> floatToString(item.yValue)
                else -> item.yValue.toString()
            }
            val tooltip = Tooltip(item.xValue.toString() + "\n" + yString).apply {
                showDelay = Duration.seconds(0.5)
            }
            Tooltip.install(item.node, tooltip)
        }
    }
}