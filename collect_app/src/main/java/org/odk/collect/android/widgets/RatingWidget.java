/*
 * Copyright (C) 2018 Akshay Patel
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
import android.app.Activity;
import android.content.Context;
import android.os.Build;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AbsSeekBar;

import org.javarosa.core.model.RangeQuestion;
import org.javarosa.core.model.data.IAnswerData;
import org.javarosa.core.model.data.IntegerData;
import org.javarosa.form.api.FormEntryPrompt;
import org.odk.collect.android.databinding.RatingWidgetAnswerBinding;
import org.odk.collect.android.formentry.questions.QuestionDetails;
import org.odk.collect.android.utilities.ViewUtils;
import org.odk.collect.androidshared.utils.ScreenUtils;

import java.lang.reflect.Field;

import timber.log.Timber;

@SuppressLint("ViewConstructor")
public class RatingWidget extends QuestionWidget {
    private static final int ASSUMED_TOTAL_MARGIN_AROUND_WIDGET = 40;
    private static final int STANDARD_WIDTH_OF_STAR = 48;

    RatingWidgetAnswerBinding binding;

    public RatingWidget(Context context, QuestionDetails questionDetails, Dependencies dependencies) {
        super(context, dependencies, questionDetails);
        render();
    }

    @Override
    protected View onCreateAnswerView(Context context, FormEntryPrompt prompt, int answerFontSize) {
        binding = RatingWidgetAnswerBinding.inflate(((Activity) context).getLayoutInflater());
        View answerView = binding.getRoot();

        int numberOfStars = getTotalStars((RangeQuestion) prompt.getQuestion());
        int maxNumberOfStars = calculateMaximumStarsInOneLine();

        if (maxNumberOfStars < numberOfStars) {
            binding.ratingBar1.setNumStars(maxNumberOfStars);
            binding.ratingBar1.setMax(maxNumberOfStars);
            binding.ratingBar2.setNumStars(Math.min(numberOfStars - maxNumberOfStars, maxNumberOfStars));
            binding.ratingBar2.setMax(Math.min(numberOfStars - maxNumberOfStars, maxNumberOfStars));

            binding.ratingBar2.setVisibility(View.VISIBLE);
        } else {
            binding.ratingBar1.setNumStars(numberOfStars);
            binding.ratingBar1.setMax(numberOfStars);
        }

        if (prompt.isReadOnly()) {
            binding.ratingBar1.setEnabled(false);
            binding.ratingBar2.setEnabled(false);
        } else {
            setUpRatingBar(maxNumberOfStars);
        }

        if (prompt.getAnswerText() != null) {
            int rating = Integer.parseInt(prompt.getAnswerText());
            if (rating > maxNumberOfStars) {
                binding.ratingBar2.setRating(rating - maxNumberOfStars);
            } else {
                binding.ratingBar1.setRating(rating);
            }
        }
        return answerView;
    }

    @Override
    public void setOnLongClickListener(OnLongClickListener listener) {
        binding.ratingBar1.setOnLongClickListener(listener);
        binding.ratingBar2.setOnLongClickListener(listener);
    }

    @Override
    public IAnswerData getAnswer() {
        return binding.ratingBar1.getRating() == 0.0F
                ? null
                : new IntegerData((int) (binding.ratingBar1.getRating() + binding.ratingBar2.getRating()));
    }

    @Override
    public void clearAnswer() {
        binding.ratingBar1.setRating(0.0F);
    }

    @SuppressLint("ClickableViewAccessibility")
    private void setUpRatingBar(int maxNumberOfStars) {
        // to quickly change rating on other rating bar in case onRatingChange listener is not called
        binding.ratingBar1.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                binding.ratingBar2.setRating(0);
            }
            return false;
        });

        binding.ratingBar2.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                binding.ratingBar1.setRating(maxNumberOfStars);
            }
            return false;
        });

        binding.ratingBar1.setOnRatingBarChangeListener((ratingBar, rating, fromUser) -> {
            binding.ratingBar2.setRating(0);
            binding.ratingBar1.setRating(rating);
            widgetValueChanged();
        });

        binding.ratingBar2.setOnRatingBarChangeListener((ratingBar, rating, fromUser) -> {
            binding.ratingBar1.setRating(maxNumberOfStars);
            binding.ratingBar2.setRating(rating);
            widgetValueChanged();
        });

        // fix for rating bar showing incorrect rating on Android Nougat(7.0/API 24)
        // See https://stackoverflow.com/questions/44342481
        if (Build.VERSION.SDK_INT == Build.VERSION_CODES.N) {
            try {
                Field field = AbsSeekBar.class.getDeclaredField("mTouchProgressOffset");
                field.setAccessible(true);
                field.set(binding.ratingBar1, 0.6f);
                field.set(binding.ratingBar2, 0.6f);
            } catch (NoSuchFieldException | IllegalAccessException e) {
                Timber.e(e);
            }
        }
    }

    private int calculateMaximumStarsInOneLine() {
        return (int) ((ScreenUtils.getScreenWidth(getContext()) - ASSUMED_TOTAL_MARGIN_AROUND_WIDGET)
                / ViewUtils.pxFromDp(getContext(), STANDARD_WIDTH_OF_STAR));
    }

    private int getTotalStars(RangeQuestion rangeQuestion) {
        return rangeQuestion.getRangeEnd().intValue();
    }
}
