package hr.ferit.dominikzivko.unistat.data

import org.jetbrains.exposed.dao.LongEntity
import org.jetbrains.exposed.dao.LongEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.select

data class BillEntry(
    val article: Article,
    val amount: Int,
    val subsidy: Float,
    val id: Long? = null
) {
    /*constructor(billDAO: BillDAO, articleDAO: ArticleDAO) : this(BillEntries.select { (BillEntries.bill eq billDAO.id) and (BillEntries.article eq articleDAO.id) }.first(), articleDAO)

    private constructor(entry: ResultRow, articleDAO: ArticleDAO) : this(
        Article(articleDAO),
        entry[BillEntries.amount],
        entry[BillEntries.subsidy]
    )*/

    constructor(dao: BillEntryDAO) : this(
        Article(dao.article),
        dao.amount,
        dao.subsidy,
        dao.id.value
    )

    fun areDetailsEqual(other: BillEntry): Boolean {
        return article.areDetailsEqual(other.article) &&
                amount == other.amount &&
                subsidy == other.subsidy
    }
}

object BillEntries : LongIdTable() {
    val bill = reference("bill", Bills)
    val article = reference("article", Articles)
    val amount = integer("amount")
    val subsidy = float("subsidy")
}

class BillEntryDAO(id: EntityID<Long>) : LongEntity(id) {
    companion object : LongEntityClass<BillEntryDAO>(BillEntries)

    var bill by BillDAO referencedOn BillEntries.bill
    var article by ArticleDAO referencedOn BillEntries.article
    var amount by BillEntries.amount
    var subsidy by BillEntries.subsidy
}