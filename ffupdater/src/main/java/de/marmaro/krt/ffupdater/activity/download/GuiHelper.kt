package de.marmaro.krt.ffupdater.activity.download

import android.view.View

class GuiHelper(val activity: DownloadActivity) {

    fun show(viewId: Int) {
        activity.findViewById<View>(viewId).visibility = View.VISIBLE
    }
}
