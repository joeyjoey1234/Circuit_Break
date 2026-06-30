package com.circuitbreak.app

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import com.circuitbreak.app.data.ItemStore

class MainActivity : ComponentActivity() {

    private lateinit var webView: WebView

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        webView = WebView(this).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.allowFileAccess = true
            webViewClient = WebViewClient()
            webChromeClient = WebChromeClient()
            addJavascriptInterface(CircuitBridge(), "android")
        }

        setContentView(webView)

        // Load the HTML once, then push items
        webView.loadUrl("file:///android_asset/spin.html")
    }

    override fun onResume() {
        super.onResume()
        // Push merged item lists to the WebView
        pushItems()
    }

    private fun pushItems() {
        val (defPhys, defCog) = ItemStore.loadDefaults(this)
        val phys = ItemStore.getMergedItems(this, defPhys, "physical")
        val cog = ItemStore.getMergedItems(this, defCog, "cognitive")
        val json = ItemStore.allToJson(phys, cog).replace("'", "\\'")

        webView.post {
            webView.evaluateJavascript("if(window.loadItems) window.loadItems('$json')", null)
        }
    }

    private val settingsLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            pushItems()
        }

    inner class CircuitBridge {
        @JavascriptInterface
        fun openSettings() {
            runOnUiThread {
                settingsLauncher.launch(Intent(this@MainActivity, SettingsActivity::class.java))
            }
        }

        @JavascriptInterface
        fun showToast(msg: String) {
            runOnUiThread { Toast.makeText(this@MainActivity, msg, Toast.LENGTH_SHORT).show() }
        }
    }
}
