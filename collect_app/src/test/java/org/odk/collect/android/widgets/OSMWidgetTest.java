package org.odk.collect.android.widgets;

import android.support.annotation.NonNull;

import com.google.common.collect.ImmutableList;

import net.bytebuddy.utility.RandomString;

import org.javarosa.core.model.FormDef;
import org.javarosa.core.model.QuestionDef;
import org.javarosa.core.model.data.StringData;
import org.javarosa.core.model.osm.OSMTag;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.odk.collect.android.BuildConfig;
import org.odk.collect.android.logic.FormController;
import org.odk.collect.android.widgets.base.BinaryWidgetTest;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.io.File;

import static org.mockito.Mockito.when;

/**
 * @author James Knight
 */
@Config(constants = BuildConfig.class)
@RunWith(RobolectricTestRunner.class)
public class OSMWidgetTest extends BinaryWidgetTest<OSMWidget> {

    private String fileName = null;

    @Mock
    File mediaFolder;

    @Mock
    FormDef formDef;

    @Mock
    QuestionDef questionDef;

    public OSMWidgetTest() {
        super(OSMWidget.class);
    }

    @NonNull
    @Override
    public OSMWidget createWidget() {
        return new OSMWidget(RuntimeEnvironment.application, formEntryPrompt);
    }

    @NonNull
    @Override
    public StringData getNextAnswer() {
        return new StringData(fileName);
    }

    @Override
    public Object createBinaryData(StringData answerData) {
        return fileName;
    }

    @Before
    public void setUp() throws Exception {
        super.setUp();
        when(formEntryPrompt.isReadOnly()).thenReturn(false);

        when(formController.getMediaFolder()).thenReturn(mediaFolder);
        when(formController.getSubmissionMetadata()).thenReturn(
                new FormController.InstanceMetadata("", "", false)
        );

        when(formController.getFormDef()).thenReturn(formDef);
        when(formDef.getID()).thenReturn(0);

        when(mediaFolder.getName()).thenReturn("test-media");

        when(formEntryPrompt.getQuestion()).thenReturn(questionDef);
        when(questionDef.getOsmTags()).thenReturn(ImmutableList.<OSMTag>of());

        fileName = RandomString.make();
    }
}