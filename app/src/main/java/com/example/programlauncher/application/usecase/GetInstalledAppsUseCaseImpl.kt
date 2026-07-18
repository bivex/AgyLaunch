package com.example.programlauncher.application.usecase

import com.example.programlauncher.domain.port.input.GetInstalledAppsUseCase
import com.example.programlauncher.domain.port.output.AppDetail
import com.example.programlauncher.domain.port.output.AppService

class GetInstalledAppsUseCaseImpl(
    private val appService: AppService
) : GetInstalledAppsUseCase {
    override fun execute(): List<AppDetail> {
        return appService.getInstalledApps()
    }
}
