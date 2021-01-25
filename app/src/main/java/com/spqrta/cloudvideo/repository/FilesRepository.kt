package com.spqrta.cloudvideo.repository

import com.spqrta.cloudvideo.utility.pure.FileUtils
import com.spqrta.cloudvideo.utility.pure.FileUtils.size
import com.spqrta.cloudvideo.MyApplication
import java.io.File

object FilesRepository {

    fun getVideos(): List<File> {
        return FileUtils.listFiles(MyApplication.VIDEOS_FOLDER).filter { it.size() > 0 }
    }

    fun getUnsyncedVideos(): List<File> {
        val syncedFiles = DatabaseRepository.getSyncedFiles()
        return getVideos().filter { !syncedFiles.contains(it.name) }
    }

}