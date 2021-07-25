package hr.ferit.dominikzivko.unistat.gui.component

import domyutil.jfx.*
import hr.ferit.dominikzivko.unistat.boundLabelFor
import hr.ferit.dominikzivko.unistat.data.Bill
import hr.ferit.dominikzivko.unistat.data.articleCount
import hr.ferit.dominikzivko.unistat.data.totalCost
import hr.ferit.dominikzivko.unistat.data.totalValue
import hr.ferit.dominikzivko.unistat.gui.floatToString
import javafx.beans.binding.Bindings
import javafx.beans.property.SimpleListProperty
import javafx.beans.property.SimpleStringProperty
import javafx.collections.FXCollections
import javafx.collections.ObservableList
import javafx.geometry.HPos
import javafx.geometry.Insets
import javafx.geometry.Pos
import javafx.scene.control.Label
import javafx.scene.layout.ColumnConstraints
import javafx.scene.layout.GridPane
import javafx.scene.layout.Priority

class BillSummary(
    initialTitle: String,
    observableBills: ObservableList<Bill> = FXCollections.emptyObservableList()
) : GridPane() {

    private val titleProperty = SimpleStringProperty(this, "title", initialTitle)
    private var title: String by titleProperty

    private val billsProperty = SimpleListProperty<Bill>(this, "bills")
    private var bills: ObservableList<Bill> by billsProperty

    init {
        // avoiding memory leaks
        billsProperty.bind(Bindings.createObjectBinding({
            FXCollections.observableList(observableBills)
        }, observableBills))

        prefWidth = 180.0
        minWidth = 150.0
        padding = Insets(10.0)

        columnConstraints += listOf(
            ColumnConstraints().apply { hgrow = Priority.ALWAYS },
            ColumnConstraints().apply { halignment = HPos.RIGHT }
        )

        val lblTitle = Label().apply {
            styleClass += "title"
            maxWidth = Double.POSITIVE_INFINITY
            alignment = Pos.CENTER
            textProperty().bind(titleProperty)
        }
        val lblBills = boundLabelFor(billsProperty) { bills.size.toString() }
        val lblArticles = boundLabelFor(billsProperty) { bills.articleCount.toString() }
        val lblValue = boundLabelFor(billsProperty) { floatToString(bills.totalValue) }
        val lblCost = boundLabelFor(billsProperty) { floatToString(bills.totalCost) }

        add(lblTitle, 0, 0, 2, 1)
        addRow(1, Label(strings["summary_bills"] + ":"), lblBills)
        addRow(2, Label(strings["summary_articles"] + ":"), lblArticles)
        addRow(3, Label(strings["summary_value"] + ":"), lblValue)
        addRow(4, Label(strings["summary_cost"] + ":"), lblCost)
    }
}