package hr.ferit.dominikzivko.unistat.gui

import domyutil.*
import domyutil.jfx.*
import hr.ferit.dominikzivko.unistat.AppBase
import hr.ferit.dominikzivko.unistat.bindText
import hr.ferit.dominikzivko.unistat.data.Bill
import hr.ferit.dominikzivko.unistat.data.totalCost
import hr.ferit.dominikzivko.unistat.gui.component.BillView
import hr.ferit.dominikzivko.unistat.isToday
import javafx.beans.binding.Bindings
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.value.ObservableValue
import javafx.collections.FXCollections
import javafx.fxml.FXML
import javafx.geometry.Pos
import javafx.scene.control.*
import javafx.scene.effect.ColorAdjust
import javafx.scene.image.ImageView
import javafx.scene.layout.Priority
import javafx.scene.layout.VBox
import javafx.util.Callback
import org.koin.core.context.GlobalContext
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.time.temporal.WeekFields
import java.util.*

class GuiCalendar {
    private val app: AppBase by lazy { GlobalContext.get().get() }

    @FXML
    private lateinit var calTable: TableView<Week>

    @FXML
    private lateinit var colMon: TableColumn<Week, Day>

    @FXML
    private lateinit var colTue: TableColumn<Week, Day>

    @FXML
    private lateinit var colWed: TableColumn<Week, Day>

    @FXML
    private lateinit var colThu: TableColumn<Week, Day>

    @FXML
    private lateinit var colFri: TableColumn<Week, Day>

    @FXML
    private lateinit var colSat: TableColumn<Week, Day>

    @FXML
    private lateinit var colSun: TableColumn<Week, Day>


    @FXML
    private lateinit var lblYearMonth: Label


    @FXML
    private lateinit var detailsPanel: VBox

    @FXML
    private lateinit var billDetailsPanel: ScrollPane

    @FXML
    private lateinit var detailsTitle: TitledPane

    @FXML
    private lateinit var billsList: ListView<Bill>

    @FXML
    private lateinit var lblNoDaySelected: Label

    @FXML
    private lateinit var lblNoBills: Label


    private val billView = BillView()

    private val selectedMonthProperty = SimpleObjectProperty(YearMonth.now())
    private var selectedMonth by selectedMonthProperty

    private val selectedDayProperty = SimpleObjectProperty<Day?>()
    private val selectedDay by selectedDayProperty

    private val selectedBillProperty get() = billsList.selectionModel.selectedItemProperty()

    @FXML
    private fun initialize() {
        setupCalendar()
        setupDetailsPanel()
    }

    private fun setupCalendar() {

        fun setupCalendarColumn(column: TableColumn<Week, Day>, dayOfWeek: DayOfWeek) {
            column.text = dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault())
            column.cellValueFactory = CalendarCellValueFactory()
            column.cellFactory = CalendarCellFactory()
        }
        setupCalendarColumn(colMon, DayOfWeek.MONDAY)
        setupCalendarColumn(colTue, DayOfWeek.TUESDAY)
        setupCalendarColumn(colWed, DayOfWeek.WEDNESDAY)
        setupCalendarColumn(colThu, DayOfWeek.THURSDAY)
        setupCalendarColumn(colFri, DayOfWeek.FRIDAY)
        setupCalendarColumn(colSat, DayOfWeek.SATURDAY)
        setupCalendarColumn(colSun, DayOfWeek.SUNDAY)

        calTable.selectionModel.isCellSelectionEnabled = true

        val cellHeightBinding = Bindings.min(
            Bindings.max(colMon.widthProperty(), 100),
            calTable.heightProperty().divide(6).subtract(5)
        )
        calTable.fixedCellSizeProperty().bind(cellHeightBinding)

        calTable.itemsProperty().bind(Bindings.createObjectBinding({
            FXCollections.observableList(getWeeks(selectedMonth))
        }, selectedMonthProperty, app.repository.filteredBills))

        lblYearMonth.bindText(selectedMonthProperty) { selectedMonth.format(MONTH_FORMATTER) }

        selectedDayProperty.bind(Bindings.createObjectBinding({
            val selectedCell = calTable.selectionModel.selectedCells.firstOrNull()
            return@createObjectBinding selectedCell?.let {
                val selectedWeek = calTable.items[it.row]
                it.tableColumn.getCellObservableValue(selectedWeek).value as Day
            }
        }, calTable.selectionModel.selectedCells))
    }

    private fun getWeeks(yearMonth: YearMonth): List<Week> {
        val weeks = mutableListOf<Week>()

        val costByDate = app.repository.filteredBills.filter { it.date.yearMonth == yearMonth }
            .groupingBy { it.date }.fold(0.0) { acc, bill ->
                acc + bill.totalCost
            }
        val lowestCost = costByDate.values.minOrNull()
        val highestCost = costByDate.values.maxOrNull()

        val firstWeekNumber = yearMonth.atDay(1)[WeekFields.ISO.weekOfYear()]
        val lastWeekNumber = yearMonth.atEndOfMonth()[WeekFields.ISO.weekOfYear()]
        for (weekNumber in firstWeekNumber..lastWeekNumber) {

            val monday = LocalDate.of(yearMonth.year, 1, 1)
                .with(WeekFields.ISO.weekOfYear(), weekNumber.toLong())
                .with(WeekFields.ISO.dayOfWeek(), 1)

            val days = Array(7) { dayNumber ->
                val dayOfWeek = monday.plusDays(dayNumber.toLong())
                val bills = app.repository.filteredBills.filter { it.date == dayOfWeek }
                val normalizedCost = when {
                    lowestCost == null || highestCost == null || bills.totalCost == 0.0 -> null
                    highestCost == lowestCost -> 1.0
                    else -> (bills.totalCost - lowestCost) / (highestCost - lowestCost)
                }
                Day(dayOfWeek, bills, normalizedCost)
            }

            weeks += Week(days)
        }
        return weeks
    }

    private fun setupDetailsPanel() {
        lblNoDaySelected.visibleProperty().bind(selectedDayProperty.isNull)
        lblNoBills.visibleProperty().bind(Bindings.createBooleanBinding({
            selectedDay != null && selectedDay?.billCount == 0
        }, selectedDayProperty))
        detailsPanel.visibleProperty().bind(selectedBillProperty.isNotNull)

        detailsTitle.textProperty().bind(Bindings.createStringBinding({
            strings["calendar_bills"] + "  " + selectedDay?.date?.format(DATE_FORMATTER)
        }, selectedDayProperty))

        billsList.setCellFactory { BillListCell() }
        billsList.itemsProperty().bind(Bindings.createObjectBinding({
            selectedDay?.let { FXCollections.observableList(it.bills) }
        }, selectedDayProperty))

        selectedDayProperty.addListener { _, _, _ -> billsList.selectionModel.selectFirst() }

        billView.visibleProperty().bind(selectedBillProperty.isNotNull)
        billView.billProperty.bind(selectedBillProperty)
        billDetailsPanel.content = billView
    }

    @FXML
    private fun prevMonth() {
        selectedMonth = selectedMonth.minusMonths(1)
    }

    @FXML
    private fun nextMonth() {
        selectedMonth = selectedMonth.plusMonths(1)
    }


    private inner class CalendarCellValueFactory :
        Callback<TableColumn.CellDataFeatures<Week, Day>, ObservableValue<Day>> {
        override fun call(cellData: TableColumn.CellDataFeatures<Week, Day>): ObservableValue<Day> {
            return Bindings.createObjectBinding({
                cellData.value[cellData.tableColumn.id.toInt()]
            })
        }
    }

    private inner class CalendarCellFactory : Callback<TableColumn<Week, Day>, TableCell<Week, Day>> {
        override fun call(column: TableColumn<Week, Day>): TableCell<Week, Day> {
            return CalendarTableCell()
        }
    }

    private inner class CalendarTableCell : TableCell<Week, Day>() {
        private val lblDay = Label()
        private val lblCost = Label()
        private val lblBills = Label()
        private val content = VBox(lblDay, lblCost, lblBills)

        init {
            content.alignment = Pos.CENTER

            lblDay.styleClass += "calendar-day-label"
            lblCost.styleClass += "calendar-cost-label"
            lblBills.styleClass += "calendar-bills-label"
            lblBills.graphic = ImageView("/hr/ferit/dominikzivko/unistat/gui/images/bill-small.png").apply {
                fitWidth = 20.0
                fitHeight = 20.0
                effect = ColorAdjust(0.0, 1.0, 0.5, -1.0)
            }

            VBox.setVgrow(lblCost, Priority.ALWAYS)
        }

        override fun updateItem(item: Day?, empty: Boolean) {
            super.updateItem(item, empty)

            graphic = if (empty || item == null) null
            else {
                updateContent(item)
                content
            }
        }

        private fun updateContent(day: Day) {
            lblDay.text = day.date.dayOfMonth.toString()
            lblBills.text = day.billCount.toString()
            lblCost.text = if (day.bills.totalCost != 0.0) floatToString(day.bills.totalCost) else ""
            lblBills.isVisible = day.billCount != 0

            when {
                day.date.isToday() -> {
                    style = "-fx-calendar-cell-background: -fx-today-color"
                    effect = null
                }
                day.date.yearMonth != selectedMonth -> {
                    style = "-fx-calendar-cell-background: white"
                    effect = ColorAdjust().apply {
                        brightness = -0.17
                    }
                }
                else -> {
                    val highlight = if (day.normalizedCost != null) (15.0 + day.normalizedCost * 55.0) else 0.0
                    style = "-fx-calendar-cell-background: derive(-fx-accent, ${100.0 - highlight}%)"
                    effect = null
                }
            }
        }
    }

    private class BillListCell : ListCell<Bill>() {
        override fun updateItem(item: Bill?, empty: Boolean) {
            super.updateItem(item, empty)
            text = if (empty || item == null) null
            else item.dateTime.format(TIME_FORMATTER) + " - " + floatToString(item.totalCost)
        }
    }

    private class Week(
        private val days: Array<Day>
    ) {
        operator fun get(dayId: Int): Day = days[dayId]
    }

    private class Day(
        val date: LocalDate,
        val bills: List<Bill>,
        val normalizedCost: Double?
    ) {
        val billCount get() = bills.size
    }
}