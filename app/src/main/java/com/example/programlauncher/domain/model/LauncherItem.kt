package com.example.programlauncher.domain.model

import androidx.compose.runtime.Immutable

@Immutable
data class GridPosition(
    val x: Int,
    val y: Int,
    val screen: Int
)

@Immutable
sealed class LauncherItem {
    abstract val id: Long?
    abstract val position: GridPosition
    abstract val label: String

    @Immutable
    data class AppShortcut(
        override val id: Long? = null,
        val packageName: String,
        val className: String,
        override val label: String,
        override val position: GridPosition
    ) : LauncherItem()

    @Immutable
    data class Folder(
        override val id: Long? = null,
        override val label: String,
        override val position: GridPosition,
        val items: List<AppShortcut> = emptyList()
    ) : LauncherItem()
}
