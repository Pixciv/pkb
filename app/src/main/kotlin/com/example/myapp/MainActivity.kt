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
import java.net.URLEncoder

class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private var filePathCallback: ValueCallback<Array<Uri>>? = null
    private val FILE_CHOOSER_REQUEST_CODE = 1
    private val PERMISSION_REQUEST_CODE = 2

    // LibreOffice Viewer URL
    private val LIBREOFFICE_VIEWER_URL = "https://viewer.documentfoundation.org"

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
            
            // JavaScript iletişimi için
            addJavascriptInterface(WebAppInterface(this@MainActivity), "Android")
            
            webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    injectJavaScriptInterface()
                }
                
                override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                    if (url?.startsWith("file://") == true) {
                        openFileInWebView(url)
                        return true
                    }
                    return false
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

    // JavaScript enjeksiyonu
    private fun injectJavaScriptInterface() {
        val jsCode = """
            <script>
            function setupFileClickListeners() {
                var fileElements = document.querySelectorAll('.file-item, [data-file-path]');
                fileElements.forEach(function(element) {
                    element.addEventListener('click', function(e) {
                        e.preventDefault();
                        var filePath = this.getAttribute('data-file-path');
                        if (filePath && typeof Android !== 'undefined') {
                            Android.openFile(filePath);
                        }
                    });
                });
            }
            document.addEventListener('DOMContentLoaded', setupFileClickListeners);
            setInterval(setupFileClickListeners, 1000);
            </script>
        """.trimIndent()
        
        webView.loadUrl("javascript:(function() { " +
            "var script = document.createElement('script');" +
            "script.innerHTML = `$jsCode`;" +
            "document.head.appendChild(script);" +
            "})()")
    }

    // JavaScript iletişim interface'i
    class WebAppInterface(private val activity: MainActivity) {
        @android.webkit.JavascriptInterface
        fun openFile(filePath: String) {
            activity.runOnUiThread {
                activity.openFileInWebView(filePath)
            }
        }
    }

    // ANA DOSYA AÇMA FONKSİYONU - LIBREOFFICE ENTEGRASYONLU
    fun openFileInWebView(filePath: String) {
        try {
            val decodedPath = java.net.URLDecoder.decode(filePath, "UTF-8")
            val uri = Uri.parse(decodedPath)
            val fileExtension = getFileExtension(uri)

            when (fileExtension) {
                "pdf" -> showWithLibreOffice(uri)
                "doc", "docx" -> showWithLibreOffice(uri)
                "xls", "xlsx" -> showWithLibreOffice(uri)
                "ppt", "pptx" -> showWithLibreOffice(uri)
                "txt", "html", "htm" -> showTextFile(uri)
                "jpg", "jpeg", "png", "gif", "bmp" -> showImage(uri)
                else -> showWithLibreOffice(uri)
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Dosya açılamadı", Toast.LENGTH_SHORT).show()
        }
    }

    // LIBREOFFICE VIEWER İLE GÖSTER
    private fun showWithLibreOffice(uri: Uri) {
        try {
            val encodedUrl = URLEncoder.encode(uri.toString(), "UTF-8")
            val libreOfficeUrl = "$LIBREOFFICE_VIEWER_URL/?url=$encodedUrl"
            
            webView.loadUrl(libreOfficeUrl)
            
        } catch (e: Exception) {
            // Fallback: harici uygulamada aç
            openWithExternalApp(uri)
        }
    }

    // METİN DOSYALARI İÇİN
    private fun showTextFile(uri: Uri) {
        try {
            val content = getFileContent(uri)
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
                        }
                        pre { 
                            white-space: pre-wrap; 
                            word-wrap: break-word; 
                        }
                    </style>
                </head>
                <body>
                    <pre>${content.replace("<", "&lt;").replace(">", "&gt;")}</pre>
                </body>
                </html>
            """.trimIndent()
            
            webView.loadDataWithBaseURL(null, htmlContent, "text/html", "UTF-8", null)
        } catch (e: Exception) {
            showWithLibreOffice(uri)
        }
    }

    // RESİM DOSYALARI İÇİN
    private fun showImage(uri: Uri) {
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
                    }
                    img { 
                        max-width: 100%; 
                        height: auto; 
                        border: 1px solid #ddd;
                        border-radius: 4px;
                    }
                </style>
            </head>
            <body>
                <img src="$uri" alt="Image">
            </body>
            </html>
        """.trimIndent()
        
        webView.loadDataWithBaseURL(null, htmlContent, "text/html", "UTF-8", null)
    }

    // HARİCİ UYGULAMA FALLBACK
    private fun openWithExternalApp(uri: Uri) {
        try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, getMimeType(uri))
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(Intent.createChooser(intent, "Dosyayı aç"))
        } catch (e: Exception) {
            Toast.makeText(this, "Dosya açılamadı", Toast.LENGTH_SHORT).show()
        }
    }

    private fun getFileExtension(uri: Uri): String {
        val path = uri.toString()
        return path.substringAfterLast('.', "").lowercase()
    }

    private fun getMimeType(uri: Uri): String {
        return when (getFileExtension(uri)) {
            "pdf" -> "application/pdf"
            "doc" -> "application/msword"
            "docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
            "xls" -> "application/vnd.ms-excel"
            "xlsx" -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
            "ppt" -> "application/vnd.ms-powerpoint"
            "pptx" -> "application/vnd.openxmlformats-officedocument.presentationml.presentation"
            else -> "*/*"
        }
    }

    private fun getFileContent(uri: Uri): String {
        return try {
            val inputStream = contentResolver.openInputStream(uri)
            inputStream?.bufferedReader()?.use { it.readText() } ?: ""
        } catch (e: Exception) {
            ""
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        
        if (requestCode == FILE_CHOOSER_REQUEST_CODE) {
            val results = when {
                resultCode == RESULT_OK && data != null -> {
                    val uri = data.data
                    if (uri != null) {
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
