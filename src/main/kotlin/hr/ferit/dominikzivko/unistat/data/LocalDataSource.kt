package hr.ferit.dominikzivko.unistat.data

import domyutil.jfx.*
import hr.ferit.dominikzivko.unistat.App
import hr.ferit.dominikzivko.unistat.CancellationException
import hr.ferit.dominikzivko.unistat.gui.UIManager
import hr.ferit.dominikzivko.unistat.gui.component.ProgressMonitor
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.apache.logging.log4j.LogManager
import org.koin.core.context.GlobalContext
import java.util.*

/**
 * An implementation of [DataSource] which gathers data from a user-specified file. The user is asked to specify the
 * file at the time of fetching.
 *
 * This implementation does not mandate any authentication described in the [DataSource] interface.
 */
class LocalDataSource : DataSource {
    private val log by lazy { LogManager.getLogger(javaClass) }
    private val uiManager: UIManager get() = GlobalContext.get().get()

    override val userID: UUID = LOCAL_USER.id

    override fun start() {}
    override fun stop() {}

    override fun fetchGeneralData(progressMonitor: ProgressMonitor?): User {
        return User(LOCAL_USER.username, "(${strings["demo"]})", "---", "---", 0f, LOCAL_USER.id)
    }

    override fun fetchBills(existingBills: List<Bill>, progressMonitor: ProgressMonitor?): List<Bill> {
        progressMonitor?.hide()

        val billsFile = uiManager.showOpenDialog(
            title = strings["title_open"],
            extensionFilters = App.billFileExtensionFilters
        ) ?: throw CancellationException()

        progressMonitor?.applyFx {
            message = strings["loadingBills"]
            progress = -1
            show()
        }

        return runCatching {
            val json = billsFile.readText()
            Json.decodeFromString<List<Bill>>(json)
        }.recover { e ->
            Alerts.catching(strings["msg_importFailed"], e, log)
            fetchBills(existingBills, progressMonitor)
        }.getOrThrow()
    }

    override fun revokeAuthorization() {}
}