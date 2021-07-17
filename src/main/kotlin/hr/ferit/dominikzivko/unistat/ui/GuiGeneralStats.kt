package hr.ferit.dominikzivko.unistat.ui

import domyutil.*
import domyutil.jfx.*
import hr.ferit.dominikzivko.unistat.AppBase
import hr.ferit.dominikzivko.unistat.bindData
import hr.ferit.dominikzivko.unistat.data.totalCost
import hr.ferit.dominikzivko.unistat.data.totalSubsidy
import hr.ferit.dominikzivko.unistat.data.totalValue
import hr.ferit.dominikzivko.unistat.enableBarTooltips
import hr.ferit.dominikzivko.unistat.enablePieTooltips
import javafx.collections.FXCollections
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
        setupMonthlySpendingChart()
        setupBillsBySourceChart()
        setupSpendingBySourceChart()
    }

    private fun setupMonthlySpendingChart() {
        monthlySpendingChart.enableBarTooltips()
        monthlySpendingChart.bindData(app.repository.bills) { series ->
            val costData = FXCollections.observableArrayList<XYChart.Data<String, Number>>()
            val subsidyData = FXCollections.observableArrayList<XYChart.Data<String, Number>>()

            if (app.repository.bills.isNotEmpty()) {
                val billsByMonth = app.repository.bills.groupBy { it.date.yearMonth }
                val firstBillMonth = app.repository.bills.first().date.yearMonth
                val lastBillMonth = app.repository.bills.last().date.yearMonth
                for (month in firstBillMonth..lastBillMonth) {
                    val monthBills = billsByMonth[month] ?: emptyList()
                    val monthString = month.format(MONTH_FORMATTER)
                    costData += XYChart.Data(monthString, monthBills.totalCost)
                    subsidyData += XYChart.Data(monthString, monthBills.totalSubsidy)
                }
            }

            series += XYChart.Series(strings["chart_series_personalCost"], costData)
            series += XYChart.Series(strings["chart_series_subsidy"], subsidyData)
        }
    }

    private fun setupBillsBySourceChart() {
        billsBySourceChart.enablePieTooltips()
        billsBySourceChart.bindData(app.repository.bills) { pieData ->
            app.repository.bills.groupingBy { it.source }
                .eachCount().toSortedMap()
                .forEach { (source, count) ->
                    pieData += PieChart.Data(source, count.toDouble())
                }
        }
    }

    private fun setupSpendingBySourceChart() {
        spendingBySourceChart.enableBarTooltips()
        spendingBySourceChart.bindData(app.repository.bills) { series ->
            val costData = FXCollections.observableArrayList<XYChart.Data<String, Number>>()
            val subsidyData = FXCollections.observableArrayList<XYChart.Data<String, Number>>()

            app.repository.bills.groupBy { it.source }
                .toList().sortedByDescending { it.second.totalValue }
                .forEach { (source, bills) ->
                    costData += XYChart.Data(source, bills.totalCost)
                    subsidyData += XYChart.Data(source, bills.totalSubsidy)
                }

            series += XYChart.Series(strings["chart_series_personalCost"], costData)
            series += XYChart.Series(strings["chart_series_subsidy"], subsidyData)
        }
    }
}