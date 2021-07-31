package hr.ferit.dominikzivko.unistat.data

import kotlinx.serialization.Serializable
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable

@Serializable
data class Article(
    val name: String,
    val price: Float
) {
    constructor(dao: ArticleDAO) : this(dao.name, dao.price)
}

object Articles : IntIdTable() {
    val name = varchar("name", 100)
    val price = float("price")
}

class ArticleDAO(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<ArticleDAO>(Articles)

    var name by Articles.name
    var price by Articles.price
}