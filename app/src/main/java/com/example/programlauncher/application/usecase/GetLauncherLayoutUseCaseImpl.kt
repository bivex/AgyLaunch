package com.example.programlauncher.application.usecase

import com.example.programlauncher.domain.model.LauncherLayout
import com.example.programlauncher.domain.port.inputput.GetLauncherLayoutUseCase
import com.example.programlauncher.domain.port.output.LauncherLayoutRepository

class GetLauncherLayoutUseCaseImpl(
    private val layoutRepository: LauncherLayoutRepository
) : GetLauncherLayoutUseCase {
    override fun execute(): LauncherLayout {
        val gridSize = layoutRepository.getGridSize()
        val items = layoutRepository.getItems()
        return LauncherLayout(gridSize, items.toMutableList())
    }
}
