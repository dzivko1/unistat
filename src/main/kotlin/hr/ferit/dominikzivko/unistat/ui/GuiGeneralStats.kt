package hr.ferit.dominikzivko.unistat.ui

import domyutil.*
import domyutil.jfx.*
import hr.ferit.dominikzivko.unistat.AppBase
import hr.ferit.dominikzivko.unistat.installBarTooltips
import javafx.collections.FXCollections
import javafx.collections.ListChangeListener
import javafx.fxml.FXML
import javafx.scene.chart.PieChart
import javafx.scene.chart.StackedBarChart
import javafx.scene.chart.XYChart
import org.koin.core.context.GlobalContext

class GuiGeneralStats {
    private val app: AppBase by lazy { GlobalContext.get().get() }

    @FXML
    private lateinit var monthlySpendingChart: StackedBarChart<String, Number>

    @FXML
    private lateinit var billsBySourceChart: PieChart

    @FXML
    private lateinit var spendingBySourceChart: StackedBarChart<String, Number>

    @FXML
    private fun initialize() {
        app.repository.bills.addListener(ListChangeListener {
            populateCharts()
        })
        populateCharts()
    }

    private fun populateCharts() {
        populateMonthlySpendingChart()
        populateBillsBySourceChart()
        populateSpendingBySourceChart()
    }

    private fun populateMonthlySpendingChart() {
        val costs = FXCollections.observableArrayList<XYChart.Data<String, Number>>()
        val subsidies = FXCollections.observableArrayList<XYChart.Data<String, Number>>()

        val billsByMonth = app.repository.bills.groupBy { it.date.yearMonth }
        val firstBillMonth = app.repository.bills.first().date.yearMonth
        val lastBillMonth = app.repository.bills.last().date.yearMonth
        for (month in firstBillMonth..lastBillMonth) {
            val monthBills = billsByMonth[month] ?: emptyList()
            val subsidy = monthBills.sumOf { it.totalSubsidy }
            val cost = monthBills.sumOf { it.totalCost }
            val monthString = month.format(MONTH_FORMATTER)
            costs += XYChart.Data(monthString, cost)
            subsidies += XYChart.Data(monthString, subsidy)
        }

        monthlySpendingChart.data = FXCollections.observableArrayList()
        monthlySpendingChart.data.add(XYChart.Series(strings["chart_series_personalCost"], costs))
        monthlySpendingChart.data.add(XYChart.Series(strings["chart_series_subsidy"], subsidies))
        monthlySpendingChart.installBarTooltips()
    }

    private fun populateBillsBySourceChart() {
        billsBySourceChart.data.clear()
        app.repository.bills.groupingBy { it.source }
            .eachCount().toSortedMap()
            .forEach { (source, count) ->
                billsBySourceChart.data.add(PieChart.Data(source, count.toDouble()))
            }
    }

    private fun populateSpendingBySourceChart() {
        val costs = FXCollections.observableArrayList<XYChart.Data<String, Number>>()
        val subsidies = FXCollections.observableArrayList<XYChart.Data<String, Number>>()

        app.repository.bills.groupingBy { it.source }
            .fold(Pair(0.0, 0.0)) { acc, bill -> Pair(acc.first + bill.totalCost, acc.second + bill.totalSubsidy) }
            .toList().sortedByDescending { entry -> entry.second.run { first + second } }
            .forEach { (source, sum) ->
                costs += XYChart.Data(source, sum.first)
                subsidies += XYChart.Data(source, sum.second)
            }

        spendingBySourceChart.data = FXCollections.observableArrayList()
        spendingBySourceChart.data.add(XYChart.Series(strings["chart_series_personalCost"], costs))
        spendingBySourceChart.data.add(XYChart.Series(strings["chart_series_subsidy"], subsidies))
        spendingBySourceChart.installBarTooltips()
    }
}