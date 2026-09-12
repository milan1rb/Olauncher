package app.olauncher.data

import android.content.Context
import androidx.core.content.edit
import org.json.JSONArray
import org.json.JSONObject

/**
 * Olauncher V2 - Stockage des listes d'applications.
 *
 * Les listes sont enregistrees dans les SharedPreferences existantes sous
 * forme de JSON : [{"name":"Travail","apps":["com.slack|UserHandle{0}"]}]
 * L'ordre de creation est conserve.
 */
class AppLists(context: Context) {

    private val prefs = context.getSharedPreferences(PREFS_FILENAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREFS_FILENAME = "app.olauncher"
        private const val APP_LISTS = "APP_LISTS"
        const val MAX_LISTS = 12

        /** Identifiant stable d'une application ou d'un raccourci epingle. */
        fun idOf(appModel: AppModel): String = when (appModel) {
            is AppModel.PinnedShortcut -> appModel.identity
            else -> appModel.appPackage + "|" + appModel.user.toString()
        }
    }

    fun getLists(): LinkedHashMap<String, MutableSet<String>> {
        val result = LinkedHashMap<String, MutableSet<String>>()
        val raw = prefs.getString(APP_LISTS, "") ?: ""
        if (raw.isBlank()) return result
        try {
            val array = JSONArray(raw)
            for (i in 0 until array.length()) {
                val obj = array.optJSONObject(i) ?: continue
                val name = obj.optString("name")
                if (name.isBlank()) continue
                val apps = mutableSetOf<String>()
                val appsArray = obj.optJSONArray("apps") ?: JSONArray()
                for (j in 0 until appsArray.length()) apps.add(appsArray.optString(j))
                result[name] = apps
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return result
    }

    private fun save(lists: LinkedHashMap<String, MutableSet<String>>) {
        val array = JSONArray()
        lists.forEach { (name, apps) ->
            val obj = JSONObject()
            obj.put("name", name)
            obj.put("apps", JSONArray(apps.toList()))
            array.put(obj)
        }
        prefs.edit { putString(APP_LISTS, array.toString()) }
    }

    fun names(): List<String> = getLists().keys.toList()

    fun appsIn(name: String): Set<String> = getLists()[name] ?: emptySet()

    fun createList(name: String): Boolean {
        val clean = name.trim()
        if (clean.isEmpty()) return false
        val lists = getLists()
        if (lists.containsKey(clean) || lists.size >= MAX_LISTS) return false
        lists[clean] = mutableSetOf()
        save(lists)
        return true
    }

    fun deleteList(name: String) {
        val lists = getLists()
        lists.remove(name)
        save(lists)
    }

    fun renameList(oldName: String, newName: String): Boolean {
        val clean = newName.trim()
        if (clean.isEmpty()) return false
        val lists = getLists()
        if (!lists.containsKey(oldName) || lists.containsKey(clean)) return false
        val renamed = LinkedHashMap<String, MutableSet<String>>()
        lists.forEach { (key, value) -> renamed[if (key == oldName) clean else key] = value }
        save(renamed)
        return true
    }

    /** Enregistre un nouvel ordre de dossiers (glisser-deposer). */
    fun setOrder(order: List<String>) {
        val lists = getLists()
        val reordered = LinkedHashMap<String, MutableSet<String>>()
        order.forEach { name -> lists[name]?.let { reordered[name] = it } }
        lists.forEach { (name, apps) -> if (!reordered.containsKey(name)) reordered[name] = apps }
        save(reordered)
    }

    fun isInList(name: String, appId: String): Boolean = appsIn(name).contains(appId)

    fun setAppInList(name: String, appId: String, add: Boolean) {
        val lists = getLists()
        val apps = lists[name] ?: return
        if (add) apps.add(appId) else apps.remove(appId)
        lists[name] = apps
        save(lists)
    }
}
