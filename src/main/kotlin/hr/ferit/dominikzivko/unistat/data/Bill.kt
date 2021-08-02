package hr.ferit.dominikzivko.unistat.data

import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import org.jetbrains.exposed.dao.LongEntity
import org.jetbrains.exposed.dao.LongEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.`java-time`.datetime
import java.time.LocalDateTime
import java.util.*

val List<Bill>.articleCount get() = sumOf { it.articleCount }
val List<Bill>.totalValue get() = sumOf { it.totalValue }
val List<Bill>.totalSubsidy get() = sumOf { it.totalSubsidy }
val List<Bill>.totalCost get() = sumOf { it.totalCost }

@Serializable
data class Bill(
    @Serializable(with = LocalDateTimeSerializer::class)
    val dateTime: LocalDateTime,
    val source: String,
    @Transient
    val user: UUID = LOCAL_USER.id,
    val entries: List<BillEntry>
) {
    constructor(dao: BillDAO) : this(
        dao.dateTime,
        dao.source,
        dao.user.id.value,
        dao.entries.map { BillEntry(it) }
    )

    val date get() = dateTime.toLocalDate()!!
    val articles get() = entries.map { it.article }
    val articleCount get() = entries.totalAmount
    val totalValue get() = entries.totalValue
    val totalSubsidy get() = entries.totalSubsidy
    val totalCost get() = entries.totalCost

    /**
     * Checks if the contents of this bill are equal to the contents of the other bill, ignoring the user property.
     * Two bills are equal by content if all of their properties besides the user property are equal.
     */
    fun contentEquals(other: Bill) =
        dateTime == other.dateTime && source == other.source && entries.toSet() == other.entries.toSet()
}

object Bills : LongIdTable() {
    val dateTime = datetime("dateTime")
    val billSource = varchar("source", 100)
    val user = reference("user", Users)
}

class BillDAO(id: EntityID<Long>) : LongEntity(id) {
    companion object : LongEntityClass<BillDAO>(Bills)

    var dateTime by Bills.dateTime
    var source by Bills.billSource
    var user by UserDAO referencedOn Bills.user
    val entries by BillEntryDAO referrersOn BillEntries.bill
}