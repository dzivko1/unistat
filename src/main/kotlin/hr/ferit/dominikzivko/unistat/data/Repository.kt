package hr.ferit.dominikzivko.unistat.data

import domyutil.*
import domyutil.jfx.*
import hr.ferit.dominikzivko.unistat.AppBase
import hr.ferit.dominikzivko.unistat.AppComponent
import hr.ferit.dominikzivko.unistat.checkCancelled
import hr.ferit.dominikzivko.unistat.gui.component.ProgressMonitor
import javafx.beans.binding.Bindings
import javafx.beans.property.ReadOnlyObjectProperty
import javafx.beans.property.ReadOnlyObjectWrapper
import javafx.beans.property.SimpleListProperty
import javafx.collections.FXCollections
import javafx.collections.ListChangeListener
import javafx.collections.ObservableList
import org.apache.logging.log4j.LogManager
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.transactions.transaction
import org.koin.core.context.GlobalContext
import java.time.LocalDate

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

    val billFilter = RangedFilter(_bills) { it.date }
    val filteredBills: ObservableList<Bill> get() = billFilter.filteredView

    val earliestBillDate get() = _bills.minOfOrNull { it.date }
    val latestBillDate get() = _bills.maxOfOrNull { it.date }

    private val _articles = SimpleListProperty<Article>().apply {
        bind(Bindings.createObjectBinding({
            FXCollections.observableList(filteredBills.flatMap { it.articles }.distinct())
        }, filteredBills))
    }
    val articles: ObservableList<Article> = FXCollections.unmodifiableObservableList(_articles)

    override fun start() {
        dataSource.start()
        setupBillFilter()
    }

    private fun setupBillFilter() {
        billFilter.apply {
            lowerBound = runCatching { LocalDate.parse(Pref.lowerDateBound) }.getOrNull()
            upperBound = runCatching { LocalDate.parse(Pref.upperDateBound) }.getOrDefault(LocalDate.now())

            lowerBoundProperty.addListener { _, _, newValue -> Pref.lowerDateBound = newValue?.toString().orEmpty() }
            upperBoundProperty.addListener { _, _, newValue ->
                Pref.upperDateBound =
                    if (newValue != latestBillDate) newValue?.toString().orEmpty()
                    else Pref.LATEST_UPPER_BOUND
            }
        }

        // Doing this through bindings breaks the filtered list
        // I described the problem here:
        // https://stackoverflow.com/q/68458910/6640693
        _bills.addListener(object : ListChangeListener<Bill> {
            var latestBillDate: LocalDate? = null
            override fun onChanged(c: ListChangeListener.Change<out Bill>?) {
                val newMax = _bills.maxOfOrNull { it.date }
                if (billFilter.upperBound == latestBillDate) {
                    billFilter.upperBound = newMax
                }
                latestBillDate = newMax
            }
        })
    }

    override fun stop() {
        if (user != null && !Pref.autoLogin)
            forget()
        dataSource.stop()
    }

    fun forget() {
        val future = runFx<Unit> {
            user = null
            _bills.clear()
        }

        Pref.lowerDateBound = ""
        Pref.upperDateBound = ""

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
            setData(newUser, newBills)
        } else {
            persist(newUser, newBills)
            reload()
        }

        if (Pref.lowerDateBound.isEmpty())
            billFilter.lowerBound = earliestBillDate

        if (Pref.upperDateBound == Pref.LATEST_UPPER_BOUND)
            billFilter.upperBound = latestBillDate

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
        val newUser = UserDAO.find { Users.id eq dataSource.userID }.firstOrNull()?.let { User(it) }
        val newBills = newUser?.let { BillDAO.find { Bills.user eq newUser.id }.map { Bill(it) } }
        setData(newUser, newBills)
    }

    private fun setData(newUser: User?, newBills: List<Bill>?) = runFxAndWait {
        user = newUser
        if (newUser != null) {
            _bills.setAll(newBills)
        } else _bills.clear()
    }
}