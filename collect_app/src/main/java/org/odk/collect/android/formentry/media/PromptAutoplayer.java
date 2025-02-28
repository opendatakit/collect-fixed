package org.odk.collect.android.formentry.media;

import static org.odk.collect.android.formentry.media.FormMediaUtils.getClipID;
import static org.odk.collect.android.formentry.media.FormMediaUtils.getPlayableAudioURI;
import static org.odk.collect.android.utilities.Appearances.NO_BUTTONS;
import static java.util.Collections.emptyList;

import org.javarosa.core.model.Constants;
import org.javarosa.core.model.SelectChoice;
import org.javarosa.core.reference.ReferenceManager;
import org.javarosa.form.api.FormEntryPrompt;
import org.odk.collect.android.utilities.Appearances;
import org.odk.collect.android.utilities.FormEntryPromptUtils;
import org.odk.collect.android.widgets.utilities.AudioPlayer;
import org.odk.collect.audioclips.Clip;

import java.util.ArrayList;
import java.util.List;

public class PromptAutoplayer {

    private static final String AUTOPLAY_ATTRIBUTE = "autoplay";
    private static final String AUDIO_OPTION = "audio";

    private final AudioPlayer audioHelper;
    private final ReferenceManager referenceManager;

    public PromptAutoplayer(AudioPlayer audipPlayer, ReferenceManager referenceManager) {
        this.audioHelper = audipPlayer;
        this.referenceManager = referenceManager;
    }

    public Boolean autoplayIfNeeded(FormEntryPrompt prompt) {
        String autoplayOption = FormEntryPromptUtils.getAdditionalAttribute(prompt, AUTOPLAY_ATTRIBUTE);

        if (hasAudioAutoplay(autoplayOption)) {
            List<Clip> clipsToPlay = new ArrayList<>();

            Clip promptClip = getPromptClip(prompt);
            if (promptClip != null) {
                clipsToPlay.add(promptClip);
            }

            List<Clip> selectClips = getSelectClips(prompt);
            if (!selectClips.isEmpty()) {
                clipsToPlay.addAll(selectClips);
            }

            if (clipsToPlay.isEmpty()) {
                return false;
            } else {
                audioHelper.playInOrder(clipsToPlay);
                return true;
            }
        } else {
            return false;
        }
    }

    private boolean hasAudioAutoplay(String autoplayOption) {
        return autoplayOption != null && autoplayOption.equalsIgnoreCase(AUDIO_OPTION);
    }

    private List<Clip> getSelectClips(FormEntryPrompt prompt) {
        if (appearanceDoesNotShowControls(Appearances.getSanitizedAppearanceHint(prompt))) {
            return emptyList();
        }

        List<Clip> selectClips = new ArrayList<>();

        int controlType = prompt.getControlType();
        if (controlType == Constants.CONTROL_SELECT_ONE || controlType == Constants.CONTROL_SELECT_MULTI) {

            List<SelectChoice> selectChoices = prompt.getSelectChoices();

            for (SelectChoice choice : selectChoices) {
                String selectURI = getPlayableAudioURI(prompt, choice, referenceManager);

                if (selectURI != null) {
                    Clip clip = new Clip(getClipID(prompt, choice), selectURI);
                    selectClips.add(clip);
                }
            }
        }

        return selectClips;
    }

    private boolean appearanceDoesNotShowControls(String appearance) {
        return appearance.startsWith(Appearances.MINIMAL) ||
                appearance.startsWith(Appearances.COMPACT) ||
                appearance.contains(NO_BUTTONS);
    }

    private Clip getPromptClip(FormEntryPrompt prompt) {
        String uri = getPlayableAudioURI(prompt, referenceManager);
        if (uri != null) {
            return new Clip(
                    getClipID(prompt),
                    getPlayableAudioURI(prompt, referenceManager)
            );
        } else {
            return null;
        }
    }
}
