package com.nerf.launcher.ui.nodehunter

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.nerf.launcher.R
import com.nerf.launcher.databinding.ActivityNodeHunterBinding
import com.nerf.launcher.util.network.LocalNetworkScanner
import kotlinx.coroutines.launch

class NodeHunterModuleActivity : AppCompatActivity() {

    companion object {
        private const val EXTRA_LAUNCH_SOURCE = "node_hunter_launch_source"
        const val SOURCE_REACTOR = "reactor"
        const val SOURCE_ASSISTANT = "assistant"

        fun createIntent(context: Context, launchSource: String): Intent {
            return Intent(context, NodeHunterModuleActivity::class.java).apply {
                putExtra(EXTRA_LAUNCH_SOURCE, launchSource)
            }
        }
    }

    private lateinit var binding: ActivityNodeHunterBinding
    private lateinit var networkScanner: LocalNetworkScanner

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNodeHunterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        networkScanner = LocalNetworkScanner(this)
        bindLauncherChrome()
        startNetworkSweep()
    }

    private fun bindLauncherChrome() {
        binding.nodeHunterBackButton.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
        binding.nodeHunterSourceLabel.text = getString(
            when (intent.getStringExtra(EXTRA_LAUNCH_SOURCE)) {
                SOURCE_ASSISTANT -> R.string.node_hunter_source_assistant
                else -> R.string.node_hunter_source_reactor
            }
        )
        binding.scanDetailsText.text = getString(R.string.node_hunter_scan_details)
        binding.nodeHunterFooterText.text = getString(R.string.node_hunter_footer_note)
    }

    private fun startNetworkSweep() {
        binding.scanOverlay.visibility = View.VISIBLE
        binding.scanProgressBar.visibility = View.VISIBLE
        binding.nodeHunterView.visibility = View.INVISIBLE

        lifecycleScope.launch {
            val activeNodes = networkScanner.scanLocalSubnet()
            binding.scanOverlay.visibility = View.GONE
            binding.scanProgressBar.visibility = View.GONE
            binding.nodeHunterView.visibility = View.VISIBLE
            binding.nodeHunterView.populateNetworkNodes(activeNodes)
            binding.nodeHunterStatusValue.text = getString(
                if (activeNodes.isEmpty()) {
                    R.string.node_hunter_status_no_nodes
                } else {
                    R.string.node_hunter_status_nodes_detected
                },
                activeNodes.size
            )
        }
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.action == KeyEvent.ACTION_DOWN) {
            when (event.keyCode) {
                KeyEvent.KEYCODE_PROG_RED, KeyEvent.KEYCODE_VOLUME_UP -> {
                    binding.nodeHunterView.fireRedAction()
                    return true
                }

                KeyEvent.KEYCODE_PROG_YELLOW, KeyEvent.KEYCODE_VOLUME_DOWN -> {
                    binding.nodeHunterView.fireYellowAction()
                    return true
                }
            }
        }

        return super.dispatchKeyEvent(event)
    }
}
