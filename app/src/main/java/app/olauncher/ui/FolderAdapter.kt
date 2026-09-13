package app.olauncher.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import app.olauncher.R
import app.olauncher.data.Folder
import app.olauncher.data.FolderIcons
import app.olauncher.databinding.AdapterFolderBinding

/**
 * Olauncher V2 - Barre de dossiers au-dessus du tiroir d'applications.
 * Le dernier element est toujours le bouton "+".
 */
class FolderAdapter(
    private val onFolderClick: (String) -> Unit,
    private val onFolderLongClick: (String) -> Unit
) : RecyclerView.Adapter<FolderAdapter.ViewHolder>() {

    private val folders = mutableListOf<Folder>()
    private var selected: String? = null

    /** Largeur imposee a chaque pastille : la barre affiche exactement 6 colonnes. */
    var cellWidth: Int = 0
    fun setFolders(newFolders: List<Folder>, selectedFolder: String?) {
        folders.clear()
        folders.addAll(newFolders)
        selected = selectedFolder
        notifyDataSetChanged()
    }

    fun currentOrder(): List<String> = folders.map { it.name }

    fun folderAt(position: Int): String? = folders.getOrNull(position)?.name

    fun moveItem(from: Int, to: Int): Boolean {
        if (from !in folders.indices || to !in folders.indices) return false
        folders.add(to, folders.removeAt(from))
        notifyItemMoved(from, to)
        return true
    }

    override fun getItemCount(): Int = folders.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = AdapterFolderBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        if (cellWidth > 0) {
            holder.itemView.layoutParams = holder.itemView.layoutParams.also {
                it.width = cellWidth
            }
        }
        val folder = folders[position]
        holder.bindFolder(folder, folder.name == selected, onFolderClick, onFolderLongClick)
    }

    class ViewHolder(private val binding: AdapterFolderBinding) :
        RecyclerView.ViewHolder(binding.root) {

        private val defaultTypeface = binding.folderName.typeface
        private val defaultTextSize = binding.folderName.textSize /
            binding.folderName.resources.displayMetrics.scaledDensity

        fun bindFolder(
            folder: Folder,
            isSelected: Boolean,
            onClick: (String) -> Unit,
            onLongClick: (String) -> Unit
        ) = with(binding.folderName) {
                val glyph = FolderIcons.glyphOf(context, folder.icon)
                setCompoundDrawablesRelativeWithIntrinsicBounds(0, 0, 0, 0)
                when {
                    glyph != null -> {
                        typeface = FolderIcons.typeface(context, FolderIcons.weightOf(folder.icon))
                        textSize = 19f
                        text = glyph
                    }

                    folder.icon.isNotBlank() -> {
                        typeface = defaultTypeface
                        textSize = defaultTextSize
                        text = folder.icon + "  " + folder.name
                    }

                    else -> {
                        typeface = defaultTypeface
                        textSize = defaultTextSize
                        text = folder.name
                        setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.ic_folder, 0, 0, 0)
                    }
                }
                this.isSelected = isSelected
                setOnClickListener { onClick(folder.name) }
                setOnLongClickListener {
                    onLongClick(folder.name)
                    true
                }
            }
    }
}
