package hr.ferit.dominikzivko.unistat.ui

import domyutil.*
import domyutil.jfx.*
import hr.ferit.dominikzivko.unistat.*
import hr.ferit.dominikzivko.unistat.ui.component.BillSummary
import hr.ferit.dominikzivko.unistat.ui.component.ChartControlPanel
import javafx.beans.binding.Bindings
import javafx.collections.FXCollections
import javafx.collections.ListChangeListener
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
        setupCharts()
    }

    private fun setupBillSummary() {
        lblAvailableSubsidy.textProperty().bind(
            Bindings.createStringBinding(
                { "${app.repository.user?.balance.toString()} $shortCurrencyStr" },
                app.repository.userProperty
            )
        )

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

    private fun setupCharts() {
        dailySpendingChartControlPanel.entryCountProperty.addListener { _, _, _ -> populateCharts() }
        spendingByBillChartControlPanel.entryCountProperty.addListener { _, _, _ -> populateCharts() }
        app.repository.bills.addListener(ListChangeListener {
            populateCharts()
        })
        populateCharts()
    }

    private fun populateCharts() {
        populateDailySpendingChart()
        populateSpendingByBillChart()
    }

    private fun populateDailySpendingChart() {
        val values = FXCollections.observableArrayList<XYChart.Data<String, Number>>()
        val costs = FXCollections.observableArrayList<XYChart.Data<String, Number>>()

        val pointCount = dailySpendingChartControlPanel.entryCount
        val lastBillDay = app.repository.bills.last().date
        val startDay =
            if (pointCount == 0) app.repository.bills.first().date
            else lastBillDay.minusDays(pointCount.toLong() - 1L)
        val billsByDay = app.repository.bills.groupBy { it.date }

        for (day in startDay..lastBillDay) {
            val dayBills = billsByDay[day] ?: emptyList()
            val value = dayBills.sumOf { it.totalValue }
            val cost = dayBills.sumOf { it.totalCost }
            val dateString = day.format(CHART_DATE_FORMATTER)
            values += XYChart.Data(dateString, value)
            costs += XYChart.Data(dateString, cost)
        }
        dailySpendingChart.data = FXCollections.observableArrayList()
        dailySpendingChart.data.add(XYChart.Series(strings["chart_series_totalValue"], values))
        dailySpendingChart.data.add(XYChart.Series(strings["chart_series_personalCost"], costs))
    }

    private fun populateSpendingByBillChart() {
        val costs = FXCollections.observableArrayList<XYChart.Data<String, Number>>()
        val subsidies = FXCollections.observableArrayList<XYChart.Data<String, Number>>()

        val billCount = app.repository.bills.size
        val pointCount = spendingByBillChartControlPanel.entryCount.toInt()
        val bills = when {
            pointCount == 0 || pointCount >= billCount -> app.repository.bills
            else -> app.repository.bills.drop(billCount - pointCount)
        }

        var prevDateString = ""
        var repeatCounter = 0
        bills.forEach { bill ->
            var dateString = bill.date.format(CHART_DATE_FORMATTER)
            if (dateString == prevDateString) {
                dateString += "(${++repeatCounter})"
            } else {
                repeatCounter = 0
                prevDateString = dateString
            }

            costs += XYChart.Data(dateString, bill.totalCost)
            subsidies += XYChart.Data(dateString, bill.totalSubsidy)
        }
        spendingByBillChart.data = FXCollections.observableArrayList()
        spendingByBillChart.data.add(XYChart.Series(strings["chart_series_personalCost"], costs))
        spendingByBillChart.data.add(XYChart.Series(strings["chart_series_subsidy"], subsidies))
    }
}