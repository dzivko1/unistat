package hr.ferit.dominikzivko.unistat.data

import kotlinx.serialization.Serializable
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable
import java.math.BigDecimal

@Serializable
data class Article(
    val name: String,
    @Serializable(with = BigDecimalSerializer::class)
    val price: BigDecimal
) {
    constructor(dao: ArticleDAO) : this(dao.name, dao.price)

    val fPrice get() = price.toFloat()
}

object Articles : IntIdTable() {
    val name = varchar("name", 100)
    val price = decimal("price", 5, 2)
}

class ArticleDAO(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<ArticleDAO>(Articles)

    var name by Articles.name
    var price by Articles.price
}