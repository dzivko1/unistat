package hr.ferit.dominikzivko.unistat.data

import org.jetbrains.exposed.dao.id.IntIdTable

object Cookies : IntIdTable() {
    val content = blob("content")
}