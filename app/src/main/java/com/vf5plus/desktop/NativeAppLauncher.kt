package com.vf5plus.desktop

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.util.Log

data class AppItem(
    val label: String,
    val packageName: String,
    val activityName: String,
    val icon: Drawable?
)

object NativeAppLauncher {

    private const val TAG = "NativeAppLauncher"

    /**
     * Lấy danh sách tất cả các ứng dụng có thể mở (Launcher apps) trên điện thoại
     */
    fun getInstalledApps(context: Context): List<AppItem> {
        val pm = context.packageManager
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val resolveInfos = pm.queryIntentActivities(intent, 0)
        val list = mutableListOf<AppItem>()

        for (ri in resolveInfos) {
            try {
                val label = ri.loadLabel(pm).toString()
                val pkg = ri.activityInfo.packageName
                val act = ri.activityInfo.name
                val icon = ri.loadIcon(pm)
                list.add(AppItem(label, pkg, act, icon))
            } catch (e: Exception) {
                // ignore
            }
        }
        return list.sortedBy { it.label.lowercase() }
    }

    /**
     * Khởi chạy ứng dụng lên màn hình phụ (VirtualDisplay) sử dụng lệnh am start --display
     * Thông qua cơ chế Shizuku hoặc Runtime.getRuntime().exec()
     */
    fun launchOnDisplay(context: Context, appItem: AppItem, displayId: Int): Boolean {
        return try {
            val cmd = "am start --display $displayId -n ${appItem.packageName}/${appItem.activityName}"
            val process = Runtime.getRuntime().exec(arrayOf("sh", "-c", cmd))
            process.waitFor()
            true
        } catch (e: Exception) {
            Log.e(TAG, "Lỗi khởi chạy ${appItem.packageName}: ${e.message}")
            false
        }
    }
}
