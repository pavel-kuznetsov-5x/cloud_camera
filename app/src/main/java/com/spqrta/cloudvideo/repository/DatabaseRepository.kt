package com.spqrta.cloudvideo.repository

import com.google.gson.Gson
import com.spqrta.camera2demo.utility.pure.Setting
import com.spqrta.camera2demo.utility.pure.StringSetSetting
import java.io.File

object DatabaseRepository {

    private val uploadIds = object: Setting<Map<String, String>>() {
        override val key: String
            get() = "uploadIds"

        override fun load(): Map<String, String> {
            return Gson().fromJson(prefs.getString(key, "{}"), MutableMap::class.java) as MutableMap<String, String>
        }

        override fun save(value: Map<String, String>) {
            prefs.edit().putString(key, Gson().toJson(value)).apply()
        }
    }

    private val syncedFiles = object: StringSetSetting() {
        override val key: String
            get() = "syncedFiles"
    }

    fun getUploadId(file: File): String? {
        return uploadIds.load()[file.name]
    }

    fun saveUploadId(file: File, uploadId: String) {
        uploadIds.save(uploadIds.load().toMutableMap().also { it[file.name] = uploadId })
    }

    fun deleteUploadId(file: File) {
        uploadIds.save(uploadIds.load().toMutableMap().also { it.remove(file.name) })
    }

    fun getSyncedFiles(): Set<String> {
        return syncedFiles.load() ?: setOf()
    }

    fun addSyncedFile(file: File) {
        syncedFiles.save(HashSet(syncedFiles.load() ?: setOf()).apply { add(file.name) })
    }


}