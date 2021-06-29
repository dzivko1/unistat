package hr.ferit.dominikzivko.unistat.ui

import domyutil.jfx.*

enum class Card(
    val title: String,
    val fxmlPath: String
) {
    Overview(strings["card_overview"], "/gui/Overview.fxml"),
    Bills(strings["card_bills"], "/gui/Bills.fxml"),
    Calendar(strings["card_calendar"], "/gui/Overview.fxml"),
}