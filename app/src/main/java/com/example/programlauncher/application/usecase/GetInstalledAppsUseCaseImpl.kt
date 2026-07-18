package com.example.programlauncher.application.usecase

import com.example.programlauncher.domain.port.`in`.GetInstalledAppsUseCase
import com.example.programlauncher.domain.port.out.AppDetail
import com.example.programlauncher.domain.port.out.AppService

class GetInstalledAppsUseCaseImpl(
    private val appService: AppService
) : GetInstalledAppsUseCase {
    override fun execute(): List<AppDetail> {
        return appService.getInstalledApps()
    }
}
