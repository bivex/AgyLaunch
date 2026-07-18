package com.example.programlauncher.domain.port.input

import com.example.programlauncher.domain.model.GridSize

interface SetGridSizeUseCase {
    fun execute(gridSize: GridSize): Result<Unit>
}
