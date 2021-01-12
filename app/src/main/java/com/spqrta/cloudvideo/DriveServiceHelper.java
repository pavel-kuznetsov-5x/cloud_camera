/**
 * Copyright 2018 Google LLC
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.spqrta.cloudvideo;

import android.content.ContentResolver;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.provider.OpenableColumns;
import android.util.Pair;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.api.client.http.ByteArrayContent;
import com.google.api.client.http.FileContent;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Collections;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;


public class DriveServiceHelper {
    private final Executor mExecutor = Executors.newSingleThreadExecutor();
    private final Drive driveService;

    public DriveServiceHelper(Drive driveService) {
        this.driveService = driveService;
    }

    public Task<String> createFolder(java.io.File file) {
        return Tasks.call(mExecutor, () -> {
            File metadata = new File()
                    .setParents(Collections.singletonList("root"))
                    .setMimeType("application/vnd.google-apps.folder")
                    .setName(file.getName());

            File googleFile = driveService.files().create(metadata).execute();
            if (googleFile == null) {
                throw new IOException("Null result when requesting file creation.");
            }

            return googleFile.getId();
        });
    }

    public Task<String> searchFolder(java.io.File file) {
        return Tasks.call(mExecutor, () -> {
            String pageToken = null;
            do {
                FileList result = driveService.files().list()
                        .setQ("name='" + file.getName() + "'")
                        .setSpaces("drive")
                        .setFields("nextPageToken, files(id, name, capabilities)")
                        .setPageToken(pageToken)
                        .execute();

                for (File f : result.getFiles()) {
                    if (f.getCapabilities().getCanAddChildren()) {
                        return f.getId();
                    }
                }
                pageToken = result.getNextPageToken();
            } while (pageToken != null);
            return null;
        });
    }


    //todo mime
    public Task<String> createFile(java.io.File file, String folder) {
        return Tasks.call(mExecutor, () -> {
            File metadata = new File()
                    .setParents(Collections.singletonList(folder))
                    .setMimeType("video/mp4")
                    .setName(file.getName());

            File googleFile = driveService.files()
                    .create(metadata, new FileContent("video/mp4", file))
                    .execute();
            if (googleFile == null) {
                throw new IOException("Null result when requesting file creation.");
            }

            return googleFile.getId();
        });
    }

    public Task<Pair<String, String>> readFile(String fileId) {
        return Tasks.call(mExecutor, () -> {
            // Retrieve the metadata as a File object.
            File metadata = driveService.files().get(fileId).execute();
            String name = metadata.getName();

            // Stream the file contents to a String.
            try (InputStream is = driveService.files().get(fileId).executeMediaAsInputStream();
                 BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
                StringBuilder stringBuilder = new StringBuilder();
                String line;

                while ((line = reader.readLine()) != null) {
                    stringBuilder.append(line);
                }
                String contents = stringBuilder.toString();

                return new Pair(name, contents);
            }
        });
    }

    public Task<Void> saveFile(String fileId, String name, String content) {
        return Tasks.call(mExecutor, () -> {
            // Create a File containing any metadata changes.
            File metadata = new File().setName(name);

            // Convert content to an AbstractInputStreamContent instance.
            ByteArrayContent contentStream = ByteArrayContent.fromString("text/plain", content);

            // Update the metadata and contents.
            driveService.files().update(fileId, metadata, contentStream).execute();
            return null;
        });
    }

    public Task<FileList> queryFiles(String folderId) {
        return Tasks.call(mExecutor, () -> {
                    FileList res = driveService.files()
                            .list()
                            .setSpaces("drive")
                            .setQ("'" + folderId + "' in parents and trashed = false")
                            .execute();
                    return res;
                }
        );
    }

    /**
     * Returns an {@link Intent} for opening the Storage Access Framework file picker.
     */
    public Intent createFilePickerIntent() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("text/plain");

        return intent;
    }

    /**
     * Opens the file at the {@code uri} returned by a Storage Access Framework {@link Intent}
     * created by {@link #createFilePickerIntent()} using the given {@code contentResolver}.
     */
    public Task<Pair<String, String>> openFileUsingStorageAccessFramework(
            ContentResolver contentResolver, Uri uri) {
        return Tasks.call(mExecutor, () -> {
            // Retrieve the document's display name from its metadata.
            String name;
            try (Cursor cursor = contentResolver.query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    name = cursor.getString(nameIndex);
                } else {
                    throw new IOException("Empty cursor returned for file.");
                }
            }

            // Read the document's contents as a String.
            String content;
            try (InputStream is = contentResolver.openInputStream(uri);
                 BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
                StringBuilder stringBuilder = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    stringBuilder.append(line);
                }
                content = stringBuilder.toString();
            }

            return Pair.create(name, content);
        });
    }
}
