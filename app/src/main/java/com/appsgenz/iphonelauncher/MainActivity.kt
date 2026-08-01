package com.appsgenz.iphonelauncher

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView

class MainActivity : AppCompatActivity() {

    private val dockPackages = listOf(
        "com.android.dialer",
        "com.android.chrome",
        "com.google.android.apps.messaging",
        "com.android.camera2"
    )

    private lateinit var adapter: AppAdapter
    private var allApps: List<AppInfo> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        allApps = loadInstalledApps()

        val grid = findViewById<RecyclerView>(R.id.appGrid)
        grid.layoutManager = GridLayoutManager(this, 4)
        adapter = AppAdapter(allApps) { app -> launchApp(app) }
        grid.adapter = adapter

        setupDock()
        setupSearch()
    }

    override fun onResume() {
        super.onResume()
        allApps = loadInstalledApps()
        adapter.submitList(allApps)
    }

    private fun loadInstalledApps(): List<AppInfo> {
        val pm = packageManager
        val mainIntent = Intent(Intent.ACTION_MAIN, null)
        mainIntent.addCategory(Intent.CATEGORY_LAUNCHER)

        val resolved = pm.queryIntentActivities(mainIntent, PackageManager.MATCH_ALL)
        return resolved
            .map {
                AppInfo(
                    label = it.loadLabel(pm).toString(),
                    packageName = it.activityInfo.packageName,
                    icon = it.loadIcon(pm)
                )
            }
            .distinctBy { it.packageName }
            .sortedBy { it.label.lowercase() }
    }

    private fun launchApp(app: AppInfo) {
        val launchIntent = packageManager.getLaunchIntentForPackage(app.packageName)
        if (launchIntent != null) {
            startActivity(launchIntent)
        } else {
            Toast.makeText(this, "Não foi possível abrir ${app.label}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupDock() {
        val dock = findViewById<LinearLayout>(R.id.dock)
        val dockApps = allApps.filter { it.packageName in dockPackages }
        val inflater = LayoutInflater.from(this)

        for (app in dockApps) {
            val itemView = inflater.inflate(R.layout.item_app, dock, false)
            val icon = itemView.findViewById<android.widget.ImageView>(R.id.appIcon)
            val label = itemView.findViewById<android.widget.TextView>(R.id.appLabel)
            icon.setImageDrawable(app.icon)
            label.text = ""
            itemView.setOnClickListener { launchApp(app) }
            dock.addView(itemView)
        }
    }

    private fun setupSearch() {
        val searchBox = findViewById<EditText>(R.id.searchBox)
        searchBox.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val query = s?.toString()?.lowercase().orEmpty()
                val filtered = if (query.isBlank()) allApps
                else allApps.filter { it.label.lowercase().contains(query) }
                adapter.submitList(filtered)
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    override fun onBackPressed() {
        // mantém o launcher sempre em primeiro plano
    }
}
