package com.example.app

import android.app.Activity
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.OpenableColumns
import android.view.MenuItem
import android.view.View
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.widget.PopupMenu
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.tabs.TabLayout
import java.io.File
import java.io.FileOutputStream
import java.util.*

class MainActivity : AppCompatActivity(), FileAdapter.Listener {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: FileAdapter
    private val files = mutableListOf<FileItem>()
    private lateinit var prefs: SharedPreferences
    private lateinit var drawer: DrawerLayout
    private lateinit var searchBar: View
    private lateinit var searchInput: EditText
    private lateinit var selectionHeader: View
    private lateinit var selectionCount: TextView

    private val importLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        uri?.let { importUriToApp(it) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        prefs = getSharedPreferences("ankd_prefs", MODE_PRIVATE)
        drawer = findViewById(R.id.drawer_layout)

        // Toolbar
        val toolbar: androidx.appcompat.widget.Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.title = "All PDF Reader"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { drawer.open() }

        // Search
        searchBar = findViewById(R.id.search_bar)
        searchInput = findViewById(R.id.search_input)

        selectionHeader = findViewById(R.id.selection_header)
        selectionCount = findViewById(R.id.selection_count)

        // Tabs
        val tabLayout: TabLayout = findViewById(R.id.tab_layout)
        tabLayout.addTab(tabLayout.newTab().setText("Tümü").setTag("all"))
        tabLayout.addTab(tabLayout.newTab().setText("PDF").setTag("pdf"))
        tabLayout.addTab(tabLayout.newTab().setText("Word").setTag("word"))
        tabLayout.addTab(tabLayout.newTab().setText("Excel").setTag("excel"))
        tabLayout.addTab(tabLayout.newTab().setText("PPT").setTag("ppt"))
        tabLayout.addOnTabSelectedListener(object: TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) { renderCurrentContent() }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) { renderCurrentContent() }
        })

        // RecyclerView
        recyclerView = findViewById(R.id.recycler_view)
        adapter = FileAdapter(files, this)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        // Bottom nav (menu defined in res/menu)
        val bottomNav: com.google.android.material.bottomnavigation.BottomNavigationView = findViewById(R.id.bottom_nav)
        bottomNav.setOnItemSelectedListener { item ->
            when(item.itemId) {
                R.id.nav_all -> { renderCurrentContent(); true }
                R.id.nav_recent -> { renderCurrentContent(); true }
                R.id.nav_fav -> { renderCurrentContent(); true }
                R.id.nav_tools -> { renderCurrentContent(); true }
                else -> false
            }
        }

        // Add/import
        findViewById<View>(R.id.add_button).setOnClickListener {
            // Open system file picker
            importLauncher.launch(arrayOf("*/*"))
        }

        // Drawer: dark mode switch
        val darkSwitch: Switch = findViewById(R.id.dark_mode_switch)
        darkSwitch.isChecked = prefs.getBoolean("dark_mode", false)
        applyDarkMode(darkSwitch.isChecked)
        darkSwitch.setOnCheckedChangeListener { _, checked ->
            prefs.edit().putBoolean("dark_mode", checked).apply()
            applyDarkMode(checked)
        }

        // Load saved files from prefs (simple JSON string could be used)
        loadFilesFromPrefs()
        // Also prefill with assets/examples folder if empty
        if (files.isEmpty()) {
            loadExamplesFromAssets()
        }
        renderCurrentContent()
    }

    private fun applyDarkMode(enabled: Boolean) {
        AppCompatDelegate.setDefaultNightMode(if (enabled) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO)
    }

    private fun loadExamplesFromAssets() {
        try {
            val assetFiles = assets.list("examples") ?: return
            for (name in assetFiles) {
                // copy asset to internal cache for open intents
                val inStream = assets.open("examples/$name")
                val outFile = File(cacheDir, name)
                val fos = FileOutputStream(outFile)
                inStream.copyTo(fos)
                inStream.close()
                fos.close()

                val ext = name.substringAfterLast('.', "").lowercase(Locale.getDefault())
                val stat = outFile.length()
                files.add(FileItem(name, outFile.absolutePath, ext, stat, outFile.lastModified(), false))
            }
            adapter.updateList(files)
            saveFilesToPrefs()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // Open file using Intent
    override fun onOpen(item: FileItem) {
        val file = File(item.path)
        val uri = androidx.core.content.FileProvider.getUriForFile(this, "${packageName}.provider", file)
        val intent = Intent(Intent.ACTION_VIEW)
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        // set mime type basic
        val mime = when(item.type) {
            "pdf" -> "application/pdf"
            "doc", "docx" -> "application/msword"
            "ppt", "pptx" -> "application/vnd.ms-powerpoint"
            "xls", "xlsx" -> "application/vnd.ms-excel"
            else -> "*/*"
        }
        intent.setDataAndType(uri, mime)
        startActivity(Intent.createChooser(intent, "Dosyayı aç"))
    }

    override fun onToggleFavorite(item: FileItem) { item.isFavorite = !item.isFavorite; saveFilesToPrefs(); renderCurrentContent() }
    override fun onRename(item: FileItem) { /* show dialog */ showRenameDialog(item) }
    override fun onDelete(item: FileItem) { files.remove(item); saveFilesToPrefs(); renderCurrentContent() }
    override fun onLongPress(item: FileItem) { /* selection mode could be implemented */ }
    override fun onMenu(item: FileItem, anchor: View) {
        val popup = PopupMenu(this, anchor)
        popup.menu.add("Favorilere Ekle").setOnMenuItemClickListener { onToggleFavorite(item); true }
        popup.menu.add("Yeniden Adlandır").setOnMenuItemClickListener { onRename(item); true }
        popup.menu.add("Sil").setOnMenuItemClickListener { onDelete(item); true }
        popup.menu.add("Paylaş").setOnMenuItemClickListener { shareFile(item); true }
        popup.show()
    }

    private fun shareFile(item: FileItem) {
        val file = File(item.path)
        val uri = androidx.core.content.FileProvider.getUriForFile(this, "${packageName}.provider", file)
        val intent = Intent(Intent.ACTION_SEND)
        intent.type = "*/*"
        intent.putExtra(Intent.EXTRA_STREAM, uri)
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        startActivity(Intent.createChooser(intent, "Paylaş"))
    }

    private fun showRenameDialog(item: FileItem) {
        val et = EditText(this)
        et.setText(item.name)
        android.app.AlertDialog.Builder(this)
            .setTitle("Yeniden adlandır")
            .setView(et)
            .setPositiveButton("Tamam") { _, _ ->
                item.name = et.text.toString()
                saveFilesToPrefs()
                renderCurrentContent()
            }
            .setNegativeButton("İptal", null)
            .show()
    }

    private fun importUriToApp(uri: Uri) {
        try {
            contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                cursor.moveToFirst()
                val name = if (nameIndex >= 0) cursor.getString(nameIndex) else "imported"
                val size = if (sizeIndex >= 0) cursor.getLong(sizeIndex) else 0L
                // copy to cache
                val inStream = contentResolver.openInputStream(uri)
                val outFile = File(cacheDir, name)
                val fos = FileOutputStream(outFile)
                inStream?.copyTo(fos)
                inStream?.close()
                fos.close()
                val ext = name.substringAfterLast('.', "").lowercase(Locale.getDefault())
                val item = FileItem(name, outFile.absolutePath, ext, size, outFile.lastModified(), false)
                files.add(0, item)
                saveFilesToPrefs()
                renderCurrentContent()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Dosya içe aktarılamadı", Toast.LENGTH_SHORT).show()
        }
    }

    private fun renderCurrentContent() {
        // Filter based on selected tab & bottom nav. For demo simple show all.
        adapter.updateList(files)
    }

    private fun saveFilesToPrefs() {
        // For simplicity save as JSON string
        val json = kotlinx.serialization.json.Json.encodeToString(
            kotlinx.serialization.builtins.ListSerializer(FileItem.serializer()), files)
        prefs.edit().putString("files_json", json).apply()
    }

    private fun loadFilesFromPrefs() {
        try {
            val json = prefs.getString("files_json", null) ?: return
            val list = kotlinx.serialization.json.Json.decodeFromString(
                kotlinx.serialization.builtins.ListSerializer(FileItem.serializer()), json)
            files.clear()
            files.addAll(list)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
