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
    private val onAddFolder: () -> Unit,
    private val onHiddenApps: () -> Unit = {}
) : RecyclerView.Adapter<FolderAdapter.ViewHolder>() {

    companion object {
        const val TYPE_FOLDER = 0
        const val TYPE_ADD = 1
        const val TYPE_HIDDEN = 2
    }

    private val folders = mutableListOf<Folder>()
    private var selected: String? = null

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

    override fun getItemCount(): Int = folders.size + 2

    override fun getItemViewType(position: Int): Int = when (position) {
        folders.size -> TYPE_ADD
        folders.size + 1 -> TYPE_HIDDEN
        else -> TYPE_FOLDER
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = AdapterFolderBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        when (getItemViewType(position)) {
            TYPE_ADD -> holder.bindAddButton(onAddFolder)
            TYPE_HIDDEN -> holder.bindHiddenButton(onHiddenApps)
            else -> {
                val folder = folders[position]
                holder.bindFolder(folder, folder.name == selected, onFolderClick)
            }
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

        fun bindAddButton(onClick: () -> Unit) = with(binding.folderName) {
            setText(R.string.folder_add)
            isSelected = false
            setCompoundDrawablesRelativeWithIntrinsicBounds(0, 0, 0, 0)
            setOnClickListener { onClick() }
        }

        fun bindHiddenButton(onClick: () -> Unit) = with(binding.folderName) {
            setText(R.string.folder_hidden_apps)
            isSelected = false
            setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.ic_hide, 0, 0, 0)
            setOnClickListener { onClick() }
        }
    }
}
