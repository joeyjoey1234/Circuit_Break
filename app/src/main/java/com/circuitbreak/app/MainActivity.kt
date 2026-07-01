package com.circuitbreak.app

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.util.Base64
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import android.app.AlertDialog
import com.circuitbreak.app.data.ItemStore

class MainActivity : ComponentActivity() {

    private var webView: WebView? = null
    private var pageLoaded = false

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val wv = WebView(this).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.allowFileAccess = true
            webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView, url: String) {
                    if (isFinishing || isDestroyed) return
                    pageLoaded = true
                    pushItems()
                    pushSoundPref()
                    checkForUpdate()
                }
            }
            webChromeClient = WebChromeClient()
            addJavascriptInterface(CircuitBridge(), "android")
        }
        webView = wv

        setContentView(wv)
        wv.loadUrl("file:///android_asset/spin.html")
    }

    override fun onResume() {
        super.onResume()
        if (pageLoaded) {
            pushItems()
            pushSoundPref()
        }
    }

    override fun onDestroy() {
        webView?.destroy()
        super.onDestroy()
    }

    private fun pushItems() {
        val wv = webView ?: return
        val (defPhys, defCog) = ItemStore.loadDefaults(this)
        val phys = ItemStore.getMergedItems(this, defPhys, "physical")
        val cog = ItemStore.getMergedItems(this, defCog, "cognitive")
        val json = ItemStore.allToJson(phys, cog)
        val b64 = Base64.encodeToString(json.toByteArray(), Base64.NO_WRAP)

        wv.post {
            wv.evaluateJavascript("if(window.loadItems) window.loadItems(atob('$b64'))", null)
        }
    }

    private fun pushSoundPref() {
        val wv = webView ?: return
        val enabled = ItemStore.isSoundEnabled(this)
        wv.post {
            wv.evaluateJavascript("if(window.setSound) window.setSound($enabled)", null)
        }
    }

    private val settingsLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            pushItems()
            pushSoundPref()
        }

    private fun checkForUpdate() {
        val currentVersion = try {
            "v" + (packageManager.getPackageInfo(packageName, 0).versionName ?: "0")
        } catch (e: Exception) {
            return
        }
        UpdateChecker.check { release ->
            if (release != null && UpdateChecker.isNewer(currentVersion, release.tagName)) {
                runOnUiThread {
                    if (isFinishing || isDestroyed) return@runOnUiThread
                    try {
                        AlertDialog.Builder(this@MainActivity)
                            .setTitle("Update Available")
                            .setMessage("${release.tagName} is available (current: $currentVersion).\nDownload and install?")
                            .setPositiveButton("Download") { _, _ ->
                                UpdateChecker.downloadAndInstall(this@MainActivity, release.downloadUrl, release.fileName)
                            }
                            .setNegativeButton("Later", null)
                            .show()
                    } catch (e: Exception) {
                        Toast.makeText(this@MainActivity, "Update check failed", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
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
