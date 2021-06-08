package hr.ferit.dominikzivko.unistat.ui

import domyutil.jfx.*

enum class Card(
    val title: String,
    val fxmlPath: String
) {
    Overview(strings["overview"], "/gui/Overview.fxml"),
    Bills(strings["bills"], "/gui/Bills.fxml"),
    Calendar(strings["calendar"], "/gui/Overview.fxml"),
}