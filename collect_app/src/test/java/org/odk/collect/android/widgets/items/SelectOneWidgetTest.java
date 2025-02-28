package org.odk.collect.android.widgets.items;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.odk.collect.android.support.CollectHelpers.setupFakeReferenceManager;
import static org.odk.collect.testshared.RobolectricHelpers.populateRecyclerView;
import static java.util.Arrays.asList;

import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.core.util.Pair;
import androidx.recyclerview.widget.GridLayoutManager;

import com.google.android.flexbox.FlexboxLayoutManager;

import org.javarosa.core.model.SelectChoice;
import org.javarosa.core.reference.ReferenceManager;
import org.javarosa.form.api.FormEntryCaption;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.odk.collect.android.R;
import org.odk.collect.android.formentry.questions.AudioVideoImageTextLabel;
import org.odk.collect.android.formentry.questions.NoButtonsItem;
import org.odk.collect.android.formentry.questions.QuestionDetails;
import org.odk.collect.android.injection.config.AppDependencyModule;
import org.odk.collect.android.listeners.AdvanceToNextListener;
import org.odk.collect.android.listeners.WidgetValueChangedListener;
import org.odk.collect.android.support.CollectHelpers;
import org.odk.collect.android.support.MockFormEntryPromptBuilder;
import org.odk.collect.android.utilities.Appearances;
import org.odk.collect.android.utilities.SoftKeyboardController;
import org.odk.collect.android.widgets.QuestionWidget;
import org.odk.collect.android.widgets.base.GeneralSelectOneWidgetTest;
import org.odk.collect.android.widgets.support.FormEntryPromptSelectChoiceLoader;
import org.odk.collect.android.widgets.utilities.AudioPlayer;
import org.odk.collect.android.widgets.utilities.QuestionFontSizeUtils;
import org.odk.collect.audioclips.Clip;

import java.util.List;

/**
 * @author James Knight
 */
public class SelectOneWidgetTest extends GeneralSelectOneWidgetTest<SelectOneWidget> {
    @Mock
    private AdvanceToNextListener listener;

    @NonNull
    @Override
    public SelectOneWidget createWidget() {
        SelectOneWidget selectOneWidget = new SelectOneWidget(activity, new QuestionDetails(formEntryPrompt), isQuick(), null, new FormEntryPromptSelectChoiceLoader(), dependencies);
        if (isQuick()) {
            selectOneWidget.setListener(listener);
        }
        selectOneWidget.setFocus(activity);
        return selectOneWidget;
    }

    @Rule
    public MockitoRule rule = MockitoJUnit.rule();

    @Before
    public void setup() throws Exception {
        overrideDependencyModule();
    }

    @Test
    public void byDefault_shouldGridLayoutManagerBeUsed() {
        assertThat(getWidget().binding.choicesRecyclerView.getLayoutManager().getClass().getName(), is(GridLayoutManager.class.getName()));
    }

    @Test
    public void whenColumnsPackAppearanceExist_shouldFlexboxLayoutManagerBeUsed() {
        when(formEntryPrompt.getAppearanceHint()).thenReturn("columns-pack");
        assertThat(getWidget().binding.choicesRecyclerView.getLayoutManager().getClass().getName(), is(FlexboxLayoutManager.class.getName()));
    }

    @Test
    public void whenButtonsModeExist_shouldFrameLayoutBeUsedAsItemView() {
        populateRecyclerView(getWidget());
        assertThat(getChoiceView(getWidget(), 0).getClass().getName(), is(AudioVideoImageTextLabel.class.getName()));
    }

    @Test
    public void whenNoButtonsModeExist_shouldFrameLayoutBeUsedAsItemView() {
        when(formEntryPrompt.getAppearanceHint()).thenReturn("no-buttons");
        populateRecyclerView(getWidget());
        assertThat(getChoiceView(getWidget(), 0).getClass().getName(), is(NoButtonsItem.class.getName()));
    }

    @Test
    public void whenAutocompleteAppearanceExist_shouldTextSizeBeSetProperly() {
        when(formEntryPrompt.getAppearanceHint()).thenReturn("autocomplete");
        assertThat((int) getSpyWidget().binding.choicesSearchBox.getTextSize(), is(QuestionFontSizeUtils.getFontSize(settingsProvider.getUnprotectedSettings(), QuestionFontSizeUtils.FontSize.HEADLINE_6)));
    }

    @Test
    public void whenAutocompleteAppearanceExist_shouldSearchBoxBeVisible() {
        when(formEntryPrompt.getAppearanceHint()).thenReturn("autocomplete");
        assertThat(getSpyWidget().binding.choicesSearchBox.getVisibility(), is(View.VISIBLE));
    }

    @Test
    public void whenAutocompleteAppearanceDoesNotExist_shouldSearchBoxBeHidden() {
        when(formEntryPrompt.getAppearanceHint()).thenReturn("");
        assertThat(getSpyWidget().binding.choicesSearchBox.getVisibility(), is(View.GONE));
    }

    @Test
    public void whenAutocompleteAppearanceDoesNotExist_shouldNotKeyboardBeDisplayed() {
        SelectOneWidget widget = getSpyWidget();
        verify(widget.softKeyboardController, never()).showSoftKeyboard(widget.binding.choicesSearchBox);
    }

    @Test
    public void whenAutocompleteAppearanceExist_shouldKeyboardBeDisplayed() {
        when(formEntryPrompt.getAppearanceHint()).thenReturn("autocomplete");
        SelectOneWidget widget = getSpyWidget();
        verify(widget.softKeyboardController).showSoftKeyboard(widget.binding.choicesSearchBox);
    }

    @Test
    public void whenAutocompleteAppearanceExistAndWidgetIsReadOnly_shouldNotKeyboardBeDisplayed() {
        when(formEntryPrompt.getAppearanceHint()).thenReturn("autocomplete");
        when(formEntryPrompt.isReadOnly()).thenReturn(true);
        SelectOneWidget widget = getSpyWidget();
        verify(widget.softKeyboardController, never()).showSoftKeyboard(widget.binding.choicesSearchBox);
    }

    @Test
    public void whenQuickAppearanceIsUsed_shouldAdvanceToNextListenerBeCalledInButtonsMode() {
        formEntryPrompt = new MockFormEntryPromptBuilder()
                .withSelectChoices(asList(
                        new SelectChoice("AAA", "AAA"),
                        new SelectChoice("BBB", "BBB")
                ))
                .withAppearance("quick")
                .build();

        SelectOneWidget widget = getWidget();
        populateRecyclerView(widget);

        clickChoice(widget, 0); // Select AAA
        assertThat(widget.getAnswer().getDisplayText(), is("AAA"));

        verify(listener).advance();
    }

    @Test
    public void whenQuickAppearanceIsUsed_shouldAdvanceToNextListenerBeCalledInNoButtonsMode() {
        formEntryPrompt = new MockFormEntryPromptBuilder()
                .withSelectChoices(asList(
                        new SelectChoice("AAA", "AAA"),
                        new SelectChoice("BBB", "BBB")
                ))
                .withAppearance("quick no-buttons")
                .build();

        SelectOneWidget widget = getWidget();
        populateRecyclerView(widget);

        clickChoice(widget, 0); // Select AAA
        assertThat(widget.getAnswer().getDisplayText(), is("AAA"));

        verify(listener).advance();
    }

    @Test
    public void whenQuickAppearanceIsNotUsed_shouldNotAdvanceToNextListenerBeCalledInButtonsMode() {
        formEntryPrompt = new MockFormEntryPromptBuilder()
                .withSelectChoices(asList(
                        new SelectChoice("AAA", "AAA"),
                        new SelectChoice("BBB", "BBB")
                ))
                .build();

        SelectOneWidget widget = getWidget();
        populateRecyclerView(widget);

        clickChoice(widget, 0); // Select AAA
        assertThat(widget.getAnswer().getDisplayText(), is("AAA"));

        verify(listener, times(0)).advance();
    }

    @Test
    public void whenQuickAppearanceIsNotUsed_shouldNotAdvanceToNextListenerBeCalledInNoButtonsMode() {
        formEntryPrompt = new MockFormEntryPromptBuilder()
                .withSelectChoices(asList(
                        new SelectChoice("AAA", "AAA"),
                        new SelectChoice("BBB", "BBB")
                ))
                .withAppearance("no-buttons")
                .build();

        SelectOneWidget widget = getWidget();
        populateRecyclerView(widget);

        clickChoice(widget, 0); // Select AAA
        assertThat(widget.getAnswer().getDisplayText(), is("AAA"));

        verify(listener, times(0)).advance();
    }

    @Test
    public void whenChoicesHaveAudio_audioButtonUsesIndexAsClipID() {
        formEntryPrompt = new MockFormEntryPromptBuilder()
                .withIndex("i am index")
                .withSelectChoices(asList(
                        new SelectChoice("1", "1"),
                        new SelectChoice("2", "2")
                ))
                .withSpecialFormSelectChoiceText(asList(
                        new Pair<>(FormEntryCaption.TEXT_FORM_AUDIO, REFERENCES.get(0).first),
                        new Pair<>(FormEntryCaption.TEXT_FORM_AUDIO, REFERENCES.get(1).first)
                ))
                .build();

        AudioPlayer audioPlayer = mock();
        dependencies = new QuestionWidget.Dependencies(audioPlayer);
        populateRecyclerView(getWidget());

        getChoiceView(getWidget(), 0).findViewById(R.id.audioButton).performClick();
        verify(audioPlayer).play(eq(new Clip("i am index 0", REFERENCES.get(0).second)));
        getChoiceView(getWidget(), 1).findViewById(R.id.audioButton).performClick();
        verify(audioPlayer).play(eq(new Clip("i am index 1", REFERENCES.get(1).second)));
    }

    @Test
    public void whenAChoiceValueIsNull_selecting_doesNotSetAnswer() {
        SelectChoice selectChoice = new SelectChoice(); // The two arg constructor protects against null values
        selectChoice.setTextID("1");

        formEntryPrompt = new MockFormEntryPromptBuilder()
                .withSelectChoices(asList(selectChoice))
                .build();

        SelectOneWidget widget = getWidget();
        populateRecyclerView(widget);

        clickChoice(widget, 0);
        assertThat(widget.getAnswer(), nullValue());
    }

    @Test
    public void usingReadOnlyOptionShouldMakeAllClickableElementsDisabled() {
        // No appearance
        formEntryPrompt = new MockFormEntryPromptBuilder()
                .withIndex("i am index")
                .withSelectChoices(asList(
                        new SelectChoice("1", "1"),
                        new SelectChoice("2", "2")
                ))
                .withReadOnly(true)
                .build();

        populateRecyclerView(getWidget());

        AudioVideoImageTextLabel avitLabel = (AudioVideoImageTextLabel) getSpyWidget().binding.choicesRecyclerView.getLayoutManager().getChildAt(0);
        assertThat(avitLabel.isEnabled(), is(Boolean.FALSE));

        resetWidget();

        // No-buttons appearance
        formEntryPrompt = new MockFormEntryPromptBuilder(formEntryPrompt)
                .withAppearance(Appearances.NO_BUTTONS)
                .build();

        populateRecyclerView(getWidget());

        FrameLayout view = (FrameLayout) getSpyWidget().binding.choicesRecyclerView.getLayoutManager().getChildAt(0);
        assertThat(view.isEnabled(), is(Boolean.FALSE));
    }

    @Test
    public void clickingItem_callsValueChangedListener() {
        formEntryPrompt = new MockFormEntryPromptBuilder()
                .withIndex("i am index")
                .withSelectChoices(asList(
                        new SelectChoice("1", "1"),
                        new SelectChoice("2", "2")
                ))
                .build();

        SelectOneWidget widget = getWidget();
        WidgetValueChangedListener valueChangedListener = mock();
        widget.setValueChangedListener(valueChangedListener);
        populateRecyclerView(widget);

        ((AudioVideoImageTextLabel) getWidget().binding.choicesRecyclerView.getChildAt(0)).getLabelTextView().performClick();
        verify(valueChangedListener).widgetValueChanged(widget);
    }

    @Test
    public void whenPromptHasNoButtonsAppearance_clickingItem_callsValueChangedListener() {
        formEntryPrompt = new MockFormEntryPromptBuilder()
                .withIndex("i am index")
                .withAppearance("no-buttons")
                .withSelectChoices(asList(
                        new SelectChoice("1", "1"),
                        new SelectChoice("2", "2")
                ))
                .build();

        SelectOneWidget widget = getWidget();
        WidgetValueChangedListener valueChangedListener = mock();
        widget.setValueChangedListener(valueChangedListener);
        populateRecyclerView(widget);

        getWidget().binding.choicesRecyclerView.getChildAt(0).performClick();
        verify(valueChangedListener).widgetValueChanged(widget);
    }

    private void overrideDependencyModule() throws Exception {
        ReferenceManager referenceManager = setupFakeReferenceManager(REFERENCES);
        CollectHelpers.overrideAppDependencyModule(new AppDependencyModule() {

            @Override
            public ReferenceManager providesReferenceManager() {
                return referenceManager;
            }

            @Override
            public SoftKeyboardController provideSoftKeyboardController() {
                return mock(SoftKeyboardController.class);
            }
        });
    }

    private void clickChoice(SelectOneWidget widget, int index) {
        if (Appearances.isNoButtonsAppearance(formEntryPrompt)) {
            clickNoButtonChoice(widget, index);
        } else {
            clickButtonChoice(widget, index);
        }
    }

    private void clickNoButtonChoice(SelectOneWidget widget, int index) {
        widget.binding.choicesRecyclerView.getChildAt(index).performClick();
    }

    private void clickButtonChoice(SelectOneWidget widget, int index) {
        ((AudioVideoImageTextLabel) getChoiceView(widget, index)).getLabelTextView().performClick();
    }

    private ViewGroup getChoiceView(SelectOneWidget widget, int index) {
        return (ViewGroup) widget.binding.choicesRecyclerView.getChildAt(index);
    }

    private static final List<Pair<String, String>> REFERENCES = asList(
            new Pair<>("ref", "file://audio.mp3"),
            new Pair<>("ref1", "file://audio1.mp3")
    );

    private boolean isQuick() {
        return Appearances.getSanitizedAppearanceHint(formEntryPrompt).contains("quick");
    }
}
