package hr.ferit.dominikzivko.unistat.gui

import domyutil.jfx.*

enum class Card(
    val section: Section,
    val title: String,
    val fxmlPath: String
) {
    Overview(Section.General, strings["card_overview"], "Overview.fxml"),
    Bills(Section.General, strings["card_bills"], "Bills.fxml"),
    Calendar(Section.General, strings["card_calendar"], "Calendar.fxml"),
    GeneralStats(Section.Stats, strings["card_generalStats"], "GeneralStats.fxml"),
    ArticleStats(Section.Stats, strings["card_articleStats"], "ArticleStats.fxml");

    enum class Section(val title: String) {
        General(""),
        Stats(strings["card_section_stats"]);
    }
}