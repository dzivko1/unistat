package hr.ferit.dominikzivko.unistat

import com.gargoylesoftware.htmlunit.html.HtmlPage
import hr.ferit.dominikzivko.unistat.data.Bill
import hr.ferit.dominikzivko.unistat.ui.floatToString
import javafx.scene.chart.XYChart
import javafx.scene.control.Tooltip
import javafx.util.Duration
import java.time.LocalDate

fun LocalDate.isToday() = this == LocalDate.now()
fun LocalDate.isYesterday() = this == LocalDate.now().minusDays(1)
fun LocalDate.isPastWeek() = this > LocalDate.now().minusWeeks(1) && this <= LocalDate.now()
fun LocalDate.isPastMonth() = this > LocalDate.now().minusMonths(1) && this <= LocalDate.now()

val HtmlPage.urlString: String get() = url.toExternalForm()

val List<Bill>.articleCount get() = sumOf { it.articleCount }
val List<Bill>.totalValue get() = sumOf { it.totalValue }
val List<Bill>.totalSubsidy get() = sumOf { it.totalSubsidy }
val List<Bill>.totalCost get() = sumOf { it.totalCost }

fun <X, Y : Number> XYChart<X, Y>.installBarTooltips(showDelay: Duration = Duration.seconds(0.5)) {
    data.forEach { series ->
        series.data.forEach { item ->
            val yString = when (item.yValue) {
                is Float, is Double -> floatToString(item.yValue)
                else -> item.yValue.toString()
            }
            val tooltip = Tooltip(item.xValue.toString() + "\n" + yString).apply {
                this.showDelay = showDelay
            }
            Tooltip.install(item.node, tooltip)
        }
    }
}