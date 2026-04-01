package com.nerf.launcher.ui.reactor

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.nerf.launcher.R
import com.nerf.launcher.databinding.ActivityReactorDiagnosticsBinding
import com.nerf.launcher.util.assistant.AiResponseRepository
import com.nerf.launcher.util.assistant.AssistantController
import com.nerf.launcher.util.assistant.AssistantSessionManager

class ReactorDiagnosticsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityReactorDiagnosticsBinding
    private lateinit var assistantController: AssistantController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityReactorDiagnosticsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        assistantController = AssistantSessionManager.acquire(this)
        assistantController.setActiveSurface("diagnostics")

        setupLeftReactor()
        setupRightReactor()
    }

    private fun setupLeftReactor() {
        binding.leftReactor.onCoreTapped = {
            assistantController.speakCategory(AiResponseRepository.Category.NETWORK_SCAN)
            showToast(getString(R.string.reactor_subnet_sweep_start))
        }

        binding.leftReactor.onSectorTapped = { sector ->
            when (sector) {
                ReactorModuleView.Sector.TOP ->
                    assistantController.speakCustom("Accessing System Net Tools. Try not to break the firewall.")
                ReactorModuleView.Sector.RIGHT ->
                    assistantController.speakCustom("Packet sniffer engaged. This ought to be boring.")
                ReactorModuleView.Sector.BOTTOM ->
                    assistantController.speakCategory(AiResponseRepository.Category.DIAGNOSTICS)
                ReactorModuleView.Sector.LEFT ->
                    assistantController.speakCustom("Targeting configuration opened. Awaiting parameters.")
            }
        }
    }

    private fun setupRightReactor() {
        binding.rightReactor.onCoreTapped = {
            assistantController.speakCategory(AiResponseRepository.Category.WAKE)
            showToast(getString(R.string.reactor_assistant_online))
        }

        binding.rightReactor.onSectorTapped = { sector ->
            when (sector) {
                ReactorModuleView.Sector.TOP ->
                    assistantController.speakCategory(AiResponseRepository.Category.RANDOM_SNARK)
                ReactorModuleView.Sector.RIGHT ->
                    assistantController.speakCustom("Voice modulation is currently unnecessary. My voice is already perfect.")
                ReactorModuleView.Sector.BOTTOM ->
                    assistantController.speakCustom("Media controls linked. I swear, if you play synth-wave again...")
                ReactorModuleView.Sector.LEFT ->
                    assistantController.speakCategory(AiResponseRepository.Category.ERROR)
            }
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    override fun onDestroy() {
        AssistantSessionManager.release(assistantController)
        super.onDestroy()
    }
}
