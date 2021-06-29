package hr.ferit.dominikzivko.unistat.ui.component

import domyutil.jfx.*
import hr.ferit.dominikzivko.unistat.data.Bill
import javafx.beans.binding.Bindings
import javafx.beans.property.SimpleListProperty
import javafx.beans.value.ObservableValue
import javafx.collections.FXCollections
import javafx.collections.ObservableList
import javafx.geometry.HPos
import javafx.geometry.Insets
import javafx.geometry.Pos
import javafx.scene.control.Label
import javafx.scene.layout.ColumnConstraints
import javafx.scene.layout.GridPane
import javafx.scene.layout.Priority

class BillSummary(title: String, observableBills: ObservableList<Bill>) : GridPane() {

    val billsProperty = SimpleListProperty(this, "bills", FXCollections.observableArrayList(observableBills))
    var bills by billsProperty

    init {
        prefWidth = 180.0
        minWidth = 150.0
        padding = Insets(10.0)

        columnConstraints += listOf(
            ColumnConstraints().apply { hgrow = Priority.ALWAYS },
            ColumnConstraints().apply { halignment = HPos.RIGHT }
        )

        val lblTitle = Label(title).apply {
            styleClass += "title"
            maxWidth = Double.POSITIVE_INFINITY
            alignment = Pos.CENTER
        }

        val lblBills = Label().bindText(Bindings.size(bills).asString())
        val lblArticles = Label().bindText(
            Bindings.createStringBinding({ bills.sumOf { it.articleCount }.toString() }, bills)
        )
        val lblValue = Label().bindText(
            Bindings.createStringBinding({ "%.2f".format(bills.sumOf { it.totalValue }) }, bills)
        )
        val lblCost = Label().bindText(
            Bindings.createStringBinding({ "%.2f".format(bills.sumOf { it.totalValue - it.totalSubsidy }) }, bills)
        )

        add(lblTitle, 0, 0, 2, 1)
        addRow(1, Label(strings["summary_bills"] + ":"), lblBills)
        addRow(2, Label(strings["summary_articles"] + ":"), lblArticles)
        addRow(3, Label(strings["summary_value"] + ":"), lblValue)
        addRow(4, Label(strings["summary_cost"] + ":"), lblCost)
    }

    private fun Label.bindText(observable: ObservableValue<String>): Label {
        textProperty().bind(observable)
        return this
    }
}