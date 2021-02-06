package de.marmaro.krt.ffupdater.app

import android.content.Context
import de.marmaro.krt.ffupdater.device.ABI
import kotlinx.coroutines.Deferred

interface UpdateCheck {
    fun updateCheckBlocking(context: Context, abi: ABI): UpdateCheckResult
    fun updateCheck(context: Context, abi: ABI): Deferred<UpdateCheckResult>
}