package hr.ferit.dominikzivko.unistat.ui

import hr.ferit.dominikzivko.unistat.data.Repository
import hr.ferit.dominikzivko.unistat.isToday
import hr.ferit.dominikzivko.unistat.ui.component.BillSummary
import javafx.fxml.FXML
import javafx.scene.chart.LineChart
import javafx.scene.layout.HBox
import org.koin.core.component.KoinApiExtension
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.time.LocalDate

@KoinApiExtension
class GuiOverview : KoinComponent {
    private val repo: Repository by inject()

    @FXML
    private lateinit var billSummaryBox: HBox
    @FXML
    private lateinit var dailySpendingChart: LineChart<LocalDate, Float>

    @FXML
    fun initialize() {
        setupBillSummary()
        setupCharts()
    }

    private fun setupBillSummary() {

    }

    private fun setupCharts() {

    }
}