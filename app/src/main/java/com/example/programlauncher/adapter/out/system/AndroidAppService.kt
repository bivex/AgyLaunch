package com.example.programlauncher.adapter.out.system

import android.content.Context
import android.content.Intent
import com.example.programlauncher.domain.port.out.AppDetail
import com.example.programlauncher.domain.port.out.AppService

class AndroidAppService(private val context: Context) : AppService {

    override fun getInstalledApps(): List<AppDetail> {
        val pm = context.packageManager
        val mainIntent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        val resolveInfos = pm.queryIntentActivities(mainIntent, 0)
        return resolveInfos.mapNotNull { info ->
            val packageName = info.activityInfo.packageName
            val className = info.activityInfo.name
            val label = info.loadLabel(pm).toString()
            if (packageName.isNotEmpty() && className.isNotEmpty()) {
                AppDetail(packageName, className, label)
            } else {
                null
            }
        }.sortedBy { it.label.lowercase() }
    }

    override fun getAppDetails(packageName: String): AppDetail? {
        val pm = context.packageManager
        val mainIntent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
            setPackage(packageName)
        }
        val resolveInfos = pm.queryIntentActivities(mainIntent, 0)
        val info = resolveInfos.firstOrNull() ?: return null
        val className = info.activityInfo.name
        val label = info.loadLabel(pm).toString()
        return AppDetail(packageName, className, label)
    }
}
