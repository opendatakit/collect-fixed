package org.odk.collect.android.widgets;

import android.content.Intent;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.provider.MediaStore;
import android.view.View;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.core.util.Pair;

import net.bytebuddy.utility.RandomString;

import org.javarosa.core.model.data.StringData;
import org.javarosa.core.reference.ReferenceManager;
import org.junit.Test;
import org.odk.collect.android.R;
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
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.when;
import static org.odk.collect.android.support.CollectHelpers.setupFakeReferenceManager;
import static org.robolectric.Shadows.shadowOf;

/**
 * @author James Knight
 */
public class ImageWidgetTest extends FileWidgetTest<ImageWidget> {

    private File currentFile;

    @NonNull
    @Override
    public ImageWidget createWidget() {
        QuestionMediaManager fakeQuestionMediaManager = new FakeQuestionMediaManager() {
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
        return new ImageWidget(activity, new QuestionDetails(formEntryPrompt, readOnlyOverride),
                fakeQuestionMediaManager, new FakeWaitingForDataRegistry(), TempFiles.getPathInTempDir(), dependencies);
    }

    @NonNull
    @Override
    public StringData getNextAnswer() {
        return new StringData(RandomString.make());
    }

    @Test
    public void buttonsShouldLaunchCorrectIntentsWhenThereIsNoCustomPackage() {
        stubAllRuntimePermissionsGranted(true);

        Intent intent = getIntentLaunchedByClick(R.id.capture_button);
        assertActionEquals(MediaStore.ACTION_IMAGE_CAPTURE, intent);
        assertThat(intent.getPackage(), equalTo(null));

        intent = getIntentLaunchedByClick(R.id.choose_button);
        assertActionEquals(Intent.ACTION_GET_CONTENT, intent);
        assertTypeEquals("image/*", intent);
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
    }

    @Test
    public void whenReadOnlyOverrideOptionIsUsed_shouldAllClickableElementsBeDisabled() {
        readOnlyOverride = true;
        when(formEntryPrompt.isReadOnly()).thenReturn(false);

        assertThat(getSpyWidget().binding.captureButton.getVisibility(), is(View.GONE));
        assertThat(getSpyWidget().binding.chooseButton.getVisibility(), is(View.GONE));
    }

    @Test
    public void whenThereIsNoAnswer_hideImageViewAndErrorMessage() {
        ImageWidget widget = createWidget();

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

        ImageWidget widget = createWidget();

        assertThat(widget.getImageView().getVisibility(), is(View.GONE));
        assertThat(widget.getImageView().getDrawable(), nullValue());

        assertThat(widget.getErrorTextView().getVisibility(), is(View.VISIBLE));
    }

    @Test
    public void whenPromptHasDefaultAnswer_doesNotShow() throws Exception {
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

        ImageWidget widget = createWidget();
        ImageView imageView = widget.getImageView();
        assertThat(imageView.getVisibility(), is(View.GONE));
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

        ImageWidget widget = createWidget();
        ImageView imageView = widget.getImageView();
        assertThat(imageView.getVisibility(), is(View.VISIBLE));
        Drawable drawable = imageView.getDrawable();
        assertThat(drawable, notNullValue());

        String loadedPath = shadowOf(((BitmapDrawable) drawable).getBitmap()).getCreatedFromPath();
        assertThat(loadedPath, equalTo(imagePath));
    }
}
