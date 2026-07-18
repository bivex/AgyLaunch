package com.example.programlauncher.domain.port.out

data class AppDetail(
    val packageName: String,
    val className: String,
    val label: String
)

interface AppService {
    fun getInstalledApps(): List<AppDetail>
    fun getAppDetails(packageName: String): AppDetail?
}
