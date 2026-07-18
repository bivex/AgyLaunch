package com.example.programlauncher.application.usecase

import com.example.programlauncher.domain.port.`in`.RemoveShortcutUseCase
import com.example.programlauncher.domain.port.out.LauncherEventPublisher
import com.example.programlauncher.domain.port.out.LauncherLayoutRepository

class RemoveShortcutUseCaseImpl(
    private val layoutRepository: LauncherLayoutRepository,
    private val eventPublisher: LauncherEventPublisher
) : RemoveShortcutUseCase {
    override fun execute(packageName: String): Boolean {
        val removed = layoutRepository.removeItem(packageName)
        if (removed) {
            eventPublisher.publishLayoutChanged()
        }
        return removed
    }

    override fun executeAt(x: Int, y: Int, screen: Int): Boolean {
        val removed = layoutRepository.removeItemAt(x, y, screen)
        if (removed) {
            eventPublisher.publishLayoutChanged()
        }
        return removed
    }
}
