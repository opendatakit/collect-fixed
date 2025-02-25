/*
 * Copyright 2017 Nafundi
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.odk.collect.selfiecamera

import android.Manifest
import android.os.Bundle
import android.view.View
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import org.odk.collect.androidshared.ui.ToastUtils.showLongToast
import org.odk.collect.externalapp.ExternalAppUtils
import org.odk.collect.permissions.PermissionsChecker
import org.odk.collect.strings.localization.LocalizedActivity
import javax.inject.Inject

class CaptureSelfieActivity : LocalizedActivity() {

    @Inject
    internal lateinit var camera: Camera

    @Inject
    lateinit var permissionsChecker: PermissionsChecker

    override fun onCreate(savedInstanceState: Bundle?) {
        (application as SelfieCameraDependencyComponentProvider)
            .selfieCameraDependencyComponent
            .inject(this)

        super.onCreate(savedInstanceState)

        if (!permissionsGranted()) {
            finish()
            return
        }

        val insetsController = WindowCompat.getInsetsController(window, window.decorView)
        insetsController.hide(WindowInsetsCompat.Type.statusBars())
        setContentView(R.layout.activity_capture_selfie)

        val previewView = findViewById<View>(R.id.preview)

        camera.initialize(this, previewView)
        camera.state().observe(this) {
            when (it) {
                Camera.State.UNINITIALIZED -> {}
                Camera.State.INITIALIZED -> setupCamera(camera)
                Camera.State.FAILED_TO_INITIALIZE -> {
                    showLongToast(org.odk.collect.strings.R.string.camera_failed_to_initialize)
                }
            }
        }
    }

    private fun setupCamera(camera: Camera) {
        val previewView = findViewById<View>(R.id.preview)

        val imagePath = intent.getStringExtra(EXTRA_TMP_PATH) + "/tmp.jpg"
        previewView.setOnClickListener {
            camera.takePicture(
                imagePath,
                { ExternalAppUtils.returnSingleValue(this, imagePath) },
                { showLongToast(org.odk.collect.strings.R.string.camera_error) }
            )
        }

        showLongToast(org.odk.collect.strings.R.string.take_picture_instruction)
    }

    private fun permissionsGranted(): Boolean {
        return permissionsChecker.isPermissionGranted(Manifest.permission.CAMERA)
    }

    companion object {
        const val EXTRA_TMP_PATH = "tmpPath"
    }
}
