package com.example.programlauncher.domain.model

data class GridPosition(
    val x: Int,
    val y: Int,
    val screen: Int
)

sealed class LauncherItem {
    abstract val id: Long?
    abstract val position: GridPosition
    abstract val label: String

    data class AppShortcut(
        override val id: Long? = null,
        val packageName: String,
        val className: String,
        override val label: String,
        override val position: GridPosition
    ) : LauncherItem()

    data class Folder(
        override val id: Long? = null,
        override val label: String,
        override val position: GridPosition,
        val items: List<AppShortcut> = emptyList()
    ) : LauncherItem()
}
