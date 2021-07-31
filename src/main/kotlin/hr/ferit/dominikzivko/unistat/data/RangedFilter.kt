package hr.ferit.dominikzivko.unistat.data

import domyutil.jfx.*
import javafx.beans.binding.Bindings
import javafx.beans.property.SimpleObjectProperty
import javafx.collections.FXCollections
import javafx.collections.ObservableList
import javafx.collections.transformation.FilteredList
import java.util.function.Predicate

/**
 * An [ObservableList] wrapper which provides the ability to filter a list by a given range of comparable values.
 *
 * This class takes the specified source list and wraps it in a [FilteredList], exposing its unmodifiable view.
 * The predicate of the filtered list depends on the lower and upper range bound properties of this class.
 */
class RangedFilter<T, S : Comparable<S>>(
    source: ObservableList<T>,
    extractor: (T) -> S
) {
    private val filteredList = FilteredList(source)
    val filteredView: ObservableList<T> = FXCollections.unmodifiableObservableList(filteredList)

    val lowerBoundProperty = SimpleObjectProperty<S?>(this, "lowerBound")
    var lowerBound: S? by lowerBoundProperty

    val upperBoundProperty = SimpleObjectProperty<S?>(this, "upperBound")
    var upperBound: S? by upperBoundProperty

    init {
        filteredList.predicateProperty().bind(Bindings.createObjectBinding({
            Predicate {
                val feature = extractor(it)
                val start = lowerBound
                val end = upperBound
                when {
                    start != null && end != null -> feature in start..end
                    start != null -> feature >= start
                    end != null -> feature <= end
                    else -> true
                }
            }
        }, lowerBoundProperty, upperBoundProperty))
    }
}