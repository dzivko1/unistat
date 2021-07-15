package hr.ferit.dominikzivko.unistat.ui

import domyutil.jfx.*

enum class Card(
    val section: Section,
    val title: String,
    val fxmlPath: String
) {
    Overview(Section.General, strings["card_overview"], "/gui/Overview.fxml"),
    Bills(Section.General, strings["card_bills"], "/gui/Bills.fxml"),
    Calendar(Section.General, strings["card_calendar"], "/gui/Calendar.fxml"),
    GeneralStats(Section.Stats, strings["card_generalStats"], "/gui/GeneralStats.fxml"),
    ArticleStats(Section.Stats, strings["card_articleStats"], "/gui/ArticleStats.fxml");

    enum class Section(val title: String) {
        General(""),
        Stats(strings["card_section_stats"]);
    }
}