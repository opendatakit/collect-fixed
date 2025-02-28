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
import org.odk.collect.android.widgets.utilities.FileRequester;
import org.odk.collect.android.widgets.utilities.QuestionFontSizeUtils;
import org.odk.collect.androidshared.system.IntentLauncher;
import org.robolectric.shadows.ShadowToast;

import java.io.File;
import java.io.IOException;

public class ExVideoWidgetTest extends FileWidgetTest<ExVideoWidget> {
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
        when(mediaUtils.isVideoFile(any())).thenReturn(true);
    }

    @Override
    public StringData getInitialAnswer() {
        return new StringData("video1.mp4");
    }

    @NonNull
    @Override
    public StringData getNextAnswer() {
        return new StringData("video2.mp4");
    }

    @NonNull
    @Override
    public ExVideoWidget createWidget() {
        return new ExVideoWidget(activity, new QuestionDetails(formEntryPrompt, readOnlyOverride),
                new FakeQuestionMediaManager(), new FakeWaitingForDataRegistry(), fileRequester, dependencies);
    }

    @Test
    public void whenWidgetCreated_shouldButtonsBeVisible() {
        assertThat(getWidget().binding.captureVideoButton.getVisibility(), is(View.VISIBLE));
        assertThat(getWidget().binding.playVideoButton.getVisibility(), is(View.VISIBLE));
    }

    @Test
    public void whenWidgetCreated_shouldButtonsHaveProperNames() {
        assertThat(getWidget().binding.captureVideoButton.getText(), is("Launch"));
        assertThat(getWidget().binding.playVideoButton.getText(), is("Play Video"));
    }

    @Test
    public void whenFontSizeChanged_CustomFontSizeShouldBeUsed() {
        settingsProvider.getUnprotectedSettings().save(KEY_FONT_SIZE, "30");

        assertThat((int) getWidget().binding.captureVideoButton.getTextSize(), is(QuestionFontSizeUtils.getFontSize(settingsProvider.getUnprotectedSettings(), QuestionFontSizeUtils.FontSize.BODY_LARGE)));
        assertThat((int) getWidget().binding.playVideoButton.getTextSize(), is(QuestionFontSizeUtils.getFontSize(settingsProvider.getUnprotectedSettings(), QuestionFontSizeUtils.FontSize.BODY_LARGE)));
    }

    @Test
    public void whenThereIsNoAnswer_shouldOnlyLaunchButtonBeEnabled() {
        assertThat(getWidget().binding.captureVideoButton.isEnabled(), is(true));
        assertThat(getWidget().binding.playVideoButton.isEnabled(), is(false));
    }

    @Test
    public void whenThereIsAnswer_shouldBothButtonsBeEnabled() {
        when(formEntryPrompt.getAnswerText()).thenReturn(getInitialAnswer().getDisplayText());

        assertThat(getWidget().binding.captureVideoButton.isEnabled(), is(true));
        assertThat(getWidget().binding.playVideoButton.isEnabled(), is(true));
    }

    @Test
    public void whenClearAnswerCall_shouldPlayButtonBecomeDisabled() {
        when(formEntryPrompt.getAnswerText()).thenReturn(getInitialAnswer().getDisplayText());

        ExVideoWidget widget = getWidget();
        widget.clearAnswer();
        assertThat(widget.binding.captureVideoButton.isEnabled(), is(true));
        assertThat(widget.binding.playVideoButton.isEnabled(), is(false));
    }

    @Test
    public void whenCaptureVideoButtonClicked_exWidgetIntentLauncherShouldBeStarted() {
        getWidget().binding.captureVideoButton.performClick();
        verify(fileRequester).launch(activity, ApplicationConstants.RequestCodes.EX_VIDEO_CHOOSER, formEntryPrompt);
    }

    @Test
    public void whenClickingOnPlayButton_shouldFileViewerByCalled() {
        when(formEntryPrompt.getAnswerText()).thenReturn(getInitialAnswer().getDisplayText());

        ExVideoWidget widget = getWidget();
        widget.binding.playVideoButton.performClick();
        verify(mediaUtils).openFile(activity, widget.answerFile, "video/*");
    }

    @Test
    public void whenSetDataCalledWithNull_shouldExistedAnswerBeRemoved() {
        when(formEntryPrompt.getAnswerText()).thenReturn(getInitialAnswer().getDisplayText());

        ExVideoWidget widget = getWidget();
        widget.setData(null);
        assertThat(widget.getAnswer(), is(nullValue()));
        assertThat(widget.binding.playVideoButton.isEnabled(), is(false));
    }

    @Test
    public void whenUnsupportedFileTypeAttached_shouldNotThatFileBeAdded() throws IOException {
        ExVideoWidget widget = getWidget();
        File answer = File.createTempFile("doc", ".pdf");
        when(mediaUtils.isVideoFile(answer)).thenReturn(false);
        widget.setData(answer);
        assertThat(widget.getAnswer(), is(nullValue()));
        assertThat(widget.binding.playVideoButton.isEnabled(), is(false));
    }

    @Test
    public void whenUnsupportedFileTypeAttached_shouldTheFileBeRemoved() throws IOException {
        ExVideoWidget widget = getWidget();
        File answer = File.createTempFile("doc", ".pdf");
        when(mediaUtils.isVideoFile(answer)).thenReturn(false);
        widget.setData(answer);
        verify(mediaUtils).deleteMediaFile(answer.getAbsolutePath());
    }

    @Test
    public void whenUnsupportedFileTypeAttached_shouldToastBeDisplayed() throws IOException {
        ExVideoWidget widget = getWidget();
        File answer = File.createTempFile("doc", ".pdf");
        when(mediaUtils.isVideoFile(answer)).thenReturn(false);
        widget.setData(answer);
        assertThat(ShadowToast.getTextOfLatestToast(), is("Application returned an invalid file type"));
    }

    @Test
    public void usingReadOnlyOptionShouldMakeAllClickableElementsDisabled() {
        when(formEntryPrompt.isReadOnly()).thenReturn(true);
        when(formEntryPrompt.getAnswerText()).thenReturn(getInitialAnswer().getDisplayText());

        ExVideoWidget widget = getWidget();
        assertThat(widget.binding.captureVideoButton.getVisibility(), is(View.GONE));
        assertThat(widget.binding.playVideoButton.getVisibility(), is(View.VISIBLE));
        assertThat(widget.binding.playVideoButton.isEnabled(), is(true));
    }

    @Test
    public void whenReadOnlyOverrideOptionIsUsed_shouldAllClickableElementsBeDisabled() {
        readOnlyOverride = true;
        when(formEntryPrompt.getAnswerText()).thenReturn(getInitialAnswer().getDisplayText());

        ExVideoWidget widget = getWidget();
        assertThat(widget.binding.captureVideoButton.getVisibility(), is(View.GONE));
        assertThat(widget.binding.playVideoButton.getVisibility(), is(View.VISIBLE));
        assertThat(widget.binding.playVideoButton.isEnabled(), is(true));
    }
}
