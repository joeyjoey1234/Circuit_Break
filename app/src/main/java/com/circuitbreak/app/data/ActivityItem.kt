package com.circuitbreak.app.data

data class ActivityItem(
    val a: String,
    val b: String,
    val d: String,
    val cat: String,
    val type: String
) {
    fun uid() = "$type::$a"
}
