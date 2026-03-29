package com.example.nerflauncher

import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.widget.ProgressBar
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.nerflauncher.games.NodeHunterGameView
import com.example.nerflauncher.network.NetworkScanner
import kotlinx.coroutines.launch

class NodeActivity : AppCompatActivity() {

    private lateinit var gameView: NodeHunterGameView
    private lateinit var loadingIndicator: ProgressBar
    private lateinit var networkScanner: NetworkScanner

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Ensure you have a layout file (e.g., activity_node.xml) with:
        // 1. NodeHunterGameView (id: nodeHunterView)
        // 2. ProgressBar (id: scanProgressBar)
        setContentView(R.layout.activity_node)

        gameView = findViewById(R.id.nodeHunterView)
        loadingIndicator = findViewById(R.id.scanProgressBar)
        networkScanner = NetworkScanner(this)

        startNetworkSweep()
    }

    private fun startNetworkSweep() {
        loadingIndicator.visibility = View.VISIBLE
        gameView.visibility = View.INVISIBLE

        lifecycleScope.launch {
            // Execute the highly parallel /24 subnet ping sweep
            val activeNodes = networkScanner.scanLocalSubnet()
            
            // Switch UI state and populate the 2D canvas engine
            loadingIndicator.visibility = View.GONE
            gameView.visibility = View.VISIBLE
            gameView.populateNetworkNodes(activeNodes)
        }
    }

    /**
     * Intercept hardware events at the highest level to ensure the physical
     * tactical buttons always work, regardless of View focus.
     */
    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        // Only trigger on the ACTION_DOWN event to prevent double-firing
        if (event.action == KeyEvent.ACTION_DOWN) {
            when (event.keyCode) {
                // Map to your physical hardware's explicit keycodes. 
                // PROG_RED/YELLOW are standard Android SDK codes for colored remote/hardware keys.
                KeyEvent.KEYCODE_PROG_RED -> {
                    gameView.fireRedAction()
                    return true // Consume the event
                }
                KeyEvent.KEYCODE_PROG_YELLOW -> {
                    gameView.fireYellowAction()
                    return true // Consume the event
                }
                // Optional Fallbacks if the specific board uses volume keys instead
                KeyEvent.KEYCODE_VOLUME_UP -> {
                    gameView.fireRedAction()
                    return true
                }
                KeyEvent.KEYCODE_VOLUME_DOWN -> {
                    gameView.fireYellowAction()
                    return true
                }
            }
        }
        // If it's not a button we care about, let the system handle it
        return super.dispatchKeyEvent(event)
    }
}
