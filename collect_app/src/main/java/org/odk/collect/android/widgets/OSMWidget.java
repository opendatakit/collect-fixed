package org.odk.collect.android.widgets;

import static org.odk.collect.android.utilities.ApplicationConstants.RequestCodes;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.view.View;

import androidx.appcompat.app.AlertDialog;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import org.javarosa.core.model.data.IAnswerData;
import org.javarosa.core.model.data.StringData;
import org.javarosa.core.model.osm.OSMTag;
import org.javarosa.core.model.osm.OSMTagItem;
import org.javarosa.form.api.FormEntryPrompt;
import org.odk.collect.android.databinding.OsmWidgetAnswerBinding;
import org.odk.collect.android.formentry.questions.QuestionDetails;
import org.odk.collect.android.javarosawrapper.FormController;
import org.odk.collect.android.utilities.FileUtils;
import org.odk.collect.android.widgets.interfaces.WidgetDataReceiver;
import org.odk.collect.android.widgets.utilities.WaitingForDataRegistry;
import org.odk.collect.androidshared.system.IntentLauncher;

import java.util.ArrayList;
import java.util.List;

/**
 * Widget that allows the user to launch OpenMapKit to get an OSM Feature with a
 * predetermined set of tags that are edited in the application.
 *
 * @author Nicholas Hallahan nhallahan@spatialdev.com
 */
@SuppressLint("ViewConstructor")
public class OSMWidget extends QuestionWidget implements WidgetDataReceiver {
    OsmWidgetAnswerBinding binding;

    public static final String FORM_ID = "FORM_ID";
    public static final String INSTANCE_ID = "INSTANCE_ID";
    public static final String INSTANCE_DIR = "INSTANCE_DIR";
    public static final String FORM_FILE_NAME = "FORM_FILE_NAME";
    public static final String OSM_EDIT_FILE_NAME = "OSM_EDIT_FILE_NAME";

    private final WaitingForDataRegistry waitingForDataRegistry;
    private final IntentLauncher intentLauncher;

    private final List<OSMTag> osmRequiredTags;
    private final String instanceId;
    private final String instanceDirectory;
    private final String formFileName;
    private final int formId;

    public OSMWidget(Context context, QuestionDetails questionDetails, WaitingForDataRegistry waitingForDataRegistry,
                     IntentLauncher intentLauncher, FormController formController, Dependencies dependencies) {
        super(context, dependencies, questionDetails);
        render();

        this.waitingForDataRegistry = waitingForDataRegistry;
        this.intentLauncher = intentLauncher;

        formFileName = FileUtils.getFormBasenameFromMediaFolder(formController.getMediaFolder());

        instanceDirectory = formController.getInstanceFile().getParent();
        instanceId = formController.getSubmissionMetadata().instanceId;
        formId = formController.getFormDef().getID();

        // Determine the tags required
        osmRequiredTags = questionDetails.getPrompt().getQuestion().getOsmTags();

        // If an OSM File has already been saved, get the name.
        String osmFileName = questionDetails.getPrompt().getAnswerText();

        if (osmFileName != null) {
            binding.launchOpenMapKitButton.setText(getContext().getString(org.odk.collect.strings.R.string.recapture_osm));
            binding.osmFileText.setText(osmFileName);
        } else {
            binding.osmFileHeaderText.setVisibility(View.GONE);
        }
        binding.osmFileText.setVisibility(binding.osmFileText.getText().toString().isBlank() ? GONE : VISIBLE);
    }

    @Override
    protected View onCreateAnswerView(Context context, FormEntryPrompt prompt, int answerFontSize) {
        binding = OsmWidgetAnswerBinding.inflate(((Activity) context).getLayoutInflater());

        if (prompt.isReadOnly()) {
            binding.launchOpenMapKitButton.setVisibility(GONE);
        } else {
            binding.launchOpenMapKitButton.setOnClickListener(v -> onButtonClick());
        }

        return binding.getRoot();
    }

    private void launchOpenMapKit() {
        try {
            //launch with intent that sends plain text
            Intent launchIntent = new Intent(Intent.ACTION_SEND);
            launchIntent.setType("text/plain");

            //send form id
            launchIntent.putExtra(FORM_ID, String.valueOf(formId));

            //send instance id
            launchIntent.putExtra(INSTANCE_ID, instanceId);

            //send instance directory
            launchIntent.putExtra(INSTANCE_DIR, instanceDirectory);

            //send form file name
            launchIntent.putExtra(FORM_FILE_NAME, formFileName);

            //send OSM file name if there was a previous edit
            String osmFileName = binding.osmFileText.getText().toString();
            if (!osmFileName.isEmpty()) {
                launchIntent.putExtra(OSM_EDIT_FILE_NAME, osmFileName);
            }

            //send encode tag data structure to intent
            writeOsmRequiredTagsToExtras(launchIntent);

            waitingForDataRegistry.waitForData(getFormEntryPrompt().getIndex());
            intentLauncher.launchForResult((Activity) getContext(), launchIntent, RequestCodes.OSM_CAPTURE, () -> {
                waitingForDataRegistry.cancelWaitingForData();
                binding.errorText.setVisibility(View.VISIBLE);
                return null;
            });
        } catch (Exception ex) {
            MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(getContext());
            builder.setTitle(org.odk.collect.strings.R.string.alert);
            builder.setMessage(org.odk.collect.strings.R.string.install_openmapkit);
            DialogInterface.OnClickListener okClickListener = (dialog, id) -> {
                //TODO: launch to app store?
            };
            builder.setPositiveButton(getContext().getString(org.odk.collect.strings.R.string.ok), okClickListener);
            AlertDialog dialog = builder.create();
            dialog.show();
        }
    }

    @Override
    public void setData(Object answer) {
        // show file name of saved osm data
        binding.osmFileText.setText((String) answer);
        binding.osmFileText.setVisibility(binding.osmFileText.getText().toString().isBlank() ? GONE : VISIBLE);
        binding.osmFileHeaderText.setVisibility(View.VISIBLE);
        binding.launchOpenMapKitButton.setText(getContext().getString(org.odk.collect.strings.R.string.recapture_osm));
        widgetValueChanged();
    }

    @Override
    public IAnswerData getAnswer() {
        String osmFileName = binding.osmFileText.getText().toString();
        return osmFileName.isEmpty() ? null : new StringData(osmFileName);
    }

    @Override
    public void clearAnswer() {
        binding.osmFileText.setText(null);
        binding.osmFileText.setVisibility(GONE);
        binding.osmFileHeaderText.setVisibility(View.GONE);
        binding.launchOpenMapKitButton.setText(getContext().getString(org.odk.collect.strings.R.string.capture_osm));
        widgetValueChanged();
    }

    @Override
    public void setOnLongClickListener(OnLongClickListener l) {
        binding.osmFileText.setOnLongClickListener(l);
        binding.launchOpenMapKitButton.setOnLongClickListener(l);
    }

    private void onButtonClick() {
        binding.errorText.setVisibility(View.GONE);
        launchOpenMapKit();
    }

    /**
     * See: https://github.com/AmericanRedCross/openmapkit/wiki/ODK-Collect-Tag-Intent-Extras
     */
    private void writeOsmRequiredTagsToExtras(Intent intent) {
        ArrayList<String> tagKeys = new ArrayList<>();
        for (OSMTag tag : osmRequiredTags) {
            tagKeys.add(tag.key);
            if (tag.label != null) {
                intent.putExtra("TAG_LABEL." + tag.key, tag.label);
            }
            ArrayList<String> tagValues = new ArrayList<>();
            if (tag.items != null) {
                for (OSMTagItem item : tag.items) {
                    tagValues.add(item.value);
                    if (item.label != null) {
                        intent.putExtra("TAG_VALUE_LABEL." + tag.key + "." + item.value,
                                item.label);
                    }
                }
            }
            intent.putStringArrayListExtra("TAG_VALUES." + tag.key, tagValues);
        }
        intent.putStringArrayListExtra("TAG_KEYS", tagKeys);
    }
}
