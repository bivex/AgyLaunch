package com.example.programlauncher.domain.port.input

import com.example.programlauncher.domain.port.output.AppDetail

interface GetInstalledAppsUseCase {
    fun execute(): List<AppDetail>
}
