package com.example.programlauncher.domain.port.inputput

import com.example.programlauncher.domain.model.LauncherLayout

interface GetLauncherLayoutUseCase {
    fun execute(): LauncherLayout
}
