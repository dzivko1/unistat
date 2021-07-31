package hr.ferit.dominikzivko.unistat.gui.component

import domyutil.jfx.*
import javafx.beans.property.SimpleIntegerProperty
import javafx.geometry.Insets
import javafx.geometry.Pos
import javafx.scene.control.Button
import javafx.scene.control.Label
import javafx.scene.control.Spinner
import javafx.scene.layout.HBox
import javafx.util.StringConverter

/**
 * A GUI component for controlling the amount of chart entries that should be shown. It contains a spinner for choosing
 * the number and a button for setting all possible entries to be shown.
 */
class ChartControlPanel : HBox() {

    val entryCountProperty = SimpleIntegerProperty(this, "entryCount")
    val entryCount: Number by entryCountProperty

    init {
        alignment = Pos.CENTER_LEFT
        padding = Insets(0.0, 10.0, 0.0, 10.0)
        spacing = 5.0

        children += Label(strings["chart_numberOfEntries"] + ":")
        val spinner = Spinner<Int>(0, Int.MAX_VALUE, 30, 1).apply {
            prefWidth = 80.0
            isEditable = true
            valueFactory.converter = object : StringConverter<Int>() {
                override fun toString(original: Int?): String {
                    return if (original == 0) strings["chart_all"] else original?.toString().orEmpty()
                }

                override fun fromString(formatted: String?): Int? {
                    return if (formatted == strings["chart_all"]) 0 else formatted?.toInt()
                }
            }
            this@ChartControlPanel.entryCountProperty.bind(this.valueProperty())
        }
        children += spinner
        children += Button(strings["btn_all"]).apply {
            setOnAction { spinner.valueFactory.value = 0 }
        }
    }
}