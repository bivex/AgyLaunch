package com.example.programlauncher.domain.port.`in`

import com.example.programlauncher.domain.port.out.AppDetail

interface GetInstalledAppsUseCase {
    fun execute(): List<AppDetail>
}
