package com.example.programlauncher.application.usecase

import com.example.programlauncher.domain.model.GridPosition
import com.example.programlauncher.domain.model.LauncherItem
import com.example.programlauncher.domain.model.LauncherLayout
import com.example.programlauncher.domain.port.`in`.AddShortcutUseCase
import com.example.programlauncher.domain.port.out.AppService
import com.example.programlauncher.domain.port.out.LauncherEventPublisher
import com.example.programlauncher.domain.port.out.LauncherLayoutRepository

class AddShortcutUseCaseImpl(
    private val appService: AppService,
    private val layoutRepository: LauncherLayoutRepository,
    private val eventPublisher: LauncherEventPublisher
) : AddShortcutUseCase {
    override fun execute(packageName: String, position: GridPosition): Result<LauncherItem.AppShortcut> {
        val appDetail = appService.getAppDetails(packageName)
            ?: return Result.failure(IllegalArgumentException("Package $packageName not found or does not have a launcher category activity."))

        val currentGridSize = layoutRepository.getGridSize()
        val currentItems = layoutRepository.getItems()
        val layout = LauncherLayout(currentGridSize, currentItems.toMutableList())

        val newShortcut = LauncherItem.AppShortcut(
            packageName = appDetail.packageName,
            className = appDetail.className,
            label = appDetail.label,
            position = position
        )

        val placementResult = layout.addItem(newShortcut)
        if (placementResult.isFailure) {
            return Result.failure(placementResult.exceptionOrNull() ?: Exception("Cannot place shortcut at standard coordinate."))
        }

        val saveResult = layoutRepository.saveItem(newShortcut)
        return if (saveResult.isSuccess) {
            eventPublisher.publishLayoutChanged()
            Result.success(newShortcut.copy(id = saveResult.getOrThrow()))
        } else {
            Result.failure(saveResult.exceptionOrNull() ?: Exception("Failed to persist shortcut layout."))
        }
    }
}
