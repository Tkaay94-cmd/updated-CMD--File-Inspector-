package com.thirdpartyinspector.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.thirdpartyinspector.domain.usecase.RunScanUseCase
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class ScanWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val runScanUseCase: RunScanUseCase
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            // Optional: accept URIs from inputData under key "treeUris" as a comma-separated string
            val uriCsv = inputData.getString("treeUris")
            val uris = uriCsv?.split(",")?.map { it.trim() }?.filter { it.isNotEmpty() }
            runScanUseCase.runScan(uris)
            Result.success()
        } catch (e: Exception) {
            Result.failure()
        }
    }
}
