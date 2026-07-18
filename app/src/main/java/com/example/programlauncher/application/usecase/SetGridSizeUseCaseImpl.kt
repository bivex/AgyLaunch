package com.example.programlauncher.application.usecase

import com.example.programlauncher.domain.model.GridSize
import com.example.programlauncher.domain.port.inputput.SetGridSizeUseCase
import com.example.programlauncher.domain.port.output.LauncherEventPublisher
import com.example.programlauncher.domain.port.output.LauncherLayoutRepository

class SetGridSizeUseCaseImpl(
    private val layoutRepository: LauncherLayoutRepository,
    private val eventPublisher: LauncherEventPublisher
) : SetGridSizeUseCase {
    override fun execute(gridSize: GridSize): Result<Unit> {
        if (gridSize.columns <= 0 || gridSize.rows <= 0) {
            return Result.failure(IllegalArgumentException("Grid size dimensions must be positive values."))
        }
        layoutRepository.setGridSize(gridSize)
        eventPublisher.publishLayoutChanged()
        return Result.success(Unit)
    }
}
