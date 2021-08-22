package hr.ferit.dominikzivko.unistat.gui.component

import domyutil.jfx.*
import hr.ferit.dominikzivko.unistat.*
import hr.ferit.dominikzivko.unistat.data.*
import hr.ferit.dominikzivko.unistat.gui.formatted
import javafx.beans.binding.Bindings
import javafx.beans.property.SimpleObjectProperty
import javafx.fxml.FXML
import javafx.fxml.FXMLLoader
import javafx.scene.chart.PieChart
import javafx.scene.control.Label
import javafx.scene.layout.GridPane
import javafx.scene.layout.HBox
import java.util.*

/**
 * A GUI component that shows the details of a single [Article] with all the [Bill]s that contain it as context from
 * which the details are gathered.
 */
class ArticleView : HBox() {

    /**
     * A wrapper for an [Article] and [Bill]s relevant to it, to be shown in an [ArticleView].
     */
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
        FXMLLoader(javaClass.getResource("ArticleView.fxml"), ResourceBundle.getBundle("Strings")).apply {
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
        val lblAmount = boundLabelFor(dataProperty) { data?.relevantEntries?.totalAmount?.formatted }
        val lblValue = boundLabelFor(dataProperty) { data?.relevantEntries?.totalValue?.formatted }
        val lblSubsidy = boundLabelFor(dataProperty) { data?.relevantEntries?.totalSubsidy?.formatted }
        val lblCost = boundLabelFor(dataProperty) { data?.relevantEntries?.totalCost?.formatted }
        detailsGrid.addRow(0, Label(strings["articleView_amountBought"] + ": "), lblAmount)
        detailsGrid.addRow(1, Label(strings["articleView_totalValue"] + ": "), lblValue)
        detailsGrid.addRow(2, Label(strings["articleView_totalSubsidy"] + ": "), lblSubsidy)
        detailsGrid.addRow(3, Label(strings["articleView_totalCost"] + ": "), lblCost)
    }

    private fun setupAmountBySourceChart() {
        amountBySourceChart.enablePieTooltips(isValueInteger = true)
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