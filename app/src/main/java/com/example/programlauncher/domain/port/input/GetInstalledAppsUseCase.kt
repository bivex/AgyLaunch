package com.example.programlauncher.domain.port.inputput

import com.example.programlauncher.domain.port.output.AppDetail

interface GetInstalledAppsUseCase {
    fun execute(): List<AppDetail>
}
