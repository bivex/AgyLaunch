package com.example.programlauncher.domain.model

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable

@Immutable
data class GridSize(val columns: Int, val rows: Int)

@Stable
class LauncherLayout(
    var gridSize: GridSize,
    private val mutableItems: MutableList<LauncherItem> = mutableListOf()
) {
    val items: List<LauncherItem> get() = mutableItems.toList()

    fun canPlaceAt(position: GridPosition): Boolean {
        if (position.x < 0 || position.x >= gridSize.columns || position.y < 0 || position.y >= gridSize.rows) {
            return false
        }
        return items.none { it.position.x == position.x && it.position.y == position.y && it.position.screen == position.screen }
    }

    fun addItem(item: LauncherItem): Result<Unit> {
        if (!canPlaceAt(item.position)) {
            return Result.failure(IllegalArgumentException("Grid position (${item.position.x}, ${item.position.y}) on screen ${item.position.screen} is occupied or out of bounds."))
        }
        mutableItems.add(item)
        return Result.success(Unit)
    }

    fun removeItemAt(position: GridPosition): Boolean {
        return mutableItems.removeIf { it.position == position }
    }

    fun removeItemByPackage(packageName: String): Boolean {
        return mutableItems.removeIf {
            when (it) {
                is LauncherItem.AppShortcut -> it.packageName == packageName
                is LauncherItem.Folder -> false
            }
        }
    }

    fun clear() {
        mutableItems.clear()
    }
}
