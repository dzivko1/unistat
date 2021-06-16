package hr.ferit.dominikzivko.unistat.data

import org.jetbrains.exposed.dao.LongEntity
import org.jetbrains.exposed.dao.LongEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.`java-time`.datetime
import java.time.LocalDateTime
import java.util.*

data class Bill(
    val dateTime: LocalDateTime,
    val source: String,
    val user: UUID,
    val entries: List<BillEntry>,
    val id: Long? = null
) {
    constructor(dao: BillDAO) : this(
        dao.dateTime,
        dao.source,
        dao.user.id.value,
        /*BillEntries.select { BillEntries.bill eq dao.id }.map { BillEntry(BillDAO.findById(it[BillEntries.bill])!!,
            ArticleDAO.findById(it[BillEntries.article])!!
        ) },*/
        dao.entries.map { BillEntry(it) },
        dao.id.value
    )
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