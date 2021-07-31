package hr.ferit.dominikzivko.unistat.gui

import domyutil.*
import domyutil.jfx.*
import hr.ferit.dominikzivko.unistat.*
import hr.ferit.dominikzivko.unistat.data.totalCost
import hr.ferit.dominikzivko.unistat.data.totalValue
import hr.ferit.dominikzivko.unistat.gui.component.BillSummary
import hr.ferit.dominikzivko.unistat.gui.component.ChartControlPanel
import javafx.beans.binding.Bindings
import javafx.collections.FXCollections
import javafx.fxml.FXML
import javafx.geometry.Orientation
import javafx.scene.chart.LineChart
import javafx.scene.chart.StackedBarChart
import javafx.scene.chart.XYChart
import javafx.scene.control.Label
import javafx.scene.control.Separator
import javafx.scene.layout.HBox
import javafx.scene.layout.VBox
import org.koin.core.context.GlobalContext
import kotlin.math.max

class GuiOverview {
    private val app: AppBase by lazy { GlobalContext.get().get() }

    @FXML
    private lateinit var billSummaryBox: HBox

    @FXML
    private lateinit var lblAvailableSubsidy: Label

    @FXML
    private lateinit var container: VBox

    @FXML
    private lateinit var dailySpendingChart: LineChart<String, Number>

    @FXML
    private lateinit var dailySpendingChartControlPanel: ChartControlPanel

    @FXML
    private lateinit var spendingByBillChart: StackedBarChart<String, Number>

    @FXML
    private lateinit var spendingByBillChartControlPanel: ChartControlPanel

    @FXML
    fun initialize() {
        setupBillSummary()
        setupDailySpendingChart()
        setupSpendingByBillChart()
    }

    private fun setupBillSummary() {
        lblAvailableSubsidy.bindText(app.repository.userProperty) {
            "${app.repository.user?.balance.toString()} $shortCurrencyStr"
        }

        billSummaryBox.children += listOf(
            BillSummary(strings["summary_today"], app.repository.filteredBills.filtered { it.date.isToday() }),
            Separator(Orientation.VERTICAL),
            BillSummary(strings["summary_yesterday"], app.repository.filteredBills.filtered { it.date.isYesterday() }),
            Separator(Orientation.VERTICAL),
            BillSummary(strings["summary_thisWeek"], app.repository.filteredBills.filtered { it.date.isPastWeek() }),
            Separator(Orientation.VERTICAL),
            BillSummary(strings["summary_thisMonth"], app.repository.filteredBills.filtered { it.date.isPastMonth() }),
            Separator(Orientation.VERTICAL),
            BillSummary(strings["summary_total"], app.repository.filteredBills),
            Separator(Orientation.VERTICAL)
        )
    }

    private fun setupDailySpendingChart() {
        dailySpendingChart.bindData(
            app.repository.filteredBills,
            dailySpendingChartControlPanel.entryCountProperty
        ) { series ->
            val valueData = FXCollections.observableArrayList<XYChart.Data<String, Number>>()
            val costData = FXCollections.observableArrayList<XYChart.Data<String, Number>>()

            if (app.repository.filteredBills.isNotEmpty()) {
                val pointCount = dailySpendingChartControlPanel.entryCount
                val lastBillDay = app.repository.latestFilteredBillDate!!
                val startDay =
                    if (pointCount == 0) app.repository.earliestFilteredBillDate!!
                    else lastBillDay.minusDays(pointCount.toLong() - 1L)
                val billsByDay = app.repository.filteredBills.groupBy { it.date }

                for (day in startDay..lastBillDay) {
                    val dayBills = billsByDay[day] ?: emptyList()
                    val dateString = day.format(SHORT_DATE_FORMATTER)
                    valueData += XYChart.Data(dateString, dayBills.totalValue)
                    costData += XYChart.Data(dateString, dayBills.totalCost)
                }
            }

            series += XYChart.Series(strings["chart_series_totalValue"], valueData)
            series += XYChart.Series(strings["chart_series_personalCost"], costData)
        }
    }

    private fun setupSpendingByBillChart() {
        spendingByBillChart.apply {
            enableBarTooltips()
            bindData(
                app.repository.filteredBills,
                spendingByBillChartControlPanel.entryCountProperty
            ) { series ->
                val costData = FXCollections.observableArrayList<XYChart.Data<String, Number>>()
                val subsidyData = FXCollections.observableArrayList<XYChart.Data<String, Number>>()

                if (app.repository.filteredBills.isNotEmpty()) {
                    val billCount = app.repository.filteredBills.size
                    val pointCount = spendingByBillChartControlPanel.entryCount.toInt()
                    val bills = when {
                        (pointCount == 0 || pointCount >= billCount) -> app.repository.filteredBills
                        else -> app.repository.filteredBills.drop(billCount - pointCount)
                    }

                    var prevDateString = ""
                    var repeatCounter = 0
                    bills.forEach { bill ->
                        var dateString = bill.date.format(SHORT_DATE_FORMATTER)
                        if (dateString == prevDateString) {
                            dateString += "(${++repeatCounter})"
                        } else {
                            repeatCounter = 0
                            prevDateString = dateString
                        }

                        costData += XYChart.Data(dateString, bill.totalCost)
                        subsidyData += XYChart.Data(dateString, bill.totalSubsidy)
                    }
                }

                series += XYChart.Series(strings["chart_series_personalCost"], costData)
                series += XYChart.Series(strings["chart_series_subsidy"], subsidyData)
            }
            prefWidthProperty().bind(
                Bindings.createDoubleBinding({
                    max(container.width - 15, data[0].data.size * 8.0)
                }, container.widthProperty(), dataProperty())
            )
        }
    }
}