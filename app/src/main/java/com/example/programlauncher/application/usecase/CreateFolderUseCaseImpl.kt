package com.example.programlauncher.application.usecase

import com.example.programlauncher.domain.model.GridPosition
import com.example.programlauncher.domain.model.LauncherItem
import com.example.programlauncher.domain.model.LauncherLayout
import com.example.programlauncher.domain.port.input.CreateFolderUseCase
import com.example.programlauncher.domain.port.output.LauncherEventPublisher
import com.example.programlauncher.domain.port.output.LauncherLayoutRepository

class CreateFolderUseCaseImpl(
    private val layoutRepository: LauncherLayoutRepository,
    private val eventPublisher: LauncherEventPublisher
) : CreateFolderUseCase {
    override fun execute(label: String, position: GridPosition): Result<LauncherItem.Folder> {
        val currentGridSize = layoutRepository.getGridSize()
        val currentItems = layoutRepository.getItems()
        val layout = LauncherLayout(currentGridSize, currentItems.toMutableList())

        val folder = LauncherItem.Folder(
            label = label,
            position = position,
            items = emptyList()
        )

        val placementResult = layout.addItem(folder)
        if (placementResult.isFailure) {
            return Result.failure(placementResult.exceptionOrNull() ?: Exception("Cannot place folder here."))
        }

        val saveResult = layoutRepository.saveItem(folder)
        return if (saveResult.isSuccess) {
            eventPublisher.publishLayoutChanged()
            Result.success(folder.copy(id = saveResult.getOrThrow()))
        } else {
            Result.failure(saveResult.exceptionOrNull() ?: Exception("Failed to persist folder."))
        }
    }
}
