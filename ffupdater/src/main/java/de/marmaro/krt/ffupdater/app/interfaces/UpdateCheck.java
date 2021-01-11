package de.marmaro.krt.ffupdater.app.interfaces;

import android.content.pm.PackageManager;

import de.marmaro.krt.ffupdater.device.ABI;

public interface UpdateCheck {
    UpdateCheckResult updateCheck(PackageManager pm, ABI abi);
}
