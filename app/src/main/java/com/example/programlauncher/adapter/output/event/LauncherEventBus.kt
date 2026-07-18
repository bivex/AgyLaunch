package com.example.programlauncher.adapter.output.event

import com.example.programlauncher.domain.port.output.LauncherEventPublisher
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

class LauncherEventBus : LauncherEventPublisher {
    private val _events = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val events: SharedFlow<Unit> = _events.asSharedFlow()

    override fun publishLayoutChanged() {
        _events.tryEmit(Unit)
    }
}
