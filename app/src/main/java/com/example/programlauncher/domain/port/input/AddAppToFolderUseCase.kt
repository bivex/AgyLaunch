package com.example.programlauncher.domain.port.input

import com.example.programlauncher.domain.model.GridPosition

interface AddAppToFolderUseCase {
    fun execute(folderPosition: GridPosition, packageName: String): Result<Unit>
    fun execute(folderLabel: String, packageName: String): Result<Unit>
}
