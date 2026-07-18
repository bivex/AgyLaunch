package com.example.programlauncher

import android.content.Context
import com.example.programlauncher.adapter.output.event.LauncherEventBus
import com.example.programlauncher.adapter.output.persistence.SqliteLauncherLayoutRepository
import com.example.programlauncher.adapter.output.system.AndroidAppService
import com.example.programlauncher.application.usecase.*
import com.example.programlauncher.domain.port.input.*

class DependencyContainer(context: Context) {
    val eventBus = LauncherEventBus()
    val appService = AndroidAppService(context.applicationContext)
    val layoutRepository = SqliteLauncherLayoutRepository(context.applicationContext)

    val addShortcutUseCase: AddShortcutUseCase = AddShortcutUseCaseImpl(appService, layoutRepository, eventBus)
    val removeShortcutUseCase: RemoveShortcutUseCase = RemoveShortcutUseCaseImpl(layoutRepository, eventBus)
    val getLauncherLayoutUseCase: GetLauncherLayoutUseCase = GetLauncherLayoutUseCaseImpl(layoutRepository)
    val clearLayoutUseCase: ClearLayoutUseCase = ClearLayoutUseCaseImpl(layoutRepository, eventBus)
    val setGridSizeUseCase: SetGridSizeUseCase = SetGridSizeUseCaseImpl(layoutRepository, eventBus)
    val getInstalledAppsUseCase: GetInstalledAppsUseCase = GetInstalledAppsUseCaseImpl(appService)
}
