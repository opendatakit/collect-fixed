/*
 * Copyright 2018 Nafundi
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

package org.odk.collect.android.adapters;

import static org.odk.collect.android.formentry.media.FormMediaUtils.getClip;
import static org.odk.collect.android.formentry.media.FormMediaUtils.getClipID;
import static org.odk.collect.android.formentry.media.FormMediaUtils.getPlayableAudioURI;

import android.content.Context;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import org.javarosa.core.model.SelectChoice;
import org.javarosa.core.model.data.helper.Selection;
import org.javarosa.core.reference.InvalidReferenceException;
import org.javarosa.core.reference.ReferenceManager;
import org.javarosa.form.api.FormEntryCaption;
import org.javarosa.form.api.FormEntryPrompt;
import org.odk.collect.android.R;
import org.odk.collect.android.dynamicpreload.ExternalSelectChoice;
import org.odk.collect.android.formentry.questions.AudioVideoImageTextLabel;
import org.odk.collect.android.formentry.questions.NoButtonsItem;
import org.odk.collect.android.utilities.Appearances;
import org.odk.collect.android.utilities.HtmlUtils;
import org.odk.collect.android.utilities.MediaUtils;
import org.odk.collect.android.widgets.utilities.AudioPlayer;
import org.odk.collect.android.widgets.utilities.QuestionFontSizeUtils;
import org.odk.collect.audioclips.Clip;
import org.odk.collect.imageloader.GlideImageLoader;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import timber.log.Timber;

public abstract class AbstractSelectListAdapter extends RecyclerView.Adapter<AbstractSelectListAdapter.ViewHolder>
        implements Filterable {

    private AudioPlayer audioPlayer;
    protected Context context;
    protected List<SelectChoice> items;
    protected List<SelectChoice> filteredItems;
    protected final FormEntryPrompt prompt;
    protected final ReferenceManager referenceManager;
    protected final int playColor;
    protected final int numColumns;
    protected boolean noButtonsMode;
    private final MediaUtils mediaUtils;

    AbstractSelectListAdapter(Context context, List<SelectChoice> items, FormEntryPrompt prompt,
                              ReferenceManager referenceManager, AudioPlayer audioPlayer,
                              int playColor, int numColumns, boolean noButtonsMode, MediaUtils mediaUtils) {
        this.context = context;
        this.items = items;
        this.filteredItems = items;
        this.prompt = prompt;
        this.referenceManager = referenceManager;
        this.audioPlayer = audioPlayer;
        this.playColor = playColor;
        this.numColumns = numColumns;
        this.noButtonsMode = noButtonsMode;
        this.mediaUtils = mediaUtils;
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int index) {
        holder.bind(index);
    }

    @Override
    public int getItemCount() {
        return filteredItems.size();
    }

    @Override
    public Filter getFilter() {
        return new Filter() {
            @Override
            protected FilterResults performFiltering(CharSequence charSequence) {
                String searchStr = charSequence.toString().toLowerCase(Locale.US);
                FilterResults filterResults = new FilterResults();
                if (searchStr.isEmpty()) {
                    filterResults.values = items;
                    filterResults.count = items.size();
                } else {
                    List<SelectChoice> filteredList = new ArrayList<>();
                    for (SelectChoice item : items) {
                        if (prompt.getSelectChoiceText(item).toLowerCase(Locale.US).contains(searchStr)) {
                            filteredList.add(item);
                        }
                    }
                    filterResults.values = filteredList;
                    filterResults.count = filteredList.size();
                }

                return filterResults;
            }

            @Override
            protected void publishResults(CharSequence charSequence, FilterResults filterResults) {
                filteredItems = (List<SelectChoice>) filterResults.values;
                notifyDataSetChanged();
            }
        };
    }

    abstract CompoundButton createButton(int index, ViewGroup parent);

    void setUpButton(TextView button, int index) {
        button.setTextSize(TypedValue.COMPLEX_UNIT_DIP, QuestionFontSizeUtils.getQuestionFontSize());
        button.setText(HtmlUtils.textToHtml(prompt.getSelectChoiceText(filteredItems.get(index))));
        button.setTag(items.indexOf(filteredItems.get(index)));
    }

    boolean isItemSelected(List<Selection> selectedItems, @NonNull Selection item) {
        for (Selection selectedItem : selectedItems) {
            if (item.getValue().equalsIgnoreCase(selectedItem.getValue())) {
                return true;
            }
        }
        return false;
    }

    abstract void onItemClick(Selection selection, View view);

    public abstract List<Selection> getSelectedItems();

    public List<SelectChoice> getFilteredItems() {
        return filteredItems;
    }

    public AudioPlayer getAudioPlayer() {
        return audioPlayer;
    }

    public void setContext(Context context) {
        this.context = context;
    }

    public void setAudioPlayer(AudioPlayer audioPlayer) {
        this.audioPlayer = audioPlayer;
    }

    abstract class ViewHolder extends RecyclerView.ViewHolder {
        AudioVideoImageTextLabel audioVideoImageTextLabel;
        NoButtonsItem noButtonsItem;

        ViewHolder(View itemView) {
            super(itemView);
        }

        void bind(final int index) {
            if (noButtonsMode) {
                File imageFile = getImageFile(index);
                noButtonsItem.setUpNoButtonsItem(imageFile, getChoiceText(index), getErrorMsg(imageFile), numColumns > 1);
                noButtonsItem.setOnClickListener(v -> onItemClick(filteredItems.get(index).selection(), v));
            } else {
                addMediaFromChoice(audioVideoImageTextLabel, index, createButton(index, audioVideoImageTextLabel), filteredItems);
                audioVideoImageTextLabel.setEnabled(!prompt.isReadOnly());
                enableLongClickToAllowRemovingAnswers(itemView);
            }
        }

        private File getImageFile(int index) {
            SelectChoice selectChoice = filteredItems.get(index);
            String imageURI = selectChoice instanceof ExternalSelectChoice
                    ? ((ExternalSelectChoice) selectChoice).getImage()
                    : prompt.getSpecialFormSelectChoiceText(selectChoice, FormEntryCaption.TEXT_FORM_IMAGE);

            try {
                return new File(ReferenceManager.instance().deriveReference(imageURI).getLocalURI());
            } catch (InvalidReferenceException e) {
                Timber.w(e);
            }
            return null;
        }

        private String getChoiceText(int index) {
            SelectChoice selectChoice = filteredItems.get(index);
            return HtmlUtils.textToHtml(prompt.getSelectChoiceText(selectChoice)).toString();
        }

        private String getErrorMsg(File imageFile) {
            return context.getString(org.odk.collect.strings.R.string.file_missing, imageFile);
        }

        private void enableLongClickToAllowRemovingAnswers(View view) {
            if (view instanceof ViewGroup) {
                view.setLongClickable(true);
                for (int i = 0; i < ((ViewGroup) view).getChildCount(); i++) {
                    enableLongClickToAllowRemovingAnswers(((ViewGroup) view).getChildAt(i));
                }
            } else {
                view.setLongClickable(true);
            }
        }

        /**
         * Pull media from the current item and add it to the media layout.
         */
        public void addMediaFromChoice(AudioVideoImageTextLabel audioVideoImageTextLabel, int index, TextView textView, List<SelectChoice> items) {
            SelectChoice item = items.get(index);

            audioVideoImageTextLabel.setTag(getClipID(prompt, item));
            audioVideoImageTextLabel.setTextView(textView);
            audioVideoImageTextLabel.setMediaUtils(mediaUtils);

            String imageURI = getImageURI(index, items);
            String videoURI = prompt.getSpecialFormSelectChoiceText(item, "video");
            String bigImageURI = prompt.getSpecialFormSelectChoiceText(item, "big-image");
            String audioURI = getPlayableAudioURI(prompt, item, referenceManager);
            try {
                if (imageURI != null) {
                    audioVideoImageTextLabel.setImage(new File(referenceManager.deriveReference(imageURI).getLocalURI()), new GlideImageLoader());
                }
                if (bigImageURI != null) {
                    audioVideoImageTextLabel.setBigImage(new File(referenceManager.deriveReference(bigImageURI).getLocalURI()));
                }
                if (videoURI != null) {
                    audioVideoImageTextLabel.setVideo(new File(referenceManager.deriveReference(videoURI).getLocalURI()));
                }
                if (audioURI != null) {
                    audioVideoImageTextLabel.setAudio(audioURI, audioPlayer);
                }
            } catch (InvalidReferenceException e) {
                Timber.d(e, "Invalid media reference due to %s ", e.getMessage());
            }

            textView.setGravity(Gravity.CENTER_VERTICAL);
        }

        private String getImageURI(int index, List<SelectChoice> items) {
            String imageURI;
            if (items.get(index) instanceof ExternalSelectChoice) {
                imageURI = ((ExternalSelectChoice) items.get(index)).getImage();
            } else {
                imageURI = prompt.getSpecialFormSelectChoiceText(items.get(index),
                        FormEntryCaption.TEXT_FORM_IMAGE);
            }
            return imageURI;
        }

        void adjustAudioVideoImageTextLabelForFlexAppearance() {
            if (Appearances.isFlexAppearance(prompt)) {
                RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);

                audioVideoImageTextLabel.findViewById(R.id.audio_video_image_text_label_container).setLayoutParams(params);
                audioVideoImageTextLabel.findViewById(R.id.image_text_label_container).setLayoutParams(params);
                audioVideoImageTextLabel.getImageView().setVisibility(View.GONE);
                audioVideoImageTextLabel.getVideoButton().setVisibility(View.GONE);
                audioVideoImageTextLabel.getAudioButton().setVisibility(View.GONE);
            }
        }
    }

    public void playAudio(SelectChoice selectChoice) {
        audioPlayer.stop();
        Clip clip = getClip(prompt, selectChoice, referenceManager);
        if (clip != null) {
            audioPlayer.play(clip);
        }
    }

    public int getNumColumns() {
        return numColumns;
    }

    public abstract void clearAnswer();

    public abstract boolean hasAnswerChanged();
}
