package de.marmaro.krt.ffupdater.background

import androidx.annotation.Keep
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkContinuation
import androidx.work.WorkManager

@Keep
class WorkRequestChainer(
    private val workManager: WorkManager,
    private val uniqueWorkName: String,
    private val existingWorkPolicy: ExistingWorkPolicy
) {

    fun chainInOrder(workRequests: List<OneTimeWorkRequest>): WorkContinuation? {
        if (workRequests.isEmpty()) {
            return null
        }

        val firstWorkRequest = workRequests[0]
        val remainingWorkRequests = workRequests.subList(1, workRequests.size)

        var work = workManager.beginUniqueWork(uniqueWorkName, existingWorkPolicy, firstWorkRequest)
        for (remainingWorkRequest in remainingWorkRequests) {
            work = work.then(remainingWorkRequest)
        }
        return work
    }
}