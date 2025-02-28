package org.odk.collect.android.formentry

import android.view.View
import android.widget.TextView
import androidx.test.core.app.ApplicationProvider
import org.javarosa.core.reference.InvalidReferenceException
import org.javarosa.core.reference.ReferenceManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.odk.collect.android.R
import org.odk.collect.android.formentry.questions.AudioVideoImageTextLabel
import org.odk.collect.android.logic.FileReference
import org.odk.collect.android.support.CollectHelpers
import org.odk.collect.android.support.WidgetTestActivity
import org.robolectric.ParameterizedRobolectricTestRunner
import java.io.File
import java.util.Random

@RunWith(ParameterizedRobolectricTestRunner::class)
class AudioVideoImageTextLabelVisibilityTest(
    private val imageURI: String?,
    private val audioURI: String?,
    private val videoURI: String?
) {

    private lateinit var referenceManager: ReferenceManager
    private lateinit var reference: FileReference
    private lateinit var audioVideoImageTextLabel: AudioVideoImageTextLabel
    private lateinit var audioButton: View
    private lateinit var videoButton: View
    private lateinit var imageView: View
    private lateinit var missingImage: View
    private lateinit var textView: TextView
    private var isReferenceManagerStubbed = false
    private var imageFileExists = false

    @Before
    fun setUp() {
        reference = mock()
        referenceManager = mock()
        textView = TextView(ApplicationProvider.getApplicationContext())
        val activity = CollectHelpers.createThemedActivity(
            WidgetTestActivity::class.java
        )
        audioVideoImageTextLabel = AudioVideoImageTextLabel(activity)
        audioButton = audioVideoImageTextLabel.findViewById(R.id.audioButton)
        videoButton = audioVideoImageTextLabel.findViewById(R.id.videoButton)
        imageView = audioVideoImageTextLabel.findViewById(R.id.imageView)
        missingImage = audioVideoImageTextLabel.findViewById(R.id.missingImage)

        /*
         * Stub reference manager randomly to account for both illegal URI and proper URI while
         * attempting to load image view
         */
        if (Random().nextBoolean()) {
            stubReferenceManager()
            imageFileExists = true
        } else {
            whenever(referenceManager.deriveReference(RANDOM_URI)).thenThrow(
                InvalidReferenceException::class.java
            )
        }
    }

    @Test
    fun viewShouldBecomeVisibleIfUriPresent() {
        assertNotNull(audioVideoImageTextLabel)
        assertEquals(View.VISIBLE, audioVideoImageTextLabel.visibility)
        assertVisibility(View.GONE, audioButton, videoButton, imageView, missingImage)
        val imageFile: File
        val videoFile: File
        audioVideoImageTextLabel.setTextView(textView)
        if (imageURI != null && isReferenceManagerStubbed) {
            imageFile = mock()
            whenever(imageFile.exists()).thenReturn(imageFileExists)
            audioVideoImageTextLabel.setImage(imageFile, mock())
        }
        if (videoURI != null && isReferenceManagerStubbed) {
            videoFile = mock()
            audioVideoImageTextLabel.setVideo(videoFile)
        }
        if (audioURI != null && isReferenceManagerStubbed) {
            audioVideoImageTextLabel.setAudio(audioURI, mock())
        }

        // we do not check for the validity of the URIs for the audio and video while loading MediaLayout
        assertVisibility(
            if (audioURI == null || !isReferenceManagerStubbed) View.GONE else View.VISIBLE,
            audioButton
        )
        assertVisibility(
            if (videoURI == null || !isReferenceManagerStubbed) View.GONE else View.VISIBLE,
            videoButton
        )
        if (imageURI == null || !isReferenceManagerStubbed) {
            // either the URI wasn't provided or it encountered InvalidReferenceException
            assertVisibility(View.GONE, imageView, missingImage)
        } else if (imageFileExists) {
            assertVisibility(View.VISIBLE, imageView)
            assertVisibility(View.GONE, missingImage)
        } else {
            assertVisibility(View.GONE, imageView)
            assertVisibility(View.VISIBLE, missingImage)
        }
    }

    /*
     * Stubbing {@link ReferenceManager} to return random file name in order to prevent
     * {@link InvalidReferenceException}
     */
    private fun stubReferenceManager() {
        isReferenceManagerStubbed = true
        whenever(referenceManager.deriveReference(RANDOM_URI)).doReturn(reference)
        whenever(reference.localURI).doReturn(RANDOM_URI)
    }

    /**
     * @param visibility Expected visibility
     * @param views      Views whose actual visibility is to be asserted
     */
    private fun assertVisibility(visibility: Int, vararg views: View) {
        for (view in views) {
            assertEquals(visibility, view.visibility)
        }
    }

    companion object {
        private const val RANDOM_URI = "randomMediaURI"

        @ParameterizedRobolectricTestRunner.Parameters
        @JvmStatic
        fun data() = listOf<Array<String?>>(
            arrayOf(null, null, null),
            arrayOf(RANDOM_URI, null, null),
            arrayOf(null, RANDOM_URI, null),
            arrayOf(null, null, RANDOM_URI),
            arrayOf(RANDOM_URI, RANDOM_URI, null),
            arrayOf(RANDOM_URI, null, RANDOM_URI),
            arrayOf(null, RANDOM_URI, RANDOM_URI),
            arrayOf(RANDOM_URI, RANDOM_URI, RANDOM_URI)
        )
    }
}
