package hr.ferit.dominikzivko.unistat.data

import domyutil.*
import hr.ferit.dominikzivko.unistat.App
import java.util.prefs.Preferences

/**
 * An access point for all stored preferences of the application. This includes various user preferences as well as
 * non-user-managed configurations. The [Preferences] API is used for data management.
 */
object Pref {

    private val userPref by lazy { Preferences.userNodeForPackage(App::class.java) }

    private inline fun <reified T : Any> preference(key: String, defaultValue: T) =
        preference(userPref, key, defaultValue)


    const val LATEST_UPPER_BOUND = "latest"

    var url_base by preference("url_base", "https://issp.srce.hr")
    var url_loginBase by preference("url_login_base", "https://login.aaiedu.hr/sso/module.php/core/loginuserpass.php")
    var url_loginChoice by preference("url_login_choice", "https://issp.srce.hr/Account/OdaberiPrijavu")
    var url_loginRequest by preference("url_login_request", "https://issp.srce.hr/account/loginaai")
    var url_logout by preference("url_logout", "https://issp.srce.hr/Account/Odjava")
    var url_student by preference("url_student", "https://issp.srce.hr/Student")
    var url_billsBase by preference("url_bills_base", "https://issp.srce.hr/Student/StudentRacuni")

    var autoLogin by preference("auto_login", false)
    var savedUsername by preference("user_id", "")
    var savedPassword by preference("al_pwd", "")
    var userCredentials by preference("user_credentials", "")

    var lowerDateBound by preference("lower_date_bound", "")
    var upperDateBound by preference("upper_date_bound", "")
}