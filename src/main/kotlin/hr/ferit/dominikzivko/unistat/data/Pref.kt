package hr.ferit.dominikzivko.unistat.data

import domyutil.*
import hr.ferit.dominikzivko.unistat.App
import java.util.prefs.Preferences

object Pref {

    private val userPref by lazy { Preferences.userNodeForPackage(App::class.java) }

    private inline fun <reified T : Any> preference(key: String, defaultValue: T) =
        preference(userPref, key, defaultValue)


    var url_base by preference("url_base", "https://issp.srce.hr")
    var url_loginBase by preference("url_login_base", "https://login.aaiedu.hr/sso/module.php/core/loginuserpass.php")
    var url_logout by preference("url_logout", "https://issp.srce.hr/Account/Odjava")
    var url_student by preference("url_student", "https://issp.srce.hr/Student")
    var url_billsBase by preference("url_bills_base", "https://issp.srce.hr/Student/StudentRacuni")

    var userAgent by preference("conn_user_agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:86.0) Gecko/20100101 Firefox/86.0")

    var autoLogin by preference("auto_login", false)
    var savedUsername by preference("user_id", "")
    var savedPassword by preference("al_pwd", "")
}