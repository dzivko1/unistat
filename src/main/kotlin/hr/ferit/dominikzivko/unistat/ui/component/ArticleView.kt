package hr.ferit.dominikzivko.unistat.ui.component

import domyutil.jfx.*
import hr.ferit.dominikzivko.unistat.*
import hr.ferit.dominikzivko.unistat.data.*
import hr.ferit.dominikzivko.unistat.ui.floatToString
import javafx.beans.binding.Bindings
import javafx.beans.property.SimpleObjectProperty
import javafx.fxml.FXML
import javafx.fxml.FXMLLoader
import javafx.scene.chart.PieChart
import javafx.scene.control.Label
import javafx.scene.layout.GridPane
import javafx.scene.layout.HBox
import java.util.*

class ArticleView : HBox() {

    class Data(
        val article: Article,
        val relevantBills: List<Bill>
    ) {
        val relevantEntries: List<BillEntry> =
            relevantBills.map { bill -> bill.entries.find { it.article == article }!! }
    }

    val dataProperty = SimpleObjectProperty<Data?>(this, "data")
    val data: Data? by dataProperty

    @FXML
    private lateinit var detailsGrid: GridPane

    @FXML
    private lateinit var amountBySourceChart: PieChart

    init {
        FXMLLoader(javaClass.getResource("/gui/ArticleView.fxml"), ResourceBundle.getBundle("Strings")).apply {
            setRoot(this@ArticleView)
            setController(this@ArticleView)
        }.load<HBox>()
        initialize()
    }

    private fun initialize() {
        setupDetailsGrid()
        setupAmountBySourceChart()
    }

    private fun setupDetailsGrid() {
        val lblAmount = boundLabelFor(dataProperty) { data?.relevantEntries?.totalAmount.toString() }
        val lblValue = boundLabelFor(dataProperty) { data?.let { floatToString(it.relevantEntries.totalValue) } }
        val lblSubsidy = boundLabelFor(dataProperty) { data?.let { floatToString(it.relevantEntries.totalSubsidy) } }
        val lblCost = boundLabelFor(dataProperty) { data?.let { floatToString(it.relevantEntries.totalCost) } }
        detailsGrid.addRow(0, Label(strings["articleView_amountBought"] + ": "), lblAmount)
        detailsGrid.addRow(1, Label(strings["articleView_totalValue"] + ": "), lblValue)
        detailsGrid.addRow(2, Label(strings["articleView_totalSubsidy"] + ": "), lblSubsidy)
        detailsGrid.addRow(3, Label(strings["articleView_totalCost"] + ": "), lblCost)
    }

    private fun setupAmountBySourceChart() {
        amountBySourceChart.enablePieTooltips()
        amountBySourceChart.bindData(dataProperty) { pieData ->
            data?.relevantBills?.groupBy { it.source }?.forEach { (source, bills) ->
                val amount = bills.sumOf { bill ->
                    bill.entries.find { it.article == data?.article }!!.amount
                }
                pieData += PieChart.Data(source, amount.toDouble())
            }
        }
        amountBySourceChart.titleProperty().bind(Bindings.createStringBinding({
            // the call to layout() fixes a mysterious pie rendering glitch
            data?.article?.name.also { amountBySourceChart.parent.layout() }
        }, dataProperty))
    }
}