package hr.ferit.dominikzivko.unistat.data

import domyutil.jfx.*
import hr.ferit.dominikzivko.unistat.ui.UIManager
import hr.ferit.dominikzivko.unistat.ui.component.ProgressMonitor
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.apache.logging.log4j.LogManager
import org.koin.core.context.GlobalContext
import java.io.File
import java.util.*

class LocalDataSource : DataSource {
    private val log by lazy { LogManager.getLogger(javaClass) }
    private val uiManager: UIManager get() = GlobalContext.get().get()

    override val userID: UUID = LOCAL_USER.id

    override fun start() {}
    override fun stop() {}

    override fun fetchGeneralData(progressMonitor: ProgressMonitor): User {
        return User(LOCAL_USER.username, "(${strings["demo"]})", "-", "-", -1f, LOCAL_USER.id)
    }

    override fun fetchBills(existingBills: List<Bill>, progressMonitor: ProgressMonitor): List<Bill> {
        progressMonitor.hide()
        //val billsFile = uiManager.showOpenDialog(extensionFilters = App.billFileExtensionFilters)
           // ?: throw CancellationException()
        val billsFile = File("C:\\Users\\domyz\\desktop\\t.json")
        progressMonitor.applyFx {
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