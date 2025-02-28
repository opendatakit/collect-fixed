package org.odk.collect.android.widgets.support

import org.odk.collect.android.widgets.utilities.AudioPlayer
import org.odk.collect.audioclips.Clip
import java.util.function.Consumer

class FakeAudioPlayer : AudioPlayer {
    private val playingChangedListeners: MutableMap<String, Consumer<Boolean>> = HashMap()
    private val positionChangedListeners: MutableMap<String, Consumer<Int>> = HashMap()
    private val positions: MutableMap<String, Int> = HashMap()

    var isPaused: Boolean = false
        private set
    var currentClip: Clip? = null
        private set

    override fun play(clip: Clip) {
        this.currentClip = clip
        isPaused = false
        playingChangedListeners[clip.clipID]!!.accept(true)
    }

    override fun pause() {
        isPaused = true
        playingChangedListeners[currentClip!!.clipID]!!.accept(false)
    }

    override fun setPosition(clipId: String, position: Int) {
        positions[clipId] = position
        positionChangedListeners[clipId]!!.accept(position)
    }

    override fun onPlayingChanged(clipID: String, playingConsumer: Consumer<Boolean>) {
        playingChangedListeners[clipID] = playingConsumer
    }

    override fun onPositionChanged(clipID: String, positionConsumer: Consumer<Int>) {
        positionChangedListeners[clipID] = positionConsumer
    }

    override fun onPlaybackError(error: Consumer<Exception>) {
        throw UnsupportedOperationException()
    }

    override fun stop() {
        currentClip?.also {
            playingChangedListeners[it.clipID]?.accept(false)
        }

        currentClip = null
    }

    override fun playInOrder(clips: List<Clip>) {
        throw UnsupportedOperationException()
    }

    fun getPosition(clipId: String): Int? {
        return positions[clipId]
    }
}
