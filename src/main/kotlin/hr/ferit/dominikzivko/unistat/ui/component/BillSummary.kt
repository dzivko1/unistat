package hr.ferit.dominikzivko.unistat.ui.component

import domyutil.jfx.*
import hr.ferit.dominikzivko.unistat.articleCount
import hr.ferit.dominikzivko.unistat.data.Bill
import hr.ferit.dominikzivko.unistat.totalCost
import hr.ferit.dominikzivko.unistat.totalValue
import hr.ferit.dominikzivko.unistat.ui.floatToString
import javafx.beans.binding.Bindings
import javafx.beans.property.SimpleListProperty
import javafx.beans.property.SimpleStringProperty
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

class BillSummary(
    initialTitle: String,
    observableBills: ObservableList<Bill> = FXCollections.emptyObservableList()
) : GridPane() {

    val titleProperty = SimpleStringProperty(this, "title", initialTitle)
    var title: String by titleProperty

    val billsProperty = SimpleListProperty(this, "bills", observableBills)
    var bills: ObservableList<Bill> by billsProperty

    init {
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

        val lblBills = Label().bindText(Bindings.size(billsProperty).asString())
        val lblArticles = Label().bindText(
            Bindings.createStringBinding({ bills.articleCount.toString() }, billsProperty)
        )
        val lblValue = Label().bindText(
            Bindings.createStringBinding({ floatToString(bills.totalValue) }, billsProperty)
        )
        val lblCost = Label().bindText(
            Bindings.createStringBinding({ floatToString(bills.totalCost) }, billsProperty)
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