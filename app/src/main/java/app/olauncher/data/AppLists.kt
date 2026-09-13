package app.olauncher.data

import android.content.Context
import androidx.core.content.edit
import org.json.JSONArray
import org.json.JSONObject

/** Un dossier : un nom, une icone (emoji, vide = icone par defaut) et des applis. */
data class Folder(
    val name: String,
    var icon: String = "",
    val apps: MutableSet<String> = mutableSetOf()
)

/**
 * Olauncher V2 - Stockage des dossiers d'applications.
 * JSON dans les SharedPreferences existantes, ordre conserve.
 */
class AppLists(context: Context) {

    private val prefs = context.getSharedPreferences(PREFS_FILENAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREFS_FILENAME = "app.olauncher"
        private const val APP_LISTS = "APP_LISTS"
        private const val VISIBLE_COUNT = "APP_LISTS_VISIBLE"
        const val MAX_LISTS = Int.MAX_VALUE

        fun idOf(appModel: AppModel): String = when (appModel) {
            is AppModel.PinnedShortcut -> appModel.identity
            else -> appModel.appPackage + "|" + appModel.user.toString()
        }
    }

    fun getFolders(): MutableList<Folder> {
        val result = mutableListOf<Folder>()
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
                result.add(Folder(name, obj.optString("icon", ""), apps))
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return result
    }

    private fun save(folders: List<Folder>) {
        val array = JSONArray()
        folders.forEach { folder ->
            val obj = JSONObject()
            obj.put("name", folder.name)
            obj.put("icon", folder.icon)
            obj.put("apps", JSONArray(folder.apps.toList()))
            array.put(obj)
        }
        prefs.edit { putString(APP_LISTS, array.toString()) }
    }

    fun names(): List<String> = getFolders().map { it.name }

    /** Nombre de dossiers affiches dans la barre. Les suivants ne vivent que dans le menu. */
    fun visibleCount(): Int {
        val size = getFolders().size
        val stored = prefs.getInt(VISIBLE_COUNT, -1)
        return if (stored < 0) size else stored.coerceIn(0, size)
    }

    fun setVisibleCount(count: Int) {
        prefs.edit { putInt(VISIBLE_COUNT, count.coerceAtLeast(0)) }
    }

    fun appsIn(name: String): Set<String> =
        getFolders().firstOrNull { it.name == name }?.apps ?: emptySet()

    /** Toutes les applis rangees dans au moins un dossier. */
    fun allFolderedApps(): Set<String> {
        val all = mutableSetOf<String>()
        getFolders().forEach { all.addAll(it.apps) }
        return all
    }

    /** id d'appli -> "Travail, Jeux" (pour l'affichage dans la recherche). */
    fun labelsByApp(): Map<String, String> {
        val map = mutableMapOf<String, MutableList<String>>()
        getFolders().forEach { folder ->
            folder.apps.forEach { appId ->
                map.getOrPut(appId) { mutableListOf() }.add(folder.name)
            }
        }
        return map.mapValues { it.value.joinToString(", ") }
    }

    fun createList(name: String): Boolean {
        val clean = name.trim()
        if (clean.isEmpty()) return false
        val folders = getFolders()
        if (folders.any { it.name == clean } || folders.size >= MAX_LISTS) return false
        folders.add(Folder(clean))
        save(folders)
        return true
    }

    fun deleteList(name: String) {
        save(getFolders().filterNot { it.name == name })
    }

    fun renameList(oldName: String, newName: String): Boolean {
        val clean = newName.trim()
        if (clean.isEmpty()) return false
        val folders = getFolders()
        if (folders.none { it.name == oldName } || folders.any { it.name == clean }) return false
        save(folders.map { if (it.name == oldName) Folder(clean, it.icon, it.apps) else it })
        return true
    }

    fun setIcon(name: String, icon: String) {
        val folders = getFolders()
        folders.firstOrNull { it.name == name }?.icon = icon
        save(folders)
    }

    /** Enregistre un nouvel ordre de dossiers (glisser-deposer). */
    fun setOrder(order: List<String>) {
        val folders = getFolders()
        val reordered = mutableListOf<Folder>()
        order.forEach { name -> folders.firstOrNull { it.name == name }?.let { reordered.add(it) } }
        folders.forEach { if (reordered.none { r -> r.name == it.name }) reordered.add(it) }
        save(reordered)
    }

    fun isInList(name: String, appId: String): Boolean = appsIn(name).contains(appId)

    fun setAppInList(name: String, appId: String, add: Boolean) {
        val folders = getFolders()
        val folder = folders.firstOrNull { it.name == name } ?: return
        if (add) folder.apps.add(appId) else folder.apps.remove(appId)
        save(folders)
    }
}
