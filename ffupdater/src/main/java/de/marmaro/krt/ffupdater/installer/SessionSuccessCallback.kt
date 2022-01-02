package de.marmaro.krt.ffupdater.installer

import android.content.pm.PackageInstaller.SessionCallback

abstract class SessionSuccessCallback(private val sessionId: Int) : SessionCallback() {
    override fun onCreated(sessionId: Int) {}
    override fun onBadgingChanged(sessionId: Int) {}
    override fun onActiveChanged(sessionId: Int, active: Boolean) {}
    override fun onProgressChanged(sessionId: Int, progress: Float) {}
    override fun onFinished(sessionId: Int, success: Boolean) {
        if (this.sessionId == sessionId) {
            onFinishedForThisSession(success)
        }
    }
    open fun onFinishedForThisSession(success: Boolean) {}
}