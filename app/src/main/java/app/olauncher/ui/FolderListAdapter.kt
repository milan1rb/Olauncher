package app.olauncher.ui

import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import app.olauncher.R
import app.olauncher.data.Folder
import app.olauncher.data.FolderIcons
import app.olauncher.databinding.AdapterFolderRowBinding
import app.olauncher.databinding.AdapterFolderSeparatorBinding

/**
 * Olauncher V2 - Liste des dossiers du menu "...".
 * Un separateur indique la limite : au-dessus, les dossiers apparaissent dans
 * la barre du tiroir ; en dessous, ils ne vivent que dans ce menu.
 */
class FolderListAdapter(
    private val onClick: (String) -> Unit,
    private val onLongClick: (String) -> Unit,
    private val onStartDrag: (RecyclerView.ViewHolder) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        const val TYPE_FOLDER = 0
        const val TYPE_SEPARATOR = 1
    }

    /** null = le separateur */
    private val items = mutableListOf<Folder?>()

    /** La section "masques de la barre" demarre repliee. */
    private var expanded = false

    private fun separatorIndex(): Int = items.indexOfFirst { it == null }.coerceAtLeast(0)

    fun setFolders(folders: List<Folder>, visibleCount: Int) {
        expanded = false
        items.clear()
        val cut = visibleCount.coerceIn(0, folders.size)
        items.addAll(folders.take(cut))
        items.add(null)
        items.addAll(folders.drop(cut))
        notifyDataSetChanged()
    }

    fun currentOrder(): List<String> = items.filterNotNull().map { it.name }

    /** Nombre de dossiers situes au-dessus du separateur. */
    fun visibleCount(): Int = items.indexOfFirst { it == null }.coerceAtLeast(0)

    fun moveItem(from: Int, to: Int): Boolean {
        if (from !in items.indices || to !in items.indices) return false
        items.add(to, items.removeAt(from))
        notifyItemMoved(from, to)
        return true
    }

    override fun getItemCount(): Int =
        if (expanded) items.size else separatorIndex() + 1

    override fun getItemViewType(position: Int): Int =
        if (items[position] == null) TYPE_SEPARATOR else TYPE_FOLDER

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return if (viewType == TYPE_SEPARATOR)
            SeparatorHolder(AdapterFolderSeparatorBinding.inflate(inflater, parent, false))
        else FolderHolder(AdapterFolderRowBinding.inflate(inflater, parent, false))
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val folder = items[position]
        if (folder == null) {
            val hidden = items.size - separatorIndex() - 1
            val label = (holder as SeparatorHolder).binding.separatorLabel
            val arrow = if (expanded) "▾" else "▸"
            label.text = label.context.getString(R.string.folder_hidden_from_bar) +
                "  ($hidden)  $arrow"
            holder.itemView.setOnClickListener {
                expanded = !expanded
                notifyDataSetChanged()
            }
            return
        }
        val binding = (holder as FolderHolder).binding

        val context = binding.root.context
        val glyph = FolderIcons.glyphOf(context, folder.icon)
        binding.folderRowIcon.visibility = View.VISIBLE
        binding.folderRowName.text = folder.name
        when {
            glyph != null -> {
                binding.folderRowIcon.typeface =
                    FolderIcons.typeface(context, FolderIcons.weightOf(folder.icon))
                binding.folderRowIcon.text = glyph
            }

            folder.icon.isNotBlank() -> {
                binding.folderRowIcon.typeface = null
                binding.folderRowIcon.text = folder.icon
            }

            else -> {
                binding.folderRowIcon.typeface =
                    FolderIcons.typeface(context, "regular")
                binding.folderRowIcon.text =
                    FolderIcons.glyph(context, "regular", "folder") ?: ""
            }
        }

        binding.folderRowName.setOnClickListener { onClick(folder.name) }
        binding.folderRowName.setOnLongClickListener {
            onLongClick(folder.name)
            true
        }
        binding.folderRowHandle.setOnTouchListener { _, event ->
            if (event.actionMasked == MotionEvent.ACTION_DOWN) onStartDrag(holder)
            false
        }
    }

    class FolderHolder(val binding: AdapterFolderRowBinding) :
        RecyclerView.ViewHolder(binding.root)

    class SeparatorHolder(val binding: AdapterFolderSeparatorBinding) :
        RecyclerView.ViewHolder(binding.root)
}
