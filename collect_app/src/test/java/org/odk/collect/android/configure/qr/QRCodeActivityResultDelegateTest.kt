package org.odk.collect.android.configure.qr

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.apache.commons.io.IOUtils
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.equalTo
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.odk.collect.android.injection.config.AppDependencyModule
import org.odk.collect.android.support.CollectHelpers
import org.odk.collect.projects.Project.Saved
import org.odk.collect.projects.ProjectConfigurationResult
import org.odk.collect.qrcode.QRCodeDecoder
import org.odk.collect.settings.ODKAppSettingsImporter
import org.robolectric.Robolectric
import org.robolectric.Shadows
import org.robolectric.shadows.ShadowToast
import java.io.ByteArrayInputStream
import java.io.IOException
import java.io.InputStream
import java.lang.Exception
import java.lang.RuntimeException
import java.util.HashMap

@RunWith(AndroidJUnit4::class)
class QRCodeActivityResultDelegateTest {
    private val fakeQRDecoder = FakeQRDecoder()
    private val settingsImporter = mock<ODKAppSettingsImporter>()
    private val project = mock<Saved>()

    private lateinit var context: Activity

    @Before
    fun setup() {
        CollectHelpers.overrideAppDependencyModule(object : AppDependencyModule() {
            override fun providesQRCodeDecoder(): QRCodeDecoder {
                return fakeQRDecoder
            }
        })
        context = Robolectric.buildActivity(Activity::class.java).get()
    }

    @Test
    fun forSelectPhoto_importsSettingsFromQRCode_showsSuccessToast() {
        importSettingsFromQRCode_successfully()
        assertThat(
            ShadowToast.getTextOfLatestToast(),
            equalTo(
                context.getString(org.odk.collect.strings.R.string.successfully_imported_settings)
            )
        )
    }

    private fun importSettingsFromQRCode_successfully() {
        val delegate = QRCodeActivityResultDelegate(context, settingsImporter, fakeQRDecoder, project)
        val data = intentWithData("file://qr", "qr")
        fakeQRDecoder.register("qr", "data")
        whenever(settingsImporter.fromJSON("data", project)).thenReturn(ProjectConfigurationResult.SUCCESS)
        delegate.onActivityResult(QRCodeMenuProvider.SELECT_PHOTO, Activity.RESULT_OK, data)
    }

    @Test
    fun forSelectPhoto_whenImportingFails_showsInvalidToast() {
        importSettingsFromQRCode_withFailedImport()
        assertThat(ShadowToast.getTextOfLatestToast(), equalTo(context.getString(org.odk.collect.strings.R.string.invalid_qrcode)))
    }

    private fun importSettingsFromQRCode_withFailedImport() {
        val delegate = QRCodeActivityResultDelegate(context, settingsImporter, fakeQRDecoder, project)
        val data = intentWithData("file://qr", "qr")
        fakeQRDecoder.register("qr", "data")
        whenever(settingsImporter.fromJSON("data", project)).thenReturn(ProjectConfigurationResult.INVALID_SETTINGS)
        delegate.onActivityResult(QRCodeMenuProvider.SELECT_PHOTO, Activity.RESULT_OK, data)
    }

    @Test
    fun forSelectPhotoWithGoogleDriveProtocol_whenImporting_showsInvalidToast() {
        val delegate = QRCodeActivityResultDelegate(context, settingsImporter, fakeQRDecoder, project)
        val data = intentWithData("file://qr", "qr")
        fakeQRDecoder.register("qr", "data")
        whenever(settingsImporter.fromJSON("data", project)).thenReturn(ProjectConfigurationResult.GD_PROJECT)
        delegate.onActivityResult(QRCodeMenuProvider.SELECT_PHOTO, Activity.RESULT_OK, data)

        assertThat(ShadowToast.getTextOfLatestToast(), equalTo(context.getString(org.odk.collect.strings.R.string.settings_with_gd_protocol)))
    }

    @Test
    fun forSelectPhoto_whenQRCodeDecodeFailsWithInvalid_showsInvalidToast() {
        importSettingsFromQrCode_withInvalidQrCode()
        assertThat(ShadowToast.getTextOfLatestToast(), equalTo(context.getString(org.odk.collect.strings.R.string.invalid_qrcode)))
    }

    private fun importSettingsFromQrCode_withInvalidQrCode() {
        val delegate = QRCodeActivityResultDelegate(context, settingsImporter, fakeQRDecoder, project)
        val data = intentWithData("file://qr", "qr")
        fakeQRDecoder.failsWith(QRCodeDecoder.QRCodeInvalidException())
        whenever(settingsImporter.fromJSON("data", project)).thenReturn(ProjectConfigurationResult.INVALID_SETTINGS)
        delegate.onActivityResult(QRCodeMenuProvider.SELECT_PHOTO, Activity.RESULT_OK, data)
    }

    @Test
    fun forSelectPhoto_whenQRCodeDecodeFailsWithNotFound_showsNoQRToast() {
        importSettingsFromImage_withoutQrCode()
        assertThat(ShadowToast.getTextOfLatestToast(), equalTo(context.getString(org.odk.collect.strings.R.string.qr_code_not_found)))
    }

    private fun importSettingsFromImage_withoutQrCode() {
        val delegate = QRCodeActivityResultDelegate(context, settingsImporter, fakeQRDecoder, project)
        val data = intentWithData("file://qr", "qr")
        fakeQRDecoder.failsWith(QRCodeDecoder.QRCodeNotFoundException())
        whenever(settingsImporter.fromJSON("data", project)).thenReturn(ProjectConfigurationResult.INVALID_SETTINGS)
        delegate.onActivityResult(QRCodeMenuProvider.SELECT_PHOTO, Activity.RESULT_OK, data)
    }

    @Test
    fun forSelectPhoto_whenDataIsNull_doesNothing() {
        val delegate = QRCodeActivityResultDelegate(context, settingsImporter, fakeQRDecoder, project)
        delegate.onActivityResult(QRCodeMenuProvider.SELECT_PHOTO, Activity.RESULT_OK, null)
    }

    @Test
    fun forSelectPhoto_whenResultCancelled_doesNothing() {
        val delegate = QRCodeActivityResultDelegate(context, settingsImporter, fakeQRDecoder, project)
        delegate.onActivityResult(
            QRCodeMenuProvider.SELECT_PHOTO,
            Activity.RESULT_CANCELED,
            Intent()
        )
    }

    private fun intentWithData(uri: String, streamContents: String): Intent {
        val inputStream = ByteArrayInputStream(streamContents.toByteArray())
        Shadows.shadowOf(ApplicationProvider.getApplicationContext<Context>().contentResolver)
            .registerInputStream(
                Uri.parse("file://qr"),
                inputStream
            )
        val data = Intent()
        data.data = Uri.parse(uri)
        return data
    }

    private class FakeQRDecoder : QRCodeDecoder {
        private val data: MutableMap<String, String> = HashMap()
        private var failsWith: Exception? = null

        @Throws(QRCodeDecoder.QRCodeInvalidException::class, QRCodeDecoder.QRCodeNotFoundException::class)
        override fun decode(inputStream: InputStream?): String {
            if (failsWith != null) {
                if (failsWith is QRCodeDecoder.QRCodeInvalidException) {
                    throw (failsWith as QRCodeDecoder.QRCodeInvalidException?)!!
                } else {
                    throw (failsWith as QRCodeDecoder.QRCodeNotFoundException?)!!
                }
            }
            return try {
                val streamData = IOUtils.toString(inputStream)
                val decoded = data[streamData]
                    ?: throw RuntimeException("No decoded data specified for $streamData")
                decoded
            } catch (e: IOException) {
                throw RuntimeException(e)
            }
        }

        fun register(streamData: String, decodedData: String) {
            data[streamData] = decodedData
        }

        fun failsWith(exception: Exception?) {
            failsWith = exception
        }
    }
}
