package org.odk.collect.android.widgets;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.odk.collect.settings.keys.ProjectKeys.KEY_FONT_SIZE;

import android.view.View;

import androidx.annotation.NonNull;

import org.javarosa.core.model.data.StringData;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.odk.collect.android.formentry.questions.QuestionDetails;
import org.odk.collect.android.injection.config.AppDependencyModule;
import org.odk.collect.android.support.CollectHelpers;
import org.odk.collect.android.utilities.ApplicationConstants;
import org.odk.collect.android.utilities.MediaUtils;
import org.odk.collect.android.widgets.base.FileWidgetTest;
import org.odk.collect.android.widgets.support.FakeQuestionMediaManager;
import org.odk.collect.android.widgets.support.FakeWaitingForDataRegistry;
import org.odk.collect.android.widgets.utilities.AudioPlayer;
import org.odk.collect.android.widgets.utilities.FileRequester;
import org.odk.collect.android.widgets.utilities.QuestionFontSizeUtils;
import org.odk.collect.androidshared.system.IntentLauncher;
import org.robolectric.shadows.ShadowToast;

import java.io.File;
import java.io.IOException;

public class ExAudioWidgetTest extends FileWidgetTest<ExAudioWidget> {
    @Mock
    AudioPlayer audioPlayer;

    @Mock
    FileRequester fileRequester;

    private MediaUtils mediaUtils;

    @Before
    public void setup() {
        mediaUtils = mock(MediaUtils.class);
        CollectHelpers.overrideAppDependencyModule(new AppDependencyModule() {
            @Override
            public MediaUtils providesMediaUtils(IntentLauncher intentLauncher) {
                return mediaUtils;
            }
        });
        when(mediaUtils.isAudioFile(any())).thenReturn(true);
    }

    @Override
    public StringData getInitialAnswer() {
        return new StringData("audio1.mp3");
    }

    @NonNull
    @Override
    public StringData getNextAnswer() {
        return new StringData("audio2.mp3");
    }

    @NonNull
    @Override
    public ExAudioWidget createWidget() {
        return new ExAudioWidget(activity, new QuestionDetails(formEntryPrompt, readOnlyOverride),
                new FakeQuestionMediaManager(), audioPlayer, new FakeWaitingForDataRegistry(), fileRequester, dependencies);
    }

    @Test
    public void whenWidgetCreated_shouldLaunchButtonBeVisible() {
        assertThat(getWidget().binding.launchExternalAppButton.getVisibility(), is(View.VISIBLE));
    }

    @Test
    public void whenWidgetCreated_shouldLaunchButtonHaveProperName() {
        assertThat(getWidget().binding.launchExternalAppButton.getText(), is("Launch"));
    }

    @Test
    public void whenFontSizeChanged_CustomFontSizeShouldBeUsed() {
        settingsProvider.getUnprotectedSettings().save(KEY_FONT_SIZE, "30");

        assertThat((int) getWidget().binding.launchExternalAppButton.getTextSize(), is(QuestionFontSizeUtils.getFontSize(settingsProvider.getUnprotectedSettings(), QuestionFontSizeUtils.FontSize.BODY_LARGE)));
    }

    @Test
    public void whenThereIsNoAnswer_shouldAudioPlayerBeHidden() {
        assertThat(getWidget().binding.audioPlayer.recordingDuration.getVisibility(), is(View.GONE));
        assertThat(getWidget().binding.audioPlayer.waveform.getVisibility(), is(View.GONE));
        assertThat(getWidget().binding.audioPlayer.audioController.getVisibility(), is(View.GONE));
    }

    @Test
    public void whenThereIsAnswer_shouldAudioPlayerBeDisplayed() {
        when(formEntryPrompt.getAnswerText()).thenReturn(getInitialAnswer().getDisplayText());

        assertThat(getWidget().binding.audioPlayer.audioController.getVisibility(), is(View.VISIBLE));
    }

    @Test
    public void whenThereIsAnswer_shouldLaunchButtonBeHidden() {
        when(formEntryPrompt.getAnswerText()).thenReturn(getInitialAnswer().getDisplayText());

        assertThat(getWidget().binding.launchExternalAppButton.getVisibility(), is(View.GONE));
    }

    @Test
    public void whenAnswerCleared_shouldAudioPlayerBeHidden() {
        when(formEntryPrompt.getAnswerText()).thenReturn(getInitialAnswer().getDisplayText());

        ExAudioWidget widget = getWidget();
        widget.clearAnswer();
        assertThat(getWidget().binding.audioPlayer.audioController.getVisibility(), is(View.GONE));
    }

    @Test
    public void whenLaunchButtonClicked_exWidgetIntentLauncherShouldBeStarted() {
        getWidget().binding.launchExternalAppButton.performClick();
        verify(fileRequester).launch(activity, ApplicationConstants.RequestCodes.EX_AUDIO_CHOOSER, formEntryPrompt);
    }

    @Test
    public void whenSetDataCalledWithNull_shouldExistedAnswerBeRemoved() {
        when(formEntryPrompt.getAnswerText()).thenReturn(getInitialAnswer().getDisplayText());

        ExAudioWidget widget = getWidget();
        widget.setData(null);
        assertThat(widget.getAnswer(), is(nullValue()));
        assertThat(getWidget().binding.audioPlayer.audioController.getVisibility(), is(View.GONE));
    }

    @Test
    public void whenUnsupportedFileTypeAttached_shouldNotThatFileBeAdded() throws IOException {
        ExAudioWidget widget = getWidget();
        File answer = File.createTempFile("doc", ".pdf");
        when(mediaUtils.isAudioFile(answer)).thenReturn(false);
        widget.setData(answer);
        assertThat(widget.getAnswer(), is(nullValue()));
        assertThat(getWidget().binding.audioPlayer.audioController.getVisibility(), is(View.GONE));
    }

    @Test
    public void whenUnsupportedFileTypeAttached_shouldTheFileBeRemoved() throws IOException {
        ExAudioWidget widget = getWidget();
        File answer = File.createTempFile("doc", ".pdf");
        when(mediaUtils.isAudioFile(answer)).thenReturn(false);
        widget.setData(answer);
        verify(mediaUtils).deleteMediaFile(answer.getAbsolutePath());
    }

    @Test
    public void whenUnsupportedFileTypeAttached_shouldToastBeDisplayed() throws IOException {
        ExAudioWidget widget = getWidget();
        File answer = File.createTempFile("doc", ".pdf");
        when(mediaUtils.isAudioFile(answer)).thenReturn(false);
        widget.setData(answer);
        assertThat(ShadowToast.getTextOfLatestToast(), is("Application returned an invalid file type"));
    }

    @Test
    public void usingReadOnlyOptionShouldMakeAllClickableElementsDisabled() {
        when(formEntryPrompt.isReadOnly()).thenReturn(true);
        when(formEntryPrompt.getAnswerText()).thenReturn(getInitialAnswer().getDisplayText());

        ExAudioWidget widget = getWidget();
        assertThat(widget.binding.launchExternalAppButton.getVisibility(), is(View.GONE));
        assertThat(getWidget().binding.audioPlayer.audioController.getVisibility(), is(View.VISIBLE));
    }

    @Test
    public void whenReadOnlyOverrideOptionIsUsed_shouldAllClickableElementsBeDisabled() {
        readOnlyOverride = true;
        when(formEntryPrompt.getAnswerText()).thenReturn(getInitialAnswer().getDisplayText());

        ExAudioWidget widget = getWidget();
        assertThat(widget.binding.launchExternalAppButton.getVisibility(), is(View.GONE));
        assertThat(getWidget().binding.audioPlayer.audioController.getVisibility(), is(View.VISIBLE));
    }
}

