package org.odk.collect.android.formentry;

import static android.view.View.VISIBLE;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.odk.collect.android.support.CollectHelpers.createThemedActivity;

import android.view.View;
import android.widget.CheckBox;
import android.widget.RadioButton;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.odk.collect.android.R;
import org.odk.collect.android.formentry.questions.AudioVideoImageTextLabel;
import org.odk.collect.android.support.WidgetTestActivity;
import org.odk.collect.android.utilities.MediaUtils;
import org.odk.collect.android.widgets.support.FakeAudioPlayer;
import org.odk.collect.audioclips.Clip;
import org.odk.collect.imageloader.ImageLoader;

import java.io.File;

@RunWith(AndroidJUnit4.class)
public class AudioVideoImageTextLabelTest {

    @Rule
    public MockitoRule rule = MockitoJUnit.rule();

    private FakeAudioPlayer audioPlayer = new FakeAudioPlayer();

    private WidgetTestActivity activity;

    @Before
    public void setup() {
        activity = createThemedActivity(WidgetTestActivity.class);
    }

    @Test
    public void withNullText_hidesTextLabel() {
        AudioVideoImageTextLabel audioVideoImageTextLabel = new AudioVideoImageTextLabel(activity);
        audioVideoImageTextLabel.setText(null, false, 16);

        assertThat(audioVideoImageTextLabel.getLabelTextView().getVisibility(), equalTo(View.GONE));
    }

    @Test
    public void withBlankText_hidesTextLabel() {
        AudioVideoImageTextLabel audioVideoImageTextLabel = new AudioVideoImageTextLabel(activity);
        audioVideoImageTextLabel.setText("", false, 16);

        assertThat(audioVideoImageTextLabel.getLabelTextView().getVisibility(), equalTo(View.GONE));
    }

    @Test
    public void withText_andAudio_showsTextAndAudioButton()  {
        AudioVideoImageTextLabel label = new AudioVideoImageTextLabel(activity);
        label.setText("blah", false, 16);
        label.setAudio("file://audio.mp3", audioPlayer);

        assertThat(label.getLabelTextView().getVisibility(), equalTo(VISIBLE));
        assertThat(label.getLabelTextView().getText().toString(), equalTo("blah"));
        assertThat(label.findViewById(R.id.audioButton).getVisibility(), equalTo(VISIBLE));
    }

    @Test
    public void withText_andAudio_playingAudio_highlightsText() {
        AudioVideoImageTextLabel audioVideoImageTextLabel = new AudioVideoImageTextLabel(activity);
        audioVideoImageTextLabel.setTag("blah");
        audioVideoImageTextLabel.setText("blah", false, 16);
        audioVideoImageTextLabel.setAudio("file://audio.mp3", audioPlayer);

        int originalTextColor = audioVideoImageTextLabel.getLabelTextView().getCurrentTextColor();

        audioPlayer.play(new Clip("blah", "file://audio.mp3"));
        int textColor = audioVideoImageTextLabel.getLabelTextView().getCurrentTextColor();
        assertThat(textColor, not(equalTo(originalTextColor)));

        audioPlayer.stop();
        textColor = audioVideoImageTextLabel.getLabelTextView().getCurrentTextColor();
        assertThat(textColor, equalTo(originalTextColor));
    }

    @Test
    public void withAudio_pressingStopOnAudioButton_stopsAudio() {
        AudioVideoImageTextLabel audioVideoImageTextLabel = new AudioVideoImageTextLabel(activity);
        audioVideoImageTextLabel.setTag("blah");
        audioVideoImageTextLabel.setAudio("file://audio.mp3", audioPlayer);

        audioPlayer.play(new Clip("blah", "file://audio.mp3"));
        audioVideoImageTextLabel.findViewById(R.id.audioButton).performClick();
        assertThat(audioPlayer.getCurrentClip(), equalTo(null));
    }

    @Test
    public void bothClickingLabelAndImageView_shouldSelectOptionInSelectOneMode() {
        File imageFile = mock(File.class);
        when(imageFile.exists()).thenReturn(true);

        AudioVideoImageTextLabel audioVideoImageTextLabel = new AudioVideoImageTextLabel(activity);
        audioVideoImageTextLabel.setImage(imageFile, mock(ImageLoader.class));
        audioVideoImageTextLabel.setTextView(new RadioButton(activity));

        assertThat(((RadioButton) audioVideoImageTextLabel.getLabelTextView()).isChecked(), is(false));

        // click on label
        audioVideoImageTextLabel.getLabelTextView().performClick();
        assertThat(((RadioButton) audioVideoImageTextLabel.getLabelTextView()).isChecked(), is(true));

        // clear answer
        ((RadioButton) audioVideoImageTextLabel.getLabelTextView()).setChecked(false);
        assertThat(((RadioButton) audioVideoImageTextLabel.getLabelTextView()).isChecked(), is(false));

        // click on image
        audioVideoImageTextLabel.getImageView().performClick();
        assertThat(((RadioButton) audioVideoImageTextLabel.getLabelTextView()).isChecked(), is(true));
    }

    @Test
    public void bothClickingLabelAndImageView_shouldSelectOptionInSelectMultiMode() {
        File imageFile = mock(File.class);
        when(imageFile.exists()).thenReturn(true);

        AudioVideoImageTextLabel audioVideoImageTextLabel = new AudioVideoImageTextLabel(activity);
        audioVideoImageTextLabel.setImage(imageFile, mock(ImageLoader.class));
        audioVideoImageTextLabel.setTextView(new CheckBox(activity));

        assertThat(((CheckBox) audioVideoImageTextLabel.getLabelTextView()).isChecked(), is(false));

        // click on label
        audioVideoImageTextLabel.getLabelTextView().performClick();
        assertThat(((CheckBox) audioVideoImageTextLabel.getLabelTextView()).isChecked(), is(true));

        // click on image
        audioVideoImageTextLabel.getImageView().performClick();
        assertThat(((CheckBox) audioVideoImageTextLabel.getLabelTextView()).isChecked(), is(false));
    }

    @Test
    public void whenClickOneElementTwiceInSelectOneMode_shouldThatElementRemainSelected() {
        File imageFile = mock(File.class);
        when(imageFile.exists()).thenReturn(true);

        AudioVideoImageTextLabel audioVideoImageTextLabel = new AudioVideoImageTextLabel(activity);
        audioVideoImageTextLabel.setImage(imageFile, mock(ImageLoader.class));
        audioVideoImageTextLabel.setTextView(new RadioButton(activity));

        assertThat(((RadioButton) audioVideoImageTextLabel.getLabelTextView()).isChecked(), is(false));

        // click on label
        audioVideoImageTextLabel.getLabelTextView().performClick();
        assertThat(((RadioButton) audioVideoImageTextLabel.getLabelTextView()).isChecked(), is(true));

        // click on label again
        audioVideoImageTextLabel.getLabelTextView().performClick();
        assertThat(((RadioButton) audioVideoImageTextLabel.getLabelTextView()).isChecked(), is(true));

        // click on image
        audioVideoImageTextLabel.getImageView().performClick();
        assertThat(((RadioButton) audioVideoImageTextLabel.getLabelTextView()).isChecked(), is(true));

        // click on image again
        audioVideoImageTextLabel.getImageView().performClick();
        assertThat(((RadioButton) audioVideoImageTextLabel.getLabelTextView()).isChecked(), is(true));
    }

    @Test
    public void whenClickOneElementTwiceInSelectMultiMode_shouldThatElementBeUnSelected() {
        File imageFile = mock(File.class);
        when(imageFile.exists()).thenReturn(true);

        AudioVideoImageTextLabel audioVideoImageTextLabel = new AudioVideoImageTextLabel(activity);
        audioVideoImageTextLabel.setImage(imageFile, mock(ImageLoader.class));
        audioVideoImageTextLabel.setTextView(new CheckBox(activity));

        assertThat(((CheckBox) audioVideoImageTextLabel.getLabelTextView()).isChecked(), is(false));

        // click on label
        audioVideoImageTextLabel.getLabelTextView().performClick();
        assertThat(((CheckBox) audioVideoImageTextLabel.getLabelTextView()).isChecked(), is(true));

        // click on label again
        audioVideoImageTextLabel.getLabelTextView().performClick();
        assertThat(((CheckBox) audioVideoImageTextLabel.getLabelTextView()).isChecked(), is(false));

        // click on image
        audioVideoImageTextLabel.getImageView().performClick();
        assertThat(((CheckBox) audioVideoImageTextLabel.getLabelTextView()).isChecked(), is(true));

        // click on image again
        audioVideoImageTextLabel.getImageView().performClick();
        assertThat(((CheckBox) audioVideoImageTextLabel.getLabelTextView()).isChecked(), is(false));
    }

    @Test
    public void whenImageFileDoesNotExist_ShouldAnAppropriateMessageBeDisplayed() {
        File imageFile = new File("file://image.png");

        AudioVideoImageTextLabel audioVideoImageTextLabel = new AudioVideoImageTextLabel(activity);
        audioVideoImageTextLabel.setImage(imageFile, mock(ImageLoader.class));

        assertThat(audioVideoImageTextLabel.getMissingImage().getVisibility(), is(VISIBLE));
        assertThat(audioVideoImageTextLabel.getMissingImage().getText().toString(), is("File: file:/image.png is missing."));
    }

    @Test
    public void whenVideoFileClicked_ShouldMediaUtilsBeCalled() {
        MediaUtils mediaUtils = mock(MediaUtils.class);

        File videoFile = mock(File.class);
        when(videoFile.exists()).thenReturn(true);

        AudioVideoImageTextLabel audioVideoImageTextLabel = new AudioVideoImageTextLabel(activity);
        audioVideoImageTextLabel.setVideo(videoFile);
        audioVideoImageTextLabel.setMediaUtils(mediaUtils);
        audioVideoImageTextLabel.getVideoButton().performClick();

        verify(mediaUtils).openFile(activity, videoFile, "video/*");
    }
}
