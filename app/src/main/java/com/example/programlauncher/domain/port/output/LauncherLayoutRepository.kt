package com.example.programlauncher.domain.port.output

import com.example.programlauncher.domain.model.GridSize
import com.example.programlauncher.domain.model.LauncherItem

interface LauncherLayoutRepository {
    fun getGridSize(): GridSize
    fun setGridSize(gridSize: GridSize)
    fun getItems(): List<LauncherItem>
    fun saveItem(item: LauncherItem): Result<Long>
    fun removeItem(packageName: String): Boolean
    fun removeItemAt(x: Int, y: Int, screen: Int): Boolean
    fun clearLayout()
}
