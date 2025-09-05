package com.arvinapp.acgb

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private var filePathCallback: ValueCallback<Array<Uri>>? = null
    private val FILE_CHOOSER_REQUEST_CODE = 1

    // Modern izin isteği için launcher
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            setupWebView()
        } else {
            showPermissionDeniedDialog()
        }
    }

    // Ayarlar ekranı için launcher
    private val settingsLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        checkPermissions()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        checkPermissions()
    }

    private fun checkPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (Environment.isExternalStorageManager()) {
                setupWebView()
            } else {
                requestStoragePermission()
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.READ_EXTERNAL_STORAGE
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                setupWebView()
            } else {
                requestStoragePermission()
            }
        } else {
            setupWebView()
        }
    }

    private fun requestStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            showAndroid11PermissionDialog()
        } else {
            requestPermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
    }

    private fun showAndroid11PermissionDialog() {
        AlertDialog.Builder(this)
            .setTitle("Dosya Erişim İzni Gerekli")
            .setMessage("Dosyaları açmak ve görüntülemek için tüm dosyalara erişim izni vermeniz gerekiyor.")
            .setPositiveButton("Ayarlara Git") { dialog, _ ->
                dialog.dismiss()
                openStorageSettings()
            }
            .setNegativeButton("İptal") { dialog, _ ->
                dialog.dismiss()
                showPermissionDeniedDialog()
            }
            .setCancelable(false)
            .show()
    }

    private fun openStorageSettings() {
        try {
            val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
            intent.data = Uri.parse("package:$packageName")
            settingsLauncher.launch(intent)
        } catch (e: Exception) {
            try {
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                intent.data = Uri.parse("package:$packageName")
                settingsLauncher.launch(intent)
            } catch (e2: Exception) {
                Toast.makeText(this, "Ayarlar açılamadı", Toast.LENGTH_SHORT).show()
                showPermissionDeniedDialog()
            }
        }
    }

    private fun showPermissionDeniedDialog() {
        AlertDialog.Builder(this)
            .setTitle("İzin Reddedildi")
            .setMessage("Dosya erişim iznini reddettiniz. Dosya açma özelliği çalışmayacaktır.")
            .setPositiveButton("Tamam") { dialog, _ ->
                dialog.dismiss()
                setupWebViewWithLimitedAccess()
            }
            .setNegativeButton("Tekrar Dene") { dialog, _ ->
                dialog.dismiss()
                checkPermissions()
            }
            .setCancelable(false)
            .show()
    }

    private fun setupWebView() {
        initializeWebView(true)
    }

    private fun setupWebViewWithLimitedAccess() {
        initializeWebView(false)
    }

    private fun initializeWebView(hasStoragePermission: Boolean) {
        webView = WebView(this).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.allowFileAccess = hasStoragePermission
            settings.allowContentAccess = hasStoragePermission
            
            // JavaScript ile Android arasındaki iletişim için
            addJavascriptInterface(WebAppInterface(this@MainActivity), "Android")
            
            webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                    // Uygulama içinde kal, harici linkleri engelle
                    return if (url?.startsWith("http") == true) {
                        // Harici linkleri WebView'de aç
                        false
                    } else {
                        // Diğer tüm linkleri uygulama içinde işle
                        true
                    }
                }
            }
            
            webChromeClient = object : WebChromeClient() {
                override fun onShowFileChooser(
                    webView: WebView?,
                    callback: ValueCallback<Array<Uri>>?,
                    fileChooserParams: FileChooserParams?
                ): Boolean {
                    if (!hasStoragePermission) {
                        runOnUiThread {
                            Toast.makeText(
                                this@MainActivity,
                                "Dosya erişim izni gerekli",
                                Toast.LENGTH_LONG
                            ).show()
                            openStorageSettings()
                        }
                        callback?.onReceiveValue(null)
                        return false
                    }

                    filePathCallback?.onReceiveValue(null)
                    filePathCallback = callback

                    val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
                        addCategory(Intent.CATEGORY_OPENABLE)
                        type = "*/*"
                    }

                    try {
                        startActivityForResult(
                            Intent.createChooser(intent, "Dosya Seçin"),
                            FILE_CHOOSER_REQUEST_CODE
                        )
                    } catch (e: ActivityNotFoundException) {
                        filePathCallback = null
                        Toast.makeText(
                            this@MainActivity,
                            "Dosya yöneticisi bulunamadı",
                            Toast.LENGTH_SHORT
                        ).show()
                        return false
                    }
                    return true
                }
            }
        }

        setContentView(webView)
        webView.loadUrl("file:///android_asset/index.html")
    }

    // JavaScript ile iletişim için class - DOSYA AÇMA ÖZELLİĞİ
    class WebAppInterface(private val activity: MainActivity) {
        @android.webkit.JavascriptInterface
        fun openFile(filePath: String) {
            activity.runOnUiThread {
                activity.openFileInWebView(filePath)
            }
        }
        
        @android.webkit.JavascriptInterface
        fun getFileContent(filePath: String): String {
            return activity.getFileContentAsString(filePath)
        }
    }

    // Dosya içeriğini string olarak oku
    private fun getFileContentAsString(filePath: String): String {
        return try {
            val uri = Uri.parse(filePath)
            val inputStream = contentResolver.openInputStream(uri)
            val content = inputStream?.bufferedReader().use { it?.readText() } ?: ""
            content
        } catch (e: Exception) {
            "Dosya okunamadı: ${e.message}"
        }
    }

    // Dosyayı WebView'de göster
    private fun openFileInWebView(filePath: String) {
        try {
            val uri = Uri.parse(filePath)
            val fileExtension = filePath.substringAfterLast('.', "").lowercase()
            
            when (fileExtension) {
                "pdf" -> showPdfInWebView(uri)
                "ppt", "pptx" -> showOfficeFileInWebView(uri, "presentation")
                "doc", "docx" -> showOfficeFileInWebView(uri, "document")
                "xls", "xlsx" -> showOfficeFileInWebView(uri, "spreadsheet")
                "txt", "html", "htm" -> showTextFileInWebView(uri)
                "jpg", "jpeg", "png", "gif", "bmp" -> showImageInWebView(uri)
                else -> showUnsupportedFileMessage(fileExtension)
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Dosya açılamadı: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showPdfInWebView(uri: Uri) {
        // PDF için Google Docs viewer kullan
        val pdfUrl = "https://docs.google.com/gview?embedded=true&url=$uri"
        webView.loadUrl(pdfUrl)
    }

    private fun showOfficeFileInWebView(uri: Uri, fileType: String) {
        // Office dosyaları için Google Docs viewer kullan
        val officeUrl = "https://docs.google.com/gview?embedded=true&url=$uri"
        webView.loadUrl(officeUrl)
    }

    private fun showTextFileInWebView(uri: Uri) {
        try {
            val content = getFileContentAsString(uri.toString())
            val htmlContent = """
                <!DOCTYPE html>
                <html>
                <head>
                    <meta charset="utf-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1">
                    <style>
                        body { 
                            font-family: Arial, sans-serif; 
                            padding: 20px; 
                            background: white;
                            color: black;
                            line-height: 1.6;
                        }
                        .container {
                            max-width: 800px;
                            margin: 0 auto;
                        }
                        pre { 
                            white-space: pre-wrap; 
                            word-wrap: break-word; 
                            background: #f8f9fa;
                            padding: 20px;
                            border-radius: 8px;
                            border: 1px solid #dee2e6;
                        }
                        .file-info {
                            background: #007bff;
                            color: white;
                            padding: 10px 15px;
                            border-radius: 5px;
                            margin-bottom: 15px;
                            display: inline-block;
                        }
                    </style>
                </head>
                <body>
                    <div class="container">
                        <div class="file-info">📝 Metin Belgesi</div>
                        <pre>${content.replace("<", "&lt;").replace(">", "&gt;")}</pre>
                    </div>
                </body>
                </html>
            """.trimIndent()
            
            webView.loadDataWithBaseURL(null, htmlContent, "text/html", "UTF-8", null)
        } catch (e: Exception) {
            Toast.makeText(this, "Metin dosyası okunamadı", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showImageInWebView(uri: Uri) {
        val htmlContent = """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="utf-8">
                <meta name="viewport" content="width=device-width, initial-scale=1">
                <style>
                    body { 
                        margin: 0; 
                        padding: 20px; 
                        background: white;
                        display: flex;
                        justify-content: center;
                        align-items: center;
                        min-height: 100vh;
                        flex-direction: column;
                    }
                    .file-info {
                        background: #28a745;
                        color: white;
                        padding: 10px 15px;
                        border-radius: 5px;
                        margin-bottom: 15px;
                    }
                    img { 
                        max-width: 100%; 
                        height: auto; 
                        border: 1px solid #ddd;
                        border-radius: 8px;
                        box-shadow: 0 2px 10px rgba(0,0,0,0.1);
                    }
                </style>
            </head>
            <body>
                <div class="file-info">🖼️ Resim Dosyası</div>
                <img src="$uri" alt="Image">
            </body>
            </html>
        """.trimIndent()
        
        webView.loadDataWithBaseURL(null, htmlContent, "text/html", "UTF-8", null)
    }

    private fun showUnsupportedFileMessage(fileExtension: String) {
        val htmlContent = """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="utf-8">
                <meta name="viewport" content="width=device-width, initial-scale=1">
                <style>
                    body { 
                        font-family: Arial, sans-serif; 
                        padding: 40px; 
                        background: white;
                        color: black;
                        text-align: center;
                    }
                    .container {
                        max-width: 500px;
                        margin: 0 auto;
                    }
                    .message { 
                        background: #f8f9fa; 
                        padding: 30px; 
                        border-radius: 12px; 
                        border: 1px solid #dee2e6;
                        box-shadow: 0 4px 15px rgba(0,0,0,0.1);
                    }
                    .icon {
                        font-size: 48px;
                        margin-bottom: 15px;
                    }
                    h3 {
                        color: #dc3545;
                        margin-bottom: 15px;
                    }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="message">
                        <div class="icon">❌</div>
                        <h3>Desteklenmeyen Dosya Türü</h3>
                        <p><strong>.$fileExtension</strong> dosyalarını görüntüleyemiyoruz.</p>
                        <p>Bu dosyayı açmak için harici bir uygulama kullanmanız gerekebilir.</p>
                    </div>
                </div>
            </body>
            </html>
        """.trimIndent()
        
        webView.loadDataWithBaseURL(null, htmlContent, "text/html", "UTF-8", null)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        
        if (requestCode == FILE_CHOOSER_REQUEST_CODE) {
            val results = when {
                resultCode == RESULT_OK && data != null -> {
                    val uri = data.data
                    if (uri != null) {
                        // Seçilen dosyayı hemen aç
                        openFileInWebView(uri.toString())
                        arrayOf(uri)
                    } else {
                        null
                    }
                }
                else -> null
            }
            filePathCallback?.onReceiveValue(results)
            filePathCallback = null
        }
    }

    override fun onBackPressed() {
        if (::webView.isInitialized && webView.canGoBack()) {
            webView.goBack()
        } else {
            super.onBackPressed()
        }
    }

    override fun onDestroy() {
        filePathCallback?.onReceiveValue(null)
        filePathCallback = null
        super.onDestroy()
    }
}
