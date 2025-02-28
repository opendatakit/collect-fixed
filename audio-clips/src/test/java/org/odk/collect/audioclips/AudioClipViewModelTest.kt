package org.odk.collect.audioclips

import android.media.MediaPlayer
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.equalTo
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers
import org.mockito.Mockito.doThrow
import org.mockito.Mockito.inOrder
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.odk.collect.androidtest.getOrAwaitValue
import org.odk.collect.testshared.FakeScheduler
import java.io.File
import java.io.IOException
import java.util.function.Supplier

@RunWith(AndroidJUnit4::class)
class AudioClipViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val mediaPlayer = mock(MediaPlayer::class.java)
    private val fakeScheduler = FakeScheduler()
    private var viewModel: AudioClipViewModel = AudioClipViewModel(Supplier { mediaPlayer }, fakeScheduler)

    @Test
    fun play_resetsAndPreparesAndStartsMediaPlayer() {
        viewModel.play(Clip("clip1", "file://audio.mp3"))
        fakeScheduler.flush()

        val inOrder = inOrder(mediaPlayer)
        inOrder.verify(mediaPlayer).reset()
        inOrder.verify(mediaPlayer).setDataSource("file://audio.mp3")
        inOrder.verify(mediaPlayer).prepare()
        inOrder.verify(mediaPlayer).start()
    }

    @Test
    fun play_whenAlreadyingPlayingClip_startsMediaPlayer() {
        viewModel.play(Clip("clip1", "file://audio.mp3"))
        fakeScheduler.flush()
        viewModel.play(Clip("clip1", "file://audio.mp3"))
        fakeScheduler.flush()

        verify(mediaPlayer, times(1)).reset()
        verify(mediaPlayer, times(2)).start()
    }

    @Test
    fun play_whenClipHasPositionSet_startsAtPosition() {
        viewModel.setPosition("clip1", 4321)
        viewModel.play(Clip("clip1", "file://audio.mp3"))
        fakeScheduler.flush()

        val inOrder = inOrder(mediaPlayer)
        inOrder.verify(mediaPlayer).seekTo(4321)
        inOrder.verify(mediaPlayer).start()
    }

    @Test
    fun playMultipleClips_updatesProgress_forAllClips() {
        viewModel.play(Clip("clip1", "file://audio.mp3"))
        fakeScheduler.flush()

        assertThat(fakeScheduler.isRepeatRunning(), equalTo(true))

        viewModel.onCleared()
        viewModel.play(Clip("clip1", "file://audio.mp3"))
        fakeScheduler.flush()
        assertThat(fakeScheduler.isRepeatRunning(), equalTo(true))
    }

    @Test
    fun playInOrder_playsClipsOneAfterTheOther_andUpdatesProgress() {
        viewModel.playInOrder(
            listOf(
                Clip("clip1", "file://audio1.mp3"),
                Clip("clip2", "file://audio2.mp3")
            )
        )
        fakeScheduler.flush()

        val captor = ArgumentCaptor.forClass(MediaPlayer.OnCompletionListener::class.java)
        verify(mediaPlayer).setOnCompletionListener(captor.capture())
        val onCompletionListener = captor.value

        verify(mediaPlayer).setDataSource("file://audio1.mp3")
        verify(mediaPlayer, times(1)).start()
        assertThat(fakeScheduler.isRepeatRunning(), equalTo(true))

        onCompletionListener.onCompletion(mediaPlayer)
        fakeScheduler.flush()
        verify(mediaPlayer).setDataSource("file://audio2.mp3")
        verify(mediaPlayer, times(2)).start()
        assertThat(fakeScheduler.isRepeatRunning(), equalTo(true))

        onCompletionListener.onCompletion(mediaPlayer)
        fakeScheduler.flush()
        verify(mediaPlayer, times(2)).start()
        assertThat(fakeScheduler.isRepeatRunning(), equalTo(false))
    }

    @Test
    fun playInOrder_whenThereIsAnErrorContinuesWithNextClip() {
        doThrow(IOException::class.java).`when`(mediaPlayer).setDataSource("file://missing.mp3")
        viewModel.playInOrder(
            listOf(
                Clip("clip1", "file://missing.mp3"),
                Clip("clip2", "file://not-missing.mp3")
            )
        )
        fakeScheduler.flush()
        fakeScheduler.flush()

        verify(mediaPlayer).setDataSource("file://not-missing.mp3")
        verify(mediaPlayer, times(1)).start()
    }

    @Test
    fun play_afterAPlayInOrder_doesNotContinuePlayingClips() {
        viewModel.playInOrder(
            listOf(
                Clip("clip1", "file://audio1.mp3"),
                Clip("clip2", "file://audio2.mp3")
            )
        )
        fakeScheduler.flush()

        viewModel.play(Clip("clip3", "file://audio3.mp3"))
        fakeScheduler.flush()

        verify(mediaPlayer, times(2)).start()
        verify(mediaPlayer).setDataSource("file://audio1.mp3")
        verify(mediaPlayer).setDataSource("file://audio3.mp3")

        val captor = ArgumentCaptor.forClass(MediaPlayer.OnCompletionListener::class.java)
        verify(mediaPlayer).setOnCompletionListener(captor.capture())
        val onCompletionListener = captor.value
        onCompletionListener.onCompletion(mediaPlayer)
        fakeScheduler.flush()

        verify(mediaPlayer, never()).setDataSource("file://audio2.mp3")
        verify(mediaPlayer, times(2)).start()
    }

    @Test
    fun isPlaying_whenNothingPlaying_is_false() {
        val isPlaying = viewModel.isPlaying("clip1")
        assertThat(isPlaying.getOrAwaitValue(), equalTo(false))
    }

    @Test
    fun isPlaying_whenClipIDPlaying_is_PLAYING() {
        val isPlaying = viewModel.isPlaying("clip1")

        viewModel.play(Clip("clip1", "file://audio.mp3"))
        fakeScheduler.flush()
        assertThat(isPlaying.getOrAwaitValue(), equalTo(true))
    }

    @Test
    fun isPlaying_whenDifferentClipIDPlaying_is_false() {
        val isPlaying = viewModel.isPlaying("clip2")

        viewModel.play(Clip("clip1", "file://other.mp3"))
        fakeScheduler.flush()
        assertThat(isPlaying.getOrAwaitValue(), equalTo(false))
    }

    @Test
    fun isPlaying_whenClipIDPlaying_thenCompleted_is_false() {
        val isPlaying = viewModel.isPlaying("clip1")
        viewModel.play(Clip("clip1", "file://audio.mp3"))
        fakeScheduler.flush()

        val captor = ArgumentCaptor.forClass(MediaPlayer.OnCompletionListener::class.java)
        verify(mediaPlayer).setOnCompletionListener(captor.capture())
        captor.value.onCompletion(mediaPlayer)
        assertThat(isPlaying.getOrAwaitValue(), equalTo(false))
    }

    @Test
    fun isPlaying_whenPlaybackFails_is_false() {
        doThrow(IOException::class.java).`when`(mediaPlayer).setDataSource("file://missing.mp3")
        val isPlaying = viewModel.isPlaying("clip1")

        viewModel.play(Clip("clip1", "file://missing.mp3"))
        fakeScheduler.flush()
        assertThat(isPlaying.getOrAwaitValue(), equalTo(false))
    }

    @Test
    fun stop_stopsMediaPlayer() {
        viewModel.play(Clip("clip1", "file://audio.mp3"))
        fakeScheduler.flush()

        viewModel.stop()
        verify(mediaPlayer).stop()
    }

    @Test
    fun stop_beforePlay_doesntCallStopOnMediaPlayer() {
        viewModel.stop()
        verify(mediaPlayer, never()).stop()
    }

    @Test
    fun stop_resetsPosition() {
        val position = viewModel.getPosition("clip1")
        viewModel.play(Clip("clip1", "file://audio.mp3"))
        fakeScheduler.flush()

        `when`(mediaPlayer.currentPosition).thenReturn(1000)
        fakeScheduler.runForeground()
        viewModel.stop()
        assertThat(position.getOrAwaitValue(), equalTo(0))
    }

    @Test
    fun background_releasesMediaPlayer() {
        viewModel.play(Clip("clip1", "file://audio.mp3"))
        fakeScheduler.flush()

        viewModel.background()
        verify(mediaPlayer).release()
    }

    @Test
    fun background_cancelsScheduler() {
        viewModel.play(Clip("clip1", "file://audio.mp3"))
        fakeScheduler.flush()

        viewModel.background()
        assertThat(fakeScheduler.isRepeatRunning(), equalTo(false))
    }

    @Test
    fun isPlaying_whenPlayingAndThenBackgrounding_is_false() {
        val isPlaying = viewModel.isPlaying("clip1")
        viewModel.play(Clip("clip1", "file://audio.mp3"))
        fakeScheduler.flush()

        viewModel.background()
        assertThat(isPlaying.getOrAwaitValue(), equalTo(false))
    }

    @Test
    fun play_afterBackground_createsANewMediaPlayer() {
        val factory = RecordingMockMediaPlayerFactory()
        val scheduler = FakeScheduler()
        val viewModel = AudioClipViewModel(factory, scheduler)
        viewModel.play(Clip("clip1", "file://audio.mp3"))
        scheduler.flush()
        assertThat(factory.createdInstances.size, equalTo(1))
        verify(factory.createdInstances[0]).start()

        viewModel.background()
        viewModel.play(Clip("clip1", "file://audio.mp3"))
        scheduler.flush()
        assertThat(factory.createdInstances.size, equalTo(2))
        verify(factory.createdInstances[1]).start()
    }

    @Test
    fun pause_pausesMediaPlayer() {
        viewModel.play(Clip("clip1", "file://audio.mp3"))
        fakeScheduler.flush()

        viewModel.pause()
        verify(mediaPlayer).pause()
    }

    @Test
    fun isPlaying_afterPause_is_false() {
        val isPlaying = viewModel.isPlaying("clip1")
        viewModel.play(Clip("clip1", "file://audio.mp3"))
        fakeScheduler.flush()

        viewModel.pause()
        assertThat(isPlaying.getOrAwaitValue(), equalTo(false))
    }

    @Test
    fun isPlaying_afterPause_andThenPlay_is_true() {
        val isPlaying = viewModel.isPlaying("clip1")
        viewModel.play(Clip("clip1", "file://audio.mp3"))
        fakeScheduler.flush()

        viewModel.pause()
        viewModel.play(Clip("clip1", "file://audio.mp3"))
        fakeScheduler.flush()

        assertThat(isPlaying.getOrAwaitValue(), equalTo(true))
    }

    @Test
    fun position_returnsMediaPlayerPositionInMilliseconds() {
        `when`(mediaPlayer.currentPosition).thenReturn(0)
        val position = viewModel.getPosition("clip1")
        assertThat(position.getOrAwaitValue(), equalTo(0))

        viewModel.play(Clip("clip1", "file://audio.mp3"))
        fakeScheduler.flush()

        `when`(mediaPlayer.currentPosition).thenReturn(1000)
        fakeScheduler.runForeground()
        assertThat(position.value, equalTo(1000))

        `when`(mediaPlayer.currentPosition).thenReturn(24135)
        fakeScheduler.runForeground()
        assertThat(position.value, equalTo(24135))
    }

    @Test
    fun position_worksWhenMultipleClipsArePlayed() {
        `when`(mediaPlayer.currentPosition).thenReturn(0)
        val position1 = viewModel.getPosition("clip1")
        val position2 = viewModel.getPosition("clip2")

        viewModel.play(Clip("clip1", "file://audio.mp3"))
        fakeScheduler.flush()

        `when`(mediaPlayer.currentPosition).thenReturn(1000)
        fakeScheduler.runForeground()
        assertThat(position1.getOrAwaitValue(), equalTo(1000))
        assertThat(position2.getOrAwaitValue(), equalTo(0))

        viewModel.play(Clip("clip2", "file://audio.mp3"))
        fakeScheduler.flush()

        `when`(mediaPlayer.currentPosition).thenReturn(2500)
        fakeScheduler.runForeground()
        assertThat(position1.getOrAwaitValue(), equalTo(1000))
        assertThat(position2.getOrAwaitValue(), equalTo(2500))
    }

    @Test
    fun setPosition_whenClipIsPlaying_seeksMediaPlayer() {
        `when`(mediaPlayer.duration).thenReturn(100000)
        viewModel.play(Clip("clip1", "file://audio.mp3"))
        fakeScheduler.flush()

        viewModel.setPosition("clip1", 54321)
        verify(mediaPlayer).seekTo(54321)
    }

    @Test
    fun setPosition_whenClipIsNotPlaying_doesNothing() {
        viewModel.setPosition("clip1", 54321)
        verify(mediaPlayer, never()).seekTo(ArgumentMatchers.anyInt())
    }

    @Test
    fun setPosition_updatesPosition() {
        val duration = viewModel.getPosition("clip1")
        viewModel.setPosition("clip1", 54321)
        assertThat(duration.getOrAwaitValue(), equalTo(54321))
    }

    @Test
    fun onCleared_releasesMediaPlayer() {
        viewModel.play(Clip("clip1", "file://audio.mp3"))
        fakeScheduler.flush()

        viewModel.onCleared()
        verify(mediaPlayer).release()
    }

    @Test
    fun onCleared_cancelsScheduler() {
        viewModel.play(Clip("clip1", "file://audio.mp3"))
        fakeScheduler.flush()

        viewModel.onCleared()
        assertThat(fakeScheduler.isRepeatRunning(), equalTo(false))
    }

    @Test
    fun whenPlaybackCompletes_cancelsScheduler() {
        viewModel.play(Clip("clip1", "file://audio.mp3"))
        fakeScheduler.flush()

        val captor = ArgumentCaptor.forClass(MediaPlayer.OnCompletionListener::class.java)
        verify(mediaPlayer).setOnCompletionListener(captor.capture())
        captor.value.onCompletion(mediaPlayer)
        assertThat(fakeScheduler.isRepeatRunning(), equalTo(false))
    }

    @Test
    fun whenPlaybackCompletes_resetsPosition() {
        val position = viewModel.getPosition("clip1")
        viewModel.play(Clip("clip1", "file://audio.mp3"))
        fakeScheduler.flush()

        `when`(mediaPlayer.currentPosition).thenReturn(1000)
        fakeScheduler.runForeground()
        val captor = ArgumentCaptor.forClass(MediaPlayer.OnCompletionListener::class.java)
        verify(mediaPlayer).setOnCompletionListener(captor.capture())
        captor.value.onCompletion(mediaPlayer)
        assertThat(position.getOrAwaitValue(), equalTo(0))
    }

    @Test
    fun error_whenPlaybackFailsBecauseOfMissingFile_is_PlaybackFailed() {
        val error = viewModel.getError()
        doThrow(IOException::class.java).`when`(mediaPlayer).setDataSource("file://missing.mp3")
        viewModel.play(Clip("clip1", "file://missing.mp3"))
        fakeScheduler.flush()

        assertThat(error.getOrAwaitValue()!!.value, equalTo(PlaybackFailedException("file://missing.mp3", 0)))
    }

    @Test
    fun error_whenPlaybackFailsBecauseOfInvalidFile_is_PlaybackFailed() {
        val error = viewModel.getError()
        val invalid = File.createTempFile("invalid", ".mp3")
        doThrow(IOException::class.java).`when`(mediaPlayer).setDataSource(invalid.absolutePath)
        viewModel.play(Clip("clip1", invalid.absolutePath))
        fakeScheduler.flush()

        assertThat(error.getOrAwaitValue()!!.value, equalTo(PlaybackFailedException(invalid.absolutePath, 1)))
    }

    @Test
    fun isLoading_isTrueWhenLoadingClip() {
        val loading = viewModel.isLoading()
        assertThat(loading.getOrAwaitValue(), equalTo(false))

        viewModel.play(Clip("clip1", "file://missing.mp3"))
        assertThat(loading.getOrAwaitValue(), equalTo(true))

        fakeScheduler.flush()
        assertThat(loading.getOrAwaitValue(), equalTo(false))
    }

    private class RecordingMockMediaPlayerFactory : Supplier<MediaPlayer> {
        var createdInstances: MutableList<MediaPlayer> = ArrayList()
        override fun get(): MediaPlayer {
            val mock = mock(MediaPlayer::class.java)
            createdInstances.add(mock)
            return mock
        }
    }
}
