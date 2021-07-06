package domyutil

import java.time.LocalDate
import java.time.Period
import java.time.YearMonth
import java.time.temporal.TemporalAmount

val LocalDate.yearMonth: YearMonth
    get() = YearMonth.from(this)

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


operator fun YearMonth.rangeTo(other: YearMonth) = YearMonthProgression(this, other, Period.ofMonths(1))

class YearMonthIterator(
    val start: YearMonth,
    val endInclusive: YearMonth,
    val step: TemporalAmount
) : Iterator<YearMonth> {

    private var next = start
    private var hasNext = start <= endInclusive

    override fun hasNext() = hasNext

    override fun next(): YearMonth {
        if (!hasNext) throw NoSuchElementException()

        val value = next
        next += step

        if (next > endInclusive)
            hasNext = false

        return value
    }
}

class YearMonthProgression(
    override val start: YearMonth,
    override val endInclusive: YearMonth,
    val step: TemporalAmount
) : Iterable<YearMonth>, ClosedRange<YearMonth> {
    override fun iterator() = YearMonthIterator(start, endInclusive, step)
    infix fun step(period: Period) = YearMonthProgression(start, endInclusive, period)
}