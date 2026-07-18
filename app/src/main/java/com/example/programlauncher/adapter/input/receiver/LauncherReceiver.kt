package com.example.programlauncher.adapter.input.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.programlauncher.LauncherApplication
import com.example.programlauncher.domain.model.GridPosition
import com.example.programlauncher.domain.model.GridSize
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class LauncherReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val app = context.applicationContext as LauncherApplication
        val container = app.dependencyContainer
        val action = intent.action ?: return

        Log.d("LauncherReceiver", "Received broadcast action: $action")

        // Offload database operations to IO dispatcher
        CoroutineScope(Dispatchers.IO).launch {
            try {
                when (action) {
                    "com.example.programlauncher.ADD_SHORTCUT" -> {
                        val packageName = intent.getStringExtra("package")
                        val x = intent.getIntExtra("x", -1)
                        val y = intent.getIntExtra("y", -1)
                        val screen = intent.getIntExtra("screen", 0)

                        if (packageName.isNullOrEmpty() || x == -1 || y == -1) {
                            Log.e("LauncherReceiver", "Missing parameters for ADD_SHORTCUT. 'package', 'x' and 'y' are required.")
                            return@launch
                        }

                        val position = GridPosition(x, y, screen)
                        val result = container.addShortcutUseCase.execute(packageName, position)
                        if (result.isSuccess) {
                            Log.i("LauncherReceiver", "Successfully added shortcut for $packageName at ($x, $y) on screen $screen")
                        } else {
                            Log.e("LauncherReceiver", "Failed to add shortcut: ${result.exceptionOrNull()?.message}")
                        }
                    }

                    "com.example.programlauncher.REMOVE_SHORTCUT" -> {
                        val packageName = intent.getStringExtra("package")
                        val x = intent.getIntExtra("x", -1)
                        val y = intent.getIntExtra("y", -1)
                        val screen = intent.getIntExtra("screen", 0)

                        val success = if (!packageName.isNullOrEmpty()) {
                            container.removeShortcutUseCase.execute(packageName)
                        } else if (x != -1 && y != -1) {
                            container.removeShortcutUseCase.executeAt(x, y, screen)
                        } else {
                            Log.e("LauncherReceiver", "Missing parameters for REMOVE_SHORTCUT. Provide either 'package' or ('x' and 'y').")
                            false
                        }

                        if (success) {
                            Log.i("LauncherReceiver", "Removed shortcut successfully.")
                        } else {
                            Log.w("LauncherReceiver", "No shortcut was removed.")
                        }
                    }

                    "com.example.programlauncher.SET_GRID" -> {
                        val cols = intent.getIntExtra("cols", -1)
                        val rows = intent.getIntExtra("rows", -1)

                        if (cols <= 0 || rows <= 0) {
                            Log.e("LauncherReceiver", "Invalid grid size values cols: $cols, rows: $rows")
                            return@launch
                        }

                        val result = container.setGridSizeUseCase.execute(GridSize(cols, rows))
                        if (result.isSuccess) {
                            Log.i("LauncherReceiver", "Updated grid size to $cols x $rows")
                        } else {
                            Log.e("LauncherReceiver", "Failed to update grid size: ${result.exceptionOrNull()?.message}")
                        }
                    }

                    "com.example.programlauncher.CLEAR_LAYOUT" -> {
                        container.clearLayoutUseCase.execute()
                        Log.i("LauncherReceiver", "Launcher layout cleared successfully.")
                    }
                }
            } catch (e: Exception) {
                Log.e("LauncherReceiver", "Error processing action $action: ${e.message}", e)
            }
        }
    }
}
