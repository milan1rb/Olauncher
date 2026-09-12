package app.olauncher.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import app.olauncher.R
import app.olauncher.databinding.AdapterFolderBinding

/**
 * Olauncher V2 - Barre de dossiers au-dessus du tiroir d'applications.
 * Le dernier element est toujours le bouton "+".
 */
class FolderAdapter(
    private val onFolderClick: (String) -> Unit,
    private val onAddFolder: () -> Unit
) : RecyclerView.Adapter<FolderAdapter.ViewHolder>() {

    companion object {
        const val TYPE_FOLDER = 0
        const val TYPE_ADD = 1
    }

    private val folders = mutableListOf<String>()
    private var selected: String? = null

    fun setFolders(newFolders: List<String>, selectedFolder: String?) {
        folders.clear()
        folders.addAll(newFolders)
        selected = selectedFolder
        notifyDataSetChanged()
    }

    fun currentOrder(): List<String> = folders.toList()

    fun folderAt(position: Int): String? = folders.getOrNull(position)

    fun moveItem(from: Int, to: Int): Boolean {
        if (from !in folders.indices || to !in folders.indices) return false
        folders.add(to, folders.removeAt(from))
        notifyItemMoved(from, to)
        return true
    }

    override fun getItemCount(): Int = folders.size + 1

    override fun getItemViewType(position: Int): Int =
        if (position == folders.size) TYPE_ADD else TYPE_FOLDER

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = AdapterFolderBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        if (getItemViewType(position) == TYPE_ADD) holder.bindAddButton(onAddFolder)
        else holder.bindFolder(folders[position], folders[position] == selected, onFolderClick)
    }

    class ViewHolder(private val binding: AdapterFolderBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bindFolder(name: String, isSelected: Boolean, onClick: (String) -> Unit) =
            with(binding.folderName) {
                text = name
                this.isSelected = isSelected
                setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.ic_folder, 0, 0, 0)
                setOnClickListener { onClick(name) }
            }

        fun bindAddButton(onClick: () -> Unit) = with(binding.folderName) {
            setText(R.string.folder_add)
            isSelected = false
            setCompoundDrawablesRelativeWithIntrinsicBounds(0, 0, 0, 0)
            setOnClickListener { onClick() }
        }
    }
}
