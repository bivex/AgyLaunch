package com.example.programlauncher.domain.port.`in`

import com.example.programlauncher.domain.model.LauncherLayout

interface GetLauncherLayoutUseCase {
    fun execute(): LauncherLayout
}
