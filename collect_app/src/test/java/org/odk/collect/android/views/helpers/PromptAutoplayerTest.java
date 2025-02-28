package org.odk.collect.android.views.helpers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.odk.collect.android.support.CollectHelpers.setupFakeReferenceManager;
import static org.odk.collect.android.utilities.Appearances.COMPACT;
import static org.odk.collect.android.utilities.Appearances.MINIMAL;
import static org.odk.collect.android.utilities.Appearances.NO_BUTTONS;
import static java.util.Arrays.asList;

import androidx.core.util.Pair;

import org.javarosa.core.model.Constants;
import org.javarosa.core.model.SelectChoice;
import org.javarosa.core.reference.ReferenceManager;
import org.javarosa.form.api.FormEntryCaption;
import org.javarosa.form.api.FormEntryPrompt;
import org.junit.Before;
import org.junit.Test;
import org.odk.collect.android.formentry.media.PromptAutoplayer;
import org.odk.collect.android.support.MockFormEntryPromptBuilder;
import org.odk.collect.android.widgets.utilities.AudioPlayer;
import org.odk.collect.audioclips.Clip;

import java.util.List;

public class PromptAutoplayerTest {

    private final AudioPlayer audioPlayer = mock(AudioPlayer.class);

    private PromptAutoplayer autoplayer;

    @Before
    public void setup() throws Exception {
        ReferenceManager referenceManager = setupFakeReferenceManager(REFERENCES);
        autoplayer = new PromptAutoplayer(audioPlayer, referenceManager);
    }

    @Test
    public void whenPromptHasAutoplayAudio_playsAudio() {
        FormEntryPrompt prompt = new MockFormEntryPromptBuilder()
                .withAudioURI(REFERENCES.get(0).first)
                .withAdditionalAttribute("autoplay", "audio")
                .build();

        assertThat(autoplayer.autoplayIfNeeded(prompt), equalTo(true));
        verify(audioPlayer).playInOrder(asList(new Clip(prompt.getIndex().toString(), REFERENCES.get(0).second)));
    }

    @Test
    public void whenPromptHasAutoplayAudio_withDifferentCapitalization_playsAudio() {
        FormEntryPrompt prompt = new MockFormEntryPromptBuilder()
                .withAudioURI(REFERENCES.get(0).first)
                .withAdditionalAttribute("autoplay", "aUdio")
                .build();

        assertThat(autoplayer.autoplayIfNeeded(prompt), equalTo(true));
        verify(audioPlayer).playInOrder(asList(new Clip(prompt.getIndex().toString(), REFERENCES.get(0).second)));
    }

    @Test
    public void whenPromptHasAutoplayAudio_butNoAudioURI_returnsFalse() {
        FormEntryPrompt prompt = new MockFormEntryPromptBuilder()
                .withAudioURI(null)
                .withAdditionalAttribute("autoplay", "audio")
                .build();

        assertThat(autoplayer.autoplayIfNeeded(prompt), equalTo(false));
        verify(audioPlayer, never()).playInOrder(any());
    }

    @Test
    public void whenPromptHasAutoplayAudio_andIsSelectOne_playsAudioInOrder() {
        FormEntryPrompt prompt = new MockFormEntryPromptBuilder()
                .withControlType(Constants.CONTROL_SELECT_ONE)
                .withAudioURI(REFERENCES.get(0).first)
                .withAdditionalAttribute("autoplay", "audio")
                .withSelectChoices(asList(
                        new SelectChoice("1", "1"),
                        new SelectChoice("2", "2")
                ))
                .withSpecialFormSelectChoiceText(asList(
                        new Pair<>(FormEntryCaption.TEXT_FORM_AUDIO, REFERENCES.get(1).first),
                        new Pair<>(FormEntryCaption.TEXT_FORM_AUDIO, REFERENCES.get(2).first)
                ))
                .build();

        assertThat(autoplayer.autoplayIfNeeded(prompt), equalTo(true));
        verify(audioPlayer).playInOrder(asList(
                new Clip(prompt.getIndex().toString(), REFERENCES.get(0).second),
                new Clip(prompt.getIndex().toString() + " 0", REFERENCES.get(1).second),
                new Clip(prompt.getIndex().toString() + " 1", REFERENCES.get(2).second)
        ));
    }

    @Test
    public void whenPromptHasAutoplayAudio_andIsSelectMultiple_playsAllAudioInOrder() {
        FormEntryPrompt prompt = new MockFormEntryPromptBuilder()
                .withControlType(Constants.CONTROL_SELECT_MULTI)
                .withAudioURI(REFERENCES.get(0).first)
                .withAdditionalAttribute("autoplay", "audio")
                .withSelectChoices(asList(
                        new SelectChoice("1", "1"),
                        new SelectChoice("2", "2")
                ))
                .withSpecialFormSelectChoiceText(asList(
                        new Pair<>(FormEntryCaption.TEXT_FORM_AUDIO, REFERENCES.get(1).first),
                        new Pair<>(FormEntryCaption.TEXT_FORM_AUDIO, REFERENCES.get(2).first)
                ))
                .build();

        assertThat(autoplayer.autoplayIfNeeded(prompt), equalTo(true));
        verify(audioPlayer).playInOrder(asList(
                new Clip(prompt.getIndex().toString(), REFERENCES.get(0).second),
                new Clip(prompt.getIndex().toString() + " 0", REFERENCES.get(1).second),
                new Clip(prompt.getIndex().toString() + " 1", REFERENCES.get(2).second)
        ));
    }

    @Test
    public void whenPromptHasAutoplayAudio_butNoAudioURI_andIsSelectOne_playsAllSelectAudioInOrder() {
        FormEntryPrompt prompt = new MockFormEntryPromptBuilder()
                .withControlType(Constants.CONTROL_SELECT_ONE)
                .withAdditionalAttribute("autoplay", "audio")
                .withSelectChoices(asList(
                        new SelectChoice("1", "1"),
                        new SelectChoice("2", "2")
                ))
                .withSpecialFormSelectChoiceText(asList(
                        new Pair<>(FormEntryCaption.TEXT_FORM_AUDIO, REFERENCES.get(0).first),
                        new Pair<>(FormEntryCaption.TEXT_FORM_AUDIO, REFERENCES.get(1).first)
                ))
                .build();

        assertThat(autoplayer.autoplayIfNeeded(prompt), equalTo(true));
        verify(audioPlayer).playInOrder(asList(
                new Clip(prompt.getIndex().toString() + " 0", REFERENCES.get(0).second),
                new Clip(prompt.getIndex().toString() + " 1", REFERENCES.get(1).second)
        ));
    }

    @Test
    public void whenPromptHasAutoplayAudio_andIsSelectOne_butNoSelectChoiceAudio_playsPromptAudio() throws Exception {
        FormEntryPrompt prompt = new MockFormEntryPromptBuilder()
                .withControlType(Constants.CONTROL_SELECT_ONE)
                .withAudioURI(REFERENCES.get(0).first)
                .withAdditionalAttribute("autoplay", "audio")
                .withSelectChoices(asList(
                        new SelectChoice("1", "1"),
                        new SelectChoice("2", "2")
                ))
                .build();

        assertThat(autoplayer.autoplayIfNeeded(prompt), equalTo(true));
        verify(audioPlayer).playInOrder(asList(new Clip(prompt.getIndex().toString(), REFERENCES.get(0).second)));
    }

    @Test
    public void whenPromptHasAutoplayAudio_andIsSelectOne_withMinimalAppearance_playsPromptAudio() throws Exception {
        FormEntryPrompt prompt = new MockFormEntryPromptBuilder()
                .withControlType(Constants.CONTROL_SELECT_ONE)
                .withAppearance(MINIMAL)
                .withAudioURI(REFERENCES.get(0).first)
                .withAdditionalAttribute("autoplay", "audio")
                .withSelectChoices(asList(
                        new SelectChoice("1", "1"),
                        new SelectChoice("2", "2")
                ))
                .withSpecialFormSelectChoiceText(asList(
                        new Pair<>(FormEntryCaption.TEXT_FORM_AUDIO, REFERENCES.get(1).first),
                        new Pair<>(FormEntryCaption.TEXT_FORM_AUDIO, REFERENCES.get(2).first)
                ))
                .build();

        assertThat(autoplayer.autoplayIfNeeded(prompt), equalTo(true));
        verify(audioPlayer).playInOrder(asList(new Clip(prompt.getIndex().toString(), REFERENCES.get(0).second)));
    }

    @Test
    public void whenPromptHasAutoplayAudio_andIsSelectOne_withNoButtonsAppearance_playsPromptAudio() throws Exception {
        FormEntryPrompt prompt = new MockFormEntryPromptBuilder()
                .withControlType(Constants.CONTROL_SELECT_ONE)
                .withAppearance("whatever " + NO_BUTTONS)
                .withAudioURI(REFERENCES.get(0).first)
                .withAdditionalAttribute("autoplay", "audio")
                .withSelectChoices(asList(
                        new SelectChoice("1", "1"),
                        new SelectChoice("2", "2")
                ))
                .withSpecialFormSelectChoiceText(asList(
                        new Pair<>(FormEntryCaption.TEXT_FORM_AUDIO, REFERENCES.get(1).first),
                        new Pair<>(FormEntryCaption.TEXT_FORM_AUDIO, REFERENCES.get(2).first)
                ))
                .build();

        assertThat(autoplayer.autoplayIfNeeded(prompt), equalTo(true));
        verify(audioPlayer).playInOrder(asList(new Clip(prompt.getIndex().toString(), REFERENCES.get(0).second)));
    }

    @Test
    public void whenPromptHasAutoplayAudio_andIsSelectOne_withDeprecatedCompactAppearance_playsPromptAudio() throws Exception {
        FormEntryPrompt prompt = new MockFormEntryPromptBuilder()
                .withControlType(Constants.CONTROL_SELECT_ONE)
                .withAppearance(COMPACT)
                .withAudioURI(REFERENCES.get(0).first)
                .withAdditionalAttribute("autoplay", "audio")
                .withSelectChoices(asList(
                        new SelectChoice("1", "1"),
                        new SelectChoice("2", "2")
                ))
                .withSpecialFormSelectChoiceText(asList(
                        new Pair<>(FormEntryCaption.TEXT_FORM_AUDIO, REFERENCES.get(1).first),
                        new Pair<>(FormEntryCaption.TEXT_FORM_AUDIO, REFERENCES.get(2).first)
                ))
                .build();

        assertThat(autoplayer.autoplayIfNeeded(prompt), equalTo(true));
        verify(audioPlayer).playInOrder(asList(new Clip(prompt.getIndex().toString(), REFERENCES.get(0).second)));
    }

    @Test // We only support audio autoplaying with the helper right now
    public void whenPromptHasAutoplayVideo_returnsFalse() {
        FormEntryPrompt prompt = new MockFormEntryPromptBuilder()
                .withAdditionalAttribute("autoplay", "video")
                .build();

        assertThat(autoplayer.autoplayIfNeeded(prompt), equalTo(false));
        verify(audioPlayer, never()).playInOrder(any());
    }

    @Test
    public void whenPromptHasNoAutoplay_returnsFalse() {
        FormEntryPrompt prompt = new MockFormEntryPromptBuilder()
                .withAdditionalAttribute("autoplay", null)
                .build();

        assertThat(autoplayer.autoplayIfNeeded(prompt), equalTo(false));
        verify(audioPlayer, never()).playInOrder(any());
    }

    private static final List<Pair<String, String>> REFERENCES = asList(
            new Pair<>("ref", "file://audio.mp3"),
            new Pair<>("ref1", "file://audio1.mp3"),
            new Pair<>("ref2", "file://audio2.mp3")
    );
}
