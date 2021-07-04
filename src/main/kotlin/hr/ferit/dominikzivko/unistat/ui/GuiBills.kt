package hr.ferit.dominikzivko.unistat.ui

import domyutil.*
import domyutil.jfx.*
import hr.ferit.dominikzivko.unistat.AppBase
import hr.ferit.dominikzivko.unistat.data.Bill
import hr.ferit.dominikzivko.unistat.ui.component.BillView
import javafx.beans.binding.Bindings
import javafx.collections.transformation.SortedList
import javafx.fxml.FXML
import javafx.scene.control.Label
import javafx.scene.control.TableColumn
import javafx.scene.control.TableView
import javafx.scene.layout.StackPane
import org.koin.core.context.GlobalContext
import java.time.LocalDateTime

class GuiBills {
    private val app: AppBase by lazy { GlobalContext.get().get() }

    @FXML
    private lateinit var billsTable: TableView<Bill>

    @FXML
    private lateinit var colSource: TableColumn<Bill, String>

    @FXML
    private lateinit var colArticleCount: TableColumn<Bill, Number>

    @FXML
    private lateinit var colValue: TableColumn<Bill, Number>

    @FXML
    private lateinit var colSubsidy: TableColumn<Bill, Number>

    @FXML
    private lateinit var colCost: TableColumn<Bill, Number>

    @FXML
    private lateinit var colDateTime: TableColumn<Bill, LocalDateTime>


    @FXML
    private lateinit var detailsPanel: StackPane

    @FXML
    private lateinit var lblNoBill: Label

    private val billView = BillView()

    private val selectedBillProperty get() = billsTable.selectionModel.selectedItemProperty()

    @FXML
    private fun initialize() {
        setupBillsTable()
        setupDetailsPanel()
    }

    private fun setupBillsTable() {
        colSource.setCellValueFactory { Bindings.createStringBinding({ it.value.source }) }
        colArticleCount.setCellValueFactory { Bindings.createIntegerBinding({ it.value.articleCount }) }
        colValue.setCellValueFactory { Bindings.createDoubleBinding({ it.value.totalValue }) }
        colSubsidy.setCellValueFactory { Bindings.createDoubleBinding({ it.value.totalSubsidy }) }
        colCost.setCellValueFactory { Bindings.createDoubleBinding({ it.value.totalCost }) }
        colDateTime.setCellValueFactory { Bindings.createObjectBinding({ it.value.dateTime }) }

        val floatFormatCellFactory = object : StringCellFactory<Bill, Number>() {
            override fun format(item: Number) = item.toString("%.2f")
        }
        colValue.cellFactory = floatFormatCellFactory
        colSubsidy.cellFactory = floatFormatCellFactory
        colCost.cellFactory = floatFormatCellFactory
        colDateTime.setStringCellFactory { it.format(APP_DATE_TIME_FORMATTER) }

        val sortedBills = SortedList(app.repository.bills)
        sortedBills.comparatorProperty().bind(billsTable.comparatorProperty())
        billsTable.items = sortedBills
    }

    private fun setupDetailsPanel() {
        lblNoBill.visibleProperty().bind(selectedBillProperty.isNull)
        billView.visibleProperty().bind(selectedBillProperty.isNotNull)
        billView.billProperty.bind(selectedBillProperty)
        detailsPanel.children += billView
    }
}