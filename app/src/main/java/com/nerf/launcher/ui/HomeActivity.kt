package com.nerf.launcher.ui

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import com.nerf.launcher.adapter.AppListAdapter
import com.nerf.launcher.databinding.ActivityHomeBinding
import com.nerf.launcher.util.AppUtils
import com.nerf.launcher.viewmodel.HomeViewModel

class HomeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHomeBinding

    private val viewModel: HomeViewModel by viewModels()

    private lateinit var adapter: AppListAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupRecyclerView()
        setupButtons()
        observeViewModel()

        viewModel.loadApps()
    }

    private fun setupRecyclerView() {
        adapter = AppListAdapter(emptyList()) { app ->
            AppUtils.launchApp(this, app)
        }
        binding.recyclerView.apply {
            layoutManager = GridLayoutManager(this@HomeActivity, 4)
            adapter = this@HomeActivity.adapter
        }
    }

    private fun setupButtons() {
        binding.buttonHomeSettings.setOnClickListener {
            AppUtils.openDefaultHomeSettings(this)
        }
    }

    private fun observeViewModel() {
        viewModel.apps.observe(this) { apps ->
            adapter.submitList(apps)
        }
    }

    override fun onBackPressed() {
        // As a launcher, ignore back to prevent exiting to previous home
    }
}