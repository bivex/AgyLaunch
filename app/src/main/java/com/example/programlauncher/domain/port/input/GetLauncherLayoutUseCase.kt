package com.example.programlauncher.domain.port.input

import com.example.programlauncher.domain.model.LauncherLayout

interface GetLauncherLayoutUseCase {
    fun execute(): LauncherLayout
}
