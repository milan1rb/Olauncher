package app.olauncher.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import app.olauncher.R
import app.olauncher.data.Folder
import app.olauncher.databinding.AdapterFolderBinding

/**
 * Olauncher V2 - Barre de dossiers au-dessus du tiroir d'applications.
 * Le dernier element est toujours le bouton "+".
 */
class FolderAdapter(
    private val onFolderClick: (String) -> Unit,
    private val onMore: () -> Unit
) : RecyclerView.Adapter<FolderAdapter.ViewHolder>() {

    companion object {
        const val TYPE_FOLDER = 0
        const val TYPE_MORE = 1
    }

    private val folders = mutableListOf<Folder>()
    private var selected: String? = null
    private var maxVisible = Int.MAX_VALUE

    /** Seuls les [maxVisible] premiers dossiers sont affiches, le reste passe dans le menu. */
    fun setFolders(newFolders: List<Folder>, selectedFolder: String?, visibleCount: Int) {
        folders.clear()
        folders.addAll(newFolders)
        selected = selectedFolder
        maxVisible = visibleCount.coerceAtLeast(0)
        notifyDataSetChanged()
    }

    private fun visibleFolders(): List<Folder> = folders.take(maxVisible)

    fun currentOrder(): List<String> = folders.map { it.name }

    fun folderAt(position: Int): String? = visibleFolders().getOrNull(position)?.name

    fun moveItem(from: Int, to: Int): Boolean {
        if (from !in folders.indices || to !in folders.indices) return false
        folders.add(to, folders.removeAt(from))
        notifyItemMoved(from, to)
        return true
    }

    override fun getItemCount(): Int = visibleFolders().size + 1

    override fun getItemViewType(position: Int): Int =
        if (position == visibleFolders().size) TYPE_MORE else TYPE_FOLDER

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = AdapterFolderBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        if (getItemViewType(position) == TYPE_MORE) holder.bindMoreButton(onMore)
        else {
            val folder = visibleFolders()[position]
            holder.bindFolder(folder, folder.name == selected, onFolderClick)
        }
    }

    class ViewHolder(private val binding: AdapterFolderBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bindFolder(folder: Folder, isSelected: Boolean, onClick: (String) -> Unit) =
            with(binding.folderName) {
                if (folder.icon.isBlank()) {
                    text = folder.name
                    setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.ic_folder, 0, 0, 0)
                } else {
                    text = folder.icon + "  " + folder.name
                    setCompoundDrawablesRelativeWithIntrinsicBounds(0, 0, 0, 0)
                }
                this.isSelected = isSelected
                setOnClickListener { onClick(folder.name) }
            }

        fun bindMoreButton(onClick: () -> Unit) = with(binding.folderName) {
            setText(R.string.folder_more)
            isSelected = false
            setCompoundDrawablesRelativeWithIntrinsicBounds(0, 0, 0, 0)
            setOnClickListener { onClick() }
        }
    }
}
