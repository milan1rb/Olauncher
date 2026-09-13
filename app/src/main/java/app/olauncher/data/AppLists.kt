package app.olauncher.data

import android.content.Context
import androidx.core.content.edit
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

/**
 * Un dossier. L'identite repose sur [id], donc le nom peut etre vide
 * ou identique a celui d'un autre dossier.
 */
data class Folder(
    val id: String,
    var name: String = "",
    var icon: String = "",
    val apps: MutableSet<String> = mutableSetOf()
)

/** Olauncher V2 - Stockage des dossiers (JSON dans les SharedPreferences). */
class AppLists(context: Context) {

    private val prefs = context.getSharedPreferences(PREFS_FILENAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREFS_FILENAME = "app.olauncher"
        private const val APP_LISTS = "APP_LISTS"
        private const val VISIBLE_COUNT = "APP_LISTS_VISIBLE"

        fun idOf(appModel: AppModel): String = when (appModel) {
            is AppModel.PinnedShortcut -> appModel.identity
            else -> appModel.appPackage + "|" + appModel.user.toString()
        }
    }

    fun getFolders(): MutableList<Folder> {
        val result = mutableListOf<Folder>()
        val raw = prefs.getString(APP_LISTS, "") ?: ""
        if (raw.isBlank()) return result
        var needsMigration = false
        try {
            val array = JSONArray(raw)
            for (i in 0 until array.length()) {
                val obj = array.optJSONObject(i) ?: continue
                val apps = mutableSetOf<String>()
                val appsArray = obj.optJSONArray("apps") ?: JSONArray()
                for (j in 0 until appsArray.length()) apps.add(appsArray.optString(j))
                var id = obj.optString("id")
                if (id.isBlank()) {
                    id = UUID.randomUUID().toString()
                    needsMigration = true
                }
                result.add(Folder(id, obj.optString("name"), obj.optString("icon"), apps))
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        if (needsMigration) save(result)
        return result
    }

    private fun save(folders: List<Folder>) {
        val array = JSONArray()
        folders.forEach { folder ->
            val obj = JSONObject()
            obj.put("id", folder.id)
            obj.put("name", folder.name)
            obj.put("icon", folder.icon)
            obj.put("apps", JSONArray(folder.apps.toList()))
            array.put(obj)
        }
        prefs.edit { putString(APP_LISTS, array.toString()) }
    }

    fun folderById(id: String?): Folder? =
        if (id == null) null else getFolders().firstOrNull { it.id == id }

    fun ids(): List<String> = getFolders().map { it.id }

    fun appsIn(id: String): Set<String> = folderById(id)?.apps ?: emptySet()

    fun allFolderedApps(): Set<String> {
        val all = mutableSetOf<String>()
        getFolders().forEach { all.addAll(it.apps) }
        return all
    }

    /** id d'appli -> noms des dossiers, pour l'affichage dans la recherche. */
    fun labelsByApp(): Map<String, String> {
        val map = mutableMapOf<String, MutableList<String>>()
        getFolders().forEach { folder ->
            if (folder.name.isBlank()) return@forEach
            folder.apps.forEach { appId ->
                map.getOrPut(appId) { mutableListOf() }.add(folder.name)
            }
        }
        return map.mapValues { it.value.joinToString(", ") }
    }

    /** Cree un dossier. Le nom peut etre vide ou deja utilise. */
    fun createFolder(name: String): String {
        val folders = getFolders()
        val folder = Folder(UUID.randomUUID().toString(), name.trim())
        folders.add(folder)
        save(folders)
        return folder.id
    }

    fun deleteFolder(id: String) {
        save(getFolders().filterNot { it.id == id })
    }

    fun renameFolder(id: String, newName: String) {
        val folders = getFolders()
        folders.firstOrNull { it.id == id }?.name = newName.trim()
        save(folders)
    }

    fun setIcon(id: String, icon: String) {
        val folders = getFolders()
        folders.firstOrNull { it.id == id }?.icon = icon
        save(folders)
    }

    fun setOrder(order: List<String>) {
        val folders = getFolders()
        val reordered = mutableListOf<Folder>()
        order.forEach { id -> folders.firstOrNull { it.id == id }?.let { reordered.add(it) } }
        folders.forEach { if (reordered.none { r -> r.id == it.id }) reordered.add(it) }
        save(reordered)
    }

    fun isInFolder(id: String, appId: String): Boolean = appsIn(id).contains(appId)

    fun setAppInFolder(id: String, appId: String, add: Boolean) {
        val folders = getFolders()
        val folder = folders.firstOrNull { it.id == id } ?: return
        if (add) folder.apps.add(appId) else folder.apps.remove(appId)
        save(folders)
    }
}
