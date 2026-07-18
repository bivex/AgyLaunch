package com.example.programlauncher.domain.port.input

import com.example.programlauncher.domain.model.GridPosition
import com.example.programlauncher.domain.model.LauncherItem

interface AddShortcutUseCase {
    fun execute(packageName: String, position: GridPosition): Result<LauncherItem.AppShortcut>
}
