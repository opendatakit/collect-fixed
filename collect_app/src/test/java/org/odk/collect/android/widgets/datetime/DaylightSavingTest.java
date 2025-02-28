/*
 * Copyright 2017 Nafundi
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.odk.collect.android.widgets.datetime;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.odk.collect.android.widgets.support.QuestionWidgetHelpers.widgetDependencies;

import android.app.DatePickerDialog;
import android.widget.DatePicker;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.javarosa.core.model.IFormElement;
import org.javarosa.core.model.QuestionDef;
import org.javarosa.form.api.FormEntryPrompt;
import org.joda.time.DateTime;
import org.joda.time.LocalDateTime;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.odk.collect.android.formentry.questions.QuestionDetails;
import org.odk.collect.android.support.CollectHelpers;
import org.odk.collect.android.support.MockFormEntryPromptBuilder;
import org.odk.collect.android.support.WidgetTestActivity;
import org.odk.collect.android.widgets.utilities.DateTimeWidgetUtils;
import org.odk.collect.testshared.TimeZoneSetter;

import java.util.TimeZone;

@RunWith(AndroidJUnit4.class)
/** https://github.com/getodk/collect/issues/356
 * Verify that the {@link DateWidget} and {@link DateTimeWidget} widget skips over
 * "daylight savings gaps".
 * This is needed on the day and time of a daylight savings transition because that date/time
 * doesn't exist.
 * In this test we set time to daylight saving gap and check if any crash occur*/
public class DaylightSavingTest {

    private static final String EAT_IME_ZONE = "Africa/Nairobi";
    private static final String CET_TIME_ZONE = "Europe/Warsaw";

    private WidgetTestActivity widgetActivity;
    private DateTimeWidgetUtils widgetUtils;
    private TimeZone currentTimeZone;

    @Before
    public void setUp() {
        widgetActivity = CollectHelpers.buildThemedActivity(WidgetTestActivity.class).get();
        widgetUtils = mock(DateTimeWidgetUtils.class);
        currentTimeZone = TimeZone.getDefault();
    }

    @After
    public void tearDown() {
        TimeZoneSetter.setTimezone(currentTimeZone);
    }

    @Test
    // 26 Mar 2017 at 02:00:00 clocks were turned forward to 03:00:00.
    public void testESTTimeZoneWithDateTimeWidget() {
        TimeZoneSetter.setTimezone(TimeZone.getTimeZone(CET_TIME_ZONE));

        DateTimeWidget dateTimeWidget = prepareDateTimeWidget(2017, 3, 26, 2, 30);

        /*
         * We would get crash in this place using old approach {@link org.joda.time.DateTime} instead of
         * {@link org.joda.time.LocalDateTime}
         */
        dateTimeWidget.getAnswer();
    }

    @Test
    // 1 Jan 1960 at 00:00:00 clocks were turned forward to 00:15:00
    public void testEATTimezoneWithDateWidget() {
        TimeZoneSetter.setTimezone(TimeZone.getTimeZone(EAT_IME_ZONE));

        DateWidget dateWidget = prepareDateWidget(1960, 0, 1);

        /*
         * We would get crash in this place using old approach {@link org.joda.time.DateTime} instead of
         * {@link org.joda.time.LocalDateTime}
         */
        dateWidget.getAnswer();
    }

    private DateWidget prepareDateWidget(int year, int month, int day) {
        QuestionDef questionDefStub = mock(QuestionDef.class);

        FormEntryPrompt formEntryPromptStub = new MockFormEntryPromptBuilder()
                .withIndex("index")
                .build();
        IFormElement iformElementStub = formEntryPromptStub.getFormElement();

        when(iformElementStub.getAdditionalAttribute(anyString(), anyString())).thenReturn(null);
        when(formEntryPromptStub.getQuestion()).thenReturn(questionDefStub);
        when(formEntryPromptStub.getFormElement()).thenReturn(iformElementStub);
        when(formEntryPromptStub.getAppearanceHint()).thenReturn("no-calendar");

        DatePickerDialog datePickerDialog = mock(DatePickerDialog.class);
        DatePicker datePicker = mock(DatePicker.class);
        when(datePickerDialog.getDatePicker()).thenReturn(datePicker);
        when(datePickerDialog.getDatePicker().getYear()).thenReturn(year);
        when(datePickerDialog.getDatePicker().getMonth()).thenReturn(month);
        when(datePickerDialog.getDatePicker().getDayOfMonth()).thenReturn(day);

        return new DateWidget(widgetActivity, new QuestionDetails(formEntryPromptStub), widgetUtils, null, widgetDependencies());
    }

    private DateTimeWidget prepareDateTimeWidget(int year, int month, int day, int hour, int minute) {
        QuestionDef questionDefStub = mock(QuestionDef.class);

        FormEntryPrompt formEntryPromptStub = new MockFormEntryPromptBuilder()
                .withIndex("index")
                .build();
        IFormElement iformElementStub = formEntryPromptStub.getFormElement();

        when(iformElementStub.getAdditionalAttribute(anyString(), anyString())).thenReturn(null);
        when(formEntryPromptStub.getQuestion()).thenReturn(questionDefStub);
        when(formEntryPromptStub.getFormElement()).thenReturn(iformElementStub);
        when(formEntryPromptStub.getAppearanceHint()).thenReturn("no-calendar");

        DateTimeWidget dateTimeWidget = new DateTimeWidget(widgetActivity, new QuestionDetails(formEntryPromptStub), widgetUtils, null, widgetDependencies());
        dateTimeWidget.setData(new LocalDateTime().withDate(year, month, day));
        dateTimeWidget.setData(new DateTime().withTime(hour, minute, 0, 0));

        return dateTimeWidget;
    }
}
