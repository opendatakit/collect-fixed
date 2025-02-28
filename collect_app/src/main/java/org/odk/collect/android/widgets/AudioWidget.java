/*
 * Copyright (C) 2018 Shobhit Agarwal
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

package org.odk.collect.android.widgets;

import android.annotation.SuppressLint;
import android.content.Context;
import android.media.MediaMetadataRetriever;
import android.view.LayoutInflater;
import android.view.View;

import org.javarosa.core.model.data.IAnswerData;
import org.javarosa.core.model.data.StringData;
import org.javarosa.form.api.FormEntryPrompt;
import org.odk.collect.android.audio.AudioControllerView;
import org.odk.collect.android.databinding.AudioWidgetAnswerBinding;
import org.odk.collect.android.formentry.questions.QuestionDetails;
import org.odk.collect.android.utilities.Appearances;
import org.odk.collect.android.utilities.QuestionMediaManager;
import org.odk.collect.android.widgets.interfaces.FileWidget;
import org.odk.collect.android.widgets.interfaces.WidgetDataReceiver;
import org.odk.collect.android.widgets.utilities.AudioFileRequester;
import org.odk.collect.android.widgets.utilities.AudioPlayer;
import org.odk.collect.android.widgets.utilities.RecordingRequester;
import org.odk.collect.android.widgets.utilities.RecordingStatusHandler;
import org.odk.collect.audioclips.Clip;

import java.io.File;
import java.util.Locale;

import timber.log.Timber;

import static org.odk.collect.strings.format.LengthFormatterKt.formatLength;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

/**
 * Widget that allows user to take pictures, sounds or video and add them to the
 * form.
 *
 * @author Carl Hartung (carlhartung@gmail.com)
 * @author Yaw Anokwa (yanokwa@gmail.com)
 */

@SuppressLint("ViewConstructor")
public class AudioWidget extends QuestionWidget implements FileWidget, WidgetDataReceiver {

    AudioWidgetAnswerBinding binding;

    private final AudioPlayer audioPlayer;
    private final RecordingRequester recordingRequester;
    private final QuestionMediaManager questionMediaManager;
    private final AudioFileRequester audioFileRequester;

    private boolean recordingInProgress;
    private String binaryName;

    public AudioWidget(Context context, QuestionDetails questionDetails, QuestionMediaManager questionMediaManager, AudioPlayer audioPlayer, RecordingRequester recordingRequester, AudioFileRequester audioFileRequester, RecordingStatusHandler recordingStatusHandler, Dependencies dependencies) {
        super(context, dependencies, questionDetails);
        render();

        this.audioPlayer = audioPlayer;

        this.questionMediaManager = questionMediaManager;
        this.recordingRequester = recordingRequester;
        this.audioFileRequester = audioFileRequester;

        binaryName = questionDetails.getPrompt().getAnswerText();
        updateVisibilities();
        updatePlayerMedia();

        recordingStatusHandler.onBlockedStatusChange(isRecordingBlocked -> {
            binding.recordAudioButton.setEnabled(!isRecordingBlocked);
            binding.chooseAudioButton.setEnabled(!isRecordingBlocked);
        });

        recordingStatusHandler.onRecordingStatusChange(getFormEntryPrompt(), session -> {
            if (session != null) {
                recordingInProgress = true;
                updateVisibilities();

                binding.audioPlayer.recordingDuration.setText(formatLength(session.first));
                binding.audioPlayer.waveform.addAmplitude(session.second);
            } else {
                recordingInProgress = false;
                updateVisibilities();
                updatePlayerMedia();
            }
        });
    }

    @Override
    protected View onCreateAnswerView(Context context, FormEntryPrompt prompt, int answerFontSize) {
        binding = AudioWidgetAnswerBinding.inflate(LayoutInflater.from(context));

        binding.recordAudioButton.setOnClickListener(v -> {
            hideError();
            binding.audioPlayer.waveform.clear();
            recordingRequester.requestRecording(getFormEntryPrompt());
        });
        binding.chooseAudioButton.setOnClickListener(v -> audioFileRequester.requestFile(getFormEntryPrompt()));

        return binding.getRoot();
    }

    @Override
    public void deleteFile() {
        audioPlayer.stop();
        questionMediaManager.deleteAnswerFile(getFormEntryPrompt().getIndex().toString(), getAudioFile().getAbsolutePath());
        binaryName = null;
    }

    @Override
    public void clearAnswer() {
        deleteFile();
        widgetValueChanged();
        updateVisibilities();
    }

    @Override
    public IAnswerData getAnswer() {
        if (binaryName != null) {
            return new StringData(binaryName);
        } else {
            return null;
        }
    }

    @Override
    public void setData(Object object) {
        if (object instanceof File) {
            File newAudio = (File) object;
            if (newAudio.exists()) {
                if (binaryName != null) {
                    deleteFile();
                }

                questionMediaManager.replaceAnswerFile(getFormEntryPrompt().getIndex().toString(), newAudio.getAbsolutePath());
                binaryName = newAudio.getName();
                updateVisibilities();
                updatePlayerMedia();
                widgetValueChanged();
            } else {
                Timber.e(new Error("NO AUDIO EXISTS at: " + newAudio.getAbsolutePath()));
            }
        } else {
            Timber.e(new Error("AudioWidget's setBinaryData must receive a File object."));
        }
    }

    private void updateVisibilities() {
        if (recordingInProgress) {
            binding.recordAudioButton.setVisibility(GONE);
            binding.chooseAudioButton.setVisibility(GONE);
            binding.audioPlayer.recordingDuration.setVisibility(VISIBLE);
            binding.audioPlayer.waveform.setVisibility(VISIBLE);
            binding.audioPlayer.audioController.setVisibility(GONE);
        } else if (getAnswer() == null) {
            binding.recordAudioButton.setVisibility(VISIBLE);
            binding.chooseAudioButton.setVisibility(VISIBLE);
            binding.audioPlayer.recordingDuration.setVisibility(GONE);
            binding.audioPlayer.waveform.setVisibility(GONE);
            binding.audioPlayer.audioController.setVisibility(GONE);
        } else {
            binding.recordAudioButton.setVisibility(GONE);
            binding.chooseAudioButton.setVisibility(GONE);
            binding.audioPlayer.recordingDuration.setVisibility(GONE);
            binding.audioPlayer.waveform.setVisibility(GONE);
            binding.audioPlayer.audioController.setVisibility(VISIBLE);
        }

        if (questionDetails.isReadOnly()) {
            binding.recordAudioButton.setVisibility(GONE);
            binding.chooseAudioButton.setVisibility(GONE);
        }

        if (getFormEntryPrompt().getAppearanceHint() != null && getFormEntryPrompt().getAppearanceHint().toLowerCase(Locale.ENGLISH).contains(Appearances.NEW)) {
            binding.chooseAudioButton.setVisibility(GONE);
        }
    }

    private void updatePlayerMedia() {
        if (binaryName != null) {
            Clip clip = new Clip("audio:" + getFormEntryPrompt().getIndex().toString(), getAudioFile().getAbsolutePath());

            audioPlayer.onPlayingChanged(clip.getClipID(), binding.audioPlayer.audioController::setPlaying);
            audioPlayer.onPositionChanged(clip.getClipID(), binding.audioPlayer.audioController::setPosition);
            binding.audioPlayer.audioController.setDuration(getDurationOfFile(clip.getURI()));
            binding.audioPlayer.audioController.setListener(new AudioControllerView.Listener() {
                @Override
                public void onPlayClicked() {
                    audioPlayer.play(clip);
                }

                @Override
                public void onPauseClicked() {
                    audioPlayer.pause();
                }

                @Override
                public void onPositionChanged(Integer newPosition) {
                    audioPlayer.setPosition(clip.getClipID(), newPosition);
                }

                @Override
                public void onRemoveClicked() {
                    new MaterialAlertDialogBuilder(getContext())
                            .setTitle(org.odk.collect.strings.R.string.delete_answer_file_question)
                            .setMessage(org.odk.collect.strings.R.string.answer_file_delete_warning)
                            .setPositiveButton(org.odk.collect.strings.R.string.delete_answer_file, (dialog, which) -> clearAnswer())
                            .setNegativeButton(org.odk.collect.strings.R.string.cancel, null)
                            .show();
                }
            });

        }
    }

    private Integer getDurationOfFile(String uri) {
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        retriever.setDataSource(uri);
        String durationString = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
        return durationString != null ? Integer.parseInt(durationString) : 0;
    }

    @Override
    public void setOnLongClickListener(OnLongClickListener l) {
        binding.recordAudioButton.setOnLongClickListener(l);
        binding.chooseAudioButton.setOnLongClickListener(l);
    }

    @Override
    public void cancelLongPress() {
        super.cancelLongPress();
        binding.recordAudioButton.cancelLongPress();
        binding.chooseAudioButton.cancelLongPress();
    }

    /**
     * Returns the audio file added to the widget for the current instance
     */
    private File getAudioFile() {
        return questionMediaManager.getAnswerFile(binaryName);
    }
}
