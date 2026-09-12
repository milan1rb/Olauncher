package app.olauncher.ui

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import app.olauncher.data.Folder
import app.olauncher.databinding.AdapterFolderRowBinding

/**
 * Olauncher V2 - Liste des dossiers dans le menu "...".
 * Chaque ligne se deplace en maintenant la poignee a droite.
 */
class FolderListAdapter(
    private val onClick: (String) -> Unit,
    private val onStartDrag: (RecyclerView.ViewHolder) -> Unit
) : RecyclerView.Adapter<FolderListAdapter.ViewHolder>() {

    private val folders = mutableListOf<Folder>()

    fun setFolders(newFolders: List<Folder>) {
        folders.clear()
        folders.addAll(newFolders)
        notifyDataSetChanged()
    }

    fun currentOrder(): List<String> = folders.map { it.name }

    fun moveItem(from: Int, to: Int): Boolean {
        if (from !in folders.indices || to !in folders.indices) return false
        folders.add(to, folders.removeAt(from))
        notifyItemMoved(from, to)
        return true
    }

    override fun getItemCount(): Int = folders.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
        ViewHolder(
            AdapterFolderRowBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        )

    @SuppressLint("ClickableViewAccessibility")
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val folder = folders[position]
        holder.binding.folderRowName.text =
            if (folder.icon.isBlank()) folder.name else folder.icon + "  " + folder.name
        holder.binding.folderRowName.setOnClickListener { onClick(folder.name) }
        holder.binding.folderRowHandle.setOnTouchListener { _, event ->
            if (event.actionMasked == MotionEvent.ACTION_DOWN) onStartDrag(holder)
            false
        }
    }

    class ViewHolder(val binding: AdapterFolderRowBinding) :
        RecyclerView.ViewHolder(binding.root)
}
