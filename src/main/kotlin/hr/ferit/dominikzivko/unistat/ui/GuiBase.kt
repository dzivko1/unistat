package hr.ferit.dominikzivko.unistat.ui

import domyutil.jfx.*
import hr.ferit.dominikzivko.unistat.AppBase
import javafx.beans.binding.Bindings
import javafx.event.Event
import javafx.fxml.FXML
import javafx.fxml.FXMLLoader
import javafx.scene.control.Label
import javafx.scene.control.ToggleButton
import javafx.scene.control.ToggleGroup
import javafx.scene.layout.BorderPane
import javafx.scene.layout.Pane
import javafx.scene.layout.VBox
import org.koin.core.context.GlobalContext
import java.util.*

class GuiBase {
    private val app: AppBase by lazy { GlobalContext.get().get() }

    @FXML
    private lateinit var root: BorderPane

    @FXML
    private lateinit var navButtonBox: VBox

    @FXML
    private lateinit var lblFullName: Label

    @FXML
    private lateinit var lblInstitution: Label

    private val navToggleGroup = ToggleGroup()
    private lateinit var cardsToButtons: Map<Card, ToggleButton>

    @FXML
    fun initialize() {
        setupNavButtons()
        setupUserInfoPanel()
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

    private fun setupUserInfoPanel() {
        lblFullName.textProperty().bind(
            Bindings.createStringBinding({ app.repository.user?.fullName.orEmpty() }, app.repository.userProperty)
        )
        lblInstitution.textProperty().bind(
            Bindings.createStringBinding({ app.repository.user?.institution.orEmpty() }, app.repository.userProperty)
        )
    }

    @FXML
    private fun logout() {
        app.logout()
    }

    private fun showCard(card: Card) {
        val cardView =
            FXMLLoader(javaClass.getResource(card.fxmlPath), ResourceBundle.getBundle("Strings")).load<Pane>()
        root.center = cardView
        navToggleGroup.selectToggle(cardsToButtons[card])
    }
}