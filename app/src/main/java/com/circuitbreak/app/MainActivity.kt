package com.circuitbreak.app

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
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
import androidx.core.app.NotificationCompat
import com.circuitbreak.app.data.ItemStore

class MainActivity : ComponentActivity() {

    private var webView: WebView? = null
    private var pageLoaded = false

    companion object {
        private const val NOTIFY_ID = 1
        private const val CHANNEL_ID = "circuit_break"
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        createNotificationChannel()

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
        cancelNotification()
        if (pageLoaded) {
            pushItems()
            pushSoundPref()
        }
    }

    override fun onPause() {
        super.onPause()
        showNotification()
    }

    override fun onDestroy() {
        webView?.destroy()
        super.onDestroy()
    }

    private fun createNotificationChannel() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID, "Circuit Break", NotificationManager.IMPORTANCE_LOW
            ).apply { description = "Persistent reminder" }
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    private fun showNotification() {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pending = PendingIntent.getActivity(
            this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Bored?")
            .setContentText("Spin instead of snack")
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setOngoing(true)
            .setContentIntent(pending)
            .build()
        getSystemService(NotificationManager::class.java).notify(NOTIFY_ID, notification)
    }

    private fun cancelNotification() {
        getSystemService(NotificationManager::class.java).cancel(NOTIFY_ID)
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
