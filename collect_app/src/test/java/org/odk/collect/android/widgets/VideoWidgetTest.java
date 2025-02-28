package org.odk.collect.android.widgets;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Intent;
import android.provider.MediaStore;
import android.view.View;

import androidx.annotation.NonNull;

import net.bytebuddy.utility.RandomString;

import org.javarosa.core.model.data.StringData;
import org.junit.Before;
import org.junit.Test;
import org.odk.collect.android.R;
import org.odk.collect.android.formentry.questions.QuestionDetails;
import org.odk.collect.android.injection.config.AppDependencyModule;
import org.odk.collect.android.support.CollectHelpers;
import org.odk.collect.android.utilities.MediaUtils;
import org.odk.collect.android.widgets.base.FileWidgetTest;
import org.odk.collect.android.widgets.support.FakeQuestionMediaManager;
import org.odk.collect.android.widgets.support.FakeWaitingForDataRegistry;
import org.odk.collect.androidshared.system.IntentLauncher;
import org.odk.collect.shared.TempFiles;

/**
 * @author James Knight
 */
public class VideoWidgetTest extends FileWidgetTest<VideoWidget> {
    private String destinationName;

    @NonNull
    @Override
    public VideoWidget createWidget() {
        return new VideoWidget(activity, new QuestionDetails(formEntryPrompt, readOnlyOverride), new FakeWaitingForDataRegistry(), new FakeQuestionMediaManager(), dependencies);
    }

    @NonNull
    @Override
    public StringData getNextAnswer() {
        return new StringData(destinationName);
    }

    @Before
    public void setUp() throws Exception {
        super.setUp();
        destinationName = RandomString.make();
    }

    @Test
    public void buttonsShouldLaunchCorrectIntents() {
        MediaUtils mediaUtils = mock(MediaUtils.class);
        CollectHelpers.overrideAppDependencyModule(new AppDependencyModule() {
            @Override
            public MediaUtils providesMediaUtils(IntentLauncher intentLauncher) {
                return mediaUtils;
            }
        });

        stubAllRuntimePermissionsGranted(true);

        Intent intent = getIntentLaunchedByClick(R.id.record_video_button);
        assertActionEquals(MediaStore.ACTION_VIDEO_CAPTURE, intent);

        intent = getIntentLaunchedByClick(R.id.choose_video_button);
        assertActionEquals(Intent.ACTION_GET_CONTENT, intent);
        assertTypeEquals("video/*", intent);

        getWidget().setData(TempFiles.createTempFile(TempFiles.createTempDir()));
        getIntentLaunchedByClick(R.id.play_video_button);
        verify(mediaUtils).openFile(any(), any(), any());
    }

    @Test
    public void buttonsShouldNotLaunchIntentsWhenPermissionsDenied() {
        stubAllRuntimePermissionsGranted(false);

        assertNull(getIntentLaunchedByClick(R.id.record_video_button));
    }

    @Test
    public void usingReadOnlyOptionShouldMakeAllClickableElementsDisabled() {
        when(formEntryPrompt.isReadOnly()).thenReturn(true);

        assertThat(getSpyWidget().binding.recordVideoButton.getVisibility(), is(View.GONE));
        assertThat(getSpyWidget().binding.chooseVideoButton.getVisibility(), is(View.GONE));
        assertThat(getSpyWidget().binding.playVideoButton.getVisibility(), is(View.VISIBLE));
        assertThat(getSpyWidget().binding.playVideoButton.isEnabled(), is(Boolean.FALSE));
        assertThat(getSpyWidget().binding.playVideoButton.getText(), is("Play Video"));
    }

    @Test
    public void whenReadOnlyOverrideOptionIsUsed_shouldAllClickableElementsBeDisabled() {
        readOnlyOverride = true;
        when(formEntryPrompt.isReadOnly()).thenReturn(false);

        assertThat(getSpyWidget().binding.recordVideoButton.getVisibility(), is(View.GONE));
        assertThat(getSpyWidget().binding.chooseVideoButton.getVisibility(), is(View.GONE));
        assertThat(getSpyWidget().binding.playVideoButton.getVisibility(), is(View.VISIBLE));
        assertThat(getSpyWidget().binding.playVideoButton.isEnabled(), is(Boolean.FALSE));
        assertThat(getSpyWidget().binding.playVideoButton.getText(), is("Play Video"));
    }
}
