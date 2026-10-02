package com.anicharging.animator.ui

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.anicharging.animator.battery.BatteryHelper
import com.anicharging.animator.databinding.ActivityMainBinding
import com.anicharging.animator.service.OverlayService
import com.anicharging.animator.settings.BatteryBarDesign
import com.anicharging.animator.settings.PositionPreset
import com.anicharging.animator.settings.SettingsRepository
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var settingsRepository: SettingsRepository

    private val categories = listOf("sharingan", "mangekyou", "tenseigan", "rinnegan", "rasengan", "rasenshuriken", "other")
    private val positions = PositionPreset.entries.map { it.name }
    private val barDesigns = BatteryBarDesign.entries.map { it.name }
    private val durations = listOf("10 Seconds", "20 Seconds", "30 Seconds", "Until Unplugged")

    private val handler = Handler(Looper.getMainLooper())
    private val previewAnimRunnable = object : Runnable {
        override fun run() {
            binding.previewOverlayView.advanceFrame()
            handler.postDelayed(this, 50L)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        settingsRepository = SettingsRepository(this)

        setupSpinners()
        setupListeners()
        observeSettings()

        binding.previewOverlayView.updateBatteryInfo(BatteryHelper.getBatteryInfo(this))
        handler.post(previewAnimRunnable)
    }

    private fun setupSpinners() {
        binding.spinnerCategory.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, categories)
        binding.spinnerPosition.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, positions)
        binding.spinnerBatteryBar.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, barDesigns)
        binding.spinnerDuration.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, durations)
    }

    private fun setupListeners() {
        binding.spinnerCategory.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p0: AdapterView<*>?, p1: View?, pos: Int, id: Long) {
                lifecycleScope.launch { settingsRepository.updateAnimationCategory(categories[pos]) }
            }
            override fun onNothingSelected(p0: AdapterView<*>?) {}
        }

        binding.spinnerPosition.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p0: AdapterView<*>?, p1: View?, pos: Int, id: Long) {
                lifecycleScope.launch { settingsRepository.updatePositionPreset(PositionPreset.valueOf(positions[pos])) }
            }
            override fun onNothingSelected(p0: AdapterView<*>?) {}
        }

        binding.spinnerBatteryBar.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p0: AdapterView<*>?, p1: View?, pos: Int, id: Long) {
                lifecycleScope.launch { settingsRepository.updateBatteryBarDesign(BatteryBarDesign.valueOf(barDesigns[pos])) }
            }
            override fun onNothingSelected(p0: AdapterView<*>?) {}
        }

        binding.spinnerDuration.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p0: AdapterView<*>?, p1: View?, pos: Int, id: Long) {
                val sec = when (pos) {
                    0 -> 10
                    1 -> 20
                    2 -> 30
                    else -> 0
                }
                lifecycleScope.launch { settingsRepository.updateDisplayDuration(sec) }
            }
            override fun onNothingSelected(p0: AdapterView<*>?) {}
        }

        binding.switchPerformance.setOnCheckedChangeListener { _, isChecked ->
            lifecycleScope.launch { settingsRepository.updatePerformanceMode(isChecked) }
        }

        binding.btnOverlayPermission.setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
                val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))
                startActivity(intent)
            } else {
                Toast.makeText(this, "Overlay Permission Already Granted", Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnTestOverlay.setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
                Toast.makeText(this, "Please grant Overlay Permission first", Toast.LENGTH_SHORT).show()
            } else {
                val overlayIntent = Intent(this, OverlayService::class.java).apply {
                    action = OverlayService.ACTION_SHOW_OVERLAY
                }
                startService(overlayIntent)
            }
        }
    }

    private fun observeSettings() {
        lifecycleScope.launch {
            settingsRepository.settingsFlow.collectLatest { settings ->
                binding.previewOverlayView.updateSettings(settings)

                val catIndex = categories.indexOf(settings.animationCategory).coerceAtLeast(0)
                if (binding.spinnerCategory.selectedItemPosition != catIndex) {
                    binding.spinnerCategory.setSelection(catIndex)
                }

                val posIndex = positions.indexOf(settings.positionPreset.name).coerceAtLeast(0)
                if (binding.spinnerPosition.selectedItemPosition != posIndex) {
                    binding.spinnerPosition.setSelection(posIndex)
                }

                val barIndex = barDesigns.indexOf(settings.batteryBarDesign.name).coerceAtLeast(0)
                if (binding.spinnerBatteryBar.selectedItemPosition != barIndex) {
                    binding.spinnerBatteryBar.setSelection(barIndex)
                }

                binding.switchPerformance.isChecked = settings.performanceMode
            }
        }
    }

    override fun onDestroy() {
        handler.removeCallbacks(previewAnimRunnable)
        super.onDestroy()
    }
}
