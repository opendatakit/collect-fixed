/*
 * Copyright (C) 2009 University of Washington
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package org.odk.collect.android.widgets;

import static org.odk.collect.android.utilities.Appearances.MAPS;
import static org.odk.collect.android.utilities.Appearances.PLACEMENT_MAP;
import static org.odk.collect.android.utilities.Appearances.hasAppearance;

import android.app.Activity;
import android.content.Context;
import android.hardware.SensorManager;

import androidx.lifecycle.LifecycleOwner;

import org.javarosa.core.model.Constants;
import org.javarosa.form.api.FormEntryPrompt;
import org.odk.collect.android.formentry.FormEntryViewModel;
import org.odk.collect.android.formentry.PrinterWidgetViewModel;
import org.odk.collect.android.formentry.questions.QuestionDetails;
import org.odk.collect.android.geo.MapConfiguratorProvider;
import org.odk.collect.android.javarosawrapper.FormController;
import org.odk.collect.android.listeners.AdvanceToNextListener;
import org.odk.collect.android.storage.StoragePathProvider;
import org.odk.collect.android.utilities.Appearances;
import org.odk.collect.android.utilities.QuestionMediaManager;
import org.odk.collect.android.widgets.datetime.DateTimeWidget;
import org.odk.collect.android.widgets.datetime.DateWidget;
import org.odk.collect.android.widgets.datetime.TimeWidget;
import org.odk.collect.android.widgets.items.LabelWidget;
import org.odk.collect.android.widgets.items.LikertWidget;
import org.odk.collect.android.widgets.items.ListMultiWidget;
import org.odk.collect.android.widgets.items.ListWidget;
import org.odk.collect.android.widgets.items.RankingWidget;
import org.odk.collect.android.widgets.items.SelectMultiImageMapWidget;
import org.odk.collect.android.widgets.items.SelectMultiMinimalWidget;
import org.odk.collect.android.widgets.items.SelectMultiWidget;
import org.odk.collect.android.widgets.items.SelectOneFromMapWidget;
import org.odk.collect.android.widgets.items.SelectOneImageMapWidget;
import org.odk.collect.android.widgets.items.SelectOneMinimalWidget;
import org.odk.collect.android.widgets.items.SelectOneWidget;
import org.odk.collect.android.widgets.range.RangeDecimalWidget;
import org.odk.collect.android.widgets.range.RangeIntegerWidget;
import org.odk.collect.android.widgets.range.RangePickerDecimalWidget;
import org.odk.collect.android.widgets.range.RangePickerIntegerWidget;
import org.odk.collect.android.widgets.utilities.ActivityGeoDataRequester;
import org.odk.collect.android.widgets.utilities.AudioPlayer;
import org.odk.collect.android.widgets.utilities.AudioRecorderRecordingStatusHandler;
import org.odk.collect.android.widgets.utilities.DateTimeWidgetUtils;
import org.odk.collect.android.widgets.utilities.FileRequester;
import org.odk.collect.android.widgets.utilities.GetContentAudioFileRequester;
import org.odk.collect.android.widgets.utilities.RecordingRequester;
import org.odk.collect.android.widgets.utilities.RecordingRequesterProvider;
import org.odk.collect.android.widgets.utilities.StringRequester;
import org.odk.collect.android.widgets.utilities.WaitingForDataRegistry;
import org.odk.collect.androidshared.system.CameraUtils;
import org.odk.collect.androidshared.system.IntentLauncherImpl;
import org.odk.collect.audiorecorder.recording.AudioRecorder;
import org.odk.collect.permissions.PermissionsProvider;
import org.odk.collect.webpage.ExternalWebPageHelper;

/**
 * Convenience class that handles creation of widgets.
 *
 * @author Carl Hartung (carlhartung@gmail.com)
 */
public class WidgetFactory {

    private static final String PICKER_APPEARANCE = "picker";

    private final Activity activity;
    private final boolean readOnlyOverride;
    private final boolean useExternalRecorder;
    private final WaitingForDataRegistry waitingForDataRegistry;
    private final QuestionMediaManager questionMediaManager;
    private final AudioPlayer audioPlayer;
    private final RecordingRequesterProvider recordingRequesterProvider;
    private final FormEntryViewModel formEntryViewModel;
    private final PrinterWidgetViewModel printerWidgetViewModel;
    private final AudioRecorder audioRecorder;
    private final LifecycleOwner viewLifecycle;
    private final FileRequester fileRequester;
    private final StringRequester stringRequester;
    private final FormController formController;
    private final AdvanceToNextListener advanceToNextListener;

    public WidgetFactory(Activity activity,
                         boolean readOnlyOverride,
                         boolean useExternalRecorder,
                         WaitingForDataRegistry waitingForDataRegistry,
                         QuestionMediaManager questionMediaManager,
                         AudioPlayer audioPlayer,
                         RecordingRequesterProvider recordingRequesterProvider,
                         FormEntryViewModel formEntryViewModel,
                         PrinterWidgetViewModel printerWidgetViewModel,
                         AudioRecorder audioRecorder,
                         LifecycleOwner viewLifecycle,
                         FileRequester fileRequester,
                         StringRequester stringRequester,
                         FormController formController,
                         AdvanceToNextListener advanceToNextListener
    ) {
        this.activity = activity;
        this.readOnlyOverride = readOnlyOverride;
        this.useExternalRecorder = useExternalRecorder;
        this.waitingForDataRegistry = waitingForDataRegistry;
        this.questionMediaManager = questionMediaManager;
        this.audioPlayer = audioPlayer;
        this.recordingRequesterProvider = recordingRequesterProvider;
        this.formEntryViewModel = formEntryViewModel;
        this.printerWidgetViewModel = printerWidgetViewModel;
        this.audioRecorder = audioRecorder;
        this.viewLifecycle = viewLifecycle;
        this.fileRequester = fileRequester;
        this.stringRequester = stringRequester;
        this.formController = formController;
        this.advanceToNextListener = advanceToNextListener;
    }

    public QuestionWidget createWidgetFromPrompt(FormEntryPrompt prompt, PermissionsProvider permissionsProvider) {
        String appearance = Appearances.getSanitizedAppearanceHint(prompt);
        QuestionDetails questionDetails = new QuestionDetails(prompt, readOnlyOverride);

        final QuestionWidget questionWidget;
        switch (prompt.getControlType()) {
            case Constants.CONTROL_INPUT:
                switch (prompt.getDataType()) {
                    case Constants.DATATYPE_DATE_TIME:
                        questionWidget = new DateTimeWidget(activity, questionDetails, new DateTimeWidgetUtils(), waitingForDataRegistry);
                        break;
                    case Constants.DATATYPE_DATE:
                        questionWidget = new DateWidget(activity, questionDetails, new DateTimeWidgetUtils(), waitingForDataRegistry);
                        break;
                    case Constants.DATATYPE_TIME:
                        questionWidget = new TimeWidget(activity, questionDetails, new DateTimeWidgetUtils(), waitingForDataRegistry);
                        break;
                    case Constants.DATATYPE_DECIMAL:
                        if (appearance.contains(Appearances.EX)) {
                            questionWidget = new ExDecimalWidget(activity, questionDetails, waitingForDataRegistry, stringRequester);
                        } else if (appearance.equals(Appearances.BEARING)) {
                            questionWidget = new BearingWidget(activity, questionDetails, waitingForDataRegistry,
                                    (SensorManager) activity.getSystemService(Context.SENSOR_SERVICE));
                        } else {
                            questionWidget = new DecimalWidget(activity, questionDetails);
                        }
                        break;
                    case Constants.DATATYPE_INTEGER:
                        if (appearance.equals(Appearances.COUNTER)) {
                            questionWidget = new CounterWidget(activity, questionDetails);
                        } else if (appearance.contains(Appearances.EX)) {
                            questionWidget = new ExIntegerWidget(activity, questionDetails, waitingForDataRegistry, stringRequester);
                        } else {
                            questionWidget = new IntegerWidget(activity, questionDetails);
                        }
                        break;
                    case Constants.DATATYPE_GEOPOINT:
                        if (hasAppearance(questionDetails.getPrompt(), PLACEMENT_MAP) || hasAppearance(questionDetails.getPrompt(), MAPS)) {
                            questionWidget = new GeoPointMapWidget(activity, questionDetails, waitingForDataRegistry,
                                    new ActivityGeoDataRequester(permissionsProvider, activity));
                        } else {
                            questionWidget = new GeoPointWidget(activity, questionDetails, waitingForDataRegistry,
                                    new ActivityGeoDataRequester(permissionsProvider, activity));
                        }
                        break;
                    case Constants.DATATYPE_GEOSHAPE:
                        questionWidget = new GeoShapeWidget(activity, questionDetails, waitingForDataRegistry,
                                new ActivityGeoDataRequester(permissionsProvider, activity));
                        break;
                    case Constants.DATATYPE_GEOTRACE:
                        questionWidget = new GeoTraceWidget(activity, questionDetails, waitingForDataRegistry,
                                MapConfiguratorProvider.getConfigurator(), new ActivityGeoDataRequester(permissionsProvider, activity));
                        break;
                    case Constants.DATATYPE_BARCODE:
                        questionWidget = new BarcodeWidget(activity, questionDetails, new BarcodeWidgetAnswer(activity), waitingForDataRegistry, new CameraUtils());
                        break;
                    case Constants.DATATYPE_TEXT:
                        String query = prompt.getQuestion().getAdditionalAttribute(null, "query");
                        if (query != null) {
                            questionWidget = getSelectOneWidget(appearance, questionDetails);
                        } else if (appearance.equals(Appearances.PRINTER)) {
                            questionWidget = new PrinterWidget(activity, questionDetails, printerWidgetViewModel, questionMediaManager);
                        } else if (appearance.contains(Appearances.EX)) {
                            questionWidget = new ExStringWidget(activity, questionDetails, waitingForDataRegistry, stringRequester);
                        } else if (appearance.contains(Appearances.NUMBERS)) {
                            questionWidget = new StringNumberWidget(activity, questionDetails);
                        } else if (appearance.equals(Appearances.URL)) {
                            questionWidget = new UrlWidget(activity, questionDetails, new ExternalWebPageHelper());
                        } else {
                            questionWidget = new StringWidget(activity, questionDetails);
                        }
                        break;
                    default:
                        questionWidget = new StringWidget(activity, questionDetails);
                        break;
                }
                break;
            case Constants.CONTROL_FILE_CAPTURE:
                if (appearance.startsWith(Appearances.EX)) {
                    questionWidget = new ExArbitraryFileWidget(activity, questionDetails, questionMediaManager, waitingForDataRegistry, fileRequester);
                } else {
                    questionWidget = new ArbitraryFileWidget(activity, questionDetails, questionMediaManager, waitingForDataRegistry);
                }
                break;
            case Constants.CONTROL_IMAGE_CHOOSE:
                if (appearance.equals(Appearances.SIGNATURE)) {
                    questionWidget = new SignatureWidget(activity, questionDetails, questionMediaManager, waitingForDataRegistry, new StoragePathProvider().getTmpImageFilePath());
                } else if (appearance.contains(Appearances.ANNOTATE)) {
                    questionWidget = new AnnotateWidget(activity, questionDetails, questionMediaManager, waitingForDataRegistry, new StoragePathProvider().getTmpImageFilePath());
                } else if (appearance.equals(Appearances.DRAW)) {
                    questionWidget = new DrawWidget(activity, questionDetails, questionMediaManager, waitingForDataRegistry, new StoragePathProvider().getTmpImageFilePath());
                } else if (appearance.startsWith(Appearances.EX)) {
                    questionWidget = new ExImageWidget(activity, questionDetails, questionMediaManager, waitingForDataRegistry, fileRequester);
                } else {
                    questionWidget = new ImageWidget(activity, questionDetails, questionMediaManager, waitingForDataRegistry, new StoragePathProvider().getTmpImageFilePath());
                }
                break;
            case Constants.CONTROL_OSM_CAPTURE:
                questionWidget = new OSMWidget(activity, questionDetails, waitingForDataRegistry,
                        IntentLauncherImpl.INSTANCE, formController);
                break;
            case Constants.CONTROL_AUDIO_CAPTURE:
                RecordingRequester recordingRequester = recordingRequesterProvider.create(prompt, useExternalRecorder);
                GetContentAudioFileRequester audioFileRequester = new GetContentAudioFileRequester(activity, IntentLauncherImpl.INSTANCE, waitingForDataRegistry);

                if (appearance.startsWith(Appearances.EX)) {
                    questionWidget = new ExAudioWidget(activity, questionDetails, questionMediaManager, audioPlayer, waitingForDataRegistry, fileRequester);
                } else {
                    questionWidget = new AudioWidget(activity, questionDetails, questionMediaManager, audioPlayer, recordingRequester, audioFileRequester, new AudioRecorderRecordingStatusHandler(audioRecorder, formEntryViewModel, viewLifecycle));
                }
                break;
            case Constants.CONTROL_VIDEO_CAPTURE:
                if (appearance.startsWith(Appearances.EX)) {
                    questionWidget = new ExVideoWidget(activity, questionDetails, questionMediaManager, waitingForDataRegistry, fileRequester);
                } else {
                    questionWidget = new VideoWidget(activity, questionDetails, questionMediaManager, waitingForDataRegistry);
                }
                break;
            case Constants.CONTROL_SELECT_ONE:
                questionWidget = getSelectOneWidget(appearance, questionDetails);
                break;
            case Constants.CONTROL_SELECT_MULTI:
                // search() appearance/function (not part of XForms spec) added by SurveyCTO gets
                // considered in each widget by calls to ExternalDataUtil.getSearchXPathExpression.
                if (appearance.contains(Appearances.MINIMAL)) {
                    questionWidget = new SelectMultiMinimalWidget(activity, questionDetails, waitingForDataRegistry, formEntryViewModel);
                } else if (appearance.contains(Appearances.LIST_NO_LABEL)) {
                    questionWidget = new ListMultiWidget(activity, questionDetails, false, formEntryViewModel);
                } else if (appearance.contains(Appearances.LIST)) {
                    questionWidget = new ListMultiWidget(activity, questionDetails, true, formEntryViewModel);
                } else if (appearance.contains(Appearances.LABEL)) {
                    questionWidget = new LabelWidget(activity, questionDetails, formEntryViewModel);
                } else if (appearance.contains(Appearances.IMAGE_MAP)) {
                    questionWidget = new SelectMultiImageMapWidget(activity, questionDetails, formEntryViewModel);
                } else {
                    questionWidget = new SelectMultiWidget(activity, questionDetails, formEntryViewModel);
                }
                break;
            case Constants.CONTROL_RANK:
                questionWidget = new RankingWidget(activity, questionDetails, waitingForDataRegistry, formEntryViewModel);
                break;
            case Constants.CONTROL_TRIGGER:
                questionWidget = new TriggerWidget(activity, questionDetails);
                break;
            case Constants.CONTROL_RANGE:
                if (appearance.startsWith(Appearances.RATING)) {
                    questionWidget = new RatingWidget(activity, questionDetails);
                } else {
                    switch (prompt.getDataType()) {
                        case Constants.DATATYPE_INTEGER:
                            if (prompt.getAppearanceHint() != null && prompt.getAppearanceHint().contains(PICKER_APPEARANCE)) {
                                questionWidget = new RangePickerIntegerWidget(activity, questionDetails);
                            } else {
                                questionWidget = new RangeIntegerWidget(activity, questionDetails);
                            }
                            break;
                        case Constants.DATATYPE_DECIMAL:
                            if (prompt.getAppearanceHint() != null && prompt.getAppearanceHint().contains(PICKER_APPEARANCE)) {
                                questionWidget = new RangePickerDecimalWidget(activity, questionDetails);
                            } else {
                                questionWidget = new RangeDecimalWidget(activity, questionDetails);
                            }
                            break;
                        default:
                            questionWidget = new StringWidget(activity, questionDetails);
                            break;
                    }
                }
                break;
            default:
                questionWidget = new StringWidget(activity, questionDetails);
                break;
        }

        return questionWidget;
    }

    private QuestionWidget getSelectOneWidget(String appearance, QuestionDetails questionDetails) {
        final QuestionWidget questionWidget;
        boolean isQuick = appearance.contains(Appearances.QUICK);
        // search() appearance/function (not part of XForms spec) added by SurveyCTO gets
        // considered in each widget by calls to ExternalDataUtil.getSearchXPathExpression.
        if (appearance.contains(Appearances.MINIMAL)) {
            questionWidget = new SelectOneMinimalWidget(activity, questionDetails, isQuick, waitingForDataRegistry, formEntryViewModel);
        } else if (appearance.contains(Appearances.LIKERT)) {
            questionWidget = new LikertWidget(activity, questionDetails, formEntryViewModel);
        } else if (appearance.contains(Appearances.LIST_NO_LABEL)) {
            questionWidget = new ListWidget(activity, questionDetails, false, isQuick, formEntryViewModel);
        } else if (appearance.contains(Appearances.LIST)) {
            questionWidget = new ListWidget(activity, questionDetails, true, isQuick, formEntryViewModel);
        } else if (appearance.contains(Appearances.LABEL)) {
            questionWidget = new LabelWidget(activity, questionDetails, formEntryViewModel);
        } else if (appearance.contains(Appearances.IMAGE_MAP)) {
            questionWidget = new SelectOneImageMapWidget(activity, questionDetails, isQuick, formEntryViewModel);
        } else if (appearance.contains(Appearances.MAP)) {
            questionWidget = new SelectOneFromMapWidget(activity, questionDetails, isQuick, advanceToNextListener);
        } else {
            questionWidget = new SelectOneWidget(activity, questionDetails, isQuick, formController, formEntryViewModel);
        }
        return questionWidget;
    }

}
