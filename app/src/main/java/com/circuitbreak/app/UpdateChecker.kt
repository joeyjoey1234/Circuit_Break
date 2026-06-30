package com.circuitbreak.app

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.widget.Toast
import androidx.core.content.FileProvider
import org.json.JSONObject
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import kotlin.concurrent.thread

object UpdateChecker {

    private const val RELEASES_URL = "https://api.github.com/repos/joeyjoey1234/Circuit_Break/releases/latest"

    data class ReleaseInfo(
        val tagName: String,
        val downloadUrl: String,
        val fileName: String
    )

    fun check(onResult: (ReleaseInfo?) -> Unit) {
        thread {
            try {
                val conn = URL(RELEASES_URL).openConnection() as HttpURLConnection
                conn.setRequestProperty("Accept", "application/vnd.github.v3+json")
                conn.connectTimeout = 8000
                conn.readTimeout = 8000
                val reader = BufferedReader(InputStreamReader(conn.inputStream))
                val json = JSONObject(reader.readText())
                reader.close()
                conn.disconnect()

                val tagName = json.getString("tag_name")
                val assets = json.getJSONArray("assets")
                if (assets.length() > 0) {
                    val apk = assets.getJSONObject(0)
                    onResult(ReleaseInfo(tagName, apk.getString("browser_download_url"), apk.getString("name")))
                } else {
                    onResult(null)
                }
            } catch (e: Exception) {
                onResult(null)
            }
        }
    }

    fun isNewer(currentVersion: String, latestTag: String): Boolean {
        // compare v1.2.3 > v1.1.1 style tags
        val cur = currentVersion.replace("v", "").split(".")
        val lat = latestTag.replace("v", "").split(".")
        for (i in 0 until maxOf(cur.size, lat.size)) {
            val c = cur.getOrElse(i) { "0" }.toIntOrNull() ?: 0
            val l = lat.getOrElse(i) { "0" }.toIntOrNull() ?: 0
            if (l > c) return true
            if (l < c) return false
        }
        return false
    }

    fun downloadAndInstall(context: Context, downloadUrl: String, fileName: String) {
        val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val uri = Uri.parse(downloadUrl)
        val request = DownloadManager.Request(uri).apply {
            setTitle("Circuit Break Update")
            setDescription("Downloading $fileName")
            setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
        }
        val downloadId = dm.enqueue(request)

        // when download completes, launch install
        val onComplete = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context, intent: Intent) {
                val id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
                if (id != downloadId) return
                ctx.applicationContext.unregisterReceiver(this)

                val file = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), fileName)
                if (!file.exists()) return

                val apkUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    FileProvider.getUriForFile(ctx, "${ctx.packageName}.fileprovider", file)
                } else {
                    Uri.fromFile(file)
                }

                val installIntent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(apkUri, "application/vnd.android.package-archive")
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                try {
                    ctx.startActivity(installIntent)
                } catch (e: Exception) {
                    Toast.makeText(ctx, "Could not install update", Toast.LENGTH_SHORT).show()
                }
            }
        }
        context.registerReceiver(onComplete, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE))
    }
}
