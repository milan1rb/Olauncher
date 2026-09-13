package app.olauncher.ui

import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import app.olauncher.data.FolderIcons

/** Olauncher V2 - Grille de selection d'icone de dossier (Phosphor Icons). */
class IconPickerAdapter(
    private val onPick: (String) -> Unit
) : RecyclerView.Adapter<IconPickerAdapter.ViewHolder>() {

    private val icons = FolderIcons.all

    override fun getItemCount(): Int = icons.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val density = parent.resources.displayMetrics.density
        val size = (52 * density).toInt()
        val padding = (12 * density).toInt()
        val image = ImageView(parent.context)
        image.layoutParams = ViewGroup.LayoutParams(size, size)
        image.setPadding(padding, padding, padding, padding)
        return ViewHolder(image)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val (key, res) = icons[position]
        holder.image.setImageResource(res)
        holder.image.contentDescription = key
        holder.image.setOnClickListener { onPick(key) }
    }

    class ViewHolder(val image: ImageView) : RecyclerView.ViewHolder(image)
}
