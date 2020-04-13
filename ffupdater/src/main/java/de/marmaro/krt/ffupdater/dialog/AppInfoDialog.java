package de.marmaro.krt.ffupdater.dialog;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.util.Preconditions;
import androidx.fragment.app.DialogFragment;

import java.util.Objects;

import de.marmaro.krt.ffupdater.App;
import de.marmaro.krt.ffupdater.R;

/**
 * Created by Tobiwan on 23.08.2019.
 */
public class AppInfoDialog extends DialogFragment {
    private final App app;

    public AppInfoDialog(App app) {
        this.app = app;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        Context context = Objects.requireNonNull(getContext());
        return new AlertDialog.Builder(getActivity())
                .setTitle(app.getTitle(context))
                .setMessage(app.getDescription(context))
                .create();
    }
}
