package org.odk.collect.android.tasks;

import android.net.Uri;
import android.os.AsyncTask;
import android.provider.MediaStore;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;

import org.odk.collect.android.R;
import org.odk.collect.android.activities.FormEntryActivity;
import org.odk.collect.android.exception.GDriveConnectionException;
import org.odk.collect.android.fragments.dialogs.ProgressDialogFragment;
import org.odk.collect.android.utilities.FileUtils;
import org.odk.collect.android.utilities.ImageConverter;
import org.odk.collect.android.utilities.MediaUtils;
import org.odk.collect.android.utilities.ToastUtils;

import java.io.File;

import timber.log.Timber;

public class ImageUploadingTask extends AsyncTask<Uri, Void, File> {

    private FormEntryActivity formEntryActivity;

    public ImageUploadingTask(FormEntryActivity formEntryActivity) {
        onAttach(formEntryActivity);
    }

    public void onAttach(FormEntryActivity formEntryActivity) {
        this.formEntryActivity = (FormEntryActivity) formEntryActivity;
    }

    public void onDetach() {
        this.formEntryActivity = null;
    }


    @Override
    protected File doInBackground(Uri... uris) {
        File instanceFile = formEntryActivity.getFormController().getInstanceFile();
        if (instanceFile != null) {
            String instanceFolder1 = instanceFile.getParent();
            String destImagePath = instanceFolder1 + File.separator + System.currentTimeMillis() + ".jpg";

            File chosenImage;
            try {
                chosenImage = MediaUtils.getFileFromUri(formEntryActivity, uris[0], MediaStore.Images.Media.DATA);
                if (chosenImage != null) {
                    final File newImage = new File(destImagePath);
                    FileUtils.copyFile(chosenImage, newImage);
                    ImageConverter.execute(newImage.getPath(), formEntryActivity.getWidgetWaitingForBinaryData(), formEntryActivity);
                    return newImage;
                } else {
                    Timber.e("Could not receive chosen image");
                    ToastUtils.showShortToastInMiddle(R.string.error_occured);
                    return null;
                }
            } catch (GDriveConnectionException e) {

                Timber.e("Could not receive chosen image due to connection problem");
                ToastUtils.showLongToastInMiddle(R.string.gdrive_connection_exception);
                return null;
            }
        } else {
            ToastUtils.showLongToast(R.string.image_not_saved);
            Timber.w(formEntryActivity.getString(R.string.image_not_saved));
            return null;
        }

    }

    @Override
    protected void onPostExecute(File result) {
        Fragment prev = formEntryActivity.getSupportFragmentManager().findFragmentByTag(ProgressDialogFragment.COLLECT_PROGRESS_DIALOG_TAG);
        if (prev != null) {
            DialogFragment df = (DialogFragment) prev;
            df.dismiss();
        }

        if (formEntryActivity.getCurrentViewIfODKView() != null) {
            formEntryActivity.getCurrentViewIfODKView().setBinaryData(result);
        }
        formEntryActivity.saveAnswersForCurrentScreen(formEntryActivity.DO_NOT_EVALUATE_CONSTRAINTS);
        formEntryActivity.refreshCurrentView();
    }
}

