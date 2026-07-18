package com.example.programlauncher.adapter.input.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.programlauncher.DependencyContainer
import com.example.programlauncher.domain.model.GridPosition
import com.example.programlauncher.domain.model.LauncherLayout
import com.example.programlauncher.domain.port.inputput.*
import com.example.programlauncher.domain.port.output.AppDetail
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class LauncherViewModel(
    private val getLauncherLayoutUseCase: GetLauncherLayoutUseCase,
    private val getInstalledAppsUseCase: GetInstalledAppsUseCase,
    private val addShortcutUseCase: AddShortcutUseCase,
    private val removeShortcutUseCase: RemoveShortcutUseCase,
    private val clearLayoutUseCase: ClearLayoutUseCase,
    private val eventBus: com.example.programlauncher.adapter.output.event.LauncherEventBus
) : ViewModel() {

    private val _layoutState = MutableStateFlow<LauncherLayout?>(null)
    val layoutState: StateFlow<LauncherLayout?> = _layoutState.asStateFlow()

    private val _installedApps = MutableStateFlow<List<AppDetail>>(emptyList())
    val installedApps: StateFlow<List<AppDetail>> = _installedApps.asStateFlow()

    init {
        loadLayout()
        loadInstalledApps()

        // Listen for layout changes published by receivers or other use cases
        viewModelScope.launch {
            eventBus.events.collect {
                loadLayout()
            }
        }
    }

    fun loadLayout() {
        viewModelScope.launch {
            val layout = getLauncherLayoutUseCase.execute()
            _layoutState.value = layout
        }
    }

    fun loadInstalledApps() {
        viewModelScope.launch {
            val apps = getInstalledAppsUseCase.execute()
            _installedApps.value = apps
        }
    }

    fun addShortcut(packageName: String, x: Int, y: Int, screen: Int = 0) {
        viewModelScope.launch {
            addShortcutUseCase.execute(packageName, GridPosition(x, y, screen))
        }
    }

    fun removeShortcut(packageName: String) {
        viewModelScope.launch {
            removeShortcutUseCase.execute(packageName)
        }
    }

    fun removeShortcutAt(x: Int, y: Int, screen: Int = 0) {
        viewModelScope.launch {
            removeShortcutUseCase.executeAt(x, y, screen)
        }
    }

    fun clearLayout() {
        viewModelScope.launch {
            clearLayoutUseCase.execute()
        }
    }

    class Factory(private val container: DependencyContainer) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return LauncherViewModel(
                container.getLauncherLayoutUseCase,
                container.getInstalledAppsUseCase,
                container.addShortcutUseCase,
                container.removeShortcutUseCase,
                container.clearLayoutUseCase,
                container.eventBus
            ) as T
        }
    }
}
