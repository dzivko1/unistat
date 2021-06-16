package hr.ferit.dominikzivko.unistat.data

import domyutil.*
import org.jetbrains.exposed.dao.UUIDEntity
import org.jetbrains.exposed.dao.UUIDEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.*

val SAMPLE_USER = UserLogon("<SAMPLE_USER>")

data class UserLogon(
    val username: String
) {
    val id by lazy { nameUUIDFromString(username) }
}

data class LoginDetails(
    val username: String,
    val password: String
)

data class User(
    val username: String,
    val fullName: String,
    val institution: String,
    val level: String,
    val balance: Float,
    val id: UUID
) {
    constructor(dao: UserDAO) : this(
        dao.username,
        dao.fullName,
        dao.institution,
        dao.level,
        dao.balance,
        dao.id.value
    )

    val dao: UserDAO? by lazy { transaction { UserDAO.findById(this@User.id) } }

    fun loadBills(): List<Bill> = transaction {
        if (dao != null) BillDAO.find { Bills.user eq dao!!.id }.map { Bill(it) }
        else emptyList()
    }
}

object Users : UUIDTable() {
    val username = varchar("username", 50)
    val fullName = varchar("fullName", 50)
    val institution = varchar("institution", 200)
    val level = varchar("level", 5)
    val balance = float("balance")
}

class UserDAO(id: EntityID<UUID>) : UUIDEntity(id) {
    companion object : UUIDEntityClass<UserDAO>(Users)

    var username by Users.username
    var fullName by Users.fullName
    var institution by Users.institution
    var level by Users.level
    var balance by Users.balance
    val bills by BillDAO referrersOn Bills.user

    fun update(user: User) {
        require(user.id == this.id.value) {
            "User IDs must match. This user's ID: ${this.id.value}. Passed user's ID: ${user.id}"
        }
        username = user.username
        fullName = user.fullName
        institution = user.institution
        level = user.level
        balance = user.balance
    }
}