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

import android.annotation.SuppressLint;
import android.content.Context;

import org.javarosa.core.model.data.IAnswerData;
import org.odk.collect.android.formentry.questions.QuestionDetails;
import org.odk.collect.android.utilities.Appearances;
import org.odk.collect.android.widgets.utilities.StringWidgetUtils;

/**
 * A widget that restricts values to floating point numbers.
 */
@SuppressLint("ViewConstructor")
public class DecimalWidget extends StringWidget {

    public DecimalWidget(Context context, QuestionDetails questionDetails, Dependencies dependencies) {
        super(context, questionDetails, dependencies);

        boolean useThousandSeparator = Appearances.useThousandSeparator(questionDetails.getPrompt());
        Double answer = StringWidgetUtils.getDoubleAnswerValueFromIAnswerData(questionDetails.getPrompt().getAnswerValue());
        widgetAnswerText.setDecimalType(useThousandSeparator, answer);
    }

    @Override
    public IAnswerData getAnswer() {
        return StringWidgetUtils.getDecimalData(getAnswerText(), getFormEntryPrompt());
    }
}
