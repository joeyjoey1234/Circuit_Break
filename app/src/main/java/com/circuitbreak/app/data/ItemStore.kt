package com.circuitbreak.app.data

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

object ItemStore {

    private const val PREFS = "circuit_break_items"
    private const val KEY_CUSTOM_PHYSICAL = "custom_physical"
    private const val KEY_CUSTOM_COGNITIVE = "custom_cognitive"
    private const val KEY_REMOVED = "removed_items"
    private const val KEY_SOUND_ENABLED = "sound_enabled"

    private val gson = Gson()

    // ── defaults cache ────────────────────────────────
    private var cachedDefaults: Pair<List<ActivityItem>, List<ActivityItem>>? = null

    fun loadDefaults(context: Context): Pair<List<ActivityItem>, List<ActivityItem>> {
        cachedDefaults?.let { return it }
        return try {
            val json = context.assets.open("defaults.json").bufferedReader().readText()
            val root = gson.fromJson(json, Map::class.java)
            val phys = gson.fromJson<List<ActivityItem>>(
                gson.toJson(root["physical"]),
                object : TypeToken<List<ActivityItem>>() {}.type
            ).map { it.copy(type = "physical") }
            val cog = gson.fromJson<List<ActivityItem>>(
                gson.toJson(root["cognitive"]),
                object : TypeToken<List<ActivityItem>>() {}.type
            ).map { it.copy(type = "cognitive") }
            val pair = Pair(phys, cog)
            cachedDefaults = pair
            pair
        } catch (e: Exception) {
            Pair(emptyList(), emptyList())
        }
    }

    // ── removed items ─────────────────────────────────
    fun getRemoved(context: Context): Set<String> {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return (prefs.getStringSet(KEY_REMOVED, emptySet()) ?: emptySet()).toSet()
    }

    fun isRemoved(context: Context, uid: String): Boolean =
        getRemoved(context).contains(uid)

    fun addRemoved(context: Context, uid: String) {
        val set = getRemoved(context).toMutableSet()
        set.add(uid)
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putStringSet(KEY_REMOVED, set).apply()
    }

    fun restoreAllDefaults(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putStringSet(KEY_REMOVED, emptySet()).apply()
    }

    // ── custom items ──────────────────────────────────
    fun getCustom(context: Context, type: String): List<ActivityItem> {
        return try {
            val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            val key = if (type == "physical") KEY_CUSTOM_PHYSICAL else KEY_CUSTOM_COGNITIVE
            val json = prefs.getString(key, "[]") ?: "[]"
            gson.fromJson(json, object : TypeToken<List<ActivityItem>>() {}.type)
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun addCustom(context: Context, type: String, item: ActivityItem) {
        val items = getCustom(context, type).toMutableList()
        items.add(sanitize(item))
        saveCustom(context, type, items)
    }

    fun updateCustom(context: Context, type: String, oldUid: String, newItem: ActivityItem) {
        val items = getCustom(context, type).toMutableList()
        val idx = items.indexOfFirst { it.uid() == oldUid }
        if (idx >= 0) {
            items[idx] = sanitize(newItem)
            saveCustom(context, type, items)
        }
    }

    fun removeCustomById(context: Context, type: String, uid: String) {
        val items = getCustom(context, type).toMutableList()
        items.removeAll { it.uid() == uid }
        saveCustom(context, type, items)
    }

    private fun saveCustom(context: Context, type: String, items: List<ActivityItem>) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val key = if (type == "physical") KEY_CUSTOM_PHYSICAL else KEY_CUSTOM_COGNITIVE
        prefs.edit().putString(key, gson.toJson(items)).apply()
    }

    // ── merged list ───────────────────────────────────
    fun getMergedItems(context: Context, defaults: List<ActivityItem>, type: String): List<ActivityItem> {
        val removed = getRemoved(context)
        val custom = getCustom(context, type)
        val sanitizedDefaults = defaults.map { sanitize(it) }
        val filtered = sanitizedDefaults.filter { !removed.contains(it.uid()) }
        return filtered + custom
    }

    fun allToJson(physical: List<ActivityItem>, cognitive: List<ActivityItem>): String {
        return gson.toJson(mapOf("physical" to physical, "cognitive" to cognitive))
    }

    // ── unicode sanitization ──────────────────────────
    private fun sanitize(item: ActivityItem): ActivityItem {
        return item.copy(
            a = clean(item.a),
            b = clean(item.b),
            d = clean(item.d),
            cat = clean(item.cat)
        )
    }

    private fun clean(s: String): String = (s ?: "")
        .replace("\u2011", "-")   // non-breaking hyphen
        .replace("\u2192", ">")   // right arrow
        .replace("\u2014", "-")   // em dash
        .replace("\u2013", "-")   // en dash
        .replace("\u00B7", ".")   // middle dot

    // ── sound ─────────────────────────────────────────
    fun isSoundEnabled(context: Context): Boolean {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getBoolean(KEY_SOUND_ENABLED, true)
    }

    fun setSoundEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_SOUND_ENABLED, enabled).apply()
    }
}
