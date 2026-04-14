package eu.kanade.tachiyomi.ui.extension

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.WindowInsetsCompat.Type.systemBars
import androidx.core.view.updatePaddingRelative
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.tabs.TabLayout
import eu.davidea.flexibleadapter.FlexibleAdapter
import eu.davidea.flexibleadapter.items.IFlexible
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.databinding.ExtensionsBottomSheetBinding
import eu.kanade.tachiyomi.databinding.RecyclerWithScrollerBinding
import eu.kanade.tachiyomi.extension.model.Extension
import eu.kanade.tachiyomi.extension.model.InstallStep
import eu.kanade.tachiyomi.extension.model.InstalledExtensionsOrder
import eu.kanade.tachiyomi.ui.extension.details.ExtensionDetailsController
import eu.kanade.tachiyomi.ui.main.MainActivity
import eu.kanade.tachiyomi.ui.migration.BaseMigrationInterface
import eu.kanade.tachiyomi.ui.migration.MangaAdapter
import eu.kanade.tachiyomi.ui.migration.MangaItem
import eu.kanade.tachiyomi.ui.migration.SourceAdapter
import eu.kanade.tachiyomi.ui.migration.SourceItem
import eu.kanade.tachiyomi.ui.migration.manga.design.PreMigrationController
import eu.kanade.tachiyomi.ui.source.BrowseController
import eu.kanade.tachiyomi.util.system.isPackageInstalled
import eu.kanade.tachiyomi.util.system.materialAlertDialog
import eu.kanade.tachiyomi.util.system.rootWindowInsetsCompat
import eu.kanade.tachiyomi.util.view.activityBinding
import eu.kanade.tachiyomi.util.view.collapse
import eu.kanade.tachiyomi.util.view.doOnApplyWindowInsetsCompat
import eu.kanade.tachiyomi.util.view.expand
import eu.kanade.tachiyomi.util.view.isExpanded
import eu.kanade.tachiyomi.util.view.popupMenu
import eu.kanade.tachiyomi.util.view.setMessage
import eu.kanade.tachiyomi.util.view.setNegativeButton
import eu.kanade.tachiyomi.util.view.setPositiveButton
import eu.kanade.tachiyomi.util.view.setText
import eu.kanade.tachiyomi.util.view.setTitle
import eu.kanade.tachiyomi.util.view.smoothScrollToTop
import eu.kanade.tachiyomi.util.view.withFadeTransaction
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import uy.kohesive.injekt.injectLazy
import yokai.domain.base.BasePreferences
import yokai.domain.base.BasePreferences.ExtensionInstaller
import yokai.i18n.MR
import yokai.util.lang.getString
import android.R as AR
// NOVEL -->
import hayai.novel.plugin.NovelPluginManager
import hayai.novel.plugin.model.NovelPluginIndex
import hayai.novel.ui.NovelPluginAdapter
import hayai.novel.ui.NovelPluginGroupItem
import hayai.novel.ui.NovelPluginItem
// NOVEL <--

class ExtensionBottomSheet @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) :
    LinearLayout(context, attrs),
    ExtensionAdapter.OnButtonClickListener,
    FlexibleAdapter.OnItemClickListener,
    FlexibleAdapter.OnItemLongClickListener,
    SourceAdapter.OnAllClickListener,
    BaseMigrationInterface,
    // NOVEL -->
    NovelPluginAdapter.OnButtonClickListener {
    // NOVEL <--

    private val basePreferences: BasePreferences by injectLazy()

    var sheetBehavior: BottomSheetBehavior<*>? = null

    var shouldCallApi = false

    /**
     * Adapter containing the list of extensions
     */
    private var extAdapter: ExtensionAdapter? = null
    private var migAdapter: FlexibleAdapter<IFlexible<*>>? = null
    // NOVEL -->
    private var novelPluginAdapter: NovelPluginAdapter? = null
    // NOVEL <--

    val adapters
        // NOVEL -->
        get() = listOf(extAdapter, novelPluginAdapter, migAdapter)
        // NOVEL <--

    val presenter = ExtensionBottomPresenter()
    var currentSourceTitle: String? = null

    private var extensions: List<ExtensionItem> = emptyList()
    // NOVEL -->
    private var novelPlugins: List<NovelPluginItem> = emptyList()
    // NOVEL <--
    var canExpand = false
    private lateinit var binding: ExtensionsBottomSheetBinding

    lateinit var controller: BrowseController
    var boundViews = arrayListOf<RecyclerWithScrollerView>()

    val extensionFrameLayout: RecyclerWithScrollerView?
        get() = binding.pager.findViewWithTag("TabbedRecycler0") as? RecyclerWithScrollerView
    // NOVEL -->
    val novelPluginFrameLayout: RecyclerWithScrollerView?
        get() = binding.pager.findViewWithTag("TabbedRecycler1") as? RecyclerWithScrollerView
    // NOVEL <--
    val migrationFrameLayout: RecyclerWithScrollerView?
        get() = binding.pager.findViewWithTag("TabbedRecycler2") as? RecyclerWithScrollerView

    var isExpanding = false

    override fun onFinishInflate() {
        super.onFinishInflate()
        binding = ExtensionsBottomSheetBinding.bind(this)
    }

    fun onCreate(controller: BrowseController) {
        // Initialize adapter, scroll listener and recycler views
        presenter.attachView(this)
        extAdapter = ExtensionAdapter(this)
        extAdapter?.stateRestorationPolicy = RecyclerView.Adapter.StateRestorationPolicy.PREVENT_WHEN_EMPTY
        if (migAdapter == null) {
            migAdapter = SourceAdapter(this)
        }
        migAdapter?.stateRestorationPolicy = RecyclerView.Adapter.StateRestorationPolicy.PREVENT_WHEN_EMPTY
        // NOVEL -->
        if (novelPluginAdapter == null) {
            novelPluginAdapter = NovelPluginAdapter(this)
        }
        novelPluginAdapter?.stateRestorationPolicy = RecyclerView.Adapter.StateRestorationPolicy.PREVENT_WHEN_EMPTY
        // NOVEL <--
        sheetBehavior = BottomSheetBehavior.from(this)
        // Create recycler and set adapter.

        binding.pager.adapter = TabbedSheetAdapter()
        binding.tabs.setupWithViewPager(binding.pager)
        this.controller = controller
        binding.pager.doOnApplyWindowInsetsCompat { _, insets, _ ->
            val bottomBar = controller.activityBinding?.bottomNav
            val bottomH = bottomBar?.height ?: insets.getInsets(systemBars()).bottom
            extensionFrameLayout?.binding?.recycler?.updatePaddingRelative(bottom = bottomH)
            migrationFrameLayout?.binding?.recycler?.updatePaddingRelative(bottom = bottomH)
            // NOVEL -->
            novelPluginFrameLayout?.binding?.recycler?.updatePaddingRelative(bottom = bottomH)
            // NOVEL <--
        }
        binding.tabs.addOnTabSelectedListener(
            object : TabLayout.OnTabSelectedListener {
                override fun onTabSelected(tab: TabLayout.Tab?) {
                    isExpanding = !sheetBehavior.isExpanded()
                    if (canExpand) {
                        this@ExtensionBottomSheet.sheetBehavior?.expand()
                    }
                    this@ExtensionBottomSheet.controller.updateTitleAndMenu()
                    getFrameLayoutForTab(tab?.position)?.binding?.recycler?.isNestedScrollingEnabled = true
                    getFrameLayoutForTab(tab?.position)?.binding?.recycler?.requestLayout()
                    sheetBehavior?.isDraggable = true
                    // NOVEL -->
                    if (tab?.position == 1) {
                        presenter.refreshNovelPlugins()
                    }
                    // NOVEL <--
                }

                override fun onTabUnselected(tab: TabLayout.Tab?) {
                    getFrameLayoutForTab(tab?.position)?.binding?.recycler?.isNestedScrollingEnabled = false
                    if (tab?.position == 2) {
                        presenter.deselectSource()
                    }
                }

                override fun onTabReselected(tab: TabLayout.Tab?) {
                    isExpanding = !sheetBehavior.isExpanded()
                    this@ExtensionBottomSheet.sheetBehavior?.expand()
                    getFrameLayoutForTab(tab?.position)?.binding?.recycler?.isNestedScrollingEnabled = true
                    sheetBehavior?.isDraggable = true
                    if (tab?.position == 1) {
                        presenter.refreshNovelPlugins()
                    }
                    if (!isExpanding) {
                        getFrameLayoutForTab(tab?.position)?.binding?.recycler?.smoothScrollToTop()
                    }
                }
            },
        )
        presenter.onCreate()
        updateExtTitle()

        binding.sheetLayout.setOnClickListener {
            if (!sheetBehavior.isExpanded()) {
                sheetBehavior?.expand()
                fetchOnlineExtensionsIfNeeded()
            } else {
                sheetBehavior?.collapse()
            }
        }
        presenter.getExtensionUpdateCount()
    }

    fun isOnView(view: View): Boolean {
        return "TabbedRecycler${binding.pager.currentItem}" == view.tag
    }

    fun updatedNestedRecyclers() {
        listOf(extensionFrameLayout, novelPluginFrameLayout, migrationFrameLayout).forEachIndexed { index, recyclerWithScrollerBinding ->
            recyclerWithScrollerBinding?.binding?.recycler?.isNestedScrollingEnabled = binding.pager.currentItem == index
        }
    }

    fun fetchOnlineExtensionsIfNeeded() {
        if (shouldCallApi) {
            presenter.findAvailableExtensions()
            shouldCallApi = false
        }
    }

    fun updateExtTitle() {
        val extCount = presenter.getExtensionUpdateCount()
        if (extCount > 0) {
            binding.tabs.getTabAt(0)?.orCreateBadge
        } else {
            binding.tabs.getTabAt(0)?.removeBadge()
        }
    }

    override fun onButtonClick(position: Int) {
        val extension = (extAdapter?.getItem(position) as? ExtensionItem)?.extension ?: return
        when (extension) {
            is Extension.Installed -> {
                if (!extension.hasUpdate) {
                    openDetails(extension)
                } else {
                    presenter.updateExtension(extension)
                }
            }
            is Extension.Available -> {
                presenter.installExtension(extension)
            }
            is Extension.Untrusted -> {
                openTrustDialog(extension)
            }
        }
    }

    override fun onCancelClick(position: Int) {
        val extension = (extAdapter?.getItem(position) as? ExtensionItem) ?: return
        presenter.cancelExtensionInstall(extension)
    }

    override fun onUpdateAllClicked(position: Int) {
        (controller.activity as? MainActivity)?.showNotificationPermissionPrompt()
        if (basePreferences.extensionInstaller().get() != ExtensionInstaller.SHIZUKU &&
            !presenter.preferences.hasPromptedBeforeUpdateAll().get()
        ) {
            controller.activity!!.materialAlertDialog()
                .setTitle(MR.strings.update_all)
                .setMessage(MR.strings.some_extensions_may_prompt)
                .setPositiveButton(AR.string.ok) { _, _ ->
                    presenter.preferences.hasPromptedBeforeUpdateAll().set(true)
                    updateAllExtensions(position)
                }
                .show()
        } else {
            updateAllExtensions(position)
        }
    }

    override fun onExtSortClicked(view: TextView, position: Int) {
        view.popupMenu(
            InstalledExtensionsOrder.entries.map { it.value to it.nameRes },
            presenter.preferences.installedExtensionsOrder().get(),
        ) {
            presenter.preferences.installedExtensionsOrder().set(itemId)
            extAdapter?.installedSortOrder = itemId
            view.setText(InstalledExtensionsOrder.fromValue(itemId).nameRes)
            presenter.refreshExtensions()
        }
    }

    private fun updateAllExtensions(position: Int) {
        val header = (extAdapter?.getSectionHeader(position)) as? ExtensionGroupItem ?: return
        val items = extAdapter?.getSectionItemPositions(header)
        val extensions = items?.mapNotNull {
            val extItem = (extAdapter?.getItem(it) as? ExtensionItem) ?: return
            val extension = (extAdapter?.getItem(it) as? ExtensionItem)?.extension ?: return
            if ((extItem.installStep == null || extItem.installStep == InstallStep.Error) &&
                extension is Extension.Installed && extension.hasUpdate
            ) {
                extension
            } else {
                null
            }
        }.orEmpty()
        presenter.updateExtensions(extensions)
    }

    override fun onItemClick(view: View?, position: Int): Boolean {
        when (binding.tabs.selectedTabPosition) {
            0 -> {
                val extension =
                    (extAdapter?.getItem(position) as? ExtensionItem)?.extension ?: return false
                if (extension is Extension.Installed) {
                    openDetails(extension)
                } else if (extension is Extension.Untrusted) {
                    openTrustDialog(extension)
                }
            }
            2 -> {
                val item = migAdapter?.getItem(position) ?: return false

                if (item is MangaItem) {
                    PreMigrationController.navigateToMigration(
                        Injekt.get<PreferencesHelper>().skipPreMigration().get(),
                        controller.router,
                        listOf(item.manga.id!!),
                    )
                } else if (item is SourceItem) {
                    presenter.setSelectedSource(item.source)
                }
            }
        }
        return false
    }

    override fun onItemLongClick(position: Int) {
        when (binding.tabs.selectedTabPosition) {
            0 -> {
                val extension = (extAdapter?.getItem(position) as? ExtensionItem)?.extension ?: return
                if (extension is Extension.Installed || extension is Extension.Untrusted) {
                    uninstallExtension(extension.name, extension.pkgName)
                }
            }
            1 -> {
                val item = (novelPluginAdapter?.getItem(position) as? NovelPluginItem) ?: return
                if (item.isInstalled) {
                    uninstallNovelPlugin(item.plugin.name, item.plugin.id)
                }
            }
        }
    }

    override fun onAllClick(position: Int) {
        val item = migAdapter?.getItem(position) as? SourceItem ?: return

        val sourceMangas =
            presenter.mangaItems[item.source.id]?.mapNotNull { it.manga.id }?.toList()
                ?: emptyList()
        PreMigrationController.navigateToMigration(
            Injekt.get<PreferencesHelper>().skipPreMigration().get(),
            controller.router,
            sourceMangas,
        )
    }

    private fun openDetails(extension: Extension.Installed) {
        val controller = ExtensionDetailsController(extension.pkgName)
        this.controller.router.pushController(controller.withFadeTransaction())
    }

    private fun openTrustDialog(extension: Extension.Untrusted) {
        val activity = controller.activity ?: return
        activity.materialAlertDialog()
            .setTitle(MR.strings.untrusted_extension)
            .setMessage(MR.strings.untrusted_extension_message)
            .setPositiveButton(MR.strings.trust) { _, _ ->
                trustExtension(extension.pkgName, extension.versionCode, extension.signatureHash)
            }
            .setNegativeButton(MR.strings.uninstall) { _, _ ->
                uninstallExtension(extension.pkgName)
            }.show()
    }

    fun setExtensions(extensions: List<ExtensionItem>, updateController: Boolean = true) {
        this.extensions = extensions
        if (updateController) {
            controller.presenter.updateSources()
        }
        drawExtensions()
    }

    override fun setMigrationSources(sources: List<SourceItem>) {
        currentSourceTitle = null
        val changingAdapters = migAdapter !is SourceAdapter
        if (migAdapter !is SourceAdapter) {
            migAdapter = SourceAdapter(this)
            migrationFrameLayout?.onBind(migAdapter!!)
            migAdapter?.stateRestorationPolicy = RecyclerView.Adapter.StateRestorationPolicy.PREVENT_WHEN_EMPTY
        }
        migAdapter?.updateDataSet(sources, changingAdapters)
        controller.updateTitleAndMenu()
    }

    override fun setMigrationManga(title: String, manga: List<MangaItem>?) {
        currentSourceTitle = title
        val changingAdapters = migAdapter !is MangaAdapter
        if (migAdapter !is MangaAdapter) {
            migAdapter = MangaAdapter(this, presenter.uiPreferences.outlineOnCovers().get())
            migrationFrameLayout?.onBind(migAdapter!!)
            migAdapter?.stateRestorationPolicy = RecyclerView.Adapter.StateRestorationPolicy.PREVENT_WHEN_EMPTY
        }
        migAdapter?.updateDataSet(manga, changingAdapters)
        controller.updateTitleAndMenu()
    }

    fun drawExtensions() {
        if (controller.extQuery.isNotBlank()) {
            extAdapter?.updateDataSet(
                extensions.filter {
                    it.extension.name.contains(controller.extQuery, ignoreCase = true)
                },
            )
        } else {
            extAdapter?.updateDataSet(extensions)
        }
        updateExtTitle()
        updateExtUpdateAllButton()
        // NOVEL -->
        drawNovelPlugins()
        // NOVEL <--
    }

    fun canStillGoBack(): Boolean {
        return (binding.tabs.selectedTabPosition == 2 && migAdapter is MangaAdapter) ||
            (binding.tabs.selectedTabPosition in 0..1 && binding.sheetToolbar.hasExpandedActionView())
    }

    fun canGoBack(): Boolean {
        return if (binding.tabs.selectedTabPosition == 2 && migAdapter is MangaAdapter) {
            presenter.deselectSource()
            false
        } else if (binding.sheetToolbar.hasExpandedActionView()) {
            binding.sheetToolbar.collapseActionView()
            false
        } else {
            true
        }
    }

    fun downloadUpdate(item: ExtensionItem) {
        extAdapter?.updateItem(item, item.installStep)
        updateExtUpdateAllButton()
    }

    private fun updateExtUpdateAllButton() {
        val updateHeader =
            extAdapter?.headerItems?.find { it is ExtensionGroupItem && it.canUpdate != null } as? ExtensionGroupItem
                ?: return
        val items = extAdapter?.getSectionItemPositions(updateHeader) ?: return
        updateHeader.canUpdate = items.any {
            val extItem = (extAdapter?.getItem(it) as? ExtensionItem) ?: return
            extItem.installStep == null || extItem.installStep == InstallStep.Error
        }
        extAdapter?.updateItem(updateHeader)
    }

    private fun trustExtension(pkgName: String, versionCode: Long, signatureHash: String) {
        presenter.trustExtension(pkgName, versionCode, signatureHash)
    }

    private fun uninstallExtension(pkgName: String) {
        presenter.uninstallExtension(pkgName)
    }

    private fun uninstallExtension(extName: String, pkgName: String) {
        if (context.isPackageInstalled(pkgName)) {
            presenter.uninstallExtension(pkgName)
        } else {
            controller.activity!!.materialAlertDialog()
                .setTitle(extName)
                .setPositiveButton(MR.strings.remove) { _, _ ->
                    presenter.uninstallExtension(pkgName)
                }
                .setNegativeButton(AR.string.cancel, null)
                .show()
        }
    }

    private fun uninstallNovelPlugin(pluginName: String, pluginId: String) {
        controller.activity!!.materialAlertDialog()
            .setTitle(pluginName)
            .setPositiveButton(MR.strings.remove) { _, _ ->
                presenter.uninstallNovelPlugin(pluginId)
            }
            .setNegativeButton(AR.string.cancel, null)
            .show()
    }

    fun setCanInstallPrivately(installPrivately: Boolean) {
        extAdapter?.installPrivately = installPrivately
    }

    fun onDestroy() {
        presenter.onDestroy()
    }

    // NOVEL -->
    private fun getFrameLayoutForTab(position: Int?): RecyclerWithScrollerView? {
        return when (position) {
            0 -> extensionFrameLayout
            1 -> novelPluginFrameLayout
            2 -> migrationFrameLayout
            else -> null
        }
    }

    fun setNovelPlugins(items: List<NovelPluginItem>) {
        novelPlugins = items
        drawNovelPlugins()
    }

    private fun drawNovelPlugins() {
        if (controller.extQuery.isNotBlank()) {
            novelPluginAdapter?.updateDataSet(
                novelPlugins.filter {
                    it.plugin.name.contains(controller.extQuery, ignoreCase = true)
                },
            )
        } else {
            novelPluginAdapter?.updateDataSet(novelPlugins)
        }
    }

    override fun onNovelPluginButtonClick(position: Int) {
        val item = (novelPluginAdapter?.getItem(position) as? NovelPluginItem) ?: return
        if (!item.isInstalled || item.hasUpdate) {
            presenter.installNovelPlugin(item.plugin)
        }
    }

    override fun onNovelUpdateAllClicked(position: Int) {
        val header = novelPluginAdapter?.getSectionHeader(position) as? NovelPluginGroupItem ?: return
        if (header.canUpdate != true) return

        val items = novelPluginAdapter?.getSectionItemPositions(header).orEmpty()
        val pluginsToUpdate = items.mapNotNull { index ->
            val pluginItem = novelPluginAdapter?.getItem(index) as? NovelPluginItem ?: return@mapNotNull null
            if (pluginItem.isInstalled && pluginItem.hasUpdate) pluginItem.plugin else null
        }
        presenter.updateNovelPlugins(pluginsToUpdate)
    }

    override fun onNovelSortClicked(view: TextView, position: Int) {
        view.popupMenu(
            InstalledExtensionsOrder.entries.map { it.value to it.nameRes },
            presenter.preferences.installedExtensionsOrder().get(),
        ) {
            presenter.preferences.installedExtensionsOrder().set(itemId)
            novelPluginAdapter?.installedSortOrder = itemId
            view.setText(InstalledExtensionsOrder.fromValue(itemId).nameRes)
            presenter.refreshNovelPlugins()
        }
    }
    // NOVEL <--

    private inner class TabbedSheetAdapter : RecyclerViewPagerAdapter() {

        override fun getCount(): Int {
            // NOVEL -->
            return 3
            // NOVEL <--
        }

        override fun getPageTitle(position: Int): CharSequence {
            return when (position) {
                0 -> context.getString(MR.strings.extensions)
                // NOVEL -->
                1 -> context.getString(MR.strings.novels)
                // NOVEL <--
                else -> context.getString(MR.strings.migration)
            }
        }

        /**
         * Creates a new view for this adapter.
         *
         * @return a new view.
         */
        override fun createView(container: ViewGroup): View {
            val binding = RecyclerWithScrollerBinding.inflate(
                LayoutInflater.from(container.context),
                container,
                false,
            )
            val view: RecyclerWithScrollerView = binding.root
            val height = this@ExtensionBottomSheet.controller.activityBinding?.bottomNav?.height
                ?: view.rootWindowInsetsCompat?.getInsets(systemBars())?.bottom ?: 0
            view.setUp(this@ExtensionBottomSheet, binding, height)

            return view
        }

        /**
         * Binds a view with a position.
         *
         * @param view the view to bind.
         * @param position the position in the adapter.
         */
        override fun bindView(view: View, position: Int) {
            (view as RecyclerWithScrollerView).onBind(adapters[position]!!)
            view.setTag("TabbedRecycler$position")
            boundViews.add(view)
        }

        /**
         * Recycles a view.
         *
         * @param view the view to recycle.
         * @param position the position in the adapter.
         */
        override fun recycleView(view: View, position: Int) {
            // (view as RecyclerWithScrollerView).onRecycle()
            boundViews.remove(view)
        }

        /**
         * Returns the position of the view.
         */
        override fun getItemPosition(obj: Any): Int {
            val view = (obj as? RecyclerWithScrollerView) ?: return POSITION_NONE
            val index = adapters.indexOfFirst { it == view.binding?.recycler?.adapter }
            return if (index == -1) POSITION_NONE else index
        }
    }
}
