package de.marmaro.krt.ffupdater;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.view.View;

/**
 * This class is a {@link android.view.View.OnClickListener} which will open the download url for
 * a specific stage in the webbrowser (but only if the download url is available in @{@link DownloadUrl}).
 * Created by Tobiwan on 15.07.2018.
 */
public class DownloadOnClick implements View.OnClickListener {

    private DownloadUrl downloadUrl;
    private UpdateChannel updateChannel;
    private Activity activity;

    public DownloadOnClick(DownloadUrl downloadUrl, UpdateChannel updateChannel, Activity activity) {
        this.downloadUrl = downloadUrl;
        this.updateChannel = updateChannel;
        this.activity = activity;
    }

    @Override
    public void onClick(View v) {
        if (downloadUrl.isUrlAvailable(updateChannel)) {
            String url = downloadUrl.getUrl(updateChannel);
            Intent i = new Intent(Intent.ACTION_VIEW);
            i.setData(Uri.parse(url));
            activity.startActivity(i);
        }
    }
}
