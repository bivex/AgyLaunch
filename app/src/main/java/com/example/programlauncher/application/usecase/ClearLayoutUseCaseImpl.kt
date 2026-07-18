package com.example.programlauncher.application.usecase

import com.example.programlauncher.domain.port.`in`.ClearLayoutUseCase
import com.example.programlauncher.domain.port.out.LauncherEventPublisher
import com.example.programlauncher.domain.port.out.LauncherLayoutRepository

class ClearLayoutUseCaseImpl(
    private val layoutRepository: LauncherLayoutRepository,
    private val eventPublisher: LauncherEventPublisher
) : ClearLayoutUseCase {
    override fun execute() {
        layoutRepository.clearLayout()
        eventPublisher.publishLayoutChanged()
    }
}
