package hr.ferit.dominikzivko.unistat.gui

import com.jfoenix.controls.JFXDatePicker
import domyutil.jfx.*
import hr.ferit.dominikzivko.unistat.AppBase
import hr.ferit.dominikzivko.unistat.data.Bill
import hr.ferit.dominikzivko.unistat.gui.component.BillView
import javafx.beans.binding.Bindings
import javafx.collections.transformation.SortedList
import javafx.fxml.FXML
import javafx.scene.control.Button
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
    private lateinit var startDatePicker: JFXDatePicker

    @FXML
    private lateinit var endDatePicker: JFXDatePicker

    @FXML
    private lateinit var btnEarliest: Button

    @FXML
    private lateinit var btnLatest: Button


    @FXML
    private lateinit var detailsPanel: StackPane

    @FXML
    private lateinit var lblNoBill: Label

    private val billView = BillView()

    private val selectedBillProperty get() = billsTable.selectionModel.selectedItemProperty()

    @FXML
    private fun initialize() {
        setupBillsTable()
        setupDateRangePicker()
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
            override fun format(item: Number) = floatToString(item)
        }
        colValue.cellFactory = floatFormatCellFactory
        colSubsidy.cellFactory = floatFormatCellFactory
        colCost.cellFactory = floatFormatCellFactory
        colDateTime.setStringCellFactory { it.format(DATE_TIME_FORMATTER) }

        val sortedBills = SortedList(app.repository.filteredBills)
        sortedBills.comparatorProperty().bind(billsTable.comparatorProperty())
        billsTable.sortOrder.setAll(colDateTime)
        billsTable.items = sortedBills
    }

    private fun setupDateRangePicker() {
        btnEarliest.disableProperty().bind(Bindings.createBooleanBinding({
            startDatePicker.value == app.repository.earliestBillDate
        }, startDatePicker.valueProperty(), app.repository.bills))

        btnLatest.disableProperty().bind(Bindings.createBooleanBinding({
            endDatePicker.value == app.repository.latestBillDate
        }, endDatePicker.valueProperty(), app.repository.bills))

        with(app.repository.billFilter) {
            Bindings.bindBidirectional(startDatePicker.valueProperty(), lowerBoundProperty)
            Bindings.bindBidirectional(endDatePicker.valueProperty(), upperBoundProperty)
        }
    }

    private fun setupDetailsPanel() {
        lblNoBill.visibleProperty().bind(selectedBillProperty.isNull)
        billView.visibleProperty().bind(selectedBillProperty.isNotNull)
        billView.billProperty.bind(selectedBillProperty)
        detailsPanel.children += billView
    }

    @FXML
    private fun setToEarliest() {
        startDatePicker.value = app.repository.earliestBillDate
    }

    @FXML
    private fun setToLatest() {
        endDatePicker.value = app.repository.latestBillDate
    }
}