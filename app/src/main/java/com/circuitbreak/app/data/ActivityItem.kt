package com.circuitbreak.app.data

data class ActivityItem(
    val a: String,   // main activity line
    val b: String,   // resource / instruction
    val d: String,   // detail / time
    val cat: String, // category
    val type: String // "physical" or "cognitive"
)
