package de.marmaro.krt.ffupdater.background;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.work.Constraints;
import androidx.work.Data;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import de.marmaro.krt.ffupdater.MozillaVersions;
import de.marmaro.krt.ffupdater.Version;

/**
 * Created by Tobiwan on 02.04.2019.
 */
public class RemoveMe extends Worker {

    public static final String VERSION = "version";

    public RemoveMe(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    public static LiveData<WorkInfo> register() {
        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();

        OneTimeWorkRequest workRequest = new OneTimeWorkRequest.Builder(RemoveMe.class)
                .setConstraints(constraints)
                .build();

        WorkManager.getInstance().enqueue(workRequest);
        return WorkManager.getInstance().getWorkInfoByIdLiveData(workRequest.getId());
    }

    @NonNull
    @Override
    public Result doWork() {
        Version version = MozillaVersions.getVersion();
        return Result.success(new Data.Builder().putString(VERSION, version.getVersion()).build());
    }
}
