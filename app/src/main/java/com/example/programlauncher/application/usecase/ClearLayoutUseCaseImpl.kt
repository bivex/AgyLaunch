package com.example.programlauncher.application.usecase

import com.example.programlauncher.domain.port.inputput.ClearLayoutUseCase
import com.example.programlauncher.domain.port.output.LauncherEventPublisher
import com.example.programlauncher.domain.port.output.LauncherLayoutRepository

class ClearLayoutUseCaseImpl(
    private val layoutRepository: LauncherLayoutRepository,
    private val eventPublisher: LauncherEventPublisher
) : ClearLayoutUseCase {
    override fun execute() {
        layoutRepository.clearLayout()
        eventPublisher.publishLayoutChanged()
    }
}
