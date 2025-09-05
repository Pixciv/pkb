package com.arvinapp.acgb

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private var mUploadMessage: ValueCallback<Array<Uri>>? = null
    private val FILECHOOSER_RESULTCODE = 1
    private val PERMISSION_REQUEST_CODE = 2

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Dosya erişim izinlerini kontrol et
        checkPermissions()
    }

    private fun checkPermissions() {
        // Eğer Android 6.0 (M) veya üstüyse, izinleri iste
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val readPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
            val writePermission = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)

            if (readPermission != PackageManager.PERMISSION_GRANTED || writePermission != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE),
                    PERMISSION_REQUEST_CODE
                )
            } else {
                // İzinler zaten verilmiş, WebView'i ayarla
                setupWebView()
            }
        } else {
            // Android 6.0 altı için izinler otomatik verilir, WebView'i ayarla
            setupWebView()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        // İzin isteğinin sonucunu kontrol et
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                setupWebView()
            } else {
                Toast.makeText(this, "Dosya erişim izni olmadan uygulama çalışmayabilir.", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun setupWebView() {
        webView = WebView(this).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            webViewClient = WebViewClient()
            webChromeClient = object : WebChromeClient() {
                override fun onShowFileChooser(
                    webView: WebView?,
                    filePathCallback: ValueCallback<Array<Uri>>?,
                    fileChooserParams: FileChooserParams?
                ): Boolean {
                    mUploadMessage?.onReceiveValue(null)
                    mUploadMessage = filePathCallback

                    val intent = fileChooserParams?.createIntent()
                    try {
                        startActivityForResult(intent, FILECHOOSER_RESULTCODE)
                    } catch (e: ActivityNotFoundException) {
                        mUploadMessage = null
                        Toast.makeText(this@MainActivity, "Dosya yöneticisi açılamadı.", Toast.LENGTH_LONG).show()
                        return false
                    }
                    return true
                }
            }
        }
        setContentView(webView)

        // assets klasöründeki HTML dosyasını aç
        webView.loadUrl("file:///android_asset/index.html")
    }

        override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == FILECHOOSER_RESULTCODE) {
            if (mUploadMessage == null) {
                return
            }
            // `data` değişkeninin null olup olmadığını ve sonucun başarılı olup olmadığını kontrol et
            val result = if (data == null || resultCode != RESULT_OK) null else data
            mUploadMessage!!.onReceiveValue(
                WebChromeClient.FileChooserParams.parseResult(resultCode, result)
            )
            mUploadMessage = null
        }
    }


    override fun onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack()
        } else {
            super.onBackPressed()
        }
    }
}
