package hr.ferit.dominikzivko.unistat.ui.component

import domyutil.*
import domyutil.jfx.*
import hr.ferit.dominikzivko.unistat.data.Bill
import hr.ferit.dominikzivko.unistat.data.BillEntry
import hr.ferit.dominikzivko.unistat.ui.APP_DATE_TIME_FORMATTER
import hr.ferit.dominikzivko.unistat.ui.floatToString
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
        FXMLLoader(javaClass.getResource("/gui/BillView.fxml"), ResourceBundle.getBundle("Strings")).apply {
            setRoot(this@BillView)
            setController(this@BillView)
        }.load<VBox>()
        initialize()
    }

    private fun initialize() {
        colArticle.setCellValueFactory { Bindings.createStringBinding({ it.value.article.name }) }
        colAmount.setCellValueFactory { Bindings.createIntegerBinding({ it.value.amount }) }
        colPrice.setCellValueFactory { Bindings.createFloatBinding({ it.value.article.price }) }
        colSubsidy.setCellValueFactory { Bindings.createFloatBinding({ it.value.subsidy }) }
        colCost.setCellValueFactory { Bindings.createFloatBinding({ it.value.totalCost }) }

        val floatFormatCellFactory = object : StringCellFactory<BillEntry, Number>() {
            override fun format(item: Number) = floatToString(item)
        }
        colPrice.cellFactory = floatFormatCellFactory
        colSubsidy.cellFactory = floatFormatCellFactory
        colCost.cellFactory = floatFormatCellFactory

        billProperty.addListener { _, _, newValue -> newValue?.let { populate(newValue) } }
    }

    private fun populate(bill: Bill) {
        lblDateTime.text = strings["billView_timeOfIssue"] + ": " + bill.dateTime.format(APP_DATE_TIME_FORMATTER)
        lblSource.text = strings["billView_placeOfIssue"] + ": " + bill.source
        lblValue.text = strings["billView_totalValue"] + ": " + floatToString(bill.totalValue)
        lblSubsidy.text = strings["billView_totalSubsidy"] + ": " + floatToString(bill.totalSubsidy)
        lblCost.text = strings["billView_personalCost"] + ": " + floatToString(bill.totalCost)
        lblArticleCount.text = strings["billView_numberOfArticles"] + ": " + bill.articleCount

        billEntriesTable.items = FXCollections.observableArrayList(bill.entries)

        valueByArticleChart.data.clear()
        bill.entries.sortedByDescending { it.totalValue }.forEach { entry ->
            valueByArticleChart.data.add(PieChart.Data(entry.article.name, entry.totalValue.toDouble()))
        }
    }
}