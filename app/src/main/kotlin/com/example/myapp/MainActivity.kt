package com.arvinapp.acgb

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: FileAdapter
    private val REQUEST_CODE_SELECT_FILE = 1001

    private val allFiles = mutableListOf<FileItem>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // WebView + RecyclerView layout
        setContentView(R.layout.activity_main)
        webView = findViewById(R.id.webView)
        recyclerView = findViewById(R.id.recyclerView)

        // WebView setup
        webView.apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            webViewClient = WebViewClient()
            webChromeClient = WebChromeClient()
            addJavascriptInterface(WebAppInterface(), "Android")
            loadUrl("file:///android_asset/index.html")
        }

        // RecyclerView setup
        adapter = FileAdapter(allFiles) { fileItem ->
            // Tıklanınca dosya açma işlemi (PDF, DOCX vs.)
            openFile(fileItem)
        }
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter
    }

    // Geri tuşu WebView'de gezinme için
    override fun onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack()
        } else {
            super.onBackPressed()
        }
    }

    // Dosya açma (PDF/Word/Excel/PPT)
    private fun openFile(fileItem: FileItem) {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(fileItem.uri, getMimeType(fileItem.type))
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        startActivity(intent)
    }

    private fun getMimeType(extension: String): String {
        return when (extension.lowercase(Locale.getDefault())) {
            "pdf" -> "application/pdf"
            "doc", "docx" -> "application/msword"
            "xls", "xlsx" -> "application/vnd.ms-excel"
            "ppt", "pptx" -> "application/vnd.ms-powerpoint"
            else -> "*/*"
        }
    }

    // JS → Android köprüsü
    inner class WebAppInterface {
        @JavascriptInterface
        fun openFilePicker() {
            runOnUiThread {
                val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                    addCategory(Intent.CATEGORY_OPENABLE)
                    type = "*/*"
                    putExtra(Intent.EXTRA_MIME_TYPES, arrayOf(
                        "application/pdf",
                        "application/msword",
                        "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                        "application/vnd.ms-excel",
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                        "application/vnd.ms-powerpoint",
                        "application/vnd.openxmlformats-officedocument.presentationml.presentation"
                    ))
                }
                startActivityForResult(intent, REQUEST_CODE_SELECT_FILE)
            }
        }
    }

    // Dosya seçimi sonucu
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_SELECT_FILE && resultCode == Activity.RESULT_OK) {
            data?.data?.let { uri ->
                val fileName = getFileName(uri)
                val fileType = getFileExtension(fileName)
                val fileSize = getFileSize(uri)

                val newFile = FileItem(
                    name = fileName,
                    type = fileType,
                    size = fileSize,
                    date = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date()),
                    isFavorite = false,
                    uri = uri
                )
                allFiles.add(newFile)
                adapter.notifyDataSetChanged()
            }
        }
    }

    // Dosya bilgileri
    private fun getFileName(uri: Uri): String {
        var name = "unknown"
        contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (cursor.moveToFirst()) {
                name = cursor.getString(nameIndex)
            }
        }
        return name
    }

    private fun getFileSize(uri: Uri): String {
        var size = 0L
        contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
            if (cursor.moveToFirst()) {
                size = cursor.getLong(sizeIndex)
            }
        }
        return "${size / 1024} kB"
    }

    private fun getFileExtension(fileName: String): String {
        return fileName.substringAfterLast('.', "").lowercase(Locale.getDefault())
    }
}}
