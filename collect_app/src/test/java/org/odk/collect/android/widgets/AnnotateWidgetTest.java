package org.odk.collect.android.widgets;

import android.content.Intent;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.provider.MediaStore;
import android.view.View;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.core.util.Pair;

import org.javarosa.core.model.data.StringData;
import org.javarosa.core.reference.ReferenceManager;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;
import org.odk.collect.android.R;
import org.odk.collect.draw.DrawActivity;
import org.odk.collect.android.formentry.questions.QuestionDetails;
import org.odk.collect.android.injection.config.AppDependencyModule;
import org.odk.collect.android.support.CollectHelpers;
import org.odk.collect.android.support.MockFormEntryPromptBuilder;
import org.odk.collect.android.widgets.base.FileWidgetTest;
import org.odk.collect.android.widgets.support.FakeQuestionMediaManager;
import org.odk.collect.android.widgets.support.FakeWaitingForDataRegistry;
import org.odk.collect.android.widgets.support.SynchronousImageLoader;
import org.odk.collect.imageloader.ImageLoader;
import org.odk.collect.shared.TempFiles;
import org.robolectric.shadows.ShadowToast;

import java.io.File;
import java.io.IOException;

import static java.util.Collections.singletonList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.when;
import static org.odk.collect.android.support.CollectHelpers.overrideReferenceManager;
import static org.odk.collect.android.support.CollectHelpers.setupFakeReferenceManager;
import static org.robolectric.Shadows.shadowOf;

/**
 *  @author James Knight
 */
public class AnnotateWidgetTest extends FileWidgetTest<AnnotateWidget> {

    private File currentFile;
    private FakeQuestionMediaManager questionMediaManager;

    private final File file = TempFiles.createTempFile("sample", ".jpg");

    @NonNull
    @Override
    public AnnotateWidget createWidget() {
        questionMediaManager = new FakeQuestionMediaManager() {
            @Override
            public File getAnswerFile(String fileName) {
                File result;
                if (currentFile == null) {
                    result = super.getAnswerFile(fileName);
                } else {
                    result = fileName.equals(DrawWidgetTest.USER_SPECIFIED_IMAGE_ANSWER) ? currentFile : null;
                }
                return result;
            }
        };
        return new AnnotateWidget(activity,
                new QuestionDetails(formEntryPrompt, readOnlyOverride),
                questionMediaManager, new FakeWaitingForDataRegistry(), TempFiles.getPathInTempDir(), dependencies);
    }

    @NonNull
    @Override
    public StringData getNextAnswer() {
        return new StringData(file.getName());
    }

    @Override
    public Object createBinaryData(@NotNull StringData answerData) {
        return file;
    }

    @Test
    public void buttonsShouldLaunchCorrectIntentsWhenThereIsNoCustomPackage() {
        stubAllRuntimePermissionsGranted(true);

        Intent intent = getIntentLaunchedByClick(R.id.capture_button);
        assertActionEquals(MediaStore.ACTION_IMAGE_CAPTURE, intent);
        assertThat(intent.getPackage(), equalTo(null));

        intent = getIntentLaunchedByClick(R.id.choose_button);
        assertActionEquals(Intent.ACTION_GET_CONTENT, intent);

        intent = getIntentLaunchedByClick(R.id.annotate_button);
        assertComponentEquals(activity, DrawActivity.class, intent);
        assertExtraEquals(DrawActivity.OPTION, DrawActivity.OPTION_ANNOTATE, intent);
    }

    @Test
    public void buttonsShouldLaunchCorrectIntentsWhenCustomPackageIsSet() {
        formEntryPrompt = new MockFormEntryPromptBuilder()
                .withAdditionalAttribute("intent", "com.customcameraapp")
                .build();

        stubAllRuntimePermissionsGranted(true);

        Intent intent = getIntentLaunchedByClick(R.id.capture_button);
        assertActionEquals(MediaStore.ACTION_IMAGE_CAPTURE, intent);
        assertThat(intent.getPackage(), equalTo("com.customcameraapp"));

        intent = getIntentLaunchedByClick(R.id.choose_button);
        assertActionEquals(Intent.ACTION_GET_CONTENT, intent);
        assertTypeEquals("image/*", intent);
    }

    @Test
    public void buttonsShouldNotLaunchIntentsWhenPermissionsDenied() {
        stubAllRuntimePermissionsGranted(false);

        assertNull(getIntentLaunchedByClick(R.id.capture_button));
    }

    @Test
    public void usingReadOnlyOptionShouldMakeAllClickableElementsDisabled() {
        when(formEntryPrompt.isReadOnly()).thenReturn(true);

        assertThat(getSpyWidget().binding.captureButton.getVisibility(), is(View.GONE));
        assertThat(getSpyWidget().binding.chooseButton.getVisibility(), is(View.GONE));
        assertThat(getSpyWidget().binding.annotateButton.getVisibility(), is(View.GONE));
    }

    @Test
    public void whenReadOnlyOverrideOptionIsUsed_shouldAllClickableElementsBeDisabled() {
        readOnlyOverride = true;
        when(formEntryPrompt.isReadOnly()).thenReturn(false);

        assertThat(getSpyWidget().binding.captureButton.getVisibility(), is(View.GONE));
        assertThat(getSpyWidget().binding.chooseButton.getVisibility(), is(View.GONE));
        assertThat(getSpyWidget().binding.annotateButton.getVisibility(), is(View.GONE));
    }

    @Test
    public void whenThereIsNoAnswer_hideImageViewAndErrorMessage() {
        AnnotateWidget widget = createWidget();

        assertThat(widget.getImageView().getVisibility(), is(View.GONE));
        assertThat(widget.getImageView().getDrawable(), nullValue());

        assertThat(widget.getErrorTextView().getVisibility(), is(View.GONE));
    }

    @Test
    public void whenGifFileSelected_doNotAttachItAndDisplayAMessage() {
        AnnotateWidget widget = createWidget();

        File file = TempFiles.createTempFile("sample", ".gif");
        questionMediaManager.addAnswerFile(file);
        widget.setData(file);

        assertThat(widget.getImageView().getVisibility(), is(View.GONE));
        assertThat(widget.getImageView().getDrawable(), nullValue());

        assertThat(ShadowToast.getTextOfLatestToast(), is("Gif files are not supported"));
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

        AnnotateWidget widget = createWidget();

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
                .withAnswerDisplayText(DrawWidgetTest.DEFAULT_IMAGE_ANSWER)
                .build();

        AnnotateWidget widget = createWidget();
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
                .withAnswerDisplayText(DrawWidgetTest.USER_SPECIFIED_IMAGE_ANSWER)
                .build();

        AnnotateWidget widget = createWidget();
        ImageView imageView = widget.getImageView();
        assertThat(imageView.getVisibility(), is(View.VISIBLE));
        Drawable drawable = imageView.getDrawable();
        assertThat(drawable, notNullValue());

        String loadedPath = shadowOf(((BitmapDrawable) drawable).getBitmap()).getCreatedFromPath();
        assertThat(loadedPath, equalTo(imagePath));
    }

    @Test
    public void markupButtonShouldBeDisabledIfImageAbsent() throws Exception {
        String badPath = "bad_path";
        overrideReferenceManager(setupFakeReferenceManager(singletonList(
                new Pair<>(DrawWidgetTest.DEFAULT_IMAGE_ANSWER, badPath)
        )));

        formEntryPrompt = new MockFormEntryPromptBuilder()
                .withAnswerDisplayText(DrawWidgetTest.DEFAULT_IMAGE_ANSWER)
                .build();

        assertThat(getWidget().binding.annotateButton.isEnabled(), is(false));

        formEntryPrompt = new MockFormEntryPromptBuilder()
                .withAnswerDisplayText(DrawWidgetTest.USER_SPECIFIED_IMAGE_ANSWER)
                .build();

        assertThat(getWidget().binding.annotateButton.isEnabled(), is(false));
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

        Intent intent = getIntentLaunchedByClick(R.id.annotate_button);
        assertComponentEquals(activity, DrawActivity.class, intent);
        assertExtraEquals(DrawActivity.OPTION, DrawActivity.OPTION_ANNOTATE, intent);
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

        Intent intent = getIntentLaunchedByClick(R.id.annotate_button);
        assertComponentEquals(activity, DrawActivity.class, intent);
        assertExtraEquals(DrawActivity.OPTION, DrawActivity.OPTION_ANNOTATE, intent);
        assertThat(intent.hasExtra(DrawActivity.REF_IMAGE), is(false));
    }

    @Test
    public void whenThereIsNoAnswer_doNotPassUriToDrawActivity() {
        Intent intent = getIntentLaunchedByClick(R.id.annotate_button);
        assertComponentEquals(activity, DrawActivity.class, intent);
        assertExtraEquals(DrawActivity.OPTION, DrawActivity.OPTION_ANNOTATE, intent);
        assertThat(intent.hasExtra(DrawActivity.REF_IMAGE), is(false));
    }
}
