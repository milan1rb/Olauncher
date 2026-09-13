package app.olauncher.ui

import android.content.Context
import android.view.Gravity
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import app.olauncher.data.FolderIcons

/** Olauncher V2 - Grille de selection parmi les 1512 icones Phosphor. */
class IconPickerAdapter(
    private val context: Context,
    private val onPick: (String, String) -> Unit
) : RecyclerView.Adapter<IconPickerAdapter.ViewHolder>() {

    private var names: List<String> = FolderIcons.names(context)
    private var weight: String = "regular"

    fun setWeight(newWeight: String) {
        weight = newWeight
        notifyDataSetChanged()
    }

    fun setQuery(query: String) {
        names = FolderIcons.search(context, query)
        notifyDataSetChanged()
    }

    override fun getItemCount(): Int = names.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val density = parent.resources.displayMetrics.density
        val size = (54 * density).toInt()
        val view = TextView(parent.context)
        view.layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, size)
        view.gravity = Gravity.CENTER
        view.textSize = 26f
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val name = names[position]
        holder.view.typeface = FolderIcons.typeface(context, weight)
        holder.view.text = FolderIcons.glyph(context, weight, name) ?: ""
        holder.view.contentDescription = name
        holder.view.setOnClickListener { onPick(weight, name) }
    }

    class ViewHolder(val view: TextView) : RecyclerView.ViewHolder(view)
}
