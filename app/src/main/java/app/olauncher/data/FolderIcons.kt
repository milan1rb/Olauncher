package app.olauncher.data

import android.content.Context
import android.graphics.Typeface
import org.json.JSONObject

/**
 * Olauncher V2 - Banque d'icones Phosphor (licence MIT), 1512 icones x 5 graisses.
 * Les icones sont rendues par une police, pas par des drawables : quelques
 * fichiers .ttf suffisent au lieu de milliers de vecteurs.
 *
 * Format stocke : "ph:<graisse>:<nom>", par exemple "ph:fill:star".
 */
object FolderIcons {

    const val PREFIX = "ph:"

    val weights = listOf("thin", "light", "regular", "bold", "fill")

    private val fontFiles = mapOf(
        "thin" to "phosphor/Phosphor-Thin.ttf",
        "light" to "phosphor/Phosphor-Light.ttf",
        "regular" to "phosphor/Phosphor.ttf",
        "bold" to "phosphor/Phosphor-Bold.ttf",
        "fill" to "phosphor/Phosphor-Fill.ttf"
    )

    private val typefaces = mutableMapOf<String, Typeface>()
    private var codes: MutableMap<String, MutableMap<String, Int>>? = null
    private var sortedNames: List<String> = emptyList()

    fun typeface(context: Context, weight: String): Typeface? {
        val key = if (fontFiles.containsKey(weight)) weight else "regular"
        typefaces[key]?.let { return it }
        return try {
            val tf = Typeface.createFromAsset(context.assets, fontFiles.getValue(key))
            typefaces[key] = tf
            tf
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun load(context: Context): MutableMap<String, MutableMap<String, Int>> {
        codes?.let { return it }
        val result = mutableMapOf<String, MutableMap<String, Int>>()
        try {
            val raw = context.assets.open("phosphor/icons.json")
                .bufferedReader().use { it.readText() }
            val root = JSONObject(raw)
            root.keys().forEach { weight ->
                val obj = root.getJSONObject(weight)
                val map = mutableMapOf<String, Int>()
                obj.keys().forEach { name -> map[name] = obj.getInt(name) }
                result[weight] = map
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        codes = result
        sortedNames = result["regular"]?.keys?.sorted() ?: emptyList()
        return result
    }

    /** Tous les noms d'icones, tries alphabetiquement. */
    fun names(context: Context): List<String> {
        load(context)
        return sortedNames
    }

    fun search(context: Context, query: String): List<String> {
        val all = names(context)
        val q = query.trim().lowercase()
        return if (q.isEmpty()) all else all.filter { it.contains(q) }
    }

    /** Caractere a afficher pour une icone stockee, ou null si inconnue. */
    fun glyphOf(context: Context, stored: String): String? {
        if (!stored.startsWith(PREFIX)) return null
        val parts = stored.removePrefix(PREFIX).split(":")
        val weight = if (parts.size > 1) parts[0] else "regular"
        val name = parts.last()
        val code = load(context)[weight]?.get(name) ?: return null
        return String(Character.toChars(code))
    }

    fun glyph(context: Context, weight: String, name: String): String? {
        val code = load(context)[weight]?.get(name) ?: return null
        return String(Character.toChars(code))
    }

    fun weightOf(stored: String): String {
        if (!stored.startsWith(PREFIX)) return "regular"
        val parts = stored.removePrefix(PREFIX).split(":")
        return if (parts.size > 1) parts[0] else "regular"
    }

    fun store(weight: String, name: String): String = "$PREFIX$weight:$name"
}
