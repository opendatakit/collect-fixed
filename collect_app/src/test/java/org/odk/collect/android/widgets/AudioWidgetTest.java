package org.odk.collect.android.widgets;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.odk.collect.android.widgets.support.QuestionWidgetHelpers.mockValueChangedListener;
import static org.odk.collect.android.widgets.support.QuestionWidgetHelpers.promptWithAnswer;
import static org.odk.collect.android.widgets.support.QuestionWidgetHelpers.promptWithReadOnly;
import static org.odk.collect.android.widgets.support.QuestionWidgetHelpers.promptWithReadOnlyAndAnswer;
import static org.odk.collect.android.widgets.support.QuestionWidgetHelpers.widgetDependencies;
import static org.odk.collect.testshared.RobolectricHelpers.setupMediaPlayerDataSource;
import static org.robolectric.shadows.ShadowDialog.getLatestDialog;

import android.util.Pair;
import android.view.View;

import androidx.appcompat.app.AlertDialog;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.javarosa.core.model.FormIndex;
import org.javarosa.core.model.data.StringData;
import org.javarosa.form.api.FormEntryPrompt;
import org.jetbrains.annotations.NotNull;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.odk.collect.android.audio.AudioControllerView;
import org.odk.collect.android.formentry.questions.QuestionDetails;
import org.odk.collect.android.listeners.WidgetValueChangedListener;
import org.odk.collect.android.support.CollectHelpers;
import org.odk.collect.android.support.WidgetTestActivity;
import org.odk.collect.android.utilities.Appearances;
import org.odk.collect.android.widgets.support.FakeAudioPlayer;
import org.odk.collect.android.widgets.support.FakeQuestionMediaManager;
import org.odk.collect.android.widgets.utilities.AudioFileRequester;
import org.odk.collect.android.widgets.utilities.RecordingRequester;
import org.odk.collect.android.widgets.utilities.RecordingStatusHandler;
import org.odk.collect.audioclips.Clip;
import org.odk.collect.testshared.RobolectricHelpers;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

@RunWith(AndroidJUnit4.class)
public class AudioWidgetTest {

    private final FakeQuestionMediaManager questionMediaManager = new FakeQuestionMediaManager();
    private final FakeRecordingRequester recordingRequester = new FakeRecordingRequester();
    private final AudioFileRequester audioFileRequester = mock(AudioFileRequester.class);

    private WidgetTestActivity widgetActivity;
    private FormIndex formIndex;
    private FakeAudioPlayer audioPlayer;

    @Before
    public void setUp() throws Exception {
        widgetActivity = CollectHelpers.buildThemedActivity(WidgetTestActivity.class).get();

        formIndex = mock(FormIndex.class);
        when(formIndex.toString()).thenReturn("questionIndex");

        audioPlayer = new FakeAudioPlayer();
    }

    @Test
    public void whenPromptDoesNotHaveAnswer_showsButtons() {
        AudioWidget widget = createWidget(promptWithAnswer(null));

        assertThat(widget.binding.audioPlayer.audioController.getVisibility(), is(GONE));
        assertThat(widget.binding.audioPlayer.recordingDuration.getVisibility(), is(GONE));
        assertThat(widget.binding.audioPlayer.waveform.getVisibility(), is(GONE));
        assertThat(widget.binding.recordAudioButton.getVisibility(), is(VISIBLE));
        assertThat(widget.binding.chooseAudioButton.getVisibility(), is(VISIBLE));
    }

    @Test
    public void whenPromptHasAnswer_showsAudioController() throws Exception {
        File answerFile = questionMediaManager.addAnswerFile(File.createTempFile("blah", ".mp3"));
        AudioWidget widget = createWidget(promptWithAnswer(new StringData(answerFile.getName())));

        assertThat(widget.binding.recordAudioButton.getVisibility(), is(GONE));
        assertThat(widget.binding.chooseAudioButton.getVisibility(), is(GONE));
        assertThat(widget.binding.audioPlayer.waveform.getVisibility(), is(GONE));
        assertThat(widget.binding.audioPlayer.recordingDuration.getVisibility(), is(GONE));
        assertThat(widget.binding.audioPlayer.audioController.getVisibility(), is(VISIBLE));
    }

    @Test
    public void usingReadOnlyOption_doesNotShowCaptureAndChooseButtons() {
        AudioWidget widget = createWidget(promptWithReadOnly());
        assertThat(widget.binding.recordAudioButton.getVisibility(), equalTo(GONE));
        assertThat(widget.binding.chooseAudioButton.getVisibility(), equalTo(GONE));
    }

    @Test
    public void getAnswer_whenPromptDoesNotHaveAnswer_returnsNull() {
        AudioWidget widget = createWidget(promptWithAnswer(null));
        assertThat(widget.getAnswer(), nullValue());
    }

    @Test
    public void getAnswer_whenPromptHasAnswer_returnsAnswer() throws Exception {
        File answerFile = questionMediaManager.addAnswerFile(File.createTempFile("blah", ".mp3"));
        AudioWidget widget = createWidget(promptWithAnswer(new StringData(answerFile.getName())));
        assertThat(widget.getAnswer().getDisplayText(), equalTo(answerFile.getName()));
    }

    @Test
    public void whenWidgetIsNew_chooseSoundButtonIsNotShown() {
        FormEntryPrompt prompt = promptWithReadOnly();
        when(prompt.getAppearanceHint()).thenReturn(Appearances.NEW);
        AudioWidget widget = createWidget(prompt);

        assertThat(widget.binding.chooseAudioButton.getVisibility(), equalTo(GONE));
    }

    @Test
    public void deleteFile_removesWidgetAnswerAndStopsPlayingMedia() throws Exception {
        File answerFile = questionMediaManager.addAnswerFile(File.createTempFile("blah", ".mp3"));
        AudioWidget widget = createWidget(promptWithAnswer(new StringData(answerFile.getName())));

        widget.deleteFile();

        assertThat(widget.getAnswer(), nullValue());
        assertThat(audioPlayer.getCurrentClip(), nullValue());
    }

    @Test
    public void deleteFile_setsFileAsideForDeleting() throws Exception {
        File answerFile = questionMediaManager.addAnswerFile(File.createTempFile("blah", ".mp3"));
        FormEntryPrompt prompt = promptWithAnswer(new StringData(answerFile.getName()));
        when(prompt.getIndex()).thenReturn(formIndex);

        AudioWidget widget = createWidget(prompt);
        widget.deleteFile();

        assertThat(questionMediaManager.originalFiles.get("questionIndex"), equalTo(answerFile.getAbsolutePath()));
    }

    @Test
    public void clearAnswer_removesAnswerAndHidesPlayer() throws Exception {
        File answerFile = questionMediaManager.addAnswerFile(File.createTempFile("blah", ".mp3"));
        AudioWidget widget = createWidget(promptWithAnswer(new StringData(answerFile.getName())));
        widget.clearAnswer();

        assertThat(widget.getAnswer(), nullValue());
        assertThat(widget.binding.audioPlayer.audioController.getVisibility(), is(GONE));
    }

    @Test
    public void clearAnswer_setsFileAsideForDeleting() throws Exception {
        File answerFile = questionMediaManager.addAnswerFile(File.createTempFile("blah", ".mp3"));
        FormEntryPrompt prompt = promptWithAnswer(new StringData(answerFile.getName()));
        when(prompt.getIndex()).thenReturn(formIndex);

        AudioWidget widget = createWidget(prompt);
        widget.clearAnswer();

        assertThat(questionMediaManager.originalFiles.get("questionIndex"), equalTo(answerFile.getAbsolutePath()));
    }

    @Test
    public void clearAnswer_callsValueChangeListeners() throws Exception {
        File answerFile = questionMediaManager.addAnswerFile(File.createTempFile("blah", ".mp3"));
        AudioWidget widget = createWidget(promptWithAnswer(new StringData(answerFile.getName())));
        WidgetValueChangedListener valueChangedListener = mockValueChangedListener(widget);
        widget.clearAnswer();

        verify(valueChangedListener).widgetValueChanged(widget);
    }

    @Test
    public void setData_whenFileExists_replacesOriginalFileWithNewFile() throws Exception {
        File answerFile = questionMediaManager.addAnswerFile(File.createTempFile("blah1", ".mp3"));
        FormEntryPrompt prompt = promptWithAnswer(new StringData(answerFile.getName()));
        when(prompt.getIndex()).thenReturn(formIndex);
        AudioWidget widget = createWidget(prompt);

        File newFile = questionMediaManager.addAnswerFile(File.createTempFile("blah2", ".mp3"));
        widget.setData(newFile);

        assertThat(questionMediaManager.recentFiles.get("questionIndex"), equalTo(newFile.getAbsolutePath()));
    }

    @Test
    public void setData_whenPromptHasDifferentAudioFile_deletesOriginalAnswer() throws Exception {
        File originalFile = questionMediaManager.addAnswerFile(File.createTempFile("blah1", ".mp3"));
        FormEntryPrompt prompt = promptWithAnswer(new StringData(originalFile.getName()));
        when(prompt.getIndex()).thenReturn(formIndex);

        AudioWidget widget = createWidget(prompt);

        File newFile = questionMediaManager.addAnswerFile(File.createTempFile("blah2", ".mp3"));
        widget.setData(newFile);

        assertThat(questionMediaManager.originalFiles.get("questionIndex"), equalTo(originalFile.getAbsolutePath()));
    }

    @Test
    public void setData_whenPromptDoesNotHaveAnswer_doesNotDeleteOriginalAnswer() throws Exception {
        AudioWidget widget = createWidget(promptWithAnswer(null));
        File newFile = questionMediaManager.addAnswerFile(File.createTempFile("blah", ".mp3"));
        widget.setData(newFile);

        assertThat(questionMediaManager.originalFiles.isEmpty(), equalTo(true));
    }

    @Test
    public void setData_whenFileDoesNotExist_doesNotUpdateWidgetAnswer() throws Exception {
        File answerFile = questionMediaManager.addAnswerFile(File.createTempFile("blah", ".mp3"));
        AudioWidget widget = createWidget(promptWithAnswer(new StringData(answerFile.getName())));
        File newFile = new File("newFile.mp3");
        widget.setData(newFile);
        assertThat(widget.getAnswer().getDisplayText(), equalTo(answerFile.getName()));
    }

    @Test
    public void setData_whenFileExists_updatesWidgetAnswer() throws Exception {
        File answerFile = questionMediaManager.addAnswerFile(File.createTempFile("blah1", ".mp3"));
        AudioWidget widget = createWidget(promptWithAnswer(new StringData(answerFile.getName())));

        File newFile = questionMediaManager.addAnswerFile(File.createTempFile("blah2", ".mp3"));
        widget.setData(newFile);
        assertThat(widget.getAnswer().getDisplayText(), equalTo(newFile.getName()));
    }

    /**
     * Currently choosing audio is locked into the {@link org.odk.collect.android.tasks.MediaLoadingTask}
     * flow and so we'd need to rip this apart to let us drop support for accepting File as data. In
     * this case it will just grab the name off the file and use {@link org.odk.collect.android.utilities.QuestionMediaManager}
     * to handle grabbing the actual file
     */
    @Test
    public void setData_supportsFilesAsWellAsStrings() throws Exception {
        AudioWidget widget = createWidget(promptWithAnswer(null));

        File newFile = questionMediaManager.addAnswerFile(File.createTempFile("blah", ".mp3"));
        widget.setData(newFile);
        assertThat(widget.getAnswer().getDisplayText(), equalTo(newFile.getName()));
    }

    @Test
    public void setData_whenFileExists_callsValueChangeListener() throws Exception {
        AudioWidget widget = createWidget(promptWithAnswer(null));
        WidgetValueChangedListener valueChangedListener = mockValueChangedListener(widget);

        File newFile = questionMediaManager.addAnswerFile(File.createTempFile("blah", ".mp3"));
        widget.setData(newFile);

        verify(valueChangedListener).widgetValueChanged(widget);
    }

    @Test
    public void setData_whenFileExists_hidesButtonsAndShowsAudioController() throws Exception {
        AudioWidget widget = createWidget(promptWithAnswer(null));

        File answerFile = questionMediaManager.addAnswerFile(File.createTempFile("blah", ".mp3"));
        widget.setData(answerFile);

        assertThat(widget.binding.recordAudioButton.getVisibility(), is(GONE));
        assertThat(widget.binding.recordAudioButton.getVisibility(), is(GONE));
        assertThat(widget.binding.audioPlayer.audioController.getVisibility(), is(VISIBLE));
    }

    @Test
    public void clickingButtonsForLong_callsOnLongClickListeners() {
        View.OnLongClickListener listener = mock(View.OnLongClickListener.class);
        AudioWidget widget = createWidget(promptWithAnswer(null));
        widget.setOnLongClickListener(listener);

        widget.binding.recordAudioButton.performLongClick();
        widget.binding.chooseAudioButton.performLongClick();

        verify(listener).onLongClick(widget.binding.recordAudioButton);
        verify(listener).onLongClick(widget.binding.chooseAudioButton);
    }

    @Test
    public void clickingChooseButton_requestsAudioFile() {
        FormEntryPrompt prompt = promptWithAnswer(null);
        AudioWidget widget = createWidget(prompt);

        widget.binding.chooseAudioButton.performClick();
        verify(audioFileRequester).requestFile(prompt);
    }

    @Test
    public void clickingCaptureButton_clearsWaveform() {
        FormEntryPrompt prompt = promptWithAnswer(null);
        AudioWidget widget = createWidget(prompt);

        recordingRequester.setAmplitude(prompt.getIndex().toString(), 11);
        widget.binding.recordAudioButton.performClick();
        assertThat(widget.binding.audioPlayer.waveform.getLatestAmplitude(), nullValue());
    }

    @Test
    public void clickingCaptureButton_clearsError() {
        FormEntryPrompt prompt = promptWithAnswer(null);
        AudioWidget widget = createWidget(prompt);
        widget.displayError("Required question!");

        assertThat(widget.errorLayout.getVisibility(), equalTo(VISIBLE));
        widget.binding.recordAudioButton.performClick();
        assertThat(widget.errorLayout.getVisibility(), equalTo(GONE));
    }

    @Test
    public void whenRecordingRequesterStopsRecording_enablesButtons() {
        AudioWidget widget = createWidget(promptWithAnswer(null));

        recordingRequester.startRecording();
        assertThat(widget.binding.recordAudioButton.isEnabled(), is(false));
        assertThat(widget.binding.chooseAudioButton.isEnabled(), is(false));

        recordingRequester.stopRecording();
        assertThat(widget.binding.recordAudioButton.isEnabled(), is(true));
        assertThat(widget.binding.chooseAudioButton.isEnabled(), is(true));
    }

    @Test
    public void whenRecordingInProgress_showsDurationAndWaveform() {
        FormEntryPrompt prompt = promptWithAnswer(null);
        AudioWidget widget = createWidget(prompt);

        recordingRequester.setDuration(prompt.getIndex().toString(), 0);
        assertThat(widget.binding.recordAudioButton.getVisibility(), is(GONE));
        assertThat(widget.binding.chooseAudioButton.getVisibility(), is(GONE));
        assertThat(widget.binding.audioPlayer.audioController.getVisibility(), is(GONE));
        assertThat(widget.binding.audioPlayer.recordingDuration.getVisibility(), is(VISIBLE));
        assertThat(widget.binding.audioPlayer.waveform.getVisibility(), is(VISIBLE));
    }

    @Test
    public void whenRecordingInProgress_updatesDuration() {
        FormEntryPrompt prompt = promptWithAnswer(null);
        AudioWidget widget = createWidget(prompt);

        recordingRequester.setDuration(prompt.getIndex().toString(), 0);
        assertThat(widget.binding.audioPlayer.recordingDuration.getText(), is("00:00"));

        recordingRequester.setDuration(prompt.getIndex().toString(), 42000);
        assertThat(widget.binding.audioPlayer.recordingDuration.getText(), is("00:42"));
    }

    @Test
    public void whenRecordingInProgress_updatesWaveform() {
        FormEntryPrompt prompt = promptWithAnswer(null);
        AudioWidget widget = createWidget(prompt);

        recordingRequester.setAmplitude(prompt.getIndex().toString(), 5);
        assertThat(widget.binding.audioPlayer.waveform.getLatestAmplitude(), is(5));

        recordingRequester.setAmplitude(prompt.getIndex().toString(), 67);
        assertThat(widget.binding.audioPlayer.waveform.getLatestAmplitude(), is(67));
    }

    @Test
    public void whenRecordingFinished_showsButtons() {
        FormEntryPrompt prompt = promptWithAnswer(null);
        AudioWidget widget = createWidget(prompt);

        recordingRequester.setDuration(prompt.getIndex().toString(), 5);
        recordingRequester.reset();

        assertThat(widget.binding.audioPlayer.audioController.getVisibility(), is(GONE));
        assertThat(widget.binding.audioPlayer.recordingDuration.getVisibility(), is(GONE));
        assertThat(widget.binding.audioPlayer.waveform.getVisibility(), is(GONE));
        assertThat(widget.binding.recordAudioButton.getVisibility(), is(VISIBLE));
        assertThat(widget.binding.chooseAudioButton.getVisibility(), is(VISIBLE));
    }

    @Test
    public void afterSetBinaryData_clickingPlayAndPause_playsAndPausesAudio() throws Exception {
        FormEntryPrompt prompt = promptWithAnswer(null);
        AudioWidget widget = createWidget(prompt);

        File answerFile = questionMediaManager.addAnswerFile(File.createTempFile("blah", ".mp3"));
        Clip expectedClip = getExpectedClip(prompt, answerFile.getName());
        widget.setData(answerFile);

        AudioControllerView audioController = widget.binding.audioPlayer.audioController;
        assertThat(audioController.getVisibility(), is(VISIBLE));
        audioController.binding.play.performClick();
        assertThat(audioPlayer.getCurrentClip(), is(expectedClip));

        audioController.binding.play.performClick();
        assertThat(audioPlayer.getCurrentClip(), is(expectedClip));
        assertThat(audioPlayer.isPaused(), is(true));

        audioController.binding.play.performClick();
        assertThat(audioPlayer.getCurrentClip(), is(expectedClip));
        assertThat(audioPlayer.isPaused(), is(false));
    }

    @Test
    public void afterSetBinaryData_whenPositionOfClipChanges_updatesPosition() throws Exception {
        FormEntryPrompt prompt = promptWithAnswer(null);

        File answerFile = questionMediaManager.addAnswerFile(File.createTempFile("blah", ".mp3"));
        Clip expectedClip = getExpectedClip(prompt, answerFile.getName());
        setupMediaPlayerDataSource(expectedClip.getURI(), 322450);

        AudioWidget widget = createWidget(prompt);
        widget.setData(answerFile);

        AudioControllerView audioController = widget.binding.audioPlayer.audioController;
        assertThat(audioController.binding.currentDuration.getText().toString(), is("00:00"));

        audioPlayer.setPosition(expectedClip.getClipID(), 42000);
        assertThat(audioController.binding.currentDuration.getText().toString(), is("00:42"));
    }

    @Test
    public void afterSetBinaryData_showsDurationOfAudio() throws Exception {
        FormEntryPrompt prompt = promptWithAnswer(null);

        File answerFile = questionMediaManager.addAnswerFile(File.createTempFile("blah", ".mp3"));
        Clip expectedClip = getExpectedClip(prompt, answerFile.getName());
        setupMediaPlayerDataSource(expectedClip.getURI(), 322450);

        AudioWidget widget = createWidget(prompt);
        widget.setData(answerFile);

        AudioControllerView audioController = widget.binding.audioPlayer.audioController;
        assertThat(audioController.binding.totalDuration.getText().toString(), is("05:22"));
    }

    @Test
    public void clickingRemove_andConfirming_clearsAnswer() throws Exception {
        File answerFile = questionMediaManager.addAnswerFile(File.createTempFile("blah", ".mp3"));
        AudioWidget widget = createWidget(promptWithAnswer(new StringData(answerFile.getName())));
        widget.binding.audioPlayer.audioController.binding.remove.performClick();

        AlertDialog dialog = (AlertDialog) getLatestDialog();
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).performClick();
        RobolectricHelpers.runLooper();
        assertThat(widget.getAnswer(), nullValue());
    }

    @Test
    public void clickingRemove_andCancelling_doesNothing() throws Exception {
        File answerFile = questionMediaManager.addAnswerFile(File.createTempFile("blah", ".mp3"));
        AudioWidget widget = createWidget(promptWithAnswer(new StringData(answerFile.getName())));
        widget.binding.audioPlayer.audioController.binding.remove.performClick();

        AlertDialog dialog = (AlertDialog) getLatestDialog();
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE).performClick();

        assertThat(widget.getAnswer(), notNullValue());
    }

    @Test
    public void clickingRemove_andConfirming_hidesAudioControllerAndShowsButtons() throws Exception {
        File answerFile = questionMediaManager.addAnswerFile(File.createTempFile("blah", ".mp3"));
        AudioWidget widget = createWidget(promptWithAnswer(new StringData(answerFile.getName())));
        widget.binding.audioPlayer.audioController.binding.remove.performClick();

        AlertDialog dialog = (AlertDialog) getLatestDialog();
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).performClick();
        RobolectricHelpers.runLooper();
        assertThat(widget.binding.audioPlayer.audioController.getVisibility(), is(GONE));
        assertThat(widget.binding.recordAudioButton.getVisibility(), is(VISIBLE));
        assertThat(widget.binding.chooseAudioButton.getVisibility(), is(VISIBLE));
    }

    @Test
    public void usingReadOnlyOptionShouldMakeAllClickableElementsDisabled() throws Exception {
        File answerFile = questionMediaManager.addAnswerFile(File.createTempFile("blah", ".mp3"));
        AudioWidget widget = createWidget(promptWithReadOnlyAndAnswer(new StringData(answerFile.getName())));
        widget.binding.audioPlayer.audioController.binding.remove.performClick();

        assertThat(widget.binding.recordAudioButton.getVisibility(), is(View.GONE));
        assertThat(widget.binding.chooseAudioButton.getVisibility(), is(View.GONE));
    }

    @Test
    public void whenReadOnlyOverrideOptionIsUsed_shouldAllClickableElementsBeDisabled() throws Exception {
        File answerFile = questionMediaManager.addAnswerFile(File.createTempFile("blah", ".mp3"));
        AudioWidget widget = createWidget(promptWithAnswer(new StringData(answerFile.getName())), true);
        widget.binding.audioPlayer.audioController.binding.remove.performClick();

        assertThat(widget.binding.recordAudioButton.getVisibility(), is(View.GONE));
        assertThat(widget.binding.chooseAudioButton.getVisibility(), is(View.GONE));
    }

    public AudioWidget createWidget(FormEntryPrompt prompt) {
        return new AudioWidget(
                widgetActivity,
                new QuestionDetails(prompt),
                questionMediaManager,
                audioPlayer,
                recordingRequester,
                audioFileRequester,
                recordingRequester,
                widgetDependencies());
    }

    public AudioWidget createWidget(FormEntryPrompt prompt, boolean readOnlyOverride) {
        return new AudioWidget(
                widgetActivity,
                new QuestionDetails(prompt, readOnlyOverride),
                questionMediaManager,
                audioPlayer,
                recordingRequester,
                audioFileRequester,
                recordingRequester,
                widgetDependencies());
    }

    @NotNull
    private Clip getExpectedClip(FormEntryPrompt prompt, String fileName) {
        return new Clip(
                "audio:" + prompt.getIndex().toString(),
                questionMediaManager.getAnswerFile(fileName).getAbsolutePath()
        );
    }

    private static class FakeRecordingRequester implements RecordingRequester, RecordingStatusHandler {

        FormEntryPrompt requestedRecordingFor;
        private Consumer<Boolean> isRecordingListener;
        private final Map<String, Consumer<Pair<Long, Integer>>> inProgressListeners = new HashMap<>();

        @Override
        public void requestRecording(FormEntryPrompt prompt) {
            requestedRecordingFor = prompt;
        }

        @Override
        public void onBlockedStatusChange(Consumer<Boolean> blockedStatusListener) {
            this.isRecordingListener = blockedStatusListener;
        }

        @Override
        public void onRecordingStatusChange(FormEntryPrompt prompt, Consumer<Pair<Long, Integer>> statusListener) {
            inProgressListeners.put(prompt.getIndex().toString(), statusListener);
        }

        public void startRecording() {
            isRecordingListener.accept(true);
        }

        public void stopRecording() {
            isRecordingListener.accept(false);
        }

        public void setDuration(String sessionId, long duration) {
            inProgressListeners.get(sessionId).accept(new Pair<>(duration, 0));
        }

        public void setAmplitude(String sessionId, int amplitude) {
            inProgressListeners.get(sessionId).accept(new Pair<>(0L, amplitude));
        }

        public void reset() {
            inProgressListeners.forEach((sessionId, listener) -> {
                listener.accept(null);
            });
        }
    }
}
