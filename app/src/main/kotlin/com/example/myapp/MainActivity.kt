package com.arvinapp.acgb

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
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
            // İzin verildi, WebView'i başlat
            setupWebView()
        } else {
            // İzin reddedildi, kullanıcıya bilgi ver
            showPermissionDeniedDialog()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Önce tema ve layout'u ayarla (beyaz ekran gözükmesin)
        setContentView(R.layout.activity_main) // Eğer layout dosyanız yoksa, sonraki satırda oluşturacağız
        
        // İzinleri kontrol et
        checkPermissions()
    }

    private fun checkPermissions() {
        // Android sürümüne göre izin kontrolü
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // Android 6.0+ için runtime izin iste
            when {
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.READ_EXTERNAL_STORAGE
                ) == PackageManager.PERMISSION_GRANTED -> {
                    // İzin zaten verilmiş
                    setupWebView()
                }
                shouldShowRequestPermissionRationale(Manifest.permission.READ_EXTERNAL_STORAGE) -> {
                    // Kullanıcı daha önce reddetmiş, açıklama göster
                    showPermissionRationale()
                }
                else -> {
                    // İlk defa izin iste
                    requestStoragePermission()
                }
            }
        } else {
            // Android 6.0 altı için izin gerekmez
            setupWebView()
        }
    }

    private fun requestStoragePermission() {
        // Modern dialog ile izin iste
        requestPermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
    }

    private fun showPermissionRationale() {
        AlertDialog.Builder(this)
            .setTitle("Dosya Erişim İzni Gerekli")
            .setMessage("Dosyaları içe aktarmak ve uygulamanın düzgün çalışması için dosya erişim iznine ihtiyacımız var. İzin vermezseniz, dosya içe aktarma özelliği çalışmayacaktır.")
            .setPositiveButton("İzin Ver") { dialog, _ ->
                dialog.dismiss()
                requestStoragePermission()
            }
            .setNegativeButton("Reddet") { dialog, _ ->
                dialog.dismiss()
                // Sınırlı erişimle devam et
                setupWebViewWithLimitedAccess()
            }
            .setCancelable(false)
            .show()
    }

    private fun showPermissionDeniedDialog() {
        AlertDialog.Builder(this)
            .setTitle("İzin Reddedildi")
            .setMessage("Dosya erişim iznini reddettiniz. Uygulamayı sınırlı modda kullanabilirsiniz, ancak dosya içe aktarma özelliği çalışmayacaktır. Ayarlardan daha sonra izin verebilirsiniz.")
            .setPositiveButton("Tamam") { dialog, _ ->
                dialog.dismiss()
                setupWebViewWithLimitedAccess()
            }
            .setNegativeButton("Ayarlar") { dialog, _ ->
                dialog.dismiss()
                openAppSettings()
            }
            .setCancelable(false)
            .show()
    }

    private fun openAppSettings() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", packageName, null)
        }
        try {
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "Ayarlar açılamadı", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupWebView() {
        // Tam erişim ile WebView'i kur
        initializeWebView(true)
    }

    private fun setupWebViewWithLimitedAccess() {
        // Sınırlı erişim ile WebView'i kur
        initializeWebView(false)
    }

    private fun initializeWebView(hasStoragePermission: Boolean) {
        webView = WebView(this).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.allowFileAccess = hasStoragePermission
            settings.allowContentAccess = hasStoragePermission
            
            webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    // JavaScript ile izin durumunu bildir
                    if (!hasStoragePermission) {
                        evaluateJavascript("""
                            if(typeof onStoragePermissionDenied === 'function') {
                                onStoragePermissionDenied();
                            }
                        """.trimIndent(), null)
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
                        // İzin yoksa kullanıcıyı bilgilendir
                        runOnUiThread {
                            Toast.makeText(
                                this@MainActivity,
                                "Dosya erişim izni olmadığı için dosya seçilemiyor",
                                Toast.LENGTH_LONG
                            ).show()
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
