package com.circuitbreak.app.data

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

object ItemStore {

    private const val PREFS = "circuit_break_items"
    private const val KEY_CUSTOM_PHYSICAL = "custom_physical"
    private const val KEY_CUSTOM_COGNITIVE = "custom_cognitive"
    private const val KEY_REMOVED = "removed_items"

    private val gson = Gson()

    fun loadDefaults(context: Context): Pair<List<ActivityItem>, List<ActivityItem>> {
        val json = context.assets.open("defaults.json").bufferedReader().readText()
        val root = gson.fromJson(json, Map::class.java)
        val phys = gson.fromJson<List<ActivityItem>>(
            gson.toJson(root["physical"]),
            object : TypeToken<List<ActivityItem>>() {}.type
        )
        val cog = gson.fromJson<List<ActivityItem>>(
            gson.toJson(root["cognitive"]),
            object : TypeToken<List<ActivityItem>>() {}.type
        )
        return Pair(phys, cog)
    }

    fun getRemoved(context: Context): Set<String> {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return prefs.getStringSet(KEY_REMOVED, emptySet()) ?: emptySet()
    }

    fun setRemoved(context: Context, items: Set<String>) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putStringSet(KEY_REMOVED, items).apply()
    }

    fun getCustom(context: Context, type: String): List<ActivityItem> {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val key = if (type == "physical") KEY_CUSTOM_PHYSICAL else KEY_CUSTOM_COGNITIVE
        val json = prefs.getString(key, "[]") ?: "[]"
        return gson.fromJson(json, object : TypeToken<List<ActivityItem>>() {}.type)
    }

    fun addCustom(context: Context, type: String, item: ActivityItem) {
        val items = getCustom(context, type).toMutableList()
        items.add(item)
        saveCustom(context, type, items)
    }

    fun removeCustom(context: Context, type: String, item: ActivityItem) {
        val items = getCustom(context, type).toMutableList()
        items.removeAll { it.a == item.a }
        saveCustom(context, type, items)
    }

    private fun saveCustom(context: Context, type: String, items: List<ActivityItem>) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val key = if (type == "physical") KEY_CUSTOM_PHYSICAL else KEY_CUSTOM_COGNITIVE
        prefs.edit().putString(key, gson.toJson(items)).apply()
    }

    fun getMergedItems(context: Context, defaults: List<ActivityItem>, type: String): List<ActivityItem> {
        val removed = getRemoved(context)
        val custom = getCustom(context, type)
        val filtered = defaults.filter { !removed.contains(it.a) }
        return filtered + custom
    }

    fun allToJson(physical: List<ActivityItem>, cognitive: List<ActivityItem>): String {
        return gson.toJson(mapOf("physical" to physical, "cognitive" to cognitive))
    }
}
