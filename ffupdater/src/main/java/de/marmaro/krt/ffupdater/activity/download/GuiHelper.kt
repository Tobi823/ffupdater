package de.marmaro.krt.ffupdater.activity.download

import android.view.View
import android.widget.TextView

class GuiHelper(val activity: DownloadActivity) {

    fun show(viewId: Int) {
        activity.findViewById<View>(viewId).visibility = View.VISIBLE
    }

    fun setText(textId: Int, text: String) {
        activity.findViewById<TextView>(textId).text = text
    }
}
