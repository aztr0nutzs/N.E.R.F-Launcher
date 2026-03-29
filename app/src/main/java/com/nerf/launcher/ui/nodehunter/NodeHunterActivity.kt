package com.nerf.launcher.ui.nodehunter

import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.nerf.launcher.databinding.ActivityNodeHunterBinding
import com.nerf.launcher.util.network.LocalNetworkScanner
import kotlinx.coroutines.launch

class NodeHunterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityNodeHunterBinding
    private lateinit var networkScanner: LocalNetworkScanner

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNodeHunterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        networkScanner = LocalNetworkScanner(this)
        startNetworkSweep()
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
