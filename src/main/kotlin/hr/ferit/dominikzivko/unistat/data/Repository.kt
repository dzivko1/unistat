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

/**
 * An [AppComponent] handling data acquisition and storage.
 *
 * The data fetching is done by a [DataSource] which changes according to the current offline/online mode of the app.
 *
 * Data provided by this repository include:
 * - Current user
 * - All user's bills
 * - User's bills filtered by a date range (derived)
 * - Dates of the earliest and latest bills, filtered and unfiltered (derived)
 * - Articles that the user has bought at least once (derived)
 */
class Repository(var dataSource: DataSource) : AppComponent {

    private val log by lazy { LogManager.getLogger(javaClass) }
    private val app: AppBase by lazy { GlobalContext.get().get() }

    private val _userProperty = ReadOnlyObjectWrapper<User?>(this, "user")
    val userProperty: ReadOnlyObjectProperty<User?> get() = _userProperty.readOnlyProperty
    var user: User?
        get() = _userProperty.value
        private set(value) {
            _userProperty.value = value
        }

    private val _bills: ObservableList<Bill> = FXCollections.observableArrayList()
    val bills: ObservableList<Bill> = FXCollections.unmodifiableObservableList(_bills)

    val billFilter = RangedFilter(_bills) { it.date }
    val filteredBills: ObservableList<Bill> get() = billFilter.filteredView

    val earliestBillDate get() = _bills.minOfOrNull { it.date }
    val earliestFilteredBillDate get() = filteredBills.minOfOrNull { it.date }
    val latestBillDate get() = _bills.maxOfOrNull { it.date }
    val latestFilteredBillDate get() = filteredBills.maxOfOrNull { it.date }

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

    override fun stop() {
        if (user != null && !Pref.autoLogin)
            forget()
        dataSource.stop()
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

    /**
     * Adds the not-already-existing bills from the specified collection to the pool of existing bills.
     */
    fun importBills(toAdd: List<Bill>) {
        val valid = toAdd.filterNot { new ->
            bills.any { existing -> new.contentEquals(existing) }
        }
        if (app.offlineMode) _bills += valid
        else {
            persist(newBills = valid)
            reload()
        }
    }

    /**
     * Forgets all user-bound data and revokes authorization from the [DataSource].
     */
    fun forget() {
        val future = runFx<Unit> {
            setData(null, null)
        }

        Pref.lowerDateBound = ""
        Pref.upperDateBound = ""

        dataSource.revokeAuthorization()

        future.get()
    }

    /**
     * Refreshes all user data while tracking progress with the specified [ProgressMonitor]. This operation supports
     * cancelling through [hr.ferit.dominikzivko.unistat.BackgroundThread].
     */
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

    /**
     * Adds the specified new data to the app database. If the [newUser] is null, the [newBills] are added for the
     * current [user] who may not be null at the time of execution of this function. This should not be called when the
     * app is in offline mode, as offline data is temporary and not persisted to the database.
     */
    private fun persist(newUser: User? = null, newBills: List<Bill>) {
        check(!app.offlineMode)

        log.debug("Saving data...")
        transaction {
            val theUser = newUser ?: user!!
            val userDAO = (theUser.dao ?: UserDAO.new(nameUUIDFromString(theUser.username)) {})
                .apply { update(theUser) }

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

    /**
     * Reads the user and bill data from the app database and stores it in properties of this repository.
     */
    private fun reload() = transaction {
        val newUser = UserDAO.find { Users.id eq dataSource.userID }.firstOrNull()?.let { User(it) }
        val newBills = newUser?.let { BillDAO.find { Bills.user eq newUser.id }.map { Bill(it) } }
        setData(newUser, newBills)
    }

    /**
     * Sets the new data to properties of this repository.
     * This runs on the FX thread and returns after completion
     */
    private fun setData(newUser: User?, newBills: List<Bill>?) = runFxAndWait {
        user = newUser
        if (newUser != null) {
            _bills.setAll(newBills?.sortedBy { it.dateTime })
        } else _bills.clear()
    }
}