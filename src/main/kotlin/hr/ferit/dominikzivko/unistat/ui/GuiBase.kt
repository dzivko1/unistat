package hr.ferit.dominikzivko.unistat.ui

import domyutil.jfx.*
import hr.ferit.dominikzivko.unistat.AppBase
import javafx.event.Event
import javafx.fxml.FXML
import javafx.fxml.FXMLLoader
import javafx.scene.control.ToggleButton
import javafx.scene.control.ToggleGroup
import javafx.scene.layout.BorderPane
import javafx.scene.layout.Pane
import javafx.scene.layout.VBox
import org.koin.core.context.GlobalContext

class GuiBase {
    private val app: AppBase get() = GlobalContext.get().get()

    @FXML
    private lateinit var root: BorderPane

    @FXML
    private lateinit var navButtonBox: VBox

    private val navToggleGroup = ToggleGroup()
    private lateinit var cardsToButtons: Map<Card, ToggleButton>

    @FXML
    fun initialize() {
        setupNavButtons()
        showCard(Card.Overview)
    }

    private fun setupNavButtons() {
        val deselectionFilter = DeselectionFilter(navToggleGroup)
        cardsToButtons = Card.values().associateWith { card ->
            ToggleButton(card.title).apply {
                styleClass += "nav-button"
                toggleGroup = navToggleGroup
                addEventFilter(Event.ANY, deselectionFilter)
                setOnAction { showCard(card) }
            }
        }
        navButtonBox.children += cardsToButtons.values
    }

    @FXML
    private fun logout() {
        app.logout()
    }

    private fun showCard(card: Card) {
        val cardView = FXMLLoader(javaClass.getResource(card.fxmlPath)).load<Pane>()
        root.center = cardView
        navToggleGroup.selectToggle(cardsToButtons[card])
    }
}