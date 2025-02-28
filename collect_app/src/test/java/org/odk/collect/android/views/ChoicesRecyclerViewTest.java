package org.odk.collect.android.views;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.odk.collect.testshared.RobolectricHelpers.populateRecyclerView;
import static java.util.Arrays.asList;

import android.app.Activity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.FrameLayout;
import android.widget.RadioButton;
import android.widget.TextView;

import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.google.android.flexbox.FlexboxLayoutManager;

import org.javarosa.core.model.SelectChoice;
import org.javarosa.core.model.data.helper.Selection;
import org.javarosa.core.reference.InvalidReferenceException;
import org.javarosa.core.reference.Reference;
import org.javarosa.core.reference.ReferenceManager;
import org.javarosa.form.api.FormEntryPrompt;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.odk.collect.android.R;
import org.odk.collect.android.adapters.AbstractSelectListAdapter;
import org.odk.collect.android.adapters.SelectMultipleListAdapter;
import org.odk.collect.android.adapters.SelectOneListAdapter;
import org.odk.collect.android.formentry.questions.AudioVideoImageTextLabel;
import org.odk.collect.android.listeners.SelectItemClickListener;
import org.odk.collect.android.support.CollectHelpers;
import org.odk.collect.android.support.MockFormEntryPromptBuilder;
import org.odk.collect.android.support.WidgetTestActivity;
import org.odk.collect.android.utilities.Appearances;
import org.odk.collect.android.utilities.MediaUtils;
import org.odk.collect.imageloader.ImageLoader;
import org.odk.collect.testshared.RobolectricHelpers;
import org.robolectric.android.controller.ActivityController;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

@RunWith(AndroidJUnit4.class)
public class ChoicesRecyclerViewTest {
    private ChoicesRecyclerView recyclerView;

    private ActivityController<WidgetTestActivity> activityController;

    private FormEntryPrompt formEntryPrompt;
    private ReferenceManager referenceManager;

    @Before
    public void setUp() throws InvalidReferenceException {
        activityController = CollectHelpers.buildThemedActivity(WidgetTestActivity.class);
        Activity activity = activityController.get();
        FrameLayout frameLayout = new FrameLayout(activity);
        activity.setContentView(frameLayout);
        activityController.create().start().visible();
        recyclerView = new ChoicesRecyclerView(activity);
        frameLayout.addView(recyclerView);
        populateRecyclerView(recyclerView);
        setUpReferenceManager();
    }

    @Test
    public void whenNonFLexAppearanceIsUsed_shouldGridLayoutManagerBeUsed() {
        SelectOneListAdapter adapter = new SelectOneListAdapter(null, null, null, new ArrayList<>(), null, null, null, 0, 1, false, mock(MediaUtils.class));
        initRecyclerView(adapter, false);
        assertThat(recyclerView.getLayoutManager().getClass().getName(), equalTo(GridLayoutManager.class.getName()));
    }

    @Test
    public void whenFLexAppearanceIsUsed_shouldFlexboxLayoutManagerBeUsed() {
        SelectOneListAdapter adapter = new SelectOneListAdapter(null, null, null, new ArrayList<>(), null, null, null, 0, 1, false, mock(MediaUtils.class));
        initRecyclerView(adapter, true);
        assertThat(recyclerView.getLayoutManager().getClass().getName(), equalTo(FlexboxLayoutManager.class.getName()));
    }

    @Test
    public void whenNonFLexAppearanceIsUsedWithOneColumn_shouldDividersBeAdded() {
        List<SelectChoice> items = getTestChoices();
        setUpFormEntryPrompt(items, "");

        SelectOneListAdapter adapter = new SelectOneListAdapter(null, null, activityController.get(), items, formEntryPrompt, null, null, 0, 1, false, mock(MediaUtils.class));

        initRecyclerView(adapter, false);

        assertThat(recyclerView.getItemDecorationCount(), is(1));
        assertThat(recyclerView.getItemDecorationAt(0), is(instanceOf(DividerItemDecoration.class)));
    }

    @Test
    public void whenNonFLexAppearanceIsUsedWithMoreThanOneColumn_shouldNotDividersBeAdded() {
        List<SelectChoice> items = getTestChoices();
        setUpFormEntryPrompt(items, "");

        SelectOneListAdapter adapter = new SelectOneListAdapter(null, null, activityController.get(), items, formEntryPrompt, null, null, 0, 2, false, mock(MediaUtils.class));

        initRecyclerView(adapter, false);

        assertThat(recyclerView.getItemDecorationCount(), is(0));
    }

    @Test
    public void whenFLexAppearanceIsUsed_shouldFlexItemDecorationBeAdded() {
        List<SelectChoice> items = getTestChoices();
        setUpFormEntryPrompt(items, "");

        SelectOneListAdapter adapter = new SelectOneListAdapter(null, null, activityController.get(), items, formEntryPrompt, null, null, 0, 2, false, mock(MediaUtils.class));

        initRecyclerView(adapter, true);

        assertThat(recyclerView.getItemDecorationCount(), is(1));
        assertThat(recyclerView.getItemDecorationAt(0), is(instanceOf(ChoicesRecyclerView.FlexItemDecoration.class)));
    }

    @Test
    public void whenChoicesFiltered_shouldProperValuesBeReturnedInSelectOneButtonsMode() {
        List<SelectChoice> items = getTestChoices();
        setUpFormEntryPrompt(items, "");

        SelectOneListAdapter adapter = new SelectOneListAdapter(null, null, activityController.get(), items, formEntryPrompt, null, null, 0, 1, false, mock(MediaUtils.class));

        initRecyclerView(adapter, false);

        assertVisibleItemsInButtonsMode("AAA", "BBB");
        filterList(adapter, "b");
        assertVisibleItemsInButtonsMode("BBB");
        filterList(adapter, "bc");
        assertVisibleItemsInButtonsMode();
        filterList(adapter, "b");
        assertVisibleItemsInButtonsMode("BBB");
        filterList(adapter, "");
        assertVisibleItemsInButtonsMode("AAA", "BBB");
    }

    @Test
    public void whenChoicesFiltered_shouldProperValuesBeReturnedInSelectMultiButtonsMode() {
        List<SelectChoice> items = getTestChoices();
        setUpFormEntryPrompt(items, "");

        SelectMultipleListAdapter adapter = new SelectMultipleListAdapter(new ArrayList<>(), null, activityController.get(), items, formEntryPrompt, null, null, 0, 1, false, mock(MediaUtils.class));

        initRecyclerView(adapter, false);

        assertVisibleItemsInButtonsMode("AAA", "BBB");
        filterList(adapter, "b");
        assertVisibleItemsInButtonsMode("BBB");
        filterList(adapter, "bc");
        assertVisibleItemsInButtonsMode();
        filterList(adapter, "b");
        assertVisibleItemsInButtonsMode("BBB");
        filterList(adapter, "");
        assertVisibleItemsInButtonsMode("AAA", "BBB");
    }

    @Test
    public void whenChoicesFiltered_shouldProperValuesBeReturnedInSelectOneNoButtonsMode() {
        List<SelectChoice> items = getTestChoices();
        setUpFormEntryPrompt(items, "no-buttons");

        SelectOneListAdapter adapter = new SelectOneListAdapter(null, null, activityController.get(), items, formEntryPrompt, null, null, 0, 1, true, mock(MediaUtils.class));

        initRecyclerView(adapter, false);

        assertVisibleItemsInNoButtonsMode("AAA", "BBB");
        filterList(adapter, "b");
        assertVisibleItemsInNoButtonsMode("BBB");
        filterList(adapter, "bc");
        assertVisibleItemsInNoButtonsMode();
        filterList(adapter, "b");
        assertVisibleItemsInNoButtonsMode("BBB");
        filterList(adapter, "");
        assertVisibleItemsInNoButtonsMode("AAA", "BBB");
    }

    @Test
    public void whenChoicesFiltered_shouldProperValuesBeReturnedInSelectMultiNoButtonsMode() {
        List<SelectChoice> items = getTestChoices();
        setUpFormEntryPrompt(items, "no-buttons");

        SelectMultipleListAdapter adapter = new SelectMultipleListAdapter(new ArrayList<>(), null, activityController.get(), items, formEntryPrompt, null, null, 0, 1, true, mock(MediaUtils.class));

        initRecyclerView(adapter, false);

        assertVisibleItemsInNoButtonsMode("AAA", "BBB");
        filterList(adapter, "b");
        assertVisibleItemsInNoButtonsMode("BBB");
        filterList(adapter, "bc");
        assertVisibleItemsInNoButtonsMode();
        filterList(adapter, "b");
        assertVisibleItemsInNoButtonsMode("BBB");
        filterList(adapter, "");
        assertVisibleItemsInNoButtonsMode("AAA", "BBB");
    }

    @Test
    public void whenClickOneOption_shouldPreviouslySelectedOptionBeUnselectedInSelectOneButtonsMode() {
        List<SelectChoice> items = getTestChoices();
        setUpFormEntryPrompt(items, "");

        SelectItemClickListener listener = mock(SelectItemClickListener.class);
        SelectOneListAdapter adapter = new SelectOneListAdapter(null, listener, activityController.get(), items, formEntryPrompt, null, null, 0, 1, false, mock(MediaUtils.class));

        initRecyclerView(adapter, false);

        clickChoice(0); // Select AAA
        assertThat(isItemSelected(0), is(true));
        assertThat(isItemSelected(1), is(false));

        clickChoice(1); // Select BBB
        assertThat(isItemSelected(0), is(false));
        assertThat(isItemSelected(1), is(true));
    }

    @Test
    public void whenClickOneOption_shouldPreviouslySelectedOptionRemainSelectedInSelectMultiButtonsMode() {
        List<SelectChoice> items = getTestChoices();
        setUpFormEntryPrompt(items, "");

        SelectItemClickListener listener = mock(SelectItemClickListener.class);
        SelectMultipleListAdapter adapter = new SelectMultipleListAdapter(new ArrayList<>(), listener, activityController.get(), items, formEntryPrompt, null, null, 0, 1, false, mock(MediaUtils.class));

        initRecyclerView(adapter, false);

        clickChoice(0); // Select AAA
        assertThat(isItemSelected(0), is(true));
        assertThat(isItemSelected(1), is(false));

        clickChoice(1); // Select BBB
        assertThat(isItemSelected(0), is(true));
        assertThat(isItemSelected(1), is(true));
    }

    @Test
    public void whenClickOneOption_shouldPreviouslySelectedOptionBeUnselectedInSelectOneNoButtonsMode() {
        List<SelectChoice> items = getTestChoices();
        setUpFormEntryPrompt(items, "no-buttons");

        SelectItemClickListener listener = mock(SelectItemClickListener.class);
        SelectOneListAdapter adapter = new SelectOneListAdapter(null, listener, activityController.get(), items, formEntryPrompt, null, mock(), 0, 1, true, mock(MediaUtils.class));

        initRecyclerView(adapter, false);

        clickChoice(0); // Select AAA
        assertThat(isItemSelected(0), is(true));
        assertThat(isItemSelected(1), is(false));

        clickChoice(1); // Select BBB
        assertThat(isItemSelected(0), is(false));
        assertThat(isItemSelected(1), is(true));
    }

    @Test
    public void whenClickOneOption_shouldPreviouslySelectedOptionRemainSelectedInSelectMultiNoButtonsMode() {
        List<SelectChoice> items = getTestChoices();
        setUpFormEntryPrompt(items, "no-buttons");

        SelectItemClickListener listener = mock(SelectItemClickListener.class);
        SelectMultipleListAdapter adapter = new SelectMultipleListAdapter(new ArrayList<>(), listener, activityController.get(), items, formEntryPrompt, null, mock(), 0, 1, true, mock(MediaUtils.class));

        initRecyclerView(adapter, false);

        clickChoice(0); // Select AAA
        assertThat(isItemSelected(0), is(true));
        assertThat(isItemSelected(1), is(false));

        clickChoice(1); // Select BBB
        assertThat(isItemSelected(0), is(true));
        assertThat(isItemSelected(1), is(true));
    }

    @Test
    public void whenClickOneElementTwice_shouldThatElementRemainSelectedInSelectOneButtonsMode() {
        List<SelectChoice> items = getTestChoices();
        setUpFormEntryPrompt(items, "");

        SelectItemClickListener listener = mock(SelectItemClickListener.class);
        SelectOneListAdapter adapter = new SelectOneListAdapter(null, listener, activityController.get(), items, formEntryPrompt, null, null, 0, 1, false, mock(MediaUtils.class));

        initRecyclerView(adapter, false);

        clickChoice(0); // Select AAA
        assertThat(isItemSelected(0), is(true));
        assertThat(isItemSelected(1), is(false));

        clickChoice(0); // Select AAA again
        assertThat(isItemSelected(0), is(true));
        assertThat(isItemSelected(1), is(false));
    }

    @Test
    public void whenClickOneElementTwice_shouldThatElementBeUnselectedInSelectMultiButtonsMode() {
        List<SelectChoice> items = getTestChoices();
        setUpFormEntryPrompt(items, "");

        SelectItemClickListener listener = mock(SelectItemClickListener.class);
        SelectMultipleListAdapter adapter = new SelectMultipleListAdapter(new ArrayList<>(), listener, activityController.get(), items, formEntryPrompt, null, null, 0, 1, false, mock(MediaUtils.class));

        initRecyclerView(adapter, false);

        clickChoice(0); // Select AAA
        assertThat(isItemSelected(0), is(true));
        assertThat(isItemSelected(1), is(false));

        clickChoice(0); // Select AAA again
        assertThat(isItemSelected(0), is(false));
        assertThat(isItemSelected(1), is(false));
    }

    @Test
    public void whenClickOneElementTwice_shouldThatElementRemainSelectedInSelectOneNoButtonsMode() {
        List<SelectChoice> items = getTestChoices();
        setUpFormEntryPrompt(items, "no-buttons");

        SelectItemClickListener listener = mock(SelectItemClickListener.class);
        SelectOneListAdapter adapter = new SelectOneListAdapter(null, listener, activityController.get(), items, formEntryPrompt, null, mock(), 0, 1, true, mock(MediaUtils.class));

        initRecyclerView(adapter, false);

        clickChoice(0); // Select AAA
        assertThat(isItemSelected(0), is(true));
        assertThat(isItemSelected(1), is(false));

        clickChoice(0); // Select AAA again
        assertThat(isItemSelected(0), is(true));
        assertThat(isItemSelected(1), is(false));
    }

    @Test
    public void whenClickOneElementTwice_shouldThatElementBeUnselectedInSelectMultiNoButtonsMode() {
        List<SelectChoice> items = getTestChoices();
        setUpFormEntryPrompt(items, "no-buttons");

        SelectItemClickListener listener = mock(SelectItemClickListener.class);
        SelectMultipleListAdapter adapter = new SelectMultipleListAdapter(new ArrayList<>(), listener, activityController.get(), items, formEntryPrompt, null, mock(), 0, 1, true, mock(MediaUtils.class));

        initRecyclerView(adapter, false);

        clickChoice(0); // Select AAA
        assertThat(isItemSelected(0), is(true));
        assertThat(isItemSelected(1), is(false));

        clickChoice(0); // Select AAA again
        assertThat(isItemSelected(0), is(false));
        assertThat(isItemSelected(1), is(false));
    }

    @Test
    public void whenButtonsModeIsUsed_shouldViewAndItsElementsBeLongClickableToSupportRemovingAnswers() {
        List<SelectChoice> items = getTestChoices();
        setUpFormEntryPrompt(items, "");

        SelectItemClickListener listener = mock(SelectItemClickListener.class);
        SelectMultipleListAdapter adapter = new SelectMultipleListAdapter(new ArrayList<>(), listener, activityController.get(), items, formEntryPrompt, null, null, 0, 1, false, mock(MediaUtils.class));

        initRecyclerView(adapter, false);

        AudioVideoImageTextLabel view = (AudioVideoImageTextLabel) getChoiceView(0);
        File file = mock(File.class);
        when(file.exists()).thenReturn(true);
        view.setImage(file, mock(ImageLoader.class));
        view.setVideo(file);
        view.setAudio("file://audio.mp3", mock());

        assertThat(view.isLongClickable(), is(true));
        assertThat(view.getImageView().isLongClickable(), is(true));
        assertThat(view.getVideoButton().isLongClickable(), is(true));
        assertThat(view.getAudioButton().isLongClickable(), is(true));
        assertThat(view.getLabelTextView().isLongClickable(), is(true));
    }

    @Test
    public void whenNoButtonsModeIsUsed_shouldViewBeLongClickableToSupportRemovingAnswers() {
        List<SelectChoice> items = getTestChoices();
        setUpFormEntryPrompt(items, "no-buttons");

        SelectItemClickListener listener = mock(SelectItemClickListener.class);
        SelectMultipleListAdapter adapter = new SelectMultipleListAdapter(new ArrayList<>(), listener, activityController.get(), items, formEntryPrompt, null, null, 0, 1, true, mock(MediaUtils.class));

        initRecyclerView(adapter, false);

        assertThat(getChoiceView(0).isLongClickable(), is(true));
    }

    @Test
    public void whenChangingAnswer_shouldHasAnswerChangedReturnCorrectValue() {
        List<SelectChoice> items = getTestChoices();
        setUpFormEntryPrompt(items, "");

        SelectItemClickListener listener = mock(SelectItemClickListener.class);
        List<Selection> selectedItems = new ArrayList<>();
        selectedItems.add(items.get(0).selection());
        SelectMultipleListAdapter adapter = new SelectMultipleListAdapter(selectedItems, listener, activityController.get(), items, formEntryPrompt, null, null, 0, 1, false, mock(MediaUtils.class));

        initRecyclerView(adapter, false);

        clickChoice(1); // Select BBB
        assertThat(adapter.hasAnswerChanged(), is(true));

        clickChoice(0); // Unselect AAA
        assertThat(adapter.hasAnswerChanged(), is(true));

        clickChoice(1); // Unselect BBB
        assertThat(adapter.hasAnswerChanged(), is(true));

        clickChoice(0); // Select AAA
        assertThat(adapter.hasAnswerChanged(), is(false));
    }

    @Test
    public void whenChoiceSelectedInSelectOneNoButtonsMode_shouldTryToPlayAudio() {
        List<SelectChoice> items = getTestChoices();
        setUpFormEntryPrompt(items, "no-buttons");

        SelectItemClickListener listener = mock(SelectItemClickListener.class);
        SelectOneListAdapter adapter = spy(new SelectOneListAdapter(null, listener, activityController.get(), items, formEntryPrompt, null, mock(), 0, 1, true, mock(MediaUtils.class)));

        initRecyclerView(adapter, false);

        clickChoice(0); // Select AAA
        verify(adapter).playAudio(any());
    }

    @Test
    public void whenChoiceSelectedInSelectMultiNoButtonsMode_shouldTryToPlayAudio() {
        List<SelectChoice> items = getTestChoices();
        setUpFormEntryPrompt(items, "no-buttons");

        SelectItemClickListener listener = mock(SelectItemClickListener.class);
        SelectMultipleListAdapter adapter = spy(new SelectMultipleListAdapter(new ArrayList<>(), listener, activityController.get(), items, formEntryPrompt, null, mock(), 0, 1, true, mock(MediaUtils.class)));
        initRecyclerView(adapter, false);

        clickChoice(0); // Select AAA
        verify(adapter).playAudio(any());
    }

    @Test
    public void whenChoiceUnselectedInSelectMultiNoButtonsMode_shouldStopPlayingAudio() {
        List<SelectChoice> items = getTestChoices();
        setUpFormEntryPrompt(items, "no-buttons");

        SelectItemClickListener listener = mock(SelectItemClickListener.class);
        List<Selection> selectedItems = new ArrayList<>();
        selectedItems.add(items.get(0).selection());
        SelectMultipleListAdapter adapter = spy(new SelectMultipleListAdapter(selectedItems, listener, activityController.get(), items, formEntryPrompt, null, mock(), 0, 1, true, mock(MediaUtils.class)));
        initRecyclerView(adapter, false);

        clickChoice(0); // Unselect AAA
        verify(adapter.getAudioPlayer()).stop();
        verify(adapter, never()).playAudio(any());
    }

    @Test
    public void whenColumnsPackAppearanceIsUsed_shouldMediaElementsBeHidden() {
        List<SelectChoice> items = getTestChoices();
        setUpFormEntryPrompt(items, "columns-pack");

        SelectItemClickListener listener = mock(SelectItemClickListener.class);
        SelectMultipleListAdapter adapter = spy(new SelectMultipleListAdapter(new ArrayList<>(), listener, activityController.get(), items, formEntryPrompt, referenceManager, null, 0, 1, false, mock(MediaUtils.class)));
        initRecyclerView(adapter, true);

        assertThat(getAudioVideoImageTextLabelView(0).getImageView().getVisibility(), is(View.GONE));
        assertThat(getAudioVideoImageTextLabelView(0).getVideoButton().getVisibility(), is(View.GONE));
        assertThat(getAudioVideoImageTextLabelView(0).getAudioButton().getVisibility(), is(View.GONE));
    }

    private void setUpReferenceManager() throws InvalidReferenceException {
        referenceManager = mock(ReferenceManager.class);
        Reference reference = mock(Reference.class);
        when(reference.getLocalURI()).thenReturn("");
        when(referenceManager.deriveReference(any())).thenReturn(reference);
    }

    private List<SelectChoice> getTestChoices() {
        return asList(
                new SelectChoice("AAA", "AAA"),
                new SelectChoice("BBB", "BBB")
        );
    }

    private void setUpFormEntryPrompt(List<SelectChoice> items, String appearance) {
        formEntryPrompt = new MockFormEntryPromptBuilder()
                .withSelectChoices(items)
                .withAppearance(appearance)
                .build();
    }

    private void clickChoice(int index) {
        if (Appearances.isNoButtonsAppearance(formEntryPrompt)) {
            clickNoButtonChoice(index);
        } else {
            clickButtonChoice(index);
        }
    }

    private void clickNoButtonChoice(int index) {
        recyclerView.getChildAt(index).performClick();
    }

    private void clickButtonChoice(int index) {
        ((AudioVideoImageTextLabel) getChoiceView(index)).getLabelTextView().performClick();
    }

    private void assertVisibleItemsInButtonsMode(String... items) {
        assertThat(recyclerView.getAdapter().getItemCount(), is(items.length));
        for (int i = 0; i < getVisibleItems().size(); i++) {
            if (recyclerView.getAdapter() instanceof SelectOneListAdapter) {
                assertThat(getRadioButton(i).getText().toString(), is(items[i]));
            } else {
                assertThat(getCheckBox(i).getText().toString(), is(items[i]));
            }
        }
    }

    private void assertVisibleItemsInNoButtonsMode(String... items) {
        assertThat(recyclerView.getAdapter().getItemCount(), is(items.length));
        for (int i = 0; i < getVisibleItems().size(); i++) {
            assertThat(((TextView) getChoiceView(i).findViewById(R.id.label)).getText().toString(), is(items[i]));
        }
    }

    private List<SelectChoice> getVisibleItems() {
        return ((AbstractSelectListAdapter) recyclerView.getAdapter())
                .getFilteredItems();
    }

    private RadioButton getRadioButton(int index) {
        return (RadioButton) getAudioVideoImageTextLabelView(index).getLabelTextView();
    }

    private CheckBox getCheckBox(int index) {
        return (CheckBox) getAudioVideoImageTextLabelView(index).getLabelTextView();
    }

    private ViewGroup getChoiceView(int index) {
        return (ViewGroup) recyclerView.getChildAt(index);
    }

    private AudioVideoImageTextLabel getAudioVideoImageTextLabelView(int index) {
        return (AudioVideoImageTextLabel) recyclerView.getChildAt(index);
    }

    private boolean isItemSelected(int index) {
        return Appearances.isNoButtonsAppearance(formEntryPrompt)
                ? isNoButtonItemSelected(index)
                : isButtonItemSelected(index);
    }

    private boolean isNoButtonItemSelected(int index) {
        return getChoiceView(index).getBackground() != null;
    }

    private boolean isButtonItemSelected(int index) {
        return recyclerView.getAdapter() instanceof SelectOneListAdapter
                ? getRadioButton(index).isChecked()
                : getCheckBox(index).isChecked();
    }

    private void initRecyclerView(AbstractSelectListAdapter adapter, boolean isFlex) {
        recyclerView.initRecyclerView(adapter, isFlex);
        RobolectricHelpers.runLooper();
    }

    private void filterList(AbstractSelectListAdapter adapter, String text) {
        adapter.getFilter().filter(text);
        RobolectricHelpers.runLooper();
    }
}
