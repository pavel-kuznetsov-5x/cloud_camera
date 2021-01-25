package com.spqrta.cloudvideo

import android.content.ContentResolver
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Pair
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.api.client.http.ByteArrayContent
import com.google.api.client.http.FileContent
import com.google.api.services.drive.Drive
import com.google.api.services.drive.model.FileList
import com.spqrta.cloudvideo.utility.pure.Stub
import java.io.BufferedReader
import java.io.File
import java.io.IOException
import java.io.InputStreamReader
import java.util.concurrent.Callable
import java.util.concurrent.Executor
import java.util.concurrent.Executors

class DriveApiWrapper(private val driveService: Drive) {
    private val mExecutor: Executor =
        Executors.newSingleThreadExecutor()

    ///////////////////////////////////////////////////////////////////////////
    // C
    ///////////////////////////////////////////////////////////////////////////

    //todo mime
    fun createFile(
        file: File,
        folder: String
    ): Task<String> {
        return Tasks.call(mExecutor, Callable {
            val metadata =
                com.google.api.services.drive.model.File()
                    .setParents(listOf(folder))
                    .setMimeType("video/mp4")
                    .setName(file.name)
            val googleFile = driveService.files()
                .create(metadata, FileContent("video/mp4", file))
                .execute()
                ?: throw IOException("Null result when requesting file creation.")
            googleFile.id
        })
    }

    fun createFolder(
        file: File,
        parentId: String?
    ): Task<String> {
        return Tasks.call(mExecutor, Callable {
            val metadata =
                com.google.api.services.drive.model.File()
                    .setMimeType("application/vnd.google-apps.folder")
                    .setName(file.name)
            if (parentId == null) {
                metadata.parents = listOf("root")
            } else {
                metadata.parents = listOf(parentId)
            }
            val googleFile =
                driveService.files().create(metadata).execute()
                    ?: throw IOException("Null result when requesting file creation.")
            googleFile.id
        })
    }

    ///////////////////////////////////////////////////////////////////////////
    // R
    ///////////////////////////////////////////////////////////////////////////

    fun getFiles(folderId: String): Task<FileList> {
        return Tasks.call(
            mExecutor, Callable {
                val res = driveService.files()
                    .list()
                    .setSpaces("drive")
                    .setQ("'$folderId' in parents and trashed = false")
                    .setFields("nextPageToken, files(id, name, size, capabilities)")
                    .execute()
                res
            }
        )
    }

    fun getFolders(folderId: String): Task<FileList> {
        return Tasks.call(
            mExecutor, Callable {
                val res = driveService.files()
                    .list()
                    .setSpaces("drive")
                    .setQ("'$folderId' in parents and trashed = false and mimeType = 'application/vnd.google-apps.folder'")
                    .setFields("nextPageToken, files(id, name, size, capabilities)")
                    .execute()
                res
            }
        )
    }

    fun searchFile(
        file: File,
        parentId: String? = null
    ): Task<com.google.api.services.drive.model.File?> {
        return Tasks.call(mExecutor,
            Callable<com.google.api.services.drive.model.File?> {
                var pageToken: String? = null
                do {
                    val list =
                        driveService.files().list()
                            .setSpaces("drive")
                            .setFields("nextPageToken, files(id, name, size, capabilities)")
                            .setPageToken(pageToken)
                    if (parentId != null) {
                        list.q =
                            "name='" + file.name + "' and '" + parentId + "' in parents and trashed = false"
                    } else {
                        list.q = "name='" + file.name + "' and trashed = false"
                    }
                    val result = list.execute()
                    for (f in result.files) {
                        if (!f.capabilities.canAddChildren) {
                            return@Callable f
                        }
                    }
                    pageToken = result.nextPageToken
                } while (pageToken != null)
                null
            }
        )
    }

    fun searchFolder(
        file: File,
        parentId: String?
    ): Task<String?> {
        return Tasks.call(mExecutor, Callable<String?> {
            var pageToken: String? = null
            do {
                val list =
                    driveService.files().list()
                        .setSpaces("drive")
                        .setFields("nextPageToken, files(id, name, capabilities)")
                        .setPageToken(pageToken)
                if (parentId != null) {
                    list.q =
                        "name='" + file.name + "' and '" + parentId + "' in parents and trashed = false"
                } else {
                    list.q = "name='" + file.name + "' and trashed = false"
                }
                val result = list.execute()
                for (f in result.files) {
                    if (f.capabilities.canAddChildren) {
                        return@Callable f.id
                    }
                }
                pageToken = result.nextPageToken
            } while (pageToken != null)
            return@Callable null
        })
    }

    fun readFile(fileId: String?): Task<Pair<String, String>> {
        return Tasks.call(mExecutor,
            Callable<Pair<String, String>> {
                val metadata =
                    driveService.files()[fileId].execute()
                val name = metadata.name
                driveService.files()[fileId].executeMediaAsInputStream().use { `is` ->
                    BufferedReader(InputStreamReader(`is`)).use { reader ->
                        val stringBuilder = StringBuilder()
                        var line: String?
                        while (reader.readLine().also { line = it } != null) {
                            stringBuilder.append(line)
                        }
                        val contents = stringBuilder.toString()
                        return@Callable Pair(name, contents)
                    }
                }
            }
        )
    }

    fun deleteFile(id: String?): Task<Stub> {
        return Tasks.call(mExecutor, Callable {
            driveService.files().delete(id)
                .execute()
            return@Callable Stub
        })
    }

}