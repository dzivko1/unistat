package hr.ferit.dominikzivko.unistat.data

import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable

data class Article(
    val name: String,
    val price: Float,
    val id: Int? = null
) {
    constructor(dao: ArticleDAO) : this(dao.name, dao.price, dao.id.value)

    fun areDetailsEqual(other: Article): Boolean {
        return name == other.name && price == other.price
    }
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