package com.example.programlauncher.adapter.output.persistence

import android.content.ContentValues
import android.content.Context
import com.example.programlauncher.domain.model.GridPosition
import com.example.programlauncher.domain.model.GridSize
import com.example.programlauncher.domain.model.LauncherItem
import com.example.programlauncher.domain.port.output.LauncherLayoutRepository

class SqliteLauncherLayoutRepository(context: Context) : LauncherLayoutRepository {
    private val dbHelper = LauncherDbHelper(context)

    override fun getGridSize(): GridSize {
        val db = dbHelper.readableDatabase
        val cursor = db.rawQuery("SELECT cols, rows FROM grid_config LIMIT 1", null)
        var cols = 4
        var rows = 5
        cursor.use {
            if (it.moveToFirst()) {
                cols = it.getInt(0)
                rows = it.getInt(1)
            }
        }
        return GridSize(cols, rows)
    }

    override fun setGridSize(gridSize: GridSize) {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put("cols", gridSize.columns)
            put("rows", gridSize.rows)
        }
        db.update("grid_config", values, null, null)
    }

    override fun getItems(): List<LauncherItem> {
        val db = dbHelper.readableDatabase
        val cursor = db.rawQuery("SELECT id, package_name, class_name, label, screen, cell_x, cell_y, item_type FROM shortcuts", null)
        val items = mutableListOf<LauncherItem>()
        cursor.use {
            while (it.moveToNext()) {
                val id = it.getLong(0)
                val packageName = it.getString(1)
                val className = it.getString(2)
                val label = it.getString(3)
                val screen = it.getInt(4)
                val cellX = it.getInt(5)
                val cellY = it.getInt(6)
                val itemType = it.getInt(7)

                if (itemType == 0) {
                    items.add(
                        LauncherItem.AppShortcut(
                            id = id,
                            packageName = packageName,
                            className = className,
                            label = label,
                            position = GridPosition(cellX, cellY, screen)
                        )
                    )
                } else if (itemType == 1) {
                    val itemsList = mutableListOf<LauncherItem.AppShortcut>()
                    if (packageName.isNotEmpty()) {
                        try {
                            val array = org.json.JSONArray(packageName)
                            for (i in 0 until array.length()) {
                                val obj = array.getJSONObject(i)
                                itemsList.add(
                                    LauncherItem.AppShortcut(
                                        packageName = obj.getString("package"),
                                        className = obj.getString("class"),
                                        label = obj.getString("label"),
                                        position = GridPosition(-1, -1, -1)
                                    )
                                )
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                    items.add(
                        LauncherItem.Folder(
                            id = id,
                            label = label,
                            position = GridPosition(cellX, cellY, screen),
                            items = itemsList
                        )
                    )
                }
            }
        }
        return items
    }

    override fun saveItem(item: LauncherItem): Result<Long> {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            when (item) {
                is LauncherItem.AppShortcut -> {
                    put("package_name", item.packageName)
                    put("class_name", item.className)
                    put("label", item.label)
                    put("screen", item.position.screen)
                    put("cell_x", item.position.x)
                    put("cell_y", item.position.y)
                    put("item_type", 0)
                }
                is LauncherItem.Folder -> {
                    val array = org.json.JSONArray()
                    item.items.forEach { shortcut ->
                        val obj = org.json.JSONObject().apply {
                            put("package", shortcut.packageName)
                            put("class", shortcut.className)
                            put("label", shortcut.label)
                        }
                        array.put(obj)
                    }
                    put("package_name", array.toString())
                    put("class_name", "")
                    put("label", item.label)
                    put("screen", item.position.screen)
                    put("cell_x", item.position.x)
                    put("cell_y", item.position.y)
                    put("item_type", 1)
                }
            }
        }

        return if (item.id != null) {
            val rows = db.update("shortcuts", values, "id = ?", arrayOf(item.id.toString()))
            if (rows > 0) {
                Result.success(item.id!!)
            } else {
                val id = db.insert("shortcuts", null, values)
                if (id != -1L) Result.success(id) else Result.failure(Exception("Failed to save layout item."))
            }
        } else {
            val id = db.insert("shortcuts", null, values)
            if (id != -1L) {
                Result.success(id)
            } else {
                Result.failure(Exception("Failed to insert item into SQLite database."))
            }
        }
    }

    override fun removeItem(packageName: String): Boolean {
        val db = dbHelper.writableDatabase
        val count = db.delete("shortcuts", "package_name = ?", arrayOf(packageName))
        return count > 0
    }

    override fun removeItemAt(x: Int, y: Int, screen: Int): Boolean {
        val db = dbHelper.writableDatabase
        val count = db.delete(
            "shortcuts",
            "cell_x = ? AND cell_y = ? AND screen = ?",
            arrayOf(x.toString(), y.toString(), screen.toString())
        )
        return count > 0
    }

    override fun clearLayout() {
        val db = dbHelper.writableDatabase
        db.delete("shortcuts", null, null)
    }
}
