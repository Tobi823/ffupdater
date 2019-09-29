package de.marmaro.krt.ffupdater.dialog;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

/**
 * Created by Tobiwan on 24.08.2019.
 */
public class FetchDownloadUrlDialog extends DialogFragment {

    public static final String TAG = "fetch_download_url_dialog";

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        return new AlertDialog.Builder(getActivity())
                .setMessage("Fetch download url for the current app - this can take a while")
                .create();
    }
}
