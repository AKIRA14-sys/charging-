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

    private var activeCategory: String? = null
    private var activePosition: String? = null
    private var activeBarDesign: String? = null
    private var activeDurationSec: Int? = null

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
                if (pos in categories.indices) {
                    val selected = categories[pos]
                    if (selected != activeCategory) {
                        activeCategory = selected
                        lifecycleScope.launch { settingsRepository.updateAnimationCategory(selected) }
                    }
                }
            }
            override fun onNothingSelected(p0: AdapterView<*>?) {}
        }

        binding.spinnerPosition.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p0: AdapterView<*>?, p1: View?, pos: Int, id: Long) {
                if (pos in positions.indices) {
                    val selected = positions[pos]
                    if (selected != activePosition) {
                        activePosition = selected
                        lifecycleScope.launch { settingsRepository.updatePositionPreset(PositionPreset.valueOf(selected)) }
                    }
                }
            }
            override fun onNothingSelected(p0: AdapterView<*>?) {}
        }

        binding.spinnerBatteryBar.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p0: AdapterView<*>?, p1: View?, pos: Int, id: Long) {
                if (pos in barDesigns.indices) {
                    val selected = barDesigns[pos]
                    if (selected != activeBarDesign) {
                        activeBarDesign = selected
                        lifecycleScope.launch { settingsRepository.updateBatteryBarDesign(BatteryBarDesign.valueOf(selected)) }
                    }
                }
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
                if (sec != activeDurationSec) {
                    activeDurationSec = sec
                    lifecycleScope.launch { settingsRepository.updateDisplayDuration(sec) }
                }
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
                try {
                    val overlayIntent = Intent(this, OverlayService::class.java).apply {
                        action = OverlayService.ACTION_SHOW_OVERLAY
                    }
                    startService(overlayIntent)
                } catch (e: Exception) {
                    Toast.makeText(this, "Unable to start overlay service", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun observeSettings() {
        lifecycleScope.launch {
            settingsRepository.settingsFlow.collectLatest { settings ->
                binding.previewOverlayView.updateSettings(settings)

                activeCategory = settings.animationCategory
                val catIndex = categories.indexOf(settings.animationCategory)
                if (catIndex >= 0 && binding.spinnerCategory.selectedItemPosition != catIndex) {
                    binding.spinnerCategory.setSelection(catIndex, false)
                }

                activePosition = settings.positionPreset.name
                val posIndex = positions.indexOf(settings.positionPreset.name)
                if (posIndex >= 0 && binding.spinnerPosition.selectedItemPosition != posIndex) {
                    binding.spinnerPosition.setSelection(posIndex, false)
                }

                activeBarDesign = settings.batteryBarDesign.name
                val barIndex = barDesigns.indexOf(settings.batteryBarDesign.name)
                if (barIndex >= 0 && binding.spinnerBatteryBar.selectedItemPosition != barIndex) {
                    binding.spinnerBatteryBar.setSelection(barIndex, false)
                }

                activeDurationSec = settings.displayDurationSeconds

                if (binding.switchPerformance.isChecked != settings.performanceMode) {
                    binding.switchPerformance.isChecked = settings.performanceMode
                }
            }
        }
    }

    override fun onDestroy() {
        handler.removeCallbacks(previewAnimRunnable)
        super.onDestroy()
    }
}
