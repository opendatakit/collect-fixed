package org.odk.collect.android.widgets;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.odk.collect.android.support.CollectHelpers.setupFakeReferenceManager;
import static java.util.Arrays.asList;

import android.content.Context;

import androidx.core.util.Pair;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.javarosa.core.model.data.IAnswerData;
import org.javarosa.core.reference.ReferenceManager;
import org.javarosa.form.api.FormEntryPrompt;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.odk.collect.android.R;
import org.odk.collect.android.audio.AudioButton;
import org.odk.collect.android.formentry.questions.QuestionDetails;
import org.odk.collect.android.injection.config.AppDependencyModule;
import org.odk.collect.android.support.CollectHelpers;
import org.odk.collect.android.support.MockFormEntryPromptBuilder;
import org.odk.collect.android.support.WidgetTestActivity;
import org.odk.collect.android.widgets.utilities.AudioPlayer;
import org.odk.collect.audioclips.Clip;

@RunWith(AndroidJUnit4.class)
public class QuestionWidgetTest {

    @Rule
    public MockitoRule rule = MockitoJUnit.rule();

    @Before
    public void setup() throws Exception {
        overrideDependencyModule();
    }

    @Test
    public void whenQuestionHasAudio_audioButtonUsesIndexAsClipID() throws Exception {
        FormEntryPrompt prompt = new MockFormEntryPromptBuilder()
                .withIndex("i am index")
                .withAudioURI("ref")
                .build();

        WidgetTestActivity activity = CollectHelpers.createThemedActivity(WidgetTestActivity.class);
        AudioPlayer audioPlayer = mock();
        TestWidget widget = new TestWidget(activity, new QuestionDetails(prompt), new QuestionWidget.Dependencies(audioPlayer));

        AudioButton audioButton = widget.getAudioVideoImageTextLabel().findViewById(R.id.audioButton);
        audioButton.performClick();
        verify(audioPlayer).play(new Clip("i am index", "blah.mp3"));
    }

    private void overrideDependencyModule() throws Exception {
        ReferenceManager referenceManager = setupFakeReferenceManager(asList(new Pair<>("ref", "blah.mp3")));
        CollectHelpers.overrideAppDependencyModule(new AppDependencyModule() {

            @Override
            public ReferenceManager providesReferenceManager() {
                return referenceManager;
            }
        });
    }

    private static class TestWidget extends QuestionWidget {

        TestWidget(Context context, QuestionDetails questionDetails, Dependencies dependencies) {
            super(context, dependencies, questionDetails);
        }

        @Override
        public IAnswerData getAnswer() {
            return null;
        }

        @Override
        public void clearAnswer() {

        }

        @Override
        public void setOnLongClickListener(OnLongClickListener l) {

        }
    }
}
