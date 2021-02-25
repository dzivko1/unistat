package hr.ferit.dominikzivko.unistat

import javafx.application.Application
import javafx.scene.Scene
import javafx.scene.control.Label
import javafx.stage.Stage

class App : Application() {

    override fun start(primaryStage: Stage) {
        primaryStage.scene = Scene(Label("Hello JavaFX with Kotlin"))
        primaryStage.show()
    }
}