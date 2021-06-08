package hr.ferit.dominikzivko.unistat.data

import domyutil.*
import domyutil.jfx.*
import hr.ferit.dominikzivko.unistat.ui.component.ProgressMonitor
import javafx.beans.property.ReadOnlyObjectWrapper
import javafx.collections.FXCollections
import javafx.collections.ObservableList
import kotlinx.coroutines.*
import org.apache.logging.log4j.LogManager
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.batchInsert
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction

class Repository(val dataSource: DataSource) {

    private val log by lazy { LogManager.getLogger(javaClass) }

    private val _userProperty = ReadOnlyObjectWrapper<User>(this, "user")
    val userProperty get() = _userProperty.readOnlyProperty
    var user
        get() = _userProperty.value
        private set(value) {
            _userProperty.value = value
        }

    private val _bills: ObservableList<Bill> = FXCollections.observableArrayList()
    val bills: ObservableList<Bill> = FXCollections.unmodifiableObservableList(_bills)

    fun reload() = transaction {
        user = UserDAO.find { Users.id eq dataSource.userID }.firstOrNull()?.let { User(it) }
        _bills.setAll(BillDAO.find { Bills.user eq dataSource.userID }.map { Bill(it) })
    }

    fun refresh(progressMonitor: ProgressMonitor) {
        log.info("Refreshing user data...")
        runFx {
            progressMonitor.message = strings["fetching_general_data"] + "..."
            progressMonitor.progress = -1
        }

        val newUser = dataSource.fetchGeneralData()
        val newBills = dataSource.fetchBills(bills, progressMonitor)
        persist(newUser, newBills)
        log.info("Data refresh finished.")
    }

    private fun persist(newUser: User, newBills: List<Bill>) = transaction {
        log.debug("Saving data...")
        val userDAO = (newUser.dao ?: UserDAO.new(nameUUIDFromString(newUser.username)) {}).apply { update(newUser) }

        val newBillDAOs = mutableListOf<BillDAO>()

        newBills.forEach { newBill ->
            val billDAO = BillDAO.new {
                dateTime = newBill.dateTime
                source = newBill.source
                user = userDAO
            }.also { newBillDAOs += it }

            val articles = newBill.entries.map { it.article }
            val newArticles = articles.minus(articles.intersect(ArticleDAO.all().map { Article(it.name, it.price) }))
            //val newArticles = newBill.entries.map { it.article }
            //    .filter { ArticleDAO.find { (Articles.name eq it.name) and (Articles.price eq it.price) }.empty() }

            newArticles.forEach { newArticle ->
                ArticleDAO.new {
                    name = newArticle.name
                    price = newArticle.price
                }
            }

            /*BillEntries.batchInsert(newBill.entries) { newEntry ->
                this[BillEntries.bill] = billDAO.id
                this[BillEntries.article] = Articles.select { (Articles.name eq newEntry.article.name) and (Articles.price eq newEntry.article.price) }.first()[Articles.id]
                this[BillEntries.amount] = newEntry.amount
                this[BillEntries.subsidy] = newEntry.subsidy
            }*/

            newBill.entries.forEach { newEntry ->
                BillEntryDAO.new {
                    bill = billDAO
                    article =
                        ArticleDAO.find { (Articles.name eq newEntry.article.name) and (Articles.price eq newEntry.article.price) }
                            .first()
                    amount = newEntry.amount
                    subsidy = newEntry.subsidy
                }
            }
        }

        user = newUser
        _bills.addAll(newBillDAOs.map { Bill(it) })
        log.debug("Saving finished.")


        /*val newBills = bills.filter { it.id == null }
        val insertedBillRows = Bills.batchInsert(newBills) { bill ->
            this[Bills.dateTime] = bill.dateTime
            this[Bills.billSource] = bill.source
            this[Bills.user] = bill.user
        }
        val inserted = insertedBillRows.associate { row -> Pair(row[Bills.id], BillDAO.forEntityIds(insertedBillRows.map { it[Bills.id] }).first()) }
        newBills.forEach { newBill ->
            val newArticles = newBill.entries.map { it.article }.filter { Articles.select { (Articles.name eq it.name) and (Articles.price eq it.price) }.empty() }
            Articles.batchInsert(newArticles) { newArticle ->
                this[Articles.name] = newArticle.name
                this[Articles.price] = newArticle.price
            }
            BillEntries.batchInsert(newBill.entries) { entry ->
                this[BillEntries.bill] =
                this[BillEntries.article] = entry.article.id
            }
        }*/
    }

}