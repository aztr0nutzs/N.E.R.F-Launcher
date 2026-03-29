package com.nerf.launcher.ui.reactor

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.nerf.launcher.R
import com.nerf.launcher.databinding.ActivityReactorDiagnosticsBinding
import com.nerf.launcher.util.assistant.ReactorAssistant

class ReactorDiagnosticsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityReactorDiagnosticsBinding
    private lateinit var assistant: ReactorAssistant

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityReactorDiagnosticsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        assistant = ReactorAssistant(this)

        setupLeftReactor()
        setupRightReactor()
    }

    private fun setupLeftReactor() {
        binding.leftReactor.onCoreTapped = {
            assistant.speakCategory(ReactorAssistant.Category.NETWORK_SCAN)
            showToast(getString(R.string.reactor_subnet_sweep_start))
        }

        binding.leftReactor.onSectorTapped = { sector ->
            when (sector) {
                ReactorModuleView.Sector.TOP ->
                    assistant.speakCustom("Accessing System Net Tools. Try not to break the firewall.")
                ReactorModuleView.Sector.RIGHT ->
                    assistant.speakCustom("Packet sniffer engaged. This ought to be boring.")
                ReactorModuleView.Sector.BOTTOM ->
                    assistant.speakCategory(ReactorAssistant.Category.DIAGNOSTICS)
                ReactorModuleView.Sector.LEFT ->
                    assistant.speakCustom("Targeting configuration opened. Awaiting parameters.")
            }
        }
    }

    private fun setupRightReactor() {
        binding.rightReactor.onCoreTapped = {
            assistant.speakCategory(ReactorAssistant.Category.WAKE)
            showToast(getString(R.string.reactor_assistant_online))
        }

        binding.rightReactor.onSectorTapped = { sector ->
            when (sector) {
                ReactorModuleView.Sector.TOP ->
                    assistant.speakCategory(ReactorAssistant.Category.RANDOM_SNARK)
                ReactorModuleView.Sector.RIGHT ->
                    assistant.speakCustom("Voice modulation is currently unnecessary. My voice is already perfect.")
                ReactorModuleView.Sector.BOTTOM ->
                    assistant.speakCustom("Media controls linked. I swear, if you play synth-wave again...")
                ReactorModuleView.Sector.LEFT ->
                    assistant.speakCategory(ReactorAssistant.Category.ERROR)
            }
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    override fun onDestroy() {
        assistant.shutdown()
        super.onDestroy()
    }
}
