package com.arvinapp.acgb

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private var mUploadMessage: ValueCallback<Array<Uri>>? = null
    private val FILECHOOSER_RESULTCODE = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        webView = WebView(this).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            webViewClient = WebViewClient()
            webChromeClient = object : WebChromeClient() {
                // Dosya seçme penceresini açmak için bu metot gereklidir
                override fun onShowFileChooser(
                    webView: WebView?,
                    filePathCallback: ValueCallback<Array<Uri>>?,
                    fileChooserParams: FileChooserParams?
                ): Boolean {
                    if (mUploadMessage != null) {
                        mUploadMessage!!.onReceiveValue(null)
                        mUploadMessage = null
                    }
                    mUploadMessage = filePathCallback
                    val intent = fileChooserParams?.createIntent()
                    startActivityForResult(intent, FILECHOOSER_RESULTCODE)
                    return true
                }
            }
        }

        setContentView(webView)

        // assets içindeki HTML dosyasını aç
        webView.loadUrl("file:///android_asset/index.html")
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == FILECHOOSER_RESULTCODE) {
            if (mUploadMessage == null) {
                return
            }
            mUploadMessage!!.onReceiveValue(WebChromeClient.FileChooserParams.parseResult(resultCode, data))
            mUploadMessage = null
        }
    }

    // Geri tuşu WebView'de gezinme için
    override fun onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack()
        } else {
            super.onBackPressed()
        }
    }
}
