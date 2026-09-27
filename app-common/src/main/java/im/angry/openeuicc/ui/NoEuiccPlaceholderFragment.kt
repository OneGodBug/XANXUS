package im.angry.openeuicc.ui

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import im.angry.openeuicc.common.R
import im.angry.openeuicc.util.OpenEuiccContextMarker

class NoEuiccPlaceholderFragment : Fragment(), OpenEuiccContextMarker {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 允许本 Fragment 向右上角菜单添加项目
        setHasOptionsMenu(true)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(
            R.layout.fragment_no_euicc_placeholder,
            container,
            false
        )

        val textView =
            view.requireViewById<TextView>(R.id.no_euicc_placeholder)

        textView.text = appContainer.customizableTextProvider.noEuiccExplanation

        return view
    }

    override fun onCreateOptionsMenu(
        menu: Menu,
        inflater: MenuInflater
    ) {
        super.onCreateOptionsMenu(menu, inflater)

        // 加载 eUICC 页面使用的菜单
        inflater.inflate(R.menu.fragment_euicc, menu)
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        super.onPrepareOptionsMenu(menu)

        // 模拟 eUICC 环境：
        // 只显示 eSIM Info
        menu.findItem(R.id.show_notifications)?.isVisible = false
        menu.findItem(R.id.euicc_info)?.isVisible = true
        menu.findItem(R.id.euicc_memory_reset)?.isVisible = false
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {

            R.id.euicc_info -> {
                // 打开我们之前制作的模拟 eUICC Info 页面
                startActivity(
                    Intent(
                        requireContext(),
                        EuiccInfoActivity::class.java
                    )
                )
                true
            }

            else -> super.onOptionsItemSelected(item)
        }
    }
}
