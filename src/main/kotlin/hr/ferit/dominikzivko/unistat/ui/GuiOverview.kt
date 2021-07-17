package hr.ferit.dominikzivko.unistat.ui

import domyutil.*
import domyutil.jfx.*
import hr.ferit.dominikzivko.unistat.*
import hr.ferit.dominikzivko.unistat.data.totalCost
import hr.ferit.dominikzivko.unistat.data.totalValue
import hr.ferit.dominikzivko.unistat.ui.component.BillSummary
import hr.ferit.dominikzivko.unistat.ui.component.ChartControlPanel
import javafx.collections.FXCollections
import javafx.fxml.FXML
import javafx.geometry.Orientation
import javafx.scene.chart.LineChart
import javafx.scene.chart.StackedBarChart
import javafx.scene.chart.XYChart
import javafx.scene.control.Label
import javafx.scene.control.Separator
import javafx.scene.layout.HBox
import org.koin.core.context.GlobalContext

class GuiOverview {
    private val app: AppBase by lazy { GlobalContext.get().get() }

    @FXML
    private lateinit var billSummaryBox: HBox

    @FXML
    private lateinit var lblAvailableSubsidy: Label

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
            BillSummary(strings["summary_today"], app.repository.bills.filtered { it.date.isToday() }),
            Separator(Orientation.VERTICAL),
            BillSummary(strings["summary_yesterday"], app.repository.bills.filtered { it.date.isYesterday() }),
            Separator(Orientation.VERTICAL),
            BillSummary(strings["summary_thisWeek"], app.repository.bills.filtered { it.date.isPastWeek() }),
            Separator(Orientation.VERTICAL),
            BillSummary(strings["summary_thisMonth"], app.repository.bills.filtered { it.date.isPastMonth() }),
            Separator(Orientation.VERTICAL),
            BillSummary(strings["summary_total"], app.repository.bills),
            Separator(Orientation.VERTICAL)
        )
    }

    private fun setupDailySpendingChart() {
        dailySpendingChart.bindData(
            app.repository.bills,
            dailySpendingChartControlPanel.entryCountProperty
        ) { series ->
            val valueData = FXCollections.observableArrayList<XYChart.Data<String, Number>>()
            val costData = FXCollections.observableArrayList<XYChart.Data<String, Number>>()

            if (app.repository.bills.isNotEmpty()) {
                val pointCount = dailySpendingChartControlPanel.entryCount
                val lastBillDay = app.repository.bills.last().date
                val startDay =
                    if (pointCount == 0) app.repository.bills.first().date
                    else lastBillDay.minusDays(pointCount.toLong() - 1L)
                val billsByDay = app.repository.bills.groupBy { it.date }

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
        spendingByBillChart.enableBarTooltips()
        spendingByBillChart.bindData(
            app.repository.bills,
            spendingByBillChartControlPanel.entryCountProperty
        ) { series ->
            val costData = FXCollections.observableArrayList<XYChart.Data<String, Number>>()
            val subsidyData = FXCollections.observableArrayList<XYChart.Data<String, Number>>()

            if (app.repository.bills.isNotEmpty()) {
                val billCount = app.repository.bills.size
                val pointCount = spendingByBillChartControlPanel.entryCount.toInt()
                val bills = when {
                    (pointCount == 0 || pointCount >= billCount) -> app.repository.bills
                    else -> app.repository.bills.drop(billCount - pointCount)
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
    }
}