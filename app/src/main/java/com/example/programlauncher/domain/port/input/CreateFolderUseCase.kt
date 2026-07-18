package com.example.programlauncher.domain.port.input

import com.example.programlauncher.domain.model.GridPosition
import com.example.programlauncher.domain.model.LauncherItem

interface CreateFolderUseCase {
    fun execute(label: String, position: GridPosition): Result<LauncherItem.Folder>
}
