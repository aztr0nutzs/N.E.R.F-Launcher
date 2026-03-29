package com.example.nerflauncher

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.nerflauncher.ai.SarcasticAiAssistant
import com.example.nerflauncher.ui.ReactorModuleView

class ReactorActivity : AppCompatActivity() {

    private lateinit var leftReactor: ReactorModuleView
    private lateinit var rightReactor: ReactorModuleView
    private lateinit var aiAssistant: SarcasticAiAssistant

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_reactors)

        leftReactor = findViewById(R.id.leftReactor)
        rightReactor = findViewById(R.id.rightReactor)
        
        // Initialize the AI engine (which automatically parses the JSON)
        aiAssistant = SarcasticAiAssistant(this)

        setupLeftReactor()
        setupRightReactor()
    }

    private fun setupLeftReactor() {
        // Left Reactor: Network and Systems Focus
        leftReactor.onCoreTapped = {
            aiAssistant.speakCategory(SarcasticAiAssistant.Category.NETWORK_SCAN)
            showToast("N-Core: Initializing Subnet Sweep...")
            // TODO: Execute your NetworkScanner.kt logic here
        }

        leftReactor.onSectorTapped = { sector ->
            when (sector) {
                ReactorModuleView.Sector.TOP -> {
                    aiAssistant.speakCustom("Accessing System Net Tools. Try not to break the firewall.")
                }
                ReactorModuleView.Sector.RIGHT -> {
                    aiAssistant.speakCustom("Packet Sniffer engaged. This ought to be boring.")
                }
                ReactorModuleView.Sector.BOTTOM -> {
                    aiAssistant.speakCategory(SarcasticAiAssistant.Category.DIAGNOSTICS)
                }
                ReactorModuleView.Sector.LEFT -> {
                    aiAssistant.speakCustom("Targeting configuration opened. Awaiting parameters.")
                }
            }
        }
    }

    private fun setupRightReactor() {
        // Right Reactor: AI and Diagnostics Focus
        rightReactor.onCoreTapped = {
            aiAssistant.speakCategory(SarcasticAiAssistant.Category.WAKE)
            showToast("AI Assistant Online")
        }

        rightReactor.onSectorTapped = { sector ->
            when (sector) {
                ReactorModuleView.Sector.TOP -> {
                    aiAssistant.speakCategory(SarcasticAiAssistant.Category.RANDOM_SNARK)
                }
                ReactorModuleView.Sector.RIGHT -> {
                    aiAssistant.speakCustom("Voice modulation is currently unnecessary. My voice is already perfect.")
                }
                ReactorModuleView.Sector.BOTTOM -> {
                    aiAssistant.speakCustom("Media controls linked. I swear, if you play synth-wave again...")
                }
                ReactorModuleView.Sector.LEFT -> {
                    aiAssistant.speakCategory(SarcasticAiAssistant.Category.ERROR)
                }
            }
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    override fun onDestroy() {
        aiAssistant.shutdown()
        super.onDestroy()
    }
}
