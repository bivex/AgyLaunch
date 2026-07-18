package com.example.programlauncher.adapter.output.persistence

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class LauncherDbHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(SQL_CREATE_GRID_CONFIG)
        db.execSQL(SQL_CREATE_SHORTCUTS)
        
        // Initialize default grid size (e.g. 4x5)
        db.execSQL("INSERT INTO grid_config (cols, rows) VALUES (4, 5)")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS shortcuts")
        db.execSQL("DROP TABLE IF EXISTS grid_config")
        onCreate(db)
    }

    companion object {
        const val DATABASE_VERSION = 1
        const val DATABASE_NAME = "Launcher.db"

        private const val SQL_CREATE_GRID_CONFIG = """
            CREATE TABLE grid_config (
                id INTEGER PRIMARY KEY DEFAULT 1,
                cols INTEGER NOT NULL,
                rows INTEGER NOT NULL
            )
        """

        private const val SQL_CREATE_SHORTCUTS = """
            CREATE TABLE shortcuts (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                package_name TEXT NOT NULL,
                class_name TEXT NOT NULL,
                label TEXT NOT NULL,
                screen INTEGER NOT NULL,
                cell_x INTEGER NOT NULL,
                cell_y INTEGER NOT NULL,
                item_type INTEGER DEFAULT 0
            )
        """
    }
}
