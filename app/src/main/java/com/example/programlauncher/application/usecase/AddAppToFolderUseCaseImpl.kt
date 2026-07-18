package com.example.programlauncher.application.usecase

import com.example.programlauncher.domain.model.GridPosition
import com.example.programlauncher.domain.model.LauncherItem
import com.example.programlauncher.domain.port.input.AddAppToFolderUseCase
import com.example.programlauncher.domain.port.output.AppService
import com.example.programlauncher.domain.port.output.LauncherEventPublisher
import com.example.programlauncher.domain.port.output.LauncherLayoutRepository

class AddAppToFolderUseCaseImpl(
    private val appService: AppService,
    private val layoutRepository: LauncherLayoutRepository,
    private val eventPublisher: LauncherEventPublisher
) : AddAppToFolderUseCase {

    override fun execute(folderPosition: GridPosition, packageName: String): Result<Unit> {
        val items = layoutRepository.getItems()
        val folder = items.find { it.position == folderPosition && it is LauncherItem.Folder } as? LauncherItem.Folder
            ?: return Result.failure(IllegalArgumentException("No folder found at grid position."))

        return addShortcutToFolder(folder, packageName)
    }

    override fun execute(folderLabel: String, packageName: String): Result<Unit> {
        val items = layoutRepository.getItems()
        val folder = items.find { it is LauncherItem.Folder && it.label.equals(folderLabel, ignoreCase = true) } as? LauncherItem.Folder
            ?: return Result.failure(IllegalArgumentException("No folder found with name $folderLabel."))

        return addShortcutToFolder(folder, packageName)
    }

    private fun addShortcutToFolder(folder: LauncherItem.Folder, packageName: String): Result<Unit> {
        val appDetail = appService.getAppDetails(packageName)
            ?: return Result.failure(IllegalArgumentException("Package $packageName not found."))

        val newShortcut = LauncherItem.AppShortcut(
            packageName = appDetail.packageName,
            className = appDetail.className,
            label = appDetail.label,
            position = GridPosition(-1, -1, -1)
        )

        // Check if app is already in the folder
        if (folder.items.any { it.packageName == packageName }) {
            return Result.success(Unit)
        }

        val updatedFolder = folder.copy(items = folder.items + newShortcut)
        val saveResult = layoutRepository.saveItem(updatedFolder)
        
        return if (saveResult.isSuccess) {
            eventPublisher.publishLayoutChanged()
            Result.success(Unit)
        } else {
            Result.failure(saveResult.exceptionOrNull() ?: Exception("Failed to update folder in database."))
        }
    }
}
