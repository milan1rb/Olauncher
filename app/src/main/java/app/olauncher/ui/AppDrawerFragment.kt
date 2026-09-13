package app.olauncher.ui

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.os.Process
import android.text.Spannable
import android.view.GestureDetector
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.view.inputmethod.BaseInputConnection
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.SearchView
import androidx.core.os.bundleOf
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.Recycler
import app.olauncher.MainViewModel
import app.olauncher.R
import app.olauncher.data.AppLists
import app.olauncher.data.AppModel
import app.olauncher.data.Folder
import app.olauncher.data.FolderIcons
import app.olauncher.data.Constants
import app.olauncher.data.Prefs
import app.olauncher.databinding.DialogFoldersBinding
import app.olauncher.databinding.FragmentAppDrawerBinding
import app.olauncher.helper.deletePinnedShortcut
import app.olauncher.helper.hideKeyboard
import app.olauncher.helper.isEinkDisplay
import app.olauncher.helper.isSystemAnimationsDisabled
import app.olauncher.helper.isSystemApp
import app.olauncher.helper.openAppInfo
import app.olauncher.helper.openSearch
import app.olauncher.helper.openUrl
import app.olauncher.helper.showKeyboard
import kotlin.math.abs
import app.olauncher.helper.showToast
import app.olauncher.helper.uninstall

class AppDrawerFragment : BaseFragment() {


    private lateinit var prefs: Prefs
    private lateinit var appListsPrefs: AppLists
    private var selectedFolder: String? = null
    private lateinit var folderAdapter: FolderAdapter
    private var dragMoved = false
    private lateinit var adapter: AppDrawerAdapter
    private lateinit var linearLayoutManager: LinearLayoutManager
    private var searchTextView: TextView? = null
    private var cachedIsCjkKeyboard: Boolean? = null

    private var flag = Constants.FLAG_LAUNCH_APP
    private var canRename = false
    private var currentAppList: List<AppModel>? = null
    private var currentPrivateSpaceApps: List<AppModel>? = null
    private var currentPrivateSpaceLocked: Boolean = true
    private var currentPrivateSpaceAvailable: Boolean = false

    private val viewModel: MainViewModel by activityViewModels()
    private var _binding: FragmentAppDrawerBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentAppDrawerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        prefs = Prefs(requireContext())
        appListsPrefs = AppLists(requireContext())
        arguments?.let {
            flag = it.getInt(Constants.Key.FLAG, Constants.FLAG_LAUNCH_APP)
            canRename = it.getBoolean(Constants.Key.RENAME, false)
        }

        initViews()
        initSearch()
        initAdapter()
        initObservers()
        initClickListeners()
    }

    private fun initViews() {
        if (flag == Constants.FLAG_HIDDEN_APPS)
            binding.search.queryHint = getString(R.string.hidden_apps)
        else if (flag in Constants.FLAG_SET_HOME_APP_1..Constants.FLAG_SET_CALENDAR_APP)
            binding.search.queryHint = "Please select an app"
        try {
            searchTextView = binding.search.findViewById(R.id.search_src_text)
            searchTextView?.gravity = prefs.appLabelAlignment
        } catch (e: Exception) {
            e.printStackTrace()
        }
        initFolders()
        initDrawerSwipe()
    }

    private fun initSearch() {
        binding.search.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                if (query?.startsWith("!") == true)
                    requireContext().openUrl(Constants.URL_DUCK_SEARCH + query.replace(" ", "%20"))
                else if (adapter.itemCount == 0)
                    requireContext().openSearch(query?.trim())
                else
                    adapter.launchFirstInList()
                return true
            }

            override fun onQueryTextChange(newText: String): Boolean {
                try {
                    adapter.allowAutoLaunch = !isSearchComposing()
                    adapter.filter.filter(newText)
                    binding.appRename.visibility =
                        if (canRename && newText.isNotBlank()) View.VISIBLE else View.GONE
                    return true
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                return false
            }
        })
    }

    private fun isSearchComposing(): Boolean {
        val text = searchTextView?.text
        if (text !is Spannable) return false
        val start = BaseInputConnection.getComposingSpanStart(text)
        val end = BaseInputConnection.getComposingSpanEnd(text)
        if (start !in 0 until end) return false
        return isCjkKeyboard()
    }

    private fun isCjkKeyboard(): Boolean {
        cachedIsCjkKeyboard?.let { return it }
        val result = try {
            val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            val subtype = imm.currentInputMethodSubtype
            val language = when {
                subtype == null -> ""
                subtype.languageTag.isNotEmpty() -> subtype.languageTag // e.g. "zh-CN", "ja-JP", "en-US"
                else -> subtype.locale // deprecated fallback, e.g. "zh_CN"
            }
            language.startsWith("zh") || language.startsWith("ja") || language.startsWith("ko")
        } catch (e: Exception) {
            false
        }
        cachedIsCjkKeyboard = result
        return result
    }

    private fun initAdapter() {
        adapter = AppDrawerAdapter(
            flag,
            prefs.appLabelAlignment,
            appClickListener = { appModel ->
                viewModel.selectedApp(appModel, flag)
                if (flag == Constants.FLAG_LAUNCH_APP || flag == Constants.FLAG_HIDDEN_APPS)
                    findNavController().popBackStack(R.id.mainFragment, false)
                else
                    findNavController().popBackStack()
            },
            appInfoListener = {
                openAppInfo(
                    requireContext(),
                    it.user,
                    it.appPackage
                )
                findNavController().popBackStack(R.id.mainFragment, false)
            },
            appDeleteListener = { appModel ->
                when (appModel) {
                    is AppModel.PrivateSpaceHeader -> {}
                    is AppModel.PinnedShortcut ->
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
                            requireContext().deletePinnedShortcut(
                                packageName = appModel.appPackage,
                                shortcutIdToDelete = appModel.shortcutId,
                                user = appModel.user,
                            )
                        }

                    is AppModel.App -> {
                        if (appModel.user != Process.myUserHandle()) {
                            openAppInfo(requireContext(), appModel.user, appModel.appPackage)
                        } else if (requireContext().isSystemApp(appModel.appPackage, appModel.user)) {
                            requireContext().showToast(getString(R.string.system_app_cannot_delete))
                            openAppInfo(requireContext(), appModel.user, appModel.appPackage)
                        } else {
                            requireContext().uninstall(appModel.appPackage)
                        }
                    }
                }
                viewModel.getAppList()
            },
            appHideListener = { appModel, position ->
                if (appModel is AppModel.PinnedShortcut) {
                    requireContext().showToast("Hiding pinned shortcuts is not supported")
                    return@AppDrawerAdapter
                }
                adapter.appFilteredList.removeAt(position)
                adapter.notifyItemRemoved(position)
                adapter.appsList.remove(appModel)

                val newSet = mutableSetOf<String>()
                newSet.addAll(prefs.hiddenApps)
                if (flag == Constants.FLAG_HIDDEN_APPS)
                    newSet.remove(appModel.appPackage + "|" + appModel.user.toString())
                else
                    newSet.add(appModel.appPackage + "|" + appModel.user.toString())

                prefs.hiddenApps = newSet
                if (newSet.isEmpty())
                    findNavController().popBackStack()
                if (prefs.firstHide) {
                    binding.search.hideKeyboard()
                    prefs.firstHide = false
                    viewModel.showDialog.postValue(Constants.Dialog.HIDDEN)
                    findNavController().navigate(R.id.action_appListFragment_to_settingsFragment2)
                }
                viewModel.getAppList()
                viewModel.getHiddenApps()
            },
            appRenameListener = { appModel, renameLabel ->
                val identifier = when (appModel) {
                    is AppModel.PinnedShortcut -> appModel.identity
                    is AppModel.App -> appModel.appPackage
                    else -> return@AppDrawerAdapter
                }
                prefs.setAppRenameLabel(identifier, renameLabel)
                viewModel.getAppList()
            },
            appFoldersListener = { appModel ->
                binding.search.hideKeyboard()
                showAppFoldersDialog(appModel)
            },
            privateSpaceToggleListener = {
                viewModel.togglePrivateSpaceLock()
            },
            privateSpaceSettingsListener = {
                viewModel.openPrivateSpaceSettings()
                findNavController().popBackStack(R.id.mainFragment, false)
            }
        )

        linearLayoutManager = object : LinearLayoutManager(requireContext()) {
            override fun scrollVerticallyBy(
                dx: Int,
                recycler: Recycler,
                state: RecyclerView.State,
            ): Int {
                val scrollRange = super.scrollVerticallyBy(dx, recycler, state)
                val overScroll = dx - scrollRange
                if (overScroll < -10 && binding.recyclerView.scrollState == RecyclerView.SCROLL_STATE_DRAGGING)
                    checkMessageAndExit()
                return scrollRange
            }
        }

        binding.recyclerView.layoutManager = linearLayoutManager
        binding.recyclerView.adapter = adapter
        binding.recyclerView.addOnScrollListener(getRecyclerViewOnScrollListener())
        binding.recyclerView.itemAnimator = null
        if (requireContext().isEinkDisplay())
            binding.recyclerView.overScrollMode = View.OVER_SCROLL_NEVER
        else if (requireContext().isSystemAnimationsDisabled().not())
            binding.recyclerView.layoutAnimation =
                AnimationUtils.loadLayoutAnimation(requireContext(), R.anim.layout_anim_from_bottom)
    }

    private fun initObservers() {
        viewModel.firstOpen.observe(viewLifecycleOwner) {
        }
        if (flag == Constants.FLAG_HIDDEN_APPS) {
            viewModel.hiddenApps.observe(viewLifecycleOwner) {
                it?.let {
                    adapter.setAppList(it.toMutableList())
                }
            }
        } else {
            viewModel.appList.observe(viewLifecycleOwner) {
                currentAppList = it
                updateCombinedAppList()
            }
            if (flag == Constants.FLAG_LAUNCH_APP) {
                viewModel.privateSpaceAvailable.observe(viewLifecycleOwner) {
                    currentPrivateSpaceAvailable = it
                    updateCombinedAppList()
                }
                viewModel.privateSpaceLocked.observe(viewLifecycleOwner) {
                    currentPrivateSpaceLocked = it
                    updateCombinedAppList()
                }
                viewModel.privateSpaceApps.observe(viewLifecycleOwner) {
                    currentPrivateSpaceApps = it
                    updateCombinedAppList()
                }
            }
        }
    }

    private fun updateCombinedAppList() {
        val apps = currentAppList ?: return
        val combined = apps.toMutableList()

        if (flag == Constants.FLAG_LAUNCH_APP && currentPrivateSpaceAvailable) {
            combined.add(AppModel.PrivateSpaceHeader(isLocked = currentPrivateSpaceLocked))
            if (!currentPrivateSpaceLocked) {
                currentPrivateSpaceApps?.let { combined.addAll(it) }
            }
        }

        if (flag == Constants.FLAG_LAUNCH_APP) {
            adapter.folderedApps = appListsPrefs.allFolderedApps()
            adapter.openFolderApps = selectedFolder?.let { appListsPrefs.appsIn(it) }
            adapter.folderLabels = appListsPrefs.labelsByApp()
        } else {
            adapter.folderedApps = emptySet()
            adapter.openFolderApps = null
            adapter.folderLabels = emptyMap()
        }

        adapter.setAppList(combined)
        adapter.filter.filter(binding.search.query)
    }

    private fun initClickListeners() {
        binding.appRename.setOnClickListener {
            val name = binding.search.query.toString().trim()
            if (name.isEmpty()) {
                requireContext().showToast(getString(R.string.type_a_new_app_name_first))
                binding.search.showKeyboard()
                return@setOnClickListener
            }

            when (flag) {
                Constants.FLAG_SET_HOME_APP_1 -> prefs.appName1 = name
                Constants.FLAG_SET_HOME_APP_2 -> prefs.appName2 = name
                Constants.FLAG_SET_HOME_APP_3 -> prefs.appName3 = name
                Constants.FLAG_SET_HOME_APP_4 -> prefs.appName4 = name
                Constants.FLAG_SET_HOME_APP_5 -> prefs.appName5 = name
                Constants.FLAG_SET_HOME_APP_6 -> prefs.appName6 = name
                Constants.FLAG_SET_HOME_APP_7 -> prefs.appName7 = name
                Constants.FLAG_SET_HOME_APP_8 -> prefs.appName8 = name
            }
            findNavController().popBackStack()
        }
    }

    // ---------------------------------------------------------------
    // Olauncher V2 - Dossiers
    // ---------------------------------------------------------------

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    private fun initFolders() {
        if (flag != Constants.FLAG_LAUNCH_APP) {
            binding.folderBar.visibility = View.GONE
            return
        }

        folderAdapter = FolderAdapter(
            onFolderClick = { name ->
                selectedFolder = if (selectedFolder == name) null else name
                refreshFolders()
                updateCombinedAppList()
            }
        )
        binding.folderMore.setOnClickListener { showAllFoldersDialog() }

        binding.folderRecycler.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        binding.folderRecycler.adapter = folderAdapter
        ItemTouchHelper(folderDragCallback()).attachToRecyclerView(binding.folderRecycler)

        refreshFolders()
    }

    private fun refreshFolders() {
        if (flag != Constants.FLAG_LAUNCH_APP) return
        val folders: List<Folder> = appListsPrefs.getFolders()
        if (selectedFolder != null && folders.none { it.name == selectedFolder }) selectedFolder = null
        binding.folderBar.visibility = View.VISIBLE
        folderAdapter.setFolders(folders.take(appListsPrefs.visibleCount()), selectedFolder)
    }

    /** Balayage horizontal dans le tiroir = dossier suivant / precedent. */
    private fun initDrawerSwipe() {
        val detector = GestureDetector(
            requireContext(),
            object : GestureDetector.SimpleOnGestureListener() {
                override fun onFling(
                    e1: MotionEvent?,
                    e2: MotionEvent,
                    velocityX: Float,
                    velocityY: Float
                ): Boolean {
                    if (e1 == null) return false
                    val dx = e2.x - e1.x
                    val dy = e2.y - e1.y
                    if (abs(dx) < dp(70) || abs(dx) < abs(dy) * 2) return false
                    switchFolder(if (dx < 0) 1 else -1)
                    return true
                }
            }
        )
        binding.recyclerView.addOnItemTouchListener(
            object : RecyclerView.SimpleOnItemTouchListener() {
                override fun onInterceptTouchEvent(rv: RecyclerView, e: MotionEvent): Boolean {
                    detector.onTouchEvent(e)
                    return false
                }
            }
        )
    }

    private fun switchFolder(direction: Int) {
        if (flag != Constants.FLAG_LAUNCH_APP) return
        if (binding.search.query.isNullOrBlank().not()) return
        val names = appListsPrefs.names().take(appListsPrefs.visibleCount())
        if (names.isEmpty()) return

        val items: List<String?> = listOf(null) + names
        var next = items.indexOf(selectedFolder) + direction
        if (next < 0) next = items.size - 1
        if (next >= items.size) next = 0

        selectedFolder = items[next]
        refreshFolders()
        updateCombinedAppList()
        if (next > 0) binding.folderRecycler.smoothScrollToPosition(next - 1)
        binding.recyclerView.scrollToPosition(0)
    }

    /**
     * Appui long + glissement = reordonner les dossiers.
     * Appui long sans bouger = menu renommer / icone / supprimer.
     */
    private fun folderDragCallback(): ItemTouchHelper.Callback {
        return object : ItemTouchHelper.SimpleCallback(
            ItemTouchHelper.START or ItemTouchHelper.END, 0
        ) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                val moved = folderAdapter.moveItem(
                    viewHolder.bindingAdapterPosition,
                    target.bindingAdapterPosition
                )
                if (moved) dragMoved = true
                return moved
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {}

            override fun onSelectedChanged(
                viewHolder: RecyclerView.ViewHolder?,
                actionState: Int
            ) {
                super.onSelectedChanged(viewHolder, actionState)
                if (actionState == ItemTouchHelper.ACTION_STATE_DRAG) {
                    dragMoved = false
                    viewHolder?.itemView?.alpha = 0.6f
                }
            }

            override fun clearView(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder
            ) {
                super.clearView(recyclerView, viewHolder)
                viewHolder.itemView.alpha = 1f
                val position = viewHolder.bindingAdapterPosition
                if (dragMoved) {
                    appListsPrefs.setOrder(folderAdapter.currentOrder())
                } else {
                    folderAdapter.folderAt(position)?.let { showFolderOptionsDialog(it) }
                }
                dragMoved = false
            }
        }
    }

    /** Menu "..." : dossiers reordonnables, creation et applis masquees. */
    private fun showAllFoldersDialog() {
        binding.search.hideKeyboard()
        val dialogBinding = DialogFoldersBinding.inflate(layoutInflater)
        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogBinding.root)
            .create()

        lateinit var touchHelper: ItemTouchHelper
        val listAdapter = FolderListAdapter(
            onClick = { name ->
                selectedFolder = name
                refreshFolders()
                updateCombinedAppList()
                binding.recyclerView.scrollToPosition(0)
                dialog.dismiss()
            },
            onLongClick = { name ->
                dialog.dismiss()
                showFolderOptionsDialog(name)
            },
            onStartDrag = { holder -> touchHelper.startDrag(holder) }
        )
        listAdapter.setFolders(appListsPrefs.getFolders(), appListsPrefs.visibleCount())

        dialogBinding.dialogFolderList.layoutManager = LinearLayoutManager(requireContext())
        dialogBinding.dialogFolderList.adapter = listAdapter

        touchHelper = ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(
            ItemTouchHelper.UP or ItemTouchHelper.DOWN, 0
        ) {
            override fun isLongPressDragEnabled(): Boolean = false

            override fun getMovementFlags(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder
            ): Int {
                if (viewHolder.itemViewType == FolderListAdapter.TYPE_SEPARATOR) return 0
                return super.getMovementFlags(recyclerView, viewHolder)
            }

            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean = listAdapter.moveItem(
                viewHolder.bindingAdapterPosition,
                target.bindingAdapterPosition
            )

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {}
        })
        touchHelper.attachToRecyclerView(dialogBinding.dialogFolderList)

        dialogBinding.dialogAllApps.setOnClickListener {
            selectedFolder = null
            refreshFolders()
            updateCombinedAppList()
            dialog.dismiss()
        }
        dialogBinding.dialogNewFolder.setOnClickListener {
            dialog.dismiss()
            showCreateFolderDialog(null)
        }
        dialogBinding.dialogHiddenApps.setOnClickListener {
            dialog.dismiss()
            findNavController().navigate(
                R.id.action_appListFragment_self,
                bundleOf(Constants.Key.FLAG to Constants.FLAG_HIDDEN_APPS)
            )
        }

        dialog.setOnDismissListener {
            appListsPrefs.setOrder(listAdapter.currentOrder())
            appListsPrefs.setVisibleCount(listAdapter.visibleCount())
            refreshFolders()
        }
        dialog.show()

        // Hauteur plafonnee : la liste defile au lieu de s'etirer
        dialogBinding.dialogFolderList.post {
            val maxHeight = (resources.displayMetrics.heightPixels * 0.45f).toInt()
            if (dialogBinding.dialogFolderList.height > maxHeight) {
                dialogBinding.dialogFolderList.layoutParams =
                    dialogBinding.dialogFolderList.layoutParams.apply { height = maxHeight }
                dialogBinding.dialogFolderList.requestLayout()
            }
        }
    }

    private fun showAppFoldersDialog(appModel: AppModel) {
        val names = appListsPrefs.names()
        if (names.isEmpty()) {
            showCreateFolderDialog(appModel)
            return
        }
        val appId = AppLists.idOf(appModel)
        val checked = BooleanArray(names.size) { appListsPrefs.isInList(names[it], appId) }
        AlertDialog.Builder(requireContext())
            .setTitle(appModel.appLabel)
            .setMultiChoiceItems(names.toTypedArray(), checked) { _, which, isChecked ->
                appListsPrefs.setAppInList(names[which], appId, isChecked)
            }
            .setNeutralButton(R.string.new_folder) { _, _ -> showCreateFolderDialog(appModel) }
            .setPositiveButton(R.string.folder_done, null)
            .setOnDismissListener {
                refreshFolders()
                updateCombinedAppList()
            }
            .show()
    }

    private fun showCreateFolderDialog(appModel: AppModel?) {
        val input = EditText(requireContext())
        input.hint = getString(R.string.folder_name_hint)
        input.setSingleLine()
        val container = FrameLayout(requireContext())
        container.setPadding(dp(24), dp(8), dp(24), 0)
        container.addView(input)

        AlertDialog.Builder(requireContext())
            .setTitle(R.string.new_folder)
            .setView(container)
            .setPositiveButton(R.string.folder_create) { _, _ ->
                val name = input.text.toString().trim()
                if (appListsPrefs.createList(name)) {
                    appModel?.let { appListsPrefs.setAppInList(name, AppLists.idOf(it), true) }
                    refreshFolders()
                    updateCombinedAppList()
                } else {
                    requireContext().showToast(getString(R.string.folder_name_invalid))
                }
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
        input.showKeyboard()
    }

    private fun showFolderOptionsDialog(name: String) {
        val options = arrayOf(
            getString(R.string.rename),
            getString(R.string.folder_icon),
            getString(R.string.folder_delete)
        )
        AlertDialog.Builder(requireContext())
            .setTitle(name)
            .setItems(options) { _, which ->
                when (which) {
                    0 -> showRenameFolderDialog(name)
                    1 -> showFolderIconDialog(name)
                    else -> {
                        appListsPrefs.deleteList(name)
                        if (selectedFolder == name) selectedFolder = null
                        refreshFolders()
                        updateCombinedAppList()
                    }
                }
            }
            .show()
    }

    private fun showFolderIconDialog(name: String) {
        val grid = RecyclerView(requireContext())
        grid.layoutManager = GridLayoutManager(requireContext(), 5)
        grid.setPadding(dp(16), dp(16), dp(16), dp(16))
        grid.clipToPadding = false

        val dialog = AlertDialog.Builder(requireContext())
            .setTitle(name)
            .setView(grid)
            .setNeutralButton(R.string.folder_icon_default) { _, _ ->
                appListsPrefs.setIcon(name, "")
                refreshFolders()
            }
            .create()

        grid.adapter = IconPickerAdapter { icon ->
            appListsPrefs.setIcon(name, FolderIcons.PREFIX + icon)
            refreshFolders()
            dialog.dismiss()
        }
        dialog.show()
    }

    private fun showRenameFolderDialog(name: String) {
        val input = EditText(requireContext())
        input.setText(name)
        input.setSingleLine()
        input.setSelectAllOnFocus(true)
        val container = FrameLayout(requireContext())
        container.setPadding(dp(24), dp(8), dp(24), 0)
        container.addView(input)

        AlertDialog.Builder(requireContext())
            .setTitle(R.string.rename)
            .setView(container)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                val newName = input.text.toString().trim()
                if (appListsPrefs.renameList(name, newName)) {
                    if (selectedFolder == name) selectedFolder = newName
                    refreshFolders()
                    updateCombinedAppList()
                } else {
                    requireContext().showToast(getString(R.string.folder_name_invalid))
                }
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
        input.showKeyboard()
    }

    private fun getRecyclerViewOnScrollListener(): RecyclerView.OnScrollListener {
        return object : RecyclerView.OnScrollListener() {

            var onTop = false

            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)
                when (newState) {

                    RecyclerView.SCROLL_STATE_DRAGGING -> {
                        onTop = !recyclerView.canScrollVertically(-1)
                        if (onTop)
                            binding.search.hideKeyboard()
                    }

                    RecyclerView.SCROLL_STATE_IDLE -> {
                        if (!recyclerView.canScrollVertically(1))
                            binding.search.hideKeyboard()
                        else if (!recyclerView.canScrollVertically(-1))
                            if (!onTop && isRemoving.not())
                                binding.search.showKeyboard(prefs.autoShowKeyboard)
                    }
                }
            }
        }
    }

    private fun checkMessageAndExit() {
        findNavController().popBackStack()
        if (flag == Constants.FLAG_LAUNCH_APP)
            viewModel.checkForMessages.call()
    }

    override fun onStart() {
        super.onStart()
        cachedIsCjkKeyboard = null
        binding.search.showKeyboard(prefs.autoShowKeyboard)
    }

    override fun onStop() {
        binding.search.hideKeyboard()
        super.onStop()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        searchTextView = null
        _binding = null
    }
}
