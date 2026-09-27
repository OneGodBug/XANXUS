package im.angry.openeuicc.ui

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.annotation.StringRes
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import im.angry.openeuicc.common.R
import im.angry.openeuicc.core.EuiccChannel
import im.angry.openeuicc.core.EuiccChannelManager
import im.angry.openeuicc.util.*
import kotlinx.coroutines.launch

class EuiccInfoActivity : BaseEuiccAccessActivity(), OpenEuiccContextMarker {

    companion object {
        private val YES_NO = Pair(
            R.string.euicc_info_yes,
            R.string.euicc_info_no
        )
    }

    private lateinit var swipeRefresh: SwipeRefreshLayout
    private lateinit var infoList: RecyclerView

    private var logicalSlotId: Int = -1
    private var seId: EuiccChannel.SecureElementId =
        EuiccChannel.SecureElementId.DEFAULT

    data class Item(
        @get:StringRes
        val titleResId: Int,
        val content: String?,
        val copiedToastResId: Int? = null,
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_euicc_info)
        setSupportActionBar(requireViewById(R.id.toolbar))
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)

        swipeRefresh = requireViewById(R.id.swipe_refresh)

        infoList = requireViewById<RecyclerView>(R.id.recycler_view).also {
            it.layoutManager =
                LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)

            it.addItemDecoration(
                DividerItemDecoration(
                    this,
                    LinearLayoutManager.VERTICAL
                )
            )

            it.adapter = EuiccInfoAdapter()
        }

        logicalSlotId = intent.getIntExtra("logicalSlotId", 0)

        seId = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(
                "seId",
                EuiccChannel.SecureElementId::class.java
            )
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra("seId")
        } ?: EuiccChannel.SecureElementId.DEFAULT

        setChannelTitle(
            if (logicalSlotId == EuiccChannelManager.USB_CHANNEL_ID) {
                getString(R.string.channel_name_format_usb)
            } else {
                appContainer.customizableTextProvider
                    .formatNonUsbChannelName(logicalSlotId)
            }
        )

        swipeRefresh.setOnRefreshListener {
            refresh()
        }

        setupRootViewSystemBarInsets(
            window.decorView.rootView,
            arrayOf(
                this::activityToolbarInsetHandler,
                mainViewPaddingInsetHandler(infoList)
            )
        )
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean =
        when (item.itemId) {
            android.R.id.home -> {
                finish()
                true
            }

            else -> super.onOptionsItemSelected(item)
        }

    private fun setChannelTitle(title: CharSequence) {
        super.setTitle(
            getString(
                R.string.euicc_info_activity_title,
                title
            )
        )
    }

    override fun onInit() {
        refresh()
    }

    /**
     * Demo 模式：
     *
     * 不访问真实 eUICC。
     * 不调用 euiccChannelManager。
     * 不读取 APDU。
     * 不读取 EuiccInfo2。
     *
     * 直接显示模拟数据。
     */
    private fun refresh() {
        swipeRefresh.isRefreshing = true

        lifecycleScope.launch {
            val items = buildDemoEuiccInfoItems()

            (infoList.adapter!! as EuiccInfoAdapter)
                .euiccInfoItems = items

            swipeRefresh.isRefreshing = false
        }
    }

    /**
     * 虚拟 eUICC 数据。
     *
     * 这些数据只用于界面展示，不代表真实设备。
     */
    private fun buildDemoEuiccInfoItems() = buildList {

        add(
            Item(
                R.string.euicc_info_access_mode,
                "Demo"
            )
        )

        add(
            Item(
                R.string.euicc_info_removable,
                getString(R.string.euicc_info_yes)
            )
        )

        add(
            Item(
                R.string.euicc_info_eid,
                "89033012345678901234567890123456",
                copiedToastResId = R.string.toast_eid_copied
            )
        )

        add(
            Item(
                R.string.euicc_info_isdr_aid,
                "A0000005591010FFFFFFFF8900000100"
            )
        )

        add(
            Item(
                R.string.euicc_info_sku,
                "Demo eUICC"
            )
        )

        add(
            Item(
                R.string.euicc_info_sn,
                "DEMO123456789",
                copiedToastResId = R.string.toast_sn_copied
            )
        )

        add(
            Item(
                R.string.euicc_info_fw_ver,
                "4.6.0"
            )
        )

        add(
            Item(
                R.string.euicc_info_sgp22_version,
                "2.3.0"
            )
        )

        add(
            Item(
                R.string.euicc_info_sas_accreditation_number,
                "GS-AC-UP-0000"
            )
        )

        add(
            Item(
                R.string.euicc_info_free_nvram,
                "291666 KB"
            )
        )

        add(
            Item(
                R.string.euicc_info_ci_type,
                getString(R.string.euicc_info_ci_unknown)
            )
        )

        add(
            Item(
                R.string.euicc_info_atr,
                "3B9F96801FC7A0230A",
                copiedToastResId = R.string.toast_atr_copied
            )
        )
    }

    inner class EuiccInfoViewHolder(root: View) : ViewHolder(root) {

        private val title: TextView =
            root.requireViewById(R.id.euicc_info_title)

        private val content: TextView =
            root.requireViewById(R.id.euicc_info_content)

        private var copiedToastResId: Int? = null

        init {
            root.setOnClickListener {

                if (copiedToastResId != null) {

                    val label = title.text.toString()

                    getSystemService(
                        ClipboardManager::class.java
                    )!!
                        .setPrimaryClip(
                            ClipData.newPlainText(
                                label,
                                content.text
                            )
                        )

                    if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.S_V2) {
                        Toast.makeText(
                            this@EuiccInfoActivity,
                            copiedToastResId!!,
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        }

        fun bind(item: Item) {
            copiedToastResId = item.copiedToastResId

            title.setText(item.titleResId)

            content.text =
                item.content
                    ?: getString(R.string.euicc_info_unknown)
        }
    }

    inner class EuiccInfoAdapter :
        RecyclerView.Adapter<EuiccInfoViewHolder>() {

        var euiccInfoItems: List<Item> = listOf()

            @SuppressLint("NotifyDataSetChanged")
            set(newVal) {
                field = newVal
                notifyDataSetChanged()
            }

        override fun onCreateViewHolder(
            parent: ViewGroup,
            viewType: Int
        ): EuiccInfoViewHolder {

            val root = LayoutInflater
                .from(parent.context)
                .inflate(
                    R.layout.euicc_info_item,
                    parent,
                    false
                )

            return EuiccInfoViewHolder(root)
        }

        override fun getItemCount(): Int =
            euiccInfoItems.size

        override fun onBindViewHolder(
            holder: EuiccInfoViewHolder,
            position: Int
        ) {
            holder.bind(
                euiccInfoItems[position]
            )
        }
    }
}
