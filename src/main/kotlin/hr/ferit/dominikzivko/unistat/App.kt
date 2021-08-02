/*
Ovaj projekt je dio studentskog završnog rada.

Naziv rada: Aplikacija za analizu potrošnje na X-ici
Studij: Preddiplomski sveučilišni studij Računarstvo
Student: Dominik Živko
Mentor: Alfonzo Baumgartner

Aplikacija je pisana u programskom jeziku Kotlin uz korištenje JavaFX programskog okvira za izradu desktop aplikacija.
Izvorni projekt je pohranjen na GitLab repozitoriju:
    https://gitlab.com/domy.zivko/unistat2

Sav sadržaj programskog koda, kao i njegova dokumentacija pisani su na engleskom jeziku radi poštivanja industrijskog
standarda.


Key terms:
    Bill - the main data unit of the application; represents a single receipt; the name 'bill' was chosen over 'receipt'
        or 'invoice' because of its similar meaning and shorter spelling
    Webserver - a term representing the website from which the students' data is gathered
*/

package hr.ferit.dominikzivko.unistat

import domyutil.jfx.*
import hr.ferit.dominikzivko.unistat.data.*
import hr.ferit.dominikzivko.unistat.gui.UIManager
import hr.ferit.dominikzivko.unistat.web.AuthWebGateway
import hr.ferit.dominikzivko.unistat.web.WebGateway
import javafx.application.Application
import javafx.application.Platform
import javafx.stage.FileChooser
import javafx.stage.Stage
import org.apache.logging.log4j.LogManager
import org.koin.core.context.GlobalContext
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import java.awt.SplashScreen
import java.io.IOException
import java.io.RandomAccessFile
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import javax.swing.JOptionPane
import kotlin.io.path.createDirectories
import kotlin.system.exitProcess

/** A path representing the directory where application data is stored between runs. */
val workDir: Path get() = Path.of(System.getProperty("app.workdir"))

class App : Application() {
    private val log by lazy { LogManager.getLogger(javaClass) }

    private val appBase: AppBase by lazy { GlobalContext.get().get() }

    override fun start(primaryStage: Stage) {
        log.info("Application started.")
        try {
            Platform.setImplicitExit(false)
            startKoin { modules(baseModule, remoteDatasourceModule) }
            AppDatabase.initialize()
            SplashScreen.getSplashScreen()?.close()
            appBase.start(primaryStage, false)
        } catch (t: Throwable) {
            log.fatal("Fatal error", t)
            Alerts.catching(strings["msg_errorOccurred"], t)
            exit()
        }
    }

    override fun stop() {
        log.info("Application stopping.")
        appBase.stop()
        stopKoin()
        super.stop()
    }


    companion object {
        const val AUTHOR = "Altline"
        const val APPNAME = "UniStat"

        private val log by lazy { LogManager.getLogger(App::class.java) }

        /** String locator of the application UI stylesheet. */
        val mainStylesheet: String by lazy {
            App::class.java.getResource("gui/application.css").toExternalForm()
        }

        /** A collection of valid file extension filters for files containing bill data. */
        val billFileExtensionFilters = listOf(
            FileChooser.ExtensionFilter("JSON Files", "*.json")
        )

        /** Initiates a full application shutdown. */
        fun exit() {
            Platform.exit()
        }

        @JvmStatic
        fun main(args: Array<String>) {
            try {
                initWorkDirectory()
                applock()
                launch(App::class.java, *args)
            } catch (t: Throwable) {
                log.fatal("Top level error", t)
                Alerts.catching(strings["msg_errorOccurred"], t)
                exitProcess(ExitCodes.FAIL)
            }
        }

        /**
         * Sets up an application-wide directory path to serve as a data store. The work directory path should be
         * accessed through the [workDir] property.
         */
        private fun initWorkDirectory() {
            val appdata = System.getenv("APPDATA")
            val workDirPath: Path = if (!appdata.isNullOrBlank()) Paths.get(appdata, AUTHOR, APPNAME)
            else Paths.get(System.getProperty("user.home"), AUTHOR, APPNAME)

            workDirPath.createDirectories()
            System.setProperty("app.workdir", workDirPath.toString())
        }

        /**
         * Sets up a lock file that prevents two instances of this application from running at the same time. The lock
         * must be open for a new instance to start.
         */
        private fun applock() {
            try {
                val applockFile = Paths.get(System.getProperty("app.workdir"), "app.lock")
                val raf = RandomAccessFile(applockFile.toFile(), "rw")
                val channel = raf.channel
                val lock = channel?.tryLock()
                if (lock == null) {
                    log.warn("Unable to obtain app file lock - assuming another instance is already running. This instance will not launch.")
                    channel?.close()
                    raf.close()
                    JOptionPane.showMessageDialog(
                        null,
                        strings["error"],
                        strings["msg_alreadyRunning"],
                        JOptionPane.ERROR_MESSAGE
                    )
                    exitProcess(ExitCodes.ALREADY_RUNNING)
                }

                Runtime.getRuntime().addShutdownHook(object : Thread() {
                    override fun run() {
                        try {
                            lock.release()
                            channel.close()
                            raf.close()
                            Files.delete(applockFile)
                        } catch (e: IOException) {
                            log.error("", e)
                        }
                    }

                })
            } catch (t: Throwable) {
                log.fatal("Launch error", t)
                exitProcess(ExitCodes.FAIL)
            }
        }
    }
}

/** The main Koin module that should always be loaded for the application to function. */
val baseModule = module {
    single { AppBase(get(), get()) }
    single { UIManager() }
    single { Repository(get()) }
}

/** A Koin module which supports sourcing data from the webserver. */
val remoteDatasourceModule = module {
    single { WebGateway() }
    single { AuthWebGateway(get()) }
    single<DataSource> { WebDataSource(get()) }
}

/** A Koin module which supports sourcing data from a local file. */
val localDatasourceModule = module {
    single<DataSource> { LocalDataSource() }
}

/** Basic exit codes. */
object ExitCodes {
    /** A general failure exit code. */
    const val FAIL = 1

    /** An exit code denoting that the application is already running. */
    const val ALREADY_RUNNING = 10
}