package app.olauncher.data

import app.olauncher.R

/**
 * Olauncher V2 - Banque d'icones de dossier (Phosphor Icons, licence MIT).
 * L'icone est stockee sous la forme "ph:nom". Les anciennes valeurs (emoji)
 * restent affichees telles quelles.
 */
object FolderIcons {

    const val PREFIX = "ph:"

    val all: List<Pair<String, Int>> = listOf(
        "airplane-tilt" to R.drawable.ic_ph_airplane_tilt,
        "book-open" to R.drawable.ic_ph_book_open,
        "briefcase" to R.drawable.ic_ph_briefcase,
        "calendar-blank" to R.drawable.ic_ph_calendar_blank,
        "camera" to R.drawable.ic_ph_camera,
        "chat-circle" to R.drawable.ic_ph_chat_circle,
        "cloud" to R.drawable.ic_ph_cloud,
        "envelope" to R.drawable.ic_ph_envelope,
        "film-slate" to R.drawable.ic_ph_film_slate,
        "folder" to R.drawable.ic_ph_folder,
        "game-controller" to R.drawable.ic_ph_game_controller,
        "gear" to R.drawable.ic_ph_gear,
        "globe" to R.drawable.ic_ph_globe,
        "hamburger" to R.drawable.ic_ph_hamburger,
        "heart" to R.drawable.ic_ph_heart,
        "house" to R.drawable.ic_ph_house,
        "lightning" to R.drawable.ic_ph_lightning,
        "lock" to R.drawable.ic_ph_lock,
        "map-pin" to R.drawable.ic_ph_map_pin,
        "music-notes" to R.drawable.ic_ph_music_notes,
        "note-pencil" to R.drawable.ic_ph_note_pencil,
        "palette" to R.drawable.ic_ph_palette,
        "person-simple-run" to R.drawable.ic_ph_person_simple_run,
        "phone" to R.drawable.ic_ph_phone,
        "shopping-cart" to R.drawable.ic_ph_shopping_cart,
        "star" to R.drawable.ic_ph_star,
        "wallet" to R.drawable.ic_ph_wallet,
        "wrench" to R.drawable.ic_ph_wrench,
    )

    /** Ressource drawable correspondant a une icone stockee, ou null. */
    fun resOf(icon: String): Int? {
        if (!icon.startsWith(PREFIX)) return null
        val key = icon.removePrefix(PREFIX)
        return all.firstOrNull { it.first == key }?.second
    }
}
