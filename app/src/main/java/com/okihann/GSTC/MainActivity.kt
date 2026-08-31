package com.okihann.GSTC

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.RenderEffect
import android.graphics.Shader
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.toColorInt

class MainActivity : AppCompatActivity() {

    private var gameLibraryList = mutableListOf<String>()
    private var isTrackingActive = false
    private var currentTab = R.id.btn_nav_main

    private val profileOptions = arrayOf(
        "MAX PERFORMANCE (100°C)",
        "UNLOCKED / GAMING (80°C)",
        "CPU INTENSE (90°C)",
        "BALANCED (70°C)",
        "BATTERY SAVER (55°C)"
    )
    private var targetProfile = "MAX PERFORMANCE (100°C)"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        loadGameLibrary()
        applyLiquidGlassEffect()

        val navLibrary = findViewById<LinearLayout>(R.id.btn_nav_library)
        val navMain = findViewById<LinearLayout>(R.id.btn_nav_main)
        val navSettings = findViewById<LinearLayout>(R.id.btn_nav_settings)

        loadMenuContainer(R.layout.menu_main_dashboard)
        updateTabUI()

        navLibrary.setOnClickListener { 
            currentTab = R.id.btn_nav_library
            loadMenuContainer(R.layout.menu_game_library) 
            updateTabUI()
        }
        navMain.setOnClickListener { 
            currentTab = R.id.btn_nav_main
            loadMenuContainer(R.layout.menu_main_dashboard) 
            updateTabUI()
        }
        navSettings.setOnClickListener { 
            currentTab = R.id.btn_nav_settings
            loadMenuContainer(R.layout.menu_settings_panel) 
            updateTabUI()
        }
    }

    private fun applyLiquidGlassEffect() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val bottomNav = findViewById<LinearLayout>(R.id.bottom_navigation_bar)
            bottomNav.setRenderEffect(RenderEffect.createBlurEffect(40f, 40f, Shader.TileMode.MIRROR))
        }
    }

    private fun updateTabUI() {
        val txtLibrary = findViewById<TextView>(R.id.text_nav_library)
        val txtMain = findViewById<TextView>(R.id.text_nav_main)
        val txtSettings = findViewById<TextView>(R.id.text_nav_settings)

        txtLibrary.setTextColor("#8E8E93".toColorInt())
        txtMain.setTextColor("#8E8E93".toColorInt())
        txtSettings.setTextColor("#8E8E93".toColorInt())

        when (currentTab) {
            R.id.btn_nav_library -> txtLibrary.setTextColor("#0A84FF".toColorInt())
            R.id.btn_nav_main -> txtMain.setTextColor("#0A84FF".toColorInt())
            R.id.btn_nav_settings -> txtSettings.setTextColor("#0A84FF".toColorInt())
        }
    }

    private fun loadMenuContainer(layoutResId: Int) {
        val container = findViewById<FrameLayout>(R.id.fragment_container)
        container.removeAllViews()
        val view = layoutInflater.inflate(layoutResId, container, false)
        container.addView(view)

        when (layoutResId) {
            R.layout.menu_main_dashboard -> setupMainDashboardControls(view)
            R.layout.menu_game_library -> setupLibraryControls(view)
        }
    }

    private fun setupMainDashboardControls(view: View) {
        val btnStart = view.findViewById<Button>(R.id.btn_toggle_profile)
        val spinner = view.findViewById<Spinner>(R.id.profile_dropdown)

        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, profileOptions)
        spinner.adapter = adapter
        spinner.setSelection(profileOptions.indexOf(targetProfile))

        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, v: View?, position: Int, id: Long) {
                if (!isTrackingActive) {
                    targetProfile = profileOptions[position]
                } else {
                    spinner.setSelection(profileOptions.indexOf(targetProfile))
                    Toast.makeText(this@MainActivity, "Cannot change profile while running!", Toast.LENGTH_SHORT).show()
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        updateButtonUI(btnStart)

        btnStart.setOnClickListener {
            if (!isTrackingActive) {
                isTrackingActive = true
                updateButtonUI(btnStart)
                SessionLogger.clearSession()
                ThermalController.setProfile(targetProfile)
                
                startService(Intent(this, OverlayService::class.java))
                Toast.makeText(this, "Profiling Started.", Toast.LENGTH_SHORT).show()
            } else {
                isTrackingActive = false
                updateButtonUI(btnStart)
                ThermalController.setProfile("BALANCED (70°C)")
                stopService(Intent(this, OverlayService::class.java))
                SessionLogger.finishSession(gameLibraryList)
                Toast.makeText(this, "Session saved to Documents!", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun updateButtonUI(btn: Button) {
        if (isTrackingActive) {
            btn.text = "Stop & Save Logs"
            btn.setBackgroundColor("#FF453A".toColorInt()) 
        } else {
            btn.text = "Start Profiling"
            btn.setBackgroundColor("#0A84FF".toColorInt()) 
        }
    }

    private fun setupLibraryControls(view: View) {
        val inputPackage = view.findViewById<EditText>(R.id.input_package_name)
        val btnAdd = view.findViewById<Button>(R.id.btn_add_game)
        
        refreshLibraryUI(view)

        btnAdd.setOnClickListener {
            val pkg = inputPackage.text.toString().trim()
            if (pkg.isNotEmpty() && !gameLibraryList.contains(pkg)) {
                gameLibraryList.add(pkg)
                saveGameLibrary()
                inputPackage.text.clear()
                refreshLibraryUI(view)
            }
        }
    }

    private fun refreshLibraryUI(view: View) {
        val container = view.findViewById<LinearLayout>(R.id.game_list_container)
        container.removeAllViews()

        for (game in gameLibraryList) {
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                setPadding(0, 24, 0, 24)
            }

            val pkgText = TextView(this).apply {
                text = game
                setTextColor(Color.WHITE)
                textSize = 16f
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }

            val btnDelete = Button(this).apply {
                text = "Remove"
                setTextColor("#FF453A".toColorInt())
                setBackgroundColor(Color.TRANSPARENT)
                setOnClickListener {
                    gameLibraryList.remove(game)
                    saveGameLibrary()
                    refreshLibraryUI(view)
                }
            }

            row.addView(pkgText)
            row.addView(btnDelete)
            container.addView(row)
        }
    }

    private fun saveGameLibrary() {
        val prefs = getSharedPreferences("GSTC_PREFS", Context.MODE_PRIVATE)
        prefs.edit().putString("GAME_LIST", gameLibraryList.joinToString(",")).apply()
    }

    private fun loadGameLibrary() {
        val prefs = getSharedPreferences("GSTC_PREFS", Context.MODE_PRIVATE)
        val str = prefs.getString("GAME_LIST", "com.kurogame.wutheringwaves.global") ?: ""
        gameLibraryList = if (str.isEmpty()) mutableListOf() else str.split(",").toMutableList()
    }
}