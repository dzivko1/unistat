package hr.ferit.dominikzivko.unistat.gui

import domyutil.jfx.*

/**
 * A card is a screen representing an independent GUI section. Each card belongs to a [Card.Section], has a title and a path
 * to the FXML file defining its content.
 */
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

    /**
     * An organizational utility which enables titled separation of cards in the UI.
     */
    enum class Section(val title: String) {
        General(""),
        Stats(strings["card_section_stats"]);
    }
}