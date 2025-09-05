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
        // Ayarlardan dönüldüğünde izinleri tekrar kontrol et
        checkPermissions()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        checkPermissions()
    }

    private fun checkPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Android 11+ için MANAGE_EXTERNAL_STORAGE veya dosya erişim izni kontrolü
            if (Environment.isExternalStorageManager()) {
                setupWebView()
            } else {
                requestStoragePermission()
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // Android 6.0-10 için READ_EXTERNAL_STORAGE izni
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
            // Android 6.0 altı için izin gerekmez
            setupWebView()
        }
    }

    private fun requestStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Android 11+ için tüm dosyalara erişim izni iste
            showAndroid11PermissionDialog()
        } else {
            // Android 6.0-10 için runtime izin iste
            requestPermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
    }

    private fun showAndroid11PermissionDialog() {
        AlertDialog.Builder(this)
            .setTitle("Dosya Erişim İzni Gerekli")
            .setMessage("Dosyaları içe aktarmak için tüm dosyalara erişim izni vermeniz gerekiyor. 'Ayarlara Git' butonuna tıklayıp 'Tüm dosyalara erişim izni ver' seçeneğini etkinleştirin.")
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
            // Android 11+ için doğru ayar ekranını aç
            val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
            intent.data = Uri.parse("package:$packageName")
            settingsLauncher.launch(intent)
        } catch (e: Exception) {
            try {
                // Fallback: genel ayarlar ekranı
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
            .setMessage("Dosya erişim iznini reddettiniz. Uygulamayı sınırlı modda kullanabilirsiniz, ancak dosya içe aktarma özelliği çalışmayacaktır.")
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
            
            webViewClient = WebViewClient()
            
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
                                "Dosya erişim izni gerekli. Lütfen ayarlardan izin verin.",
                                Toast.LENGTH_LONG
                            ).show()
                            // İzin yoksa kullanıcıyı ayarlara yönlendir
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

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        
        if (requestCode == FILE_CHOOSER_REQUEST_CODE) {
            val results = when {
                resultCode == RESULT_OK && data != null -> {
                    val uri = data.data
                    if (uri != null) arrayOf(uri) else null
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
