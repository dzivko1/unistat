package hr.ferit.dominikzivko.unistat.ui

import domyutil.jfx.*
import hr.ferit.dominikzivko.unistat.*
import hr.ferit.dominikzivko.unistat.ui.component.BillSummary
import javafx.beans.binding.Bindings
import javafx.fxml.FXML
import javafx.geometry.Orientation
import javafx.scene.chart.LineChart
import javafx.scene.control.Label
import javafx.scene.control.Separator
import javafx.scene.layout.HBox
import org.koin.core.context.GlobalContext
import java.time.LocalDate

class GuiOverview {
    private val app: AppBase by lazy { GlobalContext.get().get() }

    @FXML
    private lateinit var billSummaryBox: HBox

    @FXML
    private lateinit var lblAvailableSubsidy: Label

    @FXML
    private lateinit var dailySpendingChart: LineChart<LocalDate, Float>

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

    }
}