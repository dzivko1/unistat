package hr.ferit.dominikzivko.unistat.ui

import domyutil.jfx.*
import javafx.event.Event
import javafx.fxml.FXML
import javafx.fxml.FXMLLoader
import javafx.scene.control.ToggleButton
import javafx.scene.control.ToggleGroup
import javafx.scene.layout.BorderPane
import javafx.scene.layout.Pane
import javafx.scene.layout.VBox

class GuiBase {
    @FXML
    private lateinit var root: BorderPane

    @FXML
    private lateinit var navButtonBox: VBox

    private val navToggleGroup = ToggleGroup()

    @FXML
    fun initialize() {
        setupNavButtons()

        navToggleGroup.selectedToggleProperty().addListener { _, _, newValue ->
            showCard(newValue.userData as Card)
        }

        showCard(Card.Overview)
    }

    private fun setupNavButtons() {
        for (card in Card.values()) {
            with(ToggleButton(card.title)) {
                styleClass += "nav-button"
                toggleGroup = navToggleGroup
                userData = card
                addEventFilter(Event.ANY, DeselectionFilter(navToggleGroup))
                navButtonBox.children += this

                if (card == Card.Overview) isSelected = true
            }
        }
    }

    private fun showCard(card: Card) {
        val cardView = FXMLLoader(javaClass.getResource(card.fxmlPath)).load<Pane>()
        root.center = cardView
    }
}