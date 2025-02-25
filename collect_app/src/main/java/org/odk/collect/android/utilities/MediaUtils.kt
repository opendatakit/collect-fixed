/*
 * Copyright (C) 2009 University of Washington
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package org.odk.collect.android.utilities

import android.app.Activity
import android.content.Context
import android.content.Intent
import org.odk.collect.android.BuildConfig
import org.odk.collect.androidshared.system.IntentLauncher
import org.odk.collect.androidshared.ui.ToastUtils
import timber.log.Timber
import java.io.File

/**
 * Consolidate all interactions with media providers here.
 *
 *
 * The functionality of getPath() was provided by paulburke as described here:
 * See
 * http://stackoverflow.com/questions/20067508/get-real-path-from-uri-android
 * -kitkat-new-storage-access-framework for details
 *
 * @author mitchellsundt@gmail.com
 * @author paulburke
 */
class MediaUtils(private val intentLauncher: IntentLauncher, private val contentUriProvider: ContentUriProvider) {
    fun deleteMediaFile(imageFile: String) {
        FileUtils.deleteAndReport(File(imageFile))
    }

    fun openFile(context: Context, file: File, expectedMimeType: String?) {
        if (!file.exists()) {
            val errorMsg: String = context.getString(org.odk.collect.strings.R.string.file_missing, file)
            Timber.d("File %s is missing", file)
            ToastUtils.showLongToast(errorMsg)
            return
        }

        val contentUri = contentUriProvider.getUriForFile(
            context,
            BuildConfig.APPLICATION_ID + ".provider",
            file
        )

        if (contentUri == null) {
            ToastUtils.showLongToast("Can't open file. If you are on a Huawei device, this is expected and will not be fixed.")
            return
        }

        val intent = Intent().apply {
            action = Intent.ACTION_VIEW
            setDataAndType(contentUri, getMimeType(file, expectedMimeType))
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        intentLauncher.launch(context, intent) {
            val message = context.getString(
                org.odk.collect.strings.R.string.activity_not_found,
                context.getString(org.odk.collect.strings.R.string.open_file)
            )
            ToastUtils.showLongToast(message)
            Timber.w(message)
        }
    }

    private fun getMimeType(file: File, expectedMimeType: String?) =
        if (expectedMimeType == null || expectedMimeType.isEmpty()) {
            FileUtils.getMimeType(file)
        } else {
            expectedMimeType
        }

    fun pickFile(activity: Activity, mimeType: String, requestCode: Int) {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = mimeType
        }

        intentLauncher.launchForResult(activity, intent, requestCode) {
            // Do nothing
        }
    }

    fun isVideoFile(file: File): Boolean {
        return FileUtils.getMimeType(file).startsWith("video")
    }

    fun isImageFile(file: File): Boolean {
        return FileUtils.getMimeType(file).startsWith("image")
    }

    fun isAudioFile(file: File): Boolean {
        return FileUtils.getMimeType(file).startsWith("audio")
    }
}
