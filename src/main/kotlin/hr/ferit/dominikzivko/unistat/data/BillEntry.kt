package hr.ferit.dominikzivko.unistat.data

import kotlinx.serialization.Serializable
import org.jetbrains.exposed.dao.LongEntity
import org.jetbrains.exposed.dao.LongEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.LongIdTable

val List<BillEntry>.totalAmount get() = sumOf { it.amount }
val List<BillEntry>.totalValue get() = sumOf { it.totalValue.toDouble() }
val List<BillEntry>.totalSubsidy get() = sumOf { it.subsidy.toDouble() }
val List<BillEntry>.totalCost get() = sumOf { it.totalCost.toDouble() }

@Serializable
data class BillEntry(
    val article: Article,
    val amount: Int,
    val subsidy: Float,
    val id: Long? = null
) {
    constructor(dao: BillEntryDAO) : this(
        Article(dao.article),
        dao.amount,
        dao.subsidy,
        dao.id.value
    )

    val totalValue get() = amount * article.price
    val totalCost get() = amount * article.price - subsidy

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