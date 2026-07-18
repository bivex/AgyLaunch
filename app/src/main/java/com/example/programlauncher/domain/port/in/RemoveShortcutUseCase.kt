package com.example.programlauncher.domain.port.`in`

interface RemoveShortcutUseCase {
    fun execute(packageName: String): Boolean
    fun executeAt(x: Int, y: Int, screen: Int): Boolean
}
