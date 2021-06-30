package domyutil

import java.time.LocalDate
import java.time.Period
import java.time.temporal.TemporalAmount

operator fun LocalDate.rangeTo(other: LocalDate) = LocalDateProgression(this, other, Period.ofDays(1))

class LocalDateIterator(
    val start: LocalDate,
    val endInclusive: LocalDate,
    val step: TemporalAmount
) : Iterator<LocalDate> {

    private var next = start
    private var hasNext = start <= endInclusive

    override fun hasNext() = hasNext

    override fun next(): LocalDate {
        if (!hasNext) throw NoSuchElementException()

        val value = next
        next += step

        if (next > endInclusive)
            hasNext = false

        return value
    }
}

class LocalDateProgression(
    override val start: LocalDate,
    override val endInclusive: LocalDate,
    val step: TemporalAmount
) : Iterable<LocalDate>, ClosedRange<LocalDate> {
    override fun iterator() = LocalDateIterator(start, endInclusive, step)
    infix fun step(period: Period) = LocalDateProgression(start, endInclusive, period)
}