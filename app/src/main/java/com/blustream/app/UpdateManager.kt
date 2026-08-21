package com.blustream.app

import android.app.Activity
import android.app.AlertDialog
import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import android.widget.Toast
import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.URL

object UpdateManager {
    private const val RELEASES_URL = "https://api.github.com/repos/maccabluu/BluStream/releases?per_page=30"
    private const val PREFS = "blustream_updates"
    private const val LAST_AUTO_CHECK = "last_auto_check"
    private const val AUTO_COOLDOWN_MS = 15L * 60L * 1000L

    data class AppVersion(val major: Int, val minor: Int, val patch: Int = 0, val build: Int = 0) : Comparable<AppVersion> {
        override fun compareTo(other: AppVersion): Int = compareValuesBy(this, other, AppVersion::major, AppVersion::minor, AppVersion::patch, AppVersion::build)
    }

    data class ReleaseInfo(val tag: String, val title: String, val notes: String, val apkUrl: String, val version: AppVersion)

    fun check(activity: Activity, manual: Boolean) {
        val prefs = activity.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val now = System.currentTimeMillis()
        if (!manual) {
            val last = prefs.getLong(LAST_AUTO_CHECK, 0L)
            if (now - last < AUTO_COOLDOWN_MS) return
            prefs.edit().putLong(LAST_AUTO_CHECK, now).apply()
        }

        if (manual) Toast.makeText(activity, "Checking for BluStream updates…", Toast.LENGTH_SHORT).show()

        Thread {
            val result = runCatching {
                val current = currentVersion(activity)
                fetchReleases().filter { it.version > current }.maxByOrNull { it.version }
            }
            activity.runOnUiThread {
                result.onSuccess { release ->
                    if (release != null) showUpdateDialog(activity, release)
                    else if (manual) Toast.makeText(activity, "BluStream is up to date.", Toast.LENGTH_SHORT).show()
                }.onFailure {
                    if (manual) Toast.makeText(activity, "Update check failed. Try again later.", Toast.LENGTH_LONG).show()
                }
            }
        }.start()
    }

    private fun currentVersion(context: Context): AppVersion {
        val name = runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName.orEmpty() }.getOrDefault("")
        return parseVersion(name) ?: AppVersion(0, 0)
    }

    private fun parseVersion(value: String): AppVersion? {
        val clean = value.trim().removePrefix("v")
        Regex("^(\\d+)\\.(\\d+)$").matchEntire(clean)?.let {
            return AppVersion(it.groupValues[1].toInt(), it.groupValues[2].toInt())
        }
        Regex("^(\\d+)\\.(\\d+)\\.(\\d+)$").matchEntire(clean)?.let {
            return AppVersion(it.groupValues[1].toInt(), it.groupValues[2].toInt(), it.groupValues[3].toInt())
        }
        Regex("^(\\d+)\\.(\\d+)\\.(\\d+)-alpha\\.(\\d+)$").matchEntire(clean)?.let {
            return AppVersion(it.groupValues[1].toInt(), it.groupValues[2].toInt(), it.groupValues[3].toInt(), it.groupValues[4].toInt())
        }
        Regex("^(\\d+)\\.(\\d+)\\.(\\d+)-alpha$").matchEntire(clean)?.let {
            return AppVersion(it.groupValues[1].toInt(), it.groupValues[2].toInt(), it.groupValues[3].toInt())
        }
        return null
    }

    private fun fetchReleases(): List<ReleaseInfo> {
        val connection = (URL(RELEASES_URL).openConnection() as HttpURLConnection).apply {
            connectTimeout = 10_000
            readTimeout = 15_000
            requestMethod = "GET"
            setRequestProperty("Accept", "application/vnd.github+json")
            setRequestProperty("User-Agent", "BluStream-Android")
        }
        try {
            if (connection.responseCode !in 200..299) error("GitHub returned ${connection.responseCode}")
            val releases = JSONArray(connection.inputStream.bufferedReader().use { it.readText() })
            val result = mutableListOf<ReleaseInfo>()
            for (i in 0 until releases.length()) {
                val release = releases.getJSONObject(i)
                if (release.optBoolean("draft", false)) continue
                val tag = release.optString("tag_name")
                val version = parseVersion(tag) ?: continue
                val assets = release.optJSONArray("assets") ?: continue
                var apkUrl: String? = null
                for (j in 0 until assets.length()) {
                    val asset = assets.getJSONObject(j)
                    if (asset.optString("name").endsWith(".apk", true)) {
                        apkUrl = asset.optString("browser_download_url")
                        break
                    }
                }
                if (!apkUrl.isNullOrBlank()) result += ReleaseInfo(tag, release.optString("name").ifBlank { tag }, release.optString("body").ifBlank { "No release notes were provided." }, apkUrl, version)
            }
            return result
        } finally {
            connection.disconnect()
        }
    }

    private fun showUpdateDialog(activity: Activity, release: ReleaseInfo) {
        AlertDialog.Builder(activity)
            .setTitle("BluStream update available")
            .setMessage("${release.title} is ready to install.")
            .setPositiveButton("Update now") { _, _ -> prepareDownload(activity, release) }
            .setNeutralButton("What's new") { _, _ -> showReleaseNotes(activity, release) }
            .setNegativeButton("Later", null)
            .show()
    }

    private fun showReleaseNotes(activity: Activity, release: ReleaseInfo) {
        AlertDialog.Builder(activity)
            .setTitle(release.title)
            .setMessage(release.notes)
            .setPositiveButton("Update now") { _, _ -> prepareDownload(activity, release) }
            .setNegativeButton("Later", null)
            .show()
    }

    private fun prepareDownload(activity: Activity, release: ReleaseInfo) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !activity.packageManager.canRequestPackageInstalls()) {
            Toast.makeText(activity, "Allow BluStream to install updates, then return and check again.", Toast.LENGTH_LONG).show()
            activity.startActivity(Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${activity.packageName}")))
            return
        }
        val manager = activity.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val fileName = "BluStream-${release.tag.removePrefix("v")}.apk"
        val request = DownloadManager.Request(Uri.parse(release.apkUrl))
            .setTitle("BluStream update")
            .setDescription("Downloading ${release.title}")
            .setMimeType("application/vnd.android.package-archive")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
        val downloadId = manager.enqueue(request)
        Toast.makeText(activity, "BluStream update download started.", Toast.LENGTH_SHORT).show()

        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                if (intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1L) != downloadId) return
                runCatching { activity.unregisterReceiver(this) }
                val uri = manager.getUriForDownloadedFile(downloadId)
                if (uri == null) {
                    Toast.makeText(activity, "Update download failed.", Toast.LENGTH_LONG).show()
                    return
                }
                activity.startActivity(Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(uri, "application/vnd.android.package-archive")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                })
            }
        }
        val filter = IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE)
        if (Build.VERSION.SDK_INT >= 33) activity.registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED)
        else {
            @Suppress("DEPRECATION")
            activity.registerReceiver(receiver, filter)
        }
    }
}
