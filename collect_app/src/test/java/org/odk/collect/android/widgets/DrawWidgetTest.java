package org.odk.collect.android.widgets;

import android.content.Intent;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.view.View;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.core.util.Pair;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import net.bytebuddy.utility.RandomString;
import org.javarosa.core.model.data.StringData;
import org.javarosa.core.reference.ReferenceManager;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.odk.collect.android.R;
import org.odk.collect.draw.DrawActivity;
import org.odk.collect.android.formentry.questions.QuestionDetails;
import org.odk.collect.android.injection.config.AppDependencyModule;
import org.odk.collect.android.support.CollectHelpers;
import org.odk.collect.android.support.MockFormEntryPromptBuilder;
import org.odk.collect.android.utilities.QuestionMediaManager;
import org.odk.collect.android.widgets.base.FileWidgetTest;
import org.odk.collect.android.widgets.support.FakeQuestionMediaManager;
import org.odk.collect.android.widgets.support.FakeWaitingForDataRegistry;
import org.odk.collect.android.widgets.support.SynchronousImageLoader;
import org.odk.collect.imageloader.ImageLoader;
import org.odk.collect.shared.TempFiles;

import java.io.File;
import java.io.IOException;

import static java.util.Collections.singletonList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.when;
import static org.odk.collect.android.support.CollectHelpers.setupFakeReferenceManager;
import static org.robolectric.Shadows.shadowOf;

/**
 * @author James Knight
 */
@RunWith(AndroidJUnit4.class)
public class DrawWidgetTest extends FileWidgetTest<DrawWidget> {

    //Package visibility for sharing with related tests
    static final String DEFAULT_IMAGE_ANSWER = "jr://images/referenceURI";
    static final String USER_SPECIFIED_IMAGE_ANSWER = "current.bmp";

    private File currentFile;

    @NonNull
    @Override
    public DrawWidget createWidget() {
        QuestionMediaManager fakeQuestionMediaManager = new FakeQuestionMediaManager() {
            @Override
            public File getAnswerFile(String fileName) {
                File result;
                if (currentFile == null) {
                    result = super.getAnswerFile(fileName);
                } else {
                    result = fileName.equals(USER_SPECIFIED_IMAGE_ANSWER) ? currentFile : null;
                }
                return result;
            }
        };
        return new DrawWidget(activity,
                new QuestionDetails(formEntryPrompt, readOnlyOverride),
                fakeQuestionMediaManager, new FakeWaitingForDataRegistry(), TempFiles.getPathInTempDir(), dependencies);
    }

    @NonNull
    @Override
    public StringData getNextAnswer() {
        return new StringData(RandomString.make());
    }

    @Test
    public void usingReadOnlyOptionShouldMakeAllClickableElementsDisabled() {
        when(formEntryPrompt.isReadOnly()).thenReturn(true);

        assertThat(getSpyWidget().binding.drawButton.getVisibility(), is(View.GONE));
    }

    @Test
    public void whenReadOnlyOverrideOptionIsUsed_shouldAllClickableElementsBeDisabled() {
        readOnlyOverride = true;
        when(formEntryPrompt.isReadOnly()).thenReturn(false);

        assertThat(getSpyWidget().binding.drawButton.getVisibility(), is(View.GONE));
    }

    @Test
    public void whenThereIsNoAnswer_hideImageViewAndErrorMessage() {
        DrawWidget widget = createWidget();

        assertThat(widget.getImageView().getVisibility(), is(View.GONE));
        assertThat(widget.getImageView().getDrawable(), nullValue());

        assertThat(widget.getErrorTextView().getVisibility(), is(View.GONE));
    }

    @Test
    public void whenTheAnswerImageCanNotBeLoaded_hideImageViewAndShowErrorMessage() throws IOException {
        CollectHelpers.overrideAppDependencyModule(new AppDependencyModule() {
            @Override
            public ImageLoader providesImageLoader() {
                return new SynchronousImageLoader(true);
            }
        });

        String imagePath = File.createTempFile("current", ".bmp").getAbsolutePath();
        currentFile = new File(imagePath);

        formEntryPrompt = new MockFormEntryPromptBuilder()
                .withAnswerDisplayText(DrawWidgetTest.USER_SPECIFIED_IMAGE_ANSWER)
                .build();

        DrawWidget widget = createWidget();

        assertThat(widget.getImageView().getVisibility(), is(View.GONE));
        assertThat(widget.getImageView().getDrawable(), nullValue());

        assertThat(widget.getErrorTextView().getVisibility(), is(View.VISIBLE));
    }

    @Test
    public void whenPromptHasDefaultAnswer_showsInImageView() throws Exception {
        String imagePath = File.createTempFile("default", ".bmp").getAbsolutePath();
        ReferenceManager referenceManager = setupFakeReferenceManager(singletonList(
                new Pair<>(DrawWidgetTest.DEFAULT_IMAGE_ANSWER, imagePath)
        ));
        CollectHelpers.overrideAppDependencyModule(new AppDependencyModule() {
            @Override
            public ReferenceManager providesReferenceManager() {
                return referenceManager;
            }

            @Override
            public ImageLoader providesImageLoader() {
                return new SynchronousImageLoader();
            }
        });

        formEntryPrompt = new MockFormEntryPromptBuilder()
                .withAnswerDisplayText(DEFAULT_IMAGE_ANSWER)
                .build();

        DrawWidget widget = createWidget();
        ImageView imageView = widget.getImageView();
        assertThat(imageView.getVisibility(), is(View.VISIBLE));
        Drawable drawable = imageView.getDrawable();
        assertThat(drawable, notNullValue());

        String loadedPath = shadowOf(((BitmapDrawable) drawable).getBitmap()).getCreatedFromPath();
        assertThat(loadedPath, equalTo(imagePath));
    }

    @Test
    public void whenPromptHasCurrentAnswer_showsInImageView() throws Exception {
        CollectHelpers.overrideAppDependencyModule(new AppDependencyModule() {
            @Override
            public ImageLoader providesImageLoader() {
                return new SynchronousImageLoader();
            }
        });

        String imagePath = File.createTempFile("current", ".bmp").getAbsolutePath();
        currentFile = new File(imagePath);

        formEntryPrompt = new MockFormEntryPromptBuilder()
                .withAnswerDisplayText(USER_SPECIFIED_IMAGE_ANSWER)
                .build();

        DrawWidget widget = createWidget();
        ImageView imageView = widget.getImageView();
        assertThat(imageView.getVisibility(), is(View.VISIBLE));
        Drawable drawable = imageView.getDrawable();
        assertThat(drawable, notNullValue());

        String loadedPath = shadowOf(((BitmapDrawable) drawable).getBitmap()).getCreatedFromPath();
        assertThat(loadedPath, equalTo(imagePath));
    }

    @Test
    public void whenPromptHasDefaultAnswer_passUriToDrawActivity() throws Exception {
        File file = File.createTempFile("default", ".bmp");
        String imagePath = file.getAbsolutePath();

        ReferenceManager referenceManager = setupFakeReferenceManager(singletonList(
                new Pair<>(DrawWidgetTest.DEFAULT_IMAGE_ANSWER, imagePath)
        ));
        CollectHelpers.overrideAppDependencyModule(new AppDependencyModule() {
            @Override
            public ReferenceManager providesReferenceManager() {
                return referenceManager;
            }

            @Override
            public ImageLoader providesImageLoader() {
                return new SynchronousImageLoader();
            }
        });

        formEntryPrompt = new MockFormEntryPromptBuilder()
                .withAnswerDisplayText(DrawWidgetTest.DEFAULT_IMAGE_ANSWER)
                .build();

        Intent intent = getIntentLaunchedByClick(R.id.draw_button);
        assertComponentEquals(activity, DrawActivity.class, intent);
        assertExtraEquals(DrawActivity.OPTION, DrawActivity.OPTION_DRAW, intent);
        assertExtraEquals(DrawActivity.REF_IMAGE, Uri.fromFile(file), intent);
    }

    @Test
    public void whenPromptHasDefaultAnswerThatDoesNotExist_doNotPassUriToDrawActivity() throws Exception {
        ReferenceManager referenceManager = setupFakeReferenceManager(singletonList(
                new Pair<>(DrawWidgetTest.DEFAULT_IMAGE_ANSWER, "/something")
        ));
        CollectHelpers.overrideAppDependencyModule(new AppDependencyModule() {
            @Override
            public ReferenceManager providesReferenceManager() {
                return referenceManager;
            }
        });

        formEntryPrompt = new MockFormEntryPromptBuilder()
                .withAnswerDisplayText(DrawWidgetTest.DEFAULT_IMAGE_ANSWER)
                .build();

        Intent intent = getIntentLaunchedByClick(R.id.draw_button);
        assertComponentEquals(activity, DrawActivity.class, intent);
        assertExtraEquals(DrawActivity.OPTION, DrawActivity.OPTION_DRAW, intent);
        assertThat(intent.hasExtra(DrawActivity.REF_IMAGE), is(false));
    }

    @Test
    public void whenThereIsNoAnswer_doNotPassUriToDrawActivity() {
        Intent intent = getIntentLaunchedByClick(R.id.draw_button);
        assertComponentEquals(activity, DrawActivity.class, intent);
        assertExtraEquals(DrawActivity.OPTION, DrawActivity.OPTION_DRAW, intent);
        assertThat(intent.hasExtra(DrawActivity.REF_IMAGE), is(false));
    }
}
