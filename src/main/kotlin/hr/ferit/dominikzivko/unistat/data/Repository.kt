package hr.ferit.dominikzivko.unistat.data

import domyutil.*
import domyutil.jfx.*
import hr.ferit.dominikzivko.unistat.AppBase
import hr.ferit.dominikzivko.unistat.AppComponent
import hr.ferit.dominikzivko.unistat.checkCancelled
import hr.ferit.dominikzivko.unistat.ui.component.ProgressMonitor
import javafx.beans.property.ReadOnlyObjectProperty
import javafx.beans.property.ReadOnlyObjectWrapper
import javafx.collections.FXCollections
import javafx.collections.ObservableList
import org.apache.logging.log4j.LogManager
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.transactions.transaction
import org.koin.core.context.GlobalContext

class Repository(var dataSource: DataSource) : AppComponent {

    private val log by lazy { LogManager.getLogger(javaClass) }
    private val app: AppBase by lazy { GlobalContext.get().get() }

    private val _userProperty = ReadOnlyObjectWrapper<User?>(this, "user")
    val userProperty: ReadOnlyObjectProperty<User?> get() = _userProperty.readOnlyProperty
    var user
        get() = _userProperty.value
        private set(value) {
            _userProperty.value = value
        }

    private val _bills: ObservableList<Bill> = FXCollections.observableArrayList()
    val bills: ObservableList<Bill> = FXCollections.unmodifiableObservableList(_bills)

    override fun start() {
        dataSource.start()
    }

    override fun stop() {
        dataSource.stop()
    }

    fun forget() {
        val future = runFx<Unit> {
            user = null
            _bills.clear()
        }
        dataSource.revokeAuthorization()
        future.get()
    }

    fun refresh(progressMonitor: ProgressMonitor) {
        log.info("Refreshing user data...")
        val newUser = dataSource.fetchGeneralData(progressMonitor)
        checkCancelled()

        val existingBills = newUser.loadBills()
        val newBills = dataSource.fetchBills(existingBills, progressMonitor)
        checkCancelled()

        if (app.offlineMode) {
            user = newUser
            _bills.setAll(newBills)
        } else {
            persist(newUser, newBills)
            reload()
        }

        log.info("Data refresh finished.")
    }

    private fun persist(newUser: User, newBills: List<Bill>) {
        log.debug("Saving data...")
        transaction {
            val userDAO = (newUser.dao ?: UserDAO.new(nameUUIDFromString(newUser.username)) {})
                .apply { update(newUser) }

            newBills.forEach { newBill ->
                val billDAO = BillDAO.new {
                    dateTime = newBill.dateTime
                    source = newBill.source
                    user = userDAO
                }

                val articles = newBill.articles
                val existingArticles = ArticleDAO.all().map { Article(it.name, it.price) }
                val newArticles = articles.minus(articles.intersect(existingArticles))

                newArticles.forEach { newArticle ->
                    ArticleDAO.new {
                        name = newArticle.name
                        price = newArticle.price
                    }
                }

                newBill.entries.forEach { newEntry ->
                    BillEntryDAO.new {
                        bill = billDAO
                        article = ArticleDAO.find {
                            (Articles.name eq newEntry.article.name) and (Articles.price eq newEntry.article.price)
                        }.first()
                        amount = newEntry.amount
                        subsidy = newEntry.subsidy
                    }
                }

            }
        }

        log.debug("Saving finished.")
    }

    private fun reload() = transaction {
        user = UserDAO.find { Users.id eq dataSource.userID }.firstOrNull()?.let { User(it) }
        if (user != null)
            _bills.setAll(BillDAO.find { Bills.user eq user!!.id }.map { Bill(it) })
        else _bills.clear()
    }
}