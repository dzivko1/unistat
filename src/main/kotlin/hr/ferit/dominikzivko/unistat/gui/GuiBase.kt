package hr.ferit.dominikzivko.unistat.gui

import domyutil.jfx.*
import hr.ferit.dominikzivko.unistat.AppBase
import javafx.beans.binding.Bindings
import javafx.event.Event
import javafx.fxml.FXML
import javafx.fxml.FXMLLoader
import javafx.scene.Parent
import javafx.scene.control.Button
import javafx.scene.control.Label
import javafx.scene.control.ToggleButton
import javafx.scene.control.ToggleGroup
import javafx.scene.layout.BorderPane
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
    private lateinit var btnRefresh: Button

    @FXML
    private lateinit var lblFullName: Label

    @FXML
    private lateinit var lblInstitution: Label

    private val navToggleGroup = ToggleGroup()
    private lateinit var cardsToButtons: Map<Card, ToggleButton>
    private val cardCache = HashMap<Card, Parent>()

    @FXML
    private fun initialize() {
        btnRefresh.disableProperty().bind(app.offlineModeProperty)
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

        var lastSection = Card.Section.General
        cardsToButtons.forEach { (card, button) ->
            if (card.section != lastSection) {
                navButtonBox.children += Label(card.section.title).apply {
                    styleClass += "nav-section"
                }
                lastSection = card.section
            }
            navButtonBox.children += button
        }
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

    @FXML
    private fun exportFilteredBills() {
        app.exportFilteredBills()
    }

    @FXML
    private fun refresh() {
        app.refreshUserData()
    }

    private fun showCard(card: Card) {
        root.center = cardCache.computeIfAbsent(card) {
            FXMLLoader(javaClass.getResource(card.fxmlPath), ResourceBundle.getBundle("Strings")).load()
        }
        navToggleGroup.selectToggle(cardsToButtons[card])
    }
}