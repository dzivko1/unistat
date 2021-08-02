package hr.ferit.dominikzivko.unistat.gui.component

import domyutil.*
import domyutil.jfx.*
import hr.ferit.dominikzivko.unistat.bindData
import hr.ferit.dominikzivko.unistat.bindText
import hr.ferit.dominikzivko.unistat.data.Bill
import hr.ferit.dominikzivko.unistat.data.BillEntry
import hr.ferit.dominikzivko.unistat.enablePieTooltips
import hr.ferit.dominikzivko.unistat.gui.DATE_TIME_FORMATTER
import hr.ferit.dominikzivko.unistat.gui.floatToString
import javafx.beans.binding.Bindings
import javafx.beans.property.SimpleObjectProperty
import javafx.collections.FXCollections
import javafx.fxml.FXML
import javafx.fxml.FXMLLoader
import javafx.scene.chart.PieChart
import javafx.scene.control.Label
import javafx.scene.control.TableColumn
import javafx.scene.control.TableView
import javafx.scene.layout.VBox
import java.util.*

/**
 * A GUI component that shows the details of a single [Bill].
 */
class BillView : VBox() {

    val billProperty = SimpleObjectProperty<Bill>(this, "bill")
    var bill: Bill? by billProperty

    @FXML
    private lateinit var lblDateTime: Label

    @FXML
    private lateinit var lblSource: Label

    @FXML
    private lateinit var lblValue: Label

    @FXML
    private lateinit var lblSubsidy: Label

    @FXML
    private lateinit var lblCost: Label

    @FXML
    private lateinit var lblArticleCount: Label

    @FXML
    private lateinit var billEntriesTable: TableView<BillEntry>

    @FXML
    private lateinit var colArticle: TableColumn<BillEntry, String>

    @FXML
    private lateinit var colAmount: TableColumn<BillEntry, Number>

    @FXML
    private lateinit var colPrice: TableColumn<BillEntry, Number>

    @FXML
    private lateinit var colSubsidy: TableColumn<BillEntry, Number>

    @FXML
    private lateinit var colCost: TableColumn<BillEntry, Number>

    @FXML
    private lateinit var valueByArticleChart: PieChart

    init {
        FXMLLoader(javaClass.getResource("BillView.fxml"), ResourceBundle.getBundle("Strings")).apply {
            setRoot(this@BillView)
            setController(this@BillView)
        }.load<VBox>()
        initialize()
    }

    private fun initialize() {
        setupBillInfoPanel()
        setupBillEntriesTable()
        setupValueByArticleChart()
    }

    private fun setupBillInfoPanel() {
        lblDateTime.bindText(billProperty) {
            strings["billView_timeOfIssue"] + ": " + bill?.dateTime?.format(DATE_TIME_FORMATTER)
        }
        lblSource.bindText(billProperty) {
            strings["billView_placeOfIssue"] + ": " + bill?.source
        }
        lblValue.bindText(billProperty) {
            strings["billView_totalValue"] + ": " + bill?.let { floatToString(it.totalValue) }
        }
        lblSubsidy.bindText(billProperty) {
            strings["billView_totalSubsidy"] + ": " + bill?.let { floatToString(it.totalSubsidy) }
        }
        lblCost.bindText(billProperty) {
            strings["billView_personalCost"] + ": " + bill?.let { floatToString(it.totalCost) }
        }
        lblArticleCount.bindText(billProperty) {
            strings["billView_numberOfArticles"] + ": " + bill?.articleCount
        }
    }

    private fun setupBillEntriesTable() {
        colArticle.setCellValueFactory { Bindings.createStringBinding({ it.value.article.name }) }
        colAmount.setCellValueFactory { Bindings.createIntegerBinding({ it.value.amount }) }
        colPrice.setCellValueFactory { Bindings.createFloatBinding({ it.value.article.fPrice }) }
        colSubsidy.setCellValueFactory { Bindings.createFloatBinding({ it.value.fSubsidy }) }
        colCost.setCellValueFactory { Bindings.createFloatBinding({ it.value.totalCost }) }

        val floatFormatCellFactory = object : StringCellFactory<BillEntry, Number>() {
            override fun format(item: Number) = floatToString(item)
        }
        colPrice.cellFactory = floatFormatCellFactory
        colSubsidy.cellFactory = floatFormatCellFactory
        colCost.cellFactory = floatFormatCellFactory

        billEntriesTable.itemsProperty().bind(Bindings.createObjectBinding({
            bill?.let { FXCollections.observableArrayList(bill!!.entries) }
        }, billProperty))
    }

    private fun setupValueByArticleChart() {
        valueByArticleChart.enablePieTooltips()
        valueByArticleChart.bindData(billProperty) { pieData ->
            bill?.entries?.sortedByDescending { it.totalValue }?.forEach { entry ->
                pieData += PieChart.Data(entry.article.name, entry.totalValue.toDouble())
            }
        }
    }
}