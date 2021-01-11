package de.marmaro.krt.ffupdater.app.interfaces;

import android.content.Context;

import de.marmaro.krt.ffupdater.device.ABI;

public interface UpdateCheck {
    UpdateCheckResult updateCheck(Context context, ABI abi);
}
