package com.spqrta.cloudvideo.repository

import com.spqrta.camera2demo.utility.pure.FileUtils
import com.spqrta.camera2demo.utility.pure.FileUtils.size
import com.spqrta.cloudvideo.MyApplication
import java.io.File

object AppRepository {
    fun getVideos(): List<File> {
        return FileUtils.listFiles(MyApplication.VIDEOS_FOLDER).filter { it.size() > 0 }
    }

    fun getUnsyncedVideos(): List<File> {
        return getVideos().filter { !it.name.contains("synced") }
    }

    fun markSynced(file: File) {
        file.renameTo(File(file.name.replace(".mp4", "")[0]+"_synced.mp4"))
    }


}